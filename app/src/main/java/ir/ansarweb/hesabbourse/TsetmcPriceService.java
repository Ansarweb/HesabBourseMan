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

    private static final String PRICES_URL =
            "https://raw.githubusercontent.com/"
            + "Ansarweb/HesabBourseMan/main/prices.json";

    private final Handler mainHandler =
            new Handler(
                    Looper.getMainLooper()
            );

    public void getPrice(
            final String symbol,
            final Callback callback
    ) {

        if (symbol == null ||
                symbol.trim().isEmpty()) {

            callback.onError(
                    symbol,
                    "نماد وارد نشده است"
            );

            return;
        }

        final String cleanSymbol =
                symbol.trim();

        new Thread(() -> {

            try {

                String response =
                        request(PRICES_URL);

                JSONObject root =
                        new JSONObject(response);

                JSONObject prices =
                        root.optJSONObject(
                                "prices"
                        );

                if (prices == null) {

                    postError(
                            callback,
                            cleanSymbol,
                            "اطلاعات قیمت موجود نیست"
                    );

                    return;
                }

                JSONObject item =
                        prices.optJSONObject(
                                cleanSymbol
                        );

                if (item == null) {

                    postError(
                            callback,
                            cleanSymbol,
                            "قیمت نماد در منبع پیدا نشد"
                    );

                    return;
                }

                double lastPrice =
                        item.optDouble(
                                "lastPrice",
                                0
                        );

                double closingPrice =
                        item.optDouble(
                                "closingPrice",
                                0
                        );

                if (lastPrice <= 0) {

                    postError(
                            callback,
                            cleanSymbol,
                            "قیمت معتبر دریافت نشد"
                    );

                    return;
                }

                final double finalLastPrice =
                        lastPrice;

                final double finalClosingPrice =
                        closingPrice;

                mainHandler.post(
                        () ->
                                callback.onSuccess(
                                        cleanSymbol,
                                        finalLastPrice,
                                        finalClosingPrice
                                )
                );

            } catch (Exception e) {

                String message =
                        e.getMessage();

                if (message == null ||
                        message.isEmpty()) {

                    message =
                            "خطا در دریافت قیمت آنلاین";
                }

                postError(
                        callback,
                        cleanSymbol,
                        message
                );
            }

        }).start();
    }

    private String request(
            String address
    ) throws Exception {

        URL url =
                new URL(address);

        HttpURLConnection connection =
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

        if (responseCode >= 200 &&
                responseCode < 300) {

            inputStream =
                    connection.getInputStream();

        } else {

            inputStream =
                    connection.getErrorStream();
        }

        if (inputStream == null) {

            connection.disconnect();

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

        while (
                (line = reader.readLine())
                        != null
        ) {

            result.append(line);
        }

        reader.close();

        connection.disconnect();

        if (responseCode < 200 ||
                responseCode >= 300) {

            throw new Exception(
                    "خطای سرور: " +
                    responseCode
            );
        }

        return result.toString();
    }

    private void postError(
            Callback callback,
            String symbol,
            String message
    ) {

        mainHandler.post(
                () ->
                        callback.onError(
                                symbol,
                                message
                        )
        );
    }
}
