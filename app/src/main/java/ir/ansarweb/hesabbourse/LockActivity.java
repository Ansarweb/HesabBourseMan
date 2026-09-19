package ir.ansarweb.hesabbourse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.InputFilter;
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

public class LockActivity extends Activity {

    private static final String PREFS = "app_lock";
    private static final String PIN_HASH = "pin_hash";
    private static final String PIN_SALT = "pin_salt";
    private static final String KEY_NAME = "HesabBourseFingerprintKey";

    private SharedPreferences prefs;

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

    private boolean hasPin() {
        return prefs.contains(PIN_HASH) && prefs.contains(PIN_SALT);
    }

    private void showCreatePinScreen() {

        LinearLayout layout = createBaseLayout();

        TextView title = new TextView(this);
        title.setText("🔐 حساب بورس");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 20, 0, 20);
        layout.addView(title);

        TextView info = new TextView(this);
        info.setText("برای ورود یک رمز ۶ رقمی تعیین کنید");
        info.setTextSize(17);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 0, 0, 30);
        layout.addView(info);

        EditText pin1 = createPinField();
        pin1.setHint("رمز ۶ رقمی");
        layout.addView(pin1);

        EditText pin2 = createPinField();
        pin2.setHint("تکرار رمز");
        layout.addView(pin2);

        Button save = new Button(this);
        save.setText("ذخیره رمز و ورود");
        layout.addView(save);

        save.setOnClickListener(v -> {

            String p1 = pin1.getText().toString();
            String p2 = pin2.getText().toString();

            if (p1.length() != 6) {
                toast("رمز باید دقیقاً ۶ رقم باشد");
                return;
            }

            if (!p1.equals(p2)) {
                toast("دو رمز یکسان نیستند");
                return;
            }

            savePin(p1);

            toast("رمز با موفقیت ذخیره شد");

            openMainActivity();
        });

        setContentView(layout);
    }

    private void showLockScreen() {

        LinearLayout layout = createBaseLayout();

        TextView title = new TextView(this);
        title.setText("🔒 حساب بورس");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 20, 0, 20);
        layout.addView(title);

        TextView info = new TextView(this);
        info.setText("برای ورود رمز ۶ رقمی را وارد کنید");
        info.setTextSize(17);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 0, 0, 30);
        layout.addView(info);

        EditText pin = createPinField();
        pin.setHint("رمز ورود");
        layout.addView(pin);

        Button login = new Button(this);
        login.setText("ورود");
        layout.addView(login);

        login.setOnClickListener(v -> {

            String entered = pin.getText().toString();

            if (entered.length() != 6) {
                toast("رمز باید ۶ رقم باشد");
                return;
            }

            if (verifyPin(entered)) {
                openMainActivity();
            } else {
                pin.setText("");
                toast("رمز اشتباه است");
            }
        });

        if (isFingerprintAvailable()) {

            Button fingerprint = new Button(this);
            fingerprint.setText("👆 ورود با اثر انگشت");
            layout.addView(fingerprint);

            fingerprint.setOnClickListener(v -> startFingerprintAuthentication());
        }

        setContentView(layout);
    }

    private LinearLayout createBaseLayout() {

        LinearLayout layout = new LinearLayout(this);

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(50, 60, 50, 50);

        return layout;
    }

    private EditText createPinField() {

        EditText field = new EditText(this);

        field.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_VARIATION_PASSWORD
        );

        field.setGravity(Gravity.CENTER);
        field.setTextSize(22);

        field.setFilters(
                new InputFilter[]{
                        new InputFilter.LengthFilter(6)
                }
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 10, 0, 10);

        field.setLayoutParams(params);

        return field;
    }

    private void savePin(String pin) {

        try {

            SecureRandom random = new SecureRandom();

            byte[] salt = new byte[16];
            random.nextBytes(salt);

            String saltHex = bytesToHex(salt);

            String hash = hashPin(saltHex, pin);

            prefs.edit()
                    .putString(PIN_SALT, saltHex)
                    .putString(PIN_HASH, hash)
                    .apply();

        } catch (Exception e) {
            toast("خطا در ذخیره رمز");
        }
    }

    private boolean verifyPin(String pin) {

        try {

            String salt = prefs.getString(PIN_SALT, null);
            String savedHash = prefs.getString(PIN_HASH, null);

            if (salt == null || savedHash == null) {
                return false;
            }

            String enteredHash = hashPin(salt, pin);

            return MessageDigest.isEqual(
                    enteredHash.getBytes(StandardCharsets.UTF_8),
                    savedHash.getBytes(StandardCharsets.UTF_8)
            );

        } catch (Exception e) {
            return false;
        }
    }

    private String hashPin(String salt, String pin) throws Exception {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        String value = salt + ":" + pin;

        byte[] result =
                digest.digest(value.getBytes(StandardCharsets.UTF_8));

        return bytesToHex(result);
    }

    private String bytesToHex(byte[] bytes) {

        StringBuilder builder = new StringBuilder();

        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }

        return builder.toString();
    }

    private boolean isFingerprintAvailable() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }

        try {

            FingerprintManager manager =
                    (FingerprintManager)
                            getSystemService(Context.FINGERPRINT_SERVICE);

            if (manager == null) {
                return false;
            }

            return manager.isHardwareDetected()
                    && manager.hasEnrolledFingerprints();

        } catch (Exception e) {
            return false;
        }
    }

    private void startFingerprintAuthentication() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            toast("اثر انگشت در این نسخه اندروید پشتیبانی نمی‌شود");
            return;
        }

        try {

            FingerprintManager manager =
                    (FingerprintManager)
                            getSystemService(Context.FINGERPRINT_SERVICE);

            if (manager == null) {
                toast("سخت‌افزار اثر انگشت پیدا نشد");
                return;
            }

            if (!manager.isHardwareDetected()) {
                toast("این گوشی حسگر اثر انگشت ندارد");
                return;
            }

            if (!manager.hasEnrolledFingerprints()) {
                toast("ابتدا اثر انگشت را در تنظیمات گوشی ثبت کنید");
                return;
            }

            Cipher cipher = createFingerprintCipher();

            if (cipher == null) {
                toast("امکان فعال‌سازی اثر انگشت وجود ندارد");
                return;
            }

            FingerprintManager.CryptoObject cryptoObject =
                    new FingerprintManager.CryptoObject(cipher);

            FingerprintManager.AuthenticationCallback callback =
                    new FingerprintManager.AuthenticationCallback() {

                        @Override
                        public void onAuthenticationSucceeded(
                                FingerprintManager.AuthenticationResult result) {

                            runOnUiThread(() -> {
                                toast("اثر انگشت تأیید شد");
                                openMainActivity();
                            });
                        }

                        @Override
                        public void onAuthenticationFailed() {

                            runOnUiThread(() ->
                                    toast("اثر انگشت شناسایی نشد")
                            );
                        }

                        @Override
                        public void onAuthenticationError(
                                int errorCode,
                                CharSequence errString) {

                            runOnUiThread(() ->
                                    toast(errString.toString())
                            );
                        }
                    };

            manager.authenticate(
                    cryptoObject,
                    null,
                    0,
                    callback,
                    null
            );

        } catch (Exception e) {

            toast("خطا در فعال‌سازی اثر انگشت");
        }
    }

    private Cipher createFingerprintCipher() {

        try {

            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            if (!keyStore.containsAlias(KEY_NAME)) {

                KeyGenerator keyGenerator =
                        KeyGenerator.getInstance(
                                KeyProperties.KEY_ALGORITHM_AES,
                                "AndroidKeyStore"
                        );

                KeyGenParameterSpec spec =
                        new KeyGenParameterSpec.Builder(
                                KEY_NAME,
                                KeyProperties.PURPOSE_ENCRYPT |
                                KeyProperties.PURPOSE_DECRYPT
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

            SecretKey key =
                    ((KeyStore.SecretKeyEntry)
                            keyStore.getEntry(KEY_NAME, null))
                            .getSecretKey();

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

            return cipher;

        } catch (Exception e) {

            return null;
        }
    }

    private void openMainActivity() {

        Intent intent =
                new Intent(
                        LockActivity.this,
                        MainActivity.class
                );

        startActivity(intent);

        finish();
    }

    private void toast(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }
}
