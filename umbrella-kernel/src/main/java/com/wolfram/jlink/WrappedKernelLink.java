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

import com.alibaba.fastjson.JSONArray;

// WrappedKernelLink is the only full implementation of KernelLink in J/Link. The idea is to implement KernelLink
// by "wrapping" a MathLink instance, which is responsible for all the transport-specific details. In other
// words, if you give me a MathLink implementation, I can create a KernelLink implementation by simply doing:
//
//    KernelLink kl = new WrappedKernelLink(theMathLink); 
//
// This is exactly what happens in MathLinkFactory when createKernelLink is called. In this way, new KernelLinks
// can be created that use new transport mechanisms (like RMI and HTTP instead of native MathLink) simply by writing
// a MathLink implementation that uses the mechanism and then supplying this MathLink instance to WrappedKernelLink.
// All of the KernelLink-specific implementation is inherited from KernelLinkImpl. The only thing here is the
// implementation of the MathLink interface by forwarding calls to a "wrapped" MathLink instance.
//
// The philosophy is to let the WrappedKernelLink class maintain user state associated with the link (the identity of
// the Complex class, the user yielder, the user message handler, etc.) This state is part of MathLink, so both
// the WrappedKernelLink and the wrapped MathLink could contain it. It is much more sensible to have it all
// held here and only forward non-state-dependent MathLink calls to the wrapped MathLink. For example, we don't
// forward putComplex() or getComplex().


public class WrappedKernelLink extends KernelLinkImpl implements KernelLink {

	protected MathLink impl;

	// Not a reliable indicator of connectedness. Just a simple optimization for nextPacket()
	// to allow it to not repeatedly try to connect.
	private boolean linkConnected = false;

	
	///////////////////////////////  Constructor  /////////////////////////////////

	public WrappedKernelLink() {
		this(null);
	}
	
	public WrappedKernelLink(MathLink ml) {
		setMathLink(ml);
		if (ml != null)
		    addMessageHandler(null, this, "msgHandler");
	}

	////////////////////////////////////////////////////////////////////////////
	
	public MathLink getMathLink() {
		return impl;
	}

	public void setMathLink(MathLink ml) {
		impl = ml;
	}

	//////////////////  Implementation of MathLink Interface by wrapping  /////////////////////

