package ir.ansarweb.hesabbourse;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceTradeParser {

    public static class TradeData {
        public String portfolio = "اصلی";
        public String broker = "";
        public String symbol = "";
        public double amount = 0;
        public double price = 0;
        public double quantity = 0;
        public boolean isBuy = true;
    }

    public static TradeData parse(String text) {

        TradeData result = new TradeData();

        if (text == null || text.trim().isEmpty()) {
            return result;
        }

        String t = normalize(text);
        String lower = t.toLowerCase(Locale.ROOT);

        result.isBuy = !lower.contains("فروش");
        result.portfolio = extractPortfolio(t);
        result.broker = extractBroker(t);

        String[] words = t.split("\\s+");

        for (String word : words) {

            String clean = cleanToken(word);

            if (isLikelySymbol(clean)) {
                result.symbol = clean;
            }
        }

        result.price = extractPrice(t);
        result.quantity = extractQuantity(t);
        result.amount = extractAmount(t);

        if (result.quantity <= 0 &&
                result.amount > 0 &&
                result.price > 0) {

            result.quantity =
                    Math.floor(result.amount / result.price);
        }

        if (result.amount <= 0 &&
                result.quantity > 0 &&
                result.price > 0) {

            result.amount =
                    result.quantity * result.price;
        }

        return result;
    }

    private static String normalize(String text) {

        return text
                .replace('۰', '0')
                .replace('۱', '1')
                .replace('۲', '2')
                .replace('۳', '3')
                .replace('۴', '4')
                .replace('۵', '5')
                .replace('۶', '6')
                .replace('۷', '7')
                .replace('۸', '8')
                .replace('۹', '9')
                .replace('٬', ' ')
                .replace('،', ' ')
                .replace(',', ' ')
                .replace('٫', '.')
                .replace("تومنی", "تومان")
                .replace("تومن", "تومان")
                .replace("سهمی", "سهم")
                .trim();
    }

    private static String extractPortfolio(String text) {

        Pattern p = Pattern.compile(
                "(?:سبد|سد)\\s+([^\\s]+)"
        );

        Matcher m = p.matcher(text);

        if (m.find()) {

            String name =
                    cleanToken(m.group(1));

            if (!name.isEmpty()
                    && !isNumber(name)
                    && !name.equals("خرید")
                    && !name.equals("فروش")) {

                return name;
            }
        }

        return "اصلی";
    }

    private static String extractBroker(String text) {

        String[] brokers = {
                "مفید",
                "آگاه",
                "فارابی",
                "مبین سرمایه",
                "معین"
        };

        for (String broker : brokers) {

            if (text.contains(broker)) {
                return broker;
            }
        }

        return "";
    }

    private static double extractAmount(String text) {

        Pattern p = Pattern.compile(
                "(\\d+(?:\\.\\d+)?)\\s*" +
                "(میلیارد|میلیون|هزار)?\\s*تومان"
        );

        Matcher m = p.matcher(text);

        if (!m.find()) {
            return 0;
        }

        double number =
                Double.parseDouble(m.group(1));

        String unit = m.group(2);

        if ("میلیارد".equals(unit)) {
            return number * 1000000000d;
        }

        if ("میلیون".equals(unit)) {
            return number * 1000000d;
        }

        if ("هزار".equals(unit)) {
            return number * 1000d;
        }

        return number;
    }

    private static double extractPrice(String text) {

        Pattern p = Pattern.compile(
                "(\\d+(?:\\.\\d+)?)\\s*" +
                "(?:تومان|سهم|تومانی)"
        );

        Matcher m = p.matcher(text);

        double last = 0;

        while (m.find()) {
            last = Double.parseDouble(m.group(1));
        }

        return last;
    }

    private static double extractQuantity(String text) {

        Pattern p = Pattern.compile(
                "(\\d+(?:\\.\\d+)?)\\s*" +
                "(?:تا\\s*)?(?:دونه|عدد|سهم)"
        );

        Matcher m = p.matcher(text);

        double last = 0;

        while (m.find()) {
            last = Double.parseDouble(m.group(1));
        }

        return last;
    }

    private static boolean isLikelySymbol(String word) {

        if (word == null ||
                word.isEmpty() ||
                isNumber(word)) {

            return false;
        }

        String w =
                word.replace(" ", "");

        String[] ignored = {
                "خرید",
                "فروش",
                "سبد",
                "سد",
                "تومان",
                "میلیون",
                "میلیارد",
                "هزار",
                "هر",
                "سهم",
                "سهمی",
                "تا",
                "دونه",
                "عدد",
                "اصلی",
                "کارگزاری"
        };

        for (String x : ignored) {

            if (w.equals(x)) {
                return false;
            }
        }

        return w.matches("[آ-ی]{2,8}");
    }

    private static String cleanToken(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("،", "")
                .replace(",", "")
                .replace(".", "")
                .trim();
    }

    private static boolean isNumber(String value) {

        try {
            Double.parseDouble(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
