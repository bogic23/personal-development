package com.abc.personaldashboard.fragments;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.abc.personaldashboard.LoginActivity;
import com.abc.personaldashboard.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {
    private EditText nameInput, birthInput, emailInput, phoneInput;
    private Spinner genderInput;
    private EditText oldPasswordInput, newPasswordInput, confirmPasswordInput;
    private Button saveProfileButton, changePasswordButton, logoutButton;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        nameInput = view.findViewById(R.id.name_input);
        genderInput = view.findViewById(R.id.gender_input);
        birthInput = view.findViewById(R.id.birth_input);
        emailInput = view.findViewById(R.id.email_input);
        phoneInput = view.findViewById(R.id.phone_input);
        saveProfileButton = view.findViewById(R.id.save_profile_button);

        oldPasswordInput = view.findViewById(R.id.old_password);
        newPasswordInput = view.findViewById(R.id.new_password);
        confirmPasswordInput = view.findViewById(R.id.confirm_password);
        changePasswordButton = view.findViewById(R.id.change_password_button);
        logoutButton = view.findViewById(R.id.logout_button);

        loadProfile();

        birthInput.setOnClickListener(v -> showBirthDatePicker());
        birthInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showBirthDatePicker();
            }
        });
        saveProfileButton.setOnClickListener(v -> saveProfile());
        changePasswordButton.setOnClickListener(v -> changePassword());
        logoutButton.setOnClickListener(v -> logout());

        return view;
    }

    private void loadProfile() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        String email = user.getEmail() == null ? "" : user.getEmail();
        emailInput.setText(email);

        firestore.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) {
                        return;
                    }

                    String username = documentSnapshot.getString("username");
                    String gender = documentSnapshot.getString("gender");
                    String birthDate = documentSnapshot.getString("birthDate");
                    String phoneNumber = documentSnapshot.getString("phoneNumber");

                    nameInput.setText(username == null ? "" : username);
                    selectGender(gender);
                    birthInput.setText(birthDate == null ? "" : birthDate);
                    phoneInput.setText(phoneNumber == null ? "" : phoneNumber);
                })
                .addOnFailureListener(exception -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveProfile() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        String email = user.getEmail() == null ? "" : user.getEmail();
        Map<String, Object> profile = new HashMap<>();
        profile.put("email", email);
        profile.put("username", nameInput.getText().toString().trim());
        profile.put("gender", genderInput.getSelectedItem().toString());
        profile.put("birthDate", birthInput.getText().toString().trim());
        profile.put("phoneNumber", phoneInput.getText().toString().trim());

        saveProfileButton.setEnabled(false);
        firestore.collection("users").document(user.getUid())
                .set(profile, SetOptions.merge())
                .addOnCompleteListener(task -> {
                    saveProfileButton.setEnabled(true);
                    if (getContext() == null) {
                        return;
                    }

                    if (task.isSuccessful()) {
                        emailInput.setText(email);
                        Toast.makeText(getContext(), "Profile saved!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to save profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void changePassword() {
        String newPass = newPasswordInput.getText().toString();
        String confirmPass = confirmPasswordInput.getText().toString();

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            return;
        }

        if (!newPass.equals(confirmPass)) {
            Toast.makeText(getContext(), "New passwords don't match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPass.isEmpty()) {
            Toast.makeText(getContext(), "Password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(oldPasswordInput.getText().toString())) {
            Toast.makeText(getContext(), "Current password is required", Toast.LENGTH_SHORT).show();
            return;
        }

        changePasswordButton.setEnabled(false);
        user.updatePassword(newPass).addOnCompleteListener(task -> {
            changePasswordButton.setEnabled(true);
            if (getContext() == null) {
                return;
            }

            if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Password changed!", Toast.LENGTH_SHORT).show();
                    oldPasswordInput.setText("");
                    newPasswordInput.setText("");
                    confirmPasswordInput.setText("");
            } else {
                Toast.makeText(getContext(), "Log in again before changing password", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void selectGender(String gender) {
        if (gender == null) {
            return;
        }

        for (int index = 0; index < genderInput.getCount(); index++) {
            if (gender.equals(genderInput.getItemAtPosition(index).toString())) {
                genderInput.setSelection(index);
                return;
            }
        }
    }

    private void showBirthDatePicker() {
        if (getContext() == null) {
            return;
        }

        Calendar selectedDate = Calendar.getInstance();
        String currentBirthDate = birthInput.getText().toString().trim();
        if (!TextUtils.isEmpty(currentBirthDate)) {
            String[] dateParts = currentBirthDate.split("-");
            if (dateParts.length == 3) {
                try {
                    selectedDate.set(
                            Integer.parseInt(dateParts[0]),
                            Integer.parseInt(dateParts[1]) - 1,
                            Integer.parseInt(dateParts[2])
                    );
                } catch (NumberFormatException ignored) {
                }
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> birthInput.setText(
                        String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                ),
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void logout() {
        if (getActivity() == null) {
            return;
        }

        FirebaseAuth.getInstance().signOut();
        GoogleSignIn.getClient(getActivity(), com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN).signOut();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }
}
