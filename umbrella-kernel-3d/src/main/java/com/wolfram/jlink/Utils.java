//////////////////////////////////////////////////////////////////////////////////////
//
//   J/Link source code (c) 1999-2002, Wolfram Research, Inc. All rights reserved.
//
//   Use is governed by the terms of the J/Link license agreement, which can be found at
//   www.wolfram.com/solutions/mathlink/jlink.
//
//   Author: Todd Gayley
//
//////////////////////////////////////////////////////////////////////////////////////

package com.wolfram.jlink;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.*;
import java.math.MathContext;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;


public class Utils {

	///////////////////////////////////////////  Array utilities  /////////////////////////////////////////////

	public static boolean isPrimitiveArray(Class cls) {

		Class leafCls = getArrayComponentType(cls);
		if (leafCls == null)
			return false;
		else
			return leafCls.isPrimitive();
	}

	// Replacement for Class.getComponentType, which only looks at the next level down. This one returns
	// the Class at the bottom of the array. Must also work for non-array classes, returning null.
	public static Class getArrayComponentType(Class cls) {

		Class compCls = cls.getComponentType();
		if (compCls == null)
			return null;
		else if (compCls.isArray())
			return getArrayComponentType(compCls);
		else
			return compCls;
	}

	// Needless to say, this will only work for non-ragged arrays.
	public static int[] getArrayDims(Object arr) {

		int depth = getArrayDepth(arr);
		int[] result = new int[depth];
		Object subArray = arr;
		for (int i = 0; i < depth; i++) {
			int len = Array.getLength(subArray);
			if (len == 0) {
				// Encountered a level with length 0; fill out the rest of the dims with 0's.
				for (int j = i; j < depth; j++) {
					result[j] = 0;
				}
				break;
			} else {
				result[i] = len;
				subArray = Array.get(subArray, 0);
			}
		}
		return result;
	}

    // Works for ragged or non-ragged arrays.
    public static int getArrayDepth(Object arr) {

        int depth = 0;
        Class compCls = arr.getClass().getComponentType();
        while (compCls != null) {
            depth++;
            compCls = compCls.getComponentType();
        }
        return depth;
    }


	//////////////////////////////////////  Simple environment predicates ////////////////////////////////////////

	private static boolean isMacOSX;
	private static boolean isWindows;

	static {
		try {
            String osName = System.getProperty("os.name").toLowerCase();
            isWindows = osName.startsWith("windows");
            // Try two different tests to determine OSX. The second test is the one
            // recommended by Apple, but I leave both here because the first test has
            // been used and has worked for years (including on OSX-x86).
			isMacOSX = System.getProperty("mrj.version") != null || osName.startsWith("mac os x");
		} catch (SecurityException e) {
			// These properties should not throw.
		}
	}

	public static boolean isMacOSX() {
		return isMacOSX;
	}

	public static boolean isWindows() {
		return isWindows;
	}


	////////////////////////////////////////////  Ragged arrays  ////////////////////////////////////////////////

	// Programs consult the isRaggedArrays() method to detect whether ragged (non-rectangular) arrays of primitive
	// types are allowed to be read and writen. Arrays of non-primitive types (String, objects, Expr, complex, etc.)
	// and booleans can always be ragged, no matter what any settings say. These sets correspond to the distinction
	// between types that do/don't have single MLGetXXXArray and MLPutXXXArray calls in the MathLink C API.
	// There are two ways in which the support for ragged arrays is controlled. The first way is
	// via the system property JLINK_RAGGED_ARRAYS, which can be set to true via the command line when Java is
	// launched: -DJLINK_RAGGED_ARRAYS=true. This setting can also be controlled by a call from Mathematica
	// (AllowRaggedArrays[True]) if Java is being driven from Mathematica. The default is false.
	// The advantage of false is speed, but note that data is duplicated in memory during writes. The alternative
	// slower handling (this was the pre-J/Link 1.1 behavior) does not duplicate the data in memory and allows ragged
	// arrays at a great speed cost for large 2-D or deeper arrays.

	private static boolean allowRaggedArrays;

	static {
		try {
			String prop = System.getProperty("JLINK_RAGGED_ARRAYS");
			allowRaggedArrays = prop != null && prop.toLowerCase().equals("true");
		} catch (SecurityException e) {
			allowRaggedArrays = false;
		}
	}

