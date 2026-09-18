package ir.ansarweb.hesabbourse;

import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private DatabaseHelper database;
    private TextView dashboard;

    private static final double BUY_FEE_RATE = 0.0037;
    private static final double SELL_FEE_RATE = 0.0088;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database =
                new DatabaseHelper(this);

        buildInterface();
        refreshDashboard();
    }

    private void buildInterface() {

        LinearLayout root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                24, 24, 24, 24
        );

        root.addView(
                createText(
                        "حساب بورس",
                        28,
                        true
                )
        );

        root.addView(
                createText(
                        "حسابداری معاملات و سبد سرمایه‌گذاری",
                        16,
                        false
                )
        );

        addButton(
                root,
                "➕ ثبت خرید",
                () -> showTradeDialog("BUY")
        );

        addButton(
                root,
                "➖ ثبت فروش",
                () -> showTradeDialog("SELL")
        );

        addButton(
                root,
                "💰 ثبت واریز",
                () -> showCashDialog("DEPOSIT")
        );

        addButton(
                root,
                "💸 ثبت برداشت",
                () -> showCashDialog("WITHDRAW")
        );

        addButton(
                root,
                "📊 داشبورد سبدها",
                this::showPortfolioDialog
        );

        addButton(
                root,
                "🧾 تاریخچه معاملات",
                this::showHistoryDialog
        );

        root.addView(
                createText(
                        "وضعیت فعلی",
                        21,
                        true
                )
        );

        dashboard =
                createText(
                        "",
                        16,
                        false
                );

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(dashboard);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        setContentView(root);
    }

    private TextView createText(
            String text,
            float size,
            boolean bold) {

        TextView view =
                new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setPadding(
                0, 10, 0, 10
        );

        if (bold) {
            view.setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
            );
        }

        return view;
    }

    private void addButton(
            LinearLayout parent,
            String text,
            Runnable action) {

        Button button =
                new Button(this);

        button.setText(text);
        button.setTextSize(17);

        button.setOnClickListener(
                v -> action.run()
        );

        parent.addView(button);
    }

    private LinearLayout createForm() {

        LinearLayout form =
                new LinearLayout(this);

        form.setOrientation(
                LinearLayout.VERTICAL
        );

        form.setPadding(
                35, 5, 35, 5
        );

        return form;
    }

    private EditText createField(
            String hint,
            String value) {

        EditText field =
                new EditText(this);

        field.setHint(hint);
        field.setText(value);
        field.setSingleLine(true);

        return field;
    }

    private double number(String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return 0;
        }

        String normalized =
                value
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

        return Double.parseDouble(
                normalized
        );
    }

    private String formatNumber(
            double value) {

        if (Math.abs(
                value - Math.round(value)
        ) < 0.000001) {

            return String.format(
                    java.util.Locale.US,
                    "%,d",
                    Math.round(value)
            );
        }

        return String.format(
                java.util.Locale.US,
                "%,.2f",
                value
        );
    }

    private String normalizePortfolio(
            String portfolio) {

        if (portfolio == null ||
                portfolio.trim().isEmpty()) {

            return "اصلی";
        }

        return portfolio.trim();
    }

    /*
     * مهم:
     * تراکنش‌ها باید از قدیمی به جدید
     * محاسبه شوند.
     *
     * مشکل اصلی فروش قبلی همین بود.
     */
    private Map<String, PortfolioEngine.Position>
    calculatePositions() {

        Cursor cursor =
                database.getReadableDatabase()
                        .rawQuery(
                                "SELECT * FROM transactions " +
                                        "ORDER BY date ASC, id ASC",
                                null
                        );

        try {

            return PortfolioEngine.calculate(
                    cursor
            );

        } finally {

            cursor.close();
        }
    }

    private double getCurrentQuantity(
            String portfolio,
            String symbol) {

        Map<String, PortfolioEngine.Position>
                positions =
                calculatePositions();

        String key =
                normalizePortfolio(portfolio)
                        + "|"
                        + symbol.trim();

        PortfolioEngine.Position position =
                positions.get(key);

        if (position == null) {
            return 0;
        }

        return position.quantity;
    }

    private void showTradeDialog(
            String type) {

        LinearLayout form =
                createForm();

        EditText portfolio =
                createField(
                        "نام سبد",
                        "اصلی"
                );

        EditText broker =
                createField(
                        "کارگزاری",
                        ""
                );

        EditText symbol =
                createField(
                        "نماد",
                        ""
                );

        EditText quantity =
                createField(
                        "تعداد سهم",
                        ""
                );

        EditText price =
                createField(
                        "قیمت هر سهم",
                        ""
                );

        EditText totalAmount =
                createField(
                        "مبلغ کل",
                        ""
                );

        EditText description =
                createField(
                        "توضیحات",
                        ""
                );

        form.addView(portfolio);
        form.addView(broker);
        form.addView(symbol);
        form.addView(quantity);
        form.addView(price);
        form.addView(totalAmount);
        form.addView(description);

        String title =
                type.equals("BUY")
                        ? "ثبت خرید"
                        : "ثبت فروش";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(form)
                .setPositiveButton(
                        "ثبت",
                        (dialog, which) -> {

                            try {

                                String portfolioText =
                                        normalizePortfolio(
                                                portfolio
                                                        .getText()
                                                        .toString()
                                        );

                                String symbolText =
                                        symbol.getText()
                                                .toString()
                                                .trim()
                                                .toUpperCase();

                                double q =
                                        number(
                                                quantity
                                                        .getText()
                                                        .toString()
                                        );

                                double p =
                                        number(
                                                price
                                                        .getText()
                                                        .toString()
                                        );

                                double total =
                                        number(
                                                totalAmount
                                                        .getText()
                                                        .toString()
                                        );

                                if (symbolText.isEmpty()) {

                                    Toast.makeText(
                                            this,
                                            "نماد را وارد کنید",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                if (p <= 0) {

                                    Toast.makeText(
                                            this,
                                            "قیمت باید بیشتر از صفر باشد",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                /*
                                 * حالت مبلغ:
                                 * تعداد = مبلغ ÷ قیمت
                                 */
                                if (q <= 0 &&
                                        total > 0) {

                                    q =
                                            Math.floor(
                                                    total / p
                                            );

                                    if (q <= 0) {

                                        Toast.makeText(
                                                this,
                                                "مبلغ برای یک سهم کافی نیست",
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        return;
                                    }

                                    total =
                                            q * p;

                                } else if (q > 0) {

                                    total =
                                            q * p;

                                } else {

                                    Toast.makeText(
                                            this,
                                            "تعداد یا مبلغ کل را وارد کنید",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                /*
                                 * کنترل موجودی فروش
                                 */
                                if ("SELL".equals(type)) {

                                    double available =
                                            getCurrentQuantity(
                                                    portfolioText,
                                                    symbolText
                                            );

                                    if (available <= 0) {

                                        Toast.makeText(
                                                this,
                                                "این نماد در این سبد موجود نیست",
                                                Toast.LENGTH_LONG
                                        ).show();

                                        return;
                                    }

                                    if (q >
                                            available + 0.000001) {

                                        Toast.makeText(
                                                this,
                                                "فروش بیشتر از موجودی است\n"
                                                        + "موجودی: "
                                                        + formatNumber(
                                                        available
                                                )
                                                        + "\nفروش: "
                                                        + formatNumber(q),
                                                Toast.LENGTH_LONG
                                        ).show();

                                        return;
                                    }
                                }

                                double feeRate =
                                        "BUY".equals(type)
                                                ? BUY_FEE_RATE
                                                : SELL_FEE_RATE;

                                double fee =
                                        total * feeRate;

                                long id =
                                        database
                                                .addTransactionWithDescription(
                                                        type,
                                                        portfolioText,
                                                        broker.getText()
                                                                .toString(),
                                                        symbolText,
                                                        q,
                                                        p,
                                                        fee,
                                                        total,
                                                        description.getText()
                                                                .toString()
                                                );

                                if (id <= 0) {

                                    Toast.makeText(
                                            this,
                                            "ثبت معامله انجام نشد",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                double finalAmount;

                                String message;

                                if ("BUY".equals(type)) {

                                    /*
                                     * خرید نهایی =
                                     * مبلغ معامله + کارمزد
                                     */
                                    finalAmount =
                                            total + fee;

                                    message =
                                            "خرید ثبت شد\n"
                                                    + "مبلغ معامله: "
                                                    + formatNumber(total)
                                                    + "\nکارمزد: "
                                                    + formatNumber(fee)
                                                    + "\nمبلغ نهایی خرید: "
                                                    + formatNumber(
                                                    finalAmount
                                            );

                                } else {

                                    /*
                                     * دریافتی خالص فروش =
                                     * مبلغ معامله - کارمزد
                                     */
                                    finalAmount =
                                            total - fee;

                                    message =
                                            "فروش ثبت شد\n"
                                                    + "مبلغ فروش: "
                                                    + formatNumber(total)
                                                    + "\nکارمزد: "
                                                    + formatNumber(fee)
                                                    + "\nدریافتی خالص: "
                                                    + formatNumber(
                                                    finalAmount
                                            );
                                }

                                Toast.makeText(
                                        this,
                                        message,
                                        Toast.LENGTH_LONG
                                ).show();

                                refreshDashboard();

                            } catch (Exception e) {

                                Toast.makeText(
                                        this,
                                        "اطلاعات واردشده صحیح نیست",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .setNegativeButton(
                        "انصراف",
                        null
                )
                .show();
    }

    private void showCashDialog(
            String type) {

        LinearLayout form =
                createForm();

        EditText portfolio =
                createField(
                        "نام سبد",
                        "اصلی"
                );

        EditText amount =
                createField(
                        "مبلغ",
                        ""
                );

        EditText description =
                createField(
                        "توضیحات",
                        ""
                );

        form.addView(portfolio);
        form.addView(amount);
        form.addView(description);

        String title =
                "DEPOSIT".equals(type)
                        ? "ثبت واریز"
                        : "ثبت برداشت";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(form)
                .setPositiveButton(
                        "ثبت",
                        (dialog, which) -> {

                            try {

                                double value =
                                        number(
                                                amount.getText()
                                                        .toString()
                                        );

                                if (value <= 0) {

                                    Toast.makeText(
                                            this,
                                            "مبلغ باید بیشتر از صفر باشد",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                long id =
                                        database
                                                .addTransactionWithDescription(
                                                        type,
                                                        normalizePortfolio(
                                                                portfolio
                                                                        .getText()
                                                                        .toString()
                                                        ),
                                                        "",
                                                        "",
                                                        0,
                                                        0,
                                                        0,
                                                        value,
                                                        description
                                                                .getText()
                                                                .toString()
                                                );

                                if (id <= 0) {

                                    Toast.makeText(
                                            this,
                                            "ثبت انجام نشد",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                Toast.makeText(
                                        this,
                                        "عملیات با موفقیت ثبت شد",
                                        Toast.LENGTH_SHORT
                                ).show();

                                refreshDashboard();

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
                        "انصراف",
                        null
                )
                .show();
    }

    private void refreshDashboard() {

        Map<String, PortfolioEngine.Position>
                positions =
                calculatePositions();

        StringBuilder result =
                new StringBuilder();

        result.append(
                "📊 داشبورد سبدها\n\n"
        );

        double totalCurrentCost = 0;
        double totalRealizedProfit = 0;
        double totalBuyCost = 0;
        double totalSellNet = 0;
        double totalFees = 0;

        boolean found = false;

        for (PortfolioEngine.Position position
                : positions.values()) {

            totalCurrentCost +=
                    position.cost;

            totalRealizedProfit +=
                    position.realizedProfit;

            totalBuyCost +=
                    position.totalBuyCost();

            totalSellNet +=
                    position.totalSellNet();

            totalFees +=
                    position.buyFees
                            + position.sellFees;

            if (position.quantity <= 0 &&
                    position.sellQuantity <= 0) {

                continue;
            }

            found = true;

            result.append(
                    "━━━━━━━━━━━━━━━━━━\n"
            );

            result.append(
                    "سبد: "
            ).append(
                    position.portfolio
            ).append("\n");

            result.append(
                    "نماد: "
            ).append(
                    position.symbol
            ).append("\n\n");

            result.append(
                    "📦 تعداد فعلی: "
            ).append(
                    formatNumber(
                            position.quantity
                    )
            ).append("\n");

            result.append(
                    "🟢 میانگین خرید: "
            ).append(
                    formatNumber(
                            position.averageBuyPrice()
                    )
            ).append("\n");

            result.append(
                    "🔴 میانگین فروش: "
            ).append(
                    formatNumber(
                            position.averageSellPrice()
                    )
            ).append("\n");

            result.append(
                    "💰 بهای تمام‌شده فعلی: "
            ).append(
                    formatNumber(
                            position.cost
                    )
            ).append("\n\n");

            result.append(
                    "خرید کل: "
            ).append(
                    formatNumber(
                            position.buyAmount
                    )
            ).append("\n");

            result.append(
                    "کارمزد خرید: "
            ).append(
                    formatNumber(
                            position.buyFees
                    )
            ).append("\n");

            result.append(
                    "نهایی خرید: "
            ).append(
                    formatNumber(
                            position.totalBuyCost()
                    )
            ).append("\n\n");

            result.append(
                    "فروش کل: "
            ).append(
                    formatNumber(
                            position.sellAmount
                    )
            ).append("\n");

            result.append(
                    "کارمزد فروش: "
            ).append(
                    formatNumber(
                            position.sellFees
                    )
            ).append("\n");

            result.append(
                    "دریافتی خالص فروش: "
            ).append(
                    formatNumber(
                            position.totalSellNet()
                    )
            ).append("\n\n");

            result.append(
                    "📈 سود تحقق‌یافته: "
            ).append(
                    formatNumber(
                            position.realizedProfit
                    )
            ).append("\n");
        }

        result.append(
                "\n━━━━━━━━━━━━━━━━━━\n"
        );

        result.append(
                "📊 خلاصه کل\n\n"
        );

        result.append(
                "مجموع خرید با کارمزد: "
        ).append(
                formatNumber(totalBuyCost)
        ).append("\n");

        result.append(
                "مجموع فروش خالص: "
        ).append(
                formatNumber(totalSellNet)
        ).append("\n");

        result.append(
                "مجموع کارمزد: "
        ).append(
                formatNumber(totalFees)
        ).append("\n");

        result.append(
                "سود تحقق‌یافته: "
        ).append(
                formatNumber(totalRealizedProfit)
        ).append("\n");

        result.append(
                "بهای تمام‌شده سهام فعلی: "
        ).append(
                formatNumber(totalCurrentCost)
        ).append("\n");

        if (!found) {

            result.append(
                    "\nهنوز سهمی در سبد ثبت نشده است."
            );
        }

        dashboard.setText(
                result.toString()
        );
    }

    private void showPortfolioDialog() {

        Map<String, PortfolioEngine.Position>
                positions =
                calculatePositions();

        StringBuilder result =
                new StringBuilder();

        result.append(
                "📊 گزارش کامل سبدها\n\n"
        );

        boolean found = false;

        for (PortfolioEngine.Position position
                : positions.values()) {

            if (position.quantity <= 0 &&
                    position.sellQuantity <= 0) {

                continue;
            }

            found = true;

            result.append(
                    "سبد: "
            ).append(
                    position.portfolio
            ).append("\n");

            result.append(
                    "نماد: "
            ).append(
                    position.symbol
            ).append("\n");

            result.append(
                    "تعداد: "
            ).append(
                    formatNumber(
                            position.quantity
                    )
            ).append("\n");

            result.append(
                    "میانگین خرید: "
            ).append(
                    formatNumber(
                            position.averageBuyPrice()
                    )
            ).append("\n");

            result.append(
                    "میانگین فروش: "
            ).append(
                    formatNumber(
                            position.averageSellPrice()
                    )
            ).append("\n");

            result.append(
                    "سود تحقق‌یافته: "
            ).append(
                    formatNumber(
                            position.realizedProfit
                    )
            ).append("\n");

            result.append(
                    "--------------------\n"
            );
        }

        if (!found) {

            result.append(
                    "هنوز معامله‌ای ثبت نشده است."
            );
        }

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(
                createText(
                        result.toString(),
                        16,
                        false
                )
        );

        new AlertDialog.Builder(this)
                .setTitle(
                        "داشبورد سبدها"
                )
                .setView(scroll)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    private void showHistoryDialog() {

        Cursor cursor =
                database.getAllTransactions();

        StringBuilder result =
                new StringBuilder();

        try {

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

                String broker =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "broker"
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

                String description =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "description"
                                )
                        );

                String shamsiDate = "";

                int dateColumn =
                        cursor.getColumnIndex(
                                "date_shamsi"
                        );

                if (dateColumn >= 0) {

                    shamsiDate =
                            cursor.getString(
                                    dateColumn
                            );
                }

                String action;

                if ("BUY".equals(type)) {

                    action = "🟢 خرید";

                } else if ("SELL".equals(type)) {

                    action = "🔴 فروش";

                } else if ("DEPOSIT".equals(type)) {

                    action = "💰 واریز";

                } else {

                    action = "💸 برداشت";
                }

                result.append(
                        action
                ).append("\n");

                /*
                 * تاریخ از date_shamsi خوانده می‌شود،
                 * بنابراین دیگر میلادی نمایش داده نمی‌شود.
                 */
                result.append(
                        "📅 تاریخ: "
                ).append(
                        shamsiDate == null ||
                                shamsiDate.isEmpty()
                                ? "بدون تاریخ"
                                : shamsiDate
                ).append("\n");

                result.append(
                        "سبد: "
                ).append(
                        portfolio == null
                                ? "اصلی"
                                : portfolio
                ).append("\n");

                if (broker != null &&
                        !broker.trim().isEmpty()) {

                    result.append(
                            "کارگزاری: "
                    ).append(
                            broker
                    ).append("\n");
                }

                if (symbol != null &&
                        !symbol.trim().isEmpty()) {

                    result.append(
                            "نماد: "
                    ).append(
                            symbol
                    ).append("\n");
                }

                if (quantity > 0) {

                    result.append(
                            "تعداد: "
                    ).append(
                            formatNumber(quantity)
                    ).append("\n");
                }

                if (price > 0) {

                    result.append(
                            "قیمت: "
                    ).append(
                            formatNumber(price)
                    ).append("\n");
                }

                if (amount > 0) {

                    result.append(
                            "مبلغ معامله: "
                    ).append(
                            formatNumber(amount)
                    ).append("\n");
                }

                if (fee > 0) {

                    result.append(
                            "کارمزد: "
                    ).append(
                            formatNumber(fee)
                    ).append("\n");

                    if ("BUY".equals(type)) {

                        result.append(
                                "مبلغ نهایی خرید: "
                        ).append(
                                formatNumber(
                                        amount + fee
                                )
                        ).append("\n");

                    } else if ("SELL".equals(type)) {

                        result.append(
                                "دریافتی خالص فروش: "
                        ).append(
                                formatNumber(
                                        amount - fee
                                )
                        ).append("\n");
                    }
                }

                if (description != null &&
                        !description.trim().isEmpty()) {

                    result.append(
                            "توضیحات: "
                    ).append(
                            description
                    ).append("\n");
                }

                result.append(
                        "--------------------\n\n"
                );
            }

        } finally {

            cursor.close();
        }

        if (result.length() == 0) {

            result.append(
                    "تراکنشی ثبت نشده است."
            );
        }

        ScrollView scroll =
                new ScrollView(this);

        scroll.addView(
                createText(
                        result.toString(),
                        16,
                        false
                )
        );

        new AlertDialog.Builder(this)
                .setTitle(
                        "تاریخچه معاملات"
                )
                .setView(scroll)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    @Override
    protected void onDestroy() {

        if (database != null) {
            database.close();
        }

        super.onDestroy();
    }
}
