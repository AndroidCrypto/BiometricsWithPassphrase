package de.androidcrypto.biometricswithpassphrase;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.snackbar.Snackbar;

public class SettingsActivity extends AppCompatActivity {

    ScrollView settingScrollView;

    private static final int MINIMAL_PASSWORD_LENGTH = 3;
    Button setAppPassword, changeAppPassword, resetAppPassword;
    SwitchCompat useBiometrics;
    boolean isAppPasswordSet = false;
    boolean isAppBiometricsEnrolled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setAppPassword = findViewById(R.id.btnSettingsSetAppPassword);
        changeAppPassword = findViewById(R.id.btnSettingsChangeAppPassword);
        resetAppPassword = findViewById(R.id.btnSettingsResetAppPassword);
        useBiometrics = findViewById(R.id.swBiometricsUse);
        settingScrollView = findViewById(R.id.SettingsScrollView);

        isAppPasswordSet = EncryptedSharedPreferencesUtils.isAppPasswordSet();
        if (isAppPasswordSet) {
            activateButtons();
        }

        isAppBiometricsEnrolled = Biometrics.checkBiometric(getApplicationContext());
        if (!isAppBiometricsEnrolled) {
            EncryptedSharedPreferencesUtils.setSettingsBiometricsLoginAllowed(false);
            useBiometrics.setChecked(false);
            useBiometrics.setEnabled(false);
            warningAlert (settingScrollView.getContext(), "Your device has not setup Biometrics so you can't use Biometrics for login.");
        }