	public static boolean isRaggedArrays() {
		return allowRaggedArrays;
	}

	public static void setRaggedArrays(boolean allow) {
		allowRaggedArrays = allow;
	}


    // Returns false if array is ragged, true otherwise. Works for any depth.
    // Not used in J/Link, but potentially useful in the future. Not deeply tested, however.
    public boolean isRectangularArray(Object arr) {

        boolean result = false;
        int depth = getArrayDepth(arr);
        if (depth == 1)
            return true;
        // Although getArrayDims() says it doesn't work for ragged arrays, that's what we want--
        // checkLengths() will detect that things aren't right and return false.
        int[] dims = getArrayDims(arr);
        return checkLengths(arr, dims, 1);
    }

    // Worker function for isRectangularArray().
    private boolean checkLengths(Object a, int[] dims, int curDepth) {

        int expectedLenOfChildren = dims[curDepth];
        int len = Array.getLength(a);
        boolean goDeeper = curDepth < dims.length - 1;
        for (int i = 0; i < len; i++) {
            Object subArray = Array.get(a, i);
            if (Array.getLength(subArray) != expectedLenOfChildren)
                return false;
            if (goDeeper && !checkLengths(subArray, dims, curDepth + 1))
                return false;
        }
        return true;
    }


	///////////////////////////////////////  Link args parsing rountines  //////////////////////////////////////////

	// These next two are public because they are needed in jlink and jlink.ext packages.
	// They are not user-level methods.

	public static String determineLinkname(String cmdLine) {

		java.util.StringTokenizer st = new java.util.StringTokenizer(cmdLine);
		while (st != null && st.hasMoreTokens()) {
			String tok = st.nextToken().toLowerCase();
			if ((tok.equals("-linkname") || tok.equals("-linklaunch")) && st.hasMoreTokens())
				return st.nextToken();
		}
		return null;
	}

	public static String determineLinkname(String[] argv) {

		if (argv != null) {
			for (int i = 0; i < argv.length - 1; i++) {
				String s = argv[i].toLowerCase();
				if (s.equals("-linkname") || s.equals("-linklaunch"))
					return argv[i+1];
			}
		}
		return null;
	}

	// These two are needed only in NativeLink, but I'll make them public anyway; they might
	// be useful in the future.

	public static String determineLinkmode(String cmdLine) {

		java.util.StringTokenizer st = new java.util.StringTokenizer(cmdLine);
		while (st != null && st.hasMoreTokens()) {
			String tok = st.nextToken().toLowerCase();
			if (tok.equals("-linkmode") && st.hasMoreTokens())
				return st.nextToken().toLowerCase();
		}
		return null;
	}

	public static String determineLinkmode(String[] argv) {

		if (argv != null) {
			for (int i = 0; i < argv.length - 1; i++) {
				String s = argv[i].toLowerCase();
				if (s.equals("-linkmode"))
					return argv[i+1].toLowerCase();
			}
		}
		return null;
	}


	//////////////////////////////////////////  Expression-writing methods  ////////////////////////////////////////////

	// These next methods are called by implementors of the KernelLink "evaluateTo" methods. It is useful to separate the
	// code to create the appropriate expression to send (which is what these methods do) and the code that runs the
	// reading loop. For example, HTTPLinkServlet needs to separate these two aspects.
	// In these methods, obj must be a string or Expr.

	// This original signature was not rich enough, but since it was public (but undocumented) I'll leave it here.
	public static void writeEvalToStringExpression(MathLink ml, Object obj, int pageWidth, boolean isOutputForm) throws MathLinkException {
		writeEvalToStringExpression(ml, obj, pageWidth, isOutputForm ? "OutputForm" : "InputForm");
	}

	// This is ready to accommodate an evaluateToMathML() function, but I have not added such a function to KernelLink yet.
	public static void writeEvalToStringExpression(MathLink ml, Object obj, int pageWidth, String format) throws MathLinkException {

		ml.putFunction("EvaluatePacket", 1);
		ml.putFunction("ToString", 3);
		if (obj instanceof String)
			ml.putFunction("ToExpression", 1);
		ml.put(obj);
		ml.putFunction("Rule", 2);
		ml.putSymbol("FormatType");
		ml.putSymbol(format);
		ml.putFunction("Rule", 2);
		ml.putSymbol("PageWidth");
		if (pageWidth > 0)
			ml.put(pageWidth);
		else
			ml.putSymbol("Infinity");
		ml.endPacket();
	}

