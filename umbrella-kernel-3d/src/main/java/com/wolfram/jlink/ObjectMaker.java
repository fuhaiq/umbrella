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

// Used by the Mathematica function MakeJavaObject. To implement MakeJavaObject for arrays (it's trivial to
// do within Mathematica for the other types), all we need are some trivial methods that make one round trip
// through the standard J/Link get/return machinery. Most of these methods are not used by MakeJavaObject as
// defined in JLink.m. Users can call them directly, though, if they want a non-standard array type (such as
// byte or short instead of int). That is, MakeJavaObject[{1,2,3}] constructs an int[], but if you want to
// construct a byte[] instead, you can do:
//    LoadClass["com.wolfram.jlink.ObjectMaker"];
//    myByteArray = ReturnAsJavaObject [ObjectMaker`makeByteArray[{1,2,3}]];

public class ObjectMaker {

	public static byte[] makeByteArray(byte[] a) { return a; }
	public static byte[][] makeByteArray2(byte[][] a) { return a; }
	public static byte[][][] makeByteArray3(byte[][][] a) { return a; }
	
	public static char[] makeCharArray(char[] a) { return a; }
	public static char[][] makeCharArray2(char[][] a) { return a; }
	public static char[][][] makeCharArray3(char[][][] a) { return a; }
	
	public static short[] makeShortArray(short[] a) { return a; }
	public static short[][] makeShortArray2(short[][] a) { return a; }
	public static short[][][] makeShortArray3(short[][][] a) { return a; }
	
	public static int[] makeIntArray(int[] a) { return a; }
	public static int[][] makeIntArray2(int[][] a) { return a; }
	public static int[][][] makeIntArray3(int[][][] a) { return a; }
	
	public static long[] makeLongArray(long[] a) { return a; }
	public static long[][] makeLongArray2(long[][] a) { return a; }
	public static long[][][] makeLongArray3(long[][][] a) { return a; }
	
	public static float[] makeFloatArray(float[] a) { return a; }
	public static float[][] makeFloatArray2(float[][] a) { return a; }
	public static float[][][] makeFloatArray3(float[][][] a) { return a; }
	
	public static double[] makeDoubleArray(double[] a) { return a; }
	public static double[][] makeDoubleArray2(double[][] a) { return a; }
	public static double[][][] makeDoubleArray3(double[][][] a) { return a; }
	
	public static String[] makeStringArray(String[] a) { return a; }
	public static String[][] makeStringArray2(String[][] a) { return a; }
	public static String[][][] makeStringArray3(String[][][] a) { return a; }
	
	public static boolean[] makeBooleanArray(boolean[] a) { return a; }
	public static boolean[][] makeBooleanArray2(boolean[][] a) { return a; }
	public static boolean[][][] makeBooleanArray3(boolean[][][] a) { return a; }
	
	public static Object[] makeObjectArray(Object[] a) { return a; }
	public static Object[][] makeObjectArray2(Object[][] a) { return a; }
	public static Object[][][] makeObjectArray3(Object[][][] a) { return a; }
	
	public static Expr makeExpr(Expr e) { return e; }

}
