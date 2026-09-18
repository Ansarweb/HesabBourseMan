
package ir.ansarweb.hesabbourse;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "hesab_bourse.db";
    private static final int DB_VERSION = 2;

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
            "broker TEXT," +
            "symbol TEXT," +
            "quantity REAL DEFAULT 0," +
            "price REAL DEFAULT 0," +
            "fee REAL DEFAULT 0," +
            "amount REAL DEFAULT 0," +
            "date INTEGER NOT NULL)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(
                "ALTER TABLE transactions ADD COLUMN portfolio TEXT DEFAULT 'اصلی'"
            );
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

        ContentValues values = new ContentValues();

        values.put("type", type);
        values.put(
                "portfolio",
                portfolio == null || portfolio.trim().isEmpty()
                        ? "اصلی"
                        : portfolio.trim()
        );
        values.put("broker", broker);
        values.put("symbol", symbol);
        values.put("quantity", quantity);
        values.put("price", price);
        values.put("fee", fee);
        values.put("amount", amount);
        values.put("date", System.currentTimeMillis());

        return getWritableDatabase()
                .insert("transactions", null, values);
    }

    public Cursor getAllTransactions() {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM transactions ORDER BY date DESC",
                null
        );
    }
}
