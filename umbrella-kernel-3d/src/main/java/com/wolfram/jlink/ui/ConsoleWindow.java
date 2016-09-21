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

import java.io.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

/**
 * A top-level frame window that displays output printed to the System.out and/or System.err streams.
 * It has no input facilities.
 * <p>
 * This class is a singleton, meaning that there is only ever one instance in existence. It has no public constructors.
 * You call the getInstance() method to acquire the sole ConsoleWindow object.
 * <p>
 * Here is a code fragment that demonstrates how to use ConsoleWindow:
 * <pre>
 * 	ConsoleWindow cw = ConsoleWindow.getInstance();
 * 	cw.setLocation(100, 100);
 * 	cw.setSize(450, 400);
 * 	cw.show();
 * 	cw.setCapture(ConsoleWindow.STDOUT | ConsoleWindow.STDERR);
 * 	System.out.println("hello world from stdout");
 * 	System.err.println("hello world from stderr");</pre>
 * 
 * @since 2.0
 */
public class ConsoleWindow extends com.wolfram.jlink.MathJFrame {
	
	/**
	 * An integer constant for use in the setCapture() method that specifies that no streams
	 * should be captured.
	 */
	public static final int NONE = 0;

	/**
	 * An integer constant for use in the setCapture() method that specifies the System.out stream
	 * should be captured.
	 */
	public static final int STDOUT = 1;

	/**
	 * An integer constant for use in the setCapture() method that specifies the System.err stream
	 * should be captured.
	 */
	public static final int STDERR = 2;
	
	private static ConsoleWindow theConsoleWindow;
    
	private boolean isFirstTime = true;
    private final TextAreaOutputStream taos;
	private PrintStream strm;
    private OutputStream oldOut, oldErr;
    private boolean oldOutWasWrapped, oldErrWasWrapped;
    private boolean isCapturingOut = false;
    private boolean isCapturingErr = false;
    private Checkbox stdoutButton;
    private Checkbox stderrButton;
	
	/**
	 * Returns the sole ConsoleWindow instance.
	 */
	public static synchronized ConsoleWindow getInstance() {
		
		if (theConsoleWindow == null)
			theConsoleWindow = new ConsoleWindow();
		return theConsoleWindow;
	}
	
	/**
	 * Sets the maximum number of output lines to display in the scrolling window. The default is 1000.
	 * 
	 * @param maxLines
	 */
	public synchronized void setMaxLines(int maxLines) {
		taos.maxLines = maxLines;
	}
	
	/**
	 * Sets which streams to capture. No capturing occurs until this method is called. Each time it is called,
	 * the new stream specification overrides the previous one.
	 * 
	 * @param strmsToCapture The streams to capture, either STDERR, STDOUT, both (STDERR | STDOUT), or NONE
	 */
	public synchronized void setCapture(int strmsToCapture) {
		
        // See comments for ConsoleStream class for info on what is going on here with
        // setSystemStdoutStream(), oldOutWasWrapped, etc.
        if ((strmsToCapture & STDOUT) != 0) {
            ConsoleStream.setSystemStdoutStream(strm);
			isCapturingOut = true;
        } else {
            if (oldOutWasWrapped)
                ConsoleStream.setSystemStdoutStream(oldOut);
            else
                System.setOut((PrintStream) oldOut);
           isCapturingOut = false;
        } if ((strmsToCapture & STDERR) != 0) {
            ConsoleStream.setSystemStderrStream(strm);
            isCapturingErr = true;
        } else {
            if (oldErrWasWrapped)
                ConsoleStream.setSystemStderrStream(oldErr);
            else
                System.setErr((PrintStream) oldErr);
            isCapturingErr = false;
        }
        updateButtons();
	}

	/**
	 * Not for general use.
	 */
	public boolean isFirstTime() {
		return isFirstTime;
	}

	/**
	 * Not for general use.
	 * 
	 * @param first
	 */
	public void setFirstTime(boolean first) {
		isFirstTime = first;
	}


