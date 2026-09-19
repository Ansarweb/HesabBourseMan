package ir.ansarweb.hesabbourse;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PortfolioEngine {

    public static class Position {

        public String portfolio;
        public String symbol;

        // موجودی فعلی
        public double quantity;

        // بهای تمام شده موجودی فعلی
        public double cost;

        // اطلاعات کل خریدها
        public double buyQuantity;
        public double buyAmount;
        public double buyFees;

        // اطلاعات کل فروش‌ها
        public double sellQuantity;
        public double sellAmount;
        public double sellFees;

        // سود تحقق یافته
        public double realizedProfit;

        public Position(
                String portfolio,
                String symbol) {

            this.portfolio = portfolio;
            this.symbol = symbol;
        }

        // میانگین قیمت تمام شده خریدها با احتساب کارمزد
        public double averageBuyPrice() {

            if (buyQuantity <= 0) {
                return 0;
            }

            return (buyAmount + buyFees)
                    / buyQuantity;
        }

        // میانگین قیمت فروش
        public double averageSellPrice() {

            if (sellQuantity <= 0) {
                return 0;
            }

            return sellAmount
                    / sellQuantity;
        }

        // میانگین بهای تمام شده موجودی فعلی
        public double averagePrice() {

            if (quantity <= 0) {
                return 0;
            }

            return cost / quantity;
        }

        // کل هزینه خریدها
        public double totalBuyCost() {

            return buyAmount + buyFees;
        }

        // خالص دریافتی از فروش‌ها
        public double totalSellNet() {

            return sellAmount - sellFees;
        }
    }

    /*
     * یک رکورد موقت برای اینکه بتوانیم معاملات را
     * مستقل از ترتیب Cursor مرتب کنیم.
     */
    private static class TradeRecord {

        String type;
        String portfolio;
        String symbol;

        double quantity;
        double price;
        double fee;
        double amount;

        long date;
        long id;
    }

    public static Map<String, Position> calculate(
            Cursor cursor) {

        Map<String, Position> positions =
                new LinkedHashMap<>();

        if (cursor == null) {
            return positions;
        }

        /*
         * بسیار مهم:
         *
         * Cursor دیتابیس ممکن است DESC باشد.
         * بنابراین ابتدا تمام معاملات را می‌خوانیم
         * و سپس آنها را بر اساس تاریخ و ID از قدیمی
         * به جدید مرتب می‌کنیم.
         */
        List<TradeRecord> trades =
                new ArrayList<>();

        int typeIndex =
                cursor.getColumnIndexOrThrow("type");

        int portfolioIndex =
                cursor.getColumnIndexOrThrow("portfolio");

        int symbolIndex =
                cursor.getColumnIndexOrThrow("symbol");

        int quantityIndex =
                cursor.getColumnIndexOrThrow("quantity");

        int priceIndex =
                cursor.getColumnIndexOrThrow("price");

        int feeIndex =
                cursor.getColumnIndexOrThrow("fee");

        int amountIndex =
                cursor.getColumnIndexOrThrow("amount");

        int dateIndex =
                cursor.getColumnIndexOrThrow("date");

        int idIndex =
                cursor.getColumnIndexOrThrow("id");

        while (cursor.moveToNext()) {

            TradeRecord trade =
                    new TradeRecord();

            trade.type =
                    cursor.getString(typeIndex);

            trade.portfolio =
                    cursor.getString(portfolioIndex);

            trade.symbol =
                    cursor.getString(symbolIndex);

            trade.quantity =
                    cursor.getDouble(quantityIndex);

            trade.price =
                    cursor.getDouble(priceIndex);

            trade.fee =
                    cursor.getDouble(feeIndex);

            trade.amount =
                    cursor.getDouble(amountIndex);

            trade.date =
                    cursor.getLong(dateIndex);

            trade.id =
                    cursor.getLong(idIndex);

            trades.add(trade);
        }

        /*
         * قدیمی‌ترین معامله اول.
         *
         * اگر دو معامله دقیقاً زمان یکسان داشته باشند،
         * ID تعیین‌کننده ترتیب خواهد بود.
         */
        trades.sort(
                new Comparator<TradeRecord>() {

                    @Override
                    public int compare(
                            TradeRecord a,
                            TradeRecord b) {

                        int dateCompare =
                                Long.compare(
                                        a.date,
                                        b.date
                                );

                        if (dateCompare != 0) {
                            return dateCompare;
                        }

                        return Long.compare(
                                a.id,
                                b.id
                        );
                    }
                }
        );

        /*
         * حالا معاملات را به ترتیب صحیح پردازش می‌کنیم.
         */
        for (TradeRecord trade : trades) {

            String type =
                    trade.type;

            String portfolio =
                    trade.portfolio;

            String symbol =
                    trade.symbol;

            if (portfolio == null ||
                    portfolio.trim().isEmpty()) {

                portfolio = "اصلی";
            }

            if (symbol == null ||
                    symbol.trim().isEmpty()) {

                continue;
            }

            String key =
                    portfolio + "|" + symbol;

            Position position =
                    positions.get(key);

            if (position == null) {

                position =
                        new Position(
                                portfolio,
                                symbol
                        );

                positions.put(
                        key,
                        position
                );
            }

            double quantity =
                    trade.quantity;

            double fee =
                    trade.fee;

            double amount =
                    trade.amount;

            /*
             * =========================
             * خرید
             * =========================
             */
            if ("BUY".equals(type)) {

                double buyCost =
                        amount + fee;

                position.buyQuantity +=
                        quantity;

                position.buyAmount +=
                        amount;

                position.buyFees +=
                        fee;

                position.cost +=
                        buyCost;

                position.quantity +=
                        quantity;
            }

            /*
             * =========================
             * فروش
             * =========================
             */
            else if ("SELL".equals(type)) {

                /*
                 * فروش نمی‌تواند بیشتر از موجودی
                 * واقعی باشد.
                 */
                double sellQuantity =
                        Math.min(
                                quantity,
                                position.quantity
                        );

                if (sellQuantity <= 0) {
                    continue;
                }

                /*
                 * میانگین بهای تمام شده درستِ
                 * موجودی در لحظه فروش.
                 */
                double average =
                        position.quantity > 0
                                ? position.cost
                                / position.quantity
                                : 0;

                /*
                 * بهای تمام شده سهم‌هایی که فروخته شده‌اند.
                 */
                double costOfSold =
                        sellQuantity * average;

                /*
                 * اگر فروش کامل باشد،
                 * کل مبلغ و کارمزد اعمال می‌شود.
                 *
                 * اگر فروش ثبت‌شده بیشتر از موجودی باشد،
                 * مبلغ و کارمزد متناسب با مقدار واقعی
                 * قابل فروش محاسبه می‌شود.
                 */
                double ratio =
                        quantity > 0
                                ? sellQuantity / quantity
                                : 0;

                double effectiveAmount =
                        amount * ratio;

                double effectiveFee =
                        fee * ratio;

                /*
                 * ثبت آمار فروش
                 */
                position.sellQuantity +=
                        sellQuantity;

                position.sellAmount +=
                        effectiveAmount;

                position.sellFees +=
                        effectiveFee;

                /*
                 * سود/زیان تحقق‌یافته:
                 *
                 * خالص دریافتی فروش
                 * منهای بهای تمام‌شده سهم فروخته‌شده
                 */
                double netSell =
                        effectiveAmount
                                - effectiveFee;

                position.realizedProfit +=
                        netSell
                                - costOfSold;

                /*
                 * کاهش موجودی
                 */
                position.quantity -=
                        sellQuantity;

                /*
                 * کاهش بهای تمام‌شده
                 */
                position.cost -=
                        costOfSold;

                /*
                 * جلوگیری از خطای اعشاری
                 */
                if (Math.abs(position.quantity)
                        < 0.0000001) {

                    position.quantity = 0;
                }

                if (Math.abs(position.cost)
                        < 0.0000001) {

                    position.cost = 0;
                }

                /*
                 * هیچ‌وقت موجودی یا هزینه منفی نشود.
                 */
                if (position.quantity < 0) {
                    position.quantity = 0;
                }

                if (position.cost < 0) {
                    position.cost = 0;
                }
            }
        }

        return positions;
    }
}