	public static void writeEvalToTypesetExpression(MathLink ml, Object obj, int pageWidth, boolean useStdForm) throws MathLinkException {

		ml.putFunction("EvaluatePacket", 1);
		int numArgs = 1 + (useStdForm ? 0 : 1) + (pageWidth > 0 ? 1 : 0);
		ml.putFunction("EvaluateToTypeset", numArgs);
		ml.put(obj);
		if (!useStdForm)
			ml.putSymbol("TraditionalForm");
		if (pageWidth > 0)
			ml.put(pageWidth);
		ml.endPacket();
	}

	public static void writeEvalToImageExpression(MathLink ml, Object obj, int width, int height, int dpi, boolean useFE) throws MathLinkException {

		ml.putFunction("EvaluatePacket", 1);
		int numArgs = 1 + (useFE ? 1 : 0) + (dpi > 0 ? 1 : 0) + (width > 0 || height > 0 ? 1 : 0);
		ml.putFunction("EvaluateToImage", numArgs);
		ml.put(obj);
		if (useFE)
			ml.put(true);
		if (dpi > 0) {
			ml.putFunction("Rule", 2);
			ml.putSymbol("ImageResolution");
			ml.put(dpi);
		}
		if (width > 0 || height > 0) {
			ml.putFunction("Rule", 2);
			ml.putSymbol("ImageSize");
			ml.putFunction("List", 2);
			if (width > 0)
				ml.put(width);
			else
				ml.putSymbol("Automatic");
			if (height > 0)
				ml.put(height);
			else
				ml.putSymbol("Automatic");
		}
		ml.endPacket();
	}


	/////////////////////////////////////////////  Miscellaneous  ////////////////////////////////////////////////////

	// This information can also be obtained by directly using KernelLink.VERSION, but that is a compile-time
	// lookup because the constant is final. It is convenient to have a run-time lookup so classes that use this
	// value do not need to be recompiled when J/Link is updated. This is used by the MRJHandlers class, which
	// needs to be compiled on a Mac, so we do not want to have to recompile it every time we update some other
	// part of J/Link.
	public static String getJLinkVersion() {
		return KernelLink.VERSION;
	}


