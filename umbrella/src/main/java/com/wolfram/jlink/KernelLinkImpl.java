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

import java.util.*;
import java.lang.reflect.*;


// KernelLinkImpl is the largest and most important class in J/Link. It implements the entire
// KernelLink interface, including all internal support methods and fields, except (more or less)
// the MathLink interface (it does inherit significant implementation of MathLink via MathLinkImpl).
// KernelLink is implemented here in terms of the MathLink interface. To create an implementation
// of KernelLink, you can just subclass KernelLinkImpl and provide implementations of the low-level
// MathLink interface methods. All the complex logic that makes a KernelLink beyond a MathLink is coded
// here. Put another way, KernelLinkImpl is the repository for a huge amount of reusable logic that
// creates the extra functionality of KernelLink via calls to methods in the lower MathLink interface.
//
// The main class that extends this one is WrappedKernelLink, which implements the raw MathLink
// put/get methods by forwarding them to the MathLink implementation it "wraps". Other KernelLink
// implementation classes that extend this one will want to override more of its KernelLink
// implementation if they are not happy with its fine-grained use of the MathLink methods
// (e.g., the experimental KernelLink_HTTP overrides most of the methods to make them single network hits).
//
// Readers who want to understand how the "installable Java" features of J/Link (i.e., calling Java
// from Mathematica), will want to start with the handleCallPacket() method here.


public abstract class KernelLinkImpl extends MathLinkImpl implements KernelLink {

	// ObjectHandler belongs not to a link, but really to a kernel. It is important that all KernelLinks that point
	// to the same kernel share the same ObjectHandler (if they are to be used for reading/writing object references).
	private ObjectHandler objectHandler = new ObjectHandler();

	private Object msgSync = new Object();
	private volatile int msg;

	protected boolean isManual = false;

	protected Throwable lastError;

	protected Throwable lastExceptionDuringCallPacketHandling;

	private StringBuffer accumulatingPS;

	private boolean lastPktWasMsg = false; // Used only inside handlePacket().


	// These are all the M symbols that are named directly in Java code. Even though some of them are only used in
	// the ObjectHandler class, it is useful to hewave them all listed together.
	static final String PACKAGE_PROTECTED_CONTEXT				= KernelLink.PACKAGE_CONTEXT + "Package`";

	static final String MMA_LOADCLASSANDCREATEINSTANCEDEFS	= PACKAGE_PROTECTED_CONTEXT + "loadClassAndCreateInstanceDefs";
	static final String MMA_CREATEINSTANCEDEFS					= PACKAGE_PROTECTED_CONTEXT + "createInstanceDefs";
	static final String MMA_LOADCLASS								= PACKAGE_PROTECTED_CONTEXT + "loadClassFromJava";

	static final String MMA_PREPAREFORMANUALRETURN				= PACKAGE_PROTECTED_CONTEXT + "prepareForManualReturn";
	static final String MMA_HANDLECLEANEXCEPTION					= PACKAGE_PROTECTED_CONTEXT + "handleCleanException";
	static final String MMA_AUTOEXCEPTION							= PACKAGE_PROTECTED_CONTEXT + "autoException";
	static final String MMA_MANUALEXCEPTION						= PACKAGE_PROTECTED_CONTEXT + "manualException";


	// These TYPE_ constants are extensions to the set in the MathLink interface. They are
	// implementation details of this class (and the .c and .m files) and not for users to
	// see or use. Define array types recursively in terms of ARRAY1. Must maintain this
	// additive relationship. Could use any number greater than the max of the primitive
	// types for TYPE_ARRAY1.
	static final int TYPE_FLOATORINT		= -15;
	static final int TYPE_DOUBLEORINT	= -16;
	static final int TYPE_ARRAY1			= -17;
	static final int TYPE_ARRAY2			= TYPE_ARRAY1 + TYPE_ARRAY1;
	static final int TYPE_ARRAY3			= TYPE_ARRAY2 + TYPE_ARRAY1;
	static final int TYPE_ARRAY4			= TYPE_ARRAY3 + TYPE_ARRAY1;
	static final int TYPE_ARRAY5			= TYPE_ARRAY4 + TYPE_ARRAY1;
	static final int TYPE_BAD				= -10000;


	////////////////////////////  Constructor  /////////////////////////////

	protected KernelLinkImpl() {}



	///////////////////////////////  KernelLink Interface  ///////////////////////////////////

	public synchronized void evaluate(String s) throws MathLinkException {

		putFunction("EvaluatePacket", 1);
		putFunction("ToExpression", 1);
		put(s);
		endPacket();
		flush();
	}

	public synchronized void evaluate(Expr e) throws MathLinkException {

		putFunction("EvaluatePacket", 1);
		put(e);
		endPacket();
		flush();
	}

	public synchronized String evaluateToOutputForm(String s, int pageWidth) {
		return evalToString(s, pageWidth, "OutputForm");
	}

	public synchronized String evaluateToOutputForm(Expr e, int pageWidth) {
		return evalToString(e, pageWidth, "OutputForm");
	}

	public synchronized String evaluateToInputForm(String s, int pageWidth) {
		return evalToString(s, pageWidth, "InputForm");
	}

	public synchronized String evaluateToInputForm(Expr e, int pageWidth) {
		return evalToString(e, pageWidth, "InputForm");
	}

	public synchronized byte[] evaluateToTypeset(String s, int pageWidth, boolean useStdForm) {
		return evalToTypeset(s, pageWidth, useStdForm);
	}

	public synchronized byte[] evaluateToTypeset(Expr e, int pageWidth, boolean useStdForm) {
		return evalToTypeset(e, pageWidth, useStdForm);
	}

	// pass 0 for dpi, width, height to get Automatic.
	public synchronized byte[] evaluateToImage(String s, int width, int height) {
		return evalToImage(s, width, height, 0, false);
	}

	public synchronized byte[] evaluateToImage(Expr e, int width, int height) {
		return evalToImage(e, width, height, 0, false);
	}

	// pass 0 for dpi, width, height to get Automatic.
	public synchronized byte[] evaluateToImage(String s, int width, int height, int dpi, boolean useFE) {
		return evalToImage(s, width, height, dpi, useFE);
	}

	public synchronized byte[] evaluateToImage(Expr e, int width, int height, int dpi, boolean useFE) {
		return evalToImage(e, width, height, dpi, useFE);
	}

	public synchronized String evaluateToMathML(String s) {
		return evalToString(s, 0, "MathMLForm");
	}

	public synchronized String evaluateToMathML(Expr e) {
		return evalToString(e, 0, "MathMLForm");
	}


	// Will return one of the 4 "answer" packets (return, returntext, returnexpr, inputname). The link
	// will be in the state just after nextPacket().
	public synchronized int waitForAnswer() throws MathLinkException {

		int pkt;

		// This is set to null on DisplayEndPacket, but do it here as a safety net in case the
		// packet loop doesn't end normally.
		accumulatingPS = null;

		while (true) {
			if (DEBUGLEVEL > 1) System.err.println("About to do nextpacket in waitForAnswer");
			pkt = nextPacket();
			if (DEBUGLEVEL > 1) System.err.println("back from nextpacket in waitForAnswer");
			boolean allowDefaultProcessing = notifyPacketListeners(pkt);
			if (DEBUGLEVEL > 1) System.err.println("back from notifyPacketListeners in waitForAnswer. allowDefaultProcessing = " + allowDefaultProcessing);
			if (allowDefaultProcessing)
				handlePacket(pkt);
			if (pkt == RETURNPKT || pkt == INPUTNAMEPKT || pkt == RETURNTEXTPKT || pkt == RETURNEXPRPKT)
				// These are the only ones that cause this function to exit (that is, they qualify as "answers").
				break;
			else
				newPacket();
		}
		return pkt;
	}

	public synchronized void discardAnswer() throws MathLinkException {

		int pkt = waitForAnswer();
		newPacket();
		// These are the only two packet types that constitute the absolute end of an eval.
		while (pkt != RETURNPKT && pkt != INPUTNAMEPKT) {
			// This loop will only happen once, of course, but might as well be defensive.
			pkt = waitForAnswer();
			newPacket();
		}
	}

	public Throwable getLastError() {

		int err = error();
		return err != MLEOK ? new MathLinkException(err, errorMessage()) : lastError;
	}

	// This function is for putting objects by reference.
	public synchronized void putReference(Object obj) throws MathLinkException {
		putReference(obj, null);
	}

	// This signature is not currently exposed via the KernelLink interface. I'm not sure that it's worth
	// adding. For now, I'll leave it public in this class so that programmers who absolutely need it
	// can be told how to call it.
	public synchronized void putReference(Object obj, Class upCastCls) throws MathLinkException {

		if (obj == null) {
			putSymbol("Null");
		} else {
			objectHandler.putReference(this, obj, upCastCls);
		}
	}


