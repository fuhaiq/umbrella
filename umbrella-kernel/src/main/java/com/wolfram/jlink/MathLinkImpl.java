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

import java.lang.reflect.*;
import java.util.*;


// MathLinkImpl is intended to be the parent class for a MathLink implementation. It handles a lot of
// logic that is independent of the details of the implementations of the low-level get/put methods.
// A lot of the logic that was formerly here has been moved down into MathLinkImplBase. See the comments
// in that file for the motivation for the split.

// More things could probably be moved here (or to MathLinkImplBase) from NativeLink (e.g., handling of NaN,
// infinity when sending doubles).

public abstract class MathLinkImpl extends MathLinkImplBase {


	protected Method userYielder;
	protected Object yielderObject;
	protected Vector userMsgHandlers = new Vector(2, 1);
	private long timeoutMillis = 0;
	private long startConnectTime = 0;
	private boolean connectTimeoutExpired;
	
	// See comments below in packetListener support section.
	protected Vector packetListeners = new Vector(2, 2);
	private Object packetListenerLock = new Object();
	
	protected Object yieldFunctionLock = new Object();
	
	
	// Document that this kills the user's yielder, if set.
	public synchronized void connect(long timeoutMillis) throws MathLinkException {

		setYieldFunction(null, this, "connectTimeoutYielder");
		this.timeoutMillis = timeoutMillis;
		connectTimeoutExpired = false;
		startConnectTime = System.currentTimeMillis();
		try {
			connect();
		} finally {
			// Clears the C-to-Java callback completely.
			setYieldFunction(null, null, null);
		}
		// If the connectTimeoutYielder ever returns true, then either the link will die and the
		// connect() call above will throw a fatal MathLinkException, or connect() will fail but not
		// throw because the "deferred connection" error that is returned is not deemed to be
		// exception-worthy in general. Here, we want to throw an exception on that error, so we
		// make up our own.
		if (connectTimeoutExpired)
			throw new MathLinkException(MLE_CONNECT_TIMEOUT);
	}
	

	/*****************************  Yielding and Messages  *********************************/

	// YieldFunction and MessageHandler stuff here. Note that implementing link classes
	// will likely need to override some of these methods and provide some implementation of their own.
	// What we can do here is manage the Java-level portions (like handling the list of message
	// handlers, or the user-supplied Java yield function). See NativeLink for an example of
	// what a derived class might need to do.

	// The signature of the meth must be (V)Z. Can be static; pass null as target. Can pass null
	// for cls if target is provided.
	public boolean setYieldFunction(Class cls, Object target, String methName) {

		// Convenient to set a yielder while a thread is blocking in MathLink. Thus, this method is not synchronized.
		// Instead, we synch on an object that is specific to the yielder-handling data structures.
		synchronized (yieldFunctionLock) {
			userYielder = null;
			yielderObject = null;
			if (methName != null) {
				// Note that passing methName of null clears yield function.
				Method meth = null;
				Class[] paramTypes = {};
				Class targetClass = cls != null ? cls : target.getClass();
				try {
					meth = targetClass.getMethod(methName, paramTypes);
 				} catch (Exception e) { // e.g. NoSuchMethodException
 					return false;
 				}
				// This sets the method that will be called in yielderCallback.
				userYielder = meth;
				yielderObject = target;
				// If JLink.jar is in jre/lib/ext and the class that contains the yield function is loaded from the
				// classpath, then it will throw an IllegalAccessException when invoked. Need to make it accessible.
				// This is another annoyance of the Java 2 "extensions" mechanism. [Note for 2.0: JLink.jar should NEVER
				// be in ext directory, but we'll leave this code here as it might be necessary in other circumstances]
				if (!userYielder.isAccessible()) {
					try {
						userYielder.setAccessible(true);
					} catch (SecurityException e) {
						// Using AccessibleObject is governed by the security manager. These calls could fail.
						// Not much to do if that happens except issue a warning.
						System.err.println("J/Link warning: The yield function " + methName + " might not be called due to a security restriction. " +
													"See the documentation for the class java.lang.reflect.ReflectPermission. This problem might go away " +
													"if JLink.jar is loaded from the classpath instead of the jre/lib/ext directory.");
						e.printStackTrace();
					}
				}
			}
		}
		return true;
	}


