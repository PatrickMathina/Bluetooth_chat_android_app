package com.pat254.BluChat;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class DB_Manager extends SQLiteOpenHelper {

    private Context context;
    private static final String DATABASE_NAME = "chat_history.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "chats_table";
    private static final String COLUMN_1_ID = "db_id";
    private static final String COLUMN_2_DEVICENAME = "db_deviceName";
    private static final String COLUMN_3_CONNECTEDDEVICENAME = "db_connectedDeviceName";
    private static final String COLUMN_4_TEXTMESSAGE = "db_textMessage";
    private static String query = "";

    public DB_Manager(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME + "("
                + COLUMN_1_ID + " INTEGER PRIMARY KEY AUTOINCREMENT ,"
                + COLUMN_2_DEVICENAME + " VARCHAR(20), "
                + COLUMN_3_CONNECTEDDEVICENAME + " VARCHAR(20), "
                + COLUMN_4_TEXTMESSAGE + " TEXT " + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + TABLE_NAME);
        onCreate(db);
    }

    public void addChatMessages(String devName, String connDevName, String textMsg) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_2_DEVICENAME, devName);
        cv.put(COLUMN_3_CONNECTEDDEVICENAME, connDevName);
        cv.put(COLUMN_4_TEXTMESSAGE, textMsg);
        long result = db.insert(TABLE_NAME, null, cv);
//        test if data is inserted in the db successfully
        if (result==-1) {
            Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Success", Toast.LENGTH_SHORT).show();
        }
    }

    public int deviceCheck(String devName) {
        SQLiteDatabase db = getWritableDatabase();
        String Query = "Select * from " + TABLE_NAME + " where " + COLUMN_3_CONNECTEDDEVICENAME + " = '" + devName + "'";
        Cursor c = db.rawQuery(Query, null);
        return c.getCount();
    }

    public String databaseToString(String devName, int positon) {
        String dbString = "";
        SQLiteDatabase db = getWritableDatabase();
        query = "select * from " + TABLE_NAME + " where " + COLUMN_3_CONNECTEDDEVICENAME + " = '" + devName + "'";

        Cursor c = db.rawQuery(query, null);
        c.moveToFirst();
        while (positon != 0) {
            c.moveToNext();
            positon--;
        }

        if (c.getString(c.getColumnIndex(COLUMN_3_CONNECTEDDEVICENAME)) != null) {
            dbString += c.getString(c.getColumnIndex(COLUMN_2_DEVICENAME)) + ":" + c.getString(c.getColumnIndex(COLUMN_4_TEXTMESSAGE));
        }

        db.close();
        return dbString;
    }
}
