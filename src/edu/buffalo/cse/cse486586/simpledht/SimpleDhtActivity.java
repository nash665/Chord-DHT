 /***
  * SimpleDHTActivity is the main activity for the SimpleDHT project.
  * It represents the first and only screen in this app.
  * 
  * @author Nishanth Vasisht
  */

package edu.buffalo.cse.cse486586.simpledht;

import java.util.Timer;
import java.util.TimerTask;

import edu.buffalo.cse.cse486586.simpledht.Helpers.AppData;
import edu.buffalo.cse.cse486586.simpledht.Helpers.OnTestClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.database.Cursor;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.view.View.OnClickListener;

public class SimpleDhtActivity extends Activity {

	public static TextView mtv;
	
	private Handler handler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple_dht_main);

		TextView tv = (TextView) findViewById(R.id.textView1);
		tv.setMovementMethod(new ScrollingMovementMethod());
		mtv = tv;
		findViewById(R.id.button3).setOnClickListener(
				new OnTestClickListener(tv, getContentResolver()));

		/* LDump logic */
		findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Cursor resultCursor = getContentResolver().query(AppData.mUri, null, "@", null,
						null);
				printValues(resultCursor);
			}
		});

		/* GDump logic */
		findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Cursor resultCursor = getContentResolver().query(AppData.mUri, null, "*", null,
						null);
				printValues(resultCursor);
			}
		});

		/* LDelete logic */
		findViewById(R.id.button4).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				getContentResolver().delete(AppData.mUri, "@", null);
			}
		});

		/* GDelete logic */
		findViewById(R.id.button5).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				getContentResolver().delete(AppData.mUri, "*", null);
			}
		});

		/* Clear logic */
		findViewById(R.id.button6).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mtv.setText("");
			}
		});

		/* Keep a copy of the content resolver accessible from everywhere */
		AppData.myContentResolver = getContentResolver();

		Timer t = new Timer();
		t.schedule(new TimerTask() {

			@Override
			public void run() {
				handler.post(new Runnable() {
					public void run() {
						printAfterWait();
					}
				});
			}
		}, 5000);
	}

	public void printAfterWait() {
		mtv.append("\n\tPredecessor : " + AppData.predecessor);
		mtv.append("\n\tSuccessor   : " + AppData.successor);
	}

	public void printValues(Cursor resultCursor) {

		try {
			if (resultCursor == null) {
				Log.e(AppData.TAG, "Result null");
				throw new Exception();
			}

			int keyIndex = resultCursor.getColumnIndex(AppData.KEY_FIELD);
			int valueIndex = resultCursor.getColumnIndex(AppData.VALUE_FIELD);
			if (keyIndex == -1 || valueIndex == -1) {
				Log.e(AppData.TAG, "Wrong columns");
				resultCursor.close();
				throw new Exception();
			}

			resultCursor.moveToFirst();

			while (resultCursor.isAfterLast() == false) {
				String returnKey = resultCursor.getString(keyIndex);
				String returnValue = resultCursor.getString(valueIndex);
				mtv.append("\n" + returnKey + " : " + returnValue);
				resultCursor.moveToNext();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
		return true;
	}
}
