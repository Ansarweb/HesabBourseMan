package ir.ansarweb.hesabbourse;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PortfolioEngine {

    public static class Position {

        public String assetType = "STOCK";

        public String portfolio = "";
        public String symbol = "";

        // Option fields
        public String underlying = "";
        public String optionType = "";
        public double strikePrice = 0;
        public String expiryDate = "";
        public double contractSize = 0;

        // Stock / Long option
        public double quantity = 0;
        public double cost = 0;

        // Stock statistics
        public double buyQuantity = 0;
        public double buyAmount = 0;
        public double buyFees = 0;

        public double sellQuantity = 0;
        public double sellAmount = 0;
        public double sellFees = 0;

        // Option statistics
        public double longQuantity = 0;
        public double longCost = 0;

        public double shortQuantity = 0;
        public double shortCredit = 0;

        public double realizedProfit = 0;
        public double realizedCost = 0;

        public boolean isOption() {
            return "OPTION".equalsIgnoreCase(assetType);
        }

        public boolean isStock() {
            return !isOption();
        }

        public double averagePrice() {
            if (isOption()) {
                if (longQuantity > 0) {
                    return longCost / longQuantity;
                }
                return 0;
            }

            if (quantity <= 0) return 0;
            return cost / quantity;
        }

        public double realizedProfitPercent() {
            if (realizedCost == 0) return 0;
            return (realizedProfit / realizedCost) * 100.0;
        }

        public boolean hasRealizedProfit() {
            return Math.abs(realizedProfit) > 0.0000001;
        }

        public double netOptionPosition() {
            return longQuantity - shortQuantity;
        }

        public String optionKey() {
            return portfolio + "|" +
                    underlying + "|" +
                    optionType + "|" +
                    strikePrice + "|" +
                    expiryDate + "|" +
                    contractSize;
        }
    }

    private static class TradeRecord {

        long id;
        long date;

        String type;
        String assetType;
        String portfolio;
        String symbol;

        String underlying;
        String optionType;
        double strikePrice;
        String expiryDate;
        double contractSize;
        String positionType;

        double quantity;
        double amount;
        double fee;
        double price;
    }

    public static Map<String, Position> calculate(Cursor cursor) {

        List<TradeRecord> trades = new ArrayList<>();

        if (cursor == null) {
            return new LinkedHashMap<>();
        }

        int idIndex = cursor.getColumnIndex("id");
        int dateIndex = cursor.getColumnIndex("date");
        int typeIndex = cursor.getColumnIndex("type");
        int assetTypeIndex = cursor.getColumnIndex("asset_type");
        int portfolioIndex = cursor.getColumnIndex("portfolio");
        int symbolIndex = cursor.getColumnIndex("symbol");
        int underlyingIndex = cursor.getColumnIndex("underlying");
        int optionTypeIndex = cursor.getColumnIndex("option_type");
        int strikeIndex = cursor.getColumnIndex("strike_price");
        int expiryIndex = cursor.getColumnIndex("expiry_date");
        int contractSizeIndex = cursor.getColumnIndex("contract_size");
        int positionTypeIndex = cursor.getColumnIndex("position_type");
        int quantityIndex = cursor.getColumnIndex("quantity");
        int amountIndex = cursor.getColumnIndex("amount");
        int feeIndex = cursor.getColumnIndex("fee");
        int priceIndex = cursor.getColumnIndex("price");

        while (cursor.moveToNext()) {

            TradeRecord t = new TradeRecord();

            t.id = idIndex >= 0 ? cursor.getLong(idIndex) : 0;
            t.date = dateIndex >= 0 ? cursor.getLong(dateIndex) : 0;

            t.type = typeIndex >= 0 ? cursor.getString(typeIndex) : "";
            t.assetType = assetTypeIndex >= 0
                    ? cursor.getString(assetTypeIndex)
                    : "STOCK";

            if (t.assetType == null || t.assetType.trim().isEmpty()) {
                t.assetType = "STOCK";
            }

            t.portfolio = portfolioIndex >= 0
                    ? cursor.getString(portfolioIndex)
                    : "اصلی";

            t.symbol = symbolIndex >= 0
                    ? cursor.getString(symbolIndex)
                    : "";

            t.underlying = underlyingIndex >= 0
                    ? cursor.getString(underlyingIndex)
                    : "";

            t.optionType = optionTypeIndex >= 0
                    ? cursor.getString(optionTypeIndex)
                    : "";

            t.strikePrice = strikeIndex >= 0
                    ? cursor.getDouble(strikeIndex)
                    : 0;

            t.expiryDate = expiryIndex >= 0
                    ? cursor.getString(expiryIndex)
                    : "";

            t.contractSize = contractSizeIndex >= 0
                    ? cursor.getDouble(contractSizeIndex)
                    : 0;

            t.positionType = positionTypeIndex >= 0
                    ? cursor.getString(positionTypeIndex)
                    : "";

            t.quantity = quantityIndex >= 0
                    ? cursor.getDouble(quantityIndex)
                    : 0;

            t.amount = amountIndex >= 0
                    ? cursor.getDouble(amountIndex)
                    : 0;

            t.fee = feeIndex >= 0
                    ? cursor.getDouble(feeIndex)
                    : 0;

            t.price = priceIndex >= 0
                    ? cursor.getDouble(priceIndex)
                    : 0;

            trades.add(t);
        }

        /*
         * مهم:
         * تاریخچه دیتابیس ممکن است DESC باشد.
         * برای محاسبه درست سود و میانگین باید معاملات
         * از قدیمی به جدید پردازش شوند.
         */
        Collections.sort(trades, new Comparator<TradeRecord>() {
            @Override
            public int compare(TradeRecord a, TradeRecord b) {

                int dateCompare = Long.compare(a.date, b.date);

                if (dateCompare != 0) {
                    return dateCompare;
                }

                return Long.compare(a.id, b.id);
            }
        });

        Map<String, Position> positions = new LinkedHashMap<>();

        for (TradeRecord trade : trades) {

            if ("DEPOSIT".equalsIgnoreCase(trade.type)
                    || "WITHDRAW".equalsIgnoreCase(trade.type)) {
                continue;
            }

            if ("OPTION".equalsIgnoreCase(trade.assetType)) {

                processOptionTrade(
                        positions,
                        trade
                );

            } else {

                processStockTrade(
                        positions,
                        trade
                );
            }
        }

        return positions;
    }

    // =========================================================
    // STOCK
    // =========================================================

    private static void processStockTrade(
            Map<String, Position> positions,
            TradeRecord trade
    ) {

        String portfolio = safe(trade.portfolio);
        String symbol = safe(trade.symbol);

        if (symbol.isEmpty()) {
            return;
        }

        String key = "STOCK|" + portfolio + "|" + symbol;

        Position position = positions.get(key);

        if (position == null) {

            position = new Position();

            position.assetType = "STOCK";
            position.portfolio = portfolio;
            position.symbol = symbol;

            positions.put(key, position);
        }

        if ("BUY".equalsIgnoreCase(trade.type)) {

            double qty = Math.max(0, trade.quantity);
            double amount = Math.max(0, trade.amount);
            double fee = Math.max(0, trade.fee);

            if (qty <= 0) {
                return;
            }

            position.quantity += qty;
            position.cost += amount + fee;

            position.buyQuantity += qty;
            position.buyAmount += amount;
            position.buyFees += fee;

        } else if ("SELL".equalsIgnoreCase(trade.type)) {

            double requestedQty = Math.max(0, trade.quantity);

            if (requestedQty <= 0 || position.quantity <= 0) {
                return;
            }

            double sellQty = Math.min(
                    requestedQty,
                    position.quantity
            );

            double ratio = requestedQty == 0
                    ? 0
                    : sellQty / requestedQty;

            double effectiveAmount = trade.amount * ratio;
            double effectiveFee = trade.fee * ratio;

            double averageCost =
                    position.quantity > 0
                            ? position.cost / position.quantity
                            : 0;

            double costOfSold =
                    sellQty * averageCost;

            double netSale =
                    effectiveAmount - effectiveFee;

            double realized =
                    netSale - costOfSold;

            position.realizedProfit += realized;
            position.realizedCost += costOfSold;

            position.sellQuantity += sellQty;
            position.sellAmount += effectiveAmount;
            position.sellFees += effectiveFee;

            position.quantity -= sellQty;
            position.cost -= costOfSold;

            if (Math.abs(position.quantity) < 0.0000001) {
                position.quantity = 0;
            }

            if (Math.abs(position.cost) < 0.0000001) {
                position.cost = 0;
            }
        }
    }

    // =========================================================
    // OPTIONS
    // =========================================================

    private static void processOptionTrade(
            Map<String, Position> positions,
            TradeRecord trade
    ) {

        String portfolio = safe(trade.portfolio);
        String underlying = safe(trade.underlying);
        String optionType = safe(trade.optionType);
        String expiry = safe(trade.expiryDate);

        String key =
                "OPTION|"
                        + portfolio + "|"
                        + underlying + "|"
                        + optionType + "|"
                        + trade.strikePrice + "|"
                        + expiry + "|"
                        + trade.contractSize;

        Position position = positions.get(key);

        if (position == null) {

            position = new Position();

            position.assetType = "OPTION";
            position.portfolio = portfolio;
            position.symbol = safe(trade.symbol);

            position.underlying = underlying;
            position.optionType = optionType;
            position.strikePrice = trade.strikePrice;
            position.expiryDate = expiry;
            position.contractSize = trade.contractSize;

            positions.put(key, position);
        }

        String positionType = safe(trade.positionType).toUpperCase();

        /*
         * اگر position_type خالی باشد،
         * برای سازگاری با داده‌های قدیمی LONG در نظر گرفته می‌شود.
         */
        if (positionType.isEmpty()) {
            positionType = "LONG";
        }

        double qty = Math.max(0, trade.quantity);
        double amount = Math.max(0, trade.amount);
        double fee = Math.max(0, trade.fee);

        if (qty <= 0) {
            return;
        }

        // -----------------------------------------------------
        // LONG
        // -----------------------------------------------------

        if ("LONG".equals(positionType)) {

            /*
             * BUY + LONG
             * باز کردن / افزایش موقعیت لانگ
             */
            if ("BUY".equalsIgnoreCase(trade.type)) {

                position.longQuantity += qty;
                position.longCost += amount + fee;

                position.buyQuantity += qty;
                position.buyAmount += amount;
                position.buyFees += fee;

            }

            /*
             * SELL + LONG
             * بستن موقعیت لانگ
             */
            else if ("SELL".equalsIgnoreCase(trade.type)) {

                if (position.longQuantity <= 0) {
                    return;
                }

                double closeQty =
                        Math.min(qty, position.longQuantity);

                double ratio =
                        qty > 0
                                ? closeQty / qty
                                : 0;

                double effectiveAmount =
                        amount * ratio;

                double effectiveFee =
                        fee * ratio;

                double averageLongCost =
                        position.longQuantity > 0
                                ? position.longCost
                                / position.longQuantity
                                : 0;

                double costOfClosed =
                        closeQty * averageLongCost;

                double netSale =
                        effectiveAmount - effectiveFee;

                double realized =
                        netSale - costOfClosed;

                position.realizedProfit += realized;
                position.realizedCost += costOfClosed;

                position.longQuantity -= closeQty;
                position.longCost -= costOfClosed;

                position.sellQuantity += closeQty;
                position.sellAmount += effectiveAmount;
                position.sellFees += effectiveFee;

                cleanOptionNumbers(position);
            }
        }

        // -----------------------------------------------------
        // SHORT
        // -----------------------------------------------------

        else if ("SHORT".equals(positionType)) {

            /*
             * SELL + SHORT
             * باز کردن / افزایش موقعیت شورت
             *
             * وجه خالص دریافتی:
             * premium - fee
             */
            if ("SELL".equalsIgnoreCase(trade.type)) {

                double netCredit =
                        amount - fee;

                position.shortQuantity += qty;
                position.shortCredit += netCredit;

                position.sellQuantity += qty;
                position.sellAmount += amount;
                position.sellFees += fee;
            }

            /*
             * BUY + SHORT
             * بستن موقعیت شورت
             */
            else if ("BUY".equalsIgnoreCase(trade.type)) {

                if (position.shortQuantity <= 0) {
                    return;
                }

                double closeQty =
                        Math.min(qty, position.shortQuantity);

                double ratio =
                        qty > 0
                                ? closeQty / qty
                                : 0;

                double effectiveAmount =
                        amount * ratio;

                double effectiveFee =
                        fee * ratio;

                double creditPerContract =
                        position.shortQuantity > 0
                                ? position.shortCredit
                                / position.shortQuantity
                                : 0;

                double openingCredit =
                        creditPerContract * closeQty;

                double closingCost =
                        effectiveAmount + effectiveFee;

                double realized =
                        openingCredit - closingCost;

                position.realizedProfit += realized;

                /*
                 * برای درصد سود شورت،
                 * مبلغ دریافتی اولیه را مبنای محاسبه نگه می‌داریم.
                 */
                position.realizedCost += openingCredit;

                position.shortQuantity -= closeQty;
                position.shortCredit -= openingCredit;

                position.buyQuantity += closeQty;
                position.buyAmount += effectiveAmount;
                position.buyFees += effectiveFee;

                cleanOptionNumbers(position);
            }
        }
    }

    private static void cleanOptionNumbers(Position position) {

        if (Math.abs(position.longQuantity) < 0.0000001) {
            position.longQuantity = 0;
        }

        if (Math.abs(position.longCost) < 0.0000001) {
            position.longCost = 0;
        }

        if (Math.abs(position.shortQuantity) < 0.0000001) {
            position.shortQuantity = 0;
        }

        if (Math.abs(position.shortCredit) < 0.0000001) {
            position.shortCredit = 0;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