	// The signature of the meth must be (II)V. Can be static; pass null as target. Can pass null
	// for cls if target is provided.
	public synchronized boolean addMessageHandler(Class cls, Object target, String methName) {

		Method meth = null;
		Class[] paramTypes = {int.class, int.class};
		Class targetClass = cls != null ? cls : target.getClass();
		try {
			meth = targetClass.getMethod(methName, paramTypes);
		} catch (Exception e) { // e.g. NoSuchMethodException
			if (DEBUGLEVEL > 0) System.err.println("Exception in addMessageHandler");
			return false;
		}
		// This sets the method that will be called in messageCallback.
		// First, check to see if meth is already in there.
		for (Enumeration e = userMsgHandlers.elements(); e.hasMoreElements(); ) {
			MsgHandlerRecord msgHandlerRec = (MsgHandlerRecord) e.nextElement();
			if (msgHandlerRec.meth.equals(meth))
				return true;
		}
		userMsgHandlers.addElement(new MsgHandlerRecord(target, meth, methName));
		// If JLink.jar is in jre/lib/ext and the class that contains the message function is loaded from the
		// classpath, then it will throw an IllegalAccessException when invoked. Need to make it accessible.
		// This is another annoyance of the Java 2 "extensions" mechanism".
		if (!meth.isAccessible()) {
			try {
				meth.setAccessible(true);
			} catch (SecurityException e) {
				// Using AccessibleObject is governed by the security manager. These calls could fail.
				// Not much to do if that happens except issue a warning.
				System.err.println("J/Link warning: The message handler " + methName + " might not be called due to a security restriction. " +
											"See the documentation for the class java.lang.reflect.ReflectPermission. This problem might go away " +
											"if JLink.jar is loaded from the classpath instead of the jre/lib/ext directory.");
				e.printStackTrace();
			}
		}
		return true;
	}

	public synchronized boolean removeMessageHandler(String methName) {
		
		for (int i = 0; i < userMsgHandlers.size(); i++) {
			if (((MsgHandlerRecord) userMsgHandlers.elementAt(i)).methName.equals(methName)) {
				userMsgHandlers.removeElementAt(i);
				return true;
			}
		}
		return false;
	}
	
	// Note this is protected. Derived classes will provide some protocol-specific
	// means of calling this on message callbacks. That's all they need to do.
	protected void messageCallback(int message, int n) {

		Object[] args = {new Integer(message), new Integer(n)};
		if (DEBUGLEVEL > 1) System.err.println("in messageCallback");
		for (int i = 0; i < userMsgHandlers.size(); i++) {
			try {
				MsgHandlerRecord rec = (MsgHandlerRecord) userMsgHandlers.elementAt(i);
				rec.meth.invoke(rec.target, args);
			} catch (Throwable t) {
				if (DEBUGLEVEL > 0) System.err.println("Caught exception calling user msgHandler: " + t.toString());
			}
		}
	}
	
	protected boolean yielderCallback() {

		synchronized (yieldFunctionLock) {
			Object res = null;
			if (userYielder != null) {
				try {
					res = userYielder.invoke(yielderObject, null);
				} catch (Throwable t) {
					if (DEBUGLEVEL > 0) System.err.println("Caught exception calling user yielder: " + t.toString());
				}
			}
			if (res instanceof Boolean)
				return ((Boolean)res).booleanValue();
			else
				return false;
		}
	}

	// The yield function installed during connect(long timeout). Only used during the lifetime
	// of a single call to that method.
	public boolean connectTimeoutYielder() {
		connectTimeoutExpired = System.currentTimeMillis() > startConnectTime + timeoutMillis;
		return connectTimeoutExpired;
	}
	
	
	/*****************************  PacketListener support  *********************************/