	public static java.math.BigDecimal bigDecimalFromString(String s) {

		// Need to accommodate InputForm bigdecimal, e.g. -1.234567...89e35\0`53.101 or -1.234567...89`53.101*^35\0`53.101.
		// Note that I probably need to respect the precision info that is supplied via the numbermark.
		// The idea is to extract the digits as a big integer and then determine the scale. These
		// are the components we need for the BigDecimal constructor.

        int len = s.length();
		byte[] data = s.getBytes();

        // For some reason the kernel can write real numbers with spaces embedded and only junk afterwards,
        // and when reading reals MLGetString and related funcs will convert spaces into 0 chars (\0, not '0'),
        // so the first step is to truncate the string at the first \0 char (actually, because it is not
        // clear that all versions of the kernel will do this conversion, truncate at either ' ' or \0).
        int i;
        int tickPos = -1;
        boolean finishedWithDigits = false;
        for (i = 0; i < len; i++) {
            byte b = data[i];
            if ((b == 0 || b == 32) && !finishedWithDigits) {
                len = i;
                finishedWithDigits = true;
            } else if (b == 96) {
                tickPos = i;
            }
        }

        // Get precision info, if present.
        int precision = -1;
        /* This was an attempt to get precision info from M reals into the BigDecimal objects using new features in
         * Java 5. I think it is working, although other changes are needed to use it correctly (MathLinkImplBase.put(Object),
         * where BigDecimals are written onto a link). There are issues with this idea, and M reals can never be converted
         * into BigDecimal with complete accuracy (for example, M reals can have non-integer precision). I am not 
         * convinced right now that this is the right thing to do, or at least that there wouldn't be unintended consequences
         * or code breakage, so I am leaving it commented out.
        if (tickPos > 0) {
            int startOfPrecision = tickPos + 1;
            int endOfPrecision = startOfPrecision;
            while (endOfPrecision < len && Character.isDigit(data[endOfPrecision]))
                endOfPrecision++;
            // At end of above loop, endOfPrecision points to the first position past the end of precision info. It could
            // be a decimal point, since precision info from M is often non-integer, but we can only handle integer
            // precision. The end could also be because we hit the end of the string, or we hit the *^ chars.
            if (endOfPrecision > startOfPrecision)
                precision = Integer.parseInt(s.substring(startOfPrecision, endOfPrecision));
        }
        */
        
		byte[] digitBuf = new byte[len];

		int digitCount = 0;
		int decimalPos = -1;
		boolean isNegative = false;

		// First get the digits from the number, ignoring the exponent. Record position of the decimal point.
		for (i = 0; i < len; i++) {
			byte b = data[i];
			if (b >= 48 && b <=57) {
				// Digit
				digitBuf[digitCount++] = b;
			} else if (b == 45) {
				// Minus sign
				isNegative = true;
				digitBuf[digitCount++] = b;
			} else if (b == 46) {
				decimalPos = i;
			} else {
				// End of digits for unscaled value part of BigDecimal.
				break;
			}
		}
        // Note that the value of i at the end if this loop is used later.

		// Now create in unscaledValue an integer that contains all the digits of the original real, but no decimal point.
		String unscaledValue = new String(digitBuf, 0, digitCount);
		// this scale value will be modified later if there is an exponent.
		int scale = decimalPos != -1 ? digitCount - decimalPos : 0;

		// Advance i to point to first char past either 'e' or '*^'. That position is the start of the exponent.
		for ( ; i < len; i++) {
			byte b = data[i];
			if (b == 101) {
				// e (old style number format)
                i++;
				break;
			} else if (b == 42) {
				// * (new style number format)
				i+= 2;
				break;
			}
		}

		// Now get exponent as an integer. Reuse digitBuf.
		digitCount = 0;
		for ( ; i < len; i++) {
			byte b = data[i];
			if (b == 45 || (b >= 48 && b <=57)) {
				// Minus sign or digit.
				digitBuf[digitCount++] = b;
			} else if (b == 43) {
                // Plus sign. Do nothing (Integer.parseInt() cannot handle a leading + sign, if you can believe that).
            } else {
				break;
			}
		}
		if (digitCount > 0) {
			int exponent = Integer.parseInt(new String(digitBuf, 0, digitCount));
			scale -= exponent;
		}

		if (precision >= 0)
            return new java.math.BigDecimal(new java.math.BigInteger(unscaledValue), scale, new MathContext(precision));
		else
            return new java.math.BigDecimal(new java.math.BigInteger(unscaledValue), scale);
	}


