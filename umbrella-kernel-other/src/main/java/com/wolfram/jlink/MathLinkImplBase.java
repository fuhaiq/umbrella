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


// MathLinkImplBase is intended to hold all state-independent implementation of MathLink logic. This implies that
// there will be no non-static fields in this class.  Examples of things that cannot go here are some aspects
// of Complex handling (requires holding state in the form of the complex class) and yielder/message stuff
// (requires holding yield/msg function references).
//
// The motivation for splitting up the original MathLinkImpl into a state-independent MathLinkImplBase and a
// state-dependent MathLinkImpl was that it was felt that there might be future implementations of MathLink
// that would handle state in different ways and thus would want to be able to inherit from MathLinkImplBase and
// be guaranteed that no references to state variables would occur in any of its logic. This reasoning now seems less
// relevant, but perhaps some maintainability benefits will accrue with the split.


public abstract class MathLinkImplBase implements MathLink {


	static final int DEBUGLEVEL = 0;

    // For sending BigDecimals.
    private static final byte[] decimalPointString = new byte[]{'.'};
    private static final byte[] expString = new byte[]{'*', '^'};
    private static final byte[] tickString = new byte[]{'`'};


	public synchronized void activate() throws MathLinkException {
		connect();
	}

	public synchronized boolean getBoolean() throws MathLinkException {
		return getSymbol().equals("True");
	}
	public synchronized void putData(byte[] data) throws MathLinkException {
		putData(data, data.length);
	}
	public synchronized boolean[] getBooleanArray1() throws MathLinkException {
		return (boolean[]) getArray(TYPE_BOOLEAN, 1);
	}
	public synchronized boolean[][] getBooleanArray2() throws MathLinkException {
		return (boolean[][]) getArray(TYPE_BOOLEAN, 2);
	}
	public synchronized byte[] getByteArray1() throws MathLinkException {
		return (byte[]) getArray(TYPE_BYTE, 1);
	}
	public synchronized byte[][] getByteArray2() throws MathLinkException {
		return (byte[][]) getArray(TYPE_BYTE, 2);
	}
	public synchronized char[] getCharArray1() throws MathLinkException {
		return (char[]) getArray(TYPE_CHAR, 1);
	}
	public synchronized char[][] getCharArray2() throws MathLinkException {
		return (char[][]) getArray(TYPE_CHAR, 2);
	}
	public synchronized short[] getShortArray1() throws MathLinkException {
		return (short[]) getArray(TYPE_SHORT, 1);
	}
	public synchronized short[][] getShortArray2() throws MathLinkException {
		return (short[][]) getArray(TYPE_SHORT, 2);
	}
	public synchronized int[] getIntArray1() throws MathLinkException {
		return (int[]) getArray(TYPE_INT, 1);
	}
	public synchronized int[][] getIntArray2() throws MathLinkException {
		return (int[][]) getArray(TYPE_INT, 2);
	}
	public synchronized long[] getLongArray1() throws MathLinkException {
		return (long[]) getArray(TYPE_LONG, 1);
	}
	public synchronized long[][] getLongArray2() throws MathLinkException {
		return (long[][]) getArray(TYPE_LONG, 2);
	}
	public synchronized float[] getFloatArray1() throws MathLinkException {
		return (float[]) getArray(TYPE_FLOAT, 1);
	}
	public synchronized float[][] getFloatArray2() throws MathLinkException {
		return (float[][]) getArray(TYPE_FLOAT, 2);
	}
	public synchronized double[] getDoubleArray1() throws MathLinkException {
		return (double[]) getArray(TYPE_DOUBLE, 1);
	}
	public synchronized double[][] getDoubleArray2() throws MathLinkException {
		return (double[][]) getArray(TYPE_DOUBLE, 2);
	}
	public synchronized String[] getStringArray1() throws MathLinkException {
		return (String[]) getArray(TYPE_STRING, 1);
	}
	public synchronized String[][] getStringArray2() throws MathLinkException {
		return (String[][]) getArray(TYPE_STRING, 2);
	}
	public synchronized Object[] getComplexArray1() throws MathLinkException {
		return (Object[]) getArray(TYPE_COMPLEX, 1);
	}
	public synchronized Object[][] getComplexArray2() throws MathLinkException {
		return (Object[][]) getArray(TYPE_COMPLEX, 2);
	}


