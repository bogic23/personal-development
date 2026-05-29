package com.abc.personaldashboard.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.abc.personaldashboard.R;
import com.abc.personaldashboard.database.*;

public class ProfileFragment extends Fragment {
    private EditText nameInput, genderInput, birthInput, emailInput, phoneInput;
    private EditText oldPasswordInput, newPasswordInput, confirmPasswordInput;
    private Button saveProfileButton, changePasswordButton;
    private AppDatabase database;
    private UserProfile currentProfile;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        database = AppDatabase.getInstance(getContext());

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

        loadProfile();

        saveProfileButton.setOnClickListener(v -> saveProfile());
        changePasswordButton.setOnClickListener(v -> changePassword());

        return view;
    }

    private void loadProfile() {
        new Thread(() -> {
            currentProfile = database.userProfileDao().getProfile();
            if (currentProfile == null) {
                currentProfile = new UserProfile("User", "Not Set", "Not Set", "1234");
                database.userProfileDao().insert(currentProfile);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    nameInput.setText(currentProfile.getName());
                    genderInput.setText(currentProfile.getGender());
                    birthInput.setText(currentProfile.getBirthDate());
                    emailInput.setText(currentProfile.getEmail());
                    phoneInput.setText(currentProfile.getPhoneNumber());
                });
            }
        }).start();
    }

    private void saveProfile() {
        if (currentProfile != null) {
            currentProfile.setName(nameInput.getText().toString());
            currentProfile.setGender(genderInput.getText().toString());
            currentProfile.setBirthDate(birthInput.getText().toString());
            currentProfile.setEmail(emailInput.getText().toString());
            currentProfile.setPhoneNumber(phoneInput.getText().toString());

            new Thread(() -> {
                database.userProfileDao().update(currentProfile);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Profile saved!", Toast.LENGTH_SHORT).show()
                    );
                }
            }).start();
        }
    }

    private void changePassword() {
        String oldPass = oldPasswordInput.getText().toString();
        String newPass = newPasswordInput.getText().toString();
        String confirmPass = confirmPasswordInput.getText().toString();

        if (currentProfile == null) return;

        if (!oldPass.equals(currentProfile.getPassword())) {
            Toast.makeText(getContext(), "Wrong old password", Toast.LENGTH_SHORT).show();
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

        currentProfile.setPassword(newPass);
        new Thread(() -> {
            database.userProfileDao().update(currentProfile);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Password changed!", Toast.LENGTH_SHORT).show();
                    oldPasswordInput.setText("");
                    newPasswordInput.setText("");
                    confirmPasswordInput.setText("");
                });
            }
        }).start();
    }
}