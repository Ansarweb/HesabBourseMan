package ir.ansarweb.hesabbourse;

import android.database.Cursor;

import java.util.LinkedHashMap;
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

        // میانگین خرید با احتساب کارمزد خرید
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

        // میانگین بهای تمام‌شده سهم‌های باقی‌مانده
        public double averagePrice() {

            if (quantity <= 0) {
                return 0;
            }

            return cost / quantity;
        }

        // مبلغ نهایی خریدها
        public double totalBuyCost() {
            return buyAmount + buyFees;
        }

        // مبلغ خالص دریافتی از فروش‌ها
        public double totalSellNet() {
            return sellAmount - sellFees;
        }
    }

    public static Map<String, Position> calculate(
            Cursor cursor) {

        Map<String, Position> positions =
                new LinkedHashMap<>();

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

            else if ("SELL".equals(type)) {

                /*
                 * اینجا نباید فروش بیشتر از موجودی
                 * وارد موتور شود.
                 */
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
                        sellQuantity * average;

                /*
                 * اگر مقدار فروش کمتر از مقدار ثبت‌شده
                 * باشد، کارمزد نیز متناسب می‌شود.
                 */
                double effectiveFee =
                        quantity > 0
                                ? fee *
                                (sellQuantity / quantity)
                                : 0;

                double effectiveAmount =
                        quantity > 0
                                ? amount *
                                (sellQuantity / quantity)
                                : 0;

                position.sellQuantity +=
                        sellQuantity;

                position.sellAmount +=
                        effectiveAmount;

                position.sellFees +=
                        effectiveFee;

                /*
                 * سود واقعی:
                 * دریافتی خالص فروش
                 * منهای بهای تمام‌شده سهم فروخته‌شده
                 */
                position.realizedProfit +=
                        effectiveAmount
                                - effectiveFee
                                - costOfSold;

                position.cost -=
                        costOfSold;

                position.quantity -=
                        sellQuantity;

                if (position.quantity < 0.0000001) {

                    position.quantity = 0;
                    position.cost = 0;
                }
            }
        }

        return positions;
    }
}
