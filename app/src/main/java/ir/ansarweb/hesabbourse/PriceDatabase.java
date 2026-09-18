package ir.ansarweb.hesabbourse;

import android.content.Context;
import android.content.SharedPreferences;

public class PriceDatabase {

    private static final String PREF =
            "market_prices";

    private final SharedPreferences preferences;

    public PriceDatabase(Context context) {
        preferences = context.getSharedPreferences(
                PREF,
                Context.MODE_PRIVATE
        );
    }

    public void setPrice(String symbol, double price) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return;
        }

        preferences.edit()
                .putFloat(
                        symbol.trim(),
                        (float) price
                )
                .apply();
    }

    public double getPrice(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return 0;
        }

        return preferences.getFloat(
                symbol.trim(),
                0
        );
    }

    public void deletePrice(String symbol) {
        if (symbol == null) {
            return;
        }

        preferences.edit()
                .remove(symbol.trim())
                .apply();
    }
}
