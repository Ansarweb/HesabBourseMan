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

    private static final int VOICE_REQUEST = 9001;
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

        EditText portfolio = field("نام سبد - مثلاً محمد");
        EditText broker = field("کارگزاری");
        EditText symbol = field("نماد");
        EditText quantity = field("تعداد");
        EditText price = field("قیمت هر سهم");
        EditText fee = field("کارمزد");

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

            double q = Double.parseDouble(
                    normalizeNumber(
                            quantity.getText().toString().trim()
                    )
            );

            double p = Double.parseDouble(
                    normalizeNumber(
                            price.getText().toString().trim()
                    )
            );

            double f = 0;

            String feeText =
                    fee.getText().toString().trim();

            if (!feeText.isEmpty()) {

                f = Double.parseDouble(
                        normalizeNumber(feeText)
                );
            }

            String portfolioText =
                    portfolio.getText().toString().trim();

            String brokerText =
                    broker.getText().toString().trim();

            String symbolText =
                    symbol.getText().toString().trim();

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
       
