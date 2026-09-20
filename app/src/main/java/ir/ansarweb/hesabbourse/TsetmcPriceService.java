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

        if (symbol == null || symbol.trim().isEmpty()) {

            if (callback != null) {
                callback.onError(
                        symbol,
                        "نماد وارد نشده است"
                );
            }

            return;
        }

        final String cleanSymbol =
                symbol.trim();

        new Thread(() -> {

            try {

                /*
                 * مرحله اول:
                 * پیدا کردن InsCode نماد در TSETMC
                 */

                String encodedSymbol =
                        URLEncoder.encode(
                                cleanSymbol,
                                "UTF-8"
                        );

                String searchResponse =
                        request(
                                SEARCH_URL + encodedSymbol
                        );

                String insCode =
                        findInsCode(
                                searchResponse,
                                cleanSymbol
                        );

                if (
                        insCode == null ||
                        insCode.isEmpty()
                ) {

                    postError(
                            callback,
                            cleanSymbol,
                            "کد معاملاتی نماد در TSETMC پیدا نشد"
                    );

                    return;
                }

                /*
                 * مرحله دوم:
                 * دریافت قیمت نماد
                 */

                String priceResponse =
                        request(
                                PRICE_URL + insCode
                        );

                JSONObject root =
                        new JSONObject(
                                priceResponse
                        );

                JSONObject closingPriceInfo =
                        root.optJSONObject(
                                "closingPriceInfo"
                        );

                if (closingPriceInfo == null) {

                    postError(
                            callback,
                            cleanSymbol,
                            "اطلاعات قیمت نماد دریافت نشد"
                    );

                    return;
                }

                /*
                 * آخرین معامله
                 */

                double lastPrice =
                        closingPriceInfo.optDouble(
                                "pDrCotVal",
                                0
                        );

                /*
                 * قیمت پایانی
                 */

                double closingPrice =
                        closingPriceInfo.optDouble(
                                "pClosing",
                                0
                        );

                /*
                 * اگر آخرین معامله صفر بود،
                 * از قیمت پایانی استفاده می‌کنیم.
                 */

                if (lastPrice <= 0) {
                    lastPrice = closingPrice;
                }

                if (lastPrice <= 0) {

                    postError(
                            callback,
                            cleanSymbol,
                            "قیمت معتبر برای نماد دریافت نشد"
                    );

                    return;
                }

                final double finalLastPrice =
                        lastPrice;

                final double finalClosingPrice =
                        closingPrice;

                /*
                 * برگشت نتیجه به Thread اصلی اندروید
                 */

                mainHandler.post(() -> {

                    if (callback != null) {

                        callback.onSuccess(
                                cleanSymbol,
                                finalLastPrice,
                                finalClosingPrice
                        );
                    }
                });

            } catch (Exception e) {

                String message =
                        e.getMessage();

                if (
                        message == null ||
                        message.isEmpty()
                ) {

                    message =
                            "خطا در دریافت اطلاعات بازار";
                }

                postError(
                        callback,
                        cleanSymbol,
                        message
                );
            }

        }).start();
    }

    /*
     * ارسال درخواست HTTP به TSETMC
     */

    private String request(
            String address
    ) throws Exception {

        HttpURLConnection connection = null;

        BufferedReader reader = null;

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
                    15000
            );

            connection.setReadTimeout(
                    15000
            );

            connection.setUseCaches(
                    false
            );

            connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0"
            );

            connection.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            connection.setRequestProperty(
                    "Connection",
                    "close"
            );

            int responseCode =
                    connection.getResponseCode();

            InputStream inputStream;

            if (
                    responseCode >= 200 &&
                    responseCode < 300
            ) {

                inputStream =
                        connection.getInputStream();

            } else {

                inputStream =
                        connection.getErrorStream();
            }

            if (inputStream == null) {

                throw new Exception(
                        "پاسخی از سرور TSETMC دریافت نشد"
                );
            }

            reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    inputStream,
                                    "UTF-8"
                            )
                    );

            StringBuilder result =
                    new StringBuilder();

            String line;

            while (
                    (line = reader.readLine())
                            != null
            ) {

                result.append(line);
            }

            if (
                    responseCode < 200 ||
                    responseCode >= 300
            ) {

                throw new Exception(
                        "خطای TSETMC: HTTP " +
                                responseCode
                );
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
                connection.disconnect();
            }
        }
    }

    /*
     * پیدا کردن InsCode نماد
     */

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
            return null;
        }

        String wanted =
                normalizeSymbol(
                        wantedSymbol
                );

        /*
         * اول نماد دقیق بازار بورس/فرابورس
         */

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

            int flow =
                    item.optInt(
                            "flow",
                            -1
                    );

            String insCode =
                    item.optString(
                            "insCode",
                            ""
                    );

            if (
                    wanted.equals(normalized) &&
                    !insCode.isEmpty() &&
                    flow != 3
            ) {

                return insCode;
            }
        }

        /*
         * اگر مورد بالا پیدا نشد،
         * تطبیق دقیق بدون محدودیت بازار
         */

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

            if (
                    wanted.equals(normalized)
            ) {

                String insCode =
                        item.optString(
                                "insCode",
                                ""
                        );

                if (
                        !insCode.isEmpty()
                ) {

                    return insCode;
                }
            }
        }

        return null;
    }

    /*
     * یکسان‌سازی حروف فارسی و عربی
     */

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

    /*
     * ارسال خطا به Thread اصلی
     */

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
