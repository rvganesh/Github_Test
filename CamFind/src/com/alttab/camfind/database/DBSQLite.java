
package com.alttab.camfind.database;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.alttab.camfind.bean.SearchBean;
import com.common.PrintLog;

public class DBSQLite extends SQLiteOpenHelper {

	// All Static variables
	// Database Version
	private static final int DATABASE_VERSION = 1;

	// Database Name
	private static final String DATABASE_NAME = "camfind";

	// Contacts table name
	private static final String TABLE_NAME = "SearchHistory";

	// Contacts Table Columns names
	public static final String KEY_ID = "id";
	public static final String KEY_SEARCH_DATE		="searchdate";
	public static final String KEY_SEARCH_TEXT		="searchtext";
	public static final String KEY_IMAGE_PATH		="searchimagepath";
	public static final String KEY_IMAGE_THUMBNAIL	="searchimagethumbnailpath";
	public static final String KEY_AUDIO_PATH	 	="searchaudiopath";
	public static final String KEY_LANGUAGE		 	="searchlanguage";

	public DBSQLite(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) 
	{
		PrintLog.debug("DBSQlite", "OnCreate db.");
		String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_NAME 
				+ "("
				+ KEY_ID + " INTEGER PRIMARY KEY," 
				+ KEY_SEARCH_DATE + " TEXT,"
				+ KEY_SEARCH_TEXT + " TEXT," 
				+ KEY_IMAGE_PATH + " TEXT,"
				+ KEY_IMAGE_THUMBNAIL + " TEXT," 
				+ KEY_AUDIO_PATH + " TEXT,"
				+ KEY_LANGUAGE + " TEXT"
				+ ")";
		db.execSQL(CREATE_CONTACTS_TABLE);
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
		PrintLog.debug("DBSQlite", "OnCreate upgrade.");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);

		// Create tables again
		onCreate(db);
	}

	/**
	 * All CRUD(Create, Read, Update, Delete) Operations
	 */

	public void addHistory(SearchBean searchBean) {
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(KEY_SEARCH_DATE, searchBean.getSearchDate());
		values.put(KEY_SEARCH_TEXT, searchBean.getSearchText());
		values.put(KEY_IMAGE_PATH, searchBean.getSearchImagePath());
		values.put(KEY_IMAGE_THUMBNAIL, searchBean.getSearchImageThumbnailPath());
		values.put(KEY_AUDIO_PATH, searchBean.getSearchAudioPath()); 
		values.put(KEY_LANGUAGE, searchBean.getSearchLanguage()); 

		// Inserting Row
		db.insert(TABLE_NAME, null, values);
		db.close(); // Closing database connection
	}

	public SearchBean getHistory(int id) {
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor cursor = db.query(TABLE_NAME, new String[] { KEY_ID,	KEY_SEARCH_DATE, KEY_SEARCH_TEXT }, KEY_ID + "=?",new String[] { String.valueOf(id) }, null, null, null, null);
		if (cursor != null)
			cursor.moveToFirst();

		SearchBean contact = new SearchBean(Integer.parseInt(cursor.getString(0)),
				cursor.getString(1), cursor.getString(2),cursor.getString(3),cursor.getString(4),cursor.getString(5),cursor.getString(6));
		return contact;
	}
	
	public ArrayList<SearchBean> getAllHistorys() {
		ArrayList<SearchBean> contactList = new ArrayList<SearchBean>();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_NAME;

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				SearchBean contact = new SearchBean();
				contact.setId(Integer.parseInt(cursor.getString(0)));
				contact.setSearchDate(cursor.getString(1));
				contact.setSearchText(cursor.getString(2));
				contact.setSearchImagePath(cursor.getString(3));
				contact.setSearchImageThumbnailPath(cursor.getString(4));
				contact.setSearchAudioPath(cursor.getString(5));
				contact.setSearchLanguage(cursor.getString(6));
				contactList.add(contact);
			} while (cursor.moveToNext());
		}

		return contactList;
	}

	public int updateHistroy(SearchBean contact) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(KEY_SEARCH_DATE, contact.getSearchDate());
		values.put(KEY_SEARCH_TEXT, contact.getSearchText());

		// updating row
		return db.update(TABLE_NAME, values, KEY_ID + " = ?",
				new String[] { String.valueOf(contact.getId()) });
	}

	public void deleteHistroy(SearchBean contact) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_NAME, KEY_ID + " = ?",
				new String[] { String.valueOf(contact.getId()) });
		db.close();
	}


	public int getHistroyCount() {
		String countQuery = "SELECT  * FROM " + TABLE_NAME;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		cursor.close();

		return cursor.getCount();
	}

}
