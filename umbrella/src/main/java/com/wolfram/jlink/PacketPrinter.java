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

import java.io.PrintStream;

/**
 * PacketPrinter is an implementation of the PacketListener interface that prints
 * the contents of incoming packets to a stream that you specify.
 * <p> 
 * PacketPrinter is useful for debugging, when you want to see exactly what
 * Mathematica is sending you. Here is how you might use it:
 * <pre>
 *     // ml is a KernelLink
 *     PacketListener stdoutPrinter = new PacketPrinter(System.out);
 *     ml.addPacketListener(stdoutPrinter);</pre>
 * 
 * After the execution of the above code, the contents of all packets that arrive during
 * calls to the KernelLink methods waitForAnswer(), discardAnswer(), or any of the
 * &quot;evaluateTo&quot; set will be printed to System.out. No matter how many
 * PacketListeners are registered, their monitoring of incoming packets will never
 * interfere with any other packet-handling code in your program, so you can turn on and
 * off this full packet tracing by just adding or removing the two lines above. No other
 * changes whatsoever are required to your program.
 * 
 * @see PacketListener
 * @see PacketArrivedEvent
 * @see KernelLink
 */

public class PacketPrinter implements PacketListener {

	private PrintStream strm;

	/**
	 * Prints packets to System.out.
	 */

	public PacketPrinter() {
		this(System.out);
	}

	/**
	 * @param strm the stream on which packets will be printed
	 */

	public PacketPrinter(PrintStream strm) {
		this.strm = strm;
	}

	/**
	 * Invoked when a packet arrives on the link.
	 * 
	 * @param evt the PacketArrivedEvent
	 * @return true
	 * @exception com.wolfram.jlink.MathLinkException
	 */
    
	public boolean packetArrived(PacketArrivedEvent evt) throws MathLinkException {

		KernelLink ml = (KernelLink) evt.getSource();
        int pkt = evt.getPktType();
        // CallPackets typically have 2 args, but this is not guaranteed, as some uses (like
        // the kernel calling the front end) only have 1 arg. We have gotten a little loose
        // with CallPacket usage, so to be safe we restrict ourselves to one arg and fail
        // to print the second if it is present.
        int argCount = pkt == MathLink.MESSAGEPKT ? 2 : 1;
       
        strm.println("Packet type was " + pktToName(pkt) + ". Contents follows.");
        for (int i = 0; i < argCount; i++) {
            Expr e = ml.getExpr();
    		strm.println(e.toString());
    		e.dispose();
        }
		return true;
	}
	
	private static String pktToName(int pkt) {
		switch (pkt) {
			case MathLink.ILLEGALPKT:		return "ILLEGALPKT";
			case MathLink.CALLPKT:			return "CallPacket";
			case MathLink.EVALUATEPKT:		return "EvaluatePacket";
			case MathLink.RETURNPKT:		return "ReturnPacket";
			case MathLink.INPUTNAMEPKT:	return "InputNamePacket";
			case MathLink.ENTERTEXTPKT:	return "EnterTextPacket";
			case MathLink.ENTEREXPRPKT:	return "EnterExpressionPacket";
			case MathLink.OUTPUTNAMEPKT:	return "OutputNamePacket";
			case MathLink.RETURNTEXTPKT:	return "ReturnTextPacket";
			case MathLink.RETURNEXPRPKT:	return "ReturnExpressionPacket";
			case MathLink.DISPLAYPKT:		return "DisplayPacket";
			case MathLink.DISPLAYENDPKT:	return "DisplayEndPacket";
			case MathLink.MESSAGEPKT:		return "MessagePacket";
			case MathLink.TEXTPKT:			return "TextPacket";
			case MathLink.INPUTPKT:			return "InputPacket";
			case MathLink.INPUTSTRPKT:		return "InputStringPacket";
			case MathLink.MENUPKT:			return "MenuPacket";
			case MathLink.SYNTAXPKT:		return "SyntaxPacket";
			case MathLink.SUSPENDPKT:		return "SuspendPacket";
			case MathLink.RESUMEPKT:		return "ResumePacket";
			case MathLink.BEGINDLGPKT:		return "BeginDialogPacket";
			case MathLink.ENDDLGPKT:		return "EndDialogPacket";
			case MathLink.FEPKT:				return "Special Front End-Defined Packet";
			case MathLink.EXPRESSIONPKT:	return "Special Front End-Defined Packet: ExpressionPacket";
			default:								return "No such packet exists";
		}
	}
}
