package ir.ansarweb.hesabbourse;

import android.app.Activity;
import android.app.AlertDialog;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private DatabaseHelper database;
    private TextView dashboard;

    // کارمزدهای ثابت نسخه 4
    private static final double BUY_FEE_RATE = 0.0037;   // 0.37%
    private static final double SELL_FEE_RATE = 0.0088;  // 0.88%

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

        TextView title = createText(
                "حساب بورس",
                28,
                true
        );

        root.addView(title);

        TextView subtitle = createText(
                "حسابداری پیشرفته معاملات و سبد سرمایه‌گذاری",
                16,
                false
        );

        root.addView(subtitle);

        addButton(root, "➕ ثبت خرید", () -> showTradeDialog("BUY"));
        addButton(root, "➖ ثبت فروش", () -> showTradeDialog("SELL"));
        addButton(root, "💰 ثبت واریز", () -> showCashDialog("DEPOSIT"));
        addButton(root, "💸 ثبت برداشت", () -> showCashDialog("WITHDRAW"));

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

        TextView dashboardTitle = createText(
                "وضعیت فعلی",
                21,
                true
        );

        dashboardTitle.setPadding(0, 25, 0, 5);
        root.addView(dashboardTitle);

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

        view.setPadding(
                0,
                10,
                0,
                10
        );

        if (bold) {
            view.setTypeface(
                    Typeface.DEFAULT,
                    Typeface.BOLD
            );
        }

        return view;
    }

    private void addButton(
            LinearLayout parent,
            String text,
            Runnable action) {

        TextView button = createText(
                text,
                18,
                true
        );

        button.setGravity(
                Gravity.CENTER_VERTICAL
        );

        button.setPadding(
                12,
                18,
                12,
                18
        );

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
                40,
                5,
                40,
                5
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

        if (value == null
                || value.trim().isEmpty()) {

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

        if (value == (long) value) {

            return String.valueOf(
                    (long) value
            );
        }

        return String.format(
                Locale.US,
                "%.2f",
                value
        );
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
                        "0"
                );

        EditText price =
                createField(
                        "قیمت هر سهم",
                        "0"
                );

        form.addView(portfolio);
        form.addView(broker);
        form.addView(symbol);
        form.addView(quantity);
        form.addView(price);

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

                                if (q <= 0 || p <= 0) {
                                    return;
                                }

                                /*
                                 * کارمزد نسخه 4
                                 *
                                 * خرید: 0.37%
                                 * فروش: 0.88%
                                 */

                                double tradeAmount =
                                        q * p;

                                double fee =
                                        type.equals("BUY")
                                                ? tradeAmount
                                                * BUY_FEE_RATE
                                                : tradeAmount
                                                * SELL_FEE_RATE;

                                database.addTransaction(
                                        type,
                                        portfolio
                                                .getText()
                                                .toString(),
                                        broker
                                                .getText()
                                                .toString(),
                                        symbol
                                                .getText()
                                                .toString(),
                                        q,
                                        p,
                                        fee,
                                        tradeAmount
                                );

                                refreshDashboard();

                            } catch (Exception ignored) {
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
                        "0"
                );

        form.addView(portfolio);
        form.addView(amount);

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
                                                amount
                                                        .getText()
                                                        .toString()
                                        );

                                if (value <= 0) {
                                    return;
                                }

                                database.addTransaction(
                                        type,
                                        portfolio
                                                .getText()
                                                .toString(),
                                        "",
                                        "",
                                        0,
                                        0,
                                        0,
                                        value
                                );

                                refreshDashboard();

                            } catch (Exception ignored) {
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

            return PortfolioEngine.calculate(
                    cursor
            );

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

        for (PortfolioEngine.Position position
                : positions.values()) {

            totalRealized +=
                    position.realizedProfit;

            if (position.quantity <= 0) {
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

        double totalCost = 0;
        double totalRealized = 0;

        for (PortfolioEngine.Position position
                : positions.values()) {

            found = true;

            totalCost +=
                    position.cost;

            totalRealized +=
                    position.realizedProfit;

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

        result.append("\n");

        result.append(
                "مجموع بهای تمام‌شده: "
        ).append(
                formatNumber(totalCost)
        ).append("\n");

        result.append(
                "مجموع سود/زیان تحقق‌یافته: "
        ).append(
                formatNumber(totalRealized)
        );

        if (!found) {

            result.insert(
                    0,
                    "هنوز معامله‌ای ثبت نشده است.\n\n"
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

        int count = 0;

        try {

            while (cursor.moveToNext()) {

                count++;

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
                        count
                ).append(
                        ") "
                ).append(
                        action
                );

                result.append(
                        " | سبد: "
                ).append(
                        portfolio == null
                                ? "اصلی"
                                : portfolio
                );

                if (symbol != null
                        && !symbol.trim().isEmpty()) {

                    result.append(
                            " | نماد: "
                    ).append(
                            symbol
                    );
                }

                if (quantity > 0) {

                    result.append(
                            " | تعداد: "
                    ).append(
                            formatNumber(quantity)
                    );
                }

                if (price > 0) {

                    result.append(
                            " | قیمت: "
                    ).append(
                            formatNumber(price)
                    );
                }

                if (amount > 0) {

                    result.append(
                            " | مبلغ: "
                    ).append(
                            formatNumber(amount)
                    );
                }

                if (fee > 0) {

                    result.append(
                            " | کارمزد: "
                    ).append(
                            formatNumber(fee)
                    );
                }

                result.append(
                        "\n\n"
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
