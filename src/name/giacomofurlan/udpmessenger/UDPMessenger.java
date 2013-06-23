/*

UDP Messenger by Giacomo Furlan.
http://giacomofurlan.name

This software is being distributed under the Creative Common's Attribution 3.0 Unported licence (CC BY 3.0)
http://creativecommons.org/licenses/by/3.0/

You are not allowed to use this source code for pirate purposes.

This software is provided "as-is" and it comes with no warranties.

*/


package name.giacomofurlan.udpmessenger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public abstract class UDPMessenger {
	protected static String DEBUG_TAG = "UDPMessenger";
	protected static final Integer BUFFER_SIZE = 4096;
	
	protected String TAG;
	protected int MULTICAST_PORT;
	
	private boolean receiveMessages = false;
	
	protected Context context;
	private DatagramSocket socket;
	
	protected abstract Runnable getIncomingMessageAnalyseRunnable();
	private final Handler incomingMessageHandler;
	protected Message incomingMessage;
	private Thread receiverThread;
	
	/**
	 * Class constructor
	 * @param context the application's context
	 * @param tag a valid string, used to filter the UDP broadcast messages (in and out). It can't be null or 0-characters long.
	 * @param multicastPort the port to multicast to. Must be between 1025 and 49151 (inclusive)
	 * @param connectionPort the port to get the connection back. Must be between 1025 and 49151
	 */
	public UDPMessenger(Context context, String tag, int multicastPort) throws IllegalArgumentException {
		if(context == null || tag == null || tag.length() == 0 ||
			multicastPort <= 1024 || multicastPort > 49151)
			throw new IllegalArgumentException();
		
		this.context = context.getApplicationContext();
		TAG = tag;
		MULTICAST_PORT = multicastPort;
		
		incomingMessageHandler = new Handler(Looper.getMainLooper());
	}
	
	/**
	 * Sends a broadcast message (TAG EPOCH_TIME message). Opens a new socket in case it's closed.
	 * @param message the message to send (multicast). It can't be null or 0-characters long.
	 * @return
	 * @throws IllegalArgumentException
	 */
	public boolean sendMessage(String message) throws IllegalArgumentException {
		if(message == null || message.length() == 0)
			throw new IllegalArgumentException();
		
		// Check for WiFi connectivity
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		
		if(mWifi == null || !mWifi.isConnected())
		{
			Log.d(DEBUG_TAG, "Sorry! You need to be in a WiFi network in order to send UDP multicast packets. Aborting.");
			return false;
		}
		
		// Check for IP address
		WifiManager wim = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		int ip = wim.getConnectionInfo().getIpAddress();
		
		// Create the send socket
		if(socket == null) {
			try {
				socket = new DatagramSocket();
			} catch (SocketException e) {
				Log.d(DEBUG_TAG, "There was a problem creating the sending socket. Aborting.");
				e.printStackTrace();
				return false;
			}
		}
		
		// Build the packet
		DatagramPacket packet;
		Message msg = new Message(TAG, message);
		byte data[] = msg.toString().getBytes();
		
		try {
			packet = new DatagramPacket(data, data.length, InetAddress.getByName(ipToString(ip, true)), MULTICAST_PORT);
		} catch (UnknownHostException e) {
			Log.d(DEBUG_TAG, "It seems that " + ipToString(ip, true) + " is not a valid ip! Aborting.");
			e.printStackTrace();
			return false;
		}
		
		try {
			socket.send(packet);
		} catch (IOException e) {
			Log.d(DEBUG_TAG, "There was an error sending the UDP packet. Aborted.");
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public void startMessageReceiver() {
		Runnable receiver = new Runnable() {

			@Override
			public void run() {
				WifiManager wim = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				if(wim != null) {
					MulticastLock mcLock = wim.createMulticastLock(TAG);
					mcLock.acquire();
				}
				
				byte[] buffer = new byte[BUFFER_SIZE];
				DatagramPacket rPacket = new DatagramPacket(buffer, buffer.length);
				MulticastSocket rSocket;
				
				try {
					rSocket = new MulticastSocket(MULTICAST_PORT);
				} catch (IOException e) {
					Log.d(DEBUG_TAG, "Impossible to create a new MulticastSocket on port " + MULTICAST_PORT);
					e.printStackTrace();
					return;
				}
				
				while(receiveMessages) {
					try {
						rSocket.receive(rPacket);
					} catch (IOException e1) {
						Log.d(DEBUG_TAG, "There was a problem receiving the incoming message.");
						e1.printStackTrace();
						continue;
					}
					
					if(!receiveMessages)
						break;
					
					byte data[] = rPacket.getData();
					int i;
					for(i = 0; i < data.length; i++)
					{
						if(data[i] == '\0')
							break;
					}
					
					String messageText;
					
					try {
						messageText = new String(data, 0, i, "UTF-8");
					} catch (UnsupportedEncodingException e) {
						Log.d(DEBUG_TAG, "UTF-8 encoding is not supported. Can't receive the incoming message.");
						e.printStackTrace();
						continue;
					}
					
					try {
						incomingMessage = new Message(messageText, rPacket.getAddress());
					} catch (IllegalArgumentException ex) {
						Log.d(DEBUG_TAG, "There was a problem processing the message: " + messageText);
						ex.printStackTrace();
						continue;
					}
					
					incomingMessageHandler.post(getIncomingMessageAnalyseRunnable());
				}
			}
			
		};
		
		receiveMessages = true;
		if(receiverThread == null)
			receiverThread = new Thread(receiver);
		
		if(!receiverThread.isAlive())
			receiverThread.start();
	}
	
	public void stopMessageReceiver() {
		receiveMessages = false;
	}
	
	public static String ipToString(int ip, boolean broadcast) {
		String result = new String();
		
		Integer[] address = new Integer[4];
		for(int i = 0; i < 4; i++)
			address[i] = (ip >> 8*i) & 0xFF;
		for(int i = 0; i < 4; i++) {
			if(i != 3)
				result = result.concat(address[i]+".");
			else result = result.concat("255.");
		}
		return result.substring(0, result.length() - 2);
	}
}
