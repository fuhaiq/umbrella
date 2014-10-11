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

import javax.swing.*;
import java.awt.AWTEvent;
import java.awt.event.*;

/**
 * MathJFrame is intended to be used from Mathematica code, as a top-level window for
 * user-interface elements created and displayed from a Mathematica program.
 * MathJFrame is a simple subclass of javax.swing.JFrame that adds three features that are
 * convenient for J/Link programmers. The first is that it calls dispose() on itself
 * when its close box is clicked (this is not the default behavior of a JFrame window).
 * The second feature is that it knows about so-called &quot;modal&quot; interaction
 * with the Mathematica kernel, as described in Part 1 of the J/Link User Guide. If its
 * setModal() method has been called, then when its close box is clicked a MathJFrame sends to
 * Mathematica EndModal[], which will cause DoModal[] to return Null. The third feature is
 * the onClose() method, which allows you to supply arbitrary Mathematica code that will be executed
 * when the window is closed.
 * <p>
 * You can use MathJFrame in Java programs as well.
 *
 * @see MathFrame
 */

public class MathJFrame extends JFrame {

	// isModal controls whether we try to interact with the kernel when the window is closed.
	// If true, we send (EndModal[];), which explains the name, but this is fine (useless but harmless)
	// for modeless operation as well. It is OK as long as the kernel is prepared for callbacks
	// from Java, as will be the case after DoModal[] (modal style) or ShareKernel[] (modeless style).
	protected boolean isModal;
	protected String onCloseCode;
	
	public MathJFrame() {
		super();
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
	}

	/**
	 * Lets you specify the window's title.
	 * 
	 * @param title the window title
	 */

	public MathJFrame(String title) {
		super(title);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
	}

	/**
	 * Marks this window as using the Mathematica kernel in a modal way. The sole effect of
	 * calling setModal() is to cause this window to evaluate EndModal[] in Mathematica
	 * when its close box is clicked. Make sure that you use the DoModal or ShareKernel
	 * fuctions in Mathematica if you call setModal().
	 * <p>
	 * Modal windows are discussed in detail in Part 1 of the J/Link User Guide.
	 */

	public void setModal() {
		isModal = true;
	}
	
	/**
	 * Lets you specify some Mathematica code to be executed when the window is closed.
	 * This will occur after any MathWindowListeners are through, and before the EndModal[]
	 * call triggered if you also called setModal(). Because the call happens after any MathWindowListeners,
	 * it is safe to turn off kernel sharing in your onClose() code if you are using a modeless-type
	 * interface. This is the typical use for onClose()--to unregister kernel or front end sharing, or clean
	 * up any outstanding object references, when your modeless window is finally closed. This can
	 * save you from having to write a "cleanup" Mathematica function that your users would need to call
	 * after they finished with your modeless window.
	 * 
	 * @param code the Mathematica code to execute
	 */

	public void onClose(String code) {
		onCloseCode = code + ";";
	}
	
	protected void processWindowEvent(WindowEvent e) {
		// This is the method in which the parent class farms out the event to WindowListeners.
		// We'll just call the inherited implementation, and then tack on our call for the onClose() code.
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            KernelLink ml = StdLink.getLink();
			if (ml != null) {
				if (onCloseCode != null) {
					StdLink.requestTransaction();
					synchronized (ml) {
						try {
							// We send this using EnterTextPacket because of issues with UnshareKernel, which is not uncommon
							// to call from an onClose handler. See the comments in the UnshareKernel implementation for an explanation.
							ml.putFunction("EnterTextPacket", 1);
							ml.put(onCloseCode);
							ml.discardAnswer();
						} catch (MathLinkException ee) {
							ml.clearError();
							ml.newPacket();
						}
					}
				}
				// Note that it would be bad if we had used setModal(), yet used a modeless interface (we did
				// not run DoModal[]), and also used the onClose() code to turn off kernel sharing. Then the
				// kernel would not be ready to handle this upcoming EndModal[]. The solution is...don't do this.
				// Don't use setModal() if you are not going to call DoModal[].
				if (isModal) {
					if (ml.equals(StdLink.getLink()))
						StdLink.requestTransaction();
					synchronized (ml) {
						try {
							ml.evaluate("EndModal[]");
							ml.discardAnswer();
						} catch (MathLinkException ee) {
							ml.clearError();
							ml.newPacket();
						}
					}
				}
			}
		}
	}

}