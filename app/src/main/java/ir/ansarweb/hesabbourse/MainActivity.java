package ir.ansarweb.hesabbourse;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
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

    // کارمزد آپشن
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
        history.setText("📋 تاریخچه معاملات");
        history.setOnClickListener(v -> showHistoryDialog("STOCK"));
        content.addView(history);

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
        optionBuy.setOnClickListener(
                v -> showOptionDialog("BUY")
        );
        content.addView(optionBuy);

        Button optionSell = new Button(this);
        optionSell.setText("🔴 فروش آپشن");
        optionSell.setOnClickListener(
                v -> showOptionDialog("SELL")
        );
        content.addView(optionSell);

        Button optionHistory = new Button(this);
        optionHistory.setText("📋 تاریخچه معاملات آپشن");
        optionHistory.setOnClickListener(
                v -> showHistoryDialog("OPTION")
        );
        content.addView(optionHistory);

        Button optionPositions = new Button(this);
        optionPositions.setText("📊 وضعیت پوزیشن‌های آپشن");
        optionPositions.setOnClickListener(
                v -> showOptionPositions()
        );
        content.addView(optionPositions);

        Button optionRealized = new Button(this);
        optionRealized.setText("💰 سود/زیان تحقق‌یافته آپشن");
        optionRealized.setOnClickListener(
                v -> showOptionRealizedProfit()
        );
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

        TextView optionTypeLabel =
                new TextView(this);

        optionTypeLabel.setText("نوع آپشن");
        optionTypeLabel.setPadding(0, 10, 0, 5);

        Spinner optionType = new Spinner(this);

        ArrayAdapter<String> optionTypeAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        new String[]{
                                "CALL",
                                "PUT"
                        }
                );

        optionTypeAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        optionType.setAdapter(optionTypeAdapter);

        TextView positionTypeLabel =
                new TextView(this);

        positionTypeLabel.setText("نوع پوزیشن");
        positionTypeLabel.setPadding(0, 10, 0, 5);

        Spinner positionType = new Spinner(this);

        ArrayAdapter<String> positionTypeAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        new String[]{
                                "LONG",
                                "SHORT"
                        }
                );

        positionTypeAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        positionType.setAdapter(positionTypeAdapter);

        EditText strike =
                numberField("قیمت اعمال (Strike)");

        EditText expiry =
                field("تاریخ سررسید");

        EditText contractSize =
                numberField("اندازه قرارداد");

        contractSize.setText("1000");

        EditText quantity =
                numberField("تعداد قرارداد");

        EditText premium =
                numberField("پرمیوم هر واحد");

        EditText amount =
                numberField("مبلغ کل پرمیوم");

        TextView feeText =
                new TextView(this);

        feeText.setText(
                "کارمزد: 0 تومان"
        );

        feeText.setTextSize(16);
        feeText.setPadding(
                0, 15, 0, 15
        );

        EditText description =
                field("توضیحات");

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

    // =========================================================
    // OPTION CALCULATION
    // =========================================================

    private void addOptionCalculation(
            EditText quantity,
            EditText premium,
            EditText contractSize,
            EditText amount,
            TextView feeText) {

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
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {

                        if (calculatingFields ||
                                formattingNumber) {
                            return;
                        }

                        double q = number(quantity);
                        double p = number(premium);
                        double cs = number(contractSize);

                        try {

                            calculatingFields = true;

                            if (q > 0 &&
                                    p > 0 &&
                                    cs > 0) {

                                double total =
                                        q * p * cs;

                                double fee =
                                        p * cs *
                                        OPTION_FEE_RATE *
                                        q;

                                setFormattedText(
                                        amount,
                                        total
                                );

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

        String underlying =
                underlyingField.getText()
                        .toString()
                        .trim();

        String optionType =
                optionTypeSpinner
                        .getSelectedItem()
                        .toString();

        String positionType =
                positionTypeSpinner
                        .getSelectedItem()
                        .toString();

        double strike =
                number(strikeField);

        String expiry =
                expiryField.getText()
                        .toString()
                        .trim();

        double contractSize =
                number(contractSizeField);

        double quantity =
                number(quantityField);

        double premium =
                number(premiumField);

        String description =
                descriptionField.getText()
                        .toString()
                        .trim();

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

        /*
         * مهم:
         *
         * SELL + SHORT
         * بدون نیاز به موجودی قبلی مجاز است.
         *
         * SELL + LONG
         * باید پوزیشن لانگ موجود باشد.
         *
         * BUY + SHORT
         * باید پوزیشن شورت موجود باشد.
         *
         * BUY + LONG
         * بدون نیاز به موجودی قبلی مجاز است.
         */

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

            if (quantity >
                    available + 0.0000001) {

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

            if (quantity >
                    available + 0.0000001) {

                toastLong(
                        "بستن شورت ثبت نشد.\n" +
                        "پوزیشن شورت موجود: " +
                        formatNumber(available)
                );

                return false;
            }
        }

        /*
         * SELL + SHORT
         * آزاد است؛ حتی اگر موجودی قبلی صفر باشد.
         */

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

            action =
                    "شورت جدید باز شد";

        } else if ("BUY".equals(type) &&
                "SHORT".equals(positionType)) {

            action =
                    "شورت بسته شد";

        } else if ("BUY".equals(type)) {

            action =
                    "پوزیشن لانگ باز شد";

        } else {

            action =
                    "پوزیشن لانگ کاهش یافت";
        }

        toastLong(
                "✅ " +
                action +
                "\n" +
                "مبلغ پرمیوم: " +
                money(amount) +
                " تومان\n" +
                "کارمزد: " +
                money(fee) +
                " تومان"
        );

        return true;
    }

    // =========================================================
    // FIND OPTION POSITION
    // =========================================================

    private PortfolioEngine.Position
    findOptionPosition(
            String portfolio,
            String underlying,
            String optionType,
            double strike,
            String expiry,
            double contractSize) {

        Map<String,
                PortfolioEngine.Position> positions =
                calculatePositions();

        for (PortfolioEngine.Position p :
                positions.values()) {

            if (!"OPTION".equalsIgnoreCase(
                    p.assetType)) {
                continue;
            }

            if (!safe(p.portfolio).equals(
                    safe(portfolio))) {
                continue;
            }

            if (!safe(p.underlying).equals(
                    safe(underlying))) {
                continue;
            }

            if (!safe(p.optionType).equals(
                    safe(optionType))) {
                continue;
            }

            if (!safe(p.expiryDate).equals(
                    safe(expiry))) {
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

        Map<String,
                PortfolioEngine.Position> positions =
                calculatePositions();

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                20, 10, 20, 10
        );

        boolean found = false;

        for (PortfolioEngine.Position p :
                positions.values()) {

            if (!"OPTION".equalsIgnoreCase(
                    p.assetType)) {
                continue;
            }

            if (p.longQuantity <= 0 &&
                    p.shortQuantity <= 0) {
                continue;
            }

            found = true;

            TextView item =
                    new TextView(this);

            StringBuilder text =
                    new StringBuilder();

            text.append(
                    "🔵 "
            );

            text.append(
                    safe(p.symbol)
            );

            text.append(
                    "\nدارایی پایه: "
            );

            text.append(
                    safe(p.underlying)
            );

            text.append(
                    "\nنوع: "
            );

            text.append(
                    safe(p.optionType)
            );

            text.append(
                    "\nStrike: "
            );

            text.append(
                    money(p.strikePrice)
            );

            text.append(
                    "\nسررسید: "
            );

            text.append(
                    safe(p.expiryDate)
            );

            if (p.longQuantity > 0) {

                text.append(
                        "\n🟢 لانگ: "
                );

                text.append(
                        formatNumber(
                                p.longQuantity
                        )
                );

                text.append(
                        " قرارداد"
                );

                text.append(
                        "\nمیانگین لانگ: "
                );

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

                text.append(
                        "\n🔴 شورت: "
                );

                text.append(
                        formatNumber(
                                p.shortQuantity
                        )
                );

                text.append(
                        " قرارداد"
                );
            }

            item.setText(
                    text.toString()
            );

            item.setTextSize(16);

            item.setPadding(
                    0, 15, 0, 15
            );

            box.addView(item);
        }

        if (!found) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "در حال حاضر پوزیشن آپشن باز ندارید."
            );

            empty.setTextSize(17);

            box.addView(empty);
        }

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle(
                        "📊 پوزیشن‌های باز آپشن"
                )
                .setView(scroll)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    // =========================================================
    // OPTION REALIZED PROFIT
    // =========================================================

    private void showOptionRealizedProfit() {

        Map<String,
                PortfolioEngine.Position> positions =
                calculatePositions();

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                20, 10, 20, 10
        );

        boolean found = false;

        double totalProfit = 0;

        for (PortfolioEngine.Position p :
                positions.values()) {

            if (!"OPTION".equalsIgnoreCase(
                    p.assetType)) {
                continue;
            }

            if (!p.hasRealizedProfit()) {
                continue;
            }

            found = true;

            totalProfit +=
                    p.realizedProfit;

            TextView item =
                    new TextView(this);

            item.setText(
                    "🔵 " +
                    safe(p.symbol) +
                    "\nدارایی پایه: " +
                    safe(p.underlying) +
                    "\n" +
                    (p.realizedProfit >= 0
                            ? "🟢 سود تحقق‌یافته: "
                            : "🔴 زیان تحقق‌یافته: ") +
                    money(
                            p.realizedProfit
                    ) +
                    " تومان" +
                    "\nدرصد: " +
                    formatNumber(
                            p.realizedProfitPercent()
                    ) +
                    "%"
            );

            item.setTextSize(16);

            item.setPadding(
                    0, 15, 0, 15
            );

            box.addView(item);
        }

        if (!found) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "هنوز سود/زیان تحقق‌یافته‌ای برای آپشن ثبت نشده است."
            );

            box.addView(empty);

        } else {

            TextView total =
                    new TextView(this);

            total.setText(
                    "\nجمع سود/زیان تحقق‌یافته:\n" +
                    money(totalProfit) +
                    " تومان"
            );

            total.setTextSize(18);

            total.setPadding(
                    0, 20, 0, 10
            );

            box.addView(total);
        }

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle(
                        "💰 سود/زیان تحقق‌یافته آپشن"
                )
                .setView(scroll)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    // =========================================================
    // STOCK REALIZED PROFIT
    // =========================================================

    private void showRealizedProfitDialog() {

        Map<String,
                PortfolioEngine.Position> positions =
                calculatePositions();

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                20, 10, 20, 10
        );

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

            total +=
                    p.realizedProfit;

            TextView item =
                    new TextView(this);

            item.setText(
                    "📈 " +
                    p.symbol +
                    "\n" +
                    (p.realizedProfit >= 0
                            ? "🟢 سود تحقق‌یافته: "
                            : "🔴 زیان تحقق‌یافته: ") +
                    money(
                            p.realizedProfit
                    ) +
                    " تومان" +
                    "\nدرصد: " +
                    formatNumber(
                            p.realizedProfitPercent()
                    ) +
                    "%"
            );

            item.setTextSize(16);

            item.setPadding(
                    0, 15, 0, 15
            );

            box.addView(item);
        }

        if (!found) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "هنوز سود/زیان تحقق‌یافته‌ای ثبت نشده است."
            );

            box.addView(empty);

        } else {

            TextView totalText =
                    new TextView(this);

            totalText.setText(
                    "\nجمع کل:\n" +
                    money(total) +
                    " تومان"
            );

            totalText.setTextSize(18);

            box.addView(totalText);
        }

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle(
                        "💰 سود/زیان تحقق‌یافته سهام"
                )
                .setView(scroll)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    // =========================================================
    // BACKUP MENU
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

    // =========================================================
    // BACKUP EXPORT
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
                new Intent(
                        Intent.ACTION_CREATE_DOCUMENT
                );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType(
                "application/json"
        );

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
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

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

        Uri uri =
                data.getData();

        if (requestCode ==
                REQUEST_EXPORT_BACKUP) {

            exportBackupToUri(uri);

        } else if (requestCode ==
                REQUEST_IMPORT_BACKUP) {

            confirmBackupImport(uri);
        }
    }

    private void exportBackupToUri(
            Uri uri) {

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

    private void confirmBackupImport(
            Uri uri) {

        new AlertDialog.Builder(this)
                .setTitle(
                        "⚠️ بازیابی بک‌آپ"
                )
                .setMessage(
                        "با بازیابی این فایل، " +
                        "تراکنش‌های فعلی حذف و اطلاعات " +
                        "بک‌آپ جایگزین می‌شوند.\n\n" +
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

    private void importBackupFromUri(
            Uri uri) {

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

            while ((line =
                    reader.readLine()) != null) {

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

    private void showMoneyDialog(
            String type) {

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
                        ? "💰 ثبت واریزی"
                        : "💸 ثبت برداشت";

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
    // STOCK TRADE
    // =========================================================

    private void showTradeDialog(
            String type) {

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

        EditText broker =
                field("کارگزاری");

        EditText symbol =
                field("نماد");

        EditText quantity =
                numberField("تعداد سهم");

        EditText price =
                numberField("قیمت هر سهم");

        EditText total =
                numberField("مبلغ کل معامله");

        EditText description =
                field("توضیحات");

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
    // STOCK AUTO CALCULATION
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

        double quantity =
                number(quantityField);

        double price =
                number(priceField);

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

            Map<String,
                    PortfolioEngine.Position> positions =
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

        toast(
                "معامله با موفقیت ثبت شد"
        );

        return true;
    }

    // =========================================================
    // PORTFOLIO
    // =========================================================

    private void showPortfolioDialog() {

        Map<String,
                PortfolioEngine.Position> positions =
                calculatePositions();

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setPadding(
                20, 10, 20, 10
        );

        Map<String, Boolean> portfolios =
                new LinkedHashMap<>();

        for (PortfolioEngine.Position p :
                positions.values()) {

            if (p.isStock() &&
                    p.quantity > 0) {

                portfolios.put(
                        p.portfolio,
                        true
                );
            }
        }

        if (portfolios.isEmpty()) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "در حال حاضر سهم فعالی در سبدها وجود ندارد."
            );

            box.addView(empty);

        } else {

            for (String portfolio :
                    portfolios.keySet()) {

                Button b =
                        new Button(this);

                b.setText(
                        "📁 " + portfolio
                );

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
                .setTitle(
                        "📊 وضعیت سبدها"
                )
                .setView(box)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    private void showPortfolioSymbolsDialog(
            String portfolioName) {

        Map<String,
                PortfolioEngine.Position> positions =
                calculatePositions();

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        boolean found = false;

        for (PortfolioEngine.Position p :
                positions.values()) {

            if (!p.isStock()) {
                continue;
            }

            if (!portfolioName.equals(
                    p.portfolio)) {
                continue;
            }

            if (p.quantity <= 0) {
                continue;
            }

            found = true;

            Button b =
                    new Button(this);

            b.setText(
                    p.symbol +
                    "\nتعداد: " +
                    formatNumber(p.quantity) +
                    "\nمیانگین: " +
                    money(p.averagePrice())
            );

            String symbol =
                    p.symbol;

            b.setOnClickListener(
                    v ->
                            showSymbolTransactionsDialog(
                                    portfolioName,
                                    symbol
                            )
            );

            box.addView(b);
        }

        if (!found) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "سهم فعالی در این سبد وجود ندارد."
            );

            box.addView(empty);
        }

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle(
                        "📁 " + portfolioName
                )
                .setView(scroll)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    // =========================================================
    // STOCK SYMBOL DETAILS
    // =========================================================

    private void showSymbolTransactionsDialog(
            String portfolioName,
            String symbolName) {

        Map<String,
                PortfolioEngine.Position> positions =
                calculatePositions();

        PortfolioEngine.Position position =
                positions.get(
                        "STOCK|" +
                        portfolioName +
                        "|" +
                        symbolName
                );

        if (position == null) {

            position =
                    positions.get(
                            portfolioName +
                            "|" +
                            symbolName
                    );
        }

        StringBuilder text =
                new StringBuilder();

        text.append(
                "نماد: "
        );

        text.append(symbolName);

        if (position != null) {

            text.append(
                    "\nتعداد فعلی: "
            );

            text.append(
                    formatNumber(
                            position.quantity
                    )
            );

            text.append(
                    "\nبهای تمام‌شده: "
            );

            text.append(
                    money(position.cost)
            );

            text.append(
                    " تومان"
            );

            text.append(
                    "\nمیانگین خرید: "
            );

            text.append(
                    money(
                            position.averagePrice()
                    )
            );

            text.append(
                    " تومان"
            );

            text.append(
                    "\nسود/زیان تحقق‌یافته: "
            );

            text.append(
                    money(
                            position.realizedProfit
                    )
            );

            text.append(
                    " تومان"
            );
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        symbolName
                )
                .setMessage(
                        text.toString()
                )
                .setPositiveButton(
                        "باشه",
                        null
                )
                .show();
    }

    // =========================================================
    // HISTORY
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
                    cursor.getColumnIndex(
                            "asset_type"
                    ) >= 0
                            ? cursor.getString(
                            cursor.getColumnIndex(
                                    "asset_type"
                            )
                    )
                            : "STOCK";

            if ("OPTION".equals(assetType)) {

                if (!"OPTION".equalsIgnoreCase(
                        rowAssetType)) {
                    continue;
                }

            } else {

                if ("OPTION".equalsIgnoreCase(
                        rowAssetType)) {
                    continue;
                }
            }

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
                    safe(date) +
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

        StringBuilder text =
                new StringBuilder();

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

            String date =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "date_shamsi"
                            )
                    );

            text.append(
                    safe(date)
            );

            text.append("\n");

            text.append(
                    "BUY".equals(type)
                            ? "🟢 خرید"
                            : "SELL".equals(type)
                            ? "🔴 فروش"
                            : "💰 مالی"
            );

            text.append(
                    " | "
            );

            text.append(
                    safe(symbol)
            );

            text.append(
                    "\nمبلغ: "
            );

            text.append(
                    money(amount)
            );

            text.append(
                    "\n──────────────────\n"
            );
        }

        cursor.close();

        if (!found) {

            text.append(
                    "نتیجه‌ای پیدا نشد."
            );
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "نتایج جستجو"
                )
                .setMessage(
                        text.toString()
                )
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
                .setTitle(
                        "💵 موجودی نقدی"
                )
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

    // =========================================================
    // NUMBER FIELDS
    // =========================================================

    private EditText field(
            String hint) {

        EditText e =
                new EditText(this);

        e.setHint(hint);
        e.setSingleLine(true);
        e.setPadding(
                16, 12, 16, 12
        );

        return e;
    }

    private EditText numberField(
            String hint) {

        EditText e =
                field(hint);

        addThousandsFormatter(e);

        return e;
    }

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
                                current)) {
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
            clean =
                    clean.substring(1);
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