	public synchronized Expr getExpr() throws MathLinkException {
		return Expr.createFromLink(this);
	}

	public synchronized Expr peekExpr() throws MathLinkException {
		long mark = createMark();
		try {
			return Expr.createFromLink(this);
		} finally {
			seekMark(mark);
			destroyMark(mark);
		}
	}

	/*****************************  Arrays  ********************************/

	// Reusable 1-length String array for head info. If Java had static variables, this would be declared
	// static in the getArraySlices() method.
	private String[] headHolder = new String[1];

	// Can handle here the cases where high-level mathlink work is required. The implementation in
	// derived classes (e.g., NativeLink) should handle other cases (for NativeLink, those are the cases
	// that are handled by a single call into the native library). It is safe to call this implementation
	// from derived classes for depth > 1 arrays (in which case it handles the logic of breaking up the
	// read into slices at the last dimension), or arrays of any depth of types that cannot be more efficiently done
	// by more direct methods (specifically, STRING, BOOLEAN, LONG, BIGDECIMAL, BIGINTEGER, EXPR, COMPLEX).
	public synchronized Object getArray(int type, int depth, String[] heads) throws MathLinkException {

		Object result = null;

		if (depth == 1) {
			MLFunction func = getFunction();
			int i;
			switch (type) {
				case TYPE_BOOLEAN:
					result = Array.newInstance(boolean.class, func.argCount);
					for (i = 0; i < func.argCount; i++) {
						Array.setBoolean(result, i, getBoolean());
					}
					break;
				case TYPE_STRING:
					result = Array.newInstance(String.class, func.argCount);
					for (i = 0; i < func.argCount; i++) {
						Array.set(result, i, getString());
					}
					break;
				case TYPE_LONG:
					result = Array.newInstance(long.class, func.argCount);
					for (i = 0; i < func.argCount; i++) {
						Array.setLong(result, i, getLongInteger());
					}
					break;
				case TYPE_COMPLEX:
					result = Array.newInstance(getComplexClass(), func.argCount);
					for (i = 0; i < func.argCount; i++) {
						Array.set(result, i, getComplex());
					}
					break;
				case TYPE_EXPR:
					result = new Expr[func.argCount];
					for (i = 0; i < func.argCount; i++) {
						Array.set(result, i, getExpr());
					}
					break;
				case TYPE_BIGINTEGER:
					result = new java.math.BigInteger[func.argCount];
					for (i = 0; i < func.argCount; i++) {
						Array.set(result, i, new java.math.BigInteger(getString()));
					}
					break;
				case TYPE_BIGDECIMAL:
					result = new java.math.BigDecimal[func.argCount];
					for (i = 0; i < func.argCount; i++) {
						Array.set(result, i, Utils.bigDecimalFromString(getString()));
					}
					break;
				default:
					if (DEBUGLEVEL > 0) System.err.println("Unexpected type " + type + " in MathLinkImpl.getArray. Should be handled by a subclass.");
			}
			if (heads != null)
				heads[0] = func.name;
		} else {
			result = getArraySlices(type, depth, heads, 0, null);
		}
		return result;
	}

	public synchronized Object getArray(int type, int depth) throws MathLinkException {
		return getArray(type, depth, null);
	}

	// New addition in 4.2. Because of the way array-reading was implemented, it became necessary to introduce a
	// method here that is only publicly defined in the KernelLink interface. It is intended for reading arrays of
	// object refs. This signature must exist for this class to compile, but it will never be called, as only a
	// KernelLinkImpl instance can ever call this, and KernelLinkImpl overrides it.
	protected Object getArray(Class elementType, int depth, String[] heads) throws MathLinkException {
	    throw new UnsupportedOperationException("This method should never be entered; only the superclass version should be called.");
	}