	public synchronized Object getObject() throws MathLinkException {

		try {
			return objectHandler.getObject(getSymbol());
		} catch (Exception e) {
			// Convert exceptions thrown by getSymbol() (wasn't a symbol at all) or ObjectHandler.getObject()
			// (symbol wasn't a valid object ref) into MLE_BAD_OBJECT exceptions.
			throw new MathLinkException(MLE_BAD_OBJECT);
		}
	}


	// This allows callbacks but it doesn't set up a thread that reads the link. Thus, anything
	// (like ShareKernel) that would cause packets to be sent to Java while it wasn't expecting
	// anything would break. Advanced programmers can start their own Reader thread (see the Reader
	// source).

	public synchronized void enableObjectReferences() throws MathLinkException {
        enableObjectReferences(true);
	}

    public synchronized Expr enableObjectReferences(boolean becomeDefaultJVM) throws MathLinkException {

        evaluate("Needs[\"" + PACKAGE_CONTEXT + "\"]");
        discardAnswer();
        evaluate("GetJVM[InstallJava[$ParentLink, Default->" + (becomeDefaultJVM ? "True" : "Automatic") + "]]");
        flush();
        Install.install(this);
        waitForAnswer();
        Expr jvm = getExpr();

        // As a convenience, setup StdLink, as this is what virtually all users
        // will want. But avoid overwriting a StdLink if they have already set it.
        if (StdLink.getLink() == null)
            StdLink.setLink(this);

        return jvm;
    }


    public JLinkClassLoader getClassLoader() {
        return objectHandler.getClassLoader();
    }

    public void setClassLoader(JLinkClassLoader loader) {
        objectHandler.setClassLoader(loader);
    }


	public void handlePacket(int pkt) throws MathLinkException {

		switch (pkt) {
			// If you ever change the default behavior on the 4 "answer" packets to read off the link,
			// you'll need to add a seekMark in NativeKernelLink.waitForAnswer...
			case RETURNPKT:
				if (DEBUGLEVEL > 1) System.err.println("Got RETURNPKT in handlePacket");
				break;
			case INPUTNAMEPKT:
				if (DEBUGLEVEL > 1) System.err.println("Got INPUTNAMEPKT in handlePacket");
				break;
			case RETURNTEXTPKT:
				if (DEBUGLEVEL > 1) System.err.println("Got RETURNTEXTPKT in handlePacket");
				break;
			case RETURNEXPRPKT:
				if (DEBUGLEVEL > 1) System.err.println("Got RETURNEXPRPKT in handlePacket");
				break;
			case MENUPKT:
				if (DEBUGLEVEL > 1) System.err.println("Got MENUPKT in handlePacket");
				break;
			case MESSAGEPKT:
				if (DEBUGLEVEL > 1) System.err.println("Got MESSAGEPKT in handlePacket");
				break;
		// From here on, the cases do actual work.
			case CALLPKT: {
				if (DEBUGLEVEL > 1) System.err.println("Got CALLPKT in handlePacket");
				int type = getType();
				if (type == MLTKINT) {
                    // A normal CallPacket representing a call to Java via jCallJava.
					handleCallPacket();
				} else if (getFEServerLink() != null) {
                    // A CallPacket destined for the FE via MathLink`CallFrontEnd[] and routed through
                    // Java due to ShareFrontEnd[]. This would only be in a 5.1 or later FE, as earlier
                    // versions do not use CallPacket and later versions would use the FE's Service Link.
					MathLink feLink = getFEServerLink();
					feLink.putFunction("CallPacket", 1);
					feLink.transferExpression(this);
                    // FE will always reply to a CallPacket. Note that it is technically possible for
                    // the FE to send back an EvaluatePacket, which means that we really need to run a
                    // little loop here, not just write the result back to the kernel. But this branch
                    // is only for a 5.1 and later FE, and I don't think that they ever do that.
					transferExpression(feLink);
                }
				break;
			}
			case INPUTPKT:
			case INPUTSTRPKT:
				if (DEBUGLEVEL > 1) System.err.println("Got INPUTPKT in handlePacket");
				if (getFEServerLink() != null) {
					MathLink fe = getFEServerLink();
					fe.putFunction(pkt == INPUTSTRPKT ? "InputStringPacket" : "InputPacket", 1);
					fe.put(getString());
					fe.flush();
					newPacket();
					put(fe.getString());
					flush();
				}
				break;
			case DISPLAYPKT:
			case DISPLAYENDPKT:
				if (getFEServerLink() != null) {
					if (DEBUGLEVEL > 1) System.err.println("sending DISPLAYPKT to FE in handlePacket");
					if (accumulatingPS == null)
						accumulatingPS = new StringBuffer(34000);  // 34K is large enough to hold an entire packet
					accumulatingPS.append(getString());
					if (pkt == DISPLAYENDPKT) {
						if (DEBUGLEVEL > 1) System.err.println("sending PostScript to FE in handlePacket");
						MathLink fe = getFEServerLink();
						// XXXPacket[stuff] ---> Cell[GraphicsData["PostScript", stuff], "Graphics"]
						fe.putFunction("FrontEnd`FrontEndExecute", 1);
						fe.putFunction("FrontEnd`NotebookWrite", 2);
						fe.putFunction("FrontEnd`SelectedNotebook", 0);
						fe.putFunction("Cell", 2);
						fe.putFunction("GraphicsData", 2);
						fe.put("PostScript");
						fe.put(accumulatingPS.toString());
						fe.put("Graphics");
						fe.flush();
						accumulatingPS = null;
						if (DEBUGLEVEL > 1) System.err.println("finished flush in DISPLAYPKT in handlePacket");
					}
				} else {
					if (DEBUGLEVEL > 1) System.err.println("Got DISPLAYPKT in handlePacket, but no FE link");
				}
				break;
			case TEXTPKT:
			case EXPRESSIONPKT: {
				MathLink fe = getFEServerLink();
				// Print output, or message text.
				if (fe != null) {
					if (DEBUGLEVEL > 1) System.err.println("sending TEXTPKT or EXPRPKT to FE in handlePacket");
					// XXXPacket[stuff] ---> Cell[stuff, "Print"]
					fe.putFunction("FrontEnd`FrontEndExecute", 1);
					fe.putFunction("FrontEnd`NotebookWrite", 2);
					fe.putFunction("FrontEnd`SelectedNotebook", 0);
					fe.putFunction("Cell", 2);
					fe.transferExpression(this);
					fe.put((lastPktWasMsg) ? "Message" : "Print");
					fe.flush();
					if (DEBUGLEVEL > 1) System.err.println("finished flush in TEXTPKT or EXPRPKT in handlePacket");
				} else {
					if (DEBUGLEVEL > 1) {
						System.err.println("Got TEXTPKT or EXPRPKT in handlePacket, but no FE link");
						if (pkt == TEXTPKT) System.err.println(getString());
					}
					// For one type of EXPRESSIONPKT, no part of it has been read yet. Thus we must "open" the
					// packet so that later calls to newPacket() throw it away.
					if (pkt == EXPRESSIONPKT)
						getFunction();
				}
				break;
			}
			case FEPKT:
				// This case is different from the others. At the point of entry, the link is at the point
				// _before_ the "packet" has been read. As a result, we must at least open the packet.
				// Note that FEPKT is really just a fall-through for unrecognized packets. We don't have any
				// checks that it is truly intended for the FE.
				MathLink feLink = getFEServerLink();
				if (feLink != null) {
					if (DEBUGLEVEL > 1) System.err.println("sending FEPKT to FE in handlePacket");
					long mark = createMark();
					try {
						// Wrap FrontEndExecute around it if not already there.
						MLFunction wrapper = getFunction();
						if (!wrapper.name.equals("FrontEnd`FrontEndExecute")) {
							feLink.putFunction("FrontEnd`FrontEndExecute", 1);
						}
					} finally {
						seekMark(mark);
						destroyMark(mark);
					}
					feLink.transferExpression(this);
					feLink.flush();
					// Wait until either the fe is ready (because what we just sent causes a return value)
					// or kernel is ready (the computation is continuing because the kernel is not waiting
					// for a return value).
					do {
						try { Thread.sleep(60); } catch (InterruptedException e) {}
					} while (!feLink.ready() && !ready());
					if (feLink.ready()) {
						// fe link has something to return to kernel from last FEPKT we sent it.
						transferExpression(feLink);
						flush();
					}
				} else {
					// It's OK to get here. For example, this happens if you don't share the fe, but have a
					// button that calls NotebookCreate[]. This isn't a very good example, because that
					// function expects the fe to return something, so Java will hang. you will get into
					// trouble if you make calls on the fe that expect a return. Everything is OK for calls
					// that don't expect a return, though.
					if (DEBUGLEVEL > 0) System.err.println("Got FEPKT in handlePacket, but no FE link!");
					getFunction(); // Must at least open the packet, so newPacket (back in caller) will get rid of it.
				}
				break;
			default:
				break;
		}
		lastPktWasMsg = pkt == MESSAGEPKT;
	}