	// Note that the PacketListener support methods are declared in KernelLink, not MathLink.
	// But it is convenient to put their implementation here. For MathLink implementation classes
	// that subclass this, no harm done, since the methods are not visible through those object's
	// types (MathLink). KernelLink classes can share this implementation without us
	// having to create a new abstract class slightly fatter than MathLinkImpl that implements
	// KernelLink but only adds these three methods. Design-wise, KernelLinkImpl is really too fat
	// to have these methods in it. There are some KernelLink implementations that want everything
	// in MathLinkImpl, but not everything in KernelLinkImpl (such as all the support for passing
	// object references). An example is KernelLink_HTTP, which needs MathLinkImpl + packetHandler
	// only. Rather than create a new abstract class between MathLinkImpl and KernelLinkImpl, I'll
	// just toss these here. Note one hack as a consequence--the cast to KernelLink in the
	// PacketArrivedEvent constructor.
	
	public void addPacketListener(PacketListener listener) {
		
		synchronized(packetListenerLock) {
			if (!packetListeners.contains(listener))
				packetListeners.addElement(listener);
		}
	}
	
	public void removePacketListener(PacketListener listener) {
		
		synchronized(packetListenerLock) {
			if (packetListeners.contains(listener))
				packetListeners.removeElement(listener);
		}
	}
	
	// Must leave link at same spot as when it was entered, swallow any MathLinkExceptions,
	// and clear any MathLink error state. Return false to indicate that processing by further packet
	// listeners and default mechanisms should not take place.
	public synchronized boolean notifyPacketListeners(int pkt) {

		if (packetListeners.size() == 0)
			return true;
	
		boolean allowFurtherProcessing = true;
		// See the comment block above. It will be safe to cast to KernelLink because this method will only
		// be accessed by classes that are KernelLinks.
		PacketArrivedEvent evt = new PacketArrivedEvent((KernelLink) this, pkt);
		Vector v;
		synchronized (packetListenerLock) {
			v = (Vector) packetListeners.clone();
		}
		long mark = 0;
		try {
			boolean listenerResult = true;
			mark = createMark();
			int len = v.size();
			for (int i = 0; i < len && allowFurtherProcessing; i++) {
				try {
					listenerResult = ((PacketListener) v.elementAt(i)).packetArrived(evt);
					allowFurtherProcessing = allowFurtherProcessing && listenerResult;
				} catch (MathLinkException e) {
					clearError();
				} finally {
					seekMark(mark);
				}
			}
		} catch (MathLinkException e) {
			// Just for the createMark call.
			clearError();
		} finally {
			if (mark != 0)
				destroyMark(mark);
		}
		return allowFurtherProcessing;
	}

/*****************************  Complex  ********************************/

	protected Class complexClass;
	// some cached methods.
	protected Constructor complexCtor;
	protected Method complexReMethod;
	protected Method complexImMethod;

	public synchronized boolean setComplexClass(Class cls) {
		
 		Constructor newComplexCtor = null;
 		Method newComplexReMethod = null;
 		Method newComplexImMethod = null;
	
		if (cls != null) {
			try {
 				Class[] argTypes = {double.class, double.class};
 				newComplexCtor = cls.getDeclaredConstructor(argTypes);
 				newComplexReMethod = cls.getDeclaredMethod("re", null);
 				newComplexImMethod = cls.getDeclaredMethod("im", null);
 			} catch (Exception e) {
				return false;
 			}
 		}
		complexClass = cls;
		complexCtor = newComplexCtor;
		complexReMethod = newComplexReMethod;
		complexImMethod = newComplexImMethod;
		return true;
	}
 
	public synchronized Class getComplexClass() {
		return complexClass;
	}
 
	protected synchronized Object constructComplex(double re, double im) {

		Object[] args = {new Double(re), new Double(im)};
		try {
			return complexCtor.newInstance(args);
		} catch (Exception e) {
			return null;
		}
	}
	
	protected synchronized double getRealPart(Object complex) throws Exception {
		return ((Double) complexReMethod.invoke(complex, null)).doubleValue();
	}
	
	protected synchronized double getImaginaryPart(Object complex) throws Exception {
		return ((Double) complexImMethod.invoke(complex, null)).doubleValue();
	}

}


class MsgHandlerRecord {
	
	String methName;
	Method meth;
	Object target;
	
	MsgHandlerRecord(Object target, Method meth, String methName) {
		this.meth = meth;
		this.target = target;
		this.methName = methName;
	}
}
