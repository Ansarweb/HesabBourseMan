package ir.ansarweb.hesabbourse;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private DatabaseHelper database;

    private TextView transactions;

    private static final int VOICE_REQUEST = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        database =
                new DatabaseHelper(this);

        transactions =
                findViewById(R.id.transactions);

        findViewById(R.id.addTrade)
                .setOnClickListener(v ->
                        showTradeDialog());

        findViewById(R.id.addDeposit)
                .setOnClickListener(v ->
                        showCashDialog("DEPOSIT"));

        findViewById(R.id.addWithdraw)
                .setOnClickListener(v ->
                        showCashDialog("WITHDRAW"));

        findViewById(R.id.voiceButton)
                .setOnClickListener(v ->
                        startVoiceInput());

        refreshTransactions();
    }

    private void showTradeDialog() {

        LinearLayout layout =
                createForm();

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
                                    broker,
                                    symbol,
                                    quantity,
                                    price,
                                    fee
                                )
                )
                .setNegativeButton(
                        "لغو",
                        null
                )
                .show();
    }

    private void saveTrade(
            String type,
            EditText broker,
            EditText symbol,
            EditText quantity,
            EditText price,
            EditText fee) {

        try {

            double q =
                    Double.parseDouble(
                        quantity.getText()
                                .toString()
                                .trim()
                    );

            double p =
                    Double.parseDouble(
                        price.getText()
                                .toString()
                                .trim()
                    );

            double f = 0;

            if (!fee.getText()
                    .toString()
                    .trim()
                    .isEmpty()) {

                f =
                    Double.parseDouble(
                        fee.getText()
                                .toString()
                                .trim()
                    );
            }

            database.addTransaction(
                    type,
                    broker.getText().toString(),
                    symbol.getText().toString(),
                    q,
                    p,
                    f,
                    (q * p) + f
            );

            refreshTransactions();

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
                                        amount.getText()
                                            .toString()
                                            .trim()
                                    );

                                database.addTransaction(
                                    type,
                                    "",
                                    "",
                                    0,
                                    0,
                                    0,
                                    value
                                );

                                refreshTransactions();

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

        Intent intent =
                new Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "fa-IR"
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "مثلاً: مفید خرید ومعادن ۱۲۰ میلیون تومان هر سهم ۲۹۰ تومان"
        );

        try {

            startActivityForResult(
                    intent,
                    VOICE_REQUEST
            );

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "تشخیص گفتار در دسترس نیست",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == VOICE_REQUEST &&
                resultCode == RESULT_OK &&
                data != null) {

            ArrayList<String> results =
                    data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS
                    );

            if (results != null &&
                    !results.isEmpty()) {

                Toast.makeText(
                    this,
                    "شنیدم: " + results.get(0),
                    Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void refreshTransactions() {

        StringBuilder text =
                new StringBuilder();

        android.database.Cursor cursor =
                database.getAllTransactions();

        int count = 0;

        while (cursor.moveToNext() &&
                count < 15) {

            String type =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow("type")
                    );

            String symbol =
                    cursor.getString(
                        cursor.getColumnIndexOrThrow("symbol")
                    );

            double quantity =
                    cursor.getDouble(
                        cursor.getColumnIndexOrThrow("quantity")
                    );

            double price =
                    cursor.getDouble(
                        cursor.getColumnIndexOrThrow("price")
                    );

            String title;

            if ("BUY".equals(type)) {
                title = "خرید";
            } else if ("SELL".equals(type)) {
                title = "فروش";
            } else if ("DEPOSIT".equals(type)) {
                title = "واریز";
            } else {
                title = "برداشت";
            }

            text.append(title);

            if (symbol != null &&
                    !symbol.isEmpty()) {

                text.append(" | ")
                    .append(symbol)
                    .append(" | تعداد ")
                    .append(quantity)
                    .append(" | قیمت ")
                    .append(price);
            }

            text.append("\n");

            count++;
        }

        cursor.close();

        if (count == 0) {

            transactions.setText(
                "هنوز معامله‌ای ثبت نشده است."
            );

        } else {

            transactions.setText(
                text.toString()
            );
        }
    }
}
