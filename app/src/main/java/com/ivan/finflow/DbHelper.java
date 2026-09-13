package com.ivan.finflow;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class DbHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "finflow.db";
    public static final int DB_VERSION = 2;

    private static final String[] DEFAULT_EXPENSE_CATEGORIES = {
        "Еда", "Транспорт", "Покупки", "Дом", "Здоровье", "Развлечения", "Образование", "Связь", "Путешествия", "Другое"
    };
    private static final String[] DEFAULT_INCOME_CATEGORIES = {
        "Зарплата", "Подработка", "Подарок", "Возврат", "Продажа", "Инвестиции", "Другое"
    };

    public static class Tx {
        long id;
        String type;
        double amount;
        String category;
        String account;
        String note;
        long createdAt;

        Tx(long id, String type, double amount, String category, String account, String note, long createdAt) {
            this.id = id;
            this.type = type;
            this.amount = amount;
            this.category = category;
            this.account = account;
            this.note = note;
            this.createdAt = createdAt;
        }
    }

    public static class AccountBalance {
        String name;
        double balance;

        AccountBalance(String name, double balance) {
            this.name = name;
            this.balance = balance;
        }
    }

    public static class LabelTotal {
        String label;
        double total;

        LabelTotal(String label, double total) {
            this.label = label;
            this.total = total;
        }
    }

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE accounts (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, opening_balance REAL NOT NULL DEFAULT 0)");
        db.execSQL("CREATE TABLE tx (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, amount REAL NOT NULL, category TEXT NOT NULL, account TEXT NOT NULL, note TEXT, created_at INTEGER NOT NULL)");
        createCategoriesTable(db);

        ContentValues cv = new ContentValues();
        cv.put("name", "Основной счёт");
        cv.put("opening_balance", 0.0);
        db.insert("accounts", null, cv);
        seedDefaultCategories(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            createCategoriesTable(db);
            seedDefaultCategories(db);
            db.execSQL("INSERT OR IGNORE INTO categories(type, name, sort_order) SELECT type, category, 999 FROM tx GROUP BY type, category");
        }
    }

    private void createCategoriesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, name TEXT NOT NULL, sort_order INTEGER NOT NULL DEFAULT 0, UNIQUE(type, name))");
    }

    private void seedDefaultCategories(SQLiteDatabase db) {
        int order = 0;
        for (String name : DEFAULT_EXPENSE_CATEGORIES) insertCategory(db, "expense", name, order++);
        order = 0;
        for (String name : DEFAULT_INCOME_CATEGORIES) insertCategory(db, "income", name, order++);
    }

    private void insertCategory(SQLiteDatabase db, String type, String name, int sortOrder) {
        ContentValues cv = new ContentValues();
        cv.put("type", type);
        cv.put("name", name);
        cv.put("sort_order", sortOrder);
        db.insertWithOnConflict("categories", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public long addTransaction(String type, double amount, String category, String account, String note, long createdAt) {
        ContentValues cv = new ContentValues();
        cv.put("type", type);
        cv.put("amount", amount);
        cv.put("category", category);
        cv.put("account", account);
        cv.put("note", note);
        cv.put("created_at", createdAt);
        return getWritableDatabase().insert("tx", null, cv);
    }

    public boolean updateTransaction(long id, String type, double amount, String category, String account, String note, long createdAt) {
        ContentValues cv = new ContentValues();
        cv.put("type", type);
        cv.put("amount", amount);
        cv.put("category", category);
        cv.put("account", account);
        cv.put("note", note);
        cv.put("created_at", createdAt);
        return getWritableDatabase().update("tx", cv, "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    public void deleteTransaction(long id) {
        getWritableDatabase().delete("tx", "id=?", new String[]{String.valueOf(id)});
    }

    public List<Tx> getTransactions(int limit) {
        List<Tx> out = new ArrayList<>();
        Cursor c = getReadableDatabase().query("tx", null, null, null, null, null, "created_at DESC, id DESC", limit > 0 ? String.valueOf(limit) : null);
        while (c.moveToNext()) {
            out.add(new Tx(
                c.getLong(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("type")),
                c.getDouble(c.getColumnIndexOrThrow("amount")),
                c.getString(c.getColumnIndexOrThrow("category")),
                c.getString(c.getColumnIndexOrThrow("account")),
                c.getString(c.getColumnIndexOrThrow("note")),
                c.getLong(c.getColumnIndexOrThrow("created_at"))
            ));
        }
        c.close();
        return out;
    }

    public List<String> getCategories(String type) {
        List<String> out = new ArrayList<>();
        Cursor c = getReadableDatabase().query(
            "categories",
            new String[]{"name"},
            "type=?",
            new String[]{type},
            null,
            null,
            "sort_order ASC, id ASC"
        );
        while (c.moveToNext()) out.add(c.getString(0));
        c.close();
        return out;
    }

    public int getCategoryCount(String type) {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM categories WHERE type=?", new String[]{type});
        int count = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        return count;
    }

    public int getCategoryUsageCount(String type, String name) {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM tx WHERE type=? AND category=?", new String[]{type, name});
        int count = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        return count;
    }

    public boolean addCategory(String type, String name) {
        String cleaned = name == null ? "" : name.trim();
        if (cleaned.isEmpty()) return false;
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.rawQuery("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM categories WHERE type=?", new String[]{type});
        int next = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        ContentValues cv = new ContentValues();
        cv.put("type", type);
        cv.put("name", cleaned);
        cv.put("sort_order", next);
        return db.insert("categories", null, cv) != -1;
    }

    public boolean renameCategory(String type, String oldName, String newName) {
        String cleaned = newName == null ? "" : newName.trim();
        if (cleaned.isEmpty()) return false;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put("name", cleaned);
            int changed = db.update("categories", cv, "type=? AND name=?", new String[]{type, oldName});
            if (changed <= 0) return false;

            ContentValues tx = new ContentValues();
            tx.put("category", cleaned);
            db.update("tx", tx, "type=? AND category=?", new String[]{type, oldName});
            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            db.endTransaction();
        }
    }

    public boolean deleteCategory(String type, String name) {
        if (getCategoryCount(type) <= 1) return false;
        return getWritableDatabase().delete("categories", "type=? AND name=?", new String[]{type, name}) > 0;
    }

    public List<String> getAccounts() {
        List<String> out = new ArrayList<>();
        Cursor c = getReadableDatabase().query("accounts", new String[]{"name"}, null, null, null, null, "id ASC");
        while (c.moveToNext()) out.add(c.getString(0));
        c.close();
        return out;
    }

    public boolean addAccount(String name, double openingBalance) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("opening_balance", openingBalance);
        return getWritableDatabase().insert("accounts", null, cv) != -1;
    }

    public double getTotalBalance() {
        double total = 0;
        Cursor a = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(opening_balance),0) FROM accounts", null);
        if (a.moveToFirst()) total += a.getDouble(0);
        a.close();
        Cursor t = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(CASE WHEN type='income' THEN amount ELSE -amount END),0) FROM tx", null);
        if (t.moveToFirst()) total += t.getDouble(0);
        t.close();
        return total;
    }

    public double getMonthTotal(String type, long fromInclusive, long toExclusive) {
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT COALESCE(SUM(amount),0) FROM tx WHERE type=? AND created_at>=? AND created_at<?",
            new String[]{type, String.valueOf(fromInclusive), String.valueOf(toExclusive)}
        );
        double result = c.moveToFirst() ? c.getDouble(0) : 0;
        c.close();
        return result;
    }

    public List<String[]> getCategoryTotals(long fromInclusive, long toExclusive) {
        List<String[]> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT category, SUM(amount) total FROM tx WHERE type='expense' AND created_at>=? AND created_at<? GROUP BY category ORDER BY total DESC",
            new String[]{String.valueOf(fromInclusive), String.valueOf(toExclusive)}
        );
        while (c.moveToNext()) out.add(new String[]{c.getString(0), String.valueOf(c.getDouble(1))});
        c.close();
        return out;
    }

    public List<AccountBalance> getAccountBalances() {
        List<AccountBalance> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT a.name, a.opening_balance + COALESCE(SUM(CASE WHEN t.type='income' THEN t.amount WHEN t.type='expense' THEN -t.amount ELSE 0 END),0) balance " +
                "FROM accounts a LEFT JOIN tx t ON t.account = a.name GROUP BY a.id, a.name, a.opening_balance ORDER BY balance DESC, a.id ASC",
            null
        );
        while (c.moveToNext()) out.add(new AccountBalance(c.getString(0), c.getDouble(1)));
        c.close();
        return out;
    }

    public List<LabelTotal> getLastDaysExpenseTotals(int days) {
        List<LabelTotal> out = new ArrayList<>();
        if (days <= 0) return out;
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        start.add(Calendar.DAY_OF_YEAR, -(days - 1));
        long from = start.getTimeInMillis();

        HashMap<String, Double> totals = new HashMap<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT strftime('%Y-%m-%d', created_at/1000, 'unixepoch', 'localtime') day_key, COALESCE(SUM(amount),0) total " +
                "FROM tx WHERE type='expense' AND created_at>=? GROUP BY day_key ORDER BY day_key ASC",
            new String[]{String.valueOf(from)}
        );
        while (c.moveToNext()) totals.put(c.getString(0), c.getDouble(1));
        c.close();

        Calendar cursorDay = (Calendar) start.clone();
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat labelFormat = new SimpleDateFormat("dd.MM", new Locale("ru", "RU"));
        for (int i = 0; i < days; i++) {
            String key = keyFormat.format(cursorDay.getTime());
            out.add(new LabelTotal(labelFormat.format(cursorDay.getTime()), totals.containsKey(key) ? totals.get(key) : 0));
            cursorDay.add(Calendar.DAY_OF_YEAR, 1);
        }
        return out;
    }

    public List<LabelTotal> getYearMonthExpenseTotals(int year) {
        List<LabelTotal> out = new ArrayList<>();
        Calendar start = Calendar.getInstance();
        start.set(Calendar.YEAR, year);
        start.set(Calendar.MONTH, Calendar.JANUARY);
        start.set(Calendar.DAY_OF_MONTH, 1);
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.YEAR, 1);

        HashMap<String, Double> totals = new HashMap<>();
        Cursor c = getReadableDatabase().rawQuery(
            "SELECT strftime('%Y-%m', created_at/1000, 'unixepoch', 'localtime') month_key, COALESCE(SUM(amount),0) total " +
                "FROM tx WHERE type='expense' AND created_at>=? AND created_at<? GROUP BY month_key ORDER BY month_key ASC",
            new String[]{String.valueOf(start.getTimeInMillis()), String.valueOf(end.getTimeInMillis())}
        );
        while (c.moveToNext()) totals.put(c.getString(0), c.getDouble(1));
        c.close();

        Calendar monthCursor = (Calendar) start.clone();
        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM", Locale.US);
        String[] labels = {"Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"};
        for (int i = 0; i < 12; i++) {
            String key = keyFormat.format(monthCursor.getTime());
            out.add(new LabelTotal(labels[i], totals.containsKey(key) ? totals.get(key) : 0));
            monthCursor.add(Calendar.MONTH, 1);
        }
        return out;
    }
}
