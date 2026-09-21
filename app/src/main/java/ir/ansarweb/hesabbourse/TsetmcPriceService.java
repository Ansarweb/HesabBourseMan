package ir.ansarweb.hesabbourse;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TsetmcPriceService {

    public interface Callback {

        void onSuccess(
                String symbol,
                double lastPrice,
                double closingPrice,
                double yesterdayPrice,
                double dailyChangePercent
        );

        void onError(
                String symbol,
                String message
        );
    }

    private static final String SEARCH_URL =
            "https://cdn.tsetmc.com/api/Instrument/GetInstrumentSearch/";

    private static final String PRICE_URL =
            "https://cdn.tsetmc.com/api/ClosingPrice/GetClosingPriceInfo/";

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    public void getPrice(
            final String symbol,
            final Callback callback
    ) {

        if (symbol == null ||
                symbol.trim().isEmpty()) {

            postError(
                    callback,
                    symbol,
                    "نماد خالی است"
            );

            return;
        }

        final String cleanSymbol =
                normalizeSymbol(symbol);

        new Thread(() -> {

            try {

                String encodedSymbol =
                        URLEncoder.encode(
                                cleanSymbol,
                                "UTF-8"
                        );

                String searchResponse =
                        request(
                                SEARCH_URL +
                                encodedSymbol
                        );

                String insCode =
                        findInsCode(
                                searchResponse,
                                cleanSymbol
                        );

                if (insCode == null ||
                        insCode.isEmpty()) {

                    throw new Exception(
                            "نماد «" +
                            cleanSymbol +
                            "» در TSETMC پیدا نشد"
                    );
                }

                String priceResponse =
                        request(
                                PRICE_URL +
                                insCode
                        );

                JSONObject root =
                        new JSONObject(
                                priceResponse
                        );

                JSONObject info =
                        root.optJSONObject(
                                "closingPriceInfo"
                        );

                if (info == null) {

                    throw new Exception(
                            "اطلاعات قیمت TSETMC دریافت نشد"
                    );
                }

                double lastPrice =
                        info.optDouble(
                                "pDrCotVal",
                                0
                        );

                double closingPrice =
                        info.optDouble(
                                "pClosing",
                                0
                        );

                double yesterdayPrice =
                        info.optDouble(
                                "priceYesterday",
                                0
                        );

                /*
                 * اگر آخرین معامله معتبر نبود،
                 * قیمت پایانی را استفاده می‌کنیم.
                 */

                if (lastPrice <= 0 &&
                        closingPrice > 0) {

                    lastPrice =
                            closingPrice;
                }

                if (lastPrice <= 0) {

                    throw new Exception(
                            "قیمت معتبر دریافت نشد"
                    );
                }

                /*
                 * درصد تغییر روزانه:
                 *
                 * (آخرین قیمت - قیمت دیروز)
                 * / قیمت دیروز × 100
                 */

                double dailyChangePercent = 0;

                if (yesterdayPrice > 0) {

                    dailyChangePercent =
                            (
                                    (
                                            lastPrice -
                                            yesterdayPrice
                                    )
                                    /
                                    yesterdayPrice
                            )
                            * 100.0;
                }

                final double finalLastPrice =
                        lastPrice;

                final double finalClosingPrice =
                        closingPrice;

                final double finalYesterdayPrice =
                        yesterdayPrice;

                final double finalDailyChangePercent =
                        dailyChangePercent;

                mainHandler.post(() -> {

                    if (callback != null) {

                        callback.onSuccess(
                                symbol.trim(),
                                finalLastPrice,
                                finalClosingPrice,
                                finalYesterdayPrice,
                                finalDailyChangePercent
                        );
                    }

                });

            } catch (Exception e) {

                String message =
                        e.getMessage();

                if (message == null ||
                        message.trim().isEmpty()) {

                    message =
                            e.getClass()
                                    .getSimpleName();
                }

                postError(
                        callback,
                        symbol,
                        "TSETMC: " + message
                );
            }

        }).start();
    }

    private String request(
            String address
    ) throws Exception {

        HttpURLConnection connection =
                null;

        BufferedReader reader =
                null;

        try {

            URL url =
                    new URL(address);

            connection =
                    (HttpURLConnection)
                            url.openConnection();

            connection.setRequestMethod(
                    "GET"
            );

            connection.setConnectTimeout(
                    20000
            );

            connection.setReadTimeout(
                    20000
            );

            connection.setUseCaches(
                    false
            );

            connection.setInstanceFollowRedirects(
                    true
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 16)"
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json,text/plain,*/*"
            );

            connection.setRequestProperty(
                    "Accept-Encoding",
                    "identity"
            );

            connection.setRequestProperty(
                    "Connection",
                    "close"
            );

            int responseCode =
                    connection.getResponseCode();

            InputStream inputStream;

            if (responseCode >= 200 &&
                    responseCode < 300) {

                inputStream =
                        connection.getInputStream();

            } else {

                inputStream =
                        connection.getErrorStream();

                String errorBody = "";

                if (inputStream != null) {

                    reader =
                            new BufferedReader(
                                    new InputStreamReader(
                                            inputStream,
                                            "UTF-8"
                                    )
                            );

                    StringBuilder error =
                            new StringBuilder();

                    String line;

                    while (
                            (line =
                                    reader.readLine())
                                    != null
                    ) {

                        error.append(line);
                    }

                    errorBody =
                            error.toString();

                    if (errorBody.length() > 300) {

                        errorBody =
                                errorBody.substring(
                                        0,
                                        300
                                );
                    }
                }

                throw new Exception(
                        "HTTP " +
                        responseCode +
                        (
                                errorBody.isEmpty()
                                        ? ""
                                        : " | " +
                                          errorBody
                        )
                );
            }

            if (inputStream == null) {

                throw new Exception(
                        "پاسخ سرور خالی است"
                );
            }

            if (reader == null) {

                reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        inputStream,
                                        "UTF-8"
                                )
                        );
            }

            StringBuilder result =
                    new StringBuilder();

            String line;

            while (
                    (line =
                            reader.readLine())
                            != null
            ) {

                result.append(line);
            }

            return result.toString();

        } finally {

            if (reader != null) {

                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }

            if (connection != null) {

                try {
                    connection.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private String findInsCode(
            String response,
            String wantedSymbol
    ) throws Exception {

        JSONObject root =
                new JSONObject(response);

        JSONArray array =
                root.optJSONArray(
                        "instrumentSearch"
                );

        if (array == null) {

            throw new Exception(
                    "instrumentSearch پیدا نشد"
            );
        }

        String wanted =
                normalizeSymbol(
                        wantedSymbol
                );

        for (int i = 0;
             i < array.length();
             i++) {

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

            String normalized =
                    normalizeSymbol(symbol);

            String insCode =
                    item.optString(
                            "insCode",
                            ""
                    );

            int flow =
                    item.optInt(
                            "flow",
                            -1
                    );

            if (wanted.equals(normalized) &&
                    !insCode.isEmpty() &&
                    flow != 3) {

                return insCode;
            }
        }

        for (int i = 0;
             i < array.length();
             i++) {

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

            String normalized =
                    normalizeSymbol(symbol);

            String insCode =
                    item.optString(
                            "insCode",
                            ""
                    );

            if (wanted.equals(normalized) &&
                    !insCode.isEmpty()) {

                return insCode;
            }
        }

        return null;
    }

    private String normalizeSymbol(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replace("ي", "ی")
                .replace("ى", "ی")
                .replace("ك", "ک")
                .replace("ة", "ه")
                .replace("ۀ", "ه")
                .replace("‌", "")
                .replace(" ", "")
                .toUpperCase();
    }

    private void postError(
            Callback callback,
            String symbol,
            String message
    ) {

        mainHandler.post(() -> {

            if (callback != null) {

                callback.onError(
                        symbol,
                        message
                );
            }

        });
    }
}