	// This method for reading arrays recursively walks down the levels, calling back to getArray to get the last level
	// (which are 1-D arrays).
	protected Object getArraySlices(int type, int depth, String[] heads, int headsIndex, Class componentClass) throws MathLinkException {

		Object resArray = null;

		if (depth > 1) {
			if (componentClass == null) {
                // The only need to get tricky with ClassLoaders is because the Complex class might
                // be loaded from J/Link extra classpath and thus classes representing arrays of the
                // Complex class need to be loaded by JLinkClassLoader.
                ClassLoader loader = this.getClass().getClassLoader();
				String compClassName = "";
				for (int i = 1; i < depth; i++)
					compClassName = compClassName + "[";
				switch (type) {
					case TYPE_BOOLEAN:
						compClassName = compClassName + "Z";
						break;
					case TYPE_BYTE:
						compClassName = compClassName + "B";
						break;
					case TYPE_CHAR:
						compClassName = compClassName + "C";
						break;
					case TYPE_SHORT:
						compClassName = compClassName + "S";
						break;
					case TYPE_INT:
						compClassName = compClassName + "I";
						break;
					case TYPE_LONG:
						compClassName = compClassName + "J";
						break;
					case TYPE_FLOAT:
						compClassName = compClassName + "F";
						break;
					case TYPE_DOUBLE:
						compClassName = compClassName + "D";
						break;
					case TYPE_STRING:
						compClassName = compClassName + "Ljava.lang.String;";
						break;
					case TYPE_BIGINTEGER:
						compClassName = compClassName + "Ljava.math.BigInteger;";
						break;
					case TYPE_BIGDECIMAL:
						compClassName = compClassName + "Ljava.math.BigDecimal;";
						break;
					case TYPE_EXPR:
						compClassName = compClassName + "Lcom.wolfram.jlink.Expr;";
						break;
					case TYPE_COMPLEX:
						compClassName = compClassName + "L" + getComplexClass().getName() + ";";
                        loader = getComplexClass().getClassLoader();
						break;
					default:
						if (DEBUGLEVEL > 0) System.err.println("Unimplemented type " + type + " in MathLinkImpl.getArraySlices()");
				}
				try {
					componentClass = Class.forName(compClassName, true, loader);
				} catch (ClassNotFoundException e) {
					// Should never happen.
					if (DEBUGLEVEL > 0) System.out.println("Could not find component class in getArraySlices(). " + e.toString());
				}
			}

			MLFunction func = getFunction();
			if (heads != null)
				heads[headsIndex] = func.name;
			int len = func.argCount;
			resArray = Array.newInstance(componentClass, len);
			func = null;  // Help the garbage collector.

			Class subComponentClass = componentClass.getComponentType();

			// The next few lines of commented-out code implement the feature of throwing an exception
			// if heads are not identical across a level. Since the C API MLGetXXXArray functions do not
			// signal an error in such a circumstance, it was decided that the non-native C array types
			// should not signal an error either. This code could be restored if that decision was changed.
			//String headsAtNextLevel = null;
			for (int i = 0; i < len; i++) {
				Array.set(resArray, i, getArraySlices(type, depth - 1, heads, headsIndex + 1, subComponentClass));
				/*
				if (heads != null) {
					if (headsAtNextLevel == null)
						headsAtNextLevel = heads[headsIndex + 1];
					else if (!headsAtNextLevel.equals(heads[headsIndex + 1]))
						throw new MathLinkException(MLE_HEADS_NOT_IDENTICAL);
				}
				*/
			}
		} else {
			// depth == 1. Call back to getArray to do the actual work of reading from the link.
		    // Here is the ugliness of calling the getArray(Class, ...) method that is only publicly
		    // declared in KernelLink. Because we can get down this far on object arrays, we need access
		    // to the API method that allows us to specify the object type. The implementation in this class
		    // does nothing (it throws an exception), but only a KernelLinkImpl instance will ever cause the
		    // branch to be executed, and KernelLinkImpl overrides that method. The if() test here is basically
		    // type == TYPE_OBJECT, without explicitly referring to the KernelLink.TYPE_OBJECT constant.
		    if (type < TYPE_COMPLEX)
		        // is TYPE_OBJECT
		        resArray = getArray(componentClass, 1, heads != null ? headHolder : null);
		    else
		        resArray = getArray(type, 1, heads != null ? headHolder : null);
			if (heads != null)
				heads[headsIndex] = headHolder[0];
		}
		return resArray;
	}


