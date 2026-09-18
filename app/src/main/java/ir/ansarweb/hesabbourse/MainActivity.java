package ir.ansarweb.hesabbourse;

import android.app.AlertDialog;
import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.database.Cursor;
import android.view.View;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private DatabaseHelper db;

    private static final double BUY_FEE_RATE = 0.0037;
    private static final double SELL_FEE_RATE = 0.0088;

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

        setContentView(root);
    }

    private EditText field(String hint) {

        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setPadding(16, 12, 16, 12);

        return e;
    }

    private void showTradeDialog() {

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(20, 10, 20, 10);

        EditText portfolio = field("سبد / پرتفوی");
        portfolio.setText("اصلی");

        EditText broker = field("کارگزاری");
        EditText symbol = field("نماد");
        EditText quantity = field("تعداد سهم");
        EditText price = field("قیمت هر سهم");
        EditText totalAmount = field("مبلغ کل معامله");
        EditText description = field("توضیحات");

        box.addView(portfolio);
        box.addView(broker);
        box.addView(symbol);
        box.addView(quantity);
        box.addView(price);
        box.addView(totalAmount);
        box.addView(description);

        TextView info = new TextView(this);

        info.setText(
                "\nتعداد، قیمت هر سهم و مبلغ کل می‌توانند به‌صورت خودکار از روی دو مقدار محاسبه شوند.\n" +
                "کارمزد جداگانه محاسبه می‌شود و داخل قیمت معامله نمی‌رود."
        );

        info.setPadding(0, 10, 0, 10);

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
                        description)) {

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
                        description)) {

                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void addAutoCalculation(
            EditText quantity,
            EditText price,
            EditText total) {

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

                if (calculatingFields) {
                    return;
                }

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

        if (calculatingFields) {
            return;
        }

        double q = number(quantity);
        double p = number(price);
        double t = number(total);

        try {

            calculatingFields = true;

            /*
             * تعداد + قیمت
             * مبلغ = تعداد × قیمت
             */
            if (q > 0 && p > 0) {

                double calculatedTotal =
                        q * p;

                setTextIfDifferent(
                        total,
                        formatNumber(
                                calculatedTotal
                        )
                );
            }

            /*
             * تعداد + مبلغ
             * قیمت = مبلغ ÷ تعداد
             */
            else if (q > 0 && t > 0) {

                double calculatedPrice =
                        t / q;

                setTextIfDifferent(
                        price,
                        formatNumber(
                                calculatedPrice
                        )
                );
            }

            /*
             * قیمت + مبلغ
             * تعداد = کف(مبلغ ÷ قیمت)
             */
            else if (p > 0 && t > 0) {

                double calculatedQuantity =
                        Math.floor(t / p);

                if (calculatedQuantity > 0) {

                    double calculatedTotal =
                            calculatedQuantity * p;

                    setTextIfDifferent(
                            quantity,
                            formatNumber(
                                    calculatedQuantity
                            )
                    );

                    setTextIfDifferent(
                            total,
                            formatNumber(
                                    calculatedTotal
                            )
                    );
                }
            }

        } finally {

            calculatingFields = false;
        }
    }

    private void setTextIfDifferent(
            EditText field,
            String value) {

        String old =
                field.getText().toString();

        if (!old.equals(value)) {

            field.setText(value);
            field.setSelection(field.length());
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
                portfolioField
                        .getText()
                        .toString()
                        .trim();

        String broker =
                brokerField
                        .getText()
                        .toString()
                        .trim();

        String symbol =
                symbolField
                        .getText()
                        .toString()
                        .trim();

        String description =
                descriptionField
                        .getText()
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

            android.widget.Toast.makeText(
                    this,
                    "نماد را وارد کنید",
                    android.widget.Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        if (quantity <= 0) {

            android.widget.Toast.makeText(
                    this,
                    "تعداد معتبر نیست",
                    android.widget.Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        if (price <= 0) {

            android.widget.Toast.makeText(
                    this,
                    "قیمت معتبر نیست",
                    android.widget.Toast.LENGTH_SHORT
            ).show();

            return false;
        }

        /*
         * مبلغ معامله بدون کارمزد
         */
        double amount =
                quantity * price;

        /*
         * کارمزد جداگانه
         */
        double fee;

        if ("BUY".equals(type)) {

            fee =
                    amount * BUY_FEE_RATE;

        } else {

            fee =
                    amount * SELL_FEE_RATE;
        }

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

        double finalAmount;

        if ("BUY".equals(type)) {

            finalAmount =
                    amount + fee;

        } else {

            finalAmount =
                    amount - fee;
        }

        String message;

        if ("BUY".equals(type)) {

            message =
                    "خرید ثبت شد\n" +
                    "مبلغ معامله: " +
                    money(amount) +
                    "\nکارمزد: " +
                    money(fee) +
                    "\nپرداخت نهایی: " +
                    money(finalAmount);

        } else {

            message =
                    "فروش ثبت شد\n" +
                    "مبلغ معامله: " +
                    money(amount) +
                    "\nکارمزد: " +
                    money(fee) +
                    "\nدریافتی خالص: " +
                    money(finalAmount);
        }

        android.widget.Toast.makeText(
                this,
                message,
                android.widget.Toast.LENGTH_LONG
        ).show();

        return true;
    }

    private void showMoneyDialog(String type) {

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL);

        box.setPadding(
                20, 10, 20, 10);

        EditText portfolio =
                field("سبد / پرتفوی");

        portfolio.setText("اصلی");

        EditText amount =
                field("مبلغ");

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

                    android.widget.Toast.makeText(
                            this,
                            "مبلغ معتبر نیست",
                            android.widget.Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                String p =
                        portfolio
                                .getText()
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
                        description
                                .getText()
                                .toString()
                                .trim()
                );

                android.widget.Toast.makeText(
                        this,
                        "ثبت شد",
                        android.widget.Toast.LENGTH_SHORT
                ).show();

                dialog.dismiss();
            });
        });

        dialog.show();
    }

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

            android.widget.Toast.makeText(
                    this,
                    "هنوز سبدی ثبت نشده است",
                    android.widget.Toast.LENGTH_SHORT
            ).show();

            return;
        }

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL);

        box.setPadding(
                20, 10, 20, 10);

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

        Map<String,
                PortfolioEngine.Position> positions =
                calculatePositions();

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL);

        box.setPadding(
                20, 10, 20, 10);

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

        for (Map.Entry<
                String,
                PortfolioEngine.Position> entry
                : positions.entrySet()) {

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

            String symbol =
                    position.symbol;

            symbolButton.setOnClickListener(
                    v ->
                            showSymbolTransactionsDialog(
                                    portfolioName,
                                    symbol
                            )
            );

            box.addView(symbolButton);
        }

        if (!found) {

            TextView empty =
                    new TextView(this);

            empty.setText(
                    "در این سبد سهم فعال وجود ندارد."
            );

            empty.setPadding(
                    0, 20, 0, 20
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

    private void showSymbolTransactionsDialog(
            String portfolioName,
            String symbolName) {

        Cursor cursor =
                db.getAllTransactions();

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL);

        box.setPadding(
                20, 10, 20, 10);

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

        box.addView(transactionsTitle);

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

    private Map<String,
            PortfolioEngine.Position>
            calculatePositions() {

        Cursor cursor =
                db.getAllTransactions();

        return PortfolioEngine.calculate(
                cursor
        );
    }

    private void showHistoryDialog() {

        Cursor cursor =
                db.getAllTransactions();

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL);

        box.setPadding(
                20, 10, 20, 10
        );

        while (cursor.moveToNext()) {

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

            String description =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "description"
                            )
                    );

            TextView item =
                    new TextView(this);

            if ("BUY".equals(type) ||
                    "SELL".equals(type)) {

                double finalAmount =
                        "BUY".equals(type)
                                ? amount + fee
                                : amount - fee;

                item.setText(
                        ("BUY".equals(type)
                                ? "🟢 خرید"
                                : "🔴 فروش") +
                        "\nسبد: " +
                        safe(portfolio) +
                        "\nنماد: " +
                        safe(symbol) +
                        "\nتاریخ: " +
                        safe(date) +
                        "\nتعداد: " +
                        formatNumber(quantity) +
                        "\nقیمت: " +
                        money(price) +
                        "\nمبلغ: " +
                        money(amount) +
                        "\nکارمزد: " +
                        money(fee) +
                        "\nمبلغ نهایی: " +
                        money(finalAmount) +
                        "\nتوضیحات: " +
                        safe(description) +
                        "\n──────────────────"
                );

            } else {

                item.setText(
                        ("DEPOSIT".equals(type)
                                ? "💰 واریز"
                                : "💸 برداشت") +
                        "\nسبد: " +
                        safe(portfolio) +
                        "\nمبلغ: " +
                        money(amount) +
                        "\nتاریخ: " +
                        safe(date) +
                        "\nتوضیحات: " +
                        safe(description) +
                        "\n──────────────────"
                );
            }

            item.setPadding(
                    0, 10, 0, 10
            );

            box.addView(item);
        }

        cursor.close();

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(box);

        new AlertDialog.Builder(this)
                .setTitle("تاریخچه")
                .setView(scroll)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

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

            android.widget.Toast.makeText(
                    this,
                    "عبارت جستجو را وارد کنید",
                    android.widget.Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Cursor cursor =
                db.searchTransactions(query);

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL);

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

    private void showCashBalance() {

        /*
         * اصلاح اصلی خطای Build:
         * DatabaseHelper.getCashBalance
         * نیاز به نام سبد دارد.
         */
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
                    value.replace(",", "");

            value =
                    value.replace("٬", "");

            value =
                    value.replace("٫", ".");

            value =
                    value.replace("۰", "0")
                            .replace("۱", "1")
                            .replace("۲", "2")
                            .replace("۳", "3")
                            .replace("۴", "4")
                            .replace("۵", "5")
                            .replace("۶", "6")
                            .replace("۷", "7")
                            .replace("۸", "8")
                            .replace("۹", "9");

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
                    "%.0f",
                    value
            );
        }

        return String.format(
                Locale.US,
                "%.4f",
                value
        );
    }

    private String safe(
            String value) {

        if (value == null) {
            return "";
        }

        return value;
    }
}
