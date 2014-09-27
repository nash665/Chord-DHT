package edu.buffalo.cse.cse486586.simpledht;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.NoSuchAlgorithmException;

import edu.buffalo.cse.cse486586.simpledht.Helpers.AppData;
import edu.buffalo.cse.cse486586.simpledht.Helpers.ClientTask;
import edu.buffalo.cse.cse486586.simpledht.Helpers.MySQLHelper;
import edu.buffalo.cse.cse486586.simpledht.Helpers.ServerTask;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

	static final int SERVER_PORT = 10000;

	private SQLiteDatabase myDatabase;
	MySQLHelper myDatabaseHelper;

	@Override
	public boolean onCreate() {

		AppData.buildUri("content", "edu.buffalo.cse.cse486586.simpledht.provider");

		try {
			TelephonyManager tel = (TelephonyManager) this.getContext().getSystemService(
					Context.TELEPHONY_SERVICE);
			String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
			AppData.myPortByTwoHash = AppData.genHash(portStr);
			AppData.myPort = String.valueOf((Integer.parseInt(portStr) * 2));
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}

		try {
			/*
			 * Create a server socket and a thread (AsyncTask) that listens on
			 * the server port.
			 */
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
		} catch (IOException e) {
			Log.e(AppData.TAG, "Cannot create a ServerSocket");
			return false;
		}

		try {
			Thread.sleep(3000);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

		/* Send node joins to avd0 : 5554 */
		if (!(AppData.myPort.equals(AppData.REMOTE_PORTS[0]))) {
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "Join", AppData.myPort);
		}

		myDatabaseHelper = new MySQLHelper(getContext());
		myDatabase = myDatabaseHelper.getWritableDatabase();

		if (myDatabase == null)
			return false;
		else
			return true;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

		String value = (String) values.get(AppData.VALUE_FIELD);
		String key = (String) values.get(AppData.KEY_FIELD);

		/*
		 * Insert in current node if it is right place or if no connections have been made.
		 */
		if ((AppData.successor == null) || (AppData.predecessor == null) || 
				AppData.checkIfThisIsCorrectNode(key)) {
			long row = myDatabase.insertWithOnConflict(AppData.MY_TABLE_NAME, null, values,
					SQLiteDatabase.CONFLICT_REPLACE);
			if (row <= 0) {
				throw new SQLException("Failed to add a new record into " + uri);
			}
		} else {
			/* Send it on to the next node until it reaches the right node */
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "Insert", key, value);
		}

		Log.v("insert", values.toString());
		return uri;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) {

		Cursor cursor = null;

		if (selection.equals("*")) {

			if ((AppData.successor == null) || (AppData.predecessor == null)) {
				cursor = myDatabase.rawQuery("SELECT * FROM " + AppData.MY_TABLE_NAME, null);
			} else {
				
				/* Return all key-value pairs from entire DHT */
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "QueryAll", AppData.myPort);

				while (AppData.receiverResponseMap == null) {
					/* Wait for response to come */
				}

				/* Get back all cursors here and integrate them into one */
				MatrixCursor m = new MatrixCursor(new String[] { AppData.KEY_FIELD,
						AppData.VALUE_FIELD });
				for (String s : AppData.receiverResponseMap.keySet()) {
					m.addRow(new String[] { s, AppData.receiverResponseMap.get(s) });
				}
				cursor = m;
				m.close();

				AppData.receiverResponseMap = null;
			}

		} else if (selection.equals("@")) {
			/*
			 * Received a query all from another node or an @ operation. Return
			 * key-value pairs from only this node.
			 */
			cursor = myDatabase.rawQuery("SELECT * FROM " + AppData.MY_TABLE_NAME, null);
		} else {

			/* Check which node to query */
			if ((AppData.successor == null) || (AppData.predecessor == null) ||
					AppData.checkIfThisIsCorrectNode(selection)) {
				cursor = myDatabase.rawQuery("SELECT * FROM " + AppData.MY_TABLE_NAME
						+ " WHERE key = ?", new String[] { selection });
			} else {

				/* Send it on to the next node */
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "Query", selection, AppData.myPort);

				while (AppData.queryValue == null) {
					/* Wait for response to come */
				}

				MatrixCursor m = new MatrixCursor(new String[] { AppData.KEY_FIELD,
						AppData.VALUE_FIELD });
				m.addRow(new String[] { selection, AppData.queryValue });
				cursor = m;
				m.close();

				AppData.queryValue = null;
			}
		}

		Log.v("query", selection);

		return cursor;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {

		if (selection.equals("*")) {
			/* Delete all key-value pairs from entire DHT */
			new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "DeleteAll", AppData.myPort);
		} else if (selection.equals("@")) {
			/*
			 * Received a delete all from another node or an @ operation. Delete
			 * key-value pairs from only this node.
			 */
			myDatabase.delete(AppData.MY_TABLE_NAME, null, null);
		} else { /* Delete based on selection parameter */

			if ((AppData.successor == null) || (AppData.predecessor == null) || 
					AppData.checkIfThisIsCorrectNode(selection)) {
				myDatabase.delete(AppData.MY_TABLE_NAME,
						AppData.KEY_FIELD + "='" + selection + "'", null);
			} else {
				/* Send it on to the next node */
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "Delete", selection);
			}
		}
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}

}
