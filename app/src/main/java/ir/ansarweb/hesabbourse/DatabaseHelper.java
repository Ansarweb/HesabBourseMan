package ir.ansarweb.hesabbourse;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME =
            "hesab_bourse.db";

    private static final int DB_VERSION = 7;

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

                        // نوع عملیات:
                        // BUY / SELL / DEPOSIT / WITHDRAW
                        "type TEXT NOT NULL," +

                        // نوع دارایی:
                        // STOCK / OPTION
                        "asset_type TEXT DEFAULT 'STOCK'," +

                        "portfolio TEXT DEFAULT 'اصلی'," +

                        "broker TEXT DEFAULT ''," +

                        "symbol TEXT DEFAULT ''," +

                        "quantity REAL DEFAULT 0," +

                        // برای سهام: قیمت سهم
                        // برای آپشن: پرمیوم
                        "price REAL DEFAULT 0," +

                        "fee REAL DEFAULT 0," +

                        "amount REAL DEFAULT 0," +

                        "description TEXT DEFAULT ''," +

                        // اطلاعات مخصوص آپشن
                        "underlying TEXT DEFAULT ''," +

                        // CALL / PUT
                        "option_type TEXT DEFAULT ''," +

                        // قیمت اعمال
                        "strike_price REAL DEFAULT 0," +

                        // تاریخ سررسید
                        "expiry_date TEXT DEFAULT ''," +

                        // اندازه قرارداد
                        "contract_size REAL DEFAULT 0," +

                        // LONG / SHORT
                        "position_type TEXT DEFAULT ''," +

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

        /*
         * نسخه 7:
         * اضافه شدن زیرساخت معاملات آپشن
         */
        if (oldVersion < 7) {

            addColumnIfMissing(
                    db,
                    "asset_type",
                    "TEXT DEFAULT 'STOCK'"
            );

            addColumnIfMissing(
                    db,
                    "underlying",
                    "TEXT DEFAULT ''"
            );

            addColumnIfMissing(
                    db,
                    "option_type",
                    "TEXT DEFAULT ''"
            );

            addColumnIfMissing(
                    db,
                    "strike_price",
                    "REAL DEFAULT 0"
            );

            addColumnIfMissing(
                    db,
                    "expiry_date",
                    "TEXT DEFAULT ''"
            );

            addColumnIfMissing(
                    db,
                    "contract_size",
                    "REAL DEFAULT 0"
            );

            addColumnIfMissing(
                    db,
                    "position_type",
                    "TEXT DEFAULT ''"
            );
        }
    }

    /*
     * اضافه کردن ستون فقط در صورتی که قبلاً وجود نداشته باشد.
     */
    private void addColumnIfMissing(
            SQLiteDatabase db,
            String column,
            String definition) {

        try {

            db.execSQL(
                    "ALTER TABLE " + TABLE +
                            " ADD COLUMN " +
                            column +
                            " " +
                            definition
            );

        } catch (Exception ignored) {
            // ستون از قبل وجود داشته است.
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

    // =========================================================
    // تراکنش معمولی سهام
    // =========================================================

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

        ContentValues values =
                new ContentValues();

        values.put(
                "type",
                type == null ? "" : type
        );

        values.put(
                "asset_type",
                "STOCK"
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
                toPersianDateTime(now)
        );

        return db.insert(
                TABLE,
                null,
                values
        );
    }

    // =========================================================
    // ثبت معامله آپشن
    // =========================================================

    /*
     * فرمول کارمزد آپشن:
     *
     * Premium × Contract Size × 0.00103
     *
     * تعداد قرارداد در quantity ذخیره می‌شود.
     */
    public long addOptionTransaction(
            String type,
            String portfolio,
            String broker,
            String symbol,
            String underlying,
            String optionType,
            double strikePrice,
            String expiryDate,
            double quantity,
            double premium,
            double contractSize,
            String positionType,
            String description) {

        SQLiteDatabase db =
                getWritableDatabase();

        long now =
                System.currentTimeMillis();

        /*
         * مبلغ کل پریمیوم
         */
        double amount =
                premium
                        * quantity
                        * contractSize;

        /*
         * کارمزد آپشن
         *
         * طبق فرمول تعیین‌شده:
         *
         * Premium × Contract Size × 0.00103
         *
         * برای هر قرارداد
         */
        double feePerContract =
                premium
                        * contractSize
                        * 0.00103;

        double fee =
                feePerContract
                        * quantity;

        ContentValues values =
                new ContentValues();

        values.put(
                "type",
                type == null ? "" : type
        );

        values.put(
                "asset_type",
                "OPTION"
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

        /*
         * در آپشن price همان Premium است.
         */
        values.put(
                "price",
                premium
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
                "underlying",
                underlying == null
                        ? ""
                        : underlying
        );

        values.put(
                "option_type",
                optionType == null
                        ? ""
                        : optionType
        );

        values.put(
                "strike_price",
                strikePrice
        );

        values.put(
                "expiry_date",
                expiryDate == null
                        ? ""
                        : expiryDate
        );

        values.put(
                "contract_size",
                contractSize
        );

        values.put(
                "position_type",
                positionType == null
                        ? ""
                        : positionType
        );

        values.put(
                "date",
                now
        );

        values.put(
                "date_shamsi",
                toPersianDateTime(now)
        );

        return db.insert(
                TABLE,
                null,
                values
        );
    }

    // =========================================================
    // دریافت همه تراکنش‌ها
    // =========================================================

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

    // =========================================================
    // دریافت یک تراکنش
    // =========================================================

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

    // =========================================================
    // جستجو
    // =========================================================

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
                        "underlying LIKE ? OR " +
                        "date_shamsi LIKE ?",
                new String[]{
                        like,
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

    // =========================================================
    // ویرایش تراکنش سهام
    // =========================================================

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
                "asset_type",
                "STOCK"
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

        return db.update(
                TABLE,
                values,
                "id=?",
                new String[]{
                        String.valueOf(id)
                }
        );
    }

    // =========================================================
    // ویرایش آپشن
    // =========================================================

    public int updateOptionTransaction(
            long id,
            String type,
            String portfolio,
            String broker,
            String symbol,
            String underlying,
            String optionType,
            double strikePrice,
            String expiryDate,
            double quantity,
            double premium,
            double contractSize,
            String positionType,
            String description) {

        SQLiteDatabase db =
                getWritableDatabase();

        double amount =
                premium
                        * quantity
                        * contractSize;

        double fee =
                premium
                        * contractSize
                        * 0.00103
                        * quantity;

        ContentValues values =
                new ContentValues();

        values.put(
                "type",
                type == null ? "" : type
        );

        values.put(
                "asset_type",
                "OPTION"
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
                premium
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
                "underlying",
                underlying == null
                        ? ""
                        : underlying
        );

        values.put(
                "option_type",
                optionType == null
                        ? ""
                        : optionType
        );

        values.put(
                "strike_price",
                strikePrice
        );

        values.put(
                "expiry_date",
                expiryDate == null
                        ? ""
                        : expiryDate
        );

        values.put(
                "contract_size",
                contractSize
        );

        values.put(
                "position_type",
                positionType == null
                        ? ""
                        : positionType
        );

        values.put(
                "description",
                description == null
                        ? ""
                        : description
        );

        return db.update(
                TABLE,
                values,
                "id=?",
                new String[]{
                        String.valueOf(id)
                }
        );
    }

    // =========================================================
    // حذف
    // =========================================================

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

    // =========================================================
    // موجودی نقدی
    // =========================================================

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

                String selectedPortfolio =
                        portfolio == null ||
                                portfolio.trim().isEmpty()
                                ? "اصلی"
                                : portfolio;

                if (!rowPortfolio.equals(
                        selectedPortfolio)) {

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

                String assetType =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "asset_type"
                                )
                        );

                /*
                 * واریز و برداشت
                 */
                if ("DEPOSIT".equals(type)) {

                    balance += amount;

                } else if ("WITHDRAW".equals(type)) {

                    balance -= amount;

                /*
                 * سهام و آپشن
                 *
                 * BUY:
                 * پول + کارمزد از حساب خارج می‌شود.
                 *
                 * SELL:
                 * پول خالص وارد حساب می‌شود.
                 *
                 * برای Short Option نیز SELL است،
                 * بنابراین پریمیوم خالص وارد حساب می‌شود.
                 */
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

    // =========================================================
    // تاریخ‌های قدیمی
    // =========================================================

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

    // =========================================================
    // BACKUP
    // =========================================================

    public String exportBackupJson()
            throws Exception {

        JSONObject backup =
                new JSONObject();

        backup.put(
                "app",
                "حساب بورس"
        );

        backup.put(
                "backup_version",
                2
        );

        backup.put(
                "created_at",
                System.currentTimeMillis()
        );

        JSONArray transactions =
                new JSONArray();

        Cursor cursor =
                getAllTransactions();

        try {

            while (cursor.moveToNext()) {

                JSONObject item =
                        new JSONObject();

                item.put(
                        "id",
                        cursor.getLong(
                                cursor.getColumnIndexOrThrow(
                                        "id"
                                )
                        )
                );

                item.put(
                        "type",
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "type"
                                )
                        )
                );

                item.put(
                        "asset_type",
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "asset_type"
                                )
                        )
                );

                item.put(
                        "portfolio",
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "portfolio"
                                )
                        )
                );

                item.put(
                        "broker",
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "broker"
                                )
                        )
                );

                item.put(
                        "symbol",
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "symbol"
                                )
                        )
                );

                item.put(
                        "quantity",
                        cursor.getDouble(
                                cursor.getColumnIndexOrThrow(
                                        "quantity"
                                )
                        )
                );

                item.put(
                        "price",
                        cursor.getDouble(
                                cursor.getColumnIndexOrThrow(
                                        "price"
                                )
                        )
                );

                item.put(
                        "fee",
                        cursor.getDouble(
                                cursor.getColumnIndexOrThrow(
                                        "fee"
                                )
                        )
                );

                item.put(
                        "amount",
                        cursor.getDouble(
                                cursor.getColumnIndexOrThrow(
                                        "amount"
                                )
                        )
                );

                item.put(
                        "description",
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "description"
                                )
                        )
                );

                item.put(
                        "underlying",
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "underlying"
                                )
                        )
                );

                item.put(
                        "option_type",
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "option_type"
                                )
                        )
                );

                item.put(
                        "strike_price",
                        cursor.getDouble(
                                cursor.getColumnIndexOrThrow(
                                        "strike_price"
                                )
                        )
                );

                item.put(
                        "expiry_date",
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "expiry_date"
                                )
                        )
                );

                item.put(
                        "contract_size",
                        cursor.getDouble(
                                cursor.getColumnIndexOrThrow(
                                        "contract_size"
                                )
                        )
                );

                item.put(
                        "position_type",
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "position_type"
                                )
                        )
                );

                item.put(
                        "date",
                        cursor.getLong(
                                cursor.getColumnIndexOrThrow(
                                        "date"
                                )
                        )
                );

                item.put(
                        "date_shamsi",
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "date_shamsi"
                                )
                        )
                );

                transactions.put(item);
            }

        } finally {

            cursor.close();
        }

        backup.put(
                "transactions",
                transactions
        );

        return backup.toString(2);
    }

    // =========================================================
    // RESTORE
    // =========================================================

    public void importBackupJson(
            String json) throws Exception {

        if (json == null ||
                json.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "فایل بک‌آپ خالی است."
            );
        }

        JSONObject backup =
                new JSONObject(json);

        String app =
                backup.optString(
                        "app",
                        ""
                );

        if (!"حساب بورس".equals(app)) {

            throw new IllegalArgumentException(
                    "این فایل، بک‌آپ برنامه حساب بورس نیست."
            );
        }

        int backupVersion =
                backup.optInt(
                        "backup_version",
                        0
                );

        /*
         * نسخه 1 قدیمی و نسخه 2 جدید
         * هر دو قابل بازیابی هستند.
         */
        if (backupVersion != 1 &&
                backupVersion != 2) {

            throw new IllegalArgumentException(
                    "نسخه بک‌آپ پشتیبانی نمی‌شود."
            );
        }

        JSONArray transactions =
                backup.optJSONArray(
                        "transactions"
                );

        if (transactions == null) {

            throw new IllegalArgumentException(
                    "اطلاعات تراکنش‌ها در بک‌آپ پیدا نشد."
            );
        }

        SQLiteDatabase db =
                getWritableDatabase();

        db.beginTransaction();

        try {

            db.delete(
                    TABLE,
                    null,
                    null
            );

            for (int i = 0;
                    i < transactions.length();
                    i++) {

                JSONObject item =
                        transactions.getJSONObject(i);

                long id =
                        item.optLong(
                                "id",
                                0
                        );

                String type =
                        item.optString(
                                "type",
                                ""
                        );

                if (id <= 0) {

                    throw new IllegalArgumentException(
                            "شناسه تراکنش در بک‌آپ نامعتبر است."
                    );
                }

                if (type.trim().isEmpty()) {

                    throw new IllegalArgumentException(
                            "نوع یکی از تراکنش‌های بک‌آپ نامعتبر است."
                    );
                }

                ContentValues values =
                        new ContentValues();

                values.put(
                        "id",
                        id
                );

                values.put(
                        "type",
                        type
                );

                /*
                 * بک‌آپ قدیمی فاقد asset_type است.
                 * بنابراین آن را سهام در نظر می‌گیریم.
                 */
                values.put(
                        "asset_type",
                        item.optString(
                                "asset_type",
                                "STOCK"
                        )
                );

                values.put(
                        "portfolio",
                        item.optString(
                                "portfolio",
                                "اصلی"
                        )
                );

                values.put(
                        "broker",
                        item.optString(
                                "broker",
                                ""
                        )
                );

                values.put(
                        "symbol",
                        item.optString(
                                "symbol",
                                ""
                        )
                );

                values.put(
                        "quantity",
                        item.optDouble(
                                "quantity",
                                0
                        )
                );

                values.put(
                        "price",
                        item.optDouble(
                                "price",
                                0
                        )
                );

                values.put(
                        "fee",
                        item.optDouble(
                                "fee",
                                0
                        )
                );

                values.put(
                        "amount",
                        item.optDouble(
                                "amount",
                                0
                        )
                );

                values.put(
                        "description",
                        item.optString(
                                "description",
                                ""
                        )
                );

                values.put(
                        "underlying",
                        item.optString(
                                "underlying",
                                ""
                        )
                );

                values.put(
                        "option_type",
                        item.optString(
                                "option_type",
                                ""
                        )
                );

                values.put(
                        "strike_price",
                        item.optDouble(
                                "strike_price",
                                0
                        )
                );

                values.put(
                        "expiry_date",
                        item.optString(
                                "expiry_date",
                                ""
                        )
                );

                values.put(
                        "contract_size",
                        item.optDouble(
                                "contract_size",
                                0
                        )
                );

                values.put(
                        "position_type",
                        item.optString(
                                "position_type",
                                ""
                        )
                );

                values.put(
                        "date",
                        item.optLong(
                                "date",
                                System.currentTimeMillis()
                        )
                );

                values.put(
                        "date_shamsi",
                        item.optString(
                                "date_shamsi",
                                ""
                        )
                );

                db.insertOrThrow(
                        TABLE,
                        null,
                        values
                );
            }

            db.setTransactionSuccessful();

        } finally {

            db.endTransaction();
        }
    }
}
