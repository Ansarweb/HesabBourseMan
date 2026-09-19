package ir.ansarweb.hesabbourse;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
    private static final double OPTION_FEE_RATE = 0.00103;

    private static final int REQUEST_EXPORT_BACKUP = 1001;
    private static final int REQUEST_IMPORT_BACKUP = 1002;

    private boolean formattingNumber = false;
    private boolean calculatingFields = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DatabaseHelper(this);

        buildMainScreen();
    }

    // =========================================================
    // MAIN SCREEN
    // =========================================================

    private void buildMainScreen() {

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        ScrollView scroll = new ScrollView(this);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("حساب بورس");
        title.setTextSize(28);
        title.setPadding(0, 0, 0, 25);
        content.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("بخش موردنظر را انتخاب کنید");
        subtitle.setTextSize(17);
        subtitle.setPadding(0, 0, 0, 20);
        content.addView(subtitle);

        Button stocks = new Button(this);
        stocks.setText("📈 سهام");
        stocks.setTextSize(19);
        stocks.setOnClickListener(v -> showStockSection());
        content.addView(stocks);

        Button options = new Button(this);
        options.setText("🔵 معاملات آپشن");
        options.setTextSize(19);
        options.setOnClickListener(v -> showOptionSection());
        content.addView(options);

        Button backup = new Button(this);
        backup.setText("💾 بک‌آپ / بازیابی");
        backup.setOnClickListener(v -> showBackupMenu());
        content.addView(backup);

        scroll.addView(content);
        root.addView(scroll);

        setContentView(root);
    }

    // =========================================================
    // STOCK SECTION
    // =========================================================

    private void showStockSection() {

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(24, 24, 24, 24);

        ScrollView scroll = new ScrollView(this);

        Button back = new Button(this);
        back.setText("⬅️ بازگشت");
        back.setOnClickListener(v -> buildMainScreen());
        content.addView(back);

        TextView title = new TextView(this);
        title.setText("📈 بخش سهام");
        title.setTextSize(26);
        title.setPadding(0, 20, 0, 25);
        content.addView(title);

        Button dashboard = new Button(this);
        dashboard.setText("📊 وضعیت سبدها");
        dashboard.setOnClickListener(v -> showPortfolioDialog());
        content.addView(dashboard);

        Button buy = new Button(this);
        buy.setText("🟢 ثبت خرید");
        buy.setOnClickListener(v -> showTradeDialog("BUY"));
        content.addView(buy);

        Button sell = new Button(this);
        sell.setText("🔴 ثبت فروش");
        sell.setOnClickListener(v -> showTradeDialog("SELL"));
        content.addView(sell);

        Button deposit = new Button(this);
        deposit.setText("💰 ثبت واریزی");
        deposit.setOnClickListener(v -> showMoneyDialog("DEPOSIT"));
        content.addView(deposit);

        Button withdraw = new Button(this);
        withdraw.setText("💸 ثبت برداشت");
        withdraw.setOnClickListener(v -> showMoneyDialog("WITHDRAW"));
        content.addView(withdraw);

        Button history = new Button(this);
        history.setText("📋 تاریخچه معاملات سهام");
        history.setOnClickListener(v -> showHistoryDialog("STOCK"));
        content.addView(history);

        Button moneyHistory = new Button(this);
        moneyHistory.setText("💳 تاریخچه واریز و برداشت");
        moneyHistory.setOnClickListener(v -> showMoneyHistoryDialog());
        content.addView(moneyHistory);

        Button realized = new Button(this);
        realized.setText("💰 سود/زیان تحقق‌یافته");
        realized.setOnClickListener(v -> showRealizedProfitDialog());
        content.addView(realized);

        Button search = new Button(this);
        search.setText("🔎 جستجو در معاملات");
        search.setOnClickListener(v -> showSearchDialog());
        content.addView(search);

        Button cash = new Button(this);
        cash.setText("💵 موجودی نقدی");
        cash.setOnClickListener(v -> showCashBalance());
        content.addView(cash);

        scroll.addView(content);
        setContentView(scroll);
    }

    // =========================================================
    // OPTION SECTION
    // =========================================================

    private void showOptionSection() {

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(24, 24, 24, 24);

        ScrollView scroll = new ScrollView(this);

        Button back = new Button(this);
        back.setText("⬅️ بازگشت");
        back.setOnClickListener(v -> buildMainScreen());
        content.addView(back);

        TextView title = new TextView(this);
        title.setText("🔵 معاملات آپشن");
        title.setTextSize(26);
        title.setPadding(0, 20, 0, 25);
        content.addView(title);

        Button optionBuy = new Button(this);
        optionBuy.setText("🟢 خرید آپشن");
        optionBuy.setOnClickListener(v -> showOptionDialog("BUY"));
        content.addView(optionBuy);

        Button optionSell = new Button(this);
        optionSell.setText("🔴 فروش آپشن");
        optionSell.setOnClickListener(v -> showOptionDialog("SELL"));
        content.addView(optionSell);

        Button optionHistory = new Button(this);
        optionHistory.setText("📋 تاریخچه معاملات آپشن");
        optionHistory.setOnClickListener(v -> showHistoryDialog("OPTION"));
        content.addView(optionHistory);

        Button optionPositions = new Button(this);
        optionPositions.setText("📊 وضعیت پوزیشن‌های آپشن");
        optionPositions.setOnClickListener(v -> showOptionPositions());
        content.addView(optionPositions);

        Button optionRealized = new Button(this);
        optionRealized.setText("💰 سود/زیان تحقق‌یافته آپشن");
        optionRealized.setOnClickListener(v -> showOptionRealizedProfit());
        content.addView(optionRealized);

        scroll.addView(content);
        setContentView(scroll);
    }

    // =========================================================
    // OPTION TRADE
    // =========================================================

    private void showOptionDialog(String type) {

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20, 10, 20, 10);

        EditText portfolio = field("سبد / پرتفوی");
        portfolio.setText("اصلی");

        EditText broker = field("کارگزاری");
        EditText symbol = field("نماد آپشن");
        EditText underlying = field("دارایی پایه");

        TextView optionTypeLabel = new TextView(this);
        optionTypeLabel.setText("نوع آپشن");
        optionTypeLabel.setPadding(0, 10, 0, 5);

        Spinner optionType = new Spinner(this);

        ArrayAdapter<String> optionTypeAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        new String[]{"CALL", "PUT"}
                );

        optionTypeAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        optionType.setAdapter(optionTypeAdapter);

        TextView positionTypeLabel = new TextView(this);
        positionTypeLabel.setText("نوع پوزیشن");
        positionTypeLabel.setPadding(0, 10, 0, 5);

        Spinner positionType = new Spinner(this);

        ArrayAdapter<String> positionTypeAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        new String[]{"LONG", "SHORT"}
                );

        positionTypeAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        positionType.setAdapter(positionTypeAdapter);

        EditText strike = numberField("قیمت اعمال (Strike)");
        EditText expiry = field("تاریخ سررسید");
        EditText contractSize = numberField("اندازه قرارداد");
        contractSize.setText("1000");

        EditText quantity = numberField("تعداد قرارداد");
        EditText premium = numberField("پرمیوم هر واحد");
        EditText amount = numberField("مبلغ کل پرمیوم");

        TextView feeText = new TextView(this);
        feeText.setText("کارمزد: 0 تومان");
        feeText.setTextSize(16);
        feeText.setPadding(0, 15, 0, 15);

        EditText description = field("توضیحات");

        box.addView(portfolio);
        box.addView(broker);
        box.addView(symbol);
        box.addView(underlying);
        box.addView(optionTypeLabel);
        box.addView(optionType);
        box.addView(positionTypeLabel);
        box.addView(positionType);
        box.addView(strike);
        box.addView(expiry);
        box.addView(contractSize);
        box.addView(quantity);
        box.addView(premium);
        box.addView(amount);
        box.addView(feeText);
        box.addView(description);

        addOptionCalculation(
                quantity,
                premium,
                contractSize,
                amount,
                feeText
        );

        String title =
                "BUY".equals(type)
                        ? "🟢 خرید آپشن"
                        : "🔴 فروش آپشن";

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setView(box)
                        .setNegativeButton("انصراف", null)
                        .setPositiveButton("ثبت", null)
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                if (saveOptionTrade(
                        type,
                        portfolio,
                        broker,
                        symbol,
                        underlying,
                        optionType,
                        positionType,
                        strike,
                        expiry,
                        contractSize,
                        quantity,
                        premium,
                        description
                )) {
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void addOptionCalculation(
            EditText quantity,
            EditText premium,
            EditText contractSize,
            EditText amount,
            TextView feeText) {

        TextWatcher watcher = new TextWatcher() {

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
            }

            @Override
            public void afterTextChanged(Editable s) {

                if (calculatingFields || formattingNumber) {
                    return;
                }

                double q = number(quantity);
                double p = number(premium);
                double cs = number(contractSize);

                try {

                    calculatingFields = true;

                    if (q > 0 && p > 0 && cs > 0) {

                        double total = q * p * cs;

                        double fee =
                                p *
                                cs *
                                OPTION_FEE_RATE *
                                q;

                        setFormattedText(amount, total);

                        feeText.setText(
                                "کارمزد: " +
                                money(fee) +
                                " تومان"
                        );
                    }

                } finally {
                    calculatingFields = false;
                }
            }
        };

        quantity.addTextChangedListener(watcher);
        premium.addTextChangedListener(watcher);
        contractSize.addTextChangedListener(watcher);
    }

    // =========================================================
    // SAVE OPTION
    // =========================================================

    private boolean saveOptionTrade(
            String type,
            EditText portfolioField,
            EditText brokerField,
            EditText symbolField,
            EditText underlyingField,
            Spinner optionTypeSpinner,
            Spinner positionTypeSpinner,
            EditText strikeField,
            EditText expiryField,
            EditText contractSizeField,
            EditText quantityField,
            EditText premiumField,
            EditText descriptionField) {

        String portfolio =
                portfolioField.getText().toString().trim();

        String broker =
                brokerField.getText().toString().trim();

        String symbol =
                symbolField.getText().toString().trim();

        String underlying =
                underlyingField.getText().toString().trim();

        String optionType =
                optionTypeSpinner.getSelectedItem().toString();

        String positionType =
                positionTypeSpinner.getSelectedItem().toString();

        double strike = number(strikeField);

        String expiry =
                expiryField.getText().toString().trim();

        double contractSize = number(contractSizeField);
        double quantity = number(quantityField);
        double premium = number(premiumField);

        String description =
                descriptionField.getText().toString().trim();

        if (portfolio.isEmpty()) {
            portfolio = "اصلی";
        }

        if (symbol.isEmpty()) {
            toast("نماد آپشن را وارد کنید");
            return false;
        }

        if (underlying.isEmpty()) {
            toast("دارایی پایه را وارد کنید");
            return false;
        }

        if (strike <= 0) {
            toast("قیمت اعمال معتبر نیست");
            return false;
        }

        if (contractSize <= 0) {
            toast("اندازه قرارداد معتبر نیست");
            return false;
        }

        if (quantity <= 0) {
            toast("تعداد قرارداد معتبر نیست");
            return false;
        }

        if (premium <= 0) {
            toast("پرمیوم معتبر نیست");
            return false;
        }

        if ("SELL".equals(type) &&
                "LONG".equals(positionType)) {

            PortfolioEngine.Position position =
                    findOptionPosition(
                            portfolio,
                            underlying,
                            optionType,
                            strike,
                            expiry,
                            contractSize
                    );

            double available =
                    position == null
                            ? 0
                            : position.longQuantity;

            if (quantity > available + 0.0000001) {

                toastLong(
                        "فروش لانگ ثبت نشد.\n" +
                        "پوزیشن موجود: " +
                        formatNumber(available)
                );

                return false;
            }
        }

        if ("BUY".equals(type) &&
                "SHORT".equals(positionType)) {

            PortfolioEngine.Position position =
                    findOptionPosition(
                            portfolio,
                            underlying,
                            optionType,
                            strike,
                            expiry,
                            contractSize
                    );

            double available =
                    position == null
                            ? 0
                            : position.shortQuantity;

            if (quantity > available + 0.0000001) {

                toastLong(
                        "بستن شورت ثبت نشد.\n" +
                        "پوزیشن شورت موجود: " +
                        formatNumber(available)
                );

                return false;
            }
        }

        double amount =
                premium *
                quantity *
                contractSize;

        double fee =
                premium *
                contractSize *
                OPTION_FEE_RATE *
                quantity;

        db.addOptionTransaction(
                type,
                portfolio,
                broker,
                symbol,
                underlying,
                optionType,
                strike,
                expiry,
                contractSize,
                quantity,
                premium,
                positionType,
                description
        );

        String action;

        if ("SELL".equals(type) &&
                "SHORT".equals(positionType)) {

            action = "شورت جدید باز شد";

        } else if ("BUY".equals(type) &&
                "SHORT".equals(positionType)) {

            action = "شورت بسته شد";

        } else if ("BUY".equals(type)) {

            action = "پوزیشن لانگ باز شد";

        } else {

            action = "پوزیشن لانگ کاهش یافت";
        }

        toastLong(
                "✅ " +
                action +
                "\nمبلغ پرمیوم: " +
                money(amount) +
                " تومان\nکارمزد: " +
                money(fee) +
                " تومان"
        );

        return true;
    }

    // =========================================================
    // OPTION POSITION
    // =========================================================

    private PortfolioEngine.Position findOptionPosition(
            String portfolio,
            String underlying,
            String optionType,
            double strike,
            String expiry,
            double contractSize) {

        Map<String, PortfolioEngine.Position> positions =
                calculatePositions();

        for (PortfolioEngine.Position p :
                positions.values()) {

            if (!"OPTION".equalsIgnoreCase(p.assetType)) {
                continue;
            }

            if (!safe(p.portfolio).equals(safe(portfolio))) {
                continue;
            }

            if (!safe(p.underlying).equals(safe(underlying))) {
                continue;
            }

            if (!safe(p.optionType).equals(safe(optionType))) {
                continue;
            }

            if (!safe(p.expiryDate).equals(safe(expiry))) {
                continue;
            }

            if (Math.abs(
                    p.strikePrice - strike
            ) > 0.0000001) {
                continue;
            }

            if (Math.abs(
                    p.contractSize - contractSize
            ) > 0.0000001) {
                continue;
            }

            return p;
        }

        return null;
    }

    // =========================================================
    // OPTION POSITIONS
    // =========================================================

    private void showOptionPositions() {

        Map<String, PortfolioEngine.Position> positions =
                calculatePositions();

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20, 10, 20, 10);

        boolean found = false;

        for (PortfolioEngine.Position p :
                positions.values()) {

            if (!"OPTION".equalsIgnoreCase(p.assetType)) {
                continue;
            }

            if (p.longQuantity <= 0 &&
                    p.shortQuantity <= 0) {
                continue;
            }

            found = true;

            TextView item = new TextView(this);

            StringBuilder text = new StringBuilder();

            text.append("🔵 ");
            text.append(safe(p.symbol));

            text.append("\nدارایی پایه: ");
            text.append(safe(p.underlying));

            text.append("\nنوع: ");
            text.append(safe(p.optionType));

            text.append("\nStrike: ");
            text.append(money(p.strikePrice));

            text.append("\nسررسید: ");
            text.append(safe(p.expiryDate));

            if (p.longQuantity > 0) {

                text.append("\n🟢 لانگ: ");
                text.append(formatNumber(p.longQuantity));
                text.append(" قرارداد");

                text.append("\nمیانگین لانگ: ");
                text.append(
                        money(
                                p.longQuantity > 0
                                        ? p.longCost /
                                        p.longQuantity
                                        : 0
                        )
                );
            }

            if (p.shortQuantity > 0) {

                text.append("\n🔴 شورت: ");
                text.append(formatNumber(p.shortQuantity));
                text.append(" قرارداد");
            }

            item.setText(text.toString());
            item.setTextSize(16);
            item.setPadding(0, 15, 0, 15);

            box.addView(item);
        }

        if (!found) {

            TextView empty = new TextView(this);
            empty.setText(
                    "در حال حاضر پوزیشن آپشن باز ندارید."
            );
            empty.setTextSize(17);

            box.addView(empty);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle("📊 پوزیشن‌های باز آپشن")
                .setView(scroll)
                .setPositiveButton("بستن", null)
                .show();
    }

    // =========================================================
    // OPTION REALIZED
    // =========================================================

    private void showOptionRealizedProfit() {

        Map<String, PortfolioEngine.Position> positions =
                calculatePositions();

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20, 10, 20, 10);

        boolean found = false;
        double totalProfit = 0;

        for (PortfolioEngine.Position p :
                positions.values()) {

            if (!"OPTION".equalsIgnoreCase(p.assetType)) {
                continue;
            }

            if (!p.hasRealizedProfit()) {
                continue;
            }

            found = true;
            totalProfit += p.realizedProfit;

            TextView item = new TextView(this);

            item.setText(
                    "🔵 " +
                    safe(p.symbol) +
                    "\nدارایی پایه: " +
                    safe(p.underlying) +
                    "\n" +
                    (p.realizedProfit >= 0
                            ? "🟢 سود تحقق‌یافته: "
                            : "🔴 زیان تحقق‌یافته: ") +
                    money(p.realizedProfit) +
                    " تومان" +
                    "\nدرصد: " +
                    formatNumber(
                            p.realizedProfitPercent()
                    ) +
                    "%"
            );

            item.setTextSize(16);
            item.setPadding(0, 15, 0, 15);

            box.addView(item);
        }

        if (!found) {

            TextView empty = new TextView(this);
            empty.setText(
                    "هنوز سود/زیان تحقق‌یافته‌ای برای آپشن ثبت نشده است."
            );

            box.addView(empty);

        } else {

            TextView total = new TextView(this);

            total.setText(
                    "\nجمع سود/زیان تحقق‌یافته:\n" +
                    money(totalProfit) +
                    " تومان"
            );

            total.setTextSize(18);
            total.setPadding(0, 20, 0, 10);

            box.addView(total);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle("💰 سود/زیان تحقق‌یافته آپشن")
                .setView(scroll)
                .setPositiveButton("بستن", null)
                .show();
    }

    // =========================================================
    // STOCK REALIZED
    // =========================================================

    private void showRealizedProfitDialog() {

        Map<String, PortfolioEngine.Position> positions =
                calculatePositions();

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20, 10, 20, 10);

        boolean found = false;
        double total = 0;

        for (PortfolioEngine.Position p :
                positions.values()) {

            if (!p.isStock()) {
                continue;
            }

            if (!p.hasRealizedProfit()) {
                continue;
            }

            found = true;
            total += p.realizedProfit;

            TextView item = new TextView(this);

            item.setText(
                    "📈 " +
                    safe(p.symbol) +
                    "\n" +
                    (p.realizedProfit >= 0
                            ? "🟢 سود تحقق‌یافته: "
                            : "🔴 زیان تحقق‌یافته: ") +
                    money(p.realizedProfit) +
                    " تومان" +
                    "\nدرصد: " +
                    formatNumber(
                            p.realizedProfitPercent()
                    ) +
                    "%"
            );

            item.setTextSize(16);
            item.setPadding(0, 15, 0, 15);

            box.addView(item);
        }

        if (!found) {

            TextView empty = new TextView(this);
            empty.setText(
                    "هنوز سود/زیان تحقق‌یافته‌ای ثبت نشده است."
            );

            box.addView(empty);

        } else {

            TextView totalText = new TextView(this);

            totalText.setText(
                    "\nجمع کل:\n" +
                    money(total) +
                    " تومان"
            );

            totalText.setTextSize(18);
            box.addView(totalText);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle("💰 سود/زیان تحقق‌یافته سهام")
                .setView(scroll)
                .setPositiveButton("بستن", null)
                .show();
    }

    // =========================================================
    // BACKUP
    // =========================================================

    private void showBackupMenu() {

        new AlertDialog.Builder(this)
                .setTitle("💾 پشتیبان‌گیری")
                .setItems(
                        new String[]{
                                "💾 گرفتن بک‌آپ",
                                "📥 بازیابی بک‌آپ",
                                "انصراف"
                        },
                        (dialog, which) -> {

                            if (which == 0) {
                                startBackupExport();

                            } else if (which == 1) {
                                startBackupImport();
                            }
                        }
                )
                .show();
    }

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

            String json = db.exportBackupJson();

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
                        "تراکنش‌های فعلی حذف و اطلاعات " +
                        "بک‌آپ جایگزین می‌شوند.\n\n" +
                        "آیا مطمئن هستید؟"
                )
                .setNegativeButton("انصراف", null)
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

            StringBuilder json = new StringBuilder();

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

            db.importBackupJson(json.toString());

            toastLong(
                    "✅ بازیابی با موفقیت انجام شد."
            );

            buildMainScreen();

        } catch (Exception e) {

            toastLong(
                    "❌ بازیابی انجام نشد.\n" +
                    safe(e.getMessage())
            );
        }
    }

    // =========================================================
    // MONEY
    // =========================================================

    private void showMoneyDialog(String type) {

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20, 10, 20, 10);

        EditText portfolio = field("سبد / پرتفوی");
        portfolio.setText("اصلی");

        EditText amount = numberField("مبلغ");
        EditText description = field("توضیحات");

        box.addView(portfolio);
        box.addView(amount);
        box.addView(description);

        String title =
                "DEPOSIT".equals(type)
                        ? "💰 ثبت واریزی"
                        : "💸 ثبت برداشت";

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setView(box)
                        .setNegativeButton("انصراف", null)
                        .setPositiveButton("ثبت", null)
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                double value = number(amount);

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
    // MONEY HISTORY
    // =========================================================

    private void showMoneyHistoryDialog() {

        Cursor cursor = db.getAllTransactions();

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20, 10, 20, 10);

        boolean found = false;

        while (cursor.moveToNext()) {

            String type =
                    getCursorString(cursor, "type");

            if (!"DEPOSIT".equals(type) &&
                    !"WITHDRAW".equals(type)) {
                continue;
            }

            found = true;

            final long id =
                    cursor.getLong(
                            cursor.getColumnIndexOrThrow("id")
                    );

            String portfolio =
                    getCursorString(
                            cursor,
                            "portfolio"
                    );

            double amount =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "amount"
                            )
                    );

            String description =
                    getCursorString(
                            cursor,
                            "description"
                    );

            String date =
                    getCursorString(
                            cursor,
                            "date_shamsi"
                    );

            Button item = new Button(this);

            String label =
                    "DEPOSIT".equals(type)
                            ? "💰 واریز"
                            : "💸 برداشت";

            item.setText(
                    label +
                    "\nمبلغ: " +
                    money(amount) +
                    " تومان" +
                    "\nسبد: " +
                    safe(portfolio) +
                    "\nتاریخ: " +
                    safe(date) +
                    (description.isEmpty()
                            ? ""
                            : "\n" + description)
            );

            item.setOnClickListener(
                    v -> showTransactionActionsDialog(id)
            );

            box.addView(item);
        }

        cursor.close();

        if (!found) {

            TextView empty = new TextView(this);

            empty.setText(
                    "هنوز واریز یا برداشتی ثبت نشده است."
            );

            empty.setTextSize(17);

            box.addView(empty);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle(
                        "💳 تاریخچه واریز و برداشت"
                )
                .setView(scroll)
                .setPositiveButton("بستن", null)
                .show();
    }

    // =========================================================
    // STOCK TRADE
    // =========================================================

    private void showTradeDialog(String type) {

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20, 10, 20, 10);

        EditText portfolio = field("سبد / پرتفوی");
        portfolio.setText("اصلی");

        EditText broker = field("کارگزاری");
        EditText symbol = field("نماد");
        EditText quantity = numberField("تعداد سهم");
        EditText price = numberField("قیمت هر سهم");
        EditText total = numberField("مبلغ کل معامله");
        EditText description = field("توضیحات");

        box.addView(portfolio);
        box.addView(broker);
        box.addView(symbol);
        box.addView(quantity);
        box.addView(price);
        box.addView(total);
        box.addView(description);

        addAutoCalculation(
                quantity,
                price,
                total
        );

        String title =
                "BUY".equals(type)
                        ? "🟢 ثبت خرید"
                        : "🔴 ثبت فروش";

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setView(box)
                        .setNegativeButton("انصراف", null)
                        .setPositiveButton("ثبت", null)
                        .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(v -> {

                if (saveTrade(
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
                }
            });
        });

        dialog.show();
    }

    // =========================================================
    // STOCK CALCULATION
    // =========================================================

    private void addAutoCalculation(
            EditText quantity,
            EditText price,
            EditText total) {

        addCalculationWatcher(
                quantity,
                quantity,
                price,
                total
        );

        addCalculationWatcher(
                price,
                quantity,
                price,
                total
        );

        addCalculationWatcher(
                total,
                quantity,
                price,
                total
        );
    }

    private void addCalculationWatcher(
            EditText source,
            EditText quantity,
            EditText price,
            EditText total) {

        source.addTextChangedListener(
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
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {

                        if (formattingNumber ||
                                calculatingFields) {
                            return;
                        }

                        calculateTradeFields(
                                source,
                                quantity,
                                price,
                                total
                        );
                    }
                }
        );
    }

    private void calculateTradeFields(
            EditText source,
            EditText quantity,
            EditText price,
            EditText total) {

        double q = number(quantity);
        double p = number(price);
        double t = number(total);

        try {

            calculatingFields = true;

            if (source == quantity) {

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
                }

            } else if (source == price) {

                if (q > 0 && p > 0) {

                    setFormattedText(
                            total,
                            q * p
                    );

                } else if (p > 0 && t > 0) {

                    setFormattedText(
                            quantity,
                            Math.floor(t / p)
                    );
                }

            } else {

                if (q > 0 && t > 0) {

                    setFormattedText(
                            price,
                            t / q
                    );

                } else if (p > 0 && t > 0) {

                    setFormattedText(
                            quantity,
                            Math.floor(t / p)
                    );
                }
            }

        } finally {

            calculatingFields = false;
        }
    }

    // =========================================================
    // SAVE STOCK
    // =========================================================

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
                portfolioField.getText().toString().trim();

        String broker =
                brokerField.getText().toString().trim();

        String symbol =
                symbolField.getText().toString().trim();

        double quantity = number(quantityField);
        double price = number(priceField);

        String description =
                descriptionField.getText()
                        .toString()
                        .trim();

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
                    "STOCK|" +
                    portfolio +
                    "|" +
                    symbol;

            PortfolioEngine.Position position =
                    positions.get(key);

            if (position == null) {

                key =
                        portfolio +
                        "|" +
                        symbol;

                position =
                        positions.get(key);
            }

            double available =
                    position == null
                            ? 0
                            : position.quantity;

            if (quantity >
                    available + 0.0000001) {

                toastLong(
                        "فروش ثبت نشد.\n" +
                        "موجودی: " +
                        formatNumber(available)
                );

                return false;
            }
        }

        double amount = quantity * price;

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

        toast("معامله با موفقیت ثبت شد");

        return true;
    }

    // =========================================================
    // PORTFOLIO
    // =========================================================

    private void showPortfolioDialog() {

        Map<String, PortfolioEngine.Position> positions =
                calculatePositions();

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20, 10, 20, 10);

        Map<String, Boolean> portfolios =
                new LinkedHashMap<>();

        for (PortfolioEngine.Position p :
                positions.values()) {

            if (p.isStock() && p.quantity > 0) {

                portfolios.put(
                        p.portfolio,
                        true
                );
            }
        }

        if (portfolios.isEmpty()) {

            TextView empty = new TextView(this);

            empty.setText(
                    "در حال حاضر سهم فعالی در سبدها وجود ندارد."
            );

            box.addView(empty);

        } else {

            for (String portfolio :
                    portfolios.keySet()) {

                Button b = new Button(this);

                b.setText("📁 " + portfolio);

                b.setOnClickListener(
                        v ->
                                showPortfolioSymbolsDialog(
                                        portfolio
                                )
                );

                box.addView(b);
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("📊 وضعیت سبدها")
                .setView(box)
                .setPositiveButton("بستن", null)
                .show();
    }

    // =========================================================
    // PORTFOLIO SYMBOL LIST
    // =========================================================

    private void showPortfolioSymbolsDialog(
            String portfolioName) {

        Map<String, PortfolioEngine.Position> positions =
                calculatePositions();

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(10, 10, 10, 10);

        boolean found = false;

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView hSymbol = new TextView(this);
        hSymbol.setText("نماد");
        hSymbol.setTextSize(14);
        hSymbol.setPadding(8, 8, 8, 8);

        TextView hValue = new TextView(this);
        hValue.setText("ارزش");
        hValue.setTextSize(14);
        hValue.setGravity(android.view.Gravity.CENTER);
        hValue.setPadding(8, 8, 8, 8);

        TextView hPnl = new TextView(this);
        hPnl.setText("سود/زیان");
        hPnl.setTextSize(14);
        hPnl.setGravity(android.view.Gravity.CENTER);
        hPnl.setPadding(8, 8, 8, 8);

        header.addView(
                hSymbol,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        0.28f
                )
        );

        header.addView(
                hValue,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        0.42f
                )
        );

        header.addView(
                hPnl,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        0.30f
                )
        );

        box.addView(header);

        for (PortfolioEngine.Position p :
                positions.values()) {

            if (!p.isStock()) {
                continue;
            }

            if (!portfolioName.equals(p.portfolio)) {
                continue;
            }

            if (p.quantity <= 0) {
                continue;
            }

            found = true;

            String symbol = p.symbol;

            double currentPrice =
                    getCurrentPrice(
                            portfolioName,
                            symbol
                    );

            double currentValue =
                    currentPrice > 0
                            ? p.quantity * currentPrice
                            : 0;

            double pnl =
                    currentPrice > 0
                            ? currentValue - p.cost
                            : 0;

            double pnlPercent =
                    currentPrice > 0 &&
                    p.cost > 0
                            ? pnl / p.cost * 100.0
                            : 0;

            LinearLayout row =
                    new LinearLayout(this);

            row.setOrientation(
                    LinearLayout.HORIZONTAL
            );

            row.setPadding(
                    0, 8, 0, 8
            );

            TextView symbolText =
                    new TextView(this);

            symbolText.setText(
                    safe(symbol)
            );

            symbolText.setTextSize(17);
            symbolText.setGravity(
                    android.view.Gravity.CENTER_VERTICAL
            );

            TextView valueText =
                    new TextView(this);

            if (currentPrice > 0) {

                valueText.setText(
                        "ارزش: " +
                        money(currentValue)
                );

            } else {

                valueText.setText(
                        "ارزش: —"
                );
            }

            valueText.setTextSize(14);
            valueText.setGravity(
                    android.view.Gravity.CENTER
            );

            TextView pnlText =
                    new TextView(this);

            if (currentPrice > 0) {

                String prefix =
                        pnlPercent >= 0
                                ? "سود: "
                                : "زیان: ";

                pnlText.setText(
                        prefix +
                        formatNumber(
                                Math.abs(pnlPercent)
                        ) +
                        "%"
                );

            } else {

                pnlText.setText(
                        "—"
                );
            }

            pnlText.setTextSize(14);
            pnlText.setGravity(
                    android.view.Gravity.CENTER
            );

            row.addView(
                    symbolText,
                    new LinearLayout.LayoutParams(
                            0,
                            70,
                            0.28f
                    )
            );

            row.addView(
                    valueText,
                    new LinearLayout.LayoutParams(
                            0,
                            70,
                            0.42f
                    )
            );

            row.addView(
                    pnlText,
                    new LinearLayout.LayoutParams(
                            0,
                            70,
                            0.30f
                    )
            );

            row.setClickable(true);

            row.setOnClickListener(
                    v ->
                            showSymbolTransactionsDialog(
                                    portfolioName,
                                    symbol
                            )
            );

            box.addView(row);

            View line = new View(this);

            box.addView(
                    line,
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1
                    )
            );
        }

        if (!found) {

            TextView empty = new TextView(this);

            empty.setText(
                    "سهم فعالی در این سبد وجود ندارد."
            );

            empty.setTextSize(17);

            box.addView(empty);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle("📁 " + portfolioName)
                .setView(scroll)
                .setPositiveButton("بستن", null)
                .show();
    }

    // =========================================================
    // CURRENT PRICE
    // =========================================================

    private double getCurrentPrice(
            String portfolio,
            String symbol) {

        SharedPreferences preferences =
                getSharedPreferences(
                        "current_prices",
                        MODE_PRIVATE
                );

        String key =
                portfolio +
                "|" +
                symbol;

        String value =
                preferences.getString(
                        key,
                        ""
                );

        if (value.isEmpty()) {
            return 0;
        }

        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private void saveCurrentPrice(
            String portfolio,
            String symbol,
            double price) {

        SharedPreferences preferences =
                getSharedPreferences(
                        "current_prices",
                        MODE_PRIVATE
                );

        preferences.edit()
                .putString(
                        portfolio + "|" + symbol,
                        Double.toString(price)
                )
                .apply();
    }

    // =========================================================
    // SYMBOL DETAILS
    // =========================================================

    private void showSymbolTransactionsDialog(
            String portfolioName,
            String symbolName) {

        Map<String, PortfolioEngine.Position> positions =
                calculatePositions();

        PortfolioEngine.Position foundPosition =
                positions.get(
                        "STOCK|" +
                        portfolioName +
                        "|" +
                        symbolName
                );

        if (foundPosition == null) {

            foundPosition =
                    positions.get(
                            portfolioName +
                            "|" +
                            symbolName
                    );
        }

        final PortfolioEngine.Position position =
                foundPosition;

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                20, 10, 20, 10
        );

        TextView symbolTitle =
                new TextView(this);

        symbolTitle.setText(
                "📈 " + symbolName
        );

        symbolTitle.setTextSize(23);
        symbolTitle.setPadding(0, 0, 0, 15);

        box.addView(symbolTitle);

        TextView quantityText =
                new TextView(this);

        TextView averageText =
                new TextView(this);

        TextView costText =
                new TextView(this);

        TextView currentValueText =
                new TextView(this);

        TextView pnlAmountText =
                new TextView(this);

        TextView pnlPercentText =
                new TextView(this);

        TextView realizedText =
                new TextView(this);

        EditText currentPrice =
                numberField(
                        "قیمت فعلی هر سهم"
                );

        double savedPrice =
                getCurrentPrice(
                        portfolioName,
                        symbolName
                );

        if (savedPrice > 0) {
            currentPrice.setText(
                    formatNumber(savedPrice)
            );
        }

        box.addView(quantityText);
        box.addView(averageText);
        box.addView(costText);

        box.addView(
                makeSpace(8)
        );

        box.addView(
                currentPrice
        );

        box.addView(
                currentValueText
        );

        box.addView(
                pnlAmountText
        );

        box.addView(
                pnlPercentText
        );

        box.addView(
                realizedText
        );

        if (position != null) {

            quantityText.setText(
                    "تعداد: " +
                    formatNumber(
                            position.quantity
                    )
            );

            averageText.setText(
                    "میانگین خرید: " +
                    money(
                            position.averagePrice()
                    ) +
                    " تومان"
            );

            costText.setText(
                    "بهای تمام‌شده: " +
                    money(
                            position.cost
                    ) +
                    " تومان"
            );

            realizedText.setText(
                    "سود/زیان تحقق‌یافته: " +
                    money(
                            position.realizedProfit
                    ) +
                    " تومان"
            );

        } else {

            quantityText.setText("تعداد: 0");
            averageText.setText("میانگین خرید: 0");
            costText.setText("بهای تمام‌شده: 0");
            realizedText.setText(
                    "سود/زیان تحقق‌یافته: 0"
            );
        }

        Runnable calculate = () -> {

            if (position == null) {
                return;
            }

            double cp =
                    number(currentPrice);

            if (cp <= 0) {

                currentValueText.setText(
                        "ارزش فعلی: —"
                );

                pnlAmountText.setText(
                        "سود/زیان: —"
                );

                pnlPercentText.setText(
                        "درصد سود/زیان: —"
                );

                return;
            }

            double value =
                    position.quantity * cp;

            double pnl =
                    value - position.cost;

            double percent =
                    position.cost > 0
                            ? pnl /
                            position.cost *
                            100.0
                            : 0;

            currentValueText.setText(
                    "ارزش فعلی: " +
                    money(value) +
                    " تومان"
            );

            pnlAmountText.setText(
                    (pnl >= 0
                            ? "🟢 سود: "
                            : "🔴 زیان: ") +
                    money(
                            Math.abs(pnl)
                    ) +
                    " تومان"
            );

            pnlPercentText.setText(
                    "درصد سود/زیان: " +
                    formatNumber(
                            percent
                    ) +
                    "%"
            );
        };

        currentPrice.addTextChangedListener(
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
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {

                        calculate.run();
                    }
                }
        );

        calculate.run();

        Button savePrice =
                new Button(this);

        savePrice.setText(
                "💾 ذخیره قیمت فعلی"
        );

        savePrice.setOnClickListener(v -> {

            double cp =
                    number(currentPrice);

            if (cp <= 0) {
                toast("قیمت فعلی معتبر نیست");
                return;
            }

            saveCurrentPrice(
                    portfolioName,
                    symbolName,
                    cp
            );

            calculate.run();

            toast(
                    "قیمت فعلی ذخیره شد"
            );
        });

        box.addView(savePrice);

        Button history =
                new Button(this);

        history.setText(
                "📋 تاریخچه معاملات این نماد"
        );

        history.setOnClickListener(
                v ->
                        showSymbolHistoryDialog(
                                portfolioName,
                                symbolName
                        )
        );

        box.addView(history);

        Button editCurrent =
                new Button(this);

        editCurrent.setText(
                "✏️ مدیریت معاملات"
        );

        editCurrent.setOnClickListener(
                v ->
                        showSymbolManagementDialog(
                                portfolioName,
                                symbolName
                        )
        );

        box.addView(editCurrent);

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle(symbolName)
                .setView(scroll)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    // =========================================================
    // SYMBOL HISTORY
    // =========================================================

    private void showSymbolHistoryDialog(
            String portfolio,
            String symbol) {

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

            String asset =
                    getCursorString(
                            cursor,
                            "asset_type"
                    );

            String type =
                    getCursorString(
                            cursor,
                            "type"
                    );

            String rowPortfolio =
                    getCursorString(
                            cursor,
                            "portfolio"
                    );

            String rowSymbol =
                    getCursorString(
                            cursor,
                            "symbol"
                    );

            if (!"STOCK".equalsIgnoreCase(asset)) {
                continue;
            }

            if (!"BUY".equals(type) &&
                    !"SELL".equals(type)) {
                continue;
            }

            if (!safe(portfolio).equals(
                    safe(rowPortfolio))) {
                continue;
            }

            if (!safe(symbol).equals(
                    safe(rowSymbol))) {
                continue;
            }

            found = true;

            long id =
                    cursor.getLong(
                            cursor.getColumnIndexOrThrow(
                                    "id"
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
                    getCursorString(
                            cursor,
                            "date_shamsi"
                    );

            Button item =
                    new Button(this);

            item.setText(
                    ("BUY".equals(type)
                            ? "🟢 خرید"
                            : "🔴 فروش") +
                    "\nتعداد: " +
                    formatNumber(quantity) +
                    "\nقیمت: " +
                    money(price) +
                    "\nمبلغ: " +
                    money(amount) +
                    "\nکارمزد: " +
                    money(fee) +
                    "\nتاریخ: " +
                    safe(date)
            );

            item.setOnClickListener(
                    v ->
                            showTransactionActionsDialog(
                                    id
                            )
            );

            box.addView(item);
        }

        cursor.close();

        if (!found) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "برای این نماد معامله‌ای وجود ندارد."
            );

            box.addView(empty);
        }

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle(
                        "📋 تاریخچه " + symbol
                )
                .setView(scroll)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    // =========================================================
    // SYMBOL MANAGEMENT
    // =========================================================

    private void showSymbolManagementDialog(
            String portfolio,
            String symbol) {

        Cursor cursor =
                db.getAllTransactions();

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        boolean found = false;

        while (cursor.moveToNext()) {

            String asset =
                    getCursorString(
                            cursor,
                            "asset_type"
                    );

            String type =
                    getCursorString(
                            cursor,
                            "type"
                    );

            String rowPortfolio =
                    getCursorString(
                            cursor,
                            "portfolio"
                    );

            String rowSymbol =
                    getCursorString(
                            cursor,
                            "symbol"
                    );

            if (!"STOCK".equalsIgnoreCase(asset)) {
                continue;
            }

            if (!"BUY".equals(type) &&
                    !"SELL".equals(type)) {
                continue;
            }

            if (!safe(portfolio).equals(
                    safe(rowPortfolio))) {
                continue;
            }

            if (!safe(symbol).equals(
                    safe(rowSymbol))) {
                continue;
            }

            found = true;

            long id =
                    cursor.getLong(
                            cursor.getColumnIndexOrThrow(
                                    "id"
                            )
                    );

            Button b =
                    new Button(this);

            b.setText(
                    ("BUY".equals(type)
                            ? "🟢 خرید "
                            : "🔴 فروش ") +
                    money(
                            cursor.getDouble(
                                    cursor.getColumnIndexOrThrow(
                                            "amount"
                                    )
                            )
                    )
            );

            b.setOnClickListener(
                    v ->
                            showTransactionActionsDialog(
                                    id
                            )
            );

            box.addView(b);
        }

        cursor.close();

        if (!found) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "معامله‌ای پیدا نشد."
            );

            box.addView(empty);
        }

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle(
                        "✏️ مدیریت " + symbol
                )
                .setView(scroll)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    // =========================================================
    // GENERAL HISTORY
    // =========================================================

    private void showHistoryDialog(
            String assetType) {

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

            String rowAssetType =
                    getCursorString(
                            cursor,
                            "asset_type"
                    );

            String type =
                    getCursorString(
                            cursor,
                            "type"
                    );

            if ("OPTION".equals(assetType)) {

                if (!"OPTION".equalsIgnoreCase(
                        rowAssetType)) {
                    continue;
                }

            } else {

                if (!"STOCK".equalsIgnoreCase(
                        rowAssetType)) {
                    continue;
                }

                if (!"BUY".equals(type) &&
                        !"SELL".equals(type)) {
                    continue;
                }
            }

            found = true;

            final long id =
                    cursor.getLong(
                            cursor.getColumnIndexOrThrow(
                                    "id"
                            )
                    );

            String symbol =
                    getCursorString(
                            cursor,
                            "symbol"
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
                    getCursorString(
                            cursor,
                            "date_shamsi"
                    );

            Button item =
                    new Button(this);

            if ("OPTION".equals(assetType)) {

                String position =
                        getCursorString(
                                cursor,
                                "position_type"
                        );

                String optionType =
                        getCursorString(
                                cursor,
                                "option_type"
                        );

                String underlying =
                        getCursorString(
                                cursor,
                                "underlying"
                        );

                item.setText(
                        ("BUY".equals(type)
                                ? "🟢 خرید"
                                : "🔴 فروش") +
                        " | " +
                        safe(symbol) +
                        "\nپوزیشن: " +
                        safe(position) +
                        "\nCALL/PUT: " +
                        safe(optionType) +
                        "\nپایه: " +
                        safe(underlying) +
                        "\nتعداد: " +
                        formatNumber(quantity) +
                        "\nپرمیوم: " +
                        money(price) +
                        "\nمبلغ: " +
                        money(amount) +
                        "\nکارمزد: " +
                        money(fee) +
                        "\nتاریخ: " +
                        safe(date)
                );

            } else {

                item.setText(
                        ("BUY".equals(type)
                                ? "🟢 خرید"
                                : "🔴 فروش") +
                        " | " +
                        safe(symbol) +
                        "\nتعداد: " +
                        formatNumber(quantity) +
                        "\nقیمت: " +
                        money(price) +
                        "\nمبلغ: " +
                        money(amount) +
                        "\nکارمزد: " +
                        money(fee) +
                        "\nتاریخ: " +
                        safe(date)
                );
            }

            item.setOnClickListener(
                    v ->
                            showTransactionActionsDialog(
                                    id
                            )
            );

            box.addView(item);
        }

        cursor.close();

        if (!found) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "تراکنشی وجود ندارد."
            );

            box.addView(empty);
        }

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle(
                        "OPTION".equals(assetType)
                                ? "📋 تاریخچه آپشن"
                                : "📋 تاریخچه سهام"
                )
                .setView(scroll)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    // =========================================================
    // TRANSACTION ACTIONS
    // =========================================================

    private void showTransactionActionsDialog(
            long id) {

        Cursor cursor =
                findTransaction(id);

        if (cursor == null) {
            toast("تراکنش پیدا نشد");
            return;
        }

        String type =
                getCursorString(cursor, "type");

        String asset =
                getCursorString(
                        cursor,
                        "asset_type"
                );

        cursor.close();

        new AlertDialog.Builder(this)
                .setTitle("مدیریت تراکنش")
                .setItems(
                        new String[]{
                                "✏️ ویرایش",
                                "🗑 حذف",
                                "انصراف"
                        },
                        (dialog, which) -> {

                            if (which == 0) {

                                if ("DEPOSIT".equals(type) ||
                                        "WITHDRAW".equals(type)) {

                                    showEditMoneyDialog(id);

                                } else if ("OPTION".equalsIgnoreCase(
                                        asset)) {

                                    showEditOptionDialog(id);

                                } else {

                                    showEditStockDialog(id);
                                }

                            } else if (which == 1) {

                                confirmDeleteTransaction(id);
                            }
                        }
                )
                .show();
    }

    // =========================================================
    // FIND TRANSACTION
    // =========================================================

    private Cursor findTransaction(long id) {

        Cursor cursor =
                db.getAllTransactions();

        int idIndex =
                cursor.getColumnIndex("id");

        if (idIndex < 0) {
            cursor.close();
            return null;
        }

        while (cursor.moveToNext()) {

            if (cursor.getLong(idIndex) == id) {
                return cursor;
            }
        }

        cursor.close();

        return null;
    }

    // =========================================================
    // DELETE
    // =========================================================

    private void confirmDeleteTransaction(
            long id) {

        new AlertDialog.Builder(this)
                .setTitle("🗑 حذف تراکنش")
                .setMessage(
                        "آیا از حذف این تراکنش مطمئن هستید؟"
                )
                .setNegativeButton(
                        "انصراف",
                        null
                )
                .setPositiveButton(
                        "حذف",
                        (dialog, which) -> {

                            db.deleteTransaction(id);

                            toast("تراکنش حذف شد");

                            buildMainScreen();
                        }
                )
                .show();
    }

    // =========================================================
    // EDIT MONEY
    // =========================================================

    private void showEditMoneyDialog(long id) {

        Cursor cursor =
                findTransaction(id);

        if (cursor == null) {
            toast("تراکنش پیدا نشد");
            return;
        }

        String type =
                getCursorString(cursor, "type");

        String portfolioValue =
                getCursorString(
                        cursor,
                        "portfolio"
                );

        double amountValue =
                cursor.getDouble(
                        cursor.getColumnIndexOrThrow(
                                "amount"
                        )
                );

        String descriptionValue =
                getCursorString(
                        cursor,
                        "description"
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
                portfolioValue
        );

        EditText amount =
                numberField("مبلغ");

        amount.setText(
                formatNumber(amountValue)
        );

        EditText description =
                field("توضیحات");

        description.setText(
                descriptionValue
        );

        box.addView(portfolio);
        box.addView(amount);
        box.addView(description);

        String title =
                "DEPOSIT".equals(type)
                        ? "✏️ ویرایش واریزی"
                        : "✏️ ویرایش برداشت";

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(title)
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

                /*
                 * DatabaseHelper فعلی متد اختصاصی
                 * updateMoneyTransaction ندارد.
                 *
                 * بنابراین تراکنش قبلی حذف و
                 * همان تراکنش مالی دوباره ثبت می‌شود.
                 */

                db.deleteTransaction(id);

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

                toast("واریز/برداشت ویرایش شد");

                dialog.dismiss();

                buildMainScreen();
            });
        });

        dialog.show();
    }

    // =========================================================
    // EDIT STOCK
    // =========================================================

    private void showEditStockDialog(long id) {

        Cursor cursor =
                findTransaction(id);

        if (cursor == null) {
            toast("تراکنش پیدا نشد");
            return;
        }

        String type =
                getCursorString(cursor, "type");

        String portfolioValue =
                getCursorString(cursor, "portfolio");

        String brokerValue =
                getCursorString(cursor, "broker");

        String symbolValue =
                getCursorString(cursor, "symbol");

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
                getCursorString(
                        cursor,
                        "description"
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
                portfolioValue
        );

        EditText broker =
                field("کارگزاری");

        broker.setText(
                brokerValue
        );

        EditText symbol =
                field("نماد");

        symbol.setText(
                symbolValue
        );

        EditText quantity =
                numberField("تعداد");

        quantity.setText(
                formatNumber(quantityValue)
        );

        EditText price =
                numberField("قیمت");

        price.setText(
                formatNumber(priceValue)
        );

        EditText total =
                numberField("مبلغ کل");

        total.setText(
                formatNumber(
                        quantityValue *
                        priceValue
                )
        );

        EditText description =
                field("توضیحات");

        description.setText(
                descriptionValue
        );

        box.addView(portfolio);
        box.addView(broker);
        box.addView(symbol);
        box.addView(quantity);
        box.addView(price);
        box.addView(total);
        box.addView(description);

        addAutoCalculation(
                quantity,
                price,
                total
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "BUY".equals(type)
                                        ? "✏️ ویرایش خرید"
                                        : "✏️ ویرایش فروش"
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

                String p =
                        portfolio.getText()
                                .toString()
                                .trim();

                String b =
                        broker.getText()
                                .toString()
                                .trim();

                String s =
                        symbol.getText()
                                .toString()
                                .trim();

                double q =
                        number(quantity);

                double priceValueNew =
                        number(price);

                if (p.isEmpty()) {
                    p = "اصلی";
                }

                if (s.isEmpty()) {
                    toast("نماد را وارد کنید");
                    return;
                }

                if (q <= 0) {
                    toast("تعداد معتبر نیست");
                    return;
                }

                if (priceValueNew <= 0) {
                    toast("قیمت معتبر نیست");
                    return;
                }

                double amount =
                        q * priceValueNew;

                double fee =
                        "BUY".equals(type)
                                ? amount *
                                BUY_FEE_RATE
                                : amount *
                                SELL_FEE_RATE;

                db.updateTransaction(
                        id,
                        type,
                        p,
                        b,
                        s,
                        q,
                        priceValueNew,
                        fee,
                        amount,
                        description.getText()
                                .toString()
                                .trim()
                );

                toast("معامله ویرایش شد");

                dialog.dismiss();

                buildMainScreen();
            });
        });

        dialog.show();
    }

    // =========================================================
    // EDIT OPTION
    // =========================================================

    private void showEditOptionDialog(long id) {

        Cursor cursor =
                findTransaction(id);

        if (cursor == null) {
            toast("تراکنش پیدا نشد");
            return;
        }

        String type =
                getCursorString(cursor, "type");

        String portfolioValue =
                getCursorString(cursor, "portfolio");

        String brokerValue =
                getCursorString(cursor, "broker");

        String symbolValue =
                getCursorString(cursor, "symbol");

        String underlyingValue =
                getCursorString(cursor, "underlying");

        String optionTypeValue =
                getCursorString(cursor, "option_type");

        String positionTypeValue =
                getCursorString(cursor, "position_type");

        double strikeValue =
                cursor.getDouble(
                        cursor.getColumnIndexOrThrow(
                                "strike_price"
                        )
                );

        String expiryValue =
                getCursorString(
                        cursor,
                        "expiry_date"
                );

        double contractSizeValue =
                cursor.getDouble(
                        cursor.getColumnIndexOrThrow(
                                "contract_size"
                        )
                );

        double quantityValue =
                cursor.getDouble(
                        cursor.getColumnIndexOrThrow(
                                "quantity"
                        )
                );

        double premiumValue =
                cursor.getDouble(
                        cursor.getColumnIndexOrThrow(
                                "price"
                        )
                );

        String descriptionValue =
                getCursorString(
                        cursor,
                        "description"
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
                portfolioValue
        );

        EditText broker =
                field("کارگزاری");

        broker.setText(
                brokerValue
        );

        EditText symbol =
                field("نماد آپشن");

        symbol.setText(
                symbolValue
        );

        EditText underlying =
                field("دارایی پایه");

        underlying.setText(
                underlyingValue
        );

        Spinner optionType =
                new Spinner(this);

        ArrayAdapter<String> optionAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        new String[]{
                                "CALL",
                                "PUT"
                        }
                );

        optionAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        optionType.setAdapter(optionAdapter);

        optionType.setSelection(
                "PUT".equalsIgnoreCase(
                        optionTypeValue
                )
                        ? 1
                        : 0
        );

        Spinner positionType =
                new Spinner(this);

        ArrayAdapter<String> positionAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        new String[]{
                                "LONG",
                                "SHORT"
                        }
                );

        positionAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        positionType.setAdapter(positionAdapter);

        positionType.setSelection(
                "SHORT".equalsIgnoreCase(
                        positionTypeValue
                )
                        ? 1
                        : 0
        );

        EditText strike =
                numberField("قیمت اعمال");

        strike.setText(
                formatNumber(strikeValue)
        );

        EditText expiry =
                field("تاریخ سررسید");

        expiry.setText(
                expiryValue
        );

        EditText contractSize =
                numberField("اندازه قرارداد");

        contractSize.setText(
                formatNumber(contractSizeValue)
        );

        EditText quantity =
                numberField("تعداد قرارداد");

        quantity.setText(
                formatNumber(quantityValue)
        );

        EditText premium =
                numberField("پرمیوم");

        premium.setText(
                formatNumber(premiumValue)
        );

        EditText description =
                field("توضیحات");

        description.setText(
                descriptionValue
        );

        box.addView(portfolio);
        box.addView(broker);
        box.addView(symbol);
        box.addView(underlying);
        box.addView(optionType);
        box.addView(positionType);
        box.addView(strike);
        box.addView(expiry);
        box.addView(contractSize);
        box.addView(quantity);
        box.addView(premium);
        box.addView(description);

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                "✏️ ویرایش معامله آپشن"
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

                String p =
                        portfolio.getText()
                                .toString()
                                .trim();

                String b =
                        broker.getText()
                                .toString()
                                .trim();

                String s =
                        symbol.getText()
                                .toString()
                                .trim();

                String u =
                        underlying.getText()
                                .toString()
                                .trim();

                String ot =
                        optionType.getSelectedItem()
                                .toString();

                String pt =
                        positionType.getSelectedItem()
                                .toString();

                double strikeNew =
                        number(strike);

                String expiryNew =
                        expiry.getText()
                                .toString()
                                .trim();

                double cs =
                        number(contractSize);

                double q =
                        number(quantity);

                double prem =
                        number(premium);

                if (p.isEmpty()) {
                    p = "اصلی";
                }

                if (s.isEmpty()) {
                    toast("نماد آپشن را وارد کنید");
                    return;
                }

                if (u.isEmpty()) {
                    toast("دارایی پایه را وارد کنید");
                    return;
                }

                if (strikeNew <= 0) {
                    toast("Strike معتبر نیست");
                    return;
                }

                if (cs <= 0) {
                    toast("اندازه قرارداد معتبر نیست");
                    return;
                }

                if (q <= 0) {
                    toast("تعداد قرارداد معتبر نیست");
                    return;
                }

                if (prem <= 0) {
                    toast("پرمیوم معتبر نیست");
                    return;
                }

                db.updateOptionTransaction(
                        id,
                        type,
                        p,
                        b,
                        s,
                        u,
                        ot,
                        strikeNew,
                        expiryNew,
                        q,
                        prem,
                        cs,
                        pt,
                        description.getText()
                                .toString()
                                .trim()
                );

                toast(
                        "معامله آپشن ویرایش شد"
                );

                dialog.dismiss();

                buildMainScreen();
            });
        });

        dialog.show();
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
                .setTitle("🔎 جستجو")
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
                    getCursorString(
                            cursor,
                            "type"
                    );

            String symbol =
                    getCursorString(
                            cursor,
                            "symbol"
                    );

            double amount =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "amount"
                            )
                    );

            String date =
                    getCursorString(
                            cursor,
                            "date_shamsi"
                    );

            Button item =
                    new Button(this);

            item.setText(
                    safe(date) +
                    "\n" +
                    ("BUY".equals(type)
                            ? "🟢 خرید"
                            : "SELL".equals(type)
                            ? "🔴 فروش"
                            : "💰 " + type) +
                    " | " +
                    safe(symbol) +
                    "\nمبلغ: " +
                    money(amount)
            );

            item.setOnClickListener(
                    v ->
                            showTransactionActionsDialog(
                                    id
                            )
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
                .setTitle("💵 موجودی نقدی")
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
    // POSITION CALCULATION
    // =========================================================

    private Map<String, PortfolioEngine.Position>
    calculatePositions() {

        Cursor cursor =
                db.getAllTransactions();

        Map<String, PortfolioEngine.Position> result =
                PortfolioEngine.calculate(cursor);

        cursor.close();

        return result;
    }

    // =========================================================
    // UI HELPERS
    // =========================================================

    private EditText field(String hint) {

        EditText e =
                new EditText(this);

        e.setHint(hint);
        e.setSingleLine(true);

        e.setPadding(
                16, 12, 16, 12
        );

        return e;
    }

    private EditText numberField(String hint) {

        EditText e =
                field(hint);

        addThousandsFormatter(e);

        return e;
    }

    private TextView makeSpace(int height) {

        TextView space =
                new TextView(this);

        space.setHeight(height);

        return space;
    }

    // =========================================================
    // NUMBER FORMATTER
    // =========================================================

    private void addThousandsFormatter(
            EditText editText) {

        editText.addTextChangedListener(
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

                        String clean =
                                cleanNumberText(
                                        current
                                );

                        if (clean.isEmpty()) {
                            return;
                        }

                        String formatted =
                                formatInputNumber(
                                        clean
                                );

                        if (formatted.equals(
                                current
                        )) {
                            return;
                        }

                        formattingNumber = true;

                        try {

                            editText.setText(
                                    formatted
                            );

                            editText.setSelection(
                                    formatted.length()
                            );

                        } finally {

                            formattingNumber = false;
                        }
                    }
                }
        );
    }

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

    private String formatInputNumber(
            String clean) {

        boolean negative =
                clean.startsWith("-");

        if (negative) {
            clean = clean.substring(1);
        }

        String decimal = "";

        int dot =
                clean.indexOf('.');

        if (dot >= 0) {

            decimal =
                    clean.substring(dot);

            clean =
                    clean.substring(0, dot);
        }

        if (clean.isEmpty()) {
            clean = "0";
        }

        while (clean.length() > 1 &&
                clean.startsWith("0")) {

            clean =
                    clean.substring(1);
        }

        StringBuilder result =
                new StringBuilder();

        int first =
                clean.length() % 3;

        if (first == 0) {
            first = 3;
        }

        result.append(
                clean.substring(
                        0,
                        first
                )
        );

        for (int i = first;
             i < clean.length();
             i += 3) {

            result.append(",");

            result.append(
                    clean.substring(
                            i,
                            Math.min(
                                    i + 3,
                                    clean.length()
                            )
                    )
            );
        }

        if (!decimal.isEmpty()) {
            result.append(decimal);
        }

        if (negative) {
            result.insert(0, "-");
        }

        return result.toString();
    }

    private void setFormattedText(
            EditText field,
            double value) {

        String newValue =
                formatNumber(value);

        if (field.getText()
                .toString()
                .equals(newValue)) {
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

    private double number(
            EditText field) {

        try {

            String value =
                    cleanNumberText(
                            field.getText()
                                    .toString()
                    );

            if (value.isEmpty()) {
                return 0;
            }

            return Double.parseDouble(value);

        } catch (Exception e) {

            return 0;
        }
    }

    private String money(
            double value) {

        return String.format(
                Locale.US,
                "%,.0f",
                value
        );
    }

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
    // CURSOR HELPERS
    // =========================================================

    private String getCursorString(
            Cursor cursor,
            String column) {

        int index =
                cursor.getColumnIndex(column);

        if (index < 0 ||
                cursor.isNull(index)) {

            return "";
        }

        return cursor.getString(index);
    }

    // =========================================================
    // GENERAL HELPERS
    // =========================================================

    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }

    private void toast(
            String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    private void toastLong(
            String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
