UDP Multicast text messenger
============================
UDP Messenger by [Giacomo Furlan](http://giacomofurlan.name "Giacomo Furlan's blogfolio").

This software is being distributed under the [CC BY 3.0](http://creativecommons.org/licenses/by/3.0/ "Creative Common's Attribution 3.0 Unported") licence.

You are _not allowed_ to use this source code for pirate purposes.

This software is provided "as-is" and it comes with no warranties.

SDK versions
------------

Minimum: 8

Target: 17

Android UDP multicast text messenger library
--------------------------------------------

This library aims to send simple text messages to a multicast group just joining a WiFi network (which allows UDP packets transmission).

The message group is identified by a TAG, which is being set when the class is being constructed. It's up to the developer to analyse the packet and do whatever he/she likes.

Setup
-----

1. Include the library in your Android project
2. Extend the UDPMessenger class implementing the abstract getIncomingMessageAnalyseRunnable method. The message to analyse is incomingMessage (type: Message)
3. Instantiate an object of the extended UDPMessenger type (now for reference: myExtClassObj).

Sending messages
----------------

`myExtClassObj.sendMessage("my message here")`

Receiving messages
------------------

1. start the receiver: `myExtClassObj.startMessageReceiver()`
2. analyse the Message incomingMessage in the implemented Runnable getIncomingMessageAnalyseRunnable. Note that this runnable will be run in the main Looper.

Message class methods
---------------------

* String getTag(): gets the TAG used to identify the multicast group
* String getMessage(): gets the message contained in the packet
* long getEpochTime(): gets the time in epoch format of the message
* InetAddress getSrcIp(): may be null, the address from which the packet is being sent from.
* String toString(): "TAG EPOCH MESSAGE"
