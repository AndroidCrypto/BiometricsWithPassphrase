package de.androidcrypto.biometricswithpassphrase;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class EncryptedSharedPreferencesUtils {

    /**
     * This class bundles all methods to work with EncryptedSharedPreferences
     */

    private static String masterKeyAlias;
    private static SharedPreferences sharedPreferences;
    private static final String encryptedSharedPreferencesFilename = "esp.dat";
    private static final String encryptedSharedPreferencesDefaultValue = "no data stored";
    private static final boolean encryptedSharedPreferencesDefaultValueBoolean = false;
    private static Context context;

    public static final String SETTINGS_APP_PASSWORD = "settingsAppPassword";
    public static final String SETTINGS_BIOMETRICS_LOGIN_ALLOWED = "settingsBiometricsLoginAllowed";

    public static boolean setupEncryptedSharedPreferences(Context myContext) {
        try {
            context = myContext;
            KeyGenParameterSpec keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
            masterKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec);
            sharedPreferences = EncryptedSharedPreferences.create(
                    encryptedSharedPreferencesFilename,
                    masterKeyAlias,
                    myContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void setAppPassword(String password) {
        saveEncryptedSharedPreferences(SETTINGS_APP_PASSWORD, password);
    }

    public static boolean verifyAppPassword(String password) {
        boolean result = false;
        String appPassword = getEncryptedSharedPreferences(SETTINGS_APP_PASSWORD);
        if (appPassword.equals(password)) {
            result = true;
        }
        return result;
    }

    public static boolean isAppPasswordSet() {
        boolean result = false;
        String appPassword = getEncryptedSharedPreferences(SETTINGS_APP_PASSWORD);
        if (!appPassword.equals(encryptedSharedPreferencesDefaultValue)) {
            result = true;
        }
        return result;
    }

    public static void  setSettingsBiometricsLoginAllowed(boolean value) {
        saveEncryptedSharedPreferencesBoolean(SETTINGS_BIOMETRICS_LOGIN_ALLOWED, value);
    }

    public static boolean isBiometricsLoginAllowed() {
        boolean result = false;
        result = getEncryptedSharedPreferencesBoolean(SETTINGS_BIOMETRICS_LOGIN_ALLOWED);
        return result;
    }


    /**
     *
     * private methods
     */

    private static boolean checkEncryptedSharedPreferencesStored(String key) {
        String decryptedData = sharedPreferences
                .getString(key, encryptedSharedPreferencesDefaultValue);
        if (decryptedData.equals(encryptedSharedPreferencesDefaultValue)) {
            return false;
        } else {
            return true;
        }
    }

    private static String getEncryptedSharedPreferences(String key) {
        return sharedPreferences
                .getString(key, encryptedSharedPreferencesDefaultValue);
    }

    private static void saveEncryptedSharedPreferences(String key, String value) {
        sharedPreferences
                .edit()
                .putString(key, value)
                .apply();
    }

    private static boolean getEncryptedSharedPreferencesBoolean(String key) {
        return sharedPreferences
                .getBoolean(key, encryptedSharedPreferencesDefaultValueBoolean);
    }

    private static void saveEncryptedSharedPreferencesBoolean(String key, boolean value) {
        sharedPreferences
                .edit()
                .putBoolean(key, value)
                .apply();
    }
}