	private ConsoleWindow() {
		
		oldOut = System.out;
		oldErr = System.err;
		
        // See comments for ConsoleStream section for info on what is going on here.
        // To properly save the value of the old streams, we need to save a reference not
        // to the wrapping ConsoleStream object (which gets its behavior swapped in and out
        // and is a singleton anyway), but to the lowest-level wrapped stream. Then when
        // we stop capturing and restore the old behavior, we swap the old wrapped stream
        // back in.
        if (oldOut instanceof ConsoleStream.ConsolePrintStream) {
            // Note that we ignore the value of the incoming System.out. The fact that it is a
            // ConsolePrintStream is just a flag to tell us to acquire the singleton ConsoleStream
            // instance and save its old wrapped stream implementation. That is the true holder
            // of the old behavior.
            oldOut = ConsoleStream.getStdoutStream().getWrappedStream();
            oldOutWasWrapped = true;
        }
        if (oldErr instanceof ConsoleStream.ConsolePrintStream) {
            oldErr = ConsoleStream.getStderrStream().getWrappedStream();
            oldErrWasWrapped = true;
        }
        
		setTitle("J/Link Java Console");
		// Seems like this should be windowBorder, not control, but windowBorder is black on Windows...
		setBackground(SystemColor.control);
		setResizable(true);
		
		Button clearButton = new Button("Clear");
		Button closeButton = new Button("Close");
        Panel checkboxPanel = new Panel();
        stdoutButton = new Checkbox("System.out", isCapturingOut);
        stderrButton = new Checkbox("System.err", isCapturingErr);
		
		JTextArea ta = new JTextArea();
		taos = new TextAreaOutputStream(ta, 1000);
		strm = new PrintStream(taos, true);
		// Print version info in header.
		strm.println("J/Link version " + com.wolfram.jlink.KernelLink.VERSION);
		try {
			// Catch and ignore any SecurityException (very unlikely).
			String javaVersion = System.getProperty("java.version");
			String vmName = System.getProperty("java.vm.name"); // Not supported in older VMs.
			strm.println("Java version " + javaVersion + "  " + (vmName != null ? vmName : ""));
		} catch (Exception e) {}
		strm.println("-------------------------");
		
		ta.setEditable(false);
		ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.getViewport().add(ta);

        // Lay out components.
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		getContentPane().setLayout(gridbag);
		
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 0.95;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gridbag.setConstraints(scrollPane, gbc);
		getContentPane().add(scrollPane);
        
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 2;
        gbc.weighty = 0.05;
        Panel p = new Panel();
        gridbag.setConstraints(p, gbc);
        getContentPane().add(p);
        gbc.insets = new Insets(4, 10, 1, 10);
        gridbag.setConstraints(clearButton, gbc);
        getContentPane().add(clearButton);
        gridbag.setConstraints(closeButton, gbc);
        getContentPane().add(closeButton);
        
        gridbag = new GridBagLayout();
        gbc = new GridBagConstraints();
        p.setLayout(gridbag);
        Label lbl = new Label("Capture:");
        gbc.gridheight = 2;
        gridbag.setConstraints(lbl, gbc);
        p.add(lbl);
        gbc.gridheight = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(stdoutButton, gbc);
        p.add(stdoutButton);
        gridbag.setConstraints(stderrButton, gbc);
        p.add(stderrButton);

		clearButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (taos != null) taos.reset();
			}
		});
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispatchEvent(new WindowEvent(ConsoleWindow.this, WindowEvent.WINDOW_CLOSING));
            }
        });
        stdoutButton.addItemListener(new CheckboxItemListener());
        stderrButton.addItemListener(new CheckboxItemListener());
	}

    
    private void updateButtons() {
        stdoutButton.setState(isCapturingOut);
        stderrButton.setState(isCapturingErr);
    }
    
    
    private class CheckboxItemListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            Checkbox b = (Checkbox) e.getItemSelectable();
            if (b.equals(stdoutButton) || b.equals(stderrButton)) {
                int strmsToCapture =
                        (stdoutButton.getState() ? STDOUT : 0) |
                        (stderrButton.getState() ? STDERR : 0);
                setCapture(strmsToCapture);
            }
        }
    }

} 


class TextAreaOutputStream extends OutputStream {

    protected JTextArea ta;
    public int maxLines;
    public int numLines;
    protected char[] buf = new char[8192];
    protected int count;
    private boolean lastWasCR;
    
    public TextAreaOutputStream(JTextArea ta, int maxLines) {

        this.ta = ta;
        this.maxLines = maxLines;
        reset();
    }
    
    public synchronized void write(int b) throws IOException {
        
        boolean addedALine = false;
        buf[count++] = (char) b;
        if (b == 13 || b == 10 && !lastWasCR) {
            numLines++;
            addedALine = true;
        }
        // On Win2K (but not any other flavor, including XP) the TextArea gets confused by the
        // CR/LF combo and treats it like one character, which causes problems. As a fix, we
        // replace a CR/LF pair with the char \n.
        // TODO: If count == 1, should go back into ta.getText() and remove last char.
        if (b == 10 && lastWasCR && count > 1) {
            buf[count - 2] = '\n';
            count--;
        }
        if (count == buf.length || addedALine)
            flush();
        lastWasCR = b == 13;
   }

    public synchronized void flush() throws IOException {

        int excessLines = numLines - maxLines;
        if (excessLines > 0) {
            String text = ta.getText();
            int deleteUpTo = 0;
            for (int i = 0; i < text.length() && excessLines > 0; i++) {
                char c = text.charAt(i);
                if (c == 13) {
                    if (i < text.length() - 1 && text.charAt(i+1) == 10) {
                        excessLines--;
                        i++;
                    }
                } else if (c == 10)
                    excessLines--;
                if (excessLines == 0)
                    deleteUpTo = i + 1;
            }
            if (deleteUpTo != 0)
                ta.replaceRange("", 0, deleteUpTo);
            numLines = maxLines;
        }
        ta.append(new String(buf, 0, count));
        try {
            if (count > 0) {
                char c = buf[count - 1];
                if (c == 10 || c == 13) {
                    Rectangle r = ta.modelToView(ta.getDocument().getLength());
                    ta.scrollRectToVisible(r);
                }
            }
        } catch (Exception e) {}
        count = 0;
    }

    public synchronized void reset() {

        count = 0;
        numLines = 0;
        lastWasCR = false;
        ta.setText("");
    }
}