	// This produces a string formatted for a Mathematica message by stripping off the jlink
	// internal classes from the stack trace. It's a bit of a kludge in that it needs to pull
	// out a line that arises from jlink internals but is not trivially recognizable as such
	// (java.lang.reflect.invoke). This showed up in JDK1.2, suggesting that future JDK versions
	// may alter the stack trace in ways that let other lines slip through. These lines
	// can be pretty mysterious to users, since they have no other context to suggest that they
	// arise from jlink itself. Perhaps in the future I will just skip this whole step and
	// expose all the jlink internal stack trace.
	static String createExceptionMessage(Throwable t) {

		// If t is an InvocationTargetException, we want to see the target exception, not t itself.
		if (t instanceof java.lang.reflect.InvocationTargetException)
			t = ((java.lang.reflect.InvocationTargetException) t).getTargetException();
		java.io.StringWriter sw = new java.io.StringWriter();
		java.io.PrintWriter pw = new java.io.PrintWriter(sw, false);
		t.printStackTrace(pw);
		String stackTrace = sw.toString();
        pw.close();
        // Throw away the part of the stack not of interest to users (J/Link internals).
        String[] lines = stackTrace.split("\\r\\n|\\r|\\n");
        ArrayList linesToKeep = new ArrayList(lines.length);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.indexOf("at com.wolfram.jlink") == -1 &&
                    line.indexOf("at java.lang.reflect.Method.invoke") == -1 &&
                        line.indexOf("at sun.reflect.NativeMethodAccessorImpl") == -1 &&
                            line.indexOf("at sun.reflect.DelegatingMethodAccessorImpl") == -1 &&
                                line.indexOf("at sun.reflect.GeneratedMethodAccessor1") == -1 &&
                                    line.indexOf("at sun.reflect.GeneratedMethodAccessor2") == -1)
                linesToKeep.add(line);
        }
        // shortStackTrace is "short" because it has portions of trace due to my own code removed.
        String shortStackTrace = "";
        Iterator iter = linesToKeep.iterator();
        while (iter.hasNext())
            shortStackTrace += ((String) iter.next()) + (iter.hasNext() ? "\n" : "");
		return shortStackTrace;
	}


	// Returns the Mathematica-style SystemID string for the current architecture. Returns an aray, as there might
	// be more than one valid SystemID for the platform (e.g., HP-RISC and HPUX-PA64).
	public static String[] getSystemID() {

		String os = System.getProperty("os.name");
		String arch = System.getProperty("os.arch");

		if (isWindows())
			return new String[] {"Windows", "Windows-x86-64"};
		else if (isMacOSX())
            return (arch.equals("x86_64")) ?
                        new String[] {"MacOSX-x86-64", "MacOSX-PowerPC64"} : 
                        new String[] {"MacOSX-x86", "MacOSX-x86-64", "MacOSX", "MacOSX-PowerPC64"};
        else if (arch.equals("x86") && os.equals("SunOS"))
            return new String[] {"Solaris-x86-64"};
        else if (arch.equals("i386") || arch.equals("x86"))
            return new String[] {"Linux"};
        else if (arch.equals("amd64") && os.equals("Linux"))
            return new String[] {"Linux-x86-64"};  // You get 'amd64' for AMD or Intel.
        // "sparcv9" is returned in 64-bit mode
		else if (arch.equals("sparc"))
			return new String[] {"Solaris", "UltraSPARC", "Solaris-SPARC"};
        // "PA_RISC2.0W" is returned in 64-bit mode
        else if (arch.startsWith("PA_RISC") || arch.startsWith("PA-RISC"))  // _ for 11.0, - for 10.20
            return new String[] {"HP-RISC", "HPUX-PA64"};
		else if (arch.equals("mips"))
			return new String[] {"IRIX-MIPS32", "IRIX-MIPS64"};
		else if (arch.equals("alpha"))
			return os.equals("Linux") ? new String[] {"Linux-AXP"} : new String[] {"DEC-AXP"};
		else if (arch.equals("ppc"))
			return os.equals("Linux") ? new String[] {"Linux-PPC"} : new String[] {"IBM-RISC", "AIX-Power64"};
	    else if (arch.equals("ia64") && os.equals("Linux"))
	        return new String[] {"Linux-IA64"};
	    else if (arch.equals("arm") && os.equals("Linux")) // e.g. Raspberry Pi
	        return new String[] {"Linux-ARM"};
		else
			return new String[] {""};
	}


	// Cached value.
	private static String jlinkJarDir = null;

	public static String getJLinkJarDir() {

		if (jlinkJarDir != null)
			return jlinkJarDir;

		String jarDir = null;
		java.net.URL classURL = null;
		try {
			classURL = MathLink.class.getResource("/com/wolfram/jlink/NativeLink.class");
		} catch (Exception e) {
			// Fail quietly if this throws a SecurityException (or any other, for that matter).
		}
		if (classURL != null) {
            // This will give something like file:/D:/math41/AddOns/Applications/JLink/JLink.jar!com/wolfram/jlink/MathLink.class.
            // Note that + chars in the original path get turned into spaces in jarDir, so things break if the
            // path has + chars in it. I don't see any way to get around that.
            String jarPath = classURL.getFile();
			if (jarPath != null && jarPath.startsWith("file:") && jarPath.indexOf("JLink.jar") != -1) {
                try {
                    // This step is to replace %20 with space characters. Because of the way we get the location
                    // (via a URL), some special chars are encoded, so we must decode them.
                    jarPath = URLDecoder.decode(jarPath, "UTF-8");
                } catch (UnsupportedEncodingException e) { }// Won't happen; do nothing.
				// Drop "file:" and take the rest up to JLink.jar.
				jarDir = jarPath.substring(5, jarPath.indexOf("JLink.jar"));
			}
		}
		jlinkJarDir = jarDir;
		return jlinkJarDir;
	}

}
