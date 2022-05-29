package de.androidcrypto.biometricswithpassphrase;

import android.content.Context;
import androidx.biometric.BiometricManager;

public class Biometrics {

    private static final int BIOMETRIC_STRONG = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG;

    // check if Biometric is ready to use
    public static boolean checkBiometric(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        //int success = biometricManager.canAuthenticate(BIOMETRIC_STRONG | DEVICE_CREDENTIAL);
        int success = biometricManager.canAuthenticate(BIOMETRIC_STRONG);
        if (success == BiometricManager.BIOMETRIC_SUCCESS) {
            return true;
        } else {
            return false;
        }
    }
}