	// Critical that these not be synchronized (same as with putMessage).

	public void interruptEvaluation() {
		try { putMessage(MLINTERRUPTMESSAGE); } catch (MathLinkException e) {}
	}

	public void abortEvaluation() {
		try { putMessage(MLABORTMESSAGE); } catch (MathLinkException e) {}
	}

	public void terminateKernel() {
		try { putMessage(MLTERMINATEMESSAGE); } catch (MathLinkException e) {}
	}

	public void abandonEvaluation() {
		setYieldFunction(null, this, "bailoutYielder");
	}

	public boolean bailoutYielder() {
		// Turn off the yield function (this is safe to do here).
		setYieldFunction(null, null, null);
		return true;
	}

	///////////////////////  Public but not part of the KernelLink interface  //////////////////

    public ObjectHandler getObjectHandler() {
        return objectHandler;
    }

    public void setObjectHandler(ObjectHandler objh) {
        objectHandler = objh;
    }


    ///////////////////////  Methods only relevant for StdLink-type callbacks  /////////////////////

	public synchronized void print(String s) {

		try {
			putFunction("EvaluatePacket", 1);
			putFunction("Print", 1);
			put(s);
			endPacket();
			discardAnswer();
		} catch (MathLinkException e) {
			// Not guaranteed to be a complete or useful cleanup.
			if (DEBUGLEVEL > 0) System.err.println("MathLinkException caught in print: " + e.toString());
			clearError();
			newPacket();
		}
	}

	public synchronized void message(String symtag, String arg) {
		String[] array = {arg};
		message(symtag, array);
	}

	public synchronized void message(String symtag, String[] args) {

		try {
			putFunction("EvaluatePacket", 1);
			putFunction("Apply", 2);
			putFunction("ToExpression", 1);
			put("Function[Null, Message[#1, ##2], HoldFirst]");
			putFunction("Join", 2);
			putFunction("ToHeldExpression", 1);
			put(symtag);
			putFunction("Hold", args.length);
			for (int i = 0; i < args.length; i++)
				put(args[i]);
			endPacket();
			discardAnswer();
		} catch (MathLinkException e) {
			// Not guaranteed to be a complete or useful cleanup.
			if (DEBUGLEVEL > 0) System.err.println("MathLinkException caught in message: " + e.toString());
			clearError();
			newPacket();
		}
	}

	public synchronized void beginManual() {
		setManual(true);
	}

	public boolean wasInterrupted() {
		int theMsg = 0;
		synchronized (msgSync) {
			theMsg = msg;
		}
		return theMsg == MLINTERRUPTMESSAGE || theMsg == MLABORTMESSAGE;
	}

	public void clearInterrupt() {
		synchronized (msgSync) {
			msg = 0;
		}
	}


//////////////////////////  End of KernelLink interface; Nothing public after this point  //////////////////////////////


   /////////////////////////////////  Exception handling  ///////////////////////////////////////

	// "Clean" means that we have not tried to put any partial result on the link yet. This is not
	// a user-visible function. It is only called while handling calls from Mathematica (i.e.,
	// handleCallPacket() is on the stack).

	protected void handleCleanException(Throwable t) {

		// Currently we do not check wasInterrupted() here and send back Abort[], on the grounds
		// that the exeption message is probably more useful than $Aborted. But it might be better
		// to send back Abort[] and thus stop the entire computation.
		lastExceptionDuringCallPacketHandling = t;
		try {
			if (DEBUGLEVEL > 0) {
				System.err.println("entering handleCleanException: " + t.toString());
				t.printStackTrace();
			}
			clearError();
			newPacket();
			if (wasInterrupted()) {
				putFunction("Abort", 0);
			} else {
				String msg = Utils.createExceptionMessage(t);
				putFunction(MMA_HANDLECLEANEXCEPTION, 1);
				putFunction(MMA_AUTOEXCEPTION, 1);
				put(msg);
			}
			endPacket();
			flush();
		} catch (MathLinkException e) {
			if (DEBUGLEVEL > 0) System.err.println("MathLinkException thrown inside handleCleanException: " + e.toString());
			// Need to send something back on link, or this will not be an acceptable branch.
			// About the only thing to do is call endPacket and hope that this will cause
			// $Aborted to be returned.
			try { endPacket(); } catch (MathLinkException ee) {}
		}
		if (DEBUGLEVEL > 0) System.err.println("leaving handleCleanException: " + t.toString());
	}


	////////////////////////////  Message Handler  ////////////////////////////////

	public void msgHandler(int msg, int ignore) {
		synchronized (msgSync) {
			this.msg = msg;
		}
	}


///////////////////////////////////  Implementation  /////////////////////////////////////

	private synchronized void handleCallPacket() {

		/* Strategy for exception handling here:
			1) catch ALL here, throw nothing.
			2) each function handles its own exceptions before return link is dirtied.
			3) each function minimizes the possibility of exceptions being thrown once
			   process of putting result begins. If an exception occurs after this point,
			   they throw to let it be caught by handler in this function. The result of
			   such exceptions will either be invisible in Mma (a complete expression was
			   already sent), or user will see $Aborted (if nothing or partial expr was sent).
			   These are not very desirable, hence commandment to minimize this possibility.

			These rules may not apply to callJava, which is a special case.
		*/

		// At this point, a CALLPKT has been opened.
		int index = 0;
		try {
			if (DEBUGLEVEL > 1) System.err.println("In handlecallpacket.");
			index = getInteger();
			checkFunction("List");
		} catch (MathLinkException e) {
			if (DEBUGLEVEL > 0) System.err.println("MathLinkException caught at start of handleCallPacket: " + e.toString());
			handleCleanException(e);
			return;
		}

		// Reset lastExceptionDuringCallPacketHandling unless this callpacket is a request to get its value.
		if (index != Install.GETEXCEPTION)
			lastExceptionDuringCallPacketHandling = null;

		try {
			StdLink.setup(this);
			StdLink.lastPktWasAllowUIComputations(false); // May be set to true below by an ALLOWUICOMPUTATIONS call.
			clearInterrupt();
			switch (index) {
				// The indices here are from the Install class. They mimic the standard function indices
				// in installable C programs that are established during the Install call.
				// Every one of these functions is responsible for sending a result back on the link.
				case Install.CALLJAVA:					callJava();					break;
				case Install.LOADCLASS:					loadClass();				break;
				case Install.THROW:						throwFromMathematica();	break;
				case Install.RELEASEOBJECT:			releaseInstance();		break;
				case Install.VAL:							val();						break;
				case Install.ONLOADCLASS:				callOnLoadClass();		break;
				case Install.ONUNLOADCLASS:			callOnUnloadClass();		break;
				case Install.SETCOMPLEX:				setComplexCls();			break;
				case Install.REFLECT:					reflect();					break;
                case Install.SHOW:                      showInFront();              break;
				case Install.SAMEQ:						sameObjectQ();				break;
				case Install.INSTANCEOF:				instanceOf();				break;
				case Install.ALLOWRAGGED:				allowRaggedArrays();		break;
                case Install.GETEXCEPTION:              getException();         break;
				case Install.CONNECTTOFE:				connectToFEServer();		break;
				case Install.DISCONNECTTOFE:			disconnectToFEServer();	break;
				case Install.PEEKCLASSES:				peekClasses();				break;
				case Install.PEEKOBJECTS:				peekObjects();				break;
				case Install.SETUSERDIR:				setUserDir();					break;
				case Install.CLASSPATH:					getClassPath();			break;
				case Install.ADDTOCLASSPATH:			addToClassPath();			break;
				case Install.UITHREADWAITING:			uiThreadWaiting();		break;
				case Install.ALLOWUICOMPUTATIONS:	allowUIComputations();	break;
				case Install.YIELDTIME:					yieldTime();				break;
				case Install.GETCONSOLE:				getConsole();				break;
				case Install.EXTRALINKS:                extraLinks(true);            break;
                case Install.GETWINDOWID:               getWindowID();              break;
                case Install.ADDTITLECHANGELISTENER:    addTitleChangeListener();   break;
                case Install.SETVMNAME:                 setVMName();               break;
                case Install.SETEXCEPTION:              setException();         break;
				default: break;
			}
		} catch (Exception e) {
			// All functions in switch above must handle internally exceptions that occur before
			// anything is sent on link. This catch here is for exceptions thrown when link is in unknown
			// state (i.e., unknown whether partial or full expr has been sent.)
			if (DEBUGLEVEL > 0) System.err.println("Exception caught in handlecallpacket: " + e.toString());
			// Note that we do nothing here, as per above comment.
			lastExceptionDuringCallPacketHandling = e;
		} finally {
			StdLink.remove();
			clearError();
			newPacket();
			try {
				endPacket();
				flush();
			} catch (MathLinkException ee) {
				if (DEBUGLEVEL > 0) System.err.println("Exception caught in finally handler in handlecallpacket: " + ee.toString());
			}
			if (DEBUGLEVEL > 1) System.err.println("leaving handlecallpacket.");
		}
	}


