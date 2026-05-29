package com.abc.personaldashboard;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final int GOOGLE_SIGN_IN_REQUEST = 1001;

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private Button registerButton;
    private Button googleLoginButton;
    private ProgressBar loginProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        View loginContainer = findViewById(R.id.login_container);
        int originalLeftPadding = loginContainer.getPaddingLeft();
        int originalTopPadding = loginContainer.getPaddingTop();
        int originalRightPadding = loginContainer.getPaddingRight();
        int originalBottomPadding = loginContainer.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(loginContainer, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    originalLeftPadding,
                    systemBars.top + originalTopPadding,
                    originalRightPadding,
                    systemBars.bottom + originalBottomPadding
            );
            return windowInsets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            openDashboard();
            return;
        }

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        registerButton = findViewById(R.id.register_button);
        googleLoginButton = findViewById(R.id.google_login_button);
        loginProgress = findViewById(R.id.login_progress);

        loginButton.setOnClickListener(view -> signInWithEmail());
        registerButton.setOnClickListener(view -> createAccount());
        googleLoginButton.setOnClickListener(view -> signInWithGoogle());
    }

    private void signInWithEmail() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (!validateEmailPassword(email, password)) {
            return;
        }

        setLoading(true);
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        openDashboard();
                    } else {
                        showAuthError("Email login failed", task.getException());
                    }
                });
    }

    private void createAccount() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (!validateEmailPassword(email, password)) {
            return;
        }

        setLoading(true);
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        saveUserProfile(task.getResult().getUser());
                    } else {
                        showAuthError("Account creation failed", task.getException());
                    }
                });
    }

    private void signInWithGoogle() {
        int webClientIdResource = getResources().getIdentifier(
                "default_web_client_id",
                "string",
                getPackageName()
        );

        if (webClientIdResource == 0) {
            Toast.makeText(this, "Configuration error: Missing default_web_client_id from google-services.json", Toast.LENGTH_LONG).show();
            return;
        }

        String webClientId = getString(webClientIdResource);

        if (TextUtils.isEmpty(webClientId) || !webClientId.contains(".apps.googleusercontent.com")) {
            Toast.makeText(this, "Configuration error: Invalid Google Web Client ID. Please check your google-services.json", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            if (googleSignInClient == null) {
                GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build();
                googleSignInClient = GoogleSignIn.getClient(this, signInOptions);
            }

            setLoading(true);
            startActivityForResult(googleSignInClient.getSignInIntent(), GOOGLE_SIGN_IN_REQUEST);
        } catch (Exception e) {
            setLoading(false);
            showAuthError("Failed to initialize Google Sign-In", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != GOOGLE_SIGN_IN_REQUEST) {
            return;
        }

        Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = accountTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException exception) {
            setLoading(false);
            showAuthError("Google login failed", exception);
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        saveUserProfile(task.getResult().getUser());
                    } else {
                        showAuthError("Firebase Google login failed", task.getException());
                    }
                });
    }

    private void saveUserProfile(FirebaseUser user) {
        if (user == null) {
            openDashboard();
            return;
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("email", user.getEmail() == null ? "" : user.getEmail());
        profile.put("emailLowercase", user.getEmail() == null ? "" : user.getEmail().toLowerCase(Locale.US));

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .set(profile, SetOptions.merge())
                .addOnCompleteListener(task -> openDashboard());
    }

    private boolean validateEmailPassword(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return false;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            return false;
        }

        return true;
    }

    private void setLoading(boolean loading) {
        loginProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
        registerButton.setEnabled(!loading);
        googleLoginButton.setEnabled(!loading);
    }

    private void showAuthError(String fallbackMessage, Exception exception) {
        String message = exception != null && exception.getMessage() != null
                ? exception.getMessage()
                : fallbackMessage;
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void openDashboard() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
