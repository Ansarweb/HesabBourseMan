package ir.ansarweb.hesabbourse;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME =
            "hesab_bourse.db";

    private static final int DB_VERSION = 6;

    private static final String TABLE =
            "transactions";

    public DatabaseHelper(Context context) {
        super(
                context,
                DATABASE_NAME,
                null,
                DB_VERSION
        );
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE " + TABLE + " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "type TEXT NOT NULL," +
                        "portfolio TEXT DEFAULT 'اصلی'," +
                        "broker TEXT DEFAULT ''," +
                        "symbol TEXT DEFAULT ''," +
                        "quantity REAL DEFAULT 0," +
                        "price REAL DEFAULT 0," +
                        "fee REAL DEFAULT 0," +
                        "amount REAL DEFAULT 0," +
                        "description TEXT DEFAULT ''," +
                        "date INTEGER NOT NULL," +
                        "date_shamsi TEXT DEFAULT ''" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {

        if (oldVersion < 5) {

            try {
                db.execSQL(
                        "ALTER TABLE " + TABLE +
                                " ADD COLUMN description TEXT DEFAULT ''"
                );
            } catch (Exception ignored) {
            }
        }

        if (oldVersion < 6) {

            try {
                db.execSQL(
                        "ALTER TABLE " + TABLE +
                                " ADD COLUMN date_shamsi TEXT DEFAULT ''"
                );
            } catch (Exception ignored) {
            }

            fillOldPersianDates(db);
        }
    }

    /*
     * تبدیل تاریخ میلادی به شمسی
     */
    public static String toPersianDate(long timeMillis) {

        Calendar calendar =
                Calendar.getInstance();

        calendar.setTimeInMillis(timeMillis);

        int gy =
                calendar.get(Calendar.YEAR);

        int gm =
                calendar.get(Calendar.MONTH) + 1;

        int gd =
                calendar.get(Calendar.DAY_OF_MONTH);

        int[] gDaysInMonth = {
                0,
                31, 28, 31, 30, 31, 30,
                31, 31, 30, 31, 30, 31
        };

        int gy2 =
                gy - 1600;

        int gm2 =
                gm - 1;

        int gd2 =
                gd - 1;

        int gDayNo =
                365 * gy2
                        + (gy2 + 3) / 4
                        - (gy2 + 99) / 100
                        + (gy2 + 399) / 400;

        for (int i = 0; i < gm2; i++) {

            gDayNo +=
                    gDaysInMonth[i + 1];
        }

        if (gm2 > 1 &&
                ((gy % 4 == 0 && gy % 100 != 0)
                        || (gy % 400 == 0))) {

            gDayNo++;
        }

        gDayNo += gd2;

        int jDayNo =
                gDayNo - 79;

        int jNp =
                jDayNo / 12053;

        int jDayNo2 =
                jDayNo % 12053;

        int jy =
                979 + 33 * jNp
                        + 4 * (jDayNo2 / 1461);

        jDayNo2 =
                jDayNo2 % 1461;

        if (jDayNo2 >= 366) {

            jy +=
                    (jDayNo2 - 1) / 365;

            jDayNo2 =
                    (jDayNo2 - 1) % 365;
        }

        int jm;
        int jd;

        if (jDayNo2 < 186) {

            jm =
                    1 + jDayNo2 / 31;

            jd =
                    1 + jDayNo2 % 31;

        } else {

            jm =
                    7 + (jDayNo2 - 186) / 30;

            jd =
                    1 + (jDayNo2 - 186) % 30;
        }

        int hour =
                calendar.get(Calendar.HOUR_OF_DAY);

        int minute =
                calendar.get(Calendar.MINUTE);

        int second =
                calendar.get(Calendar.SECOND);

        return String.format(
                Locale.US,
                "%04d/%02d/%02d %02d:%02d:%02d",
                jy,
                jm,
                jd,
                hour,
                minute,
                second
        );
    }

    /*
     * اعداد انگلیسی تاریخ را به فارسی تبدیل می‌کند.
     */
    public static String toPersianDigits(
            String text) {

        if (text == null) {
            return "";
        }

        return text
                .replace("0", "۰")
                .replace("1", "۱")
                .replace("2", "۲")
                .replace("3", "۳")
                .replace("4", "۴")
                .replace("5", "۵")
                .replace("6", "۶")
                .replace("7", "۷")
                .replace("8", "۸")
                .replace("9", "۹");
    }

    /*
     * تاریخ شمسی با اعداد فارسی
     */
    public static String toPersianDateTime(
            long timeMillis) {

        return toPersianDigits(
                toPersianDate(timeMillis)
        );
    }

    /*
     * ثبت عمومی تراکنش
     */
    public long addTransaction(
            String type,
            String portfolio,
            String broker,
            String symbol,
            double quantity,
            double price,
            double fee,
            double amount) {

        return addTransactionWithDescription(
                type,
                portfolio,
                broker,
                symbol,
                quantity,
                price,
                fee,
                amount,
                ""
        );
    }

    /*
     * ثبت تراکنش همراه توضیحات
     *
     * تاریخ و ساعت به‌صورت خودکار ثبت می‌شود.
     */
    public long addTransactionWithDescription(
            String type,
            String portfolio,
            String broker,
            String symbol,
            double quantity,
            double price,
            double fee,
            double amount,
            String description) {

        SQLiteDatabase db =
                getWritableDatabase();

        long now =
                System.currentTimeMillis();

        String shamsiDate =
                toPersianDateTime(now);

        ContentValues values =
                new ContentValues();

        values.put(
                "type",
                type == null ? "" : type
        );

        values.put(
                "portfolio",
                portfolio == null ||
                        portfolio.trim().isEmpty()
                        ? "اصلی"
                        : portfolio.trim()
        );

        values.put(
                "broker",
                broker == null ? "" : broker
        );

        values.put(
                "symbol",
                symbol == null ? "" : symbol
        );

        values.put(
                "quantity",
                quantity
        );

        values.put(
                "price",
                price
        );

        values.put(
                "fee",
                fee
        );

        values.put(
                "amount",
                amount
        );

        values.put(
                "description",
                description == null
                        ? ""
                        : description
        );

        values.put(
                "date",
                now
        );

        values.put(
                "date_shamsi",
                shamsiDate
        );

        return db.insert(
                TABLE,
                null,
                values
        );
    }

    /*
     * دریافت همه تراکنش‌ها
     * جدیدترین عملیات اول نمایش داده می‌شود.
     */
    public Cursor getAllTransactions() {

        SQLiteDatabase db =
                getReadableDatabase();

        return db.query(
                TABLE,
                null,
                null,
                null,
                null,
                null,
                "date DESC, id DESC"
        );
    }

    /*
     * دریافت یک تراکنش
     */
    public Cursor getTransaction(
            long id) {

        SQLiteDatabase db =
                getReadableDatabase();

        return db.query(
                TABLE,
                null,
                "id=?",
                new String[]{
                        String.valueOf(id)
                },
                null,
                null,
                null
        );
    }

    /*
     * جستجوی تراکنش‌ها
     */
    public Cursor searchTransactions(
            String query) {

        SQLiteDatabase db =
                getReadableDatabase();

        String q =
                query == null
                        ? ""
                        : query.trim();

        if (q.isEmpty()) {
            return getAllTransactions();
        }

        String like =
                "%" + q + "%";

        return db.query(
                TABLE,
                null,
                "symbol LIKE ? OR " +
                        "portfolio LIKE ? OR " +
                        "broker LIKE ? OR " +
                        "description LIKE ? OR " +
                        "date_shamsi LIKE ?",
                new String[]{
                        like,
                        like,
                        like,
                        like,
                        like
                },
                null,
                null,
                "date DESC, id DESC"
        );
    }

    /*
     * ویرایش تراکنش
     */
    public int updateTransaction(
            long id,
            String type,
            String portfolio,
            String broker,
            String symbol,
            double quantity,
            double price,
            double fee,
            double amount,
            String description) {

        SQLiteDatabase db =
                getWritableDatabase();

        ContentValues values =
                new ContentValues();

        values.put(
                "type",
                type == null ? "" : type
        );

        values.put(
                "portfolio",
                portfolio == null ||
                        portfolio.trim().isEmpty()
                        ? "اصلی"
                        : portfolio.trim()
        );

        values.put(
                "broker",
                broker == null ? "" : broker
        );

        values.put(
                "symbol",
                symbol == null ? "" : symbol
        );

        values.put(
                "quantity",
                quantity
        );

        values.put(
                "price",
                price
        );

        values.put(
                "fee",
                fee
        );

        values.put(
                "amount",
                amount
        );

        values.put(
                "description",
                description == null
                        ? ""
                        : description
        );

        /*
         * در ویرایش، تاریخ عملیات اصلی
         * دست‌نخورده باقی می‌ماند.
         */
        return db.update(
                TABLE,
                values,
                "id=?",
                new String[]{
                        String.valueOf(id)
                }
        );
    }

    /*
     * حذف تراکنش
     */
    public int deleteTransaction(
            long id) {

        SQLiteDatabase db =
                getWritableDatabase();

        return db.delete(
                TABLE,
                "id=?",
                new String[]{
                        String.valueOf(id)
                }
        );
    }

    /*
     * محاسبه موجودی نقدی یک سبد
     */
    public double getCashBalance(
            String portfolio) {

        double balance = 0;

        Cursor cursor =
                getAllTransactions();

        try {

            while (cursor.moveToNext()) {

                String rowPortfolio =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "portfolio"
                                )
                        );

                if (rowPortfolio == null ||
                        rowPortfolio.trim().isEmpty()) {

                    rowPortfolio = "اصلی";
                }

                if (portfolio == null ||
                        portfolio.trim().isEmpty()) {

                    portfolio = "اصلی";
                }

                if (!rowPortfolio.equals(
                        portfolio)) {

                    continue;
                }

                String type =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "type"
                                )
                        );

                double amount =
                        cursor.getDouble(
                                cursor.getColumnIndexOrThrow(
                                        "amount"
                                )
                        );

                double fee =
                        cursor.getDouble(
                                cursor.getColumnIndexOrThrow(
                                        "fee"
                                )
                        );

                if ("DEPOSIT".equals(type)) {

                    balance += amount;

                } else if ("WITHDRAW".equals(type)) {

                    balance -= amount;

                } else if ("BUY".equals(type)) {

                    balance -=
                            amount + fee;

                } else if ("SELL".equals(type)) {

                    balance +=
                            amount - fee;
                }
            }

        } finally {

            cursor.close();
        }

        return balance;
    }

    /*
     * پر کردن تاریخ شمسی برای تراکنش‌های قدیمی
     */
    private void fillOldPersianDates(
            SQLiteDatabase db) {

        Cursor cursor =
                db.query(
                        TABLE,
                        new String[]{
                                "id",
                                "date",
                                "date_shamsi"
                        },
                        null,
                        null,
                        null,
                        null,
                        null
                );

        try {

            while (cursor.moveToNext()) {

                long id =
                        cursor.getLong(
                                cursor.getColumnIndexOrThrow(
                                        "id"
                                )
                        );

                long date =
                        cursor.getLong(
                                cursor.getColumnIndexOrThrow(
                                        "date"
                                )
                        );

                String current =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "date_shamsi"
                                )
                        );

                if (current != null &&
                        !current.trim().isEmpty()) {

                    continue;
                }

                ContentValues values =
                        new ContentValues();

                values.put(
                        "date_shamsi",
                        toPersianDateTime(date)
                );

                db.update(
                        TABLE,
                        values,
                        "id=?",
                        new String[]{
                                String.valueOf(id)
                        }
                );
            }

        } finally {

            cursor.close();
        }
    }
}
