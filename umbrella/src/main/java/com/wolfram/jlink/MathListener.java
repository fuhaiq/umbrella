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

import java.util.*;
import java.lang.reflect.*;

/**
 * This abstract class is the parent class of all the MathXXXListener classes, which let you trigger a call
 * into Mathematica on the occurrence of a particular event. Developers are encouraged to subclass it for
 * their own "listener" classes that need to call into Mathematica. It handles virtually all of the complexity
 * in creating a listener-type class, including the interaction with Mathematica. Examine the source code
 * for any of the MathXXXListener classes to see how you can subclass this class for your own needs.
 */

public abstract class MathListener implements EventListener {

	private Hashtable handlers;
	private KernelLink usersLink;

	/**
	 * The constructor that is typically called from Mathematica.
	 */
	
	public MathListener() {
		// Let the link be decided at run time (it could be StdLink or UILink).
		this((KernelLink)null);
	}

	/**
	 * Call this version only when you know for sure what link should be used
	 * for UI-triggered computations. This is typically only done in a standalone
	 * Java program, where there is only one link. It is actually preferable in that
	 * case to set StdLink to your link by calling StdLink.setLink() and then use
	 * the MathListener constructor that takes no args, as it will pull the link to
	 * use out of StdLink.
	 * 
	 * @param ml The link to which computations will be sent when events notifications arrive.
	 */
	
	public MathListener(KernelLink ml) {
		usersLink = ml;
		setupEvents();
	}


	/**
	 * This form of the constructor lets you skip having
	 * to make a series of setHandler() calls. Use this constructor from Mathematica code only.
	 * 
	 * @param handlers An array of {meth, func} pairs associating methods in the derived class
	 * with Mathematica functions.
	 */
	
	public MathListener(String[][] handlers) {
		this();
		for (int i = 0; i < handlers.length; i++)	{
			setHandler(handlers[i][0], handlers[i][1]);
		}
	}


	/**
	 * Associates the specified Mathematica function with the specified method
	 * from the any of the "listener" interfaces implemented by the class.
	 * 
	 * @param meth The method in the "listener" interface (for example, "actionPerformed").
	 * @param func The Mathematica function to execute.
	 * @return true if the specified method exists in the interface, false otherwise.
	 */

	public boolean setHandler(String meth, String func) {

		if (handlers.containsKey(meth)) {
			handlers.put(meth, func);
			return true;
		} else {
			// There is no event handler method of this name in the class. This message is defined in JLink.m.
			if (getLink() != null)
				getLink().message("Java::nohndlr", new String[]{meth, getClass().getName()});
			return false;
		}
	}
	

	/**
	 * Derived classes call this method from their event handler methods. It performs the call into Mathematica,
	 * invoking the Mathematica function associated (via a previous call to setHandler()) with the given event
	 * handler method, and passing it the arguments supplied. The return value is an Expr giving the result of the
	 * computation. Don't forget to call dispose() on this Expr after you are done with it.
	 * This method will block until the kernel is in a state
	 * where it is receptive to calls that _originate_ in Java (meaning that they are not part of a chain of calls
	 * that includes a call from Mathematica into Java). Such computations typically
	 * arise on the user-interface thread, as a result of some user action (like clicking a button). You need to make
	 * sure that the kernel can handle such computations before calling this method. Any of the following is sufficient
	 * to guarantee that callMathHandler() will not block:
	 * <pre>
	 * - the kernel is running DoModal[]
	 * - kernel sharing (via ShareKernel[] or ShareFrontEnd[]) is turned on, and the kernel is not busy with some other computation
	 * - the kernel executes ServiceJava[]
	 * - somewhere on the stack is a call from Mathematica into Java
	 * - the program is standalone, not being scripted from a Mathematica session</pre>
	 * 
	 * @param meth the name of the event handler method that is being invoked (e.g., "actionPerformed", "windowClosing", "mousePressed", etc.)
	 * @param args the arguments to be passed to the Mathematica handler function. The arguments will be sent individually,
	 * not as a list. To pass primitive types like integers, wrap them in their corresponding wrapper classes
	 * (e.g., new Integer(i)).
	 * @return an Expr giving the result of the computation.
	 */
	 
	protected Expr callMathHandler(String meth, Object[] args) {
		
		return callMathHandler0(true, meth, args);
	}
	
	
	/**
	 * Just like callMathHandler(), except it discards the result of the computation. This is a convenience function for the
	 * common case when you are not interested in the Expr result of the call to the handler function in Mathematica.
	 *
	 * @param meth the name of the event handler method that is being invoked (e.g., "actionPerformed", "windowClosing", "mousePressed", etc.)
	 * @param args the arguments to be passed to the Mathematica handler function. The arguments will be sent individually,
	 * not as a list. To pass primitive types like integers, wrap them in their corresponding wrapper classes
	 * (e.g., new Integer(i)).
	 * @see #callMathHandler(String, Object[])
	 * @since J/Link version 2.0
	 */
	 
