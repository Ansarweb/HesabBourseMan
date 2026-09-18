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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

private DatabaseHelper database;
private TextView dashboard;

private static final double BUY_FEE_RATE = 0.0037;
private static final double SELL_FEE_RATE = 0.0088;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    database = new DatabaseHelper(this);

    buildInterface();
    refreshDashboard();
}

private void buildInterface() {

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(24, 24, 24, 24);

    root.addView(createText("حساب بورس", 28, true));
    root.addView(createText(
            "حسابداری معاملات و سبد سرمایه‌گذاری",
            16,
            false
    ));

    addButton(root, "➕ ثبت خرید", () -> showTradeDialog("BUY"));
    addButton(root, "➖ ثبت فروش", () -> showTradeDialog("SELL"));
    addButton(root, "💰 ثبت واریز", () -> showCashDialog("DEPOSIT"));
    addButton(root, "💸 ثبت برداشت", () -> showCashDialog("WITHDRAW"));
    addButton(root, "📊 داشبورد سبدها", this::showPortfolioDialog);
    addButton(root, "🧾 تاریخچه معاملات", this::showHistoryDialog);

    root.addView(createText("وضعیت فعلی", 21, true));

    dashboard = createText("", 16, false);

    ScrollView scrollView = new ScrollView(this);
    scrollView.addView(dashboard);

    root.addView(
            scrollView,
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

    TextView view = new TextView(this);

    view.setText(text);
    view.setTextSize(size);

    view.setPadding(0, 10, 0, 10);

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

    Button button = new Button(this);

    button.setText(text);
    button.setTextSize(17);

    button.setOnClickListener(
            v -> action.run()
    );

    parent.addView(button);
}

private LinearLayout createForm() {

    LinearLayout form = new LinearLayout(this);

    form.setOrientation(
            LinearLayout.VERTICAL
    );

    form.setPadding(
            35,
            5,
            35,
            5
    );

    return form;
}

private EditText createField(
        String hint,
        String value) {

    EditText field = new EditText(this);

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

    return Double.parseDouble(normalized);
}

private String formatNumber(double value) {

    if (Math.abs(value - Math.round(value)) < 0.000001) {
        return String.format(
                Locale.US,
                "%,d",
                Math.round(value)
        );
    }

    return String.format(
            Locale.US,
            "%,.2f",
            value
    );
}

private String formatDate(long timestamp) {

    SimpleDateFormat format =
            new SimpleDateFormat(
                    "yyyy/MM/dd HH:mm",
                    Locale.US
            );

    return format.format(
            new Date(timestamp)
    );
}

private double getCurrentQuantity(
        String portfolio,
        String symbol) {

    Cursor cursor =
            database.getAllTransactions();

    double quantity = 0;

    try {

        while (cursor.moveToNext()) {

            String rowPortfolio =
                    cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                    "portfolio"
                            )
                    );

            String rowSymbol =
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

            if (rowPortfolio == null ||
                    rowPortfolio.trim().isEmpty()) {
                rowPortfolio = "اصلی";
            }

            if (rowSymbol == null) {
                continue;
            }

            if (!rowPortfolio.equals(
                    normalizePortfolio(portfolio))) {
                continue;
            }

            if (!rowSymbol.equalsIgnoreCase(
                    symbol.trim())) {
                continue;
            }

            double q =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "quantity"
                            )
                    );

            if ("BUY".equals(type)) {
                quantity += q;
            } else if ("SELL".equals(type)) {
                quantity -= q;
            }
        }

    } finally {
        cursor.close();
    }

    return Math.max(quantity, 0);
}

private String normalizePortfolio(
        String portfolio) {

    if (portfolio == null ||
            portfolio.trim().isEmpty()) {
        return "اصلی";
    }

    return portfolio.trim();
}

