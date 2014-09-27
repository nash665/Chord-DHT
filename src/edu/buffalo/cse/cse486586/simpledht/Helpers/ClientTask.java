 /***
  * ClientTask is an AsyncTask that should send a string over the network. It is
  * created by ClientTask.executeOnExecutor() call.
  * 
  * @author Nishanth Vasisht
  */
package edu.buffalo.cse.cse486586.simpledht.Helpers;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;

import android.os.AsyncTask;
import android.util.Log;

public class ClientTask extends AsyncTask<String, Void, Void> {

	@Override
	protected Void doInBackground(String... msgs) {

		try {

			Log.v(AppData.TAG, "Sending message : " + msgs[0]);

			MessageHolder mHolder = null;
			String remotePort = AppData.successor;

			/* Join handling */
			if (msgs[0].equals("Join")) {
				/* Send to avd0 */
				mHolder = new MessageHolder(msgs[0], msgs[1]);
				remotePort = AppData.REMOTE_PORTS[0];
			} else if (msgs[0].equals("PassOnJoin")) {
				mHolder = new MessageHolder(msgs[0], msgs[1]);
			} else if (msgs[0].equals("JoinUpdateSuccessor")) {
				mHolder = new MessageHolder(msgs[0], msgs[2]);
				remotePort = msgs[1];
			} else if (msgs[0].equals("JoinUpdatePredecessor")) {
				mHolder = new MessageHolder(msgs[0], msgs[2]);
				remotePort = msgs[1];
			} else if (msgs[0].equals("Insert")) {
				mHolder = new MessageHolder(msgs[0], msgs[1], msgs[2]);
			} else if (msgs[0].equals("Query")) {
				mHolder = new MessageHolder(msgs[0], msgs[1], msgs[2]);
			} else if (msgs[0].equals("QueryAll")) {
				if (AppData.transitMap == null) {
					mHolder = new MessageHolder(msgs[0], msgs[1], new HashMap<String, String>());
				}
				else {
					mHolder = new MessageHolder(msgs[0], msgs[1], AppData.transitMap);
					AppData.transitMap = null;
				}
			} else if (msgs[0].equals("QueryResponse")) {
				mHolder = new MessageHolder(msgs[0], msgs[1], msgs[2]);
			} else if (msgs[0].equals("Delete")) {
				mHolder = new MessageHolder(msgs[0], msgs[1], "");
			} else if (msgs[0].equals("DeleteAll")) {
				mHolder = new MessageHolder(msgs[0], null, msgs[1]);
			}

			Socket socket = new Socket(InetAddress.getByAddress(new byte[] { 10, 0, 2, 2 }),
					Integer.parseInt(remotePort));

			OutputStream outStream = socket.getOutputStream();
			ObjectOutputStream objOutStream = new ObjectOutputStream(outStream);
			objOutStream.writeObject(mHolder);
			objOutStream.close();
			outStream.close();
			socket.close();

		} catch (UnknownHostException e) {
			Log.e(AppData.TAG, "ClientTask UnknownHostException");
		} catch (IOException e) {
			Log.e(AppData.TAG, "ClientTask socket IOException");
		}

		return null;
	}
}
