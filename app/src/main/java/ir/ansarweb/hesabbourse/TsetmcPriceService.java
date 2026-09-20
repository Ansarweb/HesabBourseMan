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
                double closingPrice
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

                // =====================================================
                // مرحله ۱: پیدا کردن InsCode
                // =====================================================

                String encodedSymbol =
                        URLEncoder.encode(
                                cleanSymbol,
                                "UTF-8"
                        );

                String searchUrl =
                        SEARCH_URL + encodedSymbol;

                String searchResponse =
                        request(searchUrl);

                if (searchResponse == null ||
                        searchResponse.trim().isEmpty()) {

                    throw new Exception(
                            "پاسخ جستجوی TSETMC خالی است"
                    );
                }

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

                // =====================================================
                // مرحله ۲: دریافت قیمت
                // =====================================================

                String priceUrl =
                        PRICE_URL + insCode;

                String priceResponse =
                        request(priceUrl);

                if (priceResponse == null ||
                        priceResponse.trim().isEmpty()) {

                    throw new Exception(
                            "پاسخ قیمت TSETMC خالی است"
                    );
                }

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
                            "بخش closingPriceInfo در پاسخ TSETMC وجود ندارد"
                    );
                }

                // =====================================================
                // آخرین قیمت
                // =====================================================

                double lastPrice =
                        info.optDouble(
                                "pDrCotVal",
                                0
                        );

                // =====================================================
                // قیمت پایانی
                // =====================================================

                double closingPrice =
                        info.optDouble(
                                "pClosing",
                                0
                        );

                // اگر آخرین معامله صفر بود،
                // قیمت پایانی را استفاده کن.

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

                final double finalLastPrice =
                        lastPrice;

                final double finalClosingPrice =
                        closingPrice;

                // =====================================================
                // ارسال نتیجه به UI
                // =====================================================

                mainHandler.post(() -> {

                    if (callback != null) {

                        callback.onSuccess(
                                symbol.trim(),
                                finalLastPrice,
                                finalClosingPrice
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

                final String finalMessage =
                        "TSETMC: " + message;

                postError(
                        callback,
                        symbol,
                        finalMessage
                );
            }

        }).start();
    }

    // =========================================================
    // HTTP REQUEST
    // =========================================================

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

        } catch (Exception e) {

            String message =
                    e.getMessage();

            if (message == null ||
                    message.isEmpty()) {

                message =
                        e.getClass()
                                .getSimpleName();
            }

            throw new Exception(
                    message,
                    e
            );

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

    // =========================================================
    // FIND INSCODE
    // =========================================================

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
                    "instrumentSearch در پاسخ TSETMC پیدا نشد"
            );
        }

        String wanted =
                normalizeSymbol(
                        wantedSymbol
                );

        // =====================================================
        // اول تطبیق دقیق نماد
        // و حذف flow=3
        // =====================================================

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

        // =====================================================
        // اگر پیدا نشد، تطبیق دقیق بدون محدودیت flow
        // =====================================================

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

    // =========================================================
    // NORMALIZE SYMBOL
    // =========================================================

    private String normalizeSymbol(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replace(
                        "ي",
                        "ی"
                )
                .replace(
                        "ى",
                        "ی"
                )
                .replace(
                        "ك",
                        "ک"
                )
                .replace(
                        "ة",
                        "ه"
                )
                .replace(
                        "ۀ",
                        "ه"
                )
                .replace(
                        "‌",
                        ""
                )
                .replace(
                        " ",
                        ""
                )
                .toUpperCase();
    }

    // =========================================================
    // ERROR
    // =========================================================

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