private void showTradeDialog(String type) {

    LinearLayout form = createForm();

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
                    "مبلغ کل — در حالت مبلغ",
                    ""
            );

    EditText description =
            createField(
                    "توضیحات (اختیاری)",
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
                                            portfolio.getText()
                                                    .toString()
                                    );

                            String symbolText =
                                    symbol.getText()
                                            .toString()
                                            .trim();

                            double q =
                                    number(
                                            quantity.getText()
                                                    .toString()
                                    );

                            double p =
                                    number(
                                            price.getText()
                                                    .toString()
                                    );

                            double total =
                                    number(
                                            totalAmount.getText()
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
                                        "قیمت سهم باید بیشتر از صفر باشد",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            /*
                             * اگر تعداد وارد نشده باشد،
                             * تعداد از مبلغ کل محاسبه می‌شود.
                             */
                            if (q <= 0 && total > 0) {

                                q = Math.floor(
                                        total / p
                                );

                                if (q <= 0) {

                                    Toast.makeText(
                                            this,
                                            "مبلغ برای خرید یا فروش یک سهم کافی نیست",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    return;
                                }

                                total = q * p;

                            } else if (q > 0) {

                                total = q * p;

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

                                if (q > available + 0.000001) {

                                    Toast.makeText(
                                            this,
                                            "فروش ثبت نشد.\n"
                                                    + "موجودی فعلی: "
                                                    + formatNumber(available)
                                                    + "\n"
                                                    + "مقدار فروش: "
                                                    + formatNumber(q),
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }
                            }

                            double feeRate =
                                    type.equals("BUY")
                                            ? BUY_FEE_RATE
                                            : SELL_FEE_RATE;

                            double fee =
                                    total * feeRate;

                            long id =
                                    database.addTransactionWithDescription(
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

                            String action =
                                    type.equals("BUY")
                                            ? "خرید"
                                            : "فروش";

                            Toast.makeText(
                                    this,
                                    action
                                            + " با موفقیت ثبت شد\n"
                                            + "مبلغ معامله: "
                                            + formatNumber(total)
                                            + "\nکارمزد: "
                                            + formatNumber(fee),
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

private void showCashDialog(String type) {

    LinearLayout form = createForm();

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
            type.equals("DEPOSIT")
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
                                    database.addTransactionWithDescription(
                                            type,
                                            normalizePortfolio(
                                                    portfolio.getText()
                                                            .toString()
                                            ),
                                            "",
                                            "",
                                            0,
                                            0,
                                            0,
                                            value,
                                            description.getText()
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
                                    type.equals("DEPOSIT")
                                            ? "واریز ثبت شد"
                                            : "برداشت ثبت شد",
                                    Toast.LENGTH_SHORT
                            ).show();

                            refreshDashboard();

                        } catch (Exception e) {

                            Toast.makeText(
                                    this,
                                    "مبلغ واردشده صحیح نیست",
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

private Map<String, PortfolioEngine.Position>
calculatePositions() {

    Cursor cursor =
            database.getAllTransactions();

    try {

        return PortfolioEngine.calculate(cursor);

    } finally {

        cursor.close();
    }
}

private void refreshDashboard() {

    if (dashboard == null) {
        return;
    }

    Map<String, PortfolioEngine.Position>
            positions =
            calculatePositions();

    StringBuilder result =
            new StringBuilder();

    result.append(
            "📊 وضعیت سبدها\n\n"
    );

    boolean hasPosition = false;

    double totalCost = 0;
    double totalRealized = 0;
    double totalFees = 0;

    Map<String, Double> cashByPortfolio =
            new LinkedHashMap<>();

    Cursor cashCursor =
            database.getAllTransactions();

    try {

        while (cashCursor.moveToNext()) {

            String portfolio =
                    cashCursor.getString(
                            cashCursor.getColumnIndexOrThrow(
                                    "portfolio"
                            )
                    );

            String type =
                    cashCursor.getString(
                            cashCursor.getColumnIndexOrThrow(
                                    "type"
                            )
                    );

            double amount =
                    cashCursor.getDouble(
                            cashCursor.getColumnIndexOrThrow(
                                    "amount"
                            )
                    );

            double fee =
                    cashCursor.getDouble(
                            cashCursor.getColumnIndexOrThrow(
                                    "fee"
                            )
                    );

            totalFees += fee;

            if (portfolio == null ||
                    portfolio.trim().isEmpty()) {
                portfolio = "اصلی";
            }

            double cash =
                    cashByPortfolio.containsKey(portfolio)
                            ? cashByPortfolio.get(portfolio)
                            : 0;

            if ("DEPOSIT".equals(type)) {
                cash += amount;
            } else if ("WITHDRAW".equals(type)) {
                cash -= amount;
            } else if ("BUY".equals(type)) {
                cash -= amount + fee;
            } else if ("SELL".equals(type)) {
                cash += amount - fee;
            }

            cashByPortfolio.put(
                    portfolio,
                    cash
            );
        }

    } finally {
        cashCursor.close();
    }

    for (PortfolioEngine.Position position
            : positions.values()) {

        totalRealized +=
                position.realizedProfit;

        /*
         * حتی اگر تعداد صفر شده باشد،
         * سود/زیان تحقق‌یافته را نشان می‌دهیم.
         */
        if (position.quantity <= 0 &&
                Math.abs(position.realizedProfit) < 0.000001) {
            continue;
        }

        hasPosition = true;

        totalCost +=
                position.cost;

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
                "تعداد فعلی: "
        ).append(
                formatNumber(
                        position.quantity
                )
        ).append("\n");

        result.append(
                "میانگین خرید: "
        ).append(
                formatNumber(
                        position.averagePrice()
                )
        ).append("\n");

        result.append(
                "بهای تمام‌شده فعلی: "
        ).append(
                formatNumber(
                        position.cost
                )
        ).append("\n");

        result.append(
                "سود/زیان تحقق‌یافته: "
        ).append(
                formatNumber(
                        position.realizedProfit
                )
        ).append("\n");

        result.append(
                "--------------------\n"
        );
    }

    result.append(
            "\n💵 موجودی نقدی سبدها\n\n"
    );

    if (cashByPortfolio.isEmpty()) {

        result.append(
                "هنوز واریز یا برداشت ثبت نشده است.\n"
        );

    } else {

        for (Map.Entry<String, Double> entry
                : cashByPortfolio.entrySet()) {

            result.append(
                    entry.getKey()
            ).append(
                    ": "
            ).append(
                    formatNumber(
                            entry.getValue()
                    )
            ).append(
                    "\n"
            );
        }
    }

    result.append("\n");

    result.append(
            "💵 مجموع بهای تمام‌شده: "
    ).append(
            formatNumber(totalCost)
    ).append("\n");

    result.append(
            "📈 مجموع سود/زیان تحقق‌یافته: "
    ).append(
            formatNumber(totalRealized)
    ).append("\n");

    result.append(
            "🧾 مجموع کارمزدها: "
    ).append(
            formatNumber(totalFees)
    ).append("\n");

    if (!hasPosition) {

        result.append(
                "\nهنوز سهمی در سبدها ثبت نشده است."
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
            "📊 گزارش سبدها\n\n"
    );

    boolean found = false;

    for (PortfolioEngine.Position position
            : positions.values()) {

        if (position.quantity <= 0 &&
                Math.abs(position.realizedProfit) < 0.000001) {
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
                "تعداد فعلی: "
        ).append(
                formatNumber(
                        position.quantity
                )
        ).append("\n");

        result.append(
                "میانگین خرید: "
        ).append(
                formatNumber(
                        position.averagePrice()
                )
        ).append("\n");

        result.append(
                "بهای تمام‌شده: "
        ).append(
                formatNumber(
                        position.cost
                )
        ).append("\n");

        result.append(
                "سود/زیان تحقق‌یافته: "
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

    TextView text =
            createText(
                    result.toString(),
                    16,
                    false
            );

    scroll.addView(text);

    new AlertDialog.Builder(this)
            .setTitle("داشبورد سبدها")
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

    int count = 0;

    try {

        while (cursor.moveToNext()) {

            count++;

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

            long date =
                    cursor.getLong(
                            cursor.getColumnIndexOrThrow(
                                    "date"
                            )
                    );

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
                    "شناسه: "
            ).append(
                    id
            ).append("\n");

            result.append(
                    action
            ).append(
                    " | "
            ).append(
                    formatDate(date)
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

    if (count == 0) {

        result.append(
                "تراکنشی ثبت نشده است."
        );
    }

    ScrollView scroll =
            new ScrollView(this);

    TextView text =
            createText(
                    result.toString(),
                    16,
                    false
            );

    scroll.addView(text);

    new AlertDialog.Builder(this)
            .setTitle("تاریخچه معاملات")
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
