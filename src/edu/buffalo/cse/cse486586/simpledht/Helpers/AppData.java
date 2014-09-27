package edu.buffalo.cse.cse486586.simpledht.Helpers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;

import android.content.ContentResolver;
import android.net.Uri;

public class AppData {

	public static final String TAG = "XXXXXXXXX";

	public static final String MY_TABLE_NAME = "mydstable";
	public static final String KEY_FIELD = "key";
	public static final String VALUE_FIELD = "value";

	public static final String REMOTE_PORTS[] = { "11108", "11112", "11116", "11120", "11124" };
	public static String predecessorByTwoHash = null;
	public static String predecessor = null;
	public static String successor = null;

	/* This is the port X 2 version, i.e, 11108 - 11124 */
	public static String myPort;
	public static String myPortByTwoHash;

	public static Uri mUri;
	public static ContentResolver myContentResolver;

	public static String queryValue = null;
	public static HashMap<String, String> transitMap = null;
	public static HashMap<String, String> receiverResponseMap = null;

	public static boolean checkIfThisIsCorrectNode(String key) {
		boolean returnValue = false;

		try {
			String keyHash = genHash(key);

			boolean chordTrue = (keyHash.compareTo(AppData.predecessorByTwoHash) > 0)
					&& (keyHash.compareTo(AppData.myPortByTwoHash) <= 0);
			/* My port node < predecessor is the edge case. Here, the key hash could be less than my port
			 * or greater than predecessor. */
			boolean chordEdgeCase = ((AppData.myPortByTwoHash.compareTo(AppData.predecessorByTwoHash) < 0)
					&& ((keyHash.compareTo(AppData.myPortByTwoHash) < 0) || (keyHash.compareTo(AppData.predecessorByTwoHash) > 0)));
			
			returnValue = chordTrue || chordEdgeCase;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		return returnValue;
	}

	public static void buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		mUri = uriBuilder.build();
	}

	@SuppressWarnings("resource")
	public static String genHash(String input) throws NoSuchAlgorithmException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		byte[] sha1Hash = sha1.digest(input.getBytes());
		Formatter formatter = new Formatter();
		for (byte b : sha1Hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}
}