	// obj will be a String or Expr
	private String evalToString(Object obj, int pageWidth, String format) {

		String res = null;

		lastError = null;
		try {
			Utils.writeEvalToStringExpression(this, obj, pageWidth, format);
			flush();
			waitForAnswer();
			res = getString();
		} catch (MathLinkException e) {
			// If one of the "evaluateTo" methods is returning null, you can get exception info printed to System.err by
			// adding -DJLINK_SHOW_INTERNAL_EXCEPTIONS=true to the command line that you use when you launch the Java runtime.
			String dbg = System.getProperty("JLINK_SHOW_INTERNAL_EXCEPTIONS");
			if (dbg != null && dbg.equals("true"))
				System.err.println("Exception in evaluateTo" + format + ": " + e.toString());
			clearError();
			lastError = e;
		} finally {
			newPacket();
		}
		return res;
	}

	// obj will be a String or Expr
	private byte[] evalToTypeset(Object obj, int pageWidth, boolean useStdForm) {

		byte[] imageData = null;

		lastError = null;
		try {
			putFunction("EvaluatePacket", 1);
			putFunction("Needs", 1);
			put(PACKAGE_CONTEXT);
			flush();
			discardAnswerNoPacketListeners();
			Utils.writeEvalToTypesetExpression(this, obj, pageWidth, useStdForm);
			flush();
			waitForAnswer();
		} catch (MathLinkException e) {
			// If one of the "evaluateTo" methods is returning null, you can get exception info printed to System.err by
			// adding -DJLINK_SHOW_INTERNAL_EXCEPTIONS=true to the command line that you use when you launch the Java runtime.
			String dbg = System.getProperty("JLINK_SHOW_INTERNAL_EXCEPTIONS");
			if (dbg != null && dbg.equals("true"))
				System.err.println("Exception in evaluateToTypeset: " + e.toString());
			clearError();
			lastError = e;
			newPacket();  // Just a guess. Hope that we are on the last packet.
			return null;
		}

		// From here on, the link will be OK no matter what happens if we ensure that we call newPacket
		try {
			if (getNext() == MLTKSTR)
				imageData = getByteString((byte) 0);
		} catch (Throwable t) {
			// I don't want this method to throw an outofmem exception if the byte array allocation fails.
			// That might need multi-megs. Just quietly return null if it fails.
			// TODO: Make this return an image that displays the text "out of memory".
			String dbg = System.getProperty("JLINK_SHOW_INTERNAL_EXCEPTIONS");
			if (dbg != null && dbg.equals("true"))
				System.err.println("Exception in evaluateToTypeset: " + t.toString());
			clearError();
			lastError = t;
		} finally {
			newPacket();
		}
		return imageData;
	}

	// obj will be a String or Expr
	private byte[] evalToImage(Object obj, int width, int height, int dpi, boolean useFE) {

		byte[] imageData = null;

		lastError = null;
		try {
			putFunction("EvaluatePacket", 1);
			putFunction("Needs", 1);
			put(PACKAGE_CONTEXT);
			flush();
			discardAnswerNoPacketListeners();
			Utils.writeEvalToImageExpression(this, obj, width, height, dpi, useFE);
			flush();
			waitForAnswer();
		} catch (MathLinkException e) {
			// If one of the "evaluateTo" methods is returning null, you can get exception info printed to System.err by
			// adding -DJLINK_SHOW_INTERNAL_EXCEPTIONS=true to the command line that you use when you launch the Java runtime.
			String dbg = System.getProperty("JLINK_SHOW_INTERNAL_EXCEPTIONS");
			if (dbg != null && dbg.equals("true"))
				System.err.println("Exception in evaluateToImage: " + e.toString());
			clearError();
			lastError = e;
			newPacket();  // Just a guess. Hope that we are on the last packet.
			return null;
		}

		// From here on, the link will be OK no matter what happens if we ensure that we call newPacket
		try {
			if (getNext() == MLTKSTR)
				imageData = getByteString((byte) 0);
		} catch (Throwable t) {
			// I don't want this method to throw an outofmem exception if the byte array allocation fails.
			// That might need multi-megs. Just quietly return null if it fails.
			// TODO: Make this return an image that displays the text "out of memory".
			String dbg = System.getProperty("JLINK_SHOW_INTERNAL_EXCEPTIONS");
			if (dbg != null && dbg.equals("true"))
				System.err.println("Exception in evaluateToImage: " + t.toString());
			clearError();
			lastError = t;
		} finally {
			newPacket();
		}
		return imageData;
	}


	MathLink getFEServerLink() {
		return objectHandler.getFEServerLink();
	}

	void setFEServerLink(MathLink feServerLink) {
		objectHandler.setFEServerLink(feServerLink);
	}


	protected void setManual(boolean val) {
		if (val && !isManual) {
			try {
				putFunction(MMA_PREPAREFORMANUALRETURN, 1);
				putSymbol("$CurrentLink");
				flush(); // Because we won't be reading.
			} catch (MathLinkException e) {
				clearError(); // What to do????
			}
		}
		isManual = val;
	}

	boolean isManual() {
		return isManual;
	}


	// From JavaThrow[] in Mathematica.
	protected void throwFromMathematica() throws Exception {

		Exception t = null;

		try {
			if (getType() == MLTKOBJECT) {
				// Passing the exception as an object.
				Object obj = getObject();
				getString(); // Discard unused string arg.
				newPacket();
				t = (Exception) obj;
			} else {
				// Passing the exception as the string name of the exception class.
				String exc = getString();
				String msg = getString();
				newPacket();
				// Now completely finished with reading of link.
				Class excClass = Class.forName(exc, true, objectHandler.getClassLoader());
				Object[] argsArray = null;
				Constructor ctor = null;
				if (msg.length() == 0) {
					ctor = excClass.getConstructor(new Class[]{});
					argsArray = new Object[0];
				} else {
					ctor = excClass.getConstructor(new Class[]{String.class});
					argsArray = new Object[1];
					argsArray[0] = msg;
				}
				t = (Exception) ctor.newInstance(argsArray);
			}
		} catch (Exception e) {
			handleCleanException(e);
			return;
		}
		putSymbol("Null");
		throw t;
	}


	protected void loadClass() throws MathLinkException {

		int classID;
		Object objSupplyingClassLoader = null;
		boolean isBeingLoadedAsComplexClass;

		try {
			// loadClass() is called with three arguments from M. The first is the class name and the second is an object
			// that will supply the classloader to be used to load the class (i.e., the object's classloader will be used).
			// It is OK, even typical, for this to be null, meaning use the JLinkClassLoader. The second argument is used
			// mainly for callbacks to load classes corresponding to objects being sent to M via putReference(). For such
			// objects, their class has already been loaded into the VM and we want to avoid reloading them with
			// JLinkClassLoader because this will make the Method objects we derive invalid for use on the original
			// object (which has a diferent classloader).
			// The third arg tells whether this LoadClass call is loading a class to be used in SetComplexClass[]. We need
			// to know that the class is becoming the complex class when it is being loaded, as the defs created in M for
			// the class need to reflect this fact.
			if (DEBUGLEVEL > 1) System.err.println("In loadClass.");
            classID = getInteger();
			String className = getString();
			objSupplyingClassLoader = getObject();
			isBeingLoadedAsComplexClass = getBoolean();
			newPacket();
			objectHandler.loadClass(classID, className, objSupplyingClassLoader);
			// Ignore it if setComplexClass() fails. If isBeingLoadedAsComplexClass is true, then a call to jSetComplex()
			// will follow immediately. The error will be detected then. The loading of the class here will continue as
			// normal, but defs will not be set up for this class as the complex class.
			if (isBeingLoadedAsComplexClass)
				setComplexClass(objectHandler.classFromID(classID));
		} catch (Throwable t) {
			// Catch Throwable instead of just Exception because it isn't impossible to get a NoClassDefFoundError.
			if (DEBUGLEVEL > 0) System.err.println("Exception caught in loadClass: " + t.toString());
			handleCleanException(t);
			return;
		}
		objectHandler.putInfo(this, classID, objSupplyingClassLoader);
		if (DEBUGLEVEL > 1) System.err.println("leaving loadClass");
	}


