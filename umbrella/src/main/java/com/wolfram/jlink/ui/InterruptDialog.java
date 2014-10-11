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

package com.wolfram.jlink.ui;

import com.wolfram.jlink.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;


/**
 * This is an "interrupt computation" dialog similar to the one you see in the notebook front
 * end when you select Kernel/Interrupt Evaluation. The dialog that appears has choices for
 * aborting, quitting the kernel, etc., depending on what the kernel is doing at the time.
 * <p>
 * This class is a standard PacketListener, and you use it like any other. For example:
 * <pre>
 *     ml.addPacketListener(new InterruptDialog(myParentFrame));</pre>
 * After this, whenever you interrupt a computation (by sending an MLINTERRUPTMESSAGE or, more
 * commonly, by calling the KernelLink.interruptEvaluation() method),
 * a modal dialog box will apppear with choices for how to proceed.
 * <p>
 * You must provide a Frame or Dialog object in the constructor. This will be the parent
 * window of the modal interrupt dialog.
 * 
 * @since 2.0
 */
public class InterruptDialog extends JDialog implements ActionListener, PacketListener {
	
	private String response;
	private JButton b1, b2, b3, b4, b5, b6, b7;
	Frame parentFrame;
	Dialog parentDialog;
	
	/**
	 * 
	 * @param parent The top-level window that will be the dialog's parent. Must not be null.
	 */
	public InterruptDialog(Frame parent) {
		super(parent, true);
		parentFrame = parent;
		setup();
	}
		
	/**
	 * 
	 * @param parent The top-level window that will be the dialog's parent. Must not be null.
	 */
	public InterruptDialog(Dialog parent) {
		super(parent, true);
		parentDialog = parent;
		setup();
	}
	
		
	private void setup() {
		
		setTitle("Interrupt");
		setResizable(false);
		Container contentPane = getContentPane();
		JPanel p = new JPanel();
		contentPane.add(p);
		p.setLayout(new GridLayout(9, 1, 6, 6));
		b1 = new JButton("Abort Command Being Evaluated");
		b2 = new JButton("Enter Inspector Dialog");
		b3 = new JButton("Send Interrupt to Linked Program");
		b4 = new JButton("Send Abort to Linked Program");
		b5 = new JButton("Kill Linked Program");
		b6 = new JButton("Continue Evaluation");
		b7 = new JButton("Quit the Mathematica Kernel");
		Panel p1 = new Panel();
		Panel p2 = new Panel();
		p.add(b1);
		p.add(b2);
		p.add(p1);
		p.add(b3);
		p.add(b4);
		p.add(b5);
		p.add(p2);
		p.add(b6);
		p.add(b7);
		b1.addActionListener(this);
		b2.addActionListener(this);
		b3.addActionListener(this);
		b4.addActionListener(this);
		b5.addActionListener(this);
		b6.addActionListener(this);
		b7.addActionListener(this);
		pack();
		doLayout();
		// To get the best-looking dimensions on all platforms, we let the components get laid out
		// at their preferred sizes, and now go back and manually grow the panel so that it does not
		// fill the entire dialog.
		Rectangle r = p.getBounds();
		contentPane.setLayout(null);
		p.setBounds(r.x + 10, r.y + 5, r.width, r.height);
		setSize(getSize().width + 20, getSize().height + 10);
	}
	
	
	public boolean packetArrived(PacketArrivedEvent evt) throws MathLinkException {

		boolean allowFurtherProcessing = true;
		KernelLink ml = (KernelLink) evt.getSource();
		if (evt.getPktType() == MathLink.MENUPKT) {
			int type = ml.getInteger();
			String prompt = ml.getString();
			doDialog(type);
			ml.put(response != null ? response : "c");  // Continue is the default response
			ml.flush();
			response = null;
			// It would be disastrous if another listener tried to handle the MENUPKT also, so we return
			// false to prevent any further processing.
			allowFurtherProcessing = false;
		}
		return allowFurtherProcessing;
	}

	
	private void doDialog(int type) {
		
		switch (type) {
			case 1:
				// Interrupt during computation.
				b1.setEnabled(true);
				b2.setEnabled(true);
				b3.setEnabled(false);
				b4.setEnabled(false);
				b5.setEnabled(false);
				break;
			case 3:
				// Interrupt during LinkRead (kernel waiting for reply from another MathLink program).
				b1.setEnabled(false);
				b2.setEnabled(false);
				b3.setEnabled(true);
				b4.setEnabled(true);
				b5.setEnabled(true);
				break;
			default:
				// No handling yet for other MenuPackets (at least one other exists--something from license manager)
				return;
		}
		if (parentFrame != null)
			setLocationRelativeTo(parentFrame);
		else
			setLocationRelativeTo(parentDialog);
		doLayout();
		show();
	}
	

	public void actionPerformed(ActionEvent evt) {
		
		Object source = evt.getSource();
		if (source == b1)
			response = "a";    // abort
		else if (source == b2)
			response = "i";    // enter dialog
		else if (source == b3)
			response = "r";    // send interrupt to linked
		else if (source == b4)
			response = "a";    // send abort to linked
		else if (source == b5)
			response = "k";    // kill linked
		else if (source == b6)
			response = "c";    // continue
		else if (source == b7)
			response = "exit"; // quit kernel
		dispose();
	}

}
