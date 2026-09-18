package ir.ansarweb.hesabbourse;

import java.util.Locale;

public class VoiceTradeParser {

    public static class TradeData {

        public String broker = "";
        public String symbol = "";
        public double amount = 0;
        public double price = 0;
        public boolean isBuy = true;
    }

    public static TradeData parse(String text) {

        TradeData result = new TradeData();

        if (text == null) {
            return result;
        }

        String t = text
                .replace("،", " ")
                .replace(",", " ")
                .trim();

        String lower = t.toLowerCase(Locale.ROOT);

        result.isBuy =
                !lower.contains("فروش");

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
        }

        return result;
    }
}
