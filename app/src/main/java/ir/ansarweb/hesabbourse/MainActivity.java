package ir.ansarweb.hesabbourse;

import java.util.Locale;

public class VoiceTradeParser {

    public static class TradeData {

        public String broker = "";
        public String symbol = "";
        public double amount = 0;
        public double price = 0;
        public double quantity = 0;
        public boolean isBuy = true;
    }

    public static TradeData parse(String text) {

        TradeData result = new TradeData();

        if (text == null) {
            return result;
        }

        String t = normalize(text);

        String lower = t.toLowerCase(Locale.ROOT);

        result.isBuy = !lower.contains("فروش");

        String[] words = t.split("\\s+");

        for (int i = 0; i < words.length; i++) {

            String word = words[i];

            if (word.equals("خرید")) {
                result.isBuy = true;
            }

            if (word.equals("فروش")) {
                result.isBuy = false;
            }

            if (word.equals("مفید")) {
                result.broker = "مفید";
            }

            if (word.equals("معین")) {
                result.broker = "معین";
            }

            if (word.equals("ومعادن")) {
                result.symbol = "ومعادن";
            }

            if (word.equals("فولاد")) {
                result.symbol = "فولاد";
            }

            if (word.equals("دابور")) {
                result.symbol = "دابور";
            }

            if (word.equals("دپارس")) {
                result.symbol = "دپارس";
            }
        }

        result.amount = extractAmount(t);
        result.price = extractPrice(t);

        if (result.price > 0 && result.amount > 0) {
            result.quantity =
                    Math.floor(result.amount / result.price);
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
                .replace("،", " ")
                .replace(",", " ")
                .replace("٫", ".")
                .trim();
    }

    private static double extractAmount(String text) {

        String[] words = text.split("\\s+");

        for (int i = 0; i < words.length; i++) {

            try {

                double number =
                        Double.parseDouble(words[i]);

                if (i + 1 < words.length) {

                    String next = words[i + 1];

                    if (next.contains("میلیون")) {
                        return number * 1000000;
                    }

                    if (next.contains("هزار")) {
                        return number * 1000;
                    }
                }

            } catch (Exception ignored) {
            }
        }

        return 0;
    }

    private static double extractPrice(String text) {

        String[] words = text.split("\\s+");

        for (int i = 0; i < words.length; i++) {

            if (words[i].contains("قیمت") ||
                words[i].contains("سهم")) {

                for (int j = i + 1;
                     j < Math.min(i + 5, words.length);
                     j++) {

                    try {

                        return Double.parseDouble(words[j]);

                    } catch (Exception ignored) {
                    }
                }
            }
        }

        return 0;
    }
}
