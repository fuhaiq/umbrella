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

/*
   TODO: Support depth > 2 in asArray.
*/

package com.wolfram.jlink;

import java.lang.reflect.*;
import java.io.*;
import java.math.BigInteger;
import java.math.BigDecimal;

   /**
    * The Expr class is a representation of arbitrary Mathematica expressions in Java.
    * Exprs are created by reading an expression from a link (using the getExpr() method),
    * they can be decomposed into component Exprs with methods like head() and part(), and
    * their structure can be queried with methods like length(), numberQ(), and matrixQ().
    * All these methods will be familiar to Mathematica programmers, and their Expr
    * counterparts work similarly. Like Mathematica expressions, Exprs are immutable, meaning
    * they can never be changed once they are created. Operations that might appear to modify
    * an Expr (like delete()) return new modified Exprs without changing the original.
    * <p>
    * Exprs are stored initially in a very efficient way, and they can be created and written
    * to links very quickly. When you call operations that inspect their structure or that
    * extract component parts, however, it is likely that they must be unpacked into a more
    * Java-native form that requires more memory.
    * <p>
    * In its present state, Expr has four main uses:
    * <p>
    * (1) Storing expressions read from a link so that they can be later written to another
    * link. This use replaces functionality that C-language programmers would use a loopback
    * link for. (J/Link has a LoopbackLink interface as well, but Expr affords an even easier
    * method.)
    * <pre>
    *     Expr e = ml.getExpr();
    *     // ... Later, write it to a different MathLink:
    *     otherML.put(e);
    *     e.dispose();</pre>
    * Note that if you just want to move an expression immediately from one link to another, you
    * can use the MathLink method transferExpression() and avoid creating an Expr to store it.
    * <p>
    * (2) Many of the KernelLink methods take either a string or an Expr. If it is not convenient
    * to build a string of Mathematica input, you can use an Expr. There are two ways to build an
    * Expr: you can use a constructor, or you can create a loopback link as a scratchpad,
    * build the expression on this link with a series of MathLink put calls, then read
    * the expression off the loopback link using getExpr(). Here is an example that creates an Expr
    * that represents 2+2 and computes it in Mathematica using these two techniques:
    * <pre>
    * 	// First method: Build it using Expr constructors:
    * 	Expr e1 = new Expr(new Expr(Expr.SYMBOL, "Plus"), new Expr[]{new Expr(2), new Expr(2)});
    *  	// ml is a KernelLink
    * 	String result = ml.evaluateToOutputForm(e1, 72);
    * 	// Second method: Build it on a LoopbackLink with MathLink calls:
    * 	LoopbackLink loop = MathLinkFactory.createLoopbackLink();
    * 	loop.putFunction("Plus", 2);
    * 	loop.put(2);
    * 	loop.put(2);
    * 	Expr e2 = loop.getExpr();
    * 	loop.close();
    * 	result = ml.evaluateToOutputForm(e2, 72);
    * 	e2.dispose();</pre>
    * (3) Getting a string representation of an expression. Sometimes you want to be able to
    * produce a readable string form of an entire expression, particularly for debugging. The
    * toString() method will do this for you:
    * <pre>
    *     // This code will print out the next expression waiting on the link without
    *     // consuming it, so that the state of the link is unchanged:
    *     System.out.println("Next expression is: " + ml.peekExpr().toString());</pre>
    * (4) Examining the structure or properties of an expression. Although it is possible to
    * do this sort of thing with MathLink calls, it is very difficult in general. Expr lets
    * you read an entire expression from a link and then examine it using a very high-level
    * interface and without having to worry about managing your current position in an
    * incoming stream of data.
    * <p>
    * Expr is a work in progress. It will be expanded in the future.
    */
    
public final class Expr implements Serializable {

	/**
	 * A type constant representing integers, for use in an Expr constructor, vectorQ(type), or matrixQ(type).
	 * 
	 * @see #Expr(int, String)
	 * @see #vectorQ(int)
	 * @see #matrixQ(int)
	 */

	// Only for use in the Q methods (e.g., matrixQ(REAL)).
	public static final int INTEGER		= 1;

	/**
	 * A type constant representing real numbers, for use in an Expr constructor, vectorQ(type), or matrixQ(type).
	 * 
	 * @see #Expr(int, String)
	 * @see #vectorQ(int)
	 * @see #matrixQ(int)
	 */
	public static final int REAL			= 2;

	/**
	 * A type constant representing strings, for use in an Expr constructor, vectorQ(type), or matrixQ(type).
	 * 
	 * @see #Expr(int, String)
	 * @see #vectorQ(int)
	 * @see #matrixQ(int)
	 */
	public static final int STRING		= 3;

	/**
	 * A type constant representing symbols, for use in an Expr constructor, vectorQ(type), or matrixQ(type).
	 * 
	 * @see #Expr(int, String)
	 * @see #vectorQ(int)
	 * @see #matrixQ(int)
	 */
	public static final int SYMBOL		= 4;

	/**
	 * A type constant representing rational numbers, for use in an Expr constructor, vectorQ(type), or matrixQ(type).
	 * 
	 * @see #Expr(int, String)
	 * @see #vectorQ(int)
	 * @see #matrixQ(int)
	 */
	public static final int RATIONAL		= 5;

	/**
	 * A type constant representing complex numbers, for use in an Expr constructor, vectorQ(type), or matrixQ(type).
	 * 
	 * @see #Expr(int, String)
	 * @see #vectorQ(int)
	 * @see #matrixQ(int)
	 */
	public static final int COMPLEX		= 6;

	/**
	 * A type constant representing integers larger than can fit in a Java int, for use in an Expr constructor, vectorQ(type), or matrixQ(type).
	 * 
	 * @see #Expr(int, String)
	 * @see #vectorQ(int)
	 * @see #matrixQ(int)
	 */
	public static final int BIGINTEGER	= 7;

	/**
	 * A type constant representing floating point numbers larger than can fit in a Java double, for use in an Expr constructor, vectorQ(type), or matrixQ(type).
	 * 
	 * @see #Expr(int, String)
	 * @see #vectorQ(int)
	 * @see #matrixQ(int)
	 */
	public static final int BIGDECIMAL	= 8;
	
	// NEWBIGDECIMAL is a new type of Expr that allows Mathematica bigreals to be turned into Exprs and then sent back
	// to Mathematica with no loss of information. This type is hidden from users, being just an implementation
	// detail of the Expr class. The idea is to keep the real number in M string form and then put that back on the
	// link when the number is sent to Mathematica. A BigDecimal object is created and cached if and only if the
	// number is needed for operations on the Java side.
	private static final int NEWBIGDECIMAL = 9;
	

	private static final int UNKNOWN		= 0;  // The loopback link hasn't been unwound.
	// Next val is never assigned to any Expr; just a sentinel value.
	private static final int FIRST_COMPOSITE = 100;
	private static final int FUNCTION		= 100;
	// Next val is never assigned to any Expr; just a sentinel value. All array types must be larger.
	private static final int FIRST_ARRAY_TYPE	= 200;
	private static final int INTARRAY1	= 200;
	private static final int REALARRAY1	= 201;
	private static final int INTARRAY2	= 202;
	private static final int REALARRAY2	= 203;

	/**
	 * Unused for now.
	 */
	public static final Expr SYM_SYMBOL		= new Expr(SYMBOL, "Symbol");  // Must be first among the SYM_xxx defs.

	/**
	 * Unused for now.
	 */
	public static final Expr SYM_INTEGER	= new Expr(SYMBOL, "Integer");

	/**
	 * Unused for now.
	 */
	public static final Expr SYM_REAL		= new Expr(SYMBOL, "Real");

	/**
	 * Unused for now.
	 */
	public static final Expr SYM_STRING		= new Expr(SYMBOL, "String");

	/**
	 * Unused for now.
	 */
	public static final Expr SYM_RATIONAL	= new Expr(SYMBOL, "Rational");

	/**
	 * Unused for now.
	 */
	public static final Expr SYM_COMPLEX	= new Expr(SYMBOL, "Complex");

	/**
	 * Unused for now.
	 */
	public static final Expr SYM_LIST		= new Expr(SYMBOL, "List");

	/**
	 * Unused for now.
	 */
	public static final Expr SYM_TRUE		= new Expr(SYMBOL, "True");

