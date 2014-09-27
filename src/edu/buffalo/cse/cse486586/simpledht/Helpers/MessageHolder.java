package edu.buffalo.cse.cse486586.simpledht.Helpers;

import java.io.Serializable;
import java.util.HashMap;

@SuppressWarnings("serial")
public class MessageHolder implements Serializable  {
	
	/* Common Parameter */
	public String message;
	
	/* Join Parameter */
	public String joiningNode;
	
	/* Insert/Query/Delete Parameters */
	public String key;
	public String value;
	public String queryAllPort;
	public HashMap<String, String> resultMap;
	
	/* Join Constructors */
	public MessageHolder(String msg, String joinNode) {
		this.message = msg;
		this.joiningNode = joinNode;
	}
	
	/* Insert/Query/Delete Constructor */
	public MessageHolder(String msg, String key, String val) {
		this.message = msg;
		this.key = key;
		this.value = val;
	}
	
	/* QueryAll Constructor */
	public MessageHolder(String msg, String port, HashMap<String, String> map) {
		this.message = msg;
		this.resultMap = map;
		this.queryAllPort = port;
	}
}