	public synchronized void close() {
	 	impl.close();
	}
    public synchronized void connect() throws MathLinkException {
        impl.connect();
    }
    public synchronized String name() throws MathLinkException {
        return impl.name();
    }
	// No signature needed for connect(long timeoutMillis);
	public synchronized void newPacket() {
	 	impl.newPacket();
	}
	public synchronized void endPacket() throws MathLinkException {
	 	impl.endPacket();
	}
	public synchronized int error() {
	 	return impl.error();
	}
	public synchronized boolean clearError() {
	 	return impl.clearError();
	}
	public synchronized String errorMessage() {
	 	return impl.errorMessage();
	}
	public synchronized void setError(int err) {
	 	impl.setError(err);
	}
	public synchronized boolean ready() throws MathLinkException {
	 	return impl.ready();
	}
	public synchronized void flush() throws MathLinkException {
	 	impl.flush();
	}
	public synchronized void putNext(int type) throws MathLinkException {
	 	impl.putNext(type);
	}
	public synchronized int getArgCount() throws MathLinkException {
	 	return impl.getArgCount();
	}
	public synchronized void putArgCount(int argCount) throws MathLinkException {
	 	impl.putArgCount(argCount);
	}
	public synchronized void putSize(int size) throws MathLinkException {
	 	impl.putSize(size);
	}
	public synchronized int bytesToPut() throws MathLinkException {
		return impl.bytesToPut();
	}
	public synchronized int bytesToGet() throws MathLinkException {
		return impl.bytesToGet();
	}
	public synchronized void putData(byte[] data, int len) throws MathLinkException {
		impl.putData(data, len);
	}
	public synchronized byte[] getData(int len) throws MathLinkException {
		return impl.getData(len);
	}
	public synchronized String getString() throws MathLinkException {
		return impl.getString();
	}
	public synchronized byte[] getByteString(int missing) throws MathLinkException {
		return impl.getByteString(missing);
	}
	public synchronized void putByteString(byte[] data) throws MathLinkException {
		impl.putByteString(data);
	}
	public synchronized String getSymbol() throws MathLinkException {
		return impl.getSymbol();
	}
	public synchronized void putSymbol(String s) throws MathLinkException {
		impl.putSymbol(s);
	}
	public synchronized void put(boolean b) throws MathLinkException {
		impl.put(b);
	}
	public synchronized int getInteger() throws MathLinkException {
		return impl.getInteger();
	}
	public synchronized void put(int i) throws MathLinkException {
		impl.put(i);
	}
	public synchronized long getLongInteger() throws MathLinkException {
		return impl.getLongInteger();
	}
	public synchronized void put(long i) throws MathLinkException {
		impl.put(i);
	}
	public synchronized double getDouble() throws MathLinkException {
		return impl.getDouble();
	}
	public synchronized void put(double d) throws MathLinkException {
		impl.put(d);
	}
	public synchronized MLFunction getFunction() throws MathLinkException {
		return impl.getFunction();
	}
	public synchronized void putFunction(String f, int argCount) throws MathLinkException {
		impl.putFunction(f, argCount);
	}
	public synchronized int checkFunction(String f) throws MathLinkException {
		return impl.checkFunction(f);
	}
	public synchronized void checkFunctionWithArgCount(String f, int argCount) throws MathLinkException {
		impl.checkFunctionWithArgCount(f, argCount);
	}
	public synchronized void transferExpression(MathLink source) throws MathLinkException {
		impl.transferExpression(source);
	}
	public synchronized void transferToEndOfLoopbackLink(LoopbackLink source) throws MathLinkException {
		impl.transferToEndOfLoopbackLink(source);
	}
	public synchronized Expr getExpr() throws MathLinkException {
		return impl.getExpr();
	}
	public synchronized Expr peekExpr() throws MathLinkException {
		return impl.peekExpr();
	}
	public int getMessage() throws MathLinkException {
		return impl.getMessage();
	}
	public void putMessage(int msg) throws MathLinkException {
		impl.putMessage(msg);
	}
	public boolean messageReady() throws MathLinkException {
		return impl.messageReady();
	}
	public synchronized long createMark() throws MathLinkException {
		return impl.createMark();
	}
	public synchronized void seekMark(long mark) {
		impl.seekMark(mark);
	}
	public synchronized void destroyMark(long mark) {
		impl.destroyMark(mark);
	}
	public boolean setYieldFunction(Class cls, Object target, String methName) {
		// Not synhchornized, as it is convenient to set a yielder while a thread is blocking in MathLink.
		// Synchronization specific to the yielder-handling data structures happens at a lower level.
		return impl.setYieldFunction(cls, target, methName);
	}
	public synchronized boolean addMessageHandler(Class cls, Object target, String methName) {
		return impl.addMessageHandler(cls, target, methName);
	}
	public synchronized boolean removeMessageHandler(String methName) {
		return impl.removeMessageHandler(methName);
	}

	///////////////////////////  Methods requiring more than a simple call to impl  /////////////////////////
	
	public synchronized int nextPacket() throws MathLinkException {

		// Code here is not just a simple call to impl.nextPacket(). For a KernelLink, nextPacket() returns a
		// wider set of packet constants than the MathLink C API itself. We want nextPacket() to work on the
		// non-packet types of heads the kernel and FE send back and forth.
		// Because for some branches below we seek back to the start before returning, it is not guaranteed
		// that when nextPacket() returns, the current packet has been "opened". Thus it is not safe to write
		// a J/Link program that loops calling nextPacket()/newPacket(). You need to call handlePacket() after
		// nextPacket() to ensure that newPacket() will throw away the curent "packet".

		// createMark will fail on an unconnected link, and the user might call this
		// before any reading that would connect the link.
		if (!linkConnected) {
			connect();
			linkConnected = true;
		}

		int pkt;
		long mark = createMark();
		try {
			pkt = impl.nextPacket();
		} catch (MathLinkException e) {
			if (e.getErrCode() == 23 /* MLEUNKNOWNPACKET */) {
				if (DEBUGLEVEL > 1) System.err.println("Unknown packet type in nextPacket");
				clearError();
				seekMark(mark);
				MLFunction f = getFunction();
				if (f.name.equals("ExpressionPacket"))
					pkt = EXPRESSIONPKT;
				else if (f.name.equals("BoxData")) {
					// Probably more than just BoxData will need to join this group. I thought that kernel
					// always sent cell contents (e.g. Print output) wrapped in ExpressionPacket, but not so.
					// "Un-wrapped" BoxData is also possible, perhaps others. We need to treat this case
					// specially, since there is no packet wrapper.
					seekMark(mark);
					pkt = EXPRESSIONPKT;
				} else {
					// Note that all other non-recognized functions get labelled as FEPKT. I could perhaps be
					// more discriminating, but then I risk not recognizing a legitimate communication
					// intended for the front end. This means that FEPKT is not a specific indication that
					// a front-end-related packet is on the link. Because there is no diagnostic packet head,
					// we need to seek back to before the function was read. That way, when programs call
					// methods to read the "contents" of the packet, they will in fact get the whole thing.
					seekMark(mark);
					pkt = FEPKT;
				}
			} else {
				// The other MathLink error from MLNextPacket is MLENEXTPACKET, which is a common one saying
				// that you have called MLNextPacket when more data is in the current packet.
				throw e;
			}
		} finally {
			destroyMark(mark);
		}
		return pkt;
	}
	