	public synchronized void put(Object obj) throws MathLinkException {

		if (obj == null) {
			// Wonder whether it would be better to have a Mma JavaObject that stood for the null object.
			// I already have an entry (index 0) in the instanceList for null.
			putSymbol("Null");
		} else if (obj instanceof String) {
			putString((String) obj);
		} else if (obj.getClass().isArray()) {
			putArray(obj, null);
		} else if (obj instanceof Expr) {
			((Expr) obj).put(this);
		} else if (getComplexClass() != null && getComplexClass().isInstance(obj)) {
			putComplex(obj);
		} else if (obj instanceof Number) {
			if (obj instanceof Integer || obj instanceof Short || obj instanceof Byte) {
				put(((Number) obj).intValue());
			} else if (obj instanceof Double || obj instanceof Float) {
				put(((Number) obj).doubleValue());
			} else if (obj instanceof Long || obj instanceof java.math.BigInteger) {
				byte[] data = obj.toString().getBytes();
				putNext(MLTKINT);
				putSize(data.length);
				putData(data, data.length);
			} else if (obj instanceof java.math.BigDecimal) {
                java.math.BigDecimal bd = (java.math.BigDecimal) obj;
                String scale = Integer.toString(-bd.scale());
                String unscaledValue = bd.unscaledValue().toString();
                putNext(MLTKREAL);
                // Send str as unscaled.*^scale.
                putSize(unscaledValue.length() + 3 + scale.length());
                putData(unscaledValue.getBytes());
                putData(decimalPointString);
                putData(expString);
                putData(scale.getBytes());
                
                /* Another experimental change. See notes in Utils.bigDecimalFromString() about attempts to 
                 * preserve M precision in BigDecimals.
                int precision = bd.precision();
                String precStr = precision > 0 ? Integer.toString(precision) : null;
                putNext(MLTKREAL);
                // Send str as unscaled.*^scale, or unscaled.`prec*^scale.
                putSize(unscaledValue.length() + (precStr != null ? (1 + precStr.length()) : 0) + 3 + scale.length());
                putData(unscaledValue.getBytes());
                putData(decimalPointString);
                if (precStr != null) {
                    putData(tickString);
                    putData(precStr.getBytes());
                }
                putData(expString);
                putData(scale.getBytes());
                */
                
  /*
                // ************  experimental, possibly use in the future
				String s = obj.toString();
				byte[] data = s.getBytes();
				// We want to preserve the precision of the number if it is less than machine precision.
				// In other words, if the bigdecimal is 1234.5678, then we don't want to just send it
				// so it is read as a M machine-precision real, because that puts extra gibberish digits
				// to pad out to machine precision. Therefore, we will send it as 1234.5678`8.
				boolean hasMinus = s.startsWith("-");
				boolean hasDecimal = s.indexOf('.') != -1;
				// Next is for numbers that are 0.xxx or -0.xxx.
				boolean startsWithZero = hasDecimal && (s.startsWith("0") || s.startsWith("-0"));
				int numDigits = data.length - (hasMinus ? 1 : 0) - (hasDecimal ? 1 : 0) - (startsWithZero ? 1 : 0);
				// I do not understand why the +1 is required in the next line. It appears that sending
				// "0.1234`4" using MLPutData does not create the same number as 0.1234`4 entered directly
				// into Mathematica.
				byte[] precisionSpec = Integer.toString(numDigits).getBytes();
				putNext(MLTKREAL);
				putSize(data.length + (hasDecimal ? 0 : 1) + 1 + precisionSpec.length);
				putData(data, data.length);
				// Need to stick a decimal point on end if there isn't one.
				if (!hasDecimal)
					putData(new byte[]{(byte)46}, 1);
				putData(new byte[]{(byte) '`'}, 1);
				putData(precisionSpec, precisionSpec.length);
  */
			} else {
				// User-defined Number subclass. Determine if the toString() form of the number looks like
			    // M can handle it. If so, use the putData() technique, which will prevent rounding or truncating
			    // of the number. If the toString() doesn't look like it can be handled that way, then call its
			    // doubleValue() method. An example where this is necessary is the com.drew.lang.Rational class,
			    // which looks like "2/3" as a string.
				byte[] data = obj.toString().getBytes();
				boolean hasDecimal = false;
				boolean mustBeConvertedToDouble = false;
				for (byte b : data) {
				    char c = (char) b;
				    if (c == '.')
				        hasDecimal = true;
				    else if (Character.isDigit(c) || c == '-' || c == '+' || c == 'E' || c == 'e')
				        continue;
				    else {
				        mustBeConvertedToDouble = true;
				        break;
				    }
				}
				if (mustBeConvertedToDouble) {
				    put(((Number) obj).doubleValue());
				} else {
	                putNext(MLTKREAL);
	                putSize(data.length + (hasDecimal ? 0 : 1));
	                putData(data, data.length);
	                // Need to stick a decimal point on end if there isn't one, otherwise Mathematica will do the equivalent of
	                // N[Integer], and you end up with 17 digits, losing everything else.
	                if (!hasDecimal)
	                    putData(new byte[]{(byte)46}, 1);
				}
			}
		} else if (obj instanceof Boolean) {
			putSymbol(((Boolean) obj).booleanValue() ? "True" : "False");
		} else if (obj instanceof Character) {
			put(((Character) obj).charValue());
		} else {
			// Only KernelLink implementations will override putReference to do something meaingful.
			putReference(obj);
		}
	}

