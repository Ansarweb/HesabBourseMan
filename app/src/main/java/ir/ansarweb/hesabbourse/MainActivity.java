package ir.ansarweb.hesabbourse;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private DatabaseHelper database;
    private TextView transactions;

    private static final int AUDIO_PERMISSION = 9002;

    private SpeechRecognizer speechRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        database = new DatabaseHelper(this);

        transactions = findViewById(R.id.transactions);

        findViewById(R.id.addTrade)
                .setOnClickListener(v -> showTradeDialog());

        findViewById(R.id.addDeposit)
                .setOnClickListener(v -> showCashDialog("DEPOSIT"));

        findViewById(R.id.addWithdraw)
                .setOnClickListener(v -> showCashDialog("WITHDRAW"));

        findViewById(R.id.voiceButton)
                .setOnClickListener(v -> startVoiceInput());

        refreshTransactions();
    }

    private void showTradeDialog() {

        LinearLayout layout = createForm();

        EditText portfolio =
                field("نام سبد - مثلاً محمد");

        EditText broker =
                field("کارگزاری");

        EditText symbol =
                field("نماد");

        EditText quantity =
                field("تعداد");

        EditText price =
                field("قیمت هر سهم");

        EditText fee =
                field("کارمزد");

        layout.addView(portfolio);
        layout.addView(broker);
        layout.addView(symbol);
        layout.addView(quantity);
        layout.addView(price);
        layout.addView(fee);

        new AlertDialog.Builder(this)
                .setTitle("ثبت معامله")
                .setView(layout)
                .setPositiveButton(
                        "خرید",
                        (dialog, which) ->
                                saveTrade(
                                        "BUY",
                                        portfolio,
                                        broker,
                                        symbol,
                                        quantity,
                                        price,
                                        fee
                                )
                )
                .setNeutralButton(
                        "فروش",
                        (dialog, which) ->
                                saveTrade(
                                        "SELL",
                                        portfolio,
                                        broker,
                                        symbol,
                                        quantity,
                                        price,
                                        fee
                                )
                )
                .setNegativeButton("لغو", null)
                .show();
    }

    private void saveTrade(
            String type,
            EditText portfolio,
            EditText broker,
            EditText symbol,
            EditText quantity,
            EditText price,
            EditText fee) {

        try {

            double q =
                    Double.parseDouble(
                            normalizeNumber(
                                    quantity
                                            .getText()
                                            .toString()
                                            .trim()
                            )
                    );

            double p =
                    Double.parseDouble(
                            normalizeNumber(
                                    price
                                            .getText()
                                            .toString()
                                            .trim()
                            )
                    );

            double f = 0;

            String feeText =
                    fee.getText()
                            .toString()
                            .trim();

            if (!feeText.isEmpty()) {

                f =
                        Double.parseDouble(
                                normalizeNumber(feeText)
                        );
            }

            String portfolioText =
                    portfolio.getText()
                            .toString()
                            .trim();

            String brokerText =
                    broker.getText()
                            .toString()
                            .trim();

            String symbolText =
                    symbol.getText()
                            .toString()
                            .trim();

            if (q <= 0 ||
                    p <= 0 ||
                    symbolText.isEmpty()) {

                throw new Exception();
            }

            database.addTransaction(
                    type,
                    portfolioText,
                    brokerText,
                    symbolText,
                    q,
                    p,
                    f,
                    (q * p) + f
            );

            refreshTransactions();

            Toast.makeText(
                    this,
                    "معامله با موفقیت ثبت شد",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "اطلاعات معامله صحیح نیست",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void showCashDialog(String type) {

        EditText amount =
                field("مبلغ تومان");

        new AlertDialog.Builder(this)
                .setTitle(
                        type.equals("DEPOSIT")
                                ? "واریز به کارگزاری"
                                : "برداشت"
                )
                .setView(amount)
                .setPositiveButton(
                        "ثبت",
                        (dialog, which) -> {

                            try {

                                double value =
                                        Double.parseDouble(
                                                normalizeNumber(
                                                        amount
                                                                .getText()
                                                                .toString()
                                                                .trim()
                                                )
                                        );

                                if (value <= 0) {
                                    throw new Exception();
                                }

                                database.addTransaction(
                                        type,
                                        "اصلی",
                                        "",
                                        "",
                                        0,
                                        0,
                                        0,
                                        value
                                );

                                refreshTransactions();

                                Toast.makeText(
                                        this,
                                        "ثبت شد",
                                        Toast.LENGTH_SHORT
                                ).show();

                            } catch (Exception e) {

                                Toast.makeText(
                                        this,
                                        "مبلغ صحیح نیست",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .setNegativeButton(
                        "لغو",
                        null
                )
                .show();
    }

    private LinearLayout createForm() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                24,
                8,
                24,
                8
        );

        return layout;
    }

    private EditText field(String hint) {

        EditText editText =
                new EditText(this);

        editText.setHint(hint);
        editText.setSingleLine(true);

        return editText;
    }

    private void startVoiceInput() {

        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(
                        Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    AUDIO_PERMISSION
            );

            return;
        }

        startSpeechRecognizer();
    }

    private void startSpeechRecognizer() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {

            Toast.makeText(
                    this,
                    "تشخیص گفتار روی این گوشی در دسترس نیست",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (speechRecognizer != null) {

            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        if (Build.VERSION.SDK_INT >= 31 &&
                SpeechRecognizer
                        .isOnDeviceRecognitionAvailable(this)) {

            speechRecognizer =
                    SpeechRecognizer
                            .createOnDeviceSpeechRecognizer(this);

        } else {

            speechRecognizer =
                    SpeechRecognizer
                            .createSpeechRecognizer(this);
        }

        speechRecognizer.setRecognitionListener(
                new RecognitionListener() {

                    @Override
                    public void onReadyForSpeech(
                            Bundle params) {

                        Toast.makeText(
                                MainActivity.this,
                                "🎙 صحبت کنید...",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onBeginningOfSpeech() {
                    }

                    @Override
                    public void onRmsChanged(
                            float rmsdB) {
                    }

                    @Override
                    public void onBufferReceived(
                            byte[] buffer) {
                    }

                    @Override
                    public void onEndOfSpeech() {
                    }

                    @Override
                    public void onError(
                            int error) {

                        String message;

                        switch (error) {

                            case SpeechRecognizer.ERROR_AUDIO:
                                message = "خطا در میکروفون";
                                break;

                            case SpeechRecognizer.ERROR_NETWORK:
                                message = "
