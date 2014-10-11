//////////////////////////////////////////////////////////////////////////////////////
//
//   J/Link source code (c) 1999-2001, Wolfram Research, Inc. All rights reserved.
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
import java.math.*;

/**
 * MathInvocationHandler can be used to implement a Java interface with Mathematica code. It uses
 * the Dynamic Proxy feature introduced in Java 1.3. This class is often used from Mathematica, where
 * it is an internal implementation detail of the ImplementInterface function. You can also use it
 * in Java programs, however. Consult the documentation for the Dynamic Proxy feature of Java for more
 * information. You use MathInvocationHandler in the same way as any class that implements InvocationHandler.
 * Use the setHandler() method or one of the constructors to specify the mapping of method names in the Java
 * interface being implemented to the Mathematica functions that will be called. The Mathematica function
 * will be called with the same arguments that the Java method is passed.
 * <p>
 * You must have called the KernelLink method enableObjectReferences() before using a MathInvocationHandler.
 */

public class MathInvocationHandler implements InvocationHandler {

	private Hashtable handlers = new Hashtable(20);
	private KernelLink ml;
	
	
	// From Sun's Proxy docs. An optimization to acquire these methods ahead of time. These three methods
	// in java.lang.Object are forwarded to the invoke() method for some reason, so we need to handle them
	// sensibly.
	private static Method hashCodeMethod;
	private static Method equalsMethod;
	private static Method toStringMethod;
	static {
		try {
			hashCodeMethod = Object.class.getMethod("hashCode", null);
			equalsMethod = Object.class.getMethod("equals", new Class[] { Object.class });
			toStringMethod = Object.class.getMethod("toString", null);
		} catch (NoSuchMethodException e) {
			throw new NoSuchMethodError(e.getMessage());
		}
	}

	
	/**
	 * The constructor that is called from Mathematica.
	 */
	
	public MathInvocationHandler() {
		this((KernelLink) null);
	}

	/**
	 * You must use this constructor when using this class in a Java program,
	 * because you need to specify the KernelLink that will be used.
	 * 
	 * @param ml The link to which computations will be sent.
	 */
	
	public MathInvocationHandler(KernelLink ml) {
		this.ml = ml;
	}

	/**
	 * This form of the constructor lets you skip having
	 * to make a series of setHandler() calls. Use this constructor from Mathematica code only.
	 * 
	 * @param handlers An array of {meth, func} pairs associating methods in the implemented
	 * interface with Mathematica functions.
	 */
	
	public MathInvocationHandler(String[][] handlers) {
		this(null, handlers);
	}
	
	
	/**
	 * This form of the constructor lets you skip having
	 * to make a series of setHandler() calls. Use this constructor from Java code, where
	 * you need to specify the link to be used.
	 * 
	 * @param handlers An array of {meth, func} pairs associating methods in the implemented
	 * interface with Mathematica functions.
	 */
	
	public MathInvocationHandler(KernelLink ml, String[][] handlers) {
		
		this(ml);
		for (int i = 0; i < handlers.length; i++) {
			setHandler(handlers[i][0], handlers[i][1]);
		}
	}
	
	
	/**
	 * Associates the specified Mathematica function with the specified method
	 * from the any of the interfaces implemented by the Proxy class using this MathInvocationHandler.
	 * 
	 * @param meth The method in the interface (for example, "actionPerformed").
	 * @param func The Mathematica function to execute.
	 */

	public void setHandler(String meth, String func) {
		handlers.put(meth, func);
	}
	

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		
		Object result = null;
		String methName = method.getName();
		Class retType = method.getReturnType();
		
		// These three methods called on the Proxy are routed to this invocation handler.
		// Code is from Sun's Proxy docs.
		if (method.getDeclaringClass() == Object.class) {
			if (method.equals(hashCodeMethod)) {
				return proxyHashCode(proxy);
			} else if (method.equals(equalsMethod)) {
				return proxyEquals(proxy, args[0]);
			} else if (method.equals(toStringMethod)) {
				return proxyToString(proxy);
			}
		}

		String mathFunc = (String) handlers.get(methName);
		// It is a debatable design decision whether to require that all methods in the interface be implemented.
		// For now, make it optional. For methods that return something and that are not implemented, an exception
		// will be thrown by the invocation machinery if we return null here.
		if (mathFunc == null)
			return null;