        // set switch from EncryptedSharedPreferences
        if (isAppBiometricsEnrolled) {
            useBiometrics.setChecked(EncryptedSharedPreferencesUtils
                    .isBiometricsLoginAllowed(
                    ));
        }
        setAppPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePasswordPressed(view);
            }
        });

        changeAppPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changePasswortPressed(view);
            }
        });

        resetAppPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetPasswordPressed(view);
            }
        });

        useBiometrics.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                EncryptedSharedPreferencesUtils.setSettingsBiometricsLoginAllowed(b);
            }
        });
    }

    private void savePasswordPressed(View v) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(v.getContext());
        alertDialog.setTitle("Enter the app password");
        String message = "\nPlease enter a (minimum)\n8-characters long password and press\nSAVE, to use all\nfunctions.";
        alertDialog.setMessage(message);
        final EditText oldPassphrase = new EditText(v.getContext());
        oldPassphrase.setBackground( ResourcesCompat.getDrawable(getResources(),R.drawable.round_rect_shape, null));
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
        alertDialog.setPositiveButton("save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int oldPassphraseLength = oldPassphrase.length();
                // test on password length
                if (oldPassphraseLength < MINIMAL_PASSWORD_LENGTH) {
                    Snackbar snackbar = Snackbar.make(v, "The password is too short", Snackbar.LENGTH_LONG);
                    snackbar.setBackgroundTint(ContextCompat.getColor(v.getContext(), R.color.red));
                    snackbar.show();
                    return;
                }
                EncryptedSharedPreferencesUtils.setAppPassword(oldPassphrase.getText().toString());
                activateButtons();
                Snackbar snackbar = Snackbar.make(v, "The app is ready now", Snackbar.LENGTH_SHORT);
                snackbar.setBackgroundTint(ContextCompat.getColor(v.getContext(), R.color.green));
                snackbar.show();
            }
        });
        alertDialog.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Snackbar snackbar = Snackbar.make(v, "No password was saved", Snackbar.LENGTH_SHORT);
                snackbar.setBackgroundTint(ContextCompat.getColor(v.getContext(), R.color.red));
                snackbar.show();
            }
        });
        alertDialog.show();
    }

    private void resetPasswordPressed(View v) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(v.getContext());
        alertDialog.setTitle("Reset app password");
        String message = "\nWhen resetting the app password\nall data get deleted.\nPlease press RESET\nto proceed.";
        alertDialog.setMessage(message);
        RelativeLayout container = new RelativeLayout(v.getContext());
        RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        container.setLayoutParams(rlParams);
        alertDialog.setView(container);
        alertDialog.setPositiveButton("reset", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                EncryptedSharedPreferencesUtils.setSettingsBiometricsLoginAllowed(false);
                EncryptedSharedPreferencesUtils.deleteAppPassword();
                deactivateButtons();
                Snackbar snackbar = Snackbar.make(v, "The app password was resetted and all data wer deleted.", Snackbar.LENGTH_LONG);
                snackbar.setBackgroundTint(ContextCompat.getColor(v.getContext(), R.color.green));
                snackbar.show();
            }
        });
        alertDialog.setNegativeButton("abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Snackbar snackbar = Snackbar.make(v, "Es wurde kein Reset durchgeführt", Snackbar.LENGTH_LONG);
                snackbar.setBackgroundTint(ContextCompat.getColor(v.getContext(), R.color.orange));
                snackbar.show();
            }
        });
        alertDialog.show();
    }

    private void changePasswortPressed(View v) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(v.getContext());
        alertDialog.setTitle("Change app password");
        String message = "\nPlease enter the old password first\nand then the new password,\nthen press on CHANGE.";
        alertDialog.setMessage(message);
        final EditText oldPassphrase = new EditText(v.getContext());
        oldPassphrase.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.round_rect_shape, null));
        oldPassphrase.setHint("  altes Passwort");
        oldPassphrase.setPadding(50, 20, 50, 20);
        final EditText newPassphrase = new EditText(v.getContext());
        newPassphrase.setBackground(ResourcesCompat.getDrawable(getResources(),R.drawable.round_rect_shape, null));
        newPassphrase.setHint("  neues Passwort");
        newPassphrase.setPadding(50, 20, 50, 20);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp1.setMargins(36, 36, 36, 36);
        oldPassphrase.setLayoutParams(lp1);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.setMargins(36, 200, 36, 36);
        newPassphrase.setLayoutParams(lp2);
        RelativeLayout container = new RelativeLayout(v.getContext());
        RelativeLayout.LayoutParams rlParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        container.setLayoutParams(rlParams);
        container.addView(oldPassphrase);
        container.addView(newPassphrase);
        //now set view to dialog
        alertDialog.setView(container);
        alertDialog.setPositiveButton("change", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int newPassphraseLength = newPassphrase.length();
                String oldPassword = oldPassphrase.getText().toString();
                String newPassword = newPassphrase.getText().toString();
                if (newPassphraseLength < MINIMAL_PASSWORD_LENGTH) {
                    Snackbar snackbar = Snackbar.make(v, "The new password is too short", Snackbar.LENGTH_LONG);
                    snackbar.setBackgroundTint(ContextCompat.getColor(v.getContext(), R.color.red));
                    snackbar.show();
                    return;
                }
                // verifying the old password
                if (!EncryptedSharedPreferencesUtils.verifyAppPassword(oldPassword)) {
                    Snackbar snackbar = Snackbar.make(v, "The old password is wrong", Snackbar.LENGTH_LONG);
                    snackbar.setBackgroundTint(ContextCompat.getColor(v.getContext(), R.color.red));
                    snackbar.show();
                    return;
                }
                // saving the new password
                EncryptedSharedPreferencesUtils.setAppPassword(newPassword);
                Snackbar snackbar = Snackbar.make(v, "The app password is changed", Snackbar.LENGTH_SHORT);
                snackbar.setBackgroundTint(ContextCompat.getColor(v.getContext(), R.color.green));
                snackbar.show();
            }
        });
        alertDialog.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Snackbar snackbar = Snackbar.make(v, "The app password change was cancelled", Snackbar.LENGTH_LONG);
                snackbar.setBackgroundTint(ContextCompat.getColor(v.getContext(), R.color.red));
                snackbar.show();
            }
        });
        alertDialog.show();
    }

    // warning dialog
    private void warningAlert(Context context, String message) {
        androidx.appcompat.app.AlertDialog alertDialog = new androidx.appcompat.app.AlertDialog.Builder(context).create();
        alertDialog.setTitle("Warning");
        alertDialog.setMessage(message);
        alertDialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        // to avoid the back button usage
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private void activateButtons() {
        setAppPassword.setEnabled(false);
        changeAppPassword.setEnabled(true);
        resetAppPassword.setEnabled(true);
        useBiometrics.setEnabled(true);
    }

    private void deactivateButtons() {
        setAppPassword.setEnabled(true);
        changeAppPassword.setEnabled(false);
        resetAppPassword.setEnabled(false);
        useBiometrics.setEnabled(false);
    }
}