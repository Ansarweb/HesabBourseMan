package ir.ansarweb.hesabbourse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class LockActivity extends Activity {

    private EditText pinInput;
    private android.content.SharedPreferences prefs;

    private static final String PREFS = "app_lock";
    private static final String PIN = "pin";
    private static final String BIOMETRIC = "biometric_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // اگر هنوز PIN تنظیم نشده، مستقیماً وارد تنظیم PIN شو
        if (!prefs.contains(PIN)) {
            showSetupScreen();
        } else {
            showLockScreen();
        }
    }

    private void showSetupScreen() {

        LinearLayout layout = createLayout();

        TextView title = new TextView(this);
        title.setText("🔐 امنیت حساب بورس");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 30, 0, 30);

        TextView info = new TextView(this);
        info.setText("برای ورود به برنامه یک رمز ۶ رقمی تعیین کنید.");
        info.setTextSize(17);
        info.setGravity(Gravity.CENTER);
        info.setPadding(20, 20, 20, 30);

        EditText pin = createPinField();
        pin.setHint("رمز ۶ رقمی");

        EditText confirm = createPinField();
        confirm.setHint("تکرار رمز");

        Button save = new Button(this);
        save.setText("ذخیره رمز");

        save.setOnClickListener(v -> {

            String p1 = pin.getText().toString();
            String p2 = confirm.getText().toString();

            if (p1.length() != 6) {
                toast("رمز باید ۶ رقمی باشد");
                return;
            }

            if (!p1.equals(p2)) {
                toast("دو رمز یکسان نیستند");
                return;
            }

            prefs.edit()
                    .putString(PIN, p1)
                    .putBoolean(BIOMETRIC, false)
                    .apply();

            showLockScreen();
        });

        layout.addView(title);
        layout.addView(info);
        layout.addView(pin);
        layout.addView(confirm);
        layout.addView(save);

        setContentView(layout);
    }

    private void showLockScreen() {

        LinearLayout layout = createLayout();

        TextView title = new TextView(this);
        title.setText("🔐 ورود به حساب بورس");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 40, 0, 30);

        pinInput = createPinField();
        pinInput.setHint("رمز ورود");

        Button login = new Button(this);
        login.setText("ورود");

        login.setOnClickListener(v -> checkPin());

        Button biometric = new Button(this);
        biometric.setText("👆 ورود با اثر انگشت");

        biometric.setOnClickListener(v -> authenticateBiometric());

        layout.addView(title);
        layout.addView(pinInput);
        layout.addView(login);
        layout.addView(biometric);

        setContentView(layout);

        // اگر قبلاً اثر انگشت فعال شده باشد، خودکار پیشنهاد بده
        if (prefs.getBoolean(BIOMETRIC, false)) {
            pinInput.postDelayed(this::authenticateBiometric, 400);
        }
    }

    private void checkPin() {

        String entered = pinInput.getText().toString();
        String saved = prefs.getString(PIN, "");

        if (entered.equals(saved)) {
            openMainActivity();
        } else {
            toast("رمز اشتباه است");
            pinInput.setText("");
        }
    }

    private void authenticateBiometric() {

        BiometricManager manager = BiometricManager.from(this);

        int result = manager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.BIOMETRIC_WEAK
        );

        if (result != BiometricManager.BIOMETRIC_SUCCESS) {
            toast("اثر انگشت روی این دستگاه در دسترس نیست");
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);

        BiometricPrompt prompt =
                new BiometricPrompt(
                        this,
                        executor,
                        new BiometricPrompt.AuthenticationCallback() {

                            @Override
                            public void onAuthenticationSucceeded(
                                    BiometricPrompt.AuthenticationResult result) {

                                super.onAuthenticationSucceeded(result);
                                openMainActivity();
                            }

                            @Override
                            public void onAuthenticationFailed() {
                                super.onAuthenticationFailed();
                                toast("اثر انگشت شناسایی نشد");
                            }

                            @Override
                            public void onAuthenticationError(
                                    int errorCode,
                                    CharSequence errString) {

                                super.onAuthenticationError(
                                        errorCode,
                                        errString
                                );

                                toast(errString.toString());
                            }
                        }
                );

        BiometricPrompt.PromptInfo promptInfo =
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("ورود به حساب بورس")
                        .setSubtitle("اثر انگشت خود را وارد کنید")
                        .setNegativeButtonText("ورود با رمز")
                        .build();

        prompt.authenticate(promptInfo);
    }

    private void openMainActivity() {

        Intent intent = new Intent(
                LockActivity.this,
                MainActivity.class
        );

        startActivity(intent);
        finish();
    }

    private LinearLayout createLayout() {

        LinearLayout layout = new LinearLayout(this);

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(50, 40, 50, 40);

        return layout;
    }

    private EditText createPinField() {

        EditText edit = new EditText(this);

        edit.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_VARIATION_PASSWORD
        );

        edit.setGravity(Gravity.CENTER);
        edit.setTextSize(20);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 15, 0, 15);

        edit.setLayoutParams(params);

        return edit;
    }

    private void toast(String text) {
        Toast.makeText(
                this,
                text,
                Toast.LENGTH_SHORT
        ).show();
    }
}