		int numArgs = args != null ? args.length : 0;
		// Calls to Mathematica that _originate_ in Java (meaning that they are not part of a chain of calls
		// that includes a call from Mathematica into Java) must be preceded by a call to requestTransaction().
		// Typically, such calls are executed on the UI thread and originate from user actions on the Java side (like
		// clicking a button).
		KernelLink linkToUse = ml != null ? ml : StdLink.getLink();
		StdLink.requestTransaction();
		// We need to snyc on ml because the Reader thread may also trying to use the link. We must be sure we complete
		// an entire transaction before giving up control of the link.
		synchronized (linkToUse) {
			try {
				// Use this method to accommodate symbolic function heads and also exprs like pure functions.
			    linkToUse.putFunction("EvaluatePacket", 1);
			    linkToUse.putNext(MathLink.MLTKFUNC);
			    linkToUse.putArgCount(numArgs);
			    linkToUse.putFunction("ToExpression", 1);
			    linkToUse.put(mathFunc);
				for (int i = 0; i < numArgs; i++)
				    linkToUse.put(args[i]);
				linkToUse.endPacket();
				linkToUse.waitForAnswer();
				if (retType.equals(Expr.class)) {
					result = linkToUse.getExpr();
				} else {
					switch (linkToUse.getNext()) {
						case MathLink.MLTKINT:
							result = readAsInt(retType, linkToUse);
							break;
						case MathLink.MLTKREAL:
							result = readAsReal(retType, linkToUse);
							break;
						case MathLink.MLTKSTR:
							result = linkToUse.getString();
							break;
						case MathLink.MLTKSYM:
							result = linkToUse.getSymbol();
							if (result.equals("Null"))
								result = null;
							break;
						case MathLink.MLTKFUNC:
							result = linkToUse.getComplex();
							break;
						case KernelLink.MLTKOBJECT:
							result = linkToUse.getObject();
							break;
					}
				}
			} catch (MathLinkException exc) {
				if (MathLinkImpl.DEBUGLEVEL > 0) System.err.println("MathLinkException in MathInvocationHandler.invoke: " + linkToUse.errorMessage());
				linkToUse.clearError();
				linkToUse.newPacket(); // Not necessarily a sufficient cleanup, but what else to do?
				throw exc;   // Re-throw to be caught as an UndeclaredThrowableException by Proxy.
			}
		}
		return result;
	}

	
	private Object readAsReal(Class retType, KernelLink ml) throws MathLinkException {
		
		if (retType.equals(double.class)) {
			return new Double(ml.getDouble());
		} else if (retType.equals(float.class)) {
			return new Float((float) ml.getDouble());
		} else if (retType.equals(BigDecimal.class)) {
			return Utils.bigDecimalFromString(ml.getString());
		} else {
			return null;
		}
	}
	
	private Object readAsInt(Class retType, KernelLink ml) throws MathLinkException {
		
		if (retType.equals(BigInteger.class)) {
			return new BigInteger(ml.getString());
		} else {
			long i = ml.getLongInteger();
			if (retType.equals(char.class)) {
				if (i >= Character.MIN_VALUE && i <= Character.MAX_VALUE)
					return new Character((char) i);
				else
					return new Long(i);  // Let it throw on the narrowing conversion.
			} else if (retType.equals(byte.class)) {
				return new Byte((byte) i);
			} else if (retType.equals(short.class)) {
				return new Short((short) i);
            } else if (retType.equals(int.class)) {
                return new Integer((int) i);
            } else if (retType.equals(float.class)) {
                return new Float((float) i);
            } else if (retType.equals(double.class)) {
                return new Double((double) i);
			} else {
				return new Long(i);
			}
		}
	}
	
	
	// From Sun's Proxy docs:
	
	protected Integer proxyHashCode(Object proxy) {
		return new Integer(System.identityHashCode(proxy));
	}

	protected Boolean proxyEquals(Object proxy, Object other) {
		return (proxy == other ? Boolean.TRUE : Boolean.FALSE);
	}

	protected String proxyToString(Object proxy) {
		return proxy.getClass().getName() + '@' + Integer.toHexString(proxy.hashCode());
	}
	
}