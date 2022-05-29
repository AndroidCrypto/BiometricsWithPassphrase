package de.androidcrypto.biometricswithpassphrase;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    TextView appStatus;
    Button settings;
    Intent settingsIntent;
    ScrollView mainActivityView;

    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    boolean isSetupDoneEncryptedSharedPreferences = false;
    boolean isAppPasswordSet = false;
    boolean appPasswordVerified = false;
    boolean isAppBiometricsEnrolled = false;
    boolean isBiometricsLoginAllowed = false;
    boolean isAppUnlocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = findViewById(R.id.btnSettings);
        settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
        mainActivityView = findViewById(R.id.mainActivityView);
        appStatus = findViewById(R.id.tvAppStatus);

        // setup EncryptedSharedPreferences
        isSetupDoneEncryptedSharedPreferences = EncryptedSharedPreferencesUtils.setupEncryptedSharedPreferences(this);
        updateStatusInfo("encrypted shared preferences is setup");

        // if app password is not set = first start up then set it
        isAppPasswordSet = EncryptedSharedPreferencesUtils.isAppPasswordSet();
        if (!isAppPasswordSet) {
            updateStatusInfo("App password is not set so far");
            startActivity(settingsIntent);
        } else {
            updateStatusInfo("App password is set, check for biometrics");
        }

        // first check that Biometrics are enrolled on the device
        isAppBiometricsEnrolled = Biometrics.checkBiometric(mainActivityView.getContext());
        if (isAppBiometricsEnrolled) {
            updateStatusInfo("Biometrics are enrolled on this device");
        } else {
            updateStatusInfo("Biometrics are NOT enrolled on this device");
        }

        if (isAppBiometricsEnrolled) {
            // if app password is set check if authorization should be with biometrics
            isBiometricsLoginAllowed = EncryptedSharedPreferencesUtils.isBiometricsLoginAllowed();
            if (isBiometricsLoginAllowed) {
                updateStatusInfo("Biometrics login is allowed");
            } else {
                updateStatusInfo("Biometrics login is NOT allowed");
                // here we are going to verify the password
                verifyPassword(mainActivityView);
            }
        }

        if (appPasswordVerified) {
            isAppUnlocked = true;
            updateStatusInfo("app password is verified, do what you want to do");
        } else {
            isAppUnlocked = false;
            updateStatusInfo("app password is NOT VERIFIED / checked");
        }

        // if biometrics login is allowed run these methods
        if (isBiometricsLoginAllowed) {
            // biometrics login
            executor = ContextCompat.getMainExecutor(this);
            biometricPrompt = new BiometricPrompt(MainActivity.this,
                    executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode,
                                                  @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    // this part is called when pressing "use app password" instead of fingerprint
                    isAppUnlocked = false;
                    verifyPassword(mainActivityView);
                }

                @Override
                public void onAuthenticationSucceeded(
                        @NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    isAppUnlocked = true;
                    updateStatusInfo("biometric/fingerprint is verified");
                    Toast.makeText(getApplicationContext(),
                            "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Toast.makeText(getApplicationContext(), "Authentication failed",
                                    Toast.LENGTH_SHORT)
                            .show();
                }

            });

            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric login for my app")
                    .setSubtitle("Log in using your biometric credentials")
                    .setNegativeButtonText("Use app password")
                    .build();

            // biometricPrompt.authenticate forces to show the prompt on startup
            biometricPrompt.authenticate(promptInfo);

        }

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(settingsIntent);
                isBiometricsLoginAllowed = EncryptedSharedPreferencesUtils.isBiometricsLoginAllowed();
                if (isBiometricsLoginAllowed) {
                    updateStatusInfo("Biometrics login is allowed");
                } else {
                    updateStatusInfo("Biometrics login is NOT allowed");
                }
            }
        });

    }

    // error dialog
    private void errorAndQuitAlert(String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finishAndRemoveTask(); // stops the app
                        finishAffinity();
                    }
                });
        // to avoid the back button usage
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                finishAndRemoveTask(); // stops the app
                finishAffinity();
            }
        });
        alertDialog.show();
    }

    private void verifyPassword(View v) {
        android.app.AlertDialog.Builder alertDialog = new android.app.AlertDialog.Builder(v.getContext());
        alertDialog.setTitle("Enter App password");
        String message = "\nPlease enter the\napp password and press\nOPEN, to use all\nfunctions.";
        alertDialog.setMessage(message);
        final EditText oldPassphrase = new EditText(v.getContext());
        oldPassphrase.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.round_rect_shape, null));
        oldPassphrase.setHint("  enter the password");
        oldPassphrase.setPadding(50, 20, 50, 20);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp1.setMargins(36, 36, 36, 36);
        oldPassphrase.setLayoutParams(lp1);
        RelativeLayout container = new RelativeLayout(v.getContext());
        RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        container.setLayoutParams(rlParams);
        container.addView(oldPassphrase);
        alertDialog.setView(container);
        alertDialog.setPositiveButton("open", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // no password length check
                appPasswordVerified = EncryptedSharedPreferencesUtils.verifyAppPassword(oldPassphrase.getText().toString());
                if (appPasswordVerified) {
                    isAppUnlocked = true;
                    Snackbar snackbar = Snackbar.make(v, "app password verified", Snackbar.LENGTH_SHORT);
                    snackbar.setBackgroundTint(ContextCompat.getColor(v.getContext(), R.color.green));
                    snackbar.show();
                } else {
                    isAppUnlocked = false;
                    Snackbar snackbar = Snackbar.make(v, "wrong password entered", Snackbar.LENGTH_SHORT);
                    snackbar.setBackgroundTint(ContextCompat.getColor(v.getContext(), R.color.red));
                    snackbar.show();
                    errorAndQuitAlert("You entered the wrong app password, the app will quit now");
                }
            }
        });
        alertDialog.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                appPasswordVerified = false;
                Snackbar snackbar = Snackbar.make(v, "No password verification", Snackbar.LENGTH_SHORT);
                snackbar.setBackgroundTint(ContextCompat.getColor(v.getContext(), R.color.red));
                snackbar.show();
                errorAndQuitAlert("You cancelled the app password verification, the app will quit now");
            }
        });
        alertDialog.show();
    }

    // update status info textview
    private void updateStatusInfo(String message) {
        String oldString = appStatus.getText().toString();
        String newString = oldString + "\n" + message;
        appStatus.setText(newString);
    }
}