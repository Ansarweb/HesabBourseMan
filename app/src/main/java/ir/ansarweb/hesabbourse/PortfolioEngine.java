package ir.ansarweb.hesabbourse;

import android.database.Cursor;
import java.util.LinkedHashMap;
import java.util.Map;

public class PortfolioEngine {

    public static class Position {
        public String portfolio;
        public String symbol;
        public double quantity;
        public double cost;
        public double realizedProfit;

        public Position(String portfolio, String symbol) {
            this.portfolio = portfolio;
            this.symbol = symbol;
        }

        public double averagePrice() {
            return quantity > 0 ? cost / quantity : 0;
        }
    }

    public static Map<String, Position> calculate(Cursor cursor) {

        Map<String, Position> positions =
                new LinkedHashMap<>();

        while (cursor.moveToNext()) {

            String type = cursor.getString(
                    cursor.getColumnIndexOrThrow("type"));

            String portfolio = cursor.getString(
                    cursor.getColumnIndexOrThrow("portfolio"));

            String symbol = cursor.getString(
                    cursor.getColumnIndexOrThrow("symbol"));

            if (portfolio == null ||
                    portfolio.trim().isEmpty()) {
                portfolio = "اصلی";
            }

            if (symbol == null ||
                    symbol.trim().isEmpty()) {
                continue;
            }

            String key = portfolio + "|" + symbol;

            Position position = positions.get(key);

            if (position == null) {
                position = new Position(portfolio, symbol);
                positions.put(key, position);
            }

            double quantity = cursor.getDouble(
                    cursor.getColumnIndexOrThrow("quantity"));

            double price = cursor.getDouble(
                    cursor.getColumnIndexOrThrow("price"));

            double fee = cursor.getDouble(
                    cursor.getColumnIndexOrThrow("fee"));

            if ("BUY".equals(type)) {

                position.cost +=
                        (quantity * price) + fee;

                position.quantity += quantity;

            } else if ("SELL".equals(type)) {

                if (position.quantity <= 0) {
                    continue;
                }

                double sellQuantity =
                        Math.min(quantity, position.quantity);

                double average =
                        position.averagePrice();

                double saleValue =
                        sellQuantity * price;

                double costOfSold =
                        sellQuantity * average;

                position.realizedProfit +=
                        saleValue - costOfSold - fee;

                position.cost -= costOfSold;
                position.quantity -= sellQuantity;

                if (position.quantity < 0.0000001) {
                    position.quantity = 0;
                    position.cost = 0;
                }
            }
        }

        return positions;
    }
}