    /**
     * Unused for now.
     */
    public static final Expr SYM_FALSE      = new Expr(SYMBOL, "False");

    /**
     * Unused for now.
     */
    public static final Expr SYM_NULL       = new Expr(SYMBOL, "Null");

	/**
	 * Unused for now.
	 */
	public static final Expr INT_ONE			= new Expr(1);

	/**
	 * Unused for now.
	 */
	public static final Expr INT_ZERO		= new Expr(0);

	/**
	 * Unused for now.
	 */
	public static final Expr INT_MINUSONE	= new Expr(-1);

	private int type;
	private Expr head;
	private Expr[] args;
	private Object val;  // Used for exprs not stored in head/args form (arrays of real/int, atomic types)
	private transient BigDecimal bigDecimalForm; // See comments elsewhere in this file about NEWBIGDECIMAL.
	private transient LoopbackLink link;
	// Cache the hash since Exprs are immutable.
	private volatile int cachedHashCode = 0;


	//////////////////////////  Constructors  ////////////////////////////
	
	private Expr() {}

	/**
	 * Creates an Expr representing a Mathematica Integer, Real, String, or Symbol whose value is
	 * given by the supplied string (for example "2", "3.14", or "Plus").
	 * 
	 * @param type the type of the Expr; must be one of INTEGER, REAL, BIGINTEGER, BIGDECIMAL, STRING, or SYMBOL
	 * @param val the value of the Expr, interpreted according to the type argument
	 * @exception IllegalArgumentException if an unsupported type is specified
	 */
	
	// Creates atomic types only, and only one-arg versions (i.e., no Complex or Rational).
	public Expr(int type, String val) {
		
		this.type = type;
		switch (type) {
			case INTEGER:
				this.head = SYM_INTEGER;
				this.val = new Long(val);
				break;
			case REAL:
				this.head = SYM_REAL;
				this.val = new Double(val);
				break;
			case STRING:
				this.head = SYM_STRING;
				this.val = val;
				break;
			case SYMBOL:
                if ("".equals(val))
                    throw new IllegalArgumentException("Cannot create a Symbol Expr from an empty string");
			    // Machinations here avoid recursion in the SYM_SYMBOL initializer.
				this.head = val.equals("Symbol") ? this : SYM_SYMBOL; 
				this.val = val;
				break;
			case BIGINTEGER:
				this.head = SYM_INTEGER;
				this.val = new BigInteger(val);
				break;
			case BIGDECIMAL:
			    // Store in NEWBIGDECIMAL form.
			    this.type = NEWBIGDECIMAL;
				this.head = SYM_REAL;
				this.val = val;
				// Lazily construct the bigDecimalForm value; might never be needed.
				break;
			default:
				throw new IllegalArgumentException("Unsupported type in Expr(type, string) constructor: " + type);
		}
	}

	/**
	 * Creates an Expr representing a Mathematica Integer with the specified value.
	 * 
	 * @param val
	 */
	
	public Expr(long val) {
		
		this.type = INTEGER;
		this.head = SYM_INTEGER;
		this.val = new Long(val);
	}

	/**
	 * Creates an Expr representing a Mathematica Real with the specified value.
	 * 
	 * @param val
	 */
	
	public Expr(double val) {
		
		this.type = REAL;
		this.head = SYM_REAL;
		this.val = new Double(val);
	}

	/**
	 * Creates an Expr representing a Mathematica String with the specified value.
	 * 
	 * @param val
	 */
	
	public Expr(String val) {
		
		// Assumes a String expr, not Symbol.
		this.type = STRING;
		this.head = SYM_STRING;
		this.val = val;
	}

	/**
	 * Creates an Expr representing a Mathematica list of integers with the specified value.
	 * 
	 * @param val
	 */
	
	public Expr(int[] val) {
		
		this.type = INTARRAY1;
		this.head = SYM_LIST;
		// Defensive copy here and in other ctors (a la "Effective Java" item 24).
		this.val = val.clone();
	}

	/**
	 * Creates an Expr representing a Mathematica list of reals with the specified value.
	 * 
	 * @param val
	 */
	
	public Expr(double[] val) {
		
		this.type = REALARRAY1;
		this.head = SYM_LIST;
		this.val = val.clone();
	}

	/**
	 * Creates an Expr representing a Mathematica matrix of integers with the specified value.
	 * 
	 * @param val
	 */
	
	public Expr(int[][] val) {
		
		this.type = INTARRAY2;
		this.head = SYM_LIST;
		this.val = new int[val.length][];
		for (int i = 0; i < val.length; i++)
			((int[][])this.val)[i] = (int[]) val[i].clone();
	}

	/**
	 * Creates an Expr representing a Mathematica matrix of reals with the specified value.
	 * 
	 * @param val
	 */
	
	public Expr(double[][] val) {
		
		this.type = REALARRAY2;
		this.head = SYM_LIST;
		this.val = new double[val.length][];
		for (int i = 0; i < val.length; i++)
			((double[][])this.val)[i] = (double[]) val[i].clone();
	}

	/**
	 * Creates an Expr representing a large Mathematica Integer with the specified value.
	 * 
	 * @param val
	 */
	
	public Expr(BigInteger val) {
		
		this.type = BIGINTEGER;
		this.head = SYM_INTEGER;
		this.val = val;
	}

	/**
	 * Creates an Expr representing a large Mathematica Real with the specified value.
	 * 
	 * @param val
	 */
	
	public Expr(BigDecimal val) {
		
		this.type = BIGDECIMAL;
		this.head = SYM_REAL;
		this.val = val;
	}

	/**
	 * Creates an Expr with the given head and arguments.
	 * 
	 * @param head an Expr giving the head of this Expr
	 * @param args an array of Exprs giving the arguments of this Expr; pass null or an empty array for no arguments
	 */
	
	public Expr(Expr head, Expr[] args) {
		
		this.type = FUNCTION;
		this.head = head;
        if (head == null)
            throw new IllegalArgumentException("The head of an Expr cannot be null. Use Expr.SYM_NULL if you want to represent the Mathematica symbol Null");
		if (args != null) {
		    for (int i = 0; i < args.length; i++)
		        if (args[i] == null)
		            throw new IllegalArgumentException("No member of the args array can be null. Use Expr.SYM_NULL if you want to represent the Mathematica symbol Null");
        }
		// The clone() below is recognized not to be a deep copy. We only need to copy the array.
		// The Exprs and their sub-Exprs are immutable, so they don't need to be cloned.
		this.args = args != null ? (Expr[]) args.clone() : new Expr[0];
	}
	
	/*
	public Expr(String head, Expr[] args);

	public Expr(Expr head, Object[] args);

	public Expr(Object atom);
	*/
	
	/////////////////  Static Factory  ////////////////
	
	/**
	 * This factory method will only be used by advanced programmers who are creating their own
	 * classes that implement the MathLink interface. You would call this method in your
	 * implementation of getExpr(). In other words, this method exists not as a means for casual
	 * users to create Exprs from a link (use the MathLink method getExpr() instead), but so that MathLink
	 * implementors can write their own getExpr() methods without having to know anything
	 * about the internals of the Expr class. Exprs know how to read themselves
	 * off a link.
	 * 
	 * @param ml
	 * @return the newly-created expr read off the link
	 * @exception com.wolfram.jlink.MathLinkException
	 */
	
	public static Expr createFromLink(MathLink ml) throws MathLinkException {
		return createFromLink(ml, true);
	}
	

	//////////////////////////  Serialization  /////////////////////////
	
	// We use the default serialized form. This could lead to stack overflows when deserializing
	// exceptionally deep Exprs (like Nest[f, x, {1000}]).
	
