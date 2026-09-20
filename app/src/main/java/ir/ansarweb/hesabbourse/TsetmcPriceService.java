package ir.ansarweb.hesabbourse;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TsetmcPriceService {

    public interface Callback {
        void onSuccess(String symbol, double lastPrice, double closingPrice);
        void onError(String symbol, String message);
    }

    private static final String SEARCH_URL =
            "https://cdn.tsetmc.com/api/Instrument/GetInstrumentSearch/";

    private static final String PRICE_URL =
            "https://cdn.tsetmc.com/api/ClosingPrice/GetClosingPriceInfo/";

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public void getPrice(final String symbol, final Callback callback) {

        if (symbol == null || symbol.trim().isEmpty()) {
            callback.onError(symbol, "نماد وارد نشده است");
            return;
        }

        final String cleanSymbol = symbol.trim();

        new Thread(() -> {

            try {

                // مرحله اول: پیدا کردن کد معاملاتی نماد
                String encodedSymbol =
                        URLEncoder.encode(cleanSymbol, "UTF-8");

                String searchResponse =
                        request(SEARCH_URL + encodedSymbol);

                String insCode =
                        findInsCode(searchResponse, cleanSymbol);

                if (insCode == null || insCode.isEmpty()) {
                    postError(
                            callback,
                            cleanSymbol,
                            "کد معاملاتی نماد پیدا نشد"
                    );
                    return;
                }

                // مرحله دوم: دریافت قیمت
                String priceResponse =
                        request(PRICE_URL + insCode);

                JSONObject root =
                        new JSONObject(priceResponse);

                JSONObject closingPriceInfo =
                        root.optJSONObject("closingPriceInfo");

                if (closingPriceInfo == null) {
                    postError(
                            callback,
                            cleanSymbol,
                            "اطلاعات قیمت دریافت نشد"
                    );
                    return;
                }

                double lastPrice =
                        closingPriceInfo.optDouble(
                                "pClosing",
                                0
                        );

                double closingPrice =
                        closingPriceInfo.optDouble(
                                "pClosing",
                                0
                        );

                if (lastPrice <= 0) {
                    lastPrice =
                            closingPriceInfo.optDouble(
                                    "pDrCotVal",
                                    0
                            );
                }

                final double finalLastPrice = lastPrice;
                final double finalClosingPrice = closingPrice;

                mainHandler.post(() ->
                        callback.onSuccess(
                                cleanSymbol,
                                finalLastPrice,
                                finalClosingPrice
                        )
                );

            } catch (Exception e) {

                postError(
                        callback,
                        cleanSymbol,
                        e.getMessage() == null
                                ? "خطا در دریافت اطلاعات بازار"
                                : e.getMessage()
                );
            }

        }).start();
    }

    private String request(String address) throws Exception {

        URL url = new URL(address);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0"
        );
        connection.setRequestProperty(
                "Accept",
                "application/json"
        );

        int responseCode =
                connection.getResponseCode();

        InputStream inputStream;

        if (responseCode >= 200 && responseCode < 300) {
            inputStream =
                    connection.getInputStream();
        } else {
            inputStream =
                    connection.getErrorStream();
        }

        if (inputStream == null) {
            throw new Exception(
                    "پاسخی از سرور دریافت نشد"
            );
        }

        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                inputStream,
                                "UTF-8"
                        )
                );

        StringBuilder result =
                new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {
            result.append(line);
        }

        reader.close();
        connection.disconnect();

        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception(
                    "خطای سرور: " + responseCode
            );
        }

        return result.toString();
    }

    private String findInsCode(
            String response,
            String wantedSymbol
    ) throws Exception {

        JSONObject root =
                new JSONObject(response);

        String[] possibleArrays = {
                "instrumentSearch",
                "instrumentSearchResults",
                "instrument"
        };

        for (String arrayName : possibleArrays) {

            if (!root.has(arrayName)) {
                continue;
            }

            Object value =
                    root.get(arrayName);

            if (!(value instanceof org.json.JSONArray)) {
                continue;
            }

            org.json.JSONArray array =
                    (org.json.JSONArray) value;

            for (int i = 0; i < array.length(); i++) {

                JSONObject item =
                        array.optJSONObject(i);

                if (item == null) {
                    continue;
                }

                String symbol =
                        item.optString(
                                "lVal18AFC",
                                ""
                        );

                if (symbol.isEmpty()) {
                    symbol =
                            item.optString(
                                    "symbol",
                                    ""
                            );
                }

                String name =
                        item.optString(
                                "lVal30",
                                ""
                        );

                if (name.isEmpty()) {
                    name =
                            item.optString(
                                    "name",
                                    ""
                            );
                }

                if (wantedSymbol.equalsIgnoreCase(symbol)
                        || wantedSymbol.equalsIgnoreCase(name)) {

                    String insCode =
                            item.optString(
                                    "insCode",
                                    ""
                            );

                    if (insCode.isEmpty()) {
                        insCode =
                                item.optString(
                                        "instrumentID",
                                        ""
                                );
                    }

                    return insCode;
                }
            }
        }

        return null;
    }

    private void postError(
            Callback callback,
            String symbol,
            String message
    ) {

        mainHandler.post(() ->
                callback.onError(
                        symbol,
                        message
                )
        );
    }
}
