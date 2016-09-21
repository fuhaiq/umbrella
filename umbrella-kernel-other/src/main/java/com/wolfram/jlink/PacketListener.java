//////////////////////////////////////////////////////////////////////////////////////
//
//   J/Link source code (c) 1999-2000, Wolfram Research, Inc. All rights reserved.
//
//   Use is governed by the terms of the J/Link license agreement, which can be found at
//   www.wolfram.com/solutions/mathlink/jlink.
//
//   Author: Todd Gayley
//
//////////////////////////////////////////////////////////////////////////////////////

package com.wolfram.jlink;

/**
 * The listener interface for receiving PacketArrivedEvents.
 * <p>
 * Objects of classes that implement this interface can be registered with a
 * KernelLink to receive notifications when packets arrive on the link. For each
 * packet that arrives, the packetArrived() method will be invoked and passed a
 * PacketArrivedEvent that describes the packet and lets you read its contents.
 * Your packetArrived() method can consume or ignore the packet without affecting
 * the internal packet loop in any way. You won't interfere with anything whether you read
 * none, some, or all of the packet contents.
 * <p>
 * Here is a trivial packetArrived() method, whichs only looks for TextPackets so that it
 * can write their contents to System.out:
 * <pre>
 *     public boolean packetArrived(PacketArrivedEvent evt) throws MathLinkException {
 *         if (evt.getPktType() == MathLink.TEXTPKT) {
 *             KernelLink ml = (KernelLink) evt.getSource();
 *             System.out.println(ml.getString()); 
 *         }
 *         return true;
 *     }</pre>
 * 
 * At the point that packetArrived is called, the packet has already been &quot;opened&quot;
 * with nextPacket(), so your code can begin reading the packet contents immediately.
 * <p>
 * Very advanced programmers can optionally indicate that the internal packet loop
 * should not see the packet. This is done by returning false from packetArrived().
 * 
 * @see PacketArrivedEvent
 * @see PacketPrinter
 * @see KernelLink
 */

public interface PacketListener extends java.util.EventListener {

	/**
	 * Called when a packet arrives.
	 * <p>
	 * Your implementation can read some or all of the packet contents, or ignore it
	 * completely, without affecting the internal packet loop. In other words, you do
	 * not need to make sure that you fully read the contents.
	 * <p>
	 * Return true in normal circumstances. Programmers with special knowledge of the internal
	 * workings of the packet loop can choose to return false to indicate that this packet
	 * should not be seen by any additional PacketListeners or J/Link's internal packet handling.
	 * 
	 * @param evt the PacketArrivedEvent
	 * @return true
	 * @exception com.wolfram.jlink.MathLinkException
	 */

	public boolean packetArrived(PacketArrivedEvent evt) throws MathLinkException;
}
