/*

UDP Messenger by Giacomo Furlan.
http://giacomofurlan.name

This software is being distributed under the Creative Common's Attribution 3.0 Unported licence (CC BY 3.0)
http://creativecommons.org/licenses/by/3.0/

You are not allowed to use this source code for pirate purposes.

This software is provided "as-is" and it comes with no warranties.

*/

package name.giacomofurlan.udpmessenger;

import java.net.InetAddress;

public class Message {
	private String tag;
	private String message;
	private long epoch = 0;
	private InetAddress ip;
	
	public Message(String message) throws IllegalArgumentException {
		this(message, (InetAddress) null);
	}
	
	public Message(String message, InetAddress ip) throws IllegalArgumentException {
		String split[] = message.split(" ");
		if(split.length < 3)
			throw new IllegalArgumentException();
		
		tag = split[0];
		epoch = Integer.parseInt(split[1]);
		this.ip = ip;
		
		message = "";
		for(int i = 2; i < split.length; i++)
			message = message.concat(split[i] + " ");
		
		this.message = message.substring(0, message.length() - 1);
		
	}
	
	public Message(String tag, String message) {
		this(tag, message, null);
	}
	
	public Message(String tag, String message, InetAddress ip) {
		this(tag, message, ip, System.currentTimeMillis()/1000);
	}
	
	public Message(String tag, String message, InetAddress ip, long time) {
		this.tag = tag;
		this.message = message;
		epoch = time;
		this.ip = ip;
	}
	
	public String getTag() { return tag; }
	public String getMessage() { return message; }
	public long getEpochTime() { return epoch; }
	public InetAddress getSrcIp() { return ip; }
	
	public String toString() { return tag+" " + epoch + " " + message; }
}
