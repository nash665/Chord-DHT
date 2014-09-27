package edu.buffalo.cse.cse486586.simpledht.Helpers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MySQLHelper extends SQLiteOpenHelper {
	
	private static final int DATABASE_VERSION = 1;
	
	private static final String DATABASE_NAME = "mydsdatabase";
	private static final String DICTIONARY_TABLE_CREATE = "CREATE TABLE " + AppData.MY_TABLE_NAME + " ("
			+ AppData.KEY_FIELD + " TEXT PRIMARY KEY, " + AppData.VALUE_FIELD + " TEXT);";

	public MySQLHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DICTIONARY_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
	}
}
