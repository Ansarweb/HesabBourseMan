package ir.ansarweb.hesabbourse;

import android.database.Cursor;

import java.util.HashMap;
import java.util.Map;

public class PortfolioEngine {

    public static class Position {

        public double quantity;
        public double cost;
        public double realizedProfit;

        public double averagePrice() {
            if (quantity <= 0) {
                return 0;
            }
            return cost / quantity;
        }
    }

    public static Map<String, Position> calculate(Cursor cursor) {

        Map<String, Position> positions = new HashMap<>();

        while (cursor.moveToNext()) {

            String type = cursor.getString(
                    cursor.getColumnIndexOrThrow("type")
            );

            String symbol = cursor.getString(
                    cursor.getColumnIndexOrThrow("symbol")
            );

            if (symbol == null || symbol.trim().isEmpty()) {
                continue;
            }

            double quantity = cursor.getDouble(
                    cursor.getColumnIndexOrThrow("quantity")
            );

            double price = cursor.getDouble(
                    cursor.getColumnIndexOrThrow("price")
            );

            double fee = cursor.getDouble(
                    cursor.getColumnIndexOrThrow("fee")
            );

            Position position = positions.get(symbol);

            if (position == null) {
                position = new Position();
                positions.put(symbol, position);
            }

            if ("BUY".equals(type)) {

                position.cost +=
                        (quantity * price) + fee;

                position.quantity += quantity;

            } else if ("SELL".equals(type)) {

                double sellQuantity =
                        Math.min(quantity, position.quantity);

                double average =
                        position.averagePrice();

                double sellValue =
                        sellQuantity * price;

                double costOfSold =
                        sellQuantity * average;

                position.realizedProfit +=
                        sellValue - costOfSold - fee;

                position.cost -= costOfSold;

                position.quantity -= sellQuantity;

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
