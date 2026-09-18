package ir.ansarweb.hesabbourse;

import android.app.AlertDialog;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.database.Cursor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private DatabaseHelper db;

    private static final double BUY_FEE_RATE = 0.0037;
    private static final double SELL_FEE_RATE = 0.0088;

    private static final int REQUEST_EXPORT_BACKUP = 1001;
    private static final int REQUEST_IMPORT_BACKUP = 1002;

    /*
     * جلوگیری از اجرای دوباره TextWatcher
     * هنگام فرمت کردن عدد
     */
    private boolean formattingNumber = false;

    /*
     * جلوگیری از محاسبه همزمان فیلدها
     */
    private boolean calculatingFields = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DatabaseHelper(this);

        buildMainScreen();
    }

    private void buildMainScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText("حساب بورس");
        title.setTextSize(28);
        title.setPadding(0, 0, 0, 30);
        root.addView(title);

        Button dashboard = new Button(this);
        dashboard.setText("📊 وضعیت سبدها");
        dashboard.setOnClickListener(v -> showPortfolioDialog());
        root.addView(dashboard);

        Button trade = new Button(this);
        trade.setText("➕ ثبت خرید / فروش");
        trade.setOnClickListener(v -> showTradeDialog());
        root.addView(trade);

        Button deposit = new Button(this);
        deposit.setText("💰 ثبت واریزی");
        deposit.setOnClickListener(v -> showMoneyDialog("DEPOSIT"));
        root.addView(deposit);

        Button withdraw = new Button(this);
        withdraw.setText("💸 ثبت برداشت");
        withdraw.setOnClickListener(v -> showMoneyDialog("WITHDRAW"));
        root.addView(withdraw);

        Button history = new Button(this);
        history.setText("📋 تاریخچه معاملات");
        history.setOnClickListener(v -> showHistoryDialog());
        root.addView(history);

        Button search = new Button(this);
        search.setText("🔎 جستجو در معاملات");
        search.setOnClickListener(v -> showSearchDialog());
        root.addView(search);

        Button cash = new Button(this);
        cash.setText("💵 موجودی نقدی");
        cash.setOnClickListener(v -> showCashBalance());
        root.addView(cash);

        Button exportBackup = new Button(this);
        exportBackup.setText("💾 گرفتن بک‌آپ");
        exportBackup.setOnClickListener(v -> startBackupExport());
        root.addView(exportBackup);

        Button importBackup = new Button(this);
        importBackup.setText("📥 بازیابی / وارد کردن بک‌آپ");
        importBackup.setOnClickListener(v -> startBackupImport());
        root.addView(importBackup);

        setContentView(root);
    }

    // =========================================================
    // BACKUP / RESTORE
    // =========================================================

    private void startBackupExport() {

        String fileName =
                "hesab_bourse_backup_" +
                new SimpleDateFormat(
                        "yyyyMMdd_HHmmss",
                        Locale.US
                ).format(new Date()) +
                ".json";

        Intent intent =
                new Intent(Intent.ACTION_CREATE_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");

        intent.putExtra(
                Intent.EXTRA_TITLE,
                fileName
        );

        startActivityForResult(
                intent,
                REQUEST_EXPORT_BACKUP
        );
    }

    private void startBackupImport() {

        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);

        /*
         * بعضی گوشی‌ها فایل JSON را با MIME دیگری
         * نشان می‌دهند، بنابراین فقط JSON محدود نمی‌کنیم.
         */
        intent.setType("*/*");

        intent.putExtra(
                Intent.EXTRA_MIME_TYPES,
                new String[]{
                        "application/json",
                        "text/plain"
                }
        );

        startActivityForResult(
                intent,
                REQUEST_IMPORT_BACKUP
        );
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

        if (resultCode != RESULT_OK ||
                data == null ||
                data.getData() == null) {

            return;
        }

        Uri uri = data.getData();

        if (requestCode == REQUEST_EXPORT_BACKUP) {

            exportBackupToUri(uri);

        } else if (requestCode == REQUEST_IMPORT_BACKUP) {

            confirmBackupImport(uri);
        }
    }

    private void exportBackupToUri(Uri uri) {

        try {

            String json =
                    db.exportBackupJson();

            OutputStream outputStream =
                    getContentResolver()
                            .openOutputStream(uri);

            if (outputStream == null) {
                throw new Exception(
                        "امکان باز کردن فایل وجود ندارد."
                );
            }

            Writer writer =
                    new OutputStreamWriter(
                            outputStream,
                            StandardCharsets.UTF_8
                    );

            writer.write(json);
            writer.flush();
            writer.close();

            toastLong(
                    "✅ بک‌آپ با موفقیت ذخیره شد."
            );

        } catch (Exception e) {

            toastLong(
                    "❌ گرفتن بک‌آپ ناموفق بود.\n" +
                    safe(e.getMessage())
            );
        }
    }

    private void confirmBackupImport(Uri uri) {

        new AlertDialog.Builder(this)
                .setTitle("⚠️ بازیابی بک‌آپ")
                .setMessage(
                        "با بازیابی این فایل، " +
                        "تراکنش‌های فعلی برنامه حذف و " +
                        "اطلاعات داخل بک‌آپ جایگزین می‌شوند.\n\n" +
                        "آیا مطمئن هستید؟"
                )
                .setNegativeButton(
                        "انصراف",
                        null
                )
                .setPositiveButton(
                        "بله، بازیابی کن",
                        (dialog, which) ->
                                importBackupFromUri(uri)
                )
                .show();
    }

    private void importBackupFromUri(Uri uri) {

        try {

            InputStream inputStream =
                    getContentResolver()
                            .openInputStream(uri);

            if (inputStream == null) {
                throw new Exception(
                        "امکان باز کردن فایل وجود ندارد."
                );
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    inputStream,
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder json =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            reader.close();

            if (json.length() == 0) {
                throw new Exception(
                        "فایل بک‌آپ خالی است."
                );
            }

            db.importBackupJson(
                    json.toString()
            );

            toastLong(
                    "✅ بازیابی بک‌آپ با موفقیت انجام شد."
            );

            buildMainScreen();

        } catch (Exception e) {

            toastLong(
                    "❌ بازیابی بک‌آپ انجام نشد.\n" +
                    safe(e.getMessage())
            );
        }
    }

    // =========================================================
    // BASIC UI
    // =========================================================

    private EditText field(String hint) {

        EditText e = new EditText(this);

        e.setHint(hint);
        e.setSingleLine(true);
        e.setPadding(16, 12, 16, 12);

        return e;
    }

    /*
     * فیلد عددی با جداکننده هزارگان
     */
    private EditText numberField(String hint) {

        EditText e = field(hint);

        addThousandsFormatter(e);

        return e;
    }

    // =========================================================
    // TRADE
    // =========================================================

    private void showTradeDialog() {

        LinearLayout box = new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                20, 10, 20, 10
        );

        EditText portfolio =
                field("سبد / پرتفوی");

        portfolio.setText("اصلی");

        EditText broker =
                field("کارگزاری");

        EditText symbol =
                field("نماد");

        EditText quantity =
                numberField("تعداد سهم");

        EditText price =
                numberField("قیمت هر سهم");

        EditText totalAmount =
                numberField("مبلغ کل معامله");

        EditText description =
                field("توضیحات");

        box.addView(portfolio);
        box.addView(broker);
        box.addView(symbol);
        box.addView(quantity);
        box.addView(price);
        box.addView(totalAmount);
        box.addView(description);

        TextView info =
                new TextView(this);

        info.setText(
                "\nتعداد، قیمت هر سهم و مبلغ کل می‌توانند " +
                "به‌صورت خودکار محاسبه شوند.\n" +
                "جداکننده هزارگان هنگام ورود عدد فعال است.\n" +
                "کارمزد جداگانه محاسبه می‌شود."
        );

        info.setPadding(
                0, 10, 0, 10
        );

        box.addView(info);

        addAutoCalculation(
                quantity,
                price,
                totalAmount
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("ثبت خرید / فروش")
                        .setView(box)
                        .setNegativeButton(
                                "انصراف",
                                null
                        )
                        .setPositiveButton(
                                "خرید",
                                null
                        )
                        .setNeutralButton(
                                "فروش",
                                null
                        )
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                if (saveTrade(
                        "BUY",
                        portfolio,
                        broker,
                        symbol,
                        quantity,
                        price,
                        totalAmount,
                        description
                )) {

                    dialog.dismiss();
                }
            });

            dialog.getButton(
                    AlertDialog.BUTTON_NEUTRAL
            ).setOnClickListener(v -> {

                if (saveTrade(
                        "SELL",
                        portfolio,
                        broker,
                        symbol,
                        quantity,
                        price,
                        totalAmount,
                        description
                )) {

                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    // =========================================================
    // NUMBER FORMATTING
    // =========================================================

    /*
     * جداکننده هزارگان همزمان با تایپ
     *
     * نکته مهم:
     * این Watcher هیچ محاسبه دیگری انجام نمی‌دهد.
     * بنابراین هنگام تایپ عدد بزرگ، حلقه ایجاد نمی‌شود.
     */
    private void addThousandsFormatter(
            EditText editText) {

        editText.addTextChangedListener(
                new TextWatcher() {

                    private String previous = "";

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {

                        previous = s.toString();
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {
                    }

                    @Override
                    public void afterTextChanged(
                            Editable editable) {

                        if (formattingNumber) {
                            return;
                        }

                        String current =
                                editable.toString();

                        if (current.isEmpty()) {
                            return;
                        }

                        /*
                         * محل کرسر قبل از فرمت
                         */
                        int cursor =
                                editText.getSelectionStart();

                        if (cursor < 0) {
                            cursor = current.length();
                        }

                        /*
                         * تعداد ارقام قبل از کرسر
                         */
                        int digitsBeforeCursor = 0;

                        for (int i = 0;
                             i < Math.min(
                                     cursor,
                                     current.length()
                             );
                             i++) {

                            char c =
                                    current.charAt(i);

                            if (Character.isDigit(c)) {
                                digitsBeforeCursor++;
                            }
                        }

                        String clean =
                                cleanNumberText(current);

                        /*
                         * اگر فقط علامت منفی/نقطه بود
                         * فعلاً دست نمی‌زنیم.
                         */
                        if (clean.isEmpty()) {
                            return;
                        }

                        String formatted =
                                formatInputNumber(clean);

                        if (formatted.equals(current)) {
                            return;
                        }

                        formattingNumber = true;

                        editText.setText(formatted);

                        int newCursor =
                                cursorForDigitPosition(
                                        formatted,
                                        digitsBeforeCursor
                                );

                        try {

                            editText.setSelection(
                                    Math.max(
                                            0,
                                            Math.min(
                                                    newCursor,
                                                    formatted.length()
                                            )
                                    )
                            );

                        } catch (Exception ignored) {
                        }

                        formattingNumber = false;
                    }
                }
        );
    }

    /*
     * حذف جداکننده‌ها و تبدیل ارقام فارسی
     */
    private String cleanNumberText(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace(",", "")
                .replace("٬", "")
                .replace("٫", ".")
                .replace(" ", "")
                .replace("۰", "0")
                .replace("۱", "1")
                .replace("۲", "2")
                .replace("۳", "3")
                .replace("۴", "4")
                .replace("۵", "5")
                .replace("۶", "6")
                .replace("۷", "7")
                .replace("۸", "8")
                .replace("۹", "9");
    }

    /*
     * فرمت عدد با جداکننده هزارگان
     */
    private String formatInputNumber(
            String clean) {

        try {

            if (clean.contains(".")) {

                double value =
                        Double.parseDouble(clean);

                return String.format(
                        Locale.US,
                        "%,.4f",
                        value
                ).replaceAll(
                        "0+$",
                        ""
                ).replaceAll(
                        "\\.$",
                        ""
                );
            }

            long value =
                    Long.parseLong(clean);

            return String.format(
                    Locale.US,
                    "%,d",
                    value
            );

        } catch (Exception e) {

            return clean;
        }
    }

    /*
     * پیدا کردن مکان کرسر بعد از اضافه شدن
     * جداکننده‌های هزارگان
     */
    private int cursorForDigitPosition(
            String formatted,
            int digitPosition) {

        if (digitPosition <= 0) {
            return 0;
        }

        int digits = 0;

        for (int i = 0;
             i < formatted.length();
             i++) {

            if (Character.isDigit(
                    formatted.charAt(i)
            )) {

                digits++;

                if (digits >= digitPosition) {
                    return i + 1;
                }
            }
        }

        return formatted.length();
    }

    // =========================================================
    // AUTO CALCULATION
    // =========================================================

    private void addAutoCalculation(
            EditText quantity,
            EditText price,
            EditText total) {

        TextWatcher watcher =
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        if (formattingNumber ||
                                calculatingFields) {
                            return;
                        }

                        /*
                         * فقط بعد از تغییر واقعی،
                         * محاسبه انجام می‌شود.
                         */
                        calculateTradeFields(
                                quantity,
                                price,
                                total
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {
                    }
                };

        quantity.addTextChangedListener(watcher);
        price.addTextChangedListener(watcher);
        total.addTextChangedListener(watcher);
    }

    private void calculateTradeFields(
            EditText quantity,
            EditText price,
            EditText total) {

        if (calculatingFields ||
                formattingNumber) {
            return;
        }

        double q = number(quantity);
        double p = number(price);
        double t = number(total);

        /*
         * بسیار مهم:
         *
         * اگر دو فیلد پر باشند، فقط فیلد سوم
         * محاسبه می‌شود.
         *
         * به این ترتیب فیلد در حال تایپ
         * دائماً بازنویسی نمی‌شود.
         */
        try {

            calculatingFields = true;

            if (q > 0 && p > 0) {

                setFormattedText(
                        total,
                        q * p
                );

            } else if (q > 0 && t > 0) {

                setFormattedText(
                        price,
                        t / q
                );

            } else if (p > 0 && t > 0) {

                double calculatedQuantity =
                        Math.floor(t / p);

                if (calculatedQuantity > 0) {

                    setFormattedText(
                            quantity,
                            calculatedQuantity
                    );

                    /*
                     * مبلغ واقعی بر اساس تعداد کامل
                     */
                    setFormattedText(
                            total,
                            calculatedQuantity * p
                    );
                }
            }

        } finally {

            calculatingFields = false;
        }
    }

    private void setFormattedText(
            EditText field,
            double value) {

        String newValue =
                formatNumber(value);

        String oldValue =
                field.getText()
                        .toString();

        if (oldValue.equals(newValue)) {
            return;
        }

        formattingNumber = true;

        try {

            field.setText(newValue);

            field.setSelection(
                    field.length()
            );

        } finally {

            formattingNumber = false;
        }
    }

    private boolean saveTrade(
            String type,
            EditText portfolioField,
            EditText brokerField,
            EditText symbolField,
            EditText quantityField,
            EditText priceField,
            EditText totalField,
            EditText descriptionField) {

        String portfolio =
                portfolioField.getText()
                        .toString()
                        .trim();

        String broker =
                brokerField.getText()
                        .toString()
                        .trim();

        String symbol =
                symbolField.getText()
                        .toString()
                        .trim();

        String description =
                descriptionField.getText()
                        .toString()
                        .trim();

        double quantity =
                number(quantityField);

        double price =
                number(priceField);

        if (portfolio.isEmpty()) {
            portfolio = "اصلی";
        }

        if (symbol.isEmpty()) {
            toast("نماد را وارد کنید");
            return false;
        }

        if (quantity <= 0) {
            toast("تعداد معتبر نیست");
            return false;
        }

        if (price <= 0) {
            toast("قیمت معتبر نیست");
            return false;
        }

        if ("SELL".equals(type)) {

            Map<String, PortfolioEngine.Position> positions =
                    calculatePositions();

            String key =
                    portfolio + "|" + symbol;

            PortfolioEngine.Position position =
                    positions.get(key);

            double available =
                    position == null
                            ? 0
                            : position.quantity;

            if (available <= 0) {

                toastLong(
                        "فروش ثبت نشد.\n" +
                        "از نماد " +
                        symbol +
                        " موجودی ندارید."
                );

                return false;
            }

            if (quantity >
                    available + 0.0000001) {

                toastLong(
                        "فروش ثبت نشد.\n" +
                        "موجودی: " +
                        formatNumber(available) +
                        "\nدرخواست فروش: " +
                        formatNumber(quantity)
                );

                return false;
            }
        }

        double amount =
                quantity * price;

        double fee =
                "BUY".equals(type)
                        ? amount * BUY_FEE_RATE
                        : amount * SELL_FEE_RATE;

        db.addTransactionWithDescription(
                type,
                portfolio,
                broker,
                symbol,
                quantity,
                price,
                fee,
                amount,
                description
        );

        if ("BUY".equals(type)) {

            toastLong(
                    "خرید ثبت شد\n" +
                    "مبلغ معامله: " +
                    money(amount) +
                    " تومان\n" +
                    "کارمزد: " +
                    money(fee) +
                    " تومان\n" +
                    "پرداخت نهایی: " +
                    money(amount + fee) +
                    " تومان"
            );

        } else {

            toastLong(
                    "فروش ثبت شد\n" +
                    "مبلغ معامله: " +
                    money(amount) +
                    " تومان\n" +
                    "کارمزد: " +
                    money(fee) +
                    " تومان\n" +
                    "دریافتی خالص: " +
                    money(amount - fee) +
                    " تومان"
            );
        }

        return true;
    }

    // =========================================================
    // DEPOSIT / WITHDRAW
    // =========================================================

    private void showMoneyDialog(String type) {

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                20, 10, 20, 10
        );

        EditText portfolio =
                field("سبد / پرتفوی");

        portfolio.setText("اصلی");

        EditText amount =
                numberField("مبلغ");

        EditText description =
                field("توضیحات");

        box.addView(portfolio);
        box.addView(amount);
        box.addView(description);

        String title =
                "DEPOSIT".equals(type)
                        ? "ثبت واریزی"
                        : "ثبت برداشت";

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setView(box)
                        .setNegativeButton(
                                "انصراف",
                                null
                        )
                        .setPositiveButton(
                                "ثبت",
                                null
                        )
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                double value =
                        number(amount);

                if (value <= 0) {

                    toast("مبلغ معتبر نیست");
                    return;
                }

                String p =
                        portfolio.getText()
                                .toString()
                                .trim();

                if (p.isEmpty()) {
                    p = "اصلی";
                }

                db.addTransactionWithDescription(
                        type,
                        p,
                        "",
                        "",
                        0,
                        0,
                        0,
                        value,
                        description.getText()
                                .toString()
                                .trim()
                );

                toast("ثبت شد");

                dialog.dismiss();
            });
        });

        dialog.show();
    }

    // =========================================================
    // PORTFOLIOS
    // =========================================================

    private void showPortfolioDialog() {

        Cursor cursor =
                db.getAllTransactions();

        Map<String, Boolean> active =
                new LinkedHashMap<>();

        while (cursor.moveToNext()) {

            String portfolio =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "portfolio"
                            )
                    );

            if (portfolio == null ||
                    portfolio.trim().isEmpty()) {

                portfolio = "اصلی";
            }

            active.put(
                    portfolio,
                    true
            );
        }

        cursor.close();

        if (active.isEmpty()) {

            toast(
                    "هنوز سبدی ثبت نشده است"
            );

            return;
        }

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                20, 10, 20, 10
        );

        TextView title =
                new TextView(this);

        title.setText(
                "سبدهای فعال\n" +
                "برای ورود، روی سبد بزنید."
        );

        title.setTextSize(18);
        title.setPadding(
                0, 0, 0, 20
        );

        box.addView(title);

        for (String portfolio :
                active.keySet()) {

            Button b =
                    new Button(this);

            b.setText(
                    "📁 " + portfolio
            );

            String selectedPortfolio =
                    portfolio;

            b.setOnClickListener(
                    v ->
                            showPortfolioSymbolsDialog(
                                    selectedPortfolio
                            )
            );

            box.addView(b);
        }

        new AlertDialog.Builder(this)
                .setTitle("وضعیت سبدها")
                .setView(box)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    private void showPortfolioSymbolsDialog(
            String portfolioName) {

        Map<String, PortfolioEngine.Position> positions =
                calculatePositions();

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                20, 10, 20, 10
        );

        TextView header =
                new TextView(this);

        header.setText(
                "سبد: " +
                portfolioName +
                "\n\nنمادها و بهای تمام‌شده:"
        );

        header.setTextSize(18);

        box.addView(header);

        boolean found = false;

        for (Map.Entry<String,
                PortfolioEngine.Position> entry :
                positions.entrySet()) {

            PortfolioEngine.Position position =
                    entry.getValue();

            if (!portfolioName.equals(
                    position.portfolio)) {
                continue;
            }

            if (position.quantity <= 0) {
                continue;
            }

            found = true;

            Button symbolButton =
                    new Button(this);

            symbolButton.setText(
                    position.symbol +
                    "     |     " +
                    money(position.cost) +
                    " تومان"
            );

            String selectedSymbol =
                    position.symbol;

            symbolButton.setOnClickListener(
                    v ->
                            showSymbolTransactionsDialog(
                                    portfolioName,
                                    selectedSymbol
                            )
            );

            box.addView(symbolButton);
        }

        if (!found) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "در این سبد سهم فعالی وجود ندارد."
            );

            box.addView(empty);
        }

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle(
                        "نمادهای " +
                        portfolioName
                )
                .setView(scroll)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    // =========================================================
    // SYMBOL DETAILS
    // =========================================================

    private void showSymbolTransactionsDialog(
            String portfolioName,
            String symbolName) {

        Cursor cursor =
                db.getAllTransactions();

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                20, 10, 20, 10
        );

        PortfolioEngine.Position position =
                calculatePositions().get(
                        portfolioName +
                        "|" +
                        symbolName
                );

        if (position != null) {

            TextView summary =
                    new TextView(this);

            summary.setText(
                    "نماد: " +
                    symbolName +
                    "\nتعداد فعلی: " +
                    formatNumber(
                            position.quantity
                    ) +
                    "\nبهای تمام‌شده فعلی: " +
                    money(position.cost) +
                    " تومان" +
                    "\nمیانگین خرید: " +
                    money(
                            position.averagePrice()
                    ) +
                    " تومان" +
                    "\nسود/زیان تحقق‌یافته: " +
                    money(
                            position.realizedProfit
                    ) +
                    " تومان\n"
            );

            summary.setTextSize(17);

            box.addView(summary);
        }

        TextView transactionsTitle =
                new TextView(this);

        transactionsTitle.setText(
                "تمام خرید و فروش‌های این نماد:"
        );

        transactionsTitle.setTextSize(18);

        transactionsTitle.setPadding(
                0, 10, 0, 10
        );

        box.addView(
                transactionsTitle
        );

        double totalBuyAmount = 0;
        double totalBuyFee = 0;
        double totalSellAmount = 0;
        double totalSellFee = 0;

        boolean found = false;

        while (cursor.moveToNext()) {

            String portfolio =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "portfolio"
                            )
                    );

            String symbol =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "symbol"
                            )
                    );

            String type =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "type"
                            )
                    );

            if (portfolio == null ||
                    !portfolioName.equals(
                            portfolio)) {
                continue;
            }

            if (symbol == null ||
                    !symbolName.equals(symbol)) {
                continue;
            }

            if (!"BUY".equals(type) &&
                    !"SELL".equals(type)) {
                continue;
            }

            found = true;

            double quantity =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "quantity"
                            )
                    );

            double price =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "price"
                            )
                    );

            double amount =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "amount"
                            )
                    );

            double fee =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "fee"
                            )
                    );

            String date =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "date_shamsi"
                            )
                    );

            TextView item =
                    new TextView(this);

            double finalAmount;

            if ("BUY".equals(type)) {

                finalAmount =
                        amount + fee;

                totalBuyAmount += amount;
                totalBuyFee += fee;

            } else {

                finalAmount =
                        amount - fee;

                totalSellAmount += amount;
                totalSellFee += fee;
            }

            String operation =
                    "BUY".equals(type)
                            ? "🟢 خرید"
                            : "🔴 فروش";

            String finalLabel =
                    "BUY".equals(type)
                            ? "پرداخت نهایی"
                            : "دریافتی خالص";

            item.setText(
                    operation +
                    "\nتاریخ: " +
                    safe(date) +
                    "\nتعداد: " +
                    formatNumber(quantity) +
                    "\nقیمت هر سهم: " +
                    money(price) +
                    "\nمبلغ معامله: " +
                    money(amount) +
                    "\nکارمزد: " +
                    money(fee) +
                    "\n" +
                    finalLabel +
                    ": " +
                    money(finalAmount) +
                    "\n──────────────────"
            );

            item.setTextSize(15);

            item.setPadding(
                    0, 12, 0, 12
            );

            box.addView(item);
        }

        cursor.close();

        if (!found) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "برای این نماد معامله‌ای ثبت نشده است."
            );

            box.addView(empty);

        } else {

            TextView totals =
                    new TextView(this);

            totals.setText(
                    "\nجمع خرید:\n" +
                    "مبلغ معاملات: " +
                    money(totalBuyAmount) +
                    "\nکارمزد خرید: " +
                    money(totalBuyFee) +
                    "\nپرداخت نهایی خرید: " +
                    money(
                            totalBuyAmount +
                            totalBuyFee
                    ) +
                    "\n\nجمع فروش:\n" +
                    "مبلغ معاملات: " +
                    money(totalSellAmount) +
                    "\nکارمزد فروش: " +
                    money(totalSellFee) +
                    "\nدریافتی خالص فروش: " +
                    money(
                            totalSellAmount -
                            totalSellFee
                    )
            );

            totals.setTextSize(16);

            totals.setPadding(
                    0, 15, 0, 15
            );

            box.addView(totals);
        }

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle(
                        symbolName +
                        " | جزئیات معاملات"
                )
                .setView(scroll)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    // =========================================================
    // POSITION ENGINE
    // =========================================================

    private Map<String,
            PortfolioEngine.Position>
            calculatePositions() {

        Cursor cursor =
                db.getAllTransactions();

        Map<String,
                PortfolioEngine.Position> result =
                PortfolioEngine.calculate(cursor);

        cursor.close();

        return result;
    }

    private Map<String,
            PortfolioEngine.Position>
            calculatePositionsExcludingTransaction(
                    long excludedId) {

        Cursor cursor =
                db.getAllTransactions();

        Map<String,
                PortfolioEngine.Position> positions =
                new LinkedHashMap<>();

        while (cursor.moveToNext()) {

            long id =
                    cursor.getLong(
                            cursor.getColumnIndexOrThrow(
                                    "id"
                            )
                    );

            if (id == excludedId) {
                continue;
            }

            String type =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "type"
                            )
                    );

            String portfolio =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "portfolio"
                            )
                    );

            String symbol =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "symbol"
                            )
                    );

            if (portfolio == null ||
                    portfolio.trim().isEmpty()) {

                portfolio = "اصلی";
            }

            if (symbol == null ||
                    symbol.trim().isEmpty()) {

                continue;
            }

            String key =
                    portfolio +
                    "|" +
                    symbol;

            PortfolioEngine.Position position =
                    positions.get(key);

            if (position == null) {

                position =
                        new PortfolioEngine.Position(
                                portfolio,
                                symbol
                        );

                positions.put(
                        key,
                        position
                );
            }

            double quantity =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "quantity"
                            )
                    );

            double fee =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "fee"
                            )
                    );

            double amount =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "amount"
                            )
                    );

            if ("BUY".equals(type)) {

                position.buyQuantity +=
                        quantity;

                position.buyAmount +=
                        amount;

                position.buyFees +=
                        fee;

                position.cost +=
                        amount + fee;

                position.quantity +=
                        quantity;

            } else if ("SELL".equals(type)) {

                double sellQuantity =
                        Math.min(
                                quantity,
                                position.quantity
                        );

                if (sellQuantity <= 0) {
                    continue;
                }

                double average =
                        position.averagePrice();

                double costOfSold =
                        sellQuantity *
                        average;

                double ratio =
                        quantity > 0
                                ? sellQuantity /
                                quantity
                                : 0;

                double effectiveFee =
                        fee * ratio;

                double effectiveAmount =
                        amount * ratio;

                position.sellQuantity +=
                        sellQuantity;

                position.sellAmount +=
                        effectiveAmount;

                position.sellFees +=
                        effectiveFee;

                position.realizedProfit +=
                        effectiveAmount -
                        effectiveFee -
                        costOfSold;

                position.cost -=
                        costOfSold;

                position.quantity -=
                        sellQuantity;

                if (position.quantity <
                        0.0000001) {

                    position.quantity = 0;
                    position.cost = 0;
                }
            }
        }

        cursor.close();

        return positions;
    }

    // =========================================================
    // HISTORY
    // =========================================================

    private void showHistoryDialog() {

        Cursor cursor =
                db.getAllTransactions();

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                20, 10, 20, 10
        );

        boolean found = false;

        while (cursor.moveToNext()) {

            found = true;

            long id =
                    cursor.getLong(
                            cursor.getColumnIndexOrThrow(
                                    "id"
                            )
                    );

            String type =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "type"
                            )
                    );

            String portfolio =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "portfolio"
                            )
                    );

            String symbol =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "symbol"
                            )
                    );

            double quantity =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "quantity"
                            )
                    );

            double price =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "price"
                            )
                    );

            double amount =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "amount"
                            )
                    );

            double fee =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "fee"
                            )
                    );

            String date =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "date_shamsi"
                            )
                    );

            Button item =
                    new Button(this);

            if ("BUY".equals(type) ||
                    "SELL".equals(type)) {

                item.setText(
                        ("BUY".equals(type)
                                ? "🟢 خرید"
                                : "🔴 فروش") +
                        " | " +
                        safe(symbol) +
                        "\nتعداد: " +
                        formatNumber(quantity) +
                        " | قیمت: " +
                        money(price) +
                        "\n" +
                        safe(date)
                );

                item.setOnClickListener(
                        v ->
                                showTransactionActions(id)
                );

            } else {

                item.setText(
                        ("DEPOSIT".equals(type)
                                ? "💰 واریز"
                                : "💸 برداشت") +
                        " | " +
                        money(amount) +
                        "\n" +
                        safe(portfolio) +
                        " | " +
                        safe(date)
                );
            }

            item.setPadding(
                    0, 12, 0, 12
            );

            box.addView(item);
        }

        cursor.close();

        if (!found) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "تاریخچه‌ای وجود ندارد."
            );

            box.addView(empty);
        }

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle(
                        "تاریخچه\n" +
                        "برای ویرایش یا حذف روی معامله بزنید."
                )
                .setView(scroll)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    // =========================================================
    // EDIT / DELETE
    // =========================================================

    private void showTransactionActions(
            long id) {

        Cursor cursor =
                db.getTransaction(id);

        if (cursor == null ||
                !cursor.moveToFirst()) {

            if (cursor != null) {
                cursor.close();
            }

            toast("معامله پیدا نشد");
            return;
        }

        String type =
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "type"
                        )
                );

        String symbol =
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "symbol"
                        )
                );

        cursor.close();

        if (!"BUY".equals(type) &&
                !"SELL".equals(type)) {

            toast(
                    "این مورد معامله خرید/فروش نیست."
            );

            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        safe(symbol) +
                        " | عملیات"
                )
                .setItems(
                        new String[]{
                                "✏️ ویرایش معامله",
                                "🗑️ حذف معامله",
                                "انصراف"
                        },
                        (dialog, which) -> {

                            if (which == 0) {

                                showEditTransactionDialog(
                                        id
                                );

                            } else if (which == 1) {

                                confirmDeleteTransaction(
                                        id
                                );
                            }
                        }
                )
                .show();
    }

    private void showEditTransactionDialog(
            long id) {

        Cursor cursor =
                db.getTransaction(id);

        if (cursor == null ||
                !cursor.moveToFirst()) {

            if (cursor != null) {
                cursor.close();
            }

            toast("معامله پیدا نشد");
            return;
        }

        String type =
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "type"
                        )
                );

        String portfolioValue =
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "portfolio"
                        )
                );

        String brokerValue =
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "broker"
                        )
                );

        String symbolValue =
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "symbol"
                        )
                );

        double quantityValue =
                cursor.getDouble(
                        cursor.getColumnIndexOrThrow(
                                "quantity"
                        )
                );

        double priceValue =
                cursor.getDouble(
                        cursor.getColumnIndexOrThrow(
                                "price"
                        )
                );

        String descriptionValue =
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "description"
                        )
                );

        cursor.close();

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                20, 10, 20, 10
        );

        EditText portfolio =
                field("سبد / پرتفوی");

        portfolio.setText(
                safe(portfolioValue)
        );

        EditText broker =
                field("کارگزاری");

        broker.setText(
                safe(brokerValue)
        );

        EditText symbol =
                field("نماد");

        symbol.setText(
                safe(symbolValue)
        );

        EditText quantity =
                numberField("تعداد سهم");

        quantity.setText(
                formatNumber(quantityValue)
        );

        EditText price =
                numberField("قیمت هر سهم");

        price.setText(
                formatNumber(priceValue)
        );

        EditText total =
                numberField("مبلغ کل معامله");

        total.setText(
                formatNumber(
                        quantityValue *
                        priceValue
                )
        );

        EditText description =
                field("توضیحات");

        description.setText(
                safe(descriptionValue)
        );

        box.addView(portfolio);
        box.addView(broker);
        box.addView(symbol);
        box.addView(quantity);
        box.addView(price);
        box.addView(total);
        box.addView(description);

        TextView info =
                new TextView(this);

        info.setText(
                "\nنوع معامله: " +
                ("BUY".equals(type)
                        ? "خرید"
                        : "فروش") +
                "\nکارمزد پس از ذخیره مجدداً محاسبه می‌شود."
        );

        info.setPadding(
                0, 10, 0, 10
        );

        box.addView(info);

        addAutoCalculation(
                quantity,
                price,
                total
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "✏️ ویرایش معامله"
                        )
                        .setView(box)
                        .setNegativeButton(
                                "انصراف",
                                null
                        )
                        .setPositiveButton(
                                "ذخیره",
                                null
                        )
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                if (updateTrade(
                        id,
                        type,
                        portfolio,
                        broker,
                        symbol,
                        quantity,
                        price,
                        total,
                        description
                )) {

                    dialog.dismiss();
                    showHistoryDialog();
                }
            });
        });

        dialog.show();
    }

    private boolean updateTrade(
            long id,
            String type,
            EditText portfolioField,
            EditText brokerField,
            EditText symbolField,
            EditText quantityField,
            EditText priceField,
            EditText totalField,
            EditText descriptionField) {

        String portfolio =
                portfolioField.getText()
                        .toString()
                        .trim();

        String broker =
                brokerField.getText()
                        .toString()
                        .trim();

        String symbol =
                symbolField.getText()
                        .toString()
                        .trim();

        String description =
                descriptionField.getText()
                        .toString()
                        .trim();

        double quantity =
                number(quantityField);

        double price =
                number(priceField);

        if (portfolio.isEmpty()) {
            portfolio = "اصلی";
        }

        if (symbol.isEmpty()) {
            toast("نماد را وارد کنید");
            return false;
        }

        if (quantity <= 0) {
            toast("تعداد معتبر نیست");
            return false;
        }

        if (price <= 0) {
            toast("قیمت معتبر نیست");
            return false;
        }

        if ("SELL".equals(type)) {

            Map<String,
                    PortfolioEngine.Position> positions =
                    calculatePositionsExcludingTransaction(
                            id
                    );

            String key =
                    portfolio +
                    "|" +
                    symbol;

            PortfolioEngine.Position position =
                    positions.get(key);

            double available =
                    position == null
                            ? 0
                            : position.quantity;

            if (available <= 0) {

                toastLong(
                        "ویرایش انجام نشد.\n" +
                        "برای این نماد موجودی ندارید."
                );

                return false;
            }

            if (quantity >
                    available + 0.0000001) {

                toastLong(
                        "ویرایش انجام نشد.\n" +
                        "موجودی قابل فروش: " +
                        formatNumber(available) +
                        "\nمقدار فروش: " +
                        formatNumber(quantity)
                );

                return false;
            }
        }

        double amount =
                quantity * price;

        double fee =
                "BUY".equals(type)
                        ? amount * BUY_FEE_RATE
                        : amount * SELL_FEE_RATE;

        int result =
                db.updateTransaction(
                        id,
                        type,
                        portfolio,
                        broker,
                        symbol,
                        quantity,
                        price,
                        fee,
                        amount,
                        description
                );

        if (result <= 0) {

            toast("ویرایش انجام نشد");
            return false;
        }

        toastLong(
                "معامله اصلاح شد\n" +
                "مبلغ معامله: " +
                money(amount) +
                " تومان\n" +
                "کارمزد: " +
                money(fee) +
                " تومان"
        );

        return true;
    }

    private void confirmDeleteTransaction(
            long id) {

        Cursor cursor =
                db.getTransaction(id);

        if (cursor == null ||
                !cursor.moveToFirst()) {

            if (cursor != null) {
                cursor.close();
            }

            toast("معامله پیدا نشد");
            return;
        }

        String type =
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "type"
                        )
                );

        String symbol =
                cursor.getString(
                        cursor.getColumnIndexOrThrow(
                                "symbol"
                        )
                );

        double quantity =
                cursor.getDouble(
                        cursor.getColumnIndexOrThrow(
                                "quantity"
                        )
                );

        cursor.close();

        new AlertDialog.Builder(this)
                .setTitle("حذف معامله")
                .setMessage(
                        "آیا این معامله حذف شود؟\n\n" +
                        ("BUY".equals(type)
                                ? "خرید"
                                : "فروش") +
                        " " +
                        safe(symbol) +
                        "\nتعداد: " +
                        formatNumber(quantity)
                )
                .setNegativeButton(
                        "انصراف",
                        null
                )
                .setPositiveButton(
                        "حذف",
                        (dialog, which) -> {

                            int result =
                                    db.deleteTransaction(id);

                            if (result > 0) {
                                toast("معامله حذف شد");
                            } else {
                                toast("حذف انجام نشد");
                            }
                        }
                )
                .show();
    }

    // =========================================================
    // SEARCH
    // =========================================================

    private void showSearchDialog() {

        EditText search =
                field(
                        "نماد، توضیحات، سبد یا کارگزاری"
                );

        new AlertDialog.Builder(this)
                .setTitle("جستجو")
                .setView(search)
                .setNegativeButton(
                        "انصراف",
                        null
                )
                .setPositiveButton(
                        "جستجو",
                        (dialog, which) ->
                                showSearchResults(
                                        search.getText()
                                                .toString()
                                                .trim()
                                )
                )
                .show();
    }

    private void showSearchResults(
            String query) {

        if (query.isEmpty()) {
            toast(
                    "عبارت جستجو را وارد کنید"
            );
            return;
        }

        Cursor cursor =
                db.searchTransactions(query);

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                20, 10, 20, 10
        );

        boolean found = false;

        while (cursor.moveToNext()) {

            found = true;

            String type =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "type"
                            )
                    );

            String symbol =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "symbol"
                            )
                    );

            double amount =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "amount"
                            )
                    );

            double fee =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "fee"
                            )
                    );

            String date =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "date_shamsi"
                            )
                    );

            TextView item =
                    new TextView(this);

            item.setText(
                    safe(date) +
                    "\n" +
                    ("BUY".equals(type)
                            ? "خرید "
                            : "SELL".equals(type)
                            ? "فروش "
                            : type) +
                    safe(symbol) +
                    "\nمبلغ: " +
                    money(amount) +
                    "\nکارمزد: " +
                    money(fee) +
                    "\n──────────────────"
            );

            item.setPadding(
                    0, 10, 0, 10
            );

            box.addView(item);
        }

        cursor.close();

        if (!found) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "نتیجه‌ای پیدا نشد."
            );

            box.addView(empty);
        }

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle("نتایج جستجو")
                .setView(scroll)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    // =========================================================
    // CASH
    // =========================================================

    private void showCashBalance() {

        double balance =
                db.getCashBalance("اصلی");

        new AlertDialog.Builder(this)
                .setTitle("موجودی نقدی")
                .setMessage(
                        money(balance) +
                        " تومان"
                )
                .setPositiveButton(
                        "باشه",
                        null
                )
                .show();
    }

    // =========================================================
    // NUMBER PARSING
    // =========================================================

    private double number(
            EditText field) {

        try {

            String value =
                    field.getText()
                            .toString()
                            .trim();

            if (value.isEmpty()) {
                return 0;
            }

            value =
                    cleanNumberText(value);

            if (value.isEmpty() ||
                    ".".equals(value)) {

                return 0;
            }

            return Double.parseDouble(value);

        } catch (Exception e) {

            return 0;
        }
    }

    /*
     * نمایش عدد با جداکننده هزارگان
     */
    private String money(
            double value) {

        return String.format(
                Locale.US,
                "%,.0f",
                value
        );
    }

    /*
     * فرمت عمومی اعداد:
     * 2500 -> 2,500
     * 120000000 -> 120,000,000
     */
    private String formatNumber(
            double value) {

        if (Math.abs(
                value -
                Math.round(value)
        ) < 0.0000001) {

            return String.format(
                    Locale.US,
                    "%,.0f",
                    value
            );
        }

        return String.format(
                Locale.US,
                "%,.4f",
                value
        )
                .replaceAll(
                        "0+$",
                        ""
                )
                .replaceAll(
                        "\\.$",
                        ""
                );
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }

    private void toast(
            String message) {

        android.widget.Toast.makeText(
                this,
                message,
                android.widget.Toast.LENGTH_SHORT
        ).show();
    }

    private void toastLong(
            String message) {

        android.widget.Toast.makeText(
                this,
                message,
                android.widget.Toast.LENGTH_LONG
        ).show();
    }
}
