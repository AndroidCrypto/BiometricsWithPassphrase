package de.androidcrypto.biometricswithpassphrase;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Base64;
import android.util.Log;
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
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.concurrent.Executor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    EditText plaintext, passphrase, ciphertext, decryptedtext;
    TextView appStatus;
    Button encrypt, decrypt, settings;
    Intent settingsIntent;
    ScrollView mainActivityView;

    int REQUEST_CODE = 100;



    private static final int BIOMETRIC_STRONG = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;
    private static final int BIOMETRIC_WEAK = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK; // not used
    private static final int DEVICE_CREDENTIAL = androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    boolean isSetupDoneEncryptedSharedPreferences = false;
    boolean isAppPasswordSet = false;
    boolean appPasswordVerified = false;
    boolean isBiometricsLoginAllowed = false;
    boolean isAppUnlocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        plaintext = findViewById(R.id.etPlaintext);
        passphrase = findViewById(R.id.etPassphrase);
        ciphertext = findViewById(R.id.etCiphertext);
        decryptedtext = findViewById(R.id.etDecryptedtext);
        encrypt = findViewById(R.id.btnEncrypt);
        decrypt = findViewById(R.id.btnDecrypt);
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

        // if app password is set check if authorization should be with biometrics
        isBiometricsLoginAllowed = EncryptedSharedPreferencesUtils.isBiometricsLoginAllowed();
        if (isBiometricsLoginAllowed) {
            updateStatusInfo("Biometrics login is allowed");
        } else {
            updateStatusInfo("Biometrics login is NOT allowed");
            // here we are going to verify the password
            verifyPassword(mainActivityView);
        }

        if (appPasswordVerified) {
            isAppUnlocked = true;
            updateStatusInfo("app password is verified, do what you want to do");
        } else {
            isAppUnlocked = false;
            updateStatusInfo("app password is NOT VERIFIED");
        }

/*
        // check that a masterkey was stored
        isPassphraseSet = EncryptedSharedPreferencesUtils.checkEncryptedSharedPreferencesStored(namePassphraseSalt);
        plaintext.setText(String.valueOf(isPassphraseSet));
        passphrase.setText(EncryptedSharedPreferencesUtils.getEncryptedSharedPreferences(namePassphraseSalt));

        EncryptedSharedPreferencesUtils.saveEncryptedSharedPreferences(namePassphraseSalt,"abcd");
*/
        //Toast.makeText(this,"isPassphraseSet " + isPassphraseSet,Toast.LENGTH_SHORT).show();

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
                    isAppUnlocked = false;
                    Toast.makeText(getApplicationContext(),
                                    "Authentication error: " + errString, Toast.LENGTH_SHORT)
                            .show();
                }

                @Override
                public void onAuthenticationSucceeded(
                        @NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    isAppUnlocked = true;
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
                    .setSubtitle("Log in using your biometric credential")
                    .setNegativeButtonText("Use app password")
                    .build();

            // Prompt appears when user clicks "Log in".
            // Consider integrating with the keystore to unlock cryptographic operations,
            // if needed by your app.
            Button biometricLoginButton = findViewById(R.id.btnBiometricsLogin);
            biometricLoginButton.setOnClickListener(view -> {
                biometricPrompt.authenticate(promptInfo);
            });

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

        encrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ciphertextStr = "";

                //EncryptionUtilities.encryptionWithKey("1234567890123456".getBytes(StandardCharsets.UTF_8), "data".getBytes(StandardCharsets.UTF_8), ciphertext);
                //ciphertext.setText(ciphertextStr);

                // get the passphrase from EditText as char array
                int passphraseLength = passphrase.length();
                char[] passphraseChar = new char[passphraseLength];
                passphrase.getText().getChars(0, passphraseLength, passphraseChar, 0);
                // do not run the encryption on main gui thread as it may block
                //String ciphertextData = doEncryptionAesGcmPbkdf2(passphraseChar, plaintext.getText().toString().getBytes(StandardCharsets.UTF_8));
                //ciphertext.setText(ciphertextData);
                // run the encryption in a different thread instead
                Thread thread = new Thread(){
                    public void run(){
                        //doAesEncryption(passphraseChar, plaintext.getText().toString().getBytes(StandardCharsets.UTF_8));
                    }
                };
                //thread.start();
            }
        });

        decrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get the passphrase from EditText as char array
                int passphraseLength = passphrase.length();
                char[] passphraseChar = new char[passphraseLength];
                passphrase.getText().getChars(0, passphraseLength, passphraseChar, 0);
                // do not run the decryption on main gui thread as it may block
                // String decryptedtextData = doDecryptionAesGcmPbkdf2(passphraseChar, ciphertext.getText().toString());
                // decryptedtext.setText(decryptedtextData);
                // run the encryption in a different thread instead
                Thread thread = new Thread(){
                    public void run(){
                        //doAesDecryption(passphraseChar, ciphertext.getText().toString());
                    }
                };
                //thread.start();
            }
        });

        // check biometrics are available
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BIOMETRIC_STRONG | DEVICE_CREDENTIAL)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.d("MY_APP_TAG", "App can authenticate using biometrics.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Log.e("MY_APP_TAG", "No biometric features available on this device.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Log.e("MY_APP_TAG", "Biometric features are currently unavailable.");
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                // Prompts the user to create credentials that your app accepts.
                final Intent enrollIntent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
                enrollIntent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        BIOMETRIC_STRONG | DEVICE_CREDENTIAL);
                startActivityForResult(enrollIntent, REQUEST_CODE);
                break;
        }
        // show a biometrics dialog
    }

    // check if Biometric is ready to use
    private boolean checkBiometric() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int success = biometricManager.canAuthenticate(BIOMETRIC_STRONG | DEVICE_CREDENTIAL);
        if (success == BiometricManager.BIOMETRIC_SUCCESS) {
            return true;
        } else {
            return false;
        }
    }

    // error dialog
    private void errorAndQuitAlert(String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Fehler");
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finishAndRemoveTask(); // stops the app
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

    // error dialog
    private void alertView(String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Fehler");
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
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
        alertDialog.setPositiveButton("open", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                /* no password length check
                int oldPassphraseLength = oldPassphrase.length();
                // test on password length
                if (oldPassphraseLength < MINIMAL_PASSWORD_LENGTH) {
                    Snackbar snackbar = Snackbar.make(v, "The password is too short", Snackbar.LENGTH_LONG);
                    snackbar.setBackgroundTint(ContextCompat.getColor(v.getContext(), R.color.red));
                    snackbar.show();
                    return;
                }*/

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