	protected void callJava() throws MathLinkException {

		int callType, classID;
		boolean byVal;
		Object instance = null;
		int[] indices = null;
		Object[] args = null;

		// On link, will be {class, type, instance, {indices}, byVal} all integers except instance is a symbol,
		// followed by argCount, then type/value pairs.
		try {
			checkFunction("List");
			classID = getInteger();
			callType = getInteger();
			instance = getObject();
			indices = getIntArray1();
			byVal = getInteger() == 0 ? false : true;
			int argCount = getInteger();
			args = new Object[argCount];
			for (int i = 0; i < argCount; i++) {
				args[i] = getTypeObjectPair();
			}
			newPacket();
		} catch (Exception e) {
			if (DEBUGLEVEL > 0) System.err.println("Exception caught in callJava: " + e.toString());
			handleCleanException(e);
			return;
		}

		try {
			switch (callType) {
				case 1: {
					// Constructor
					Object obj = null;
					try {
						obj = objectHandler.callCtor(classID, indices, args);
					} catch (Throwable t) {
                        if (isRecoverableException(t)) {
    						// Note that since ctors cannot be manual, nothing has been yet sent on link
    						// (unless an exception occurred during a Mma conversation inside the ctor, but
    						// you're screwed if you don't catch such things yourself).
    						handleCleanException(t);
    						break;
                        } else {
                            // Java will exit.
                            throw((Error) t);
                        }
					}
					if (wasInterrupted())
						putFunction("Abort", 0);
					else
						putReference(obj);
					break;
				}
				case 2: {
					// Method
					boolean wasManual = isManual();
					setManual(false);
					try {
						// instance will be null for statics.
						Object res = objectHandler.callMethod(classID, instance, indices, args);
						if (isManual()) {
						    // This will force Mma to get $Aborted if the user forgot to put a complete expression before returning:
						    endPacket();
							// This will satisfy the return read for exception info.
							putSymbol("Null");
						} else if (wasInterrupted()) {
							putFunction("Abort", 0);
						} else if (byVal) {
							put(res);
						} else {
							putReference(res);
						}
					} catch (InvocationTargetException e) {
						if (DEBUGLEVEL > 0) System.err.println("Caught InvocationTargetException");
						Throwable t = e.getTargetException();
						if (isManual()) {
							// This is set in handleCleanException() in all other branches.
						    lastExceptionDuringCallPacketHandling = t;
							if (DEBUGLEVEL > 1) System.err.println("was manual");
							clearError();  // Should never be relevant.
							// Print stack trace to stderr.
							t.printStackTrace();
							String msg = Utils.createExceptionMessage(t);
							endPacket();
							flush(); // This is crucial. It separates two cases: excptn before anything sent
											// and after a complete expr is sent.
							putFunction(MMA_MANUALEXCEPTION, 1);
							put(msg);
						} else {
							// This branch can only be entered when nothing has been sent yet. If anything
							// had been sent before an exception, it must have been a manual function
							// (provided users handle their own exceptions during conversations).
							handleCleanException(t);
							// Rethrow serious errors (like ThreadDeath resulting from "Kill linked program" in Mathematica's
							// Interrupt dialog), so Java will quit.
                            if (!isRecoverableException(t))
								throw((Error) t);
						}
					} catch (Exception t) {
						lastExceptionDuringCallPacketHandling = t;
						// This branch can only be entered when nothing has been sent yet.
						// Basically, it is for IllegalAccessException and IllegalArgumentException,
						// which occur before the user's function is entered.
						handleCleanException(t);
					} finally {
						setManual(wasManual);
					}
					break;
				}
				case 3: {
					// Field
					int fieldIndex = indices[indices.length - 1];
					try {
						// Instance will be null for statics.
						if (args.length == 0) {
							// Get
							Object res = objectHandler.getField(classID, instance, fieldIndex);
							if (byVal)
								put(res);
							else
								putReference(res);
						} else {
							// Set
							objectHandler.setField(classID, instance, fieldIndex, args[0]);
							putSymbol("Null");
						}
					} catch (Exception t) {
					    lastExceptionDuringCallPacketHandling = t;
						handleCleanException(t);
					}
					break;
				}
			}
        } catch (MathLinkException e) {
			// This is only for exceptions that occur during reporting of previous exceptions to
			// Mathematica. It should virtually never be entered. About the only thing to do is call
			// endPacket and hope that this will cause $Aborted to be returned, and further hope that
			// a second read isn't going to happen in Mma.
			System.err.println("Serious error: MathLinkException trying to report results of previous exception.");
			clearError();
			try { endPacket(); } catch (MathLinkException ee) {}
		}
	}


	protected void releaseInstance() throws MathLinkException {

		// Link will have a list of one or more symbols. None can be "Null".
		try {
			String[] syms = getStringArray1();
			newPacket();
			objectHandler.releaseInstance(syms);
		} catch (Exception e) {
			if (DEBUGLEVEL > 0) System.err.println("Exception caught in releaseInstance: " + e.toString());
			handleCleanException(e);
			return;
		}
		putSymbol("Null");
	}


	// This returns a value from a reference. Used only for classes that have a meaningful value
	// representation (strings, arrays, complexclass, and wrapped primitive types [Integer, Byte, etc.])
	// When you create these things with JavaNew, you get a JavaObject back, not the value. This function
	// gives you the value.
	protected void val() throws MathLinkException {

		Object obj = null;

		// Link will have a single symbol.
		try {
			obj = getObject();
			newPacket();
		} catch (Exception e) {
			if (DEBUGLEVEL > 0) System.err.println("Exception caught in val: " + e.toString());
			handleCleanException(e);
			return;
		}
        if (obj == null) {
            put(null);
        } else if (obj instanceof Collection) {
            // Convert Collection objects to Mathematica lists.
            put(((Collection) obj).toArray());
        } else if (obj instanceof Date || obj instanceof Calendar) {
            // Convert dates to Mathematica DateList format.
            Calendar cal;
            if (obj instanceof Date) {
                cal = Calendar.getInstance();
                cal.clear();
                cal.setTime((Date) obj);
            } else {
                cal = (Calendar) obj;
            }
            putFunction("List", 6);
            put(cal.get(Calendar.YEAR));
            put(cal.get(Calendar.MONTH) + 1);
            put(cal.get(Calendar.DATE));
            put(cal.get(Calendar.HOUR_OF_DAY));
            put(cal.get(Calendar.MINUTE));
            put(cal.get(Calendar.SECOND) + cal.get(Calendar.MILLISECOND)/1000.);
        } else {
            put(obj);
        }
	}


	protected void sameObjectQ() throws MathLinkException {

		Object obj1 = null, obj2 = null;

		// Link will have two objects.
		try {
			obj1 = getObject();
			obj2 = getObject();
			newPacket();
		} catch (Exception e) {
			if (DEBUGLEVEL > 0) System.err.println("Exception caught in sameObjectQ: " + e.toString());
			handleCleanException(e);
			return;
		}
		put(obj1 == obj2);
	}


	protected void instanceOf() throws MathLinkException {

		boolean isInstance;

		// Link will have an obj and a string.
		try {
			Object obj = getObject();
			String clsName = getString();
			newPacket();
			Class cls = Class.forName(clsName, true, JLinkClassLoader.getInstance());
			isInstance = cls.isInstance(obj);
		} catch (Exception e) {
			if (DEBUGLEVEL > 0) System.err.println("Exception caught in instanceOf: " + e.toString());
			handleCleanException(e);
			return;
		}
		put(isInstance);
	}


	protected void allowRaggedArrays() throws MathLinkException {

		boolean allow = false;

		// Link will have a boolean.
		try {
			allow = getBoolean();
			newPacket();
		} catch (Exception e) {
			if (DEBUGLEVEL > 0) System.err.println("Exception caught in instanceOf: " + e.toString());
			handleCleanException(e);
			return;
		}
		Utils.setRaggedArrays(allow);
		putSymbol("Null");
	}


    protected void getException() throws MathLinkException {

        // Link will be empty.
        try {
            newPacket();
        } catch (Exception e) {
            if (DEBUGLEVEL > 0) System.err.println("Exception caught in getException: " + e.toString());
            handleCleanException(e);
            return;
        }
        putReference(lastExceptionDuringCallPacketHandling);
    }

    // setException() is intended for code that calls Java but does not want to reset
    // the stored lastExceptionDuringCallPacketHandling to Null. Currently this is used by
    // the PacletManager in creating links for buttons at the end of messages. This involves
    // a call to Java, and it would blow away the stored exception in cases where a message
    // happened to be issued in the computation.
    protected void setException() throws MathLinkException {

        // Link will have object.
        try {
            Object obj = getObject();
            newPacket();
            lastExceptionDuringCallPacketHandling = (Throwable) obj;
        } catch (Exception e) {
            if (DEBUGLEVEL > 0) System.err.println("Exception caught in setException: " + e.toString());
            handleCleanException(e);
            return;
        }
        putSymbol("Null");
    }


	protected void setComplexCls() throws MathLinkException {

		String sym;

		// Link will have a single integer.
		try {
			int id = getInteger();
			newPacket();
			Class cls = objectHandler.classFromID(id);
			sym = setComplexClass(cls) ? "Null" : "$Failed";
		} catch (Exception e) {
			if (DEBUGLEVEL > 0) System.err.println("Exception caught in setComplex: " + e.toString());
			handleCleanException(e);
			return;
		}
		putSymbol(sym);
	}