	protected void callVoidMathHandler(String meth, Object[] args) {

		callMathHandler0(false, meth, args);
	}
	
		
	/**
	 * Gives the link that will be used by this MathListener for computations. Note that the link
	 * can change over time, depending on whether the mainLink or uiLink is to be used.
	 * If the programmer has manually set a link in the ctor, then that link will always be returned.
	 */
	 
	protected KernelLink getLink() {
		return usersLink != null ? usersLink : StdLink.getLink();
	}

	/**
	 * Allows subclasses to get the name of the Mathematica function assigned to handle the given method.
	 * This would be used if a subclass wanted to call the handler method manually, instead of delegating to
	 * the callMathHandler() method.
	 * @since J/Link version 2.0
	 */
	 
	protected String getHandler(String methName) {
		return (String) handlers.get(methName);
	}


	//////////////////////////////////////  Private  ////////////////////////////////////////
	
	// Fills the handlers hashtable with the names of all methods in this class that can be associated with a call to Mathematica.
	private void setupEvents() {
		
		handlers = new Hashtable(10);  // Small capacity, as few EventListener classes will have a large number of event handlers.
		try {
			// Place (methodName, "") pairs into handlers table for all methods in this class. There is no rigorous way
			// to tell whether a method defined in the class is actually an event handler, but it doesn't matter if we put
			// more methods into the table than are actually relevant. It may not be worth it, but we avoid cluttering
			// the table with methods that are inherited from Object.
			Method[] meths = getClass().getMethods();
			Method[] objectMethods = Object.class.getMethods();
			String[] objectMethodNames = new String[objectMethods.length];
			for (int i = 0; i < objectMethods.length; i++)
				objectMethodNames[i] = objectMethods[i].getName();
			for (int i = 0; i < meths.length; i++) {
				boolean belongs = true;
				String name = meths[i].getName();
				for (int j = 0; j < objectMethodNames.length; j++) {
					if (name.equals(objectMethodNames[j])) {
						belongs = false;
						break;
					}
				}
				if (belongs)
					handlers.put(name, "");
			}
		} catch (SecurityException e) {
			System.err.println("Warning: MathListener cannot establish event handler callbacks: " + e.toString());
		}
	}


	private Expr callMathHandler0(boolean wantResult, String meth, Object[] args) {
		
		KernelLink ml = getLink();
		
		// Bail out right away if there is no link. This lets callMathHandler() be a no-op when called in Java programs that
		// do not use Mathematica, or use it only some of the time.
		if (ml == null)
			return null;
	
		String func = (String) handlers.get(meth);
		if (func == null) {
			System.err.println("Warning: calling MathListener.callMathHandler() with a method name that does not exist in the class. " +
										"Method name is " + meth + ". Class is " + getClass().getName());
			return null;
		}

		Expr result = null;
		
		// func will be "" if user has not associated a Mathematica function with this event.
		if (!func.equals("")) {
			int numArgs = args != null ? args.length : 0;
			// Calls to Mathematica that _originate_ in Java (meaning that they are not part of a chain of calls
			// that includes a call from Mathematica into Java) must be preceded by a call to requestTransaction().
			// Typically, such calls are executed on the UI thread and originate from user actions on the Java side (like
			// clicking a button).
			if (ml.equals(StdLink.getLink()))
				StdLink.requestTransaction();
			// We need to snyc on ml because the Reader thread is also trying to use the link. We must be sure we complete
			// an entire transaction before giving up control of the link.
			synchronized (ml) {
				try {
					// Use this method to accommodate symbolic function heads and also exprs like pure functions.
					ml.putFunction("EvaluatePacket", 1);
					ml.putNext(MathLink.MLTKFUNC);
					ml.putArgCount(numArgs);
					ml.putFunction("ToExpression", 1);
					ml.put(func);
					for (int i = 0; i < numArgs; i++)
						ml.put(args[i]);
					ml.endPacket();
					if (wantResult) {
						ml.waitForAnswer();
						result = ml.getExpr();
					} else {
						ml.discardAnswer();
					}
				} catch (MathLinkException exc) {
					if (MathLinkImpl.DEBUGLEVEL > 0) System.err.println("MathLinkException in callMathHandler: " + ml.errorMessage());
					ml.clearError();
					ml.newPacket(); // Not necessarily a sufficient cleanup, but what else to do?
				}
			}
		}
		return result;
	}

}