	public synchronized void put(Object obj, String[] heads) throws MathLinkException {

		if (obj == null) {
			putSymbol("Null");
		} else if (obj.getClass().isArray()) {
			putArray(obj, heads);
		} else {
			throw new IllegalArgumentException();
		}
	}

	// This is the only method that derived classes must implement to put arrays.
	// Implementations can call putArrayPiecemeal (below) to put arrays of anything,
	// but most will want to something more efficient in at least some circumstances
	// (for example, for primitive arrays). In other words, putArray is where you have
	// your chance to do things in some efficient or special way, but you can always
	// call putArrayPiecemeal to do as much of the work as you want. In your putArray
	// implementation, you are guaranteed that the object is an array. Note that most
	// implementations will call putArrayPiecemeal anyway to put arrays of objects.
	protected abstract void putArray(Object obj, String[] heads) throws MathLinkException;


	// headIndex is the index into the heads array that should be used for the expr at the
	// current level.
	protected void putArrayPiecemeal(Object obj, String[] heads, int headIndex) throws MathLinkException {

		Class cls = (obj != null ? obj.getClass() : null);
		if (cls != null && cls.isArray()) {
			String thisHead = (heads != null && heads.length > headIndex) ? heads[headIndex] : "List";
			int len = Array.getLength(obj);
			putFunction(thisHead, len);
			headIndex++;
			for (int i = 0; i < len; i++) {
				putArrayPiecemeal(Array.get(obj, i), heads, headIndex);
			}
		} else {
			put(obj);
		}
	}


	/***************************************  Complex  ******************************************/

	public synchronized Object getComplex() throws MathLinkException {

		double re = 0;
		double im = 0;

		int type = getNext();
		switch (type) {
			case MLTKINT:
			case MLTKREAL: {
				re = getDouble();
				break;
			}
			case MLTKFUNC: {
				checkFunctionWithArgCount("Complex", 2);
				re = getDouble();
				im = getDouble();
				break;
			}
			default:
				throw new MathLinkException(MLE_BAD_COMPLEX);
		}
		// Note that we wait until the link has been drained to bail:
		if (getComplexClass() == null)
			return null;
		return constructComplex(re, im);
	}


	protected synchronized void putComplex(Object obj) throws MathLinkException {

		double re = 0;
		double im = 0;
		try {
			re = getRealPart(obj);
			im = getImaginaryPart(obj);
		} catch (Exception e) {
			putSymbol("$Failed");
			return;
		}
		putFunction("Complex", 2);
		// Use put(double), because it has code to handle NaN, infinity.
		put(re);
		put(im);
	}


	protected abstract Object constructComplex(double re, double im);
	protected abstract double getRealPart(Object complex) throws Exception;
	protected abstract double getImaginaryPart(Object complex)throws Exception;


	/***************************************  Misc  *******************************************/

	// Must be implemented by derived classes to put strings.
	protected abstract void putString(String s) throws MathLinkException;

	// putReference isn't part of the public API until you get up to KernelLink, but we need to provide
	// a fallback implementation here for when put(Object) is called on a MathLink and the object is not a by-value
	// type. This method must be protected, not private, even though the implementation is only called from
	// this class, because we need virtual function resolution when 'this' is a KernelLink.
	protected synchronized void putReference(Object obj) throws MathLinkException {
		// "Byval" put of an instance of a "real" class. Note that byval has no meaning
		// for such objects, so we just put the rather useless string representation.
		put(obj.toString());
	}

}
