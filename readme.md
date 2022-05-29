# Biometrics with passphrase

This concept study shows how to make an app using biometrics (=fingerprint) or an app password.

This differs from the usual option "biometrics use OR device pin" as it allows to use the indiviual 
app password as source for any kind of encryption and later data decryption ("data recovery").

This is usefull when the app data is in a Google drive backup but for recovering on a new device you 
need a password to decrypt the data. 

The login is done in these steps:

1) as the **app password** ("password") is the fallback option it is neccessary to have a password set. The  
first step is to check that the password is already set. If the password was not set proceed to step 2, 
if the password ist set proceed to step 5.

2) the Settings activity is launched and there is a check if biometrics credentials (= a fingerprint)  
has been enrolled on the device. If the check fails a warning is shown.

3) the only enabled button is the "SET PASSWORD" button, when pressed a dialog opens to enter the app password. 
The app Settings activity provides a minimal password check with a minimal password length check - you should 
consider to use a password checker that checks e.g. for capital letter, numbers and special characters. 
The entered password is stored encrypted in the app's internal storage using the **EncryptedSharedPreferences** 
class.

**Warning: NEVER store passwords even in encrypted form on the device, better run a key derivation so that an attacker 
has no chance to get the password data.**. An example how to do this is given in my app **EncryptedLoginWithPassword**. 

4) The "SET PASSWORD" button gets disabled but the "CHANGE PASSWORD" and "RESET PASSWORD" buttons are enabled. Depending 
on the biometrics credentials check in step 2 the "use Biometrics login (Fingerprint)" switch is enabled.

5) At this point the app is started and a password is set. Depending on the switch in the Settings activity (see step 4) 
the workflow differs:

5a) the "use Biometrics login" switch is not checked - or -

5b) the "use Biometrics login" switch is checked ("green color")

5a) A dialog opens to enter the app password - if the "cancel" button is pressed or a wrong app password is entered 
an error dialog apperas and the app is closed. If the entered password is correct the app will start.

5b) The biometrics prompt appears to use your fingerprint to log in. If the device detects a "wrong" fingerprint a warning  
is shown and the user can use a new fingerprint check (consider to close the app for security reasons instead). If 
the check worked the app is starting. 

6) The biometrics prompt has another option "USE APP PASSWORD" - when pressing this button the workflow 5a) is starting. 

7) The app is checking on every start that the biometrics enrollment is still in place, if not (you deleted the fingerprints) 
the biometrics login option is disabled for the app as well.

Additional features in **Settings activity**:

There are two more buttons on the Settings screen:

- change password: enter the old and the new password and if the old password gets verified AND the  
  password length check is successful the new password is saved.

- reset password: the stored password is deleted and the "use Biometrics login" is set to "not set" if checked before.

![Image 1](images/biometrics_00001.png?raw=true "Image 1")