	// If class being loaded implements a function onLoadClass(MathLink), call it.
	protected void callOnLoadClass() throws MathLinkException {

		try {
			int classID = getInteger();
			newPacket();
			objectHandler.callOnLoadClass(this, classID);
		} catch (Exception e) {
			if (DEBUGLEVEL > 0) System.err.println("Exception caught in callOnLoadClass: " + e.toString());
			handleCleanException(e);
			return;
		}
		putFunction("ReturnPacket", 1);
		putSymbol("Null");
		endPacket();
	}


	// If class being loaded implements a function onUnloadClass(MathLink), call it.
	protected void callOnUnloadClass() throws MathLinkException {

		try {
			int classID = getInteger();
			newPacket();
			objectHandler.callOnUnloadClass(this, classID);
		} catch (Exception e) {
			if (DEBUGLEVEL > 0) System.err.println("Exception caught in callOnUnloadClass: " + e.toString());
			handleCleanException(e);
			return;
		}
		putFunction("ReturnPacket", 1);
		putSymbol("Null");
		endPacket();
	}


	protected void reflect() throws MathLinkException {

		int classID;
		int type;
		boolean includeInherited = true;
		int num = 0;

		try {
			classID = getInteger();
			// type from same set as callJava uses: 1 == ctor, 2 == method, 3 == field.
			type = getInteger();
			includeInherited = getSymbol().equals("True");
			newPacket();
			// First time, just count the length of the resulting list.
			num = objectHandler.reflect(this, classID, type, includeInherited, false);
		} catch (Exception e) {
			if (DEBUGLEVEL > 0) System.err.println("Exception caught in reflect: " + e.toString());
			handleCleanException(e);
			return;
		}
		putFunction("List", num);
		// Now send the data. Ignore exception because we won't get here if it could possibly be thrown here
        // (it would have bee nthrown in first call above).
        try {
            objectHandler.reflect(this, classID, type, includeInherited, true);
        } catch (InvalidClassException e) {}
	}


	// This method does the best it can to try to bring a window to the foreground. On Mac (classic or OS X) and Windows, we need
	// to go into C code to do this fully. This class does what can be done from Java, and the WrappedKernelLink implementation
	// (which might be holding a NativeLink) calls into C when required.
	protected void showInFront() throws MathLinkException {

		try {
			Object obj = getObject();
			newPacket();
            if (Utils.isMacOSX()) {
                // Setting up the MRJ handlers (e.g., About box) is done here. This has no effect for the vast
                // majority of OSX users. It only matters if J/Link was launched via "java" instead of
                // JLink.app. In that case, the first time Carbon is initialized (e.g., a window is created)
                // J/Link will show up as an app in the dock and with its own menu. We want it to function
                // as normally as possible so we give it some handlers here.
                // For normal JLink.app launches, the NSUIElement property is set in the Info.plist file,
                // which prevents the J/Link menu and dock icon from appearing, so no need for these handlers
                // (but harmless to set them).
                try {
                    Class mrjHandlerCls = Class.forName("com.wolfram.jlink.MRJHandlers");
                    java.lang.reflect.Method setupMeth = mrjHandlerCls.getDeclaredMethod("setup", new Class[]{});
                    setupMeth.invoke(null, null);
                } catch (Exception e) {}
            }
			if (obj instanceof java.awt.Dialog) {
				java.awt.Dialog dlg = (java.awt.Dialog) obj;
				// This will hang until dialog is dismissed if it is modal.
				dlg.show();
				// The toFront() method would display a modal dialog for a second time.
				if (!dlg.isModal())
					dlg.toFront();
			} else {
				java.awt.Window windowObj = (java.awt.Window) obj;
				windowObj.setVisible(true);
				if (windowObj instanceof java.awt.Frame)
					((java.awt.Frame) windowObj).setState(java.awt.Frame.NORMAL);
				windowObj.toFront();
			}
		} catch (Exception e) {
			if (DEBUGLEVEL > 0) System.err.println("Exception caught in showInFront: " + e.toString());
			handleCleanException(e);
			return;
		}
		putSymbol("Null");
	}


	protected void connectToFEServer() throws MathLinkException {

		boolean result = false;
		MathLink feServerLink = null;

		try {
			String linkName = getString();
			String protocol = getString();
			newPacket();
			if (DEBUGLEVEL > 1) System.err.println("In connectToFEServer. linkname is: " + linkName);
			String mlArgs = "-linkmode connect -linkname " + linkName;
			if (!protocol.equals(""))
				mlArgs = mlArgs + " -linkprotocol " + protocol;
			feServerLink = MathLinkFactory.createMathLink(mlArgs);
			// Do nothing if link open fails. Return value of "False" will be sufficient to indicate this,
			// although I cannot currently distinguish between "link open failed" and "problem during link setup".
			if (feServerLink != null) {
				try {
					feServerLink.connect();
					if (DEBUGLEVEL > 1) System.err.println("just did connect of FE server link");
					feServerLink.putFunction("InputNamePacket", 1);
					feServerLink.put("In[1]:=");
					if (DEBUGLEVEL > 1) System.err.println("about to flush");
					feServerLink.flush();
					if (DEBUGLEVEL > 1) System.err.println("just finished flush. error code was: " + feServerLink.error());
					while (true) {
						// Here we peel off the initialization that the FE sends to the kernel when it first starts up a link.
						// We know that the first EnterTextPacket or EnterExpressionPacket is the content of the evaluating cell,
						// so we are done.
						MLFunction f = feServerLink.getFunction();
						feServerLink.newPacket();
						if (f.name.equals("EnterTextPacket") || f.name.equals("EnterExpressionPacket")) {
							result = true;
							break;
						} else if (f.name.equals("EvaluatePacket")) {
							feServerLink.putFunction("ReturnPacket", 1);
							feServerLink.putSymbol("Null");
						} else if (DEBUGLEVEL > 0) {
							System.err.println("Unexpected packet during FE server setup: " + f.name);
						}
					}
				} catch (MathLinkException e) {
					// These are exceptions dealing with the fe link, not the kernel link.
					if (DEBUGLEVEL > 0) System.err.println("MathLinkException during FE server setup: " + e.toString());
					feServerLink.close();
					feServerLink = null;
				}
			}
		} catch (Exception e) {
			if (DEBUGLEVEL > 0) System.err.println("Exception caught in connectToFEServer: " + e.toString());
			handleCleanException(e);
			return;
		}
		setFEServerLink(feServerLink);
		putFunction("ReturnPacket", 1);
		put(result);
		endPacket();
	}


	protected void disconnectToFEServer() throws MathLinkException {

		getFEServerLink().close();
		setFEServerLink(null);
		putFunction("ReturnPacket", 1);
		putSymbol("Null");
		endPacket();
	}


	protected void peekClasses() throws MathLinkException {

		objectHandler.peekClasses(this);
	}


	protected void peekObjects() throws MathLinkException {

		objectHandler.peekObjects(this);
	}


	protected void getClassPath() throws MathLinkException {
		// Link will be empty
		put(objectHandler.getClassLoader().getClassPath());
	}


	protected void addToClassPath() throws MathLinkException {

		try {
			String[] dirs = (String[]) getArray(TYPE_STRING, 1);
            boolean searchForJars = getBoolean();
            boolean prepend = getBoolean();
			newPacket();
            objectHandler.getClassLoader().addLocations(dirs, searchForJars, prepend);
		} catch (Exception e) {
			handleCleanException(e);
			return;
		}
		putSymbol("Null");
	}


    protected void setUserDir() throws MathLinkException {

        try {
            String userDir = getString();
            newPacket();
            try { System.setProperty("user.dir", userDir); } catch (Exception ee) {}
        } catch (Exception e) {
            handleCleanException(e);
            return;
        }
        putSymbol("Null");
    }


	protected void uiThreadWaiting() throws MathLinkException {
		newPacket();
		putSymbol(StdLink.uiThreadWaiting() ? "True" : "False");
	}


	protected void allowUIComputations() throws MathLinkException {

		try {
			boolean allow = getSymbol().equals("True");
			boolean enteringModal = getSymbol().equals("True");
			newPacket();
			StdLink.allowUIComputations(allow, enteringModal);
		} catch (Exception e) {
			handleCleanException(e);
			return;
		}
		putSymbol("Null");
	}


	// Called during ShareKernel loop to cause the kernel to consume less than 100% CPU.
	protected void yieldTime() throws MathLinkException {

		try {
			int millis = getInteger();
			newPacket();
			try { Thread.sleep(millis); } catch (InterruptedException ee) {}
		} catch (Exception e) {
			handleCleanException(e);
			return;
		}
		putSymbol("Null");
	}

	protected void getConsole() throws MathLinkException {

		putReference(com.wolfram.jlink.ui.ConsoleWindow.getInstance());
	}