	public synchronized int getNext() throws MathLinkException {
	 	int result = impl.getNext();
	 	if (result == MLTKSYM && isObject())
	 		result = MLTKOBJECT;
	 	return result;
	}
	
	public synchronized int getType() throws MathLinkException {
	 	int result = impl.getType();
	 	if (result == MLTKSYM && isObject())
	 		result = MLTKOBJECT;
	 	return result;
	}
	
	// This method could be left out of this class, instead relying on the superclass (KernelLinkImpl) implementation.
	// But that would eventually trickle down to the inefficient "catch-all" array-getting code in MathLinkImpl.
	// On the assumption that the wrapped link can do some array types more efficiently, we forward those to it.
	// In the common case where the wrapped link is a NativeLink, this assumption holds true for types that are
	// native in the MathLink C API.
	// The only types we must handle ourselves are TYPE_COMPLEX (since we must be sure to use _our_ notion of the
	// complex class, not the impl's, which will not even have been set), and TYPE_OBJECT (because only a
	// KernelLink can do that).
	public synchronized Object getArray(int type, int depth) throws MathLinkException {

			return getArray(type, depth, null);
	}
	public synchronized Object getArray(int type, int depth, String[] heads) throws MathLinkException {
		
		if (type == TYPE_COMPLEX || type == TYPE_OBJECT)
			return super.getArray(type, depth, heads);
		else
			// We don't _need_ to forward--just an optimization.
			return impl.getArray(type, depth, heads);
	}

	protected void putString(String s) throws MathLinkException {
		impl.put(s);
	}
	
	// Already guaranteed by caller (MathLinkImpl) that data is an array and not null. All
	// byvalue array-putting work goes through this function.
	protected void putArray(Object obj, String[] heads) throws MathLinkException {

		Class elementClass = Utils.getArrayComponentType(obj.getClass());
		if (elementClass.isPrimitive() || elementClass.equals(String.class)) {
			impl.put(obj, heads);
		} else {
			// Note that we only forwarded to the impl object primitive or string arrays.
			// We could also have forwarded arrays of byval-type objects (e.g., Integer),
			// but not the complex class (we need to use _our_ notion of the complex class,
			// not impl's, which is not even set). For efficiency reasons, we don't
			// bother to check for this larger set of arrays we could forward. Their elements
			// will eventually get forwarded to impl as the array is picked apart by putArrayPiecemeal.
			putArrayPiecemeal(obj, heads, 0);
		}
	}


	protected void showInFront() throws MathLinkException {
		
		// On Mac and Windows, we must call into C code to get the window to come to the foreground.
		// These native method calls are no-ops on the other platforms.
		if (impl instanceof NativeLink) {
			NativeLink.winJavaLayerToFront(true);
			NativeLink.macJavaLayerToFront();
		}
		super.showInFront();
		if (impl instanceof NativeLink)
			NativeLink.winJavaLayerToFront(false);
	}
    
    
    // For non-Windows platforms.
    private static long nextWindowID = 1;
    
    // Returns a unique ID for each Java window in a session. On Windows,
    // the ID should be the HWND. On other platforms it has no special significance
    // beyond its uniqueness. This is to support the ability to register Java windows
    // to be managed by the FE.
    // NOTE: Although this function is fully implemented on all platforms (even
    // down into the native library), it is only currently called on Windows.
    // This is because on other platforms the ids must be globally unique across
    // all runtimes (Mono, Java, possibly multiple instances of each, etc.)
    // Thus unless the ids refer to a native resource that itself is guaranteed
    // to be unique, the ids must be handled in Mathematica code, not down inside one
    // instance of a runtime.
    protected long getNativeWindowHandle(java.awt.Window obj) {
        
        if (impl instanceof NativeLink && Utils.isWindows()) {
            return NativeLink.getNativeWindowHandle(obj, System.getProperty("java.home"));
        } else {
            return nextWindowID++;
        }

    }

    JSONArray array = new JSONArray();
    
	@Override
	public JSONArray result() {
		return array;
	}

}
