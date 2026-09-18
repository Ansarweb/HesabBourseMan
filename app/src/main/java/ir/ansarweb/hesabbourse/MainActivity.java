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
    private SpeechRecognizer speechRecognizer;

    private static final int AUDIO_PERMISSION = 9002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = new DatabaseHelper(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText("حساب بورس");
        title.setTextSize(26);
        title.setPadding(0, 0, 0, 24);
        root.addView(title);

        TextView addTrade = new TextView(this);
        addTrade.setText("➕ ثبت معامله");
        addTrade.setTextSize(20);
        addTrade.setPadding(0, 20, 0, 20);
        root.addView(addTrade);

        TextView addDeposit = new TextView(this);
        addDeposit.setText("💰 ثبت واریز");
        addDeposit.setTextSize(20);
        addDeposit.setPadding(0, 20, 0, 20);
        root.addView(addDeposit);

        TextView addWithdraw = new TextView(this);
        addWithdraw.setText("💸 ثبت برداشت");
        addWithdraw.setTextSize(20);
        addWithdraw.setPadding(0, 20, 0, 20);
        root.addView(addWithdraw);

        TextView voiceButton = new TextView(this);
        voiceButton.setText("🎤 ثبت معامله با صدا");
        voiceButton.setTextSize(20);
        voiceButton.setPadding(0, 20, 0, 20);
        root.addView(voiceButton);

        TextView listTitle = new TextView(this);
        listTitle.setText("آخرین تراکنش‌ها");
        listTitle.setTextSize(21);
        listTitle.setPadding(0, 30, 0, 10);
        root.addView(listTitle);

        transactions = new TextView(this);
        transactions.setTextSize(16);
        root.addView(transactions);

        setContentView(root);

        addTrade.setOnClickListener(v -> showTradeDialog());
        addDeposit.setOnClickListener(v -> showCashDialog("DEPOSIT"));
        addWithdraw.setOnClickListener(v -> showCashDialog("WITHDRAW"));
        voiceButton.setOnClickListener(v -> startVoiceInput());

        refreshTransactions();
    }

    private void showTradeDialog() {

        LinearLayout form = createForm();

        EditText portfolio = field("سبد / پرتفوی", "اصلی");
        EditText broker = field("کارگزاری", "");
        EditText symbol = field("نماد", "");
        EditText quantity = field("تعداد", "0");
        EditText price = field("قیمت هر سهم", "0");
        EditText fee = field("کارمزد", "0");

        form.addView(portfolio);
        form.addView(broker);
        form.addView(symbol);
        form.addView(quantity);
        form.addView(price);
        form.addView(fee);

        new AlertDialog.Builder(this)
                .setTitle("ثبت معامله خرید")
                .setView(form)
                .setPositiveButton("ثبت", (dialog, which) -> {

                    saveTrade(
                            "BUY",
                            portfolio.getText().toString(),
                            broker.getText().toString(),
                            symbol.getText().toString(),
                            quantity.getText().toString(),
                            price.getText().toString(),
                            fee.getText().toString()
                    );
                })
                .setNegativeButton("انصراف", null)
                .show();
    }

    private void saveTrade(
            String type,
            String portfolio,
            String broker,
            String symbol,
            String quantityText,
            String priceText,
            String feeText) {

        try {

            double quantity = normalizeNumber(quantityText);
            double price = normalizeNumber(priceText);
            double fee = normalizeNumber(feeText);

            double amount = quantity * price;

            long result = database.addTransaction(
                    type,
                    portfolio,
                    broker,
                    symbol,
                    quantity,
                    price,
                    fee,
                    amount
            );

            if (result != -1) {
                Toast.makeText(
                        this,
                        "معامله با موفقیت ثبت شد",
                        Toast.LENGTH_SHORT
                ).show();

                refreshTransactions();

            } else {

                Toast.makeText(
                        this,
                        "ثبت معامله ناموفق بود",
                        Toast.LENGTH_SHORT
                ).show();
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "خطا در ثبت معامله: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void showCashDialog(String type) {

        LinearLayout form = createForm();

        EditText portfolio = field("سبد / پرتفوی", "اصلی");
        EditText amount = field("مبلغ", "0");

        form.addView(portfolio);
        form.addView(amount);

        String title = type.equals("DEPOSIT")
                ? "ثبت واریز"
                : "ثبت برداشت";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(form)
                .setPositiveButton("ثبت", (dialog, which) -> {

                    try {

                        double value =
                                normalizeNumber(amount.getText().toString());

                        long result = database.addTransaction(
                                type,
                                portfolio.getText().toString(),
                                "",
                                "",
                                0,
                                0,
                                0,
                                value
                        );

                        if (result != -1) {

                            Toast.makeText(
                                    this,
                                    "با موفقیت ثبت شد",
                                    Toast.LENGTH_SHORT
                            ).show();

                            refreshTransactions();
                        }

                    } catch (Exception e) {

                        Toast.makeText(
                                this,
                                "خطا در ثبت اطلاعات",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .setNegativeButton("انصراف", null)
                .show();
    }

    private LinearLayout createForm() {

        LinearLayout form = new LinearLayout(this);

        form.setOrientation(LinearLayout.VERTICAL);

        form.setPadding(
                40,
                10,
                40,
                10
        );

        return form;
    }

    private EditText field(String hint, String value) {

        EditText editText = new EditText(this);

        editText.setHint(hint);
        editText.setText(value);

        editText.setSingleLine(true);

        return editText;
    }

    private void startVoiceInput() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.RECORD_AUDIO
                        },
                        AUDIO_PERMISSION
                );

                return;
            }
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
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        speechRecognizer.setRecognitionListener(
                new RecognitionListener() {

                    @Override
                    public void onReadyForSpeech(Bundle params) {

                        Toast.makeText(
                                MainActivity.this,
                                "صحبت کنید...",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onBeginningOfSpeech() {
                    }

                    @Override
                    public void onRmsChanged(float rmsdB) {
                    }

                    @Override
                    public void onBufferReceived(byte[] buffer) {
                    }

                    @Override
                    public void onEndOfSpeech() {
                    }

                    @Override
                    public void onError(int error) {

                        Toast.makeText(
                                MainActivity.this,
                                "تشخیص صدا انجام نشد",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onResults(Bundle results) {

                        ArrayList<String> matches =
                                results.getStringArrayList(
                                        RecognizerIntent.EXTRA_RESULTS
                                );

                        if (matches != null
                                && !matches.isEmpty()) {

                            processVoiceTrade(matches.get(0));
                        }
                    }

                    @Override
                    public void onPartialResults(Bundle partialResults) {
                    }

                    @Override
                    public void onEvent(
                            int eventType,
                            Bundle params) {
                    }
                }
        );

        Intent intent =
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "fa-IR"
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "fa-IR"
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                5
        );

        intent.putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false
        );

        speechRecognizer.startListening(intent);
    }

    private void processVoiceTrade(String text) {

        if (text == null || text.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "صدایی تشخیص داده نشد",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        VoiceTradeParser.TradeData data =
                VoiceTradeParser.parse(text);

        if (data.symbol == null
                || data.symbol.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "نماد سهم تشخیص داده نشد",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String type = data.isBuy
                ? "BUY"
                : "SELL";

        long result = database.addTransaction(
                type,
                data.portfolio,
                data.broker,
                data.symbol,
                data.quantity,
                data.price,
                0,
                data.amount
        );

        if (result != -1) {

            Toast.makeText(
                    this,
                    "ثبت شد: "
                            + data.portfolio
                            + " / "
                            + data.symbol
                            + " / "
                            + formatNumber(data.quantity),
                    Toast.LENGTH_LONG
            ).show();

            refreshTransactions();

        } else {

            Toast.makeText(
                    this,
                    "ثبت معامله ناموفق بود",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == AUDIO_PERMISSION) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                startSpeechRecognizer();

            } else {

                Toast.makeText(
                        this,
                        "اجازه استفاده از میکروفون داده نشد",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private double normalizeNumber(String value) {

        if (value == null || value.trim().isEmpty()) {
            return 0;
        }

        String text = value
                .replace("۰", "0")
                .replace("۱", "1")
                .replace("۲", "2")
                .replace("۳", "3")
                .replace("۴", "4")
                .replace("۵", "5")
                .replace("۶", "6")
                .replace("۷", "7")
                .replace("۸", "8")
                .replace("۹", "9")
                .replace("٬", "")
                .replace(",", "")
                .replace("،", "")
                .trim();

        return Double.parseDouble(text);
    }

    private String formatNumber(double value) {

        if (value == (long) value) {
            return String.valueOf((long) value);
        }

        return String.valueOf(value);
    }

    private void refreshTransactions() {

        if (transactions == null) {
            return;
        }

        StringBuilder text = new StringBuilder();

        android.database.Cursor cursor =
                database.getAllTransactions();

        try {

            int count = 0;

            while (cursor.moveToNext()) {

                count++;

                String type =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow("type")
                        );

                String portfolio =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow("portfolio")
                        );

                String broker =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow("broker")
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

                double amount =
                        cursor.getDouble(
                                cursor.getColumnIndexOrThrow("amount")
                        );

                String action;

                if ("BUY".equals(type)) {
                    action = "خرید";
                } else if ("SELL".equals(type)) {
                    action = "فروش";
                } else if ("DEPOSIT".equals(type)) {
                    action = "واریز";
                } else if ("WITHDRAW".equals(type)) {
                    action = "برداشت";
                } else {
                    action = type;
                }

                text.append(count)
                        .append(") ")
                        .append(action)
                        .append(" | ");

                if (portfolio != null
                        && !portfolio.trim().isEmpty()) {

                    text.append("سبد: ")
                            .append(portfolio)
                            .append(" | ");
                }

                if (broker != null
                        && !broker.trim().isEmpty()) {

                    text.append("کارگزاری: ")
                            .append(broker)
                            .append(" | ");
                }

                if (symbol != null
                        && !symbol.trim().isEmpty()) {

                    text.append(symbol)
                            .append(" | ");
                }

                if (quantity > 0) {

                    text.append("تعداد: ")
                            .append(formatNumber(quantity))
                            .append(" | ");
                }

                if (price > 0) {

                    text.append("قیمت: ")
                            .append(formatNumber(price))
                            .append(" | ");
                }

                if (amount > 0) {

                    text.append("مبلغ: ")
                            .append(formatNumber(amount));
                }

                text.append("\n\n");
            }

            if (count == 0) {
                text.append("هنوز تراکنشی ثبت نشده است.");
            }

        } finally {

            cursor.close();
        }

        transactions.setText(text.toString());
    }

    @Override
    protected void onDestroy() {

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }

        if (database != null) {
            database.close();
        }

        super.onDestroy();
    }
}
