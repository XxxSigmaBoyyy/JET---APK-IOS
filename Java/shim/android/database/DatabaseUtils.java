package android.database;
/** AOSP DatabaseUtils helpers (statement classification, escaping, simple queries). */
public class DatabaseUtils {
    public static final int STATEMENT_ABORT = 6;
    public static final int STATEMENT_ATTACH = 3;
    public static final int STATEMENT_BEGIN = 4;
    public static final int STATEMENT_COMMIT = 5;
    public static final int STATEMENT_DDL = 8;
    public static final int STATEMENT_OTHER = 99;
    public static final int STATEMENT_PRAGMA = 7;
    public static final int STATEMENT_SELECT = 1;
    public static final int STATEMENT_UNPREPARED = 9;
    public static final int STATEMENT_UPDATE = 2;

    public DatabaseUtils() {}
    public static int getSqlStatementType(String sql) {
        sql = sql.trim();
        if (sql.length() < 3) return STATEMENT_OTHER;
        String prefixSql = sql.substring(0, 3).toUpperCase(java.util.Locale.ROOT);
        switch (prefixSql) {
            case "SEL": return STATEMENT_SELECT;
            case "INS": case "UPD": case "REP": case "DEL": return STATEMENT_UPDATE;
            case "ATT": return STATEMENT_ATTACH;
            case "COM": case "END": return STATEMENT_COMMIT;
            case "ROL": return sql.toUpperCase(java.util.Locale.ROOT).contains(" TO ") ? STATEMENT_OTHER : STATEMENT_ABORT;
            case "BEG": return STATEMENT_BEGIN;
            case "PRA": return STATEMENT_PRAGMA;
            case "CRE": case "DRO": case "ALT": return STATEMENT_DDL;
            case "ANA": case "DET": return STATEMENT_UNPREPARED;
        }
        return STATEMENT_OTHER;
    }
    public static void appendEscapedSQLString(StringBuilder sb, String s) {
        sb.append('\'');
        if (s.indexOf('\'') != -1) { int length = s.length(); for (int i = 0; i < length; i++) { char c = s.charAt(i); if (c == '\'') sb.append('\''); sb.append(c); } } else sb.append(s);
        sb.append('\'');
    }
    public static String sqlEscapeString(String value) { StringBuilder sb = new StringBuilder(); appendEscapedSQLString(sb, value); return sb.toString(); }
    public static final void appendValueToSql(StringBuilder sql, Object value) {
        if (value == null) sql.append("NULL");
        else if (value instanceof Boolean) sql.append(((Boolean) value) ? '1' : '0');
        else appendEscapedSQLString(sql, value.toString());
    }
    public static String concatenateWhere(String a, String b) { if (a == null || a.isEmpty()) return b; if (b == null || b.isEmpty()) return a; return "(" + a + ") AND (" + b + ")"; }
    public static String[] appendSelectionArgs(String[] originalValues, String[] newValues) {
        if (originalValues == null || originalValues.length == 0) return newValues;
        String[] result = new String[originalValues.length + newValues.length];
        System.arraycopy(originalValues, 0, result, 0, originalValues.length); System.arraycopy(newValues, 0, result, originalValues.length, newValues.length); return result;
    }
    public static long queryNumEntries(android.database.sqlite.SQLiteDatabase db, String table) { return queryNumEntries(db, table, null, null); }
    public static long queryNumEntries(android.database.sqlite.SQLiteDatabase db, String table, String selection) { return queryNumEntries(db, table, selection, null); }
    public static long queryNumEntries(android.database.sqlite.SQLiteDatabase db, String table, String selection, String[] selectionArgs) {
        String s = (selection != null && !selection.isEmpty()) ? " where " + selection : "";
        return longForQuery(db, "select count(*) from " + table + s, selectionArgs);
    }
    public static long longForQuery(android.database.sqlite.SQLiteDatabase db, String query, String[] selectionArgs) {
        Cursor c = db.rawQuery(query, selectionArgs); try { return c.moveToFirst() ? c.getLong(0) : 0; } finally { c.close(); }
    }
    public static String stringForQuery(android.database.sqlite.SQLiteDatabase db, String query, String[] selectionArgs) {
        Cursor c = db.rawQuery(query, selectionArgs); try { return c.moveToFirst() ? c.getString(0) : null; } finally { c.close(); }
    }
    // ---- BEGIN generated SDK surface (jetc/gen_sdk_surface.py) ----
    public static void writeExceptionToParcel(android.os.Parcel p0, java.lang.Exception p1) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.writeExceptionToParcel(Landroid/os/Parcel;Ljava/lang/Exception;)V"); }
    public static void readExceptionFromParcel(android.os.Parcel p0) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.readExceptionFromParcel(Landroid/os/Parcel;)V"); }
    public static void readExceptionWithFileNotFoundExceptionFromParcel(android.os.Parcel p0) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.readExceptionWithFileNotFoundExceptionFromParcel(Landroid/os/Parcel;)V"); }
    public static void readExceptionWithOperationApplicationExceptionFromParcel(android.os.Parcel p0) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.readExceptionWithOperationApplicationExceptionFromParcel(Landroid/os/Parcel;)V"); }
    public static void bindObjectToProgram(android.database.sqlite.SQLiteProgram p0, int p1, java.lang.Object p2) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.bindObjectToProgram(Landroid/database/sqlite/SQLiteProgram;ILjava/lang/Object;)V"); }
    public static java.lang.String getCollationKey(java.lang.String p0) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.getCollationKey(Ljava/lang/String;)Ljava/lang/String;"); return null; }
    public static java.lang.String getHexCollationKey(java.lang.String p0) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.getHexCollationKey(Ljava/lang/String;)Ljava/lang/String;"); return null; }
    public static void dumpCursor(android.database.Cursor p0) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.dumpCursor(Landroid/database/Cursor;)V"); }
    public static void dumpCursor(android.database.Cursor p0, java.io.PrintStream p1) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.dumpCursor(Landroid/database/Cursor;Ljava/io/PrintStream;)V"); }
    public static void dumpCursor(android.database.Cursor p0, java.lang.StringBuilder p1) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.dumpCursor(Landroid/database/Cursor;Ljava/lang/StringBuilder;)V"); }
    public static java.lang.String dumpCursorToString(android.database.Cursor p0) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.dumpCursorToString(Landroid/database/Cursor;)Ljava/lang/String;"); return null; }
    public static void dumpCurrentRow(android.database.Cursor p0) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.dumpCurrentRow(Landroid/database/Cursor;)V"); }
    public static void dumpCurrentRow(android.database.Cursor p0, java.io.PrintStream p1) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.dumpCurrentRow(Landroid/database/Cursor;Ljava/io/PrintStream;)V"); }
    public static void dumpCurrentRow(android.database.Cursor p0, java.lang.StringBuilder p1) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.dumpCurrentRow(Landroid/database/Cursor;Ljava/lang/StringBuilder;)V"); }
    public static java.lang.String dumpCurrentRowToString(android.database.Cursor p0) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.dumpCurrentRowToString(Landroid/database/Cursor;)Ljava/lang/String;"); return null; }
    public static void cursorStringToContentValues(android.database.Cursor p0, java.lang.String p1, android.content.ContentValues p2) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.cursorStringToContentValues(Landroid/database/Cursor;Ljava/lang/String;Landroid/content/ContentValues;)V"); }
    public static void cursorStringToContentValues(android.database.Cursor p0, java.lang.String p1, android.content.ContentValues p2, java.lang.String p3) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.cursorStringToContentValues(Landroid/database/Cursor;Ljava/lang/String;Landroid/content/ContentValues;Ljava/lang/String;)V"); }
    public static void cursorIntToContentValues(android.database.Cursor p0, java.lang.String p1, android.content.ContentValues p2) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.cursorIntToContentValues(Landroid/database/Cursor;Ljava/lang/String;Landroid/content/ContentValues;)V"); }
    public static void cursorIntToContentValues(android.database.Cursor p0, java.lang.String p1, android.content.ContentValues p2, java.lang.String p3) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.cursorIntToContentValues(Landroid/database/Cursor;Ljava/lang/String;Landroid/content/ContentValues;Ljava/lang/String;)V"); }
    public static void cursorLongToContentValues(android.database.Cursor p0, java.lang.String p1, android.content.ContentValues p2) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.cursorLongToContentValues(Landroid/database/Cursor;Ljava/lang/String;Landroid/content/ContentValues;)V"); }
    public static void cursorLongToContentValues(android.database.Cursor p0, java.lang.String p1, android.content.ContentValues p2, java.lang.String p3) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.cursorLongToContentValues(Landroid/database/Cursor;Ljava/lang/String;Landroid/content/ContentValues;Ljava/lang/String;)V"); }
    public static void cursorDoubleToCursorValues(android.database.Cursor p0, java.lang.String p1, android.content.ContentValues p2) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.cursorDoubleToCursorValues(Landroid/database/Cursor;Ljava/lang/String;Landroid/content/ContentValues;)V"); }
    public static void cursorDoubleToContentValues(android.database.Cursor p0, java.lang.String p1, android.content.ContentValues p2, java.lang.String p3) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.cursorDoubleToContentValues(Landroid/database/Cursor;Ljava/lang/String;Landroid/content/ContentValues;Ljava/lang/String;)V"); }
    public static void cursorRowToContentValues(android.database.Cursor p0, android.content.ContentValues p1) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.cursorRowToContentValues(Landroid/database/Cursor;Landroid/content/ContentValues;)V"); }
    public static long longForQuery(android.database.sqlite.SQLiteStatement p0, java.lang.String[] p1) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.longForQuery(Landroid/database/sqlite/SQLiteStatement;[Ljava/lang/String;)J"); return 0; }
    public static java.lang.String stringForQuery(android.database.sqlite.SQLiteStatement p0, java.lang.String[] p1) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.stringForQuery(Landroid/database/sqlite/SQLiteStatement;[Ljava/lang/String;)Ljava/lang/String;"); return null; }
    public static void cursorStringToContentValuesIfPresent(android.database.Cursor p0, android.content.ContentValues p1, java.lang.String p2) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.cursorStringToContentValuesIfPresent(Landroid/database/Cursor;Landroid/content/ContentValues;Ljava/lang/String;)V"); }
    public static void cursorLongToContentValuesIfPresent(android.database.Cursor p0, android.content.ContentValues p1, java.lang.String p2) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.cursorLongToContentValuesIfPresent(Landroid/database/Cursor;Landroid/content/ContentValues;Ljava/lang/String;)V"); }
    public static void cursorShortToContentValuesIfPresent(android.database.Cursor p0, android.content.ContentValues p1, java.lang.String p2) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.cursorShortToContentValuesIfPresent(Landroid/database/Cursor;Landroid/content/ContentValues;Ljava/lang/String;)V"); }
    public static void cursorIntToContentValuesIfPresent(android.database.Cursor p0, android.content.ContentValues p1, java.lang.String p2) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.cursorIntToContentValuesIfPresent(Landroid/database/Cursor;Landroid/content/ContentValues;Ljava/lang/String;)V"); }
    public static void cursorFloatToContentValuesIfPresent(android.database.Cursor p0, android.content.ContentValues p1, java.lang.String p2) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.cursorFloatToContentValuesIfPresent(Landroid/database/Cursor;Landroid/content/ContentValues;Ljava/lang/String;)V"); }
    public static void cursorDoubleToContentValuesIfPresent(android.database.Cursor p0, android.content.ContentValues p1, java.lang.String p2) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.cursorDoubleToContentValuesIfPresent(Landroid/database/Cursor;Landroid/content/ContentValues;Ljava/lang/String;)V"); }
    public static void createDbFromSqlStatements(android.content.Context p0, java.lang.String p1, int p2, java.lang.String p3) { jet.runtime.ShimStubs.hit("android.database.DatabaseUtils.createDbFromSqlStatements(Landroid/content/Context;Ljava/lang/String;ILjava/lang/String;)V"); }
    // ---- END generated SDK surface ----
}
