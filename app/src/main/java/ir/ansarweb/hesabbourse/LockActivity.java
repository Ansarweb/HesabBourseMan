package ir.ansarweb.hesabbourse;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import android.hardware.fingerprint.FingerprintManager;

public class LockActivity extends Activity {

    private static final String PREFS = "app_lock";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_PIN_SALT = "pin_salt";
    private static final String KEY_FINGERPRINT = "fingerprint_enabled";
    private static final String KEYSTORE_NAME = "AndroidKeyStore";
    private static final String KEY_NAME = "HesabBourseFingerprintKey";

    private SharedPreferences prefs;
    private EditText pinInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        if (!hasPin()) {
            showCreatePinScreen();
        } else {
            showLockScreen();
        }
    }

    // ---------------------------------------------------------
    // ساخت PIN
    // ---------------------------------------------------------

    private void showCreatePinScreen() {

        LinearLayout root = createRoot();

        TextView title = new TextView(this);
        title.setText("🔐 قفل برنامه");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 30, 0, 20);

        TextView info = new TextView(this);
        info.setText(
                "برای ورود به حساب بورس یک رمز ۶ رقمی تعیین کنید."
        );
        info.setTextSize(17);
        info.setGravity(Gravity.CENTER);
        info.setPadding(20, 10, 20, 30);

        EditText pin = createPinField();
        pin.setHint("رمز ۶ رقمی");

        Button save = new Button(this);
        save.setText("ذخیره رمز و ورود");

        root.addView(title);
        root.addView(info);
        root.addView(pin);
        root.addView(save);

        setContentView(root);

        save.setOnClickListener(v -> {

            String value = pin.getText().toString();

            if (value.length() != 6) {
                toast("رمز باید دقیقاً ۶ رقم باشد.");
                return;
            }

            savePin(value);

            showLockScreen();
        });
    }

    // ---------------------------------------------------------
    // صفحه ورود
    // ---------------------------------------------------------

    private void showLockScreen() {

        LinearLayout root = createRoot();

        TextView title = new TextView(this);
        title.setText("🔐 ورود به حساب بورس");
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 30, 0, 25);

        TextView info = new TextView(this);
        info.setText("رمز ۶ رقمی خود را وارد کنید");
        info.setTextSize(17);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 10, 0, 20);

        pinInput = createPinField();
        pinInput.setHint("رمز ورود");

        Button login = new Button(this);
        login.setText("ورود");

        Button fingerprint = new Button(this);
        fingerprint.setText("👆 ورود با اثر انگشت");

        root.addView(title);
        root.addView(info);
        root.addView(pinInput);
        root.addView(login);

        if (isFingerprintAvailable()) {
            root.addView(fingerprint);

            fingerprint.setOnClickListener(v -> authenticateFingerprint());
        }

        setContentView(root);

        login.setOnClickListener(v -> {

            String entered = pinInput.getText().toString();

            if (verifyPin(entered)) {
                openMainActivity();
            } else {
                toast("رمز اشتباه است.");
                pinInput.setText("");
            }
        });
    }

    // ---------------------------------------------------------
    // PIN
    // ---------------------------------------------------------

    private boolean hasPin() {
        return prefs.contains(KEY_PIN_HASH)
                && prefs.contains(KEY_PIN_SALT);
    }

    private void savePin(String pin) {

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        String saltHex = bytesToHex(salt);
        String hash = hashPin(pin, saltHex);

        prefs.edit()
                .putString(KEY_PIN_HASH, hash)
                .putString(KEY_PIN_SALT, saltHex)
                .putBoolean(KEY_FINGERPRINT, true)
                .apply();
    }

    private boolean verifyPin(String pin) {

        if (pin == null || pin.length() != 6) {
            return false;
        }

        String salt = prefs.getString(KEY_PIN_SALT, "");
        String savedHash = prefs.getString(KEY_PIN_HASH, "");

        if (salt.isEmpty() || savedHash.isEmpty()) {
            return false;
        }

        String enteredHash = hashPin(pin, salt);

        return savedHash.equals(enteredHash);
    }

    private String hashPin(String pin, String saltHex) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            String input = saltHex + ":" + pin;

            byte[] result =
                    digest.digest(input.getBytes(StandardCharsets.UTF_8));

            return bytesToHex(result);

        } catch (Exception e) {
            return "";
        }
    }

    private String bytesToHex(byte[] bytes) {

        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    // ---------------------------------------------------------
    // اثر انگشت
    // ---------------------------------------------------------

    private boolean isFingerprintAvailable() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }

        try {

            FingerprintManager manager =
                    (FingerprintManager)
                            getSystemService(FINGERPRINT_SERVICE);

            if (manager == null) {
                return false;
            }

            if (!manager.isHardwareDetected()) {
                return false;
            }

            if (!manager.hasEnrolledFingerprints()) {
                return false;
            }

            return true;

        } catch (SecurityException e) {
            return false;
        }
    }

    private void authenticateFingerprint() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            toast("اثر انگشت در این نسخه اندروید پشتیبانی نمی‌شود.");
            return;
        }

        try {

            KeyStore keyStore =
                    KeyStore.getInstance(KEYSTORE_NAME);

            keyStore.load(null);

            if (!keyStore.containsAlias(KEY_NAME)) {
                createFingerprintKey();
            }

            SecretKey key =
                    (SecretKey) keyStore.getKey(KEY_NAME, null);

            Cipher cipher =
                    Cipher.getInstance(
                            KeyProperties.KEY_ALGORITHM_AES
                                    + "/"
                                    + KeyProperties.BLOCK_MODE_CBC
                                    + "/"
                                    + KeyProperties.ENCRYPTION_PADDING_PKCS7
                    );

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    key
            );

            FingerprintManager manager =
                    (FingerprintManager)
                            getSystemService(FINGERPRINT_SERVICE);

            FingerprintManager.CryptoObject cryptoObject =
                    new FingerprintManager.CryptoObject(cipher);

            manager.authenticate(
                    cryptoObject,
                    null,
                    0,
                    new FingerprintManager.AuthenticationCallback() {

                        @Override
                        public void onAuthenticationSucceeded(
                                FingerprintManager.AuthenticationResult result) {

                            runOnUiThread(() -> {
                                openMainActivity();
                            });
                        }

                        @Override
                        public void onAuthenticationFailed() {

                            runOnUiThread(() ->
                                    toast("اثر انگشت شناسایی نشد."));
                        }

                        @Override
                        public void onAuthenticationError(
                                int errorCode,
                                CharSequence errString) {

                            runOnUiThread(() ->
                                    toast(errString.toString()));
                        }
                    },
                    null
            );

        } catch (Exception e) {

            toast("ورود با اثر انگشت در این دستگاه فعال نشد.");
        }
    }

    private void createFingerprintKey() throws Exception {

        KeyGenerator keyGenerator =
                KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        KEYSTORE_NAME
                );

        KeyGenParameterSpec spec =
                new KeyGenParameterSpec.Builder(
                        KEY_NAME,
                        KeyProperties.PURPOSE_ENCRYPT
                                | KeyProperties.PURPOSE_DECRYPT
                )
                        .setBlockModes(
                                KeyProperties.BLOCK_MODE_CBC
                        )
                        .setEncryptionPaddings(
                                KeyProperties.ENCRYPTION_PADDING_PKCS7
                        )
                        .setUserAuthenticationRequired(true)
                        .build();

        keyGenerator.init(spec);
        keyGenerator.generateKey();
    }

    // ---------------------------------------------------------
    // ورود به برنامه
    // ---------------------------------------------------------

    private void openMainActivity() {

        Intent intent =
                new Intent(
                        LockActivity.this,
                        MainActivity.class
                );

        startActivity(intent);
        finish();
    }

    // ---------------------------------------------------------
    // UI
    // ---------------------------------------------------------

    private LinearLayout createRoot() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setGravity(Gravity.CENTER_HORIZONTAL);

        int padding = dp(24);

        root.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        return root;
    }

    private EditText createPinField() {

        EditText field =
                new EditText(this);

        field.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_VARIATION_PASSWORD
        );

        field.setTextSize(22);
        field.setGravity(Gravity.CENTER);
        field.setSingleLine(true);
        field.setMaxLength(6);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(60)
                );

        params.setMargins(
                0,
                dp(10),
                0,
                dp(20)
        );

        field.setLayoutParams(params);

        return field;
    }

    private int dp(int value) {

        return (int)
                (value *
                        getResources()
                                .getDisplayMetrics()
                                .density
                        + 0.5f);
    }

    private void toast(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }
}