    // Make doConnect an argument because a derived class might want to separate that action
    // from the rest of this logic.
	protected void extraLinks(boolean doConnect) throws MathLinkException {

		// Link will have the link name and protocol as strings, followed by linkSnooperCmdLine as string.
        String uiName, preName, prot, linkSnooperCmdLine;
		try {
            uiName = getString();
            preName = getString();
            prot = getString();
            linkSnooperCmdLine = getString();
			newPacket();
		} catch (Exception e) {
			if (DEBUGLEVEL > 0) System.err.println("Exception caught in extraLinks: " + e.toString());
			handleCleanException(e);
			return;
		}

        boolean result = true;
        KernelLink ui = null;
        try {
            if (linkSnooperCmdLine.length() > 0) {
                // An undocumented method for debugging UILink traffic.
                linkSnooperCmdLine = linkSnooperCmdLine + " -kernelmode connect -kernelname " + uiName + " -kernelprot " + prot + " -feSide J";
                String[] args = {"-linkmode", "launch", "-linkname", linkSnooperCmdLine};
                ui = MathLinkFactory.createKernelLink(args);
            } else {
                ui = MathLinkFactory.createKernelLink("-linkname " + uiName + " -linkconnect -linkprotocol " + prot);
            }
            StdLink.setUILink(ui);
            ((KernelLinkImpl) ui).setObjectHandler(objectHandler);
        } catch (Throwable e) {
            if (ui != null) {
                ui.close();
                ui = null;
            }
            result = false;
        }
        KernelLink pre = null;
        try {
            pre = MathLinkFactory.createKernelLink("-linkname " + preName + " -linkconnect -linkprotocol " + prot);
            ((KernelLinkImpl) pre).setObjectHandler(objectHandler);
        } catch (Throwable e) {
            if (pre != null) {
                pre.close();
                pre = null;
            }
            result = false;
        }
        put(result);
        flush();
        if (ui != null && doConnect) {
            ui.connect();
        }
        if (pre != null && doConnect) {
            pre.connect();
            new Reader(pre, false, false).start();
        }
	}


    protected void getWindowID() throws MathLinkException {

        long id = -1;

        // Link will have the window object.
        try {
            Object obj = getObject();
            newPacket();
            if (obj instanceof java.awt.Window)
                id = getNativeWindowHandle((java.awt.Window) obj);
        } catch (Exception e) {
            if (DEBUGLEVEL > 0) System.err.println("Exception caught in getWindowID: " + e.toString());
            handleCleanException(e);
            return;
        }
        put(id);
    }

