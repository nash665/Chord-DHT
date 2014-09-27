 /***
  * ServerTask is an AsyncTask that should handle incoming messages. It is
  * created by ServerTask.executeOnExecutor() call.
  * 
  * @author Nishanth Vasisht
  */

package edu.buffalo.cse.cse486586.simpledht.Helpers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

import edu.buffalo.cse.cse486586.simpledht.SimpleDhtActivity;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

public class ServerTask extends AsyncTask<ServerSocket, String, Void> {

	Object joinLock = new Object();

	@Override
	protected Void doInBackground(ServerSocket... sockets) {

		ServerSocket serverSocket = sockets[0];

		try {
			while (true) {
				Socket socket = serverSocket.accept();

				MessageHolder incomingMessage = (MessageHolder) (new ObjectInputStream(
						socket.getInputStream())).readObject();

				Log.v(AppData.TAG, "Incoming Message : " + incomingMessage.message);

				/* Join Handling */
				if (incomingMessage.message.equals("Join")) {
					implementJoinLogic(incomingMessage.joiningNode);
					publishProgress(incomingMessage.message, incomingMessage.joiningNode);
				} else if (incomingMessage.message.equals("PassOnJoin")) {
					implementJoinLogic(incomingMessage.joiningNode);
					publishProgress(incomingMessage.message, incomingMessage.joiningNode);
				} else if (incomingMessage.message.equals("JoinUpdateSuccessor")) {
					AppData.successor = incomingMessage.joiningNode;
				} else if (incomingMessage.message.equals("JoinUpdatePredecessor")) {
					AppData.predecessor = incomingMessage.joiningNode;
					AppData.predecessorByTwoHash = AppData.genHash(String.valueOf(Integer
							.valueOf(incomingMessage.joiningNode) / 2));
				} 
				
				/* Insert Handling */
				else if (incomingMessage.message.equals("Insert")) {
					ContentValues value = new ContentValues();
					value.put(AppData.KEY_FIELD, incomingMessage.key);
					value.put(AppData.VALUE_FIELD, incomingMessage.value);
					AppData.myContentResolver.insert(AppData.mUri, value);
				} 
				
				/* Query Handling */
				else if (incomingMessage.message.equals("Query")) {

					if (AppData.checkIfThisIsCorrectNode(incomingMessage.key)) {
						/* Query here */
						Cursor c = AppData.myContentResolver.query(AppData.mUri, null,
								incomingMessage.key, null, null);
						if (c != null) {

							int keyIndex = c.getColumnIndex(AppData.KEY_FIELD);
							int valueIndex = c.getColumnIndex(AppData.VALUE_FIELD);
							if (keyIndex == -1 || valueIndex == -1) {
								Log.e(AppData.TAG, "Wrong columns");
								c.close();
								continue;
							}

							c.moveToFirst();

							if (!(c.isFirst() && c.isLast())) {
								Log.e(AppData.TAG, "Wrong number of rows");
								c.close();
								continue;
							}

							new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
									"QueryResponse", incomingMessage.value,
									c.getString(c.getColumnIndex(AppData.VALUE_FIELD)));
						}

					} else {
						new ClientTask()
								.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
										incomingMessage.message, incomingMessage.key,
										incomingMessage.value);
					}
				} else if (incomingMessage.message.equals("QueryResponse")) {
					if (AppData.myPort.equals(incomingMessage.key)) {
						AppData.queryValue = incomingMessage.value;
					} else {
						new ClientTask()
								.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
										incomingMessage.message, incomingMessage.key,
										incomingMessage.value);
					}
				} else if (incomingMessage.message.equals("QueryAll")) {

					Cursor c = AppData.myContentResolver.query(AppData.mUri, null, "@", null, null);

					if (c != null) {

						int keyIndex = c.getColumnIndex(AppData.KEY_FIELD);
						int valueIndex = c.getColumnIndex(AppData.VALUE_FIELD);
						if (keyIndex == -1 || valueIndex == -1) {
							Log.e(AppData.TAG, "Wrong columns");
							c.close();
							continue;
						}

						c.moveToFirst();

						while (c.isAfterLast() == false) {
							incomingMessage.resultMap.put(
									c.getString(c.getColumnIndex(AppData.KEY_FIELD)),
									c.getString(c.getColumnIndex(AppData.VALUE_FIELD)));
							c.moveToNext();
						}
					}

					if (incomingMessage.queryAllPort.equals(AppData.myPort)) {
						AppData.receiverResponseMap = incomingMessage.resultMap;
					} else {
						AppData.transitMap = incomingMessage.resultMap;
						new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
								"QueryAll", incomingMessage.queryAllPort);
					}

				} 
				
				/* Delete Handling */
				else if (incomingMessage.message.equals("Delete")) {
					AppData.myContentResolver.delete(AppData.mUri, incomingMessage.key, null);
				} else if (incomingMessage.message.equals("DeleteAll")) {
					AppData.myContentResolver.delete(AppData.mUri, "@", null);
					if (!(AppData.myPort.equals(incomingMessage.value))) {
						new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "DeleteAll",
								incomingMessage.value);
					}
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return null;
	}

	protected void onProgressUpdate(String... strings) {
		SimpleDhtActivity.mtv.append("\n" + strings[0] + " came from " + strings[1]);
		return;
	}

	private void implementJoinLogic(String joiningNode) {

		try {
			String nodeJoin = String.valueOf(Integer.valueOf(joiningNode) / 2);
			String joinHash = AppData.genHash(nodeJoin);
			Log.v("Join", nodeJoin + " : " + joinHash);

			boolean chordTrue = false;
			boolean chordEdgeCase = false;

			if (AppData.predecessorByTwoHash != null) {
				chordTrue = (joinHash.compareTo(AppData.predecessorByTwoHash) > 0)
						&& (joinHash.compareTo(AppData.myPortByTwoHash) <= 0);
				chordEdgeCase = ((AppData.myPortByTwoHash.compareTo(AppData.predecessorByTwoHash) < 0) && ((joinHash
						.compareTo(AppData.myPortByTwoHash) < 0) || (joinHash
						.compareTo(AppData.predecessorByTwoHash) > 0)));
			}
			boolean initialCase = (AppData.successor == null) || (AppData.predecessor == null);

			if (chordTrue || chordEdgeCase || initialCase) {

				/*
				 * First update the joining node's predecessor and successor
				 */
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
						"JoinUpdateSuccessor", joiningNode, AppData.myPort);

				String pred = (AppData.predecessor == null) ? AppData.myPort : AppData.predecessor;
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
						"JoinUpdatePredecessor", joiningNode, pred);

				/* Then update predecessor with new successor */
				if (AppData.predecessor == null) {
					AppData.successor = joiningNode;
				} else {
					new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
							"JoinUpdateSuccessor", AppData.predecessor, joiningNode);
				}

				/* Then update self with new predecessor */
				AppData.predecessor = joiningNode;

				AppData.predecessorByTwoHash = AppData.genHash(String.valueOf(Integer
						.valueOf(joiningNode) / 2));

			} else {
				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "PassOnJoin",
						joiningNode);
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
}