	private static final long serialVersionUID = 469201568023508L;  // Aribtrary value
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		
		prepareFromLoopback();
		out.defaultWriteObject();
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}


	//////////////////////////  Equals and Hashcode  ///////////////////////////////

	/**
	 * Implements an equality comparison that is similar to Mathematica's SameQ. It is
	 * not guaranteed to have the same behavior as SameQ. For example, it is possible
	 * in some rare circumstances to have two Exprs that contain real numbers, and Mathematica
	 * would say the Exprs satisfy SameQ but they would not satsify equals() in Java.
	 * 
	 * @param obj
	 */
	public boolean equals(Object obj) {
		
		if (obj == this)
			return true;
		if (!(obj instanceof Expr))
			return false;
		Expr otherExpr = (Expr) obj;
		if (cachedHashCode != 0 && otherExpr.cachedHashCode != 0 &&
				cachedHashCode != otherExpr.cachedHashCode)
			return false;		
		otherExpr.prepareFromLoopback();
		prepareFromLoopback();
		if (type != otherExpr.type)
			return false;
		if (val != null) {
			// This Expr is of the val != null type.
			if (otherExpr.val == null)
				return false;
			switch (type) {
				case INTEGER:
				case REAL:
				case STRING:
				case SYMBOL:
				case BIGINTEGER:
                case BIGDECIMAL:
                case NEWBIGDECIMAL:
                    return val.equals(otherExpr.val);
				case INTARRAY1: {
					int[] a = (int[]) val;
					int[] oa = (int[]) otherExpr.val;
					if (a.length != oa.length)
						return false;
					for (int i = 0; i < a.length; i++)
						if (a[i] != oa[i])
							return false;
					return true;
				}
				case REALARRAY1: {
					double[] a = (double[]) val;
					double[] oa = (double[]) otherExpr.val;
					if (a.length != oa.length)
						return false;
					for (int i = 0; i < a.length; i++)
						if (a[i] != oa[i])
							return false;
					return true;
				}
				case INTARRAY2: {
					int[][] a = (int[][]) val;
					int[][] oa = (int[][]) otherExpr.val;
					if (a.length != oa.length)
						return false;
					for (int i = 0; i < a.length; i++) {
						int[] aPart = a[i];
						int[] oaPart = oa[i];
						if (aPart.length != oaPart.length)
							return false;
						for (int j = 0; j < aPart.length; j++)
							if (aPart[j] != oaPart[j])
								return false;
					}
					return true;
				}
				case REALARRAY2: {
					double[][] a = (double[][]) val;
					double[][] oa = (double[][]) otherExpr.val;
					if (a.length != oa.length)
						return false;
					for (int i = 0; i < a.length; i++) {
						double[] aPart = a[i];
						double[] oaPart = oa[i];
						if (aPart.length != oaPart.length)
							return false;
						for (int j = 0; j < aPart.length; j++)
							if (aPart[j] != oaPart[j])
								return false;
					}
					return true;
				}
				default:
					// Just to make the compiler happy; should never get here.
					return false;
			}
		} else {
			// This Expr is of the head/args != null, val == null type.
			if (otherExpr.val != null)
				return false;
			if (!head.equals(otherExpr.head))
				return false;
			if (args.length != otherExpr.args.length)
				return false;
			for (int i = 0; i < args.length; i++)
				if (!args[i].equals(otherExpr.args[i]))
					return false;
			return true;
		}
	}
	
	
	public int hashCode() {
		
		if (cachedHashCode != 0)
			return cachedHashCode;
		
		prepareFromLoopback();
		
		// As always, at some point we must stop recursing into heads, as that is never-ending. We choose
		// to stop when we get a "true" atomic Expr (i.e., not RATIONAL or COMPLEX).
		if (type != RATIONAL && type != COMPLEX && atomQ())
			return val.hashCode();
			
		// Algorithm from "Effective Java" item 8.
		int hash = 17;
		hash = 37 * hash + type;
		if (head != null)
			hash = 37 * hash + head.hashCode();
		if (args != null)
			for (int i = 0; i < args.length; i++)
				hash = 37 * hash + args[i].hashCode();
		if (val != null) {
			if (type < FIRST_ARRAY_TYPE) {
				// Safe to call hashCode() on val for all these types, since their hashcodes are
				// direct reflections of their values (unlike arrays, whose hashcodes are addresses).
				hash = 37 * hash + val.hashCode();
			} else if (type == INTARRAY1) {
				int[] ia = (int[]) val;
				for (int i = 0; i < ia.length; i++)
					hash += ia[i];
			} else if (type == REALARRAY1) {
				double[] da = (double[]) val;
				for (int i = 0; i < da.length; i++)
					hash += (int) da[i];
			} else if (type == INTARRAY2) {
				int[][] iaa = (int[][]) val;
				for (int i = 0; i < iaa.length; i++) {
					int[] ia = iaa[i];
					for (int j = 0; j < ia.length; j++)
						hash += ia[j];
				}
			} else if (type == REALARRAY2) {
				double[][] daa = (double[][]) val;
				for (int i = 0; i < daa.length; i++) {
					double[] da = daa[i];
					for (int j = 0; j < da.length; j++)
						hash += (int) da[j];
				}
			}
		}

		cachedHashCode = hash;
		return hash;
	}


	// The "real" hashCode() method is quite expensive, as it forces the expression to be unwound from its
	// loopback link. We want to avoid this cost when Exprs are returned to Mathematica (they are stored
	// in a hashtable in Java), so we provide a separate method that just uses the default
	// Object.hashCode(). This is perfectly legitimate, as there is no particular reason to require the
	// actual hashCode() method to be called, as long as we are consitent and always use this method
	// for Exprs (this happens in the ObjectHandler.InstanceCollection class).
	int inheritedHashCode() {
		return super.hashCode();
	}
	
	
	//////////////////////////  Methods  ///////////////////////////////

	/**
	 * Frees resources that the Expr uses internally. The object should not be used
	 * after dispose() has been called. You should get in the habit of calling
	 * dispose() on Exprs as soon as you are finished using them.
	 */
	
	public synchronized void dispose() {

		if (link != null) {
			link.close();
			link = null;
		} else if (type == FUNCTION) {
			if (head != null) head.dispose();
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
				    Expr arg = args[i];
					if (arg != null)
					    arg.dispose();
				}
			}
		}
	}

	/**
	 * Gives a new Expr representing the head of this Expr. Works like the Mathematica
	 * function Head.
	 * 
	 * @return the head
	 */
	
	public Expr head() {
		
		prepareFromLoopback();
		return type < FIRST_ARRAY_TYPE ? head : SYM_LIST;
	}

	/**
	 * Gives an array of Exprs representing the arguments of this Expr. For Exprs of type RATIONAL and COMPLEX,
	 * returns a two-argument array giving the numerator/denominator or re/im parts, respectively. If there are
	 * no args (this is a function with zero arguments, or an atom of type INTEGER, REAL, STRING, SYMBOL, BIGINTEGER,
	 * or BIGDECIMAL), then a 0-length aray is returned.
	 * 
	 * @return an array of the arguments, as Exprs.
	 */
	
	public synchronized Expr[] args() {
		
		// Defensive copying a la "Effective Java" item 24.
		return (Expr[]) nonCopyingArgs().clone();
	}

	/**
	 * Gives the length (the number of arguments) of this Expr. Works like the Mathematica
	 * function Length.
	 * 
	 * @return the length
	 */
	
	public int length() {
		
		prepareFromLoopback();
		if (type >= FIRST_ARRAY_TYPE) {
			return Array.getLength(val);
		} else {
			// If it's not an array, we know the args cache field is already filled in.
			return args != null ? args.length : 0;
		}
	}

	/**
	 * Gives an array of integers representing the dimensions of this Expr. Works like the
	 * Mathematica function Dimensions.
	 * 
	 * @return the dimensions, as an array
	 */
	
	public int[] dimensions() {
		
		prepareFromLoopback();
		int[] dims = null;
		if (type < FIRST_COMPOSITE) {
			dims = new int[0];
		} else {
			switch (type) {
				case INTARRAY1:
				case REALARRAY1:
					dims = new int[1];
					dims[0] = Array.getLength(val);
					break;
				case INTARRAY2:
					dims = new int[2];
					dims[0] = Array.getLength(val);
					dims[1] = ((int[][])val)[0].length;
					break;
				case REALARRAY2:
					dims = new int[2];
					dims[0] = Array.getLength(val);
					dims[1] = ((double[][])val)[0].length;
					break;
				case FUNCTION: {
					if (args.length == 0) {
						dims = new int[1];
						dims[0] = 0;
						break;
					}
					int[] leafDims = args[0].dimensions();
					int[] agreed = new int[leafDims.length + 1];
					agreed[0] = args.length;
					// Fill agreed with leafDims, starting at position 1. agreed never needs to get modified
					// again. Only depthOK can change.
					System.arraycopy(leafDims, 0, agreed, 1, leafDims.length);
					// Gives the number or elements of 'agreed' that should be used in result. It can only get smaller.
					int depthOK = 1 + leafDims.length;
					for (int i = 1; i < args.length; i++) {
						// A simple optimization--if depthOK ever gets to 1, then we can stop immediately 
						if (depthOK == 1)
							break;
						int[] otherLeafDims = args[i].dimensions();
						depthOK = Math.min(depthOK, 1 + otherLeafDims.length);
						// Because of the line above, depthOK is a suitable limit for the iteration below to ensure we won't walk off
						// the end of either array.
						for (int j = 1; j < depthOK; j++) {
							if (agreed[j] != otherLeafDims[j - 1]) {
								depthOK = j;
								break;
							}
						}
					}
					// Now go back and verify the heads. Walk down to level depthOK and see if everything you find has the right head.
					String headStr = head().toString();
					int headsAgreeDepth = checkHeads(headStr, 0, depthOK);
					dims = new int[headsAgreeDepth];
					System.arraycopy(agreed, 0, dims, 0, headsAgreeDepth);
					break;
				}
				default:
					if (MathLinkImpl.DEBUGLEVEL > 0) System.err.println("Bad type in Expr.dimensions");
			}
		}
		return dims;
	}

	/**
	 * Gives a new Expr representing the specified part of this Expr. Works like the Mathematica
	 * function Part.
	 * 
	 * @param i the index of the desired part
	 * @return the specified part, as an Expr
	 * @throws IllegalArgumentException if i is beyond the bounds of the expression
	 */
		
	public Expr part(int i) {
		
		prepareFromLoopback();
		if (Math.abs(i) > length())
			throw new IllegalArgumentException("Cannot take part " + i + " from this Expr because it has length " + length() + ".");
		else if (i == 0)
			return head();
		else if (i > 0)
			return nonCopyingArgs()[i - 1];
		else 
			return nonCopyingArgs()[length() + i];
	}

	/**
	 * Gives a new Expr representing the specified part of this Expr. Works like the Mathematica
	 * function Part.
	 * <p>
	 * This form of part() allows you to extract a part more than one level deep. Thus,
	 * e.part(new int[] {3,4}) is like the Mathematica function Part[e, 3, 4] or e[[3]][[4]].
	 *
	 * @param ia the index of the desired part
	 * @return the specified part, as an Expr
	 * @throws IllegalArgumentException if any of the part specifications are beyond the bounds of the expression.
	 */
	
	public Expr part(int[] ia) {
		
		try {
			int len = ia.length;
            if (len == 0) {
                // In Mathematica, e[[]] ---> e, and we reproduce that behavior here.
                return this;
            } else if (len == 1) {
                return part(ia[0]);
            } else {
				int[] newia = new int[len - 1];
				System.arraycopy(ia, 0, newia, 0, len - 1);
				return part(newia).part(ia[len - 1]);
			}
		} catch (IllegalArgumentException e) {
			// Catch the exception thrown by one of the subsidiary part() calls so we can issue a better message.
			throw new IllegalArgumentException("Part " + (new Expr(ia).toString()) + " of this Expr does not exist.");		
		}
	}

	/**
	 * Gives the real part of an Expr that represents a complex number. For integers and reals,
	 * it gives the number itself. Works much like the Mathematica function Re.
	 * <p>
	 * This method is meaningful only for Exprs that can represent Mathematica complex numbers
	 * (which is the set of Exprs for which complexQ, integerQ, realQ, or rationalQ would
	 * return true). Otherwise, it throws ExprFormatException.
	 * 
	 * @return The real part of the complex number, or 0 if this Expr is not an integer, real, rational, or complex number
	 * @see #im()
	 */

	public double re() throws ExprFormatException {
		
		prepareFromLoopback();
		switch (type) {
			case INTEGER:
			case REAL:
			case RATIONAL:
			case BIGINTEGER:
            case BIGDECIMAL:
            case NEWBIGDECIMAL:
				return asDouble();
			case COMPLEX:
				return args[0].asDouble();
			default:
				throw new ExprFormatException("This Expr is of type " + typeToString() + ", so you cannot call re() on it.");
		}
	}

	/**
	 * Gives the imaginary part of an Expr that represents a complex number.
	 * Works much like the Mathematica function Im.
	 * <p>
	 * This method is meaningful only for Exprs that can represent Mathematica complex numbers
	 * (which is the set of Exprs for which complexQ, integerQ, realQ, or rationalQ would
	 * return true). Otherwise, it throws ExprFormatException.
	 * 
	 * @return The imaginary part of the complex number
	 * @exception com.wolfram.jlink.ExprFormatException if this Expr is not an integer, real, rational, or complex number
	 * @see #re()
	 */
	
	public double im() throws ExprFormatException {
		
		prepareFromLoopback();
		switch (type) {
			case INTEGER:
			case REAL:
			case RATIONAL:
			case BIGINTEGER:
            case BIGDECIMAL:
            case NEWBIGDECIMAL:
				return 0.0;
			case COMPLEX:
				return args[1].asDouble();
			default:
				throw new ExprFormatException("This Expr is of type " + typeToString() + ", so you cannot call im() on it.");
		}
	}

	/**
	 * Gives a readable string representation. This representation is in what amounts
	 * to Mathematica FullForm, but it is not guaranteed to be usable directly as
	 * Mathematica input.
	 * 
	 * @return the string form
	 */
	
	public String toString() {
		
		String s = null;
		
		prepareFromLoopback();
		switch (type) {
			// Note this is not how Mathematica treats these. Revisit this issue of whether these
			// are atoms or not.
			case INTEGER:
			case SYMBOL:
			case BIGINTEGER:
                s = val.toString();
                break;
            case BIGDECIMAL:
                s = bigDecimalToInputFormString((BigDecimal)val);
                break;
            case NEWBIGDECIMAL:
                if (bigDecimalForm == null)
                    bigDecimalForm = Utils.bigDecimalFromString((String) val);
                s = bigDecimalToInputFormString(bigDecimalForm);
                break;
			case REAL:
				s = doubleToInputFormString(((Double) val).doubleValue());
				break;
			case STRING: {
				s = val.toString();
				StringBuffer buf = new StringBuffer(s.length() + 10);
				buf.append('"');
				int len = s.length();
				for (int i = 0; i < len; i++) {
					char c = s.charAt(i);
					if (c == '\\' || c == '"')
						buf.append('\\');
					buf.append(c);
				}
				buf.append('"');
				s = new String(buf);
				break;
			}
			case RATIONAL:
				s = "Rational[" + args[0].toString() + ", " + args[1].toString() + "]";
				break;
			case COMPLEX:
				s = "Complex[" + args[0].toString() + ", " + args[1].toString() + "]";
				break;
			case FUNCTION: {
				boolean isList = listQ();
				int len = length();
				StringBuffer buf = new StringBuffer(len * 2);
				buf.append(isList ? "{" : (head.toString() + "["));
				for (int i = 0; i < len; i++) {
					buf.append(args[i].toString());
					if (i < len - 1)
						buf.append(", ");
				}
				buf.append(isList ? '}' : ']');
				s = new String(buf);
				break;
			}
			case INTARRAY1:
			case REALARRAY1: {
				int len = Array.getLength(val);
				int[] ia = type == INTARRAY1 ? (int[]) val : null;
				double[] da = type == REALARRAY1 ? (double[]) val : null;
				StringBuffer buf = new StringBuffer(len * 2);
				buf.append('{');
				for (int i = 0; i < len; i++) {
					buf.append(type == INTARRAY1 ? String.valueOf(ia[i]) : doubleToInputFormString(da[i]));
					if (i < len - 1)
						buf.append(',');
				}
				buf.append('}');
				s = new String(buf);
				break;
			}
			case INTARRAY2:
			case REALARRAY2: {
				int len1 = Array.getLength(val);
				int len2 = Array.getLength(Array.get(val, 0));
				int[][] ia = type == INTARRAY2 ? (int[][]) val : null;
				double[][] da = type == REALARRAY2 ? (double[][]) val : null;
				StringBuffer buf = new StringBuffer(len1 * len2 * 2);
				buf.append('{');
				for (int i = 0; i < len1; i++) {
					buf.append('{');
					for (int j = 0; j < len2; j++) {
						buf.append(type == INTARRAY2 ? String.valueOf(ia[i][j]) : doubleToInputFormString(da[i][j]));
						if (j < len2 - 1)
							buf.append(',');
					}
					buf.append(i < len1 - 1 ? "}," : "}");
				}
				buf.append('}');
				s = new String(buf);
				break;
			}
			default:
				if (MathLinkImpl.DEBUGLEVEL > 0) System.err.println("Bad type in Expr.toString(): " + type());
		}
		return s;
	}

	/**
	 * Tells whether the Expr represents a Mathematica atom.
	 * Works like the Mathematica function AtomQ.
	 * 
	 * @return true, if the Expr is an atom; false otherwise
	 */
	
	public boolean atomQ() {
		prepareFromLoopback();
		return type < FIRST_COMPOSITE;
	}

	/**
	 * Tells whether the Expr represents a Mathematica string.
	 * Works like the Mathematica function StringQ.
	 * 
	 * @return true, if the Expr is a string; false otherwise
	 */
	
	public boolean stringQ() {
		prepareFromLoopback();
		return type == STRING;
	}

	/**
	 * Tells whether the Expr represents a Mathematica symbol. Works like the test
	 * in Mathematica: Head[e] === Symbol.
	 * 
	 * @return true, if the Expr is a symbol; false otherwise
	 */
	
	public boolean symbolQ() {
		prepareFromLoopback();
		return type == SYMBOL;
	}

	/**
	 * Tells whether the Expr represents a Mathematica integer.
	 * Works like the Mathematica function IntegerQ.
	 * 
	 * @return true, if the Expr is an integer; false otherwise
	 */
	
	public boolean integerQ() {
		prepareFromLoopback();
		return type == INTEGER || type == BIGINTEGER;
	}

	/**
	 * Tells whether the Expr represents a real (floating-point) number. Will be false if it
	 * is an integer. Works like the test in Mathematica: Head[e] === Real.
	 * 
	 * @return true, if the Expr is a non-integer real number; false otherwise
	 */
	
	public boolean realQ() {
		prepareFromLoopback();
		return type == REAL || type == BIGDECIMAL || type == NEWBIGDECIMAL;
	}

	/**
	 * Tells whether the Expr represents a rational number. Will be false if it
	 * is an integer. Works like the test in Mathematica: Head[e] === Rational.
	 * 
	 * @return true, if the Expr is a non-integer rational number; false otherwise
	 */
	
	public boolean rationalQ() {
		prepareFromLoopback();
		return type == RATIONAL;
	}

	/**
	 * Tells whether the Expr represents a complex number. Will be false if it
	 * is an integer or real. Works like the test in Mathematica: Head[e] === Complex.
	 * 
	 * @return true, if the Expr is a complex number; false otherwise
	 */
	
	public boolean complexQ() {
		prepareFromLoopback();
		return type == COMPLEX;
	}

	/**
	 * Tells whether the Expr represents a number (real, integer, rational, or complex).
	 * Works like the Mathematica function NumberQ.
	 * 
	 * @return true, if the Expr is a number type; false otherwise
	 */
	
	public boolean numberQ() {
		prepareFromLoopback();
		return type == REAL || type == INTEGER || type == BIGINTEGER || type == BIGDECIMAL || type == NEWBIGDECIMAL || type == COMPLEX || type == RATIONAL;
	}

	/**
	 * Tells whether the Expr represents a Mathematica integer, but requires more digits to store than can fit into a Java int.
	 * 
	 * @return true, if the Expr is a big integer; false otherwise
	 */
	
	public boolean bigIntegerQ() {
		prepareFromLoopback();
		return type == BIGINTEGER;
	}

	/**
	 * Tells whether the Expr represents a Mathematica real (floating-point) number, but requires more digits
	 * to store than can fit into a Java double.
	 * 
	 * @return true, if the Expr is a "bigfloat" number; false otherwise
	 */
	
	public boolean bigDecimalQ() {
		prepareFromLoopback();
		return type == BIGDECIMAL || type == NEWBIGDECIMAL;
	}

	/**
	 * Tells whether the Expr represents the Mathematica symbol True.
	 * Works like the Mathematica function TrueQ.
	 * 
	 * @return true, if the Expr is the symbol True; false otherwise
	 */
	
	public boolean trueQ() {
		prepareFromLoopback();
		return type == SYMBOL && val.equals("True"); 
	}

	/**
	 * Tells whether the Expr represents a Mathematica list (that is, it has head List).
	 * Works like the Mathematica function ListQ.
	 * 
	 * @return true, if the Expr has head List; false otherwise
	 */
	
	public boolean listQ() {
		prepareFromLoopback();
		return type >= FIRST_ARRAY_TYPE || type == FUNCTION && head.type == SYMBOL && head.val.equals("List"); 
	}

	/**
	 * Tells whether the Expr represents a Mathematica vector (that is, it has head List,
	 * and no parts are themselves lists). Works like the Mathematica function VectorQ.
	 * 
	 * @return true, if the Expr is a vector; false otherwise
	 */
	
	public boolean vectorQ() {

		prepareFromLoopback();
		if (type == INTARRAY1 || type == REALARRAY1)
			return true;
		if (type == INTARRAY2 || type == REALARRAY2 || !listQ())
			return false;
		// No need to force cache filling (by calling nonCopyingArgs()), since I've already ruled out the types
		// where the args field wouldn't have been filled.
		for (int i = 0; i < args.length; i++) {
			if (args[i].listQ())
				return false;
		}
		return true;
	}

	/**
	 * Tells whether the Expr represents a Mathematica vector, every element of which is
	 * of the specified type. Works like the Mathematica function VectorQ.
	 * 
	 * @param eType an integer constant representing the queried type. Will be one of INTEGER, REAL, STRING,
	 * SYMBOL, RATIONAL, COMPLEX.
	 * @return true, if the Expr is a vector with elements of the specified type; false otherwise
	 */
	
	public boolean vectorQ(int eType) {
		
		if (!vectorQ())
			return false;
		switch (type) {
			case INTARRAY1:
				return eType == INTEGER;
			case REALARRAY1:
				return eType == REAL;
			case INTARRAY2:
			case REALARRAY2:
				return false;
			default: {
				// Fall-through to here means we must painstakingly verify every leaf.
				int len = length();
				for (int i = 0; i < len; i++) {
					if (args[i].type() != eType)
						return false;
				}
			}
		}
		return true;
	}

	/**
	 * Tells whether the Expr represents a Mathematica matrix (that is, it has head List,
	 * every element has head List, and no deeper parts are themselves lists). Works like
	 * the Mathematica function MatrixQ.
	 * 
	 * @return true, if the Expr is a matrix; false otherwise
	 */

	public boolean matrixQ() {

		// Note a bug: does not verify that matrix is fully rectangular.
		prepareFromLoopback();
		if (type == INTARRAY2 || type == REALARRAY2)
			return true;
		if (type == INTARRAY1 || type == REALARRAY1 || !listQ())
			return false;
		// No need to force cache filling (by calling nonCopyingArgs()), since I've already ruled out the types
		// where the args field wouldn't have been filled.
		if (args.length == 0)
			return false;
		for (int i = 0; i < args.length; i++) {
			if (!args[i].vectorQ())
				return false;
		}
		// So far, we have verified that we have a list of lists (and no deeper lists). Now we
		// just have to verify that the length of the dimensions is at least 2.
		return dimensions().length >= 2;
	}

	/**
	 * Tells whether the Expr represents a Mathematica matrix, every element of which is of
	 * the specified type. Works like the Mathematica function MatrixQ.
	 * 
	 * @param eType an integer constant representing the queried type. Will be one of INTEGER, REAL, STRING,
	 * SYMBOL, RATIONAL, COMPLEX.
	 * @return true, if the Expr is a matrix with elements of the specified type; false otherwise
	 */
	
	public boolean matrixQ(int eType) {
		
		// Note a bug: does not verify that matrix is fully rectangular.
		if (!matrixQ())
			return false;
		if (eType == INTEGER && type == INTARRAY2 ||
				eType == REAL && type == REALARRAY2)
			return true;
		int len = length();
		// Here we need to force cache filling. We could get here if we had an array type
		// (e.g., an array of reals and asking if it's of type INTEGER).
		nonCopyingArgs();
		for (int i = 0; i < len; i++) {
			if (!args[i].vectorQ(eType))
				return false;
		}
		return true;
	}

	/**
	 * Gives the integer value for Exprs that can be represented as integers (this is exactly
	 * the set for which integerQ() returns true).
	 *
	 * @return the integer value
	 * @exception com.wolfram.jlink.ExprFormatException if the Expr cannot be represented as an integer (e.g., if it is a function)
	 * @see #asDouble()
	 * @see #asString()
	 * @see #asArray(int, int)
	 */
	
	public int asInt() throws ExprFormatException {
		prepareFromLoopback();
		switch (type) {
			case INTEGER:
				return ((Long) val).intValue();
			case BIGINTEGER:
				return ((BigInteger) val).intValue();
			default:
				throw new ExprFormatException("This Expr is of type " + typeToString() + " and cannot be represented as a Java int");
		}
	}

	/**
	 * Gives the long value for Exprs that can be represented as integers (this is exactly
	 * the set for which integerQ() returns true).
	 *
	 * @return the long value
	 * @exception com.wolfram.jlink.ExprFormatException if the Expr cannot be represented as a long (e.g., if it is a function)
	 * @see #asDouble()
	 * @see #asString()
	 * @see #asArray(int, int)
	 */
	
	public long asLong() throws ExprFormatException {
		prepareFromLoopback();
		switch (type) {
			case INTEGER:
				return ((Long) val).longValue();
			case BIGINTEGER:
				return ((BigInteger) val).longValue();
			default:
				throw new ExprFormatException("This Expr is of type " + typeToString() + " and cannot be represented as a Java long");
		}
	}

	/**
	 * Gives the double value for Exprs that can be represented as doubles (this is exactly
	 * the set for which integerQ() or realQ() or rationalQ() returns true).
	 *
	 * @return the double value
	 * @exception com.wolfram.jlink.ExprFormatException if the Expr cannot be represented as a double (e.g., if it is a function)
	 * @see #asInt()
	 * @see #asArray(int, int)
	 * @see #asString()
	 */
	
	public double asDouble() throws ExprFormatException {
		prepareFromLoopback();
		switch (type) {
			case INTEGER:
			case REAL:
				return ((Number) val).doubleValue();
			case BIGINTEGER:
				return ((BigInteger) val).doubleValue();
            case BIGDECIMAL:
                return ((BigDecimal) val).doubleValue();
            case NEWBIGDECIMAL:
                // Here we need the BigDecimal form, so create and cache it if not done already.
                if (bigDecimalForm == null)
                    bigDecimalForm = Utils.bigDecimalFromString((String) val);
                return bigDecimalForm.doubleValue();
			case RATIONAL:
				return args[0].asDouble()/args[1].asDouble();
			default:
				throw new ExprFormatException("This Expr is of type " + typeToString() + " and cannot be represented as a Java double");
		}
	}

	/**
	 * Gives the string value for Exprs that can be represented as strings (this is exactly
	 * the set for which stringQ() or symbolQ() returns true). Do not confuse this method
	 * with toString(), which returns a string representation of <i>any</i> Expr in FullForm style.
	 *
	 * @return the string value
	 * @exception com.wolfram.jlink.ExprFormatException if the Expr cannot be represented as a string (e.g., if it is a function)
	 * @see #toString()
	 * @see #asInt()
	 * @see #asDouble()
	 * @see #asArray(int, int)
	 */
	
	public String asString() throws ExprFormatException {
		prepareFromLoopback();
		if (type != STRING && type != SYMBOL)
			throw new ExprFormatException("This Expr is of type " + typeToString() + " and cannot be represented as a Java String");
		return (String) val;
	}

	/**
	 * Gives the BigInteger value for Exprs that can be represented as BigIntegers (this is exactly
	 * the set for which integerQ() or realQ() returns true). The number will be truncated if it is a real.
	 *
	 * @return the BigInteger value
	 * @exception com.wolfram.jlink.ExprFormatException if the Expr cannot be represented as a BigInteger (e.g., if it is a function)
	 * @see #asLong()
	 * @see #asDouble()
	 * @see #asBigDecimal()
	 */
	
	public BigInteger asBigInteger() throws ExprFormatException {
		prepareFromLoopback();
		switch (type) {
			case REAL:
			case INTEGER:
				return BigInteger.valueOf(((Number) val).longValue());
			case BIGINTEGER:
				return (BigInteger) val;
			case BIGDECIMAL:
				return ((BigDecimal) val).toBigInteger();
            case NEWBIGDECIMAL:
                // Here we need the BigDecimal form, so create and cache it if not done already.
                if (bigDecimalForm == null)
                    bigDecimalForm = Utils.bigDecimalFromString((String) val);
                return bigDecimalForm.toBigInteger();
			default:
				throw new ExprFormatException("This Expr is of type " + typeToString() + " and cannot be represented as a Java BigInteger");
		}
	}

	/**
	 * Gives the BigDecimal value for Exprs that can be represented as BigDecimals (this is exactly
	 * the set for which integerQ() or realQ() returns true).
	 *
	 * @return the BigDecimal value
	 * @exception com.wolfram.jlink.ExprFormatException if the Expr cannot be represented as a BigDecimal (e.g., if it is a function)
	 * @see #asLong()
	 * @see #asDouble()
	 * @see #asBigInteger()
	 */
	
	public BigDecimal asBigDecimal() throws ExprFormatException {
		prepareFromLoopback();
		switch (type) {
			case REAL:
				return new BigDecimal(((Double) val).doubleValue());
			case INTEGER:
				return BigDecimal.valueOf(((Long) val).longValue());
			case BIGINTEGER:
				return new BigDecimal((BigInteger) val);
			case BIGDECIMAL:
				return (BigDecimal) val;
            case NEWBIGDECIMAL:
                // Here we need the BigDecimal form, so create and cache it if not done already.
                if (bigDecimalForm == null)
                    bigDecimalForm = Utils.bigDecimalFromString((String) val);
                return bigDecimalForm;
			default:
				throw new ExprFormatException("This Expr is of type " + typeToString() + " and cannot be represented as a Java BigInteger");
		}
	}

	/**
	 * Gives a Java array representation with the requested depth and element type. The element
	 * type must be either INTEGER or REAL, and the current maximum depth is 2. It will throw a
	 * MathLinkException if this Expr does not represent a rectangular array of the specified depth,
	 * if the elements of the Expr are not of the specified type, or if the heads are not "List" at every level.
	 * <pre>
	 *     try {
	 *         int[][] a = (int[][]) e.asArray(Expr.INTEGER, 2);
	 *         // ... now work with a
	 *     } catch (ExprFormatException exc) {
	 *         // e was not a depth-2 array of integers
	 *     }</pre>
	 * 
	 * @param reqType an integer constant representing the requested type
	 * @param depth the depth (number of dimensions) of the returned array. Currently the max is 2.
	 * @return the array value
	 * @exception com.wolfram.jlink.ExprFormatException if the Expr cannot be represented as an array of the
	 * desired type (e.g., if it is an integer, or if it is not the right shape)
	 * @exception IllegalArgumentException if type is not INTEGER or REAL, or depth > 2
	 * @see #asInt()
	 * @see #asDouble()
	 * @see #asString()
	 */
	
	public Object asArray(int reqType, int depth) throws ExprFormatException {

		prepareFromLoopback();
		if (depth > 2)
			throw new IllegalArgumentException("Depths > 2 are not supported in Expr.asArray()");
		if (reqType != INTEGER && reqType != REAL)
			throw new IllegalArgumentException("Unsupported type in Expr.asArray(): " + reqType);
		switch (type) {
			case INTARRAY1: {
				if (depth != 1 || reqType != INTEGER)
					throw new ExprFormatException("This Expr is of type " + typeToString() + " and cannot be represented as a Java array of the requested type and depth");
				// Note the defensive copying of arrays to preserve immutability of Exprs (item 24 in "Effective Java").
				return (int[]) ((int[]) val).clone();
			}
			case REALARRAY1: {
				if (depth != 1 || reqType != REAL)
					throw new ExprFormatException("This Expr is of type " + typeToString() + " and cannot be represented as a Java array of the requested type and depth");
				return (double[]) ((double[]) val).clone();
			}
			case INTARRAY2: {
				if (depth != 2 || reqType != INTEGER)
					throw new ExprFormatException("This Expr is of type " + typeToString() + " and cannot be represented as a Java array of the requested type and depth");
				int[][] ia = new int[((int[][]) val).length][];
				for (int i = 0; i < ia.length; i++)
					ia[i] = (int[]) ((int[][]) val)[i].clone();
				return ia;
			}
			case REALARRAY2: {
				if (depth != 2 || reqType != REAL)
					throw new ExprFormatException("This Expr is of type " + typeToString() + " and cannot be represented as a Java array of the requested type and depth");
				double[][] da = new double[((double[][]) val).length][];
				for (int i = 0; i < da.length; i++)
					da[i] =(double[]) ((double[][]) val)[i].clone();
				return da;
			}
			case FUNCTION: {
				if (depth == 1) {
					if (reqType == INTEGER) {
						int[] ia = new int[args.length];
						for (int i = 0; i < args.length; i++) {
							if (!args[i].integerQ())
								throw new ExprFormatException("This Expr cannot be represented as a Java array of ints because some elements are not integers");
							ia[i] = args[i].asInt();
						}
						return ia;
					} else {
						// reqType will be REAL
						double[] da = new double[args.length];
						for (int i = 0; i < args.length; i++) {
							if (!args[i].realQ() && !args[i].integerQ())
								throw new ExprFormatException("This Expr cannot be represented as a Java array of doubles because some elements are not real numbers");
							da[i] = args[i].asDouble();
						}
						return da;
					}
				} else {
					// depth will be 2.
					if (reqType == INTEGER) {
						int[][] iaa = new int[args.length][];
						for (int i = 0; i < args.length; i++)
							iaa[i] = (int[]) args[i].asArray(reqType, depth-1);
						return iaa;
					} else {
						// reqType will be REAL
						double[][] daa = new double[args.length][];
						for (int i = 0; i < args.length; i++)
							daa[i] = (double[]) args[i].asArray(reqType, depth-1);
						return daa;
					}
				}
			}
			default:
				throw new ExprFormatException("This Expr is of type " + typeToString() + " and cannot be represented as a Java array of the requested type and depth");
		}
	}

	/**
	 * Not intended for general use. To write an Expr on a link, use the MathLink put(Expr)
	 * method. This method is only public because developers of MathLink implementations
	 * must call it inside their put(Object) methods if the object's type is Expr.
	 * 
	 * @param ml
	 * @exception com.wolfram.jlink.MathLinkException
	 */
	
	public synchronized void put(MathLink ml) throws MathLinkException {
		
		if (link != null) {
			long mark = link.createMark();
			try {
				ml.transferExpression(link);
			} finally {
				ml.clearError();  // probably not actually an error state here
				link.seekMark(mark);
				link.destroyMark(mark);
			}
		} else {
			if (val != null) {
				if (type == SYMBOL) {
					ml.putSymbol((String) val);
				} else if (type == NEWBIGDECIMAL) {
				    // For NEW BIGDECIMAL, send the original string form of the number, not the BigDecimal object,
				    // to exactly preserve the original number.
	                ml.putNext(MathLink.MLTKREAL);
	                ml.putSize(((String) val).length());
	                ml.putData(((String) val).getBytes());
				} else {
				    ml.put(val);
				}
			} else {
				ml.putNext(MathLink.MLTKFUNC);
				ml.putArgCount(nonCopyingArgs().length);
				ml.put(head());
				for (int i = 0; i < args.length; i++)
					ml.put(args[i]);
			}
		}
	}


	///////////////////  Modifiers  ////////////////////
	
	/**
	 * Returns a new Expr that has the same head but only the first n elements of this Expr
	 * (or last n elements if n is negative). Works like the Mathematica function Take.
	 * 
	 * @param n the number of elements to take from the beginning (or end if n is negative).
	 * @return the shortened Expr.
	 * @throws IllegalArgumentException if n is beyond the bounds of the expression.
	 */
	public Expr take(int n) {
		
		int num = Math.abs(n);
		int curLen = nonCopyingArgs().length;
		if (num > curLen)
			throw new IllegalArgumentException("Cannot take " + n + " elements from this Expr because it has length " + curLen + ".");
		Expr[] newArgs = new Expr[num];
		if (n >= 0)
			System.arraycopy(args, 0, newArgs, 0, num);
		else
			System.arraycopy(args, curLen - num, newArgs, 0, num);
		return new Expr(head, newArgs);
	}

	/**
	 * Returns a new Expr that has the same head but the nth element deleted (counted from the
	 * end if n is negative). Works like the Mathematica function Delete.
	 * 
	 * @param n the index of the element to delete (counted from the end if n is negative).
	 * @return the shortened Expr.
	 * @throws IllegalArgumentException if n is beyond the bounds of the expression.
	 */
	public Expr delete(int n) {
		
		int curLen = nonCopyingArgs().length;
		if (n == 0 || Math.abs(n) > curLen)
			throw new IllegalArgumentException(n + " is an invalid deletion position in this Expr.");
		Expr[] newArgs = new Expr[curLen - 1];
		if (n > 0) {
			System.arraycopy(args, 0, newArgs, 0, n - 1);
			System.arraycopy(args, n, newArgs, n - 1, curLen - n);
		} else {
			System.arraycopy(args, 0, newArgs, 0, curLen + n);
			System.arraycopy(args, curLen + n + 1, newArgs, curLen + n, -n - 1);
		}
		return new Expr(head, newArgs);
	}
	
	/**
	 * Returns a new Expr that has the same head but with e inserted into position n (counted from the
	 * end if n is negative). Works like the Mathematica function Insert.
	 * 
	 * @param e the element to insert.
	 * @param n the index at which to perform the insertion (counted from the end if n is negative).
	 * @return the new Expr.
	 * @throws IllegalArgumentException if n is beyond the bounds of the expression.
	 */
	public Expr insert(Expr e, int n) {
		
		int curLen = nonCopyingArgs().length;
		if (n == 0 || Math.abs(n) > curLen + 1)
			throw new IllegalArgumentException(n + " is an invalid insertion position into this Expr.");
		Expr[] newArgs = new Expr[curLen + 1];
		if (n > 0) {
			System.arraycopy(args, 0, newArgs, 0, n - 1);
			newArgs[n - 1] = e;
			System.arraycopy(args, n - 1, newArgs, n, curLen - (n - 1));
		} else {
			System.arraycopy(args, 0, newArgs, 0, curLen + n + 1);
			newArgs[curLen + n + 1] = e;
			System.arraycopy(args, curLen + n + 1, newArgs, curLen + n + 2, -n - 1);
		}
		return new Expr(head, newArgs);
	}
	
		
	////////////////////////////////////////  Private Methods  /////////////////////////////////////////////
	
	
	// Clients must use Q methods; can never inspect raw type.
	private int type() {
		
		prepareFromLoopback();
		return type;
	}
	
	// Important to ensure that on exit the conditions are set so that it will never
	// be called again, even if it fails. Right now, this means just setting link to null.
	private synchronized void prepareFromLoopback() {
		
		if (link != null) {
			try {
				fillFromLink(link);
			} catch (MathLinkException e) {
				// This should never happen. An exception should have been thrown when transferring
				// onto loopback from the native link.
				if (MathLinkImpl.DEBUGLEVEL > 0) System.err.println("MathLinkException reading Expr from loopback.");
			} finally {
				link.close();
				link = null;
			}
		}
	}

	// Fills out the fields of an existing Expr by reading from a link (typically, but not always, this link is
	// the Expr's own loopback link that was first used to store its contents).
	// Up to the caller to ensure that link != null.
	private synchronized void fillFromLink(MathLink ml) throws MathLinkException {
		
		int mlType = ml.getType();  // Not getNext() here.
		if (mlType == MathLink.MLTKFUNC) {
			try {
				int argc = ml.getArgCount();
				head = createFromLink(ml, false);
				if (head.type == SYMBOL && head.val.equals("Rational")) {
					type = RATIONAL;
					args = new Expr[2];
					args[0] = createFromLink(ml, false);
					args[1] = createFromLink(ml, false);
				} else if (head.type == SYMBOL && head.val.equals("Complex")) {
					type = COMPLEX;
					args = new Expr[2];
					args[0] = createFromLink(ml, false);
					args[1] = createFromLink(ml, false);
				} else {
					// Do the full Expr form for all args.
					type = FUNCTION;
					args = new Expr[argc];
					for (int i = 0; i < argc; i++)
						args[i] = createFromLink(ml, false);
				}
			} catch (MathLinkException e) {
				// This branch only entered when an illegal expression was on the link.
				if (MathLinkImpl.DEBUGLEVEL > 0) System.err.println("MathLinkException reading Expr from link: " + e.toString());
				throw e;
			} finally {
				ml.clearError();
			}
		} else if (mlType == MathLink.MLTKINT || mlType == MathLink.MLTKREAL ||
				mlType == MathLink.MLTKSTR || mlType == MathLink.MLTKSYM) {
			// Atomic types should never be encountered by fillFromLink. They should be detected in readFromLink and
			// routed through createAtomicExpr. 
			if (MathLinkImpl.DEBUGLEVEL > 0) System.err.println("Atomic type in fillFromLink: " + type);
		} else {
			if (MathLinkImpl.DEBUGLEVEL > 0) System.err.println("Unexpected type in fillFromLink: " + type);
		}
	}

	
	// Factory method that reads an expression from the link and returns a corresponding Expr.
    private static Expr createFromLink(MathLink ml, boolean allowLoopback) throws MathLinkException {

        int type = ml.getType();
        // We don't bother to ever use a loopback link to hold atomic expressions.
        if (type == MathLink.MLTKINT || type == MathLink.MLTKREAL || type == MathLink.MLTKSTR || type == MathLink.MLTKSYM) {
            return createAtomicExpr(ml, type);
        } else {
            Expr result = new Expr();
            // This test is "will an attempt to use a loopback link NOT cause the native library to be loaded
            // for the first time?" We want to allow Expr operations to remain "pure Java" as much as possible,
            // so they can be performed on platforms for which no native library is available (e.g., handhelds).
            // We also only use a loopback link if the link we are reading from is a NativeLink. This is because
            // transferExpression() when reading from a non-NativeLink is wrriten in terms of getExpr(), which calls
            // right back here, and we get infinite recursion. 
            if (allowLoopback && NativeLink.nativeLibraryLoaded &&
                    (ml instanceof NativeLink || (ml instanceof WrappedKernelLink &&
                            ((WrappedKernelLink) ml).getMathLink() instanceof NativeLink))) {
                result.link = MathLinkFactory.createLoopbackLink();
                result.link.transferExpression(ml);
                result.type = UNKNOWN;
            } else {
                result.fillFromLink(ml);
            }
            return result;
        }
    }
    

	private static Expr createAtomicExpr(MathLink ml, int type) throws MathLinkException {
		
		Expr result = null;
		switch (type) {
			case MathLink.MLTKINT: {
				String s = ml.getString();
				// Reuse cached instances for common ints.
				if (s.equals("0"))
					result = INT_ZERO;
				else if (s.equals("1"))
					result = INT_ONE;
				else if (s.equals("-1"))
					result = INT_MINUSONE;
				else {
					result = new Expr();
					result.head = SYM_INTEGER;
					try {
						result.val = new Long(s);
						result.type = INTEGER;
					} catch (NumberFormatException e) {
						result.val = new BigInteger(s);
						result.type = BIGINTEGER;
					}
				}
				break;
			}
			case MathLink.MLTKREAL: {
				result = new Expr();
				result.head = SYM_REAL;
				// If we call getDouble() here, MathLink will return a double that may have been truncated. Thus
				// we get the data as a string and interpret it ourselves.
				String s = ml.getString();
				try {
					result.val = new Double(s);
					result.type = REAL;
				} catch (NumberFormatException e) {
					// Will get here if number has too many digits (even if its magnitude is small), or if the number
					// has some InputForm gunk in it (like `). In this latter case, the number might be representable as
					// a double, but we don't bother trying.
					result.val = s;
					result.type = NEWBIGDECIMAL;
				}
				break;
			}
			case MathLink.MLTKSTR: {
				result = new Expr();
				result.type = STRING;
				result.head = SYM_STRING;
				result.val = ml.getString();
				break;
			}
			case MathLink.MLTKSYM: {
				String sym = ml.getSymbol();
				if (sym.equals("List")) {
					result = SYM_LIST;
				} else if (sym.equals("True")) {
					result = SYM_TRUE;
				} else if (sym.equals("False")) {
					result = SYM_FALSE;
				} else {
					result = new Expr();
					result.type = SYMBOL;
					result.head = SYM_SYMBOL;
					result.val = sym;
				}
				break;
			}
			default:
				if (MathLinkImpl.DEBUGLEVEL > 0) System.err.println("Bad type passed to createAtomicExpr");
		}
		return result;
	}
	
	// For internal calls to args(), we don't need the defensive copying in the one that is public.
	// That's the reason for this separate method.
	private synchronized Expr[] nonCopyingArgs() {
		
		prepareFromLoopback();
		if (args == null) {
			if (type < FIRST_COMPOSITE) {
				// Flesh out args as empty array.
				args = new Expr[0];
			} else if (type >= FIRST_ARRAY_TYPE) {
				// args not used until now; val instead. Must now create Expr form for args array.
				args = new Expr[Array.getLength(val)];
				for (int i = 0; i < args.length; i++) {
					switch (type) {
						case INTARRAY1:
							args[i] = new Expr(((int[]) val)[i]);
							break;
						case INTARRAY2:
							args[i] = new Expr(((int[][]) val)[i]);
							break;
						case REALARRAY1:
							args[i] = new Expr(((double[]) val)[i]);
							break;
						case REALARRAY2:
							args[i] = new Expr(((double[][]) val)[i]);
							break;
						default:
							if (MathLinkImpl.DEBUGLEVEL > 0) System.err.println("Bad type in Expr.args()");
					}
				}
			}
		}
		return args;
	}

	
	// Returns the deepest level to which all subexprs have the given head. Checks down to at most level maxDepth.
	// This must return a number > 0 (0 would mean that the Expr did not have the same head as itself). Returning
	// 1 means that none of the top-level children have the same head as the Expr itself.
	private int checkHeads(String head, int curDepth, int maxDepth) {
		
		if (args == null || curDepth > maxDepth || !head().toString().equals(head))
			return curDepth;
		curDepth++;
		for (int i = 0; i < args.length; i++) {
			int thisArgDepth = args[i].checkHeads(head, curDepth, maxDepth);
			if (thisArgDepth < maxDepth)
				maxDepth = thisArgDepth;
		}
		return maxDepth;
	}
	
	
	private String typeToString() {
		
		prepareFromLoopback();
		switch (type) {
			case INTEGER:
				return "INTEGER";
			case SYMBOL:
				return "SYMBOL";
			case BIGINTEGER:
				return "BIGINTEGER";
            case BIGDECIMAL:
            case NEWBIGDECIMAL: // This method is only used for error messages, and we want to hide the difference between the two bigdecimal types.
				return "BIGDECIMAL";
			case REAL:
				return "REAL";
			case STRING:
				return "STRING";
			case RATIONAL:
				return "RATIONAL";
			case COMPLEX:
				return "COMPLEX";
			case FUNCTION:
				return "FUNCTION";
			case INTARRAY1:
				return "INTARRAY1D";
			case REALARRAY1:
				return "REALARRAY1D";
			case INTARRAY2:
				return "INTARRAY2D";
			case REALARRAY2:
				return "REALARRAY2D";
			default:
				return "BAD TYPE";
		}
	}

	
    // Double.toString() can return strs with E notation. This fixes these cases.
    private static String doubleToInputFormString(double d) {
        
        String s = Double.toString(d);
        int epos = s.lastIndexOf('E');
        if (epos == -1)
            return s;
        else
            return s.substring(0, epos) + "*^" + s.substring(epos + 1);
    }

    // BigDecimal.toString() can return strs with E notation, starting in Java 1.5. This fixes these cases.
    // Using toPlainString() produces nice strings without any E notation, but it consumes
    // vast amounts of memory for numbers with huge scale (like $MaxNumber, which causes an
    // OutOfMemoryError [bug 202501]).
    private static String bigDecimalToInputFormString(BigDecimal bd) {
        
        String s = bd.toString();
        int ePos = s.indexOf('E');
        if (ePos == -1) {
            return s;
        } else {
            // If there is an E, we want to replace it with *^. But we can also get strings with
            // no decimal point, like 9E-3, which would get converted to 9*^-3, which becomes the
            // rational number 9/1000 in M, so we add a decimal point if there isn't one.
            int decimalPos = s.indexOf('.');
            if (decimalPos == -1 || decimalPos > ePos)
                return s.replace("E", ".*^");
            else
                return s.replace("E", "*^");
        }
    }

}

