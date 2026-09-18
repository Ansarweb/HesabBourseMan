package ir.ansarweb.hesabbourse;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "hesab_bourse.db";
    private static final int DB_VERSION = 4;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE transactions (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "type TEXT NOT NULL," +
                        "portfolio TEXT DEFAULT 'اصلی'," +
                        "broker TEXT DEFAULT ''," +
                        "symbol TEXT DEFAULT ''," +
                        "quantity REAL DEFAULT 0," +
                        "price REAL DEFAULT 0," +
                        "fee REAL DEFAULT 0," +
                        "amount REAL DEFAULT 0," +
                        "date INTEGER NOT NULL)"
        );
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {

        if (oldVersion < 2) {
            addColumnIfMissing(
                    db,
                    "portfolio",
                    "TEXT DEFAULT 'اصلی'"
            );
        }

        if (oldVersion < 3) {
            // نسخه 3 ساختار جدیدی به جدول اضافه نمی‌کند.
        }

        if (oldVersion < 4) {
            // نسخه 4 نیز از همان ساختار تراکنش استفاده می‌کند.
            // امکانات جدید در لایه برنامه اضافه می‌شوند.
        }
    }

    private void addColumnIfMissing(
            SQLiteDatabase db,
            String column,
            String definition) {

        try {
            db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN "
                            + column + " " + definition
            );
        } catch (Exception ignored) {
            // ستون از قبل وجود دارد.
        }
    }

    public long addTransaction(
            String type,
            String portfolio,
            String broker,
            String symbol,
            double quantity,
            double price,
            double fee,
            double amount) {

        ContentValues values =
                new ContentValues();

        values.put("type", type);

        values.put(
                "portfolio",
                normalizePortfolio(portfolio)
        );

        values.put(
                "broker",
                broker == null
                        ? ""
                        : broker.trim()
        );

        values.put(
                "symbol",
                symbol == null
                        ? ""
                        : symbol.trim()
        );

        values.put("quantity", quantity);
        values.put("price", price);
        values.put("fee", fee);
        values.put("amount", amount);

        values.put(
                "date",
                System.currentTimeMillis()
        );

        return getWritableDatabase().insert(
                "transactions",
                null,
                values
        );
    }

    public long addTransactionWithDate(
            String type,
            String portfolio,
            String broker,
            String symbol,
            double quantity,
            double price,
            double fee,
            double amount,
            long date) {

        ContentValues values =
                new ContentValues();

        values.put("type", type);
        values.put(
                "portfolio",
                normalizePortfolio(portfolio)
        );

        values.put(
                "broker",
                broker == null
                        ? ""
                        : broker.trim()
        );

        values.put(
                "symbol",
                symbol == null
                        ? ""
                        : symbol.trim()
        );

        values.put("quantity", quantity);
        values.put("price", price);
        values.put("fee", fee);
        values.put("amount", amount);
        values.put("date", date);

        return getWritableDatabase().insert(
                "transactions",
                null,
                values
        );
    }

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
            long date) {

        ContentValues values =
                new ContentValues();

        values.put("type", type);
        values.put(
                "portfolio",
                normalizePortfolio(portfolio)
        );

        values.put(
                "broker",
                broker == null
                        ? ""
                        : broker.trim()
        );

        values.put(
                "symbol",
                symbol == null
                        ? ""
                        : symbol.trim()
        );

        values.put("quantity", quantity);
        values.put("price", price);
        values.put("fee", fee);
        values.put("amount", amount);
        values.put("date", date);

        return getWritableDatabase().update(
                "transactions",
                values,
                "id = ?",
                new String[]{
                        String.valueOf(id)
                }
        );
    }

    public int deleteTransaction(long id) {

        return getWritableDatabase().delete(
                "transactions",
                "id = ?",
                new String[]{
                        String.valueOf(id)
                }
        );
    }

    public Cursor getTransaction(long id) {

        return getReadableDatabase().query(
                "transactions",
                null,
                "id = ?",
                new String[]{
                        String.valueOf(id)
                },
                null,
                null,
                null
        );
    }

    public Cursor getAllTransactions() {

        return getReadableDatabase()
                .rawQuery(
                        "SELECT * FROM transactions " +
                                "ORDER BY date DESC, id DESC",
                        null
                );
    }

    public Cursor getTransactionsByPortfolio(
            String portfolio) {

        return getReadableDatabase()
                .rawQuery(
                        "SELECT * FROM transactions " +
                                "WHERE portfolio = ? " +
                                "ORDER BY date DESC, id DESC",
                        new String[]{
                                normalizePortfolio(portfolio)
                        }
                );
    }

    public Cursor searchTransactions(
            String query) {

        String q =
                query == null
                        ? ""
                        : query.trim();

        return getReadableDatabase()
                .rawQuery(
                        "SELECT * FROM transactions " +
                                "WHERE symbol LIKE ? " +
                                "OR portfolio LIKE ? " +
                                "OR broker LIKE ? " +
                                "OR type LIKE ? " +
                                "ORDER BY date DESC, id DESC",
                        new String[]{
                                "%" + q + "%",
                                "%" + q + "%",
                                "%" + q + "%",
                                "%" + q + "%"
                        }
                );
    }

    public double getCashBalance(
            String portfolio) {

        Cursor cursor =
                getReadableDatabase()
                        .rawQuery(
                                "SELECT " +
                                        "COALESCE(SUM(" +
                                        "CASE " +
                                        "WHEN type = 'DEPOSIT' " +
                                        "THEN amount " +
                                        "WHEN type = 'WITHDRAW' " +
                                        "THEN -amount " +
                                        "WHEN type = 'BUY' " +
                                        "THEN -(amount + fee) " +
                                        "WHEN type = 'SELL' " +
                                        "THEN (amount - fee) " +
                                        "ELSE 0 END" +
                                        "), 0) " +
                                        "FROM transactions " +
                                        "WHERE portfolio = ?",
                                new String[]{
                                        normalizePortfolio(portfolio)
                                }
                        );

        try {
            if (cursor.moveToFirst()) {
                return cursor.getDouble(0);
            }

            return 0;

        } finally {
            cursor.close();
        }
    }

    private String normalizePortfolio(
            String portfolio) {

        if (portfolio == null
                || portfolio.trim().isEmpty()) {

            return "اصلی";
        }

        return portfolio.trim();
    }

    public SQLiteDatabase getDatabase() {
        return getWritableDatabase();
    }
}
