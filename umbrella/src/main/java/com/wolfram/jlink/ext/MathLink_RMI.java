package com.wolfram.jlink.ext;

import com.wolfram.jlink.*;

import java.rmi.*;


public class MathLink_RMI extends MathLinkImpl {

	private RemoteMathLink remoteLink;
	
	//////////////////////////////////Constructors ////////////////////////////////////////

	public MathLink_RMI(String cmdLine)
			throws NotBoundException, java.net.MalformedURLException, UnknownHostException, RemoteException {

		remoteLink = (RemoteMathLink) Naming.lookup(Utils.determineLinkname(cmdLine));
	}

	public MathLink_RMI(String[] argv)
			throws NotBoundException, java.net.MalformedURLException, UnknownHostException, RemoteException {

		remoteLink = (RemoteMathLink) Naming.lookup(Utils.determineLinkname(argv));
	}
	
	////////////////////////////////////Public methods////////////////////////////////////////

	public synchronized void close() {

		try {
			remoteLink.close();
		} catch (RemoteException e) {
		}
	}


	public synchronized void connect() throws MathLinkException {

		try {
			remoteLink.connect();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}


	public synchronized void activate() throws MathLinkException {

		connect();
	}

    // TODO: Could come up with a more meaningful name!
	public String name() {
	    return "";
    }
    
    
	public synchronized void newPacket() {

		try {
			remoteLink.newPacket();
		} catch (RemoteException e) {
		}
	}

	public synchronized int nextPacket() throws MathLinkException {

		try {
			return remoteLink.nextPacket();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized void endPacket() throws MathLinkException {

		try {
			remoteLink.endPacket();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized int error() {

		try {
			return remoteLink.error();
		} catch (RemoteException e) {
			return MLE_NON_ML_ERROR;
		}
	}

	public synchronized boolean clearError() {

		try {
			return remoteLink.clearError();
		} catch (RemoteException e) {
			return false;
		}
	}

	public synchronized String errorMessage() {

		try {
			return remoteLink.errorMessage();
		} catch (RemoteException e) {
			return "RMI RemoteException occurred: " + e.toString();
		}
	}


	public synchronized void setError(int err) {

		try {
			remoteLink.setError(err);
		} catch (RemoteException e) {
		}
	}


	public synchronized boolean ready() throws MathLinkException {

		try {
			return remoteLink.ready();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	// Doesn't throw. In some sense, a cleanup function, particularly in my "polling" model.
	public synchronized void flush() throws MathLinkException {

		try {
			remoteLink.flush();
		} catch (RemoteException e) {
            throw new MathLinkException(e);
		}
	}


	public synchronized int getNext() throws MathLinkException {

		try {
			return remoteLink.getNext();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized int getType() throws MathLinkException {

		try {
			return remoteLink.getType();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized void putNext(int type) throws MathLinkException {

		try {
			remoteLink.putNext(type);
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized int getArgCount() throws MathLinkException {

		try {
			return remoteLink.getArgCount();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized void putArgCount(int argCount) throws MathLinkException {

		try {
			remoteLink.putArgCount(argCount);
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}


	public synchronized void putSize(int size) throws MathLinkException {

		try {
			remoteLink.putSize(size);
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}


	public synchronized int bytesToPut() throws MathLinkException {

		try {
			return remoteLink.bytesToPut();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}


	public synchronized int bytesToGet() throws MathLinkException {

		try {
			return remoteLink.bytesToGet();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}


	public synchronized void putData(byte[] data, int len) throws MathLinkException {

		try {
			remoteLink.putData(data, len);
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}


	public synchronized byte[] getData(int len) throws MathLinkException {

		try {
			return remoteLink.getData(len);
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}


	public synchronized String getString() throws MathLinkException {

		try {
			return remoteLink.getString();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized byte[] getByteString(int missing) throws MathLinkException {

		try {
			return remoteLink.getByteString(missing);
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized void putByteString(byte[] data) throws MathLinkException {

		try {
			remoteLink.putByteString(data);
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}


	public synchronized String getSymbol() throws MathLinkException {

		try {
			return remoteLink.getSymbol();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized void putSymbol(String s) throws MathLinkException {

		try {
			remoteLink.putSymbol(s);
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}


	public synchronized void put(boolean b) throws MathLinkException {

		try {
			remoteLink.put(b);
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized int getInteger() throws MathLinkException {

		try {
			return remoteLink.getInteger();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized void put(int i) throws MathLinkException {

		try {
			remoteLink.put(i);
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized long getLongInteger() throws MathLinkException {

		try {
			return remoteLink.getLongInteger();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized void put(long i) throws MathLinkException {

		try {
			remoteLink.put(i);
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized double getDouble() throws MathLinkException {

		try {
			return remoteLink.getDouble();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized void put(double d) throws MathLinkException {

		try {
			remoteLink.put(d);
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized MLFunction getFunction() throws MathLinkException {

		try {
			return remoteLink.getFunction();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized void putFunction(String f, int argCount) throws MathLinkException {

		try {
			remoteLink.putFunction(f, argCount);
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized int checkFunction(String f) throws MathLinkException {

		try {
			return remoteLink.checkFunction(f);
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized void checkFunctionWithArgCount(String f, int argCount) throws MathLinkException {

		try {
			remoteLink.checkFunctionWithArgCount(f, argCount);
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized void transferExpression(MathLink source) throws MathLinkException {

		try {
			remoteLink.put(source.getExpr());
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized void transferToEndOfLoopbackLink(LoopbackLink source) throws MathLinkException {

		try {
			while (source.ready()) {
				remoteLink.put(source.getExpr());
			}
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}
	
	public synchronized Expr getExpr() throws MathLinkException {
		
		try {
			return remoteLink.getExpr();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}
	
	public synchronized Expr peekExpr() throws MathLinkException {
		
		try {
			return remoteLink.peekExpr();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}
		
	public int getMessage() throws MathLinkException {

		try {
			return remoteLink.getMessage();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public void putMessage(int msg) throws MathLinkException {

		try {
			remoteLink.putMessage(msg);
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public boolean messageReady() throws MathLinkException {

		try {
			return remoteLink.messageReady();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}


	public synchronized long createMark() throws MathLinkException {

		try {
			return remoteLink.createMark();
		} catch (RemoteException e) {
			throw new MathLinkException(e);
		}
	}

	public synchronized void seekMark(long mark) {

		try {
			remoteLink.seekMark(mark);
		} catch (RemoteException e) {
		}
	}

	public synchronized void destroyMark(long mark) {

		try {
			remoteLink.destroyMark(mark);
		} catch (RemoteException e) {
		}
	}

	// Object you put must be Serializable.
	public synchronized void put(Object obj) throws MathLinkException {

		try {
			remoteLink.put(obj);
		} catch (Exception e) {
			// Could be a NotSerializableException in addition to a RemoteException
			throw new MathLinkException(e);
		}
	}

	// Object you put must be Serializable.
	public synchronized void put(Object obj, String[] heads) throws MathLinkException {

		try {
			remoteLink.put(obj, heads);
		} catch (Exception e) {
			// Could be a NotSerializableException in addition to a RemoteException
			throw new MathLinkException(e);
		}
	}
	
	public synchronized boolean setYieldFunction(Class cls, Object target, String methName) {

		boolean res = super.setYieldFunction(cls, target, methName);
		if (res)
			;
			// Do something RMI-specific to enable a callback from the other side. Everything
			// on this side has already been taken care of.
			//MLSetYieldFunction(link, env.getEnv());
		return res;
	}

	// The signature of the meth must be (JJ)V. Can be static; pass null as target.
	public synchronized boolean addMessageHandler(Class cls, Object target, String methName) {

		boolean result = super.addMessageHandler(cls, target, methName);
		if (result)
			;
			// Do something RMI-specific to enable a callback from the other side. Everything
			// on this side has already been taken care of.
			//MLSetMessageHandler(link, env.getEnv());
		return result;
	}
	
    public synchronized Object getArray(int type, int depth) throws MathLinkException {
        return getArray(type, depth, null);
    }

    public synchronized Object getArray(int type, int depth, String[] heads) throws MathLinkException {

        try {
            return remoteLink.getArray(type, depth, heads);
        } catch (Exception e) {
            throw new MathLinkException(e);
        }
    }

	///////////////////////////////////////////////////////////////////////////////////////////////////

	/* Destructor */

	protected void finalize() throws Throwable {

		super.finalize();
		close();
	}

	////////////////////////////////////////////////////////////////////////////////////////
	
	// This needs to be defined, but it is never called because this class has its own
	// implementation of put(Object, String[]). putArray is called from the MathLinkImpl
	// version of put(Object, String[]).
	protected void putArray(Object obj, String[] heads) throws MathLinkException {}

    
    protected void putString(String s) throws MathLinkException {
        try {
            remoteLink.sendString(s);
        } catch (Exception e) {
            throw new MathLinkException(e);
        }
    }
    
}