    // This method adds a callback to Mathematica when a window's title is changed.
    // This is so we can inform the FE that the window's title has changed. This is done
    // in Java instead of in M using a MathPropertyChangeListener, because we only want to
    // generate a call to M for title changes, not all other properties as well.
    protected void addTitleChangeListener() throws MathLinkException {

        // Link will have the window object followed by M func name.
        try {
            Object obj = getObject();
            final String titleChangedFunc = getString();
            newPacket();
            ((java.awt.Window) obj).addPropertyChangeListener(new java.beans.PropertyChangeListener() {
                public void propertyChange(java.beans.PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals("title")) {
                        KernelLink ml = StdLink.getLink();
                        StdLink.requestTransaction();
                        // We need to snyc on ml because the Reader thread is also trying to use the link. We must be sure we complete
                        // an entire transaction before giving up control of the link.
                        synchronized (ml) {
                            try {
                                ml.putFunction("EvaluatePacket", 1);
                                ml.putFunction(titleChangedFunc, 2);
                                ml.put(evt.getSource());
                                ml.put(evt.getNewValue());
                                ml.endPacket();
                                ml.discardAnswer();
                            } catch (MathLinkException exc) {
                                ml.clearError();
                                ml.newPacket();
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            if (DEBUGLEVEL > 0) System.err.println("Exception caught in addTitleChangeListener: " + e.toString());
            handleCleanException(e);
            return;
        }
        putSymbol("Null");
    }


    protected void setVMName() throws MathLinkException {

        try {
            String name = getString();
            newPacket();
            objectHandler.setVMName(name);
        } catch (Exception e) {
            handleCleanException(e);
            return;
        }
        putSymbol("Null");
    }


    ///////////////////////  Abstract  /////////////////////////////

    // Returns a unique ID for each Java window in a session. On Windows,
    // the ID should be the HWND. On other platforms it has no special significance
    // beyond its uniqueness. This is to support the ability to register Java windows
    // to be managed by the FE.
    abstract protected long getNativeWindowHandle(java.awt.Window obj);


    ///////////////////////////////////////////////////////////
    
    private void discardAnswerNoPacketListeners() throws MathLinkException {

		// We're already synchronized here...
		Vector v = packetListeners;
		packetListeners = new Vector(0);
		discardAnswer();
		packetListeners = v;
	}



////////////////////////////////////  getArray() Overloads  //////////////////////////////////////


    public synchronized Object getArray(Class elementType, int depth) throws MathLinkException {
        return getArray(elementType, depth, null);
    }

    public synchronized Object getArray(Class elementType, int depth, String[] heads) throws MathLinkException {

        // Although this method is intended for object arrays, detect cases where user specifies a
        // primitive type and make these go via the older getArray() API, which is optimized for
        // primitive arrays.

        int type = TYPE_OBJECT;

        if (elementType != null && elementType.isPrimitive()) {
            if (elementType == int.class)
                type = TYPE_INT;
            else if (elementType == double.class)
                type = TYPE_DOUBLE;
            else if (elementType == byte.class)
                type = TYPE_BYTE;
            else if (elementType == char.class)
                type = TYPE_CHAR;
            else if (elementType == short.class)
                type = TYPE_SHORT;
            else if (elementType == long.class)
                type = TYPE_LONG;
            else if (elementType == float.class)
                type = TYPE_FLOAT;
            else if (elementType == boolean.class)
                type = TYPE_BOOLEAN;
        /*
        // Would be nice to make getArray(String.class, n) work for string arrays, but this causes problems
        // elsewhere. Similarly for BigInteger and other types that could come in as either a value or an
        // object ref. To see this break, uncomment these lines and call Testing`objectArrayIdentityTwo2 on
        // an m x n array of String references.
        } else if (elementType == String.class) {
            type = TYPE_STRING;
        } else if (elementType == java.math.BigInteger.class) {
            type = TYPE_BIGINTEGER;
        } else if (elementType == java.math.BigDecimal.class) {
            type = TYPE_BIGDECIMAL;
        } else if (elementType == Expr.class) {
            type = TYPE_EXPR;
        } else if (elementType != null && elementType == getComplexClass()) {
            type = TYPE_COMPLEX;
        */
        }

        return getArray0(type, depth, heads, elementType);
    }

    public synchronized Object getArray(int type, int depth, String[] heads) throws MathLinkException {
        return getArray0(type, depth, heads, null);
    }

    public synchronized Object getArray(int type, int depth) throws MathLinkException {
        return getArray0(type, depth, null, null);
    }

	// Worker function for getting arrays of objects. All non-object cases are forwarded to superclass.
	// This function cannot do automatic flattening of arrays that are deeper than the requested depth,
	// which is done for other array types.
    private Object getArray0(int type, int depth, String[] heads, Class elementType) throws MathLinkException {

        Object resultArray = null;

        if (type == TYPE_OBJECT) {
            int actualDepth;
            Object firstInstance = null;
            // Figure out the depth of the array and its leaf class type. Note that the detected leaf class type
            // is only used of the elementType argument is null.
            long mark = createMark();
            try {
                MLFunction mf = getFunction();
                actualDepth = 1;
                if (mf.argCount == 0) {
                    // User is passing {} to specify a 0-length array of objects. We don't have type information
                    // here, so we don't know what type the array should be. Returning null would not be very useful,
                    // so we reutrn a 0-length array of Object.
                    firstInstance = new Object();
                } else {
                    while (actualDepth < 5) {
                        int tok = getNext();
                        if (tok == MLTKFUNC) {
                            getFunction();
                            actualDepth++;
                        } else {
                            break;
                        }
                    }
                    // We are now poised to read the first leaf element of the array.
                    firstInstance = getObject();
                }
            } finally {
                seekMark(mark);
                destroyMark(mark);
            }
            // Ignore the class of the first instance if user supplied a non-null elementClass argument.
            Class leafClass = elementType != null ? elementType : firstInstance.getClass();
            if (actualDepth < depth)
                throw new MathLinkException(MLE_ARRAY_TOO_SHALLOW);
            // Up through here, we have done what is necessary to flatten an array that is deeper than
            // requested. We have determined the actual depth of the array, and not just used the
            // requested depth. From here on, though, we assume the array is no deeper than requested.
            if (depth == 1) {
                MLFunction func = getFunction();
                resultArray = Array.newInstance(leafClass, func.argCount);
                for (int i = 0; i < func.argCount; i++) {
                    Array.set(resultArray, i, getObject());
                }
                if (heads != null)
                    heads[0] = func.name;
            } else {
                // We need to call getArraySlices ourselves, rather than just letting it happen from super.getArray(),
                // because we need to supply the array element class manually.
                String compClassName = "L" + leafClass.getName() + ";";
                // Start loop from one, as we are determining the _component_ class name.
                for (int i = 1; i < actualDepth; i++)
                    compClassName = "[" + compClassName;
                Class componentClass = null;
                try {
                    // Note that we use the leafClass loader, not JLinkClassLoader (which will not always be the same).
                    componentClass = Class.forName(compClassName, true, leafClass.getClassLoader());
                } catch (ClassNotFoundException e) {
                    // Should never happen.
                    if (DEBUGLEVEL > 0) System.out.println("Could not find component class in getArray(). " + e.toString());
                }
                resultArray = getArraySlices(type, depth, heads, 0, componentClass);
            }
        } else {
            resultArray = super.getArray(type, depth, heads);
        }
        return resultArray;
    }


////////////////////////////////////  Utility funcs  //////////////////////////////////////

	private Object getTypeObjectPair() throws MathLinkException, NumberRangeException {

		Object result = null;

		int type = getInteger();
		int i;

		// Replace TYPE_FLOATORINT and TYPE_DOUBLEORINT (and arrays of them) with just TYPE_FLOAT and TYPE_DOUBLE.
		// Those "ORINT" constants are just for pattern matching in Mathematica. They could be stripped out of the RHS of
		// the jCallJava definitions in Mathematica, but it is more efficient and convenient to just do it here, as the args
		// are being read.
		if (type % TYPE_ARRAY1 == TYPE_FLOATORINT)
			type = TYPE_FLOAT + TYPE_ARRAY1 * (type / TYPE_ARRAY1);
		else if (type % TYPE_ARRAY1 == TYPE_DOUBLEORINT)
			type = TYPE_DOUBLE + TYPE_ARRAY1 * (type / TYPE_ARRAY1);

		switch (type) {
			case TYPE_INT:
				result = new Integer(getInteger());
				break;
			case TYPE_LONG:
				result = new Long(getLongInteger());
				break;
			case TYPE_SHORT:
				i = getInteger();
				if (i < Short.MIN_VALUE || i > Short.MAX_VALUE)
					throw new NumberRangeException(i, "short");
				result = new Short((short)i);
				break;
			case TYPE_BYTE:
				i = getInteger();
				if (i < Byte.MIN_VALUE || i > Byte.MAX_VALUE)
					throw new NumberRangeException(i, "byte");
				result = new Byte((byte)i);
				break;
			case TYPE_CHAR:
				i = getInteger();
				if (i < Character.MIN_VALUE || i > Character.MAX_VALUE)
					throw new NumberRangeException(i, "char");
				result = new Character((char)i);
				break;
			case TYPE_FLOAT:
			case TYPE_FLOATORINT: {
				double d = getDouble();
				if (d < -Float.MAX_VALUE || d > Float.MAX_VALUE)
					throw new NumberRangeException(d, "float");
				result = new Float((float)d);
				break;
			}
			case TYPE_DOUBLE:
			case TYPE_DOUBLEORINT:
				result = new Double(getDouble());
				break;
			case TYPE_STRING: {
				// Could be a string or an object reference.
				int tok = getType();
				if (tok == MLTKOBJECT) {
					result = getObject();
				} else {
					// Is a raw string
					result = getString();
					// Check for the symbol Null.
					if (tok == MLTKSYM && result.equals("Null"))
						result = null;
				}
				break;
			}
			case TYPE_BOOLEAN: {
				String s = getSymbol();
				if (s.equals("True"))
					result = Boolean.TRUE;
				else
					result = Boolean.FALSE;
				break;
			}
			case TYPE_COMPLEX: {
				// Could be a complex or an object reference.
				long mark = createMark();
				try {
					int tok = getNext();
					if (tok == MLTKOBJECT) {
						result = getObject();
					} else if (tok == MLTKSYM) {
						result = getSymbol();
						if (result.equals("Null")) {
							result = null;
						} else {
							// If it was a symbol but not null, just back up and read it using getComplex()
							// so that we get the exception that it will throw.
							seekMark(mark);
							result = getComplex();
						}
					} else {
						// Is a raw Complex
						seekMark(mark);
						result = getComplex();
					}
				} finally {
					destroyMark(mark);
				}
				break;
			}
			case TYPE_BIGINTEGER: {
				// Could be an int or an object reference.
				long mark = createMark();
				try {
					int tok = getType();
					if (tok == MLTKOBJECT) {
						result = getObject();
					} else if (tok == MLTKSYM) {
						result = getSymbol();
						if (result.equals("Null")) {
							result = null;
						} else {
							// Will throw.
							result = new java.math.BigInteger((String) result);
						}
					} else {
						result = new java.math.BigInteger(getString());
					}
				} finally {
					destroyMark(mark);
				}
				break;
			}
			case TYPE_BIGDECIMAL: {
				// Could be an number or an object reference.
				long mark = createMark();
				try {
					int tok = getType();
					if (tok == MLTKOBJECT) {
						result = getObject();
					} else if (tok == MLTKSYM) {
						result = getSymbol();
						if (result.equals("Null")) {
							result = null;
						} else {
							// Will throw.
							result = Utils.bigDecimalFromString((String) result);
						}
					} else {
						result = Utils.bigDecimalFromString(getString());
					}
				} finally {
					destroyMark(mark);
				}
				break;
			}
			case TYPE_EXPR: {
				// Could be a "true" expr or an object reference.
				long mark = createMark();
				try {
					int tok = getNext();
					if (tok == MLTKOBJECT) {
						result = getObject();
						if (result != null)
							break;
					}
					seekMark(mark);
					result = getExpr();
				} finally {
					destroyMark(mark);
				}
				break;
			}
			case TYPE_OBJECT: {
				result = getObject();
				break;
			}
			case TYPE_BAD:
				break;
			default: {
				// The thing on the link might be a list of values or it might be an
				// instance index of an array object living on the Java side. Need to distinguish these.
				int tok = getNext();
				if (tok == MLTKOBJECT || tok == MLTKSYM) {   // MLTKSYM case for Null
					result = getObject();
				} else {
					// Recall that these constants are negative, so comparison is >.
					if (type > TYPE_ARRAY2)
						result = getArray(type - TYPE_ARRAY1, 1);
					else if (type > TYPE_ARRAY3)
						result = getArray(type - TYPE_ARRAY2, 2);
					else if (type > TYPE_ARRAY4)
						result = getArray(type - TYPE_ARRAY3, 3);
					else if (type > TYPE_ARRAY5)
						result = getArray(type - TYPE_ARRAY4, 4);
					else
						result = getArray(type - TYPE_ARRAY5, 5);
				}
			}
		}
		return result;
	}

	// Tests whether symbol waiting on link is a valid object reference. Returns false for the symbol Null.
	// Called by getNext() and getType(), after it has already been verified that the type is MLTKSYM.
	protected boolean isObject() {

	 	long mark = 0;
	 	try {
	 		mark = createMark();
	 		// Note that this behavior means that the symbol Null on the link will result in MLTKSYM, not MLTKOBJECT.
	 		// This is desired (?) for backward compatibility.
	 		return getObject() != null;
	 	} catch (MathLinkException e) {
	 		clearError();
	 		return false;
	 	} finally {
	 		if (mark != 0) {
	 			seekMark(mark);
	 			destroyMark(mark);
	 		}
	 	}
	}


    // Exceptions that fail this test will cause J/Link to exit. Add any Error types here that
    // J/Link should not consider fatal.
    private boolean isRecoverableException(Throwable t) {
        return (t instanceof Exception) ||
                    (t instanceof OutOfMemoryError) ||
                        (t instanceof LinkageError) ||  // e.g., UnsatisfiedLinkError, NoClassDefFoundError, etc.
                            (t instanceof AssertionError);
    }


}


/////////////////////////////////  NumberRangeException  /////////////////////////////////////////

// A private class that is thrown when an arg to an installable function is too wide.
// For example, a method that takes a byte will be typed in Mathematica as _Integer,
// so a user might call it with 1000.

class NumberRangeException extends Exception {

	private int ivalue = 0;
	private double dvalue = 0.0;
	private String type;

	public NumberRangeException(int offendingValue, String type) {
		this.ivalue = offendingValue;
		this.type = type;
	}

	public NumberRangeException(double offendingValue, String type) {
		this.dvalue = offendingValue;
		this.type = type;
	}

	public String toString() {
		return "NumberRangeException: Argument " + (ivalue != 0 ? String.valueOf(ivalue) : String.valueOf(dvalue)) + " out of range for parameter type " + type + ".";
	}
}
