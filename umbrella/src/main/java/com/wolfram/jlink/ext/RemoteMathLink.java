package com.wolfram.jlink.ext;

import java.rmi.*;
import com.wolfram.jlink.*;

public interface RemoteMathLink extends Remote {

    public void close() throws RemoteException;
	public void connect() throws MathLinkException, RemoteException;
	public void activate() throws MathLinkException, RemoteException;
	public void newPacket() throws RemoteException;
	public int nextPacket() throws MathLinkException, RemoteException;
	public void endPacket() throws MathLinkException, RemoteException;
	public int error() throws RemoteException;
	public boolean clearError() throws RemoteException;
	public String errorMessage() throws RemoteException;
	public void setError(int err) throws RemoteException;
	public boolean ready() throws MathLinkException, RemoteException;
	public void flush() throws MathLinkException, RemoteException;
	public int getNext() throws MathLinkException, RemoteException;
	public int getType() throws MathLinkException, RemoteException;
	public void putNext(int type) throws MathLinkException, RemoteException;
	public int getArgCount() throws MathLinkException, RemoteException;
	public void putArgCount(int argCount) throws MathLinkException, RemoteException;
	public void putSize(int size) throws MathLinkException, RemoteException;
	public int bytesToPut() throws MathLinkException, RemoteException;
	public int bytesToGet() throws MathLinkException, RemoteException;
	public void putData(byte[] data) throws MathLinkException, RemoteException;
	public void putData(byte[] data, int len) throws MathLinkException, RemoteException;
	public byte[] getData(int len) throws MathLinkException, RemoteException;
	public String getString() throws MathLinkException, RemoteException;
	public byte[] getByteString(int missing) throws MathLinkException, RemoteException;
	public void putByteString(byte[] s) throws MathLinkException, RemoteException;
	public String getSymbol() throws MathLinkException, RemoteException;
    public void putSymbol(String s) throws MathLinkException, RemoteException;
    public void sendString(String s) throws MathLinkException, RemoteException;
	public void put(boolean b) throws MathLinkException, RemoteException;
	public int getInteger() throws MathLinkException, RemoteException;
	public void put(int i) throws MathLinkException, RemoteException;
	public long getLongInteger() throws MathLinkException, RemoteException;
	public void put(long i) throws MathLinkException, RemoteException;
	public double getDouble() throws MathLinkException, RemoteException;
	public void put(double d) throws MathLinkException, RemoteException;
	public boolean[] getBooleanArray1() throws MathLinkException, RemoteException;
	public boolean[][] getBooleanArray2() throws MathLinkException, RemoteException;
	public byte[] getByteArray1() throws MathLinkException, RemoteException;
	public byte[][] getByteArray2() throws MathLinkException, RemoteException;
	public char[] getCharArray1() throws MathLinkException, RemoteException;
	public char[][] getCharArray2() throws MathLinkException, RemoteException;
	public short[] getShortArray1() throws MathLinkException, RemoteException;
	public short[][] getShortArray2() throws MathLinkException, RemoteException;
	public int[] getIntArray1() throws MathLinkException, RemoteException;
	public int[][] getIntArray2() throws MathLinkException, RemoteException;
	public long[] getLongArray1() throws MathLinkException, RemoteException;
	public long[][] getLongArray2() throws MathLinkException, RemoteException;
	public float[] getFloatArray1() throws MathLinkException, RemoteException;
	public float[][] getFloatArray2() throws MathLinkException, RemoteException;
	public double[] getDoubleArray1() throws MathLinkException, RemoteException;
	public double[][] getDoubleArray2() throws MathLinkException, RemoteException;
	public String[] getStringArray1() throws MathLinkException, RemoteException;
	public String[][] getStringArray2() throws MathLinkException, RemoteException;
	public Object[] getComplexArray1() throws MathLinkException, RemoteException;
	public Object[][] getComplexArray2() throws MathLinkException, RemoteException;
    public Object getArray(int type, int depth, String[] heads) throws MathLinkException, RemoteException;
	public MLFunction getFunction() throws MathLinkException, RemoteException;
	public void putFunction(String f, int argCount) throws MathLinkException, RemoteException;
	public int checkFunction(String f) throws MathLinkException, RemoteException;
	public void checkFunctionWithArgCount(String f, int argCount) throws MathLinkException, RemoteException;
	public int getMessage() throws MathLinkException, RemoteException;
	public void putMessage(int msg) throws MathLinkException, RemoteException;
	public boolean messageReady() throws MathLinkException, RemoteException;
	public long createMark() throws MathLinkException, RemoteException;
	public void seekMark(long mark) throws RemoteException;
	public void destroyMark(long mark) throws RemoteException;
	
	// From here on down are the only methods whose args and return vals aren't trivially serializable.
	
	// Expr is serializable.
	public Expr getExpr() throws MathLinkException, RemoteException;	
	public Expr peekExpr() throws MathLinkException, RemoteException;

	public Object getComplex() throws MathLinkException, RemoteException;
	
	public void put(Object obj) throws MathLinkException, RemoteException;
	public void put(Object obj, String[] heads) throws MathLinkException, RemoteException;
	
	// Are there serializing/logic problems with these??
	public boolean setComplexClass(Class cls) throws RemoteException;
	public Class getComplexClass() throws RemoteException;
	public boolean setYieldFunction(Class cls, Object target, String methName) throws RemoteException;
	public boolean addMessageHandler(Class cls, Object target, String methName) throws RemoteException;
	public boolean removeMessageHandler(String methName) throws RemoteException;
}

