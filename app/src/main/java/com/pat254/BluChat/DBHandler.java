package com.pat254.BluChat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Chat_History.db";
    public static final String TABLE_1 = "chatMessages";

    public static final String COLUMN_DEVICENAME = "_deviceName";
    public static final String COLUMN_CONNECTEDDEVICENAME = "_connectedDeviceName";
    public static final String COLUMN_TEXTMESSAGE = "_textMessage";
    public static final String COLUMN_ID = "_id";
    public static String query = "";

    public DBHandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE " + TABLE_1 + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT ," +
                COLUMN_CONNECTEDDEVICENAME + " VARCHAR(20), " +
                COLUMN_DEVICENAME + " VARCHAR(20), " +
                COLUMN_TEXTMESSAGE + " TEXT " + ");");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_1);
        onCreate(db);
    }

    public void addChatMessages(String connDevName,String devName,String text) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_CONNECTEDDEVICENAME, connDevName);
        contentValues.put(COLUMN_DEVICENAME, devName);
        contentValues.put(COLUMN_TEXTMESSAGE, text);
        db.insert(TABLE_1,null ,contentValues );
    }

    public int deviceCheck(String devName) {

        SQLiteDatabase db = getWritableDatabase();
        String Query = "Select * from " + TABLE_1 + " where " + COLUMN_CONNECTEDDEVICENAME + " = '" + devName + "'";
        Cursor cursor = db.rawQuery(Query, null);
        return cursor.getCount();
    }

    public String databaseToString(String devName,int positon) {
        String dbString = "";
        SQLiteDatabase db = getWritableDatabase();
        query = "SELECT *FROM " + TABLE_1 + " WHERE " + COLUMN_CONNECTEDDEVICENAME + " = '" + devName + "'";

        Cursor c = db.rawQuery(query, null);
        c.moveToFirst();
        while (positon!=0)
        {c.moveToNext();positon--;}

        if (c.getString(c.getColumnIndex(COLUMN_CONNECTEDDEVICENAME)) != null) {
            dbString += c.getString(c.getColumnIndex(COLUMN_DEVICENAME)) + ":" + c.getString(c.getColumnIndex(COLUMN_TEXTMESSAGE));
        }

        db.close();
        return dbString;
    }
}
