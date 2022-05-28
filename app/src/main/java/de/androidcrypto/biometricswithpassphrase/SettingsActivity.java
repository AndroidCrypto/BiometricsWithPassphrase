package de.androidcrypto.biometricswithpassphrase;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;

import com.google.android.material.snackbar.Snackbar;

import java.nio.charset.StandardCharsets;

public class SettingsActivity extends AppCompatActivity {

    private static final int MINIMAL_PASSWORD_LENGTH = 3;
    Button setAppPassword, changeAppPassword, resetAppPassword;
    SwitchCompat useBiometrics;
    boolean isAppPasswordSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        setAppPassword = findViewById(R.id.btnSettingsSetAppPassword);
        changeAppPassword = findViewById(R.id.btnSettingsChangeAppPassword);
        resetAppPassword = findViewById(R.id.btnSettingsResetAppPassword);
        useBiometrics = findViewById(R.id.swBiometricsUse);

        isAppPasswordSet = EncryptedSharedPreferencesUtils.isAppPasswordSet();
        if (isAppPasswordSet) {
            activateButtons();
        }

        // set switch from EncryptedSharedPreferences
        useBiometrics.setChecked(EncryptedSharedPreferencesUtils
                .isBiometricsLoginAllowed(
                ));

        setAppPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePasswordPressed(view);
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
        alertDialog.setTitle("Enter App password");
        String message = "\nPlease enter a (minimum)\n8-characters long password and press\nSAVE, to use all\nfunctions.";
        alertDialog.setMessage(message);
        final EditText oldPassphrase = new EditText(v.getContext());
        oldPassphrase.setBackground( ResourcesCompat.getDrawable(getResources(),R.drawable.round_rect_shape, null));
        //oldPassphrase.setBackground(getResources().getDrawable(R.drawable.round_rect_shape));
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