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
import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Vector;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import com.wolfram.jlink.ui.SyntaxTokenizer;
import com.wolfram.jlink.ui.BracketMatcher;


// A quasi-Decorator (in the GoF sense) for the MathSessionTextPane it encloses. Does not support the
// entire JTextPane interface however, just the MathTerminal-specific methods.
// The link is not automatically closed anywhere in this class. Clients do that for themselves.


/**
 * MathSessionPane is a visual component that provides a scrolling In/Out session
 * interface to the Mathematica kernel. Much more sophisticated than the kernel's
 * own "terminal" interface, it provides features such as full text editing of input
 * including copy/paste and unlimited undo/redo, support for graphics, control of
 * fonts and styles, customizable syntax coloring, and bracket matching.
 * <p>
 * To use a MathSessionPane, either supply an existing KernelLink using the setLink()
 * method, or provide a command line to create a link via the setLinkArguments() or 
 * setLinkArgumentsArray() methods. You must always arrange for the connect() method
 * to be called, which will launch the kernel (if you supplied an appropriate command 
 * line) and cause the first In[] prompt to appear.
 * <p>
 * The following key commands are supported:
 * <pre>
 * Ctrl-X    Cut
 * Ctrl-C    Copy
 * Ctrl-V    Paste
 * Ctrl-Z    Undo
 * Ctrl-Y    Redo
 * Ctrl-L    Copy Input From Above
 * Ctrl-B    Balance Brackets
 * Alt-.     Abort Computation
 * Alt-,     Interrupt Computation (brings up dialog)
 * (These all use the Command key on the Macintosh)</pre>
 * Here is a simple program that uses MathSessionPane:
 * <PRE>
 * 	public static void main(String[] argv) {
 * 		
 * 		final MathSessionPane msp = new MathSessionPane();
 * 
 * 		// Create the frame window to hold the pane.
 * 		Frame frm = new Frame();
 * 		frm.setSize(500, 500);
 * 		frm.add(msp);
 * 		frm.setVisible(true);
 * 		frm.doLayout();
 * 		
 * 		frm.addWindowListener(new WindowAdapter() {
 * 			public void windowClosing(WindowEvent e) {
 *					KernelLink ml = msp.getLink();
 *					// If we're quitting while the kernel is busy, it might not die when the link
 *					// is closed. So we force the issue by calling terminateKernel();
 *					if (ml != null)
 *						ml.terminateKernel();
 * 				System.exit(0);
 * 			}
 * 		});
 * 		
 * 		// Modify this for your setup.
 * 		msp.setLinkArgumentsArray(new String[] {"-linkmode", "launch", "-linkname", "\"d:/math41/mathkernel\" -mathlink"});
 * 		try {
 * 			msp.connect();
 * 		} catch (MathLinkException e) {
 * 			e.printStackTrace();
 * 		}
 * 	}</PRE>
 * To see a more complete application built around a MathSessionPane, look at the SimpleFrontEnd
 * example application in JLink/Examples/Part2/SimpleFrontEnd.
 *
 * @since 2.0
 */
 
public class MathSessionPane extends JScrollPane {


	private MathSessionTextPane textPane;
	private boolean showTiming = true;
	private PropertyChangeSupport propChangeSupport = new PropertyChangeSupport(this);
	

	////////////////////////////////////  Constructors  //////////////////////////////////////////
	
	/**
	 * Creates a new MathSessionPane with vertical and horizontal scrollbars.
	 */
	public MathSessionPane() {
		this(VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_ALWAYS);
	}
	
	
	/**
	 * Creates a new MathSessionPane with the specified policies for vertical and horizontal scrollbars.
	 * 
	 * @param vsbPolicy vertical scrollbar policy (see the Swing JScrollPane class)
	 * @param hsbPolicy horizontal scrollbar policy (see the Swing JScrollPane class)
	 */
	public MathSessionPane(int vsbPolicy, int hsbPolicy) {

		textPane = new MathSessionTextPane();
		
		// The only PropertyChangeEvent that anyone here fires is for starting and finishing an eval.
		// We register with the textPane so that clients can register with us, and this class appears to be
		// the source of the events. In this way, we let the textPane itself fire the events for clients who
		// want to use it directly, not via this ScrollPane wrapper, but we re-fire them for the majority of
		// users who are using this class. 
		textPane.addComputationPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				propChangeSupport.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
			}
		});

		// We need our own ScrollPaneLayout to accommodate the timing panel in lower left of window.
		ScrollPaneLayout layout = new ScrollPaneLayout() {
			public void layoutContainer(Container parent) {
				super.layoutContainer(parent);
				JScrollBar sb = getHorizontalScrollBar();
				if (sb != null) {
					Rectangle r = sb.getBounds();
					int timingPanelSize = isShowTiming() ? 100 : 0;
					sb.setBounds(r.x + timingPanelSize, r.y, r.width - timingPanelSize, r.height);
					lowerLeft.setBounds(0, r.y, timingPanelSize, r.height);
					// Mac OS X has a lot of repaint problems right now. I don't even know why this works, but
					// it seems to dramatically improve appearance.
					if (Utils.isMacOSX() && isShowTiming())
						RepaintManager.currentManager(MathSessionPane.this).markCompletelyClean(MathSessionPane.this);
				}
				sb.validate(); // Forces relayout and repaint.
			}
		};
		setLayout(layout);
		layout.syncWithScrollPane(this);
		
		setVerticalScrollBar(createVerticalScrollBar());
		setHorizontalScrollBar(createHorizontalScrollBar());
		setVerticalScrollBarPolicy(vsbPolicy);
		setHorizontalScrollBarPolicy(hsbPolicy);
		setViewportView(textPane);
		setDoubleBuffered(true);
		
		final JPanel timingPanel = new JPanel() {
			public void paintComponent(Graphics g) {
				 String s = Double.toString(textPane.getLastTiming()) + " seconds";
				 g.setColor(getHorizontalScrollBar().getBackground());
				 g.fillRect(0, 0, getSize().width, getSize().height);
				 g.setColor(Color.black);
				 g.drawString(s, 6, getHeight() - 2);
			}
		};
		setCorner(JScrollPane.LOWER_LEFT_CORNER, timingPanel);
		// This is the property listener that causes display of computation timings.
		textPane.addComputationPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (!((Boolean)evt.getNewValue()).booleanValue())
					// Computation just ended.
					timingPanel.repaint();
			}
		});
		
		// We need to redo the syntax coloring if the window is resized. For performance reasons we only
		// color the text that is actually visible. Therefore, when new text is made visible we need to re-color.
		getViewport().addChangeListener(new ChangeListener() {
			private Dimension lastVisibleBounds = new Dimension(0, 0);
			public void stateChanged(ChangeEvent e) {
				Dimension visibleBounds = textPane.getVisibleTextBounds();
				if (!visibleBounds.equals(lastVisibleBounds)) {
					textPane.doSyntaxColor();
					lastVisibleBounds = visibleBounds;
				}
			}
		});
	}
	

	//////////////////////////////////////  Property Setters/Getters  /////////////////////////////////////
	
	/**
	 * Sets the link to use for communicating with Mathematica. Use this method if you already have a link you want
	 * to use, or you want to open the link manually. Most programmers will use setLinkArguments() or setLinkArgumentsArray
	 * instead and let this class open the link itself. Even if you open the link yourself and call setLink(), you must still
	 * call the connect() method of this class to prepare the pane to be used.
	 * 
	 * @param ml
	 * @see #setLinkArguments(String)
	 * @see #setLinkArgumentsArray(String[])
	 * @see #connect()
	 */
	public void setLink(KernelLink ml) {
		textPane.setLink(ml);
	}
	
	/**
	 * Returns the link currently being used to communicate with Mathematica. Null if no link is open yet.
	 */
	public KernelLink getLink() {
		return textPane.getLink();
	}
	
	
	// Why are there two forms for the setLinkArguments method and not just one sig that takes Object? Because
	// I want to present an API that is bean-friendly. Bean property editors will display the String form
	// in an editable way. In other words, if there was only one property "linkArguments" that took an Object, it would not
	// be usable in bean property editors.
	
	/**
	 * Sets the command line to use to open a link to Mathematica. The string is in the same
	 * form as you would pass to MathLinkFactory.createKernelLink(), for example:
	 * <pre>
	 * 	"-linkmode launch -linkname \"c:/program files/wolfram research/mathematica/5.1/mathkernel.exe\""       (Windows)
	 * 	"-linkmode launch -linkname 'math -mathlink'"           (UNIX)
	 * 	"-linkmode launch -linkname '\"/Applications/Mathematica 4.1.app/Contents/MacOS/MathKernel\" -mathlink'"  (Mac OS X)</pre>
	 * The actual creation of the link will happen when you call connect().
	 * 
	 * @param linkArgs the command line to create the link
	 * @see #setLinkArgumentsArray(String[])
	 * @see #setLink(KernelLink)
	 * @see #connect()
	 * @see #closeLink()
	 */
	public void setLinkArguments(String linkArgs) {
		textPane.setLinkArguments(linkArgs);
	}
	
	/**
	 * Sets the arguments to use to open a link to Mathematica. The string array is in the same
	 * form as you would pass to MathLinkFactory.createKernelLink(), for example:
	 * <pre>
	 * 	String[] args = {"-linkmode", "launch", "-linkname", "c:\\program files\\wolfram research\\mathematica\\5.1\\mathkernel.exe"};   (Windows)
	 * 	String[] args = {"-linkmode", "launch", "-linkname", "math -mathlink"};         (UNIX)
	 * 	String[] args = {"-linkmode", "launch", "-linkname", \"/Applications/Mathematica 5.1.app/Contents/MacOS/MathKernel\" -mathlink"}; (Mac OS X)</pre>
	 * The actual creation of the link will happen when you call connect().
	 * 
	 * @param linkArgs the command line to create the link
	 * @see #setLinkArguments(String)
	 * @see #setLink(KernelLink)
	 * @see #connect()
	 * @see #closeLink()
	 */
	public void setLinkArgumentsArray(String[] linkArgs) {
		textPane.setLinkArgumentsArray(linkArgs);
	}
	
	/**
	 * Returns the string specified to create the link to Mathematica.
	 * 
	 * @see #setLinkArguments(String)
	 */
	public String getLinkArguments() {
		return textPane.getLinkArguments();
	}
	
	/**
	 * Returns the string array specified to create the link to Mathematica.
	 * 
	 * @see #setLinkArgumentsArray(String[])
	 */
	public String[] getLinkArgumentsArray() {
		return textPane.getLinkArgumentsArray();
	}
	
	/**
	 * Sets the number of milliseconds that the connect() method will wait to connect the link to Mathematica.
	 * If the timeout expires, connect() will throw a MathLinkException. You can use this timeout to prevent
	 * your program from hanging indefinitely in certain circumstances if the kernel does not launch properly.
	 * The default value is very large, effectively no timeout.
	 *
	 * @see #getConnectTimeout()
	 */
	public void setConnectTimeout(int timeoutMillis) {
		textPane.setConnectTimeout(timeoutMillis);
	}
	
	/**
	 * Returns the number of milliseconds that the connect() method will wait to connect the link to Mathematica.
	 * 
	 * @see #setConnectTimeout(int)
	 */
	public int getConnectTimeout() {
		return textPane.getConnectTimeout();
	}
	

	/**
	 * Gives the current contents of the pane as a string.
	 */
	public String getText() {
		return textPane.getText();
	}
	
	/**
	 * Gives the JTextPane component that is hosted within this MathSessionPane. The JTextPane
	 * is the actual component that handles all text input and output. Advanced
	 * programmers might want access to the JTextPane to call methods directly on it.
	 */
	public JTextPane getTextPane() {
		return textPane;
	}
	
	/**
	 * Indicates whether a computation is currently in progress. Programmers might want to know
	 * this for several reasons. First, they might want to disable/enable parts of their user interface,
	 * and second, they might want to know whether the Mathematica kernel is currently busy. This
	 * latter consideration is particularly important if other parts of the program want to use
	 * the same link. Programmers who are considering using this method might find it preferable instead
	 * to use a PropertyChangeListener to monitor the changes in the computationActive property.
	 * 
	 * @return true, if a computation is active, false otherwise
	 */
	public boolean isComputationActive() {
		return textPane.isComputationActive();
	}
	
	/**
	 * Gives the font size currently in use.
	 */
	public int getTextSize() {
		return textPane.getTextSize();
	}
	
	/**
	 * Sets the font size for input and output text.
	 * 
	 * @param size the font size, in points
	 */
	public void setTextSize(int size) {
		textPane.setTextSize(size);
	}
	
	/**
	 * Indicates whether syntax coloring is currently enabled.
	 * 
	 * @return true, if syntax coloring is on, false otherwise
	 */
	public boolean isSyntaxColoring() {
		return textPane.isSyntaxColoring();
	}
	
	/**
	 * Enables or disables syntax coloring of input. The default is true.
	 * 
	 * @param b
	 */
	public void setSyntaxColoring(boolean b) {
		textPane.setSyntaxColoring(b);
	}
			
	/**
	 * Gives the color for input and output text
	 */
	public Color getTextColor() {
		return textPane.getTextColor();
	}
	
	/**
	 * Sets the color for input and output text. This color is used for input except when
	 * syntax coloring is on and the characters are in a syntax class that is specially colored.
	 * The default is black.
	 * 
	 * @param c
	 */
	public void setTextColor(Color c) {
		textPane.setTextColor(c);
	}
	
	/**
	 * Returns the color that will be used for literal strings in input if syntax coloring is on.
	 */
	public Color getStringColor() {
		return textPane.getStringColor();
	}
	
	/**
	 * Sets the color that will be used for literal strings in input if syntax coloring is on.
	 * The default is a shade of blue.
	 * 
	 * @param c
	 */
	public void setStringColor(Color c) {
		textPane.setStringColor(c);
	}
	
	/**
	 * Returns the color that will be used for comments in input if syntax coloring is on.
	 */
	public Color getCommentColor() {
		return textPane.getCommentColor();
	}
	
	/**
	 * Sets the color that will be used for comments in input if syntax coloring is on.
	 * The default is a shade of green.
	 * 
	 * @param c
	 */
	public void setCommentColor(Color c) {
		textPane.setCommentColor(c);
	}
	
	/**
	 * Returns the color that will be used in input for symbols in the Mathematica System` context
	 * if syntax coloring is on.
	 */
	public Color getSystemSymbolColor() {
		return textPane.getSystemSymbolColor();
	}
	
	/**
	 * Sets the color that will be used in input for symbols in the Mathematica System` context
	 * if syntax coloring is on. The default is a shade of purple.
	 * 
	 * @param c
	 */
	public void setSystemSymbolColor(Color c) {
		textPane.setSystemSymbolColor(c);
	}
	
	/**
	 * Returns the background color of the text region.
	 */
	public Color getBackgroundColor() {
		return textPane.getBackgroundColor();
	}
	
	/**
	 * Sets the background color of the text region.
	 * 
	 * @param c
	 */
	public void setBackgroundColor(Color c) {
		textPane.setBackgroundColor(c);
	}
	
	/**
	 * Returns the color used for In[] and Out[] prompts.
	 */
	public Color getPromptColor() {
		return textPane.getPromptColor();
	}
	
	/**
	 * Sets the color used for In[] and Out[] prompts. The default is blue.
	 * 
	 * @param c
	 */
	public void setPromptColor(Color c) {
		textPane.setPromptColor(c);
	}
	
	/**
	 * Returns the color used for Mathematica message output.
	 */
	public Color getMessageColor() {
		return textPane.getMessageColor();
	}
	
	/**
	 * Sets the color used for Mathematica message output. The default is red.
	 * 
	 * @param c
	 */
	public void setMessageColor(Color c) {
		textPane.setMessageColor(c);
	}
	
	/**
	 * Returns the amount that input and output will be indented from the left edge.
	 * 
	 * @return the indent amount, in pixels
	 */
	public int getLeftIndent() {
		return textPane.getLeftIndent();
	}
		
	/**
	 * Sets the amount that input and output will be indented from the left edge. The default is 20.
	 * 
	 * @param indent the indentation amount in pixels
	 */
	public void setLeftIndent(int indent) {
		textPane.setLeftIndent(indent);
	}

	/**
	 * Indicates whether input is in a boldface font.
	 */
	public boolean isInputBoldface() {
		return textPane.isInputBoldface();
	}
	
	/**
	 * Sets whether to use boldface for Mathematica input. The default is true.
	 * 
	 * @param bold
	 */
	public void setInputBoldface(boolean bold) {
		textPane.setInputBoldface(bold);
	}
	
	/**
	 * Sets whether to scale graphics so as to fit into the current visible width. The default is false.
	 * 
	 * @param fit
	 */
	public void setFitGraphics(boolean fit) {
		textPane.setFitGraphics(fit);
	}
	
	/**
	 * Indicates whether graphics are set to be scaled to fit into the current visible width.
	 */
	public boolean isFitGraphics() {
		return textPane.isFitGraphics();
	}
	
	/**
	 * Sets whether to use the Mathematica front end in the background to assist in rendering graphics.
	 * This must be set to true if you want typeset output (such as plot labels) in your graphics. It
	 * will also improve the quality of most images, particularly surface graphics. Setting this to true
	 * will cause the front end to launch. The default is false.
	 * 
	 * @param b
	 */
	public void setFrontEndGraphics(boolean b) {
		textPane.setFrontEndGraphics(b);
	}
	
	/**
	 * Indicates whether the Mathematica front end will be used to assist in rendering graphics.
	 */
	public boolean isFrontEndGraphics() {
		return textPane.isFrontEndGraphics();
	}
	
	/**
	 * Lets you customize syntax coloring by specifying an array of symbol names and the color
	 * you want them drawn in when they appear in input. As an example, you could use this
	 * to specify that all the symbols from a certain context should be rendered in red. You can
	 * call this method as many times as you like, specifying many different combinations of
	 * symbols and colors.
	 * 
	 * @param syms The symbols to be specially colored, for example {"If", "For", "Do"}
	 * @param c
	 * @see #clearColoredSymbols()
	 */
	public void addColoredSymbols(String[] syms, Color c) {
		textPane.addColoredSymbols(syms, c);
	}
	
	/**
	 * Wipes out all syntax coloring customizations specified by addColoredSymbols().
	 * 
	 * @see #addColoredSymbols(String[],Color)
	 */
	public void clearColoredSymbols() {
		textPane.clearColoredSymbols();
	}

	/**
	 * Specifies whether to display the wall-clock timing for each input in a timing panel
	 * at the lower left corner of the pane. The default is true.
	 * 
	 * @param show
	 */
	public void setShowTiming(boolean show) {
		// The showTiming property belongs to this ScrollPane, not the TextPane.
		showTiming = show;
		doLayout();
	}
	
	/**
	 * Indicates whether evaluation timings will be shown in a timing panel at the lower left
	 * corner of the pane.
	 */
	public boolean isShowTiming() {
		return showTiming;
	}
	
	
	/**
	 * Evaluates the current input. This operation is automatically mapped to the Shift-Enter key combination. 
	 */
	public void evaluateInput() {
		textPane.evaluateInput();
	}

	/**
	 * Undoes the last edit. This operation is automatically mapped to the
	 * Ctrl-Z key combination (Cmd-Z on Macintosh). 
	 */
	public void undo() {
		textPane.undo();
	}

	/**
	 * Redoes the last undone edit. This operation is automatically mapped to the
	 * Ctrl-Y key combination (Cmd-Y on Macintosh).
	 */
	public void redo() {
		textPane.redo();
	}

	/**
	 * Indicates whether a redo operation is currently possible.
	 */
	public boolean canRedo() {
		return textPane.canRedo();
	}

	/**
	 * Indicates whether an undo operation is currently possible. 
	 */
	public boolean canUndo() {
		return textPane.canUndo();
	}

	/**
	 * Copies the previous input to the current cursor location. This operation is automatically mapped to the
	 * Ctrl-L key combination (Cmd-L on Macintosh). 
	 */
	public void copyInputFromAbove() {
		textPane.copyInputFromAbove();
	}

	/**
	 * Grows the current selection to highlight the nearest matching set of delimiters. This is done in a way that
	 * respects Mathematica syntax, meaning that delimiters within comments and strings are ignored. This operation
	 * is automatically mapped to the Ctrl-B key combination (Cmd-B on Macintosh). 
	 */
	public void balanceBrackets() {
		textPane.balanceBrackets();
	}


	/**
	 * Adds a PropertyChangeListener that will be notified of changes in the computationActive property.
	 * You can use this to receive notifications when computations start and end. For eample, you might want
	 * to enable or disable parts of your user interface, or lock out other parts of your program from
	 * trying to use the link.
	 * 
	 * @param listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		super.addPropertyChangeListener(listener);
		if (textPane != null)
			textPane.addComputationPropertyChangeListener(listener);
	}
	
	/**
	 * Removes a PropertyChangeListener.
	 * 
	 * @param listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		super.removePropertyChangeListener(listener);
		if (textPane != null)
			textPane.removeComputationPropertyChangeListener(listener);
	}
	
	/**
	 * Connects the link to Mathematica and prepares the pane for use. The first In[] prompt will not
	 * appear and the pane will not be editable until this method has been called, even if you have
	 * supplied a preexisting link using the setLink() method. If you used setLinkArguments() or
	 * setLinkArgumentsArray() to specify link parameters, then the link will be created and connected 
	 * at this time. The Mathematica kernel will be launched if appropriate.
	 * <p>
	 * Do not call this until after the pane has been made visible.
	 * 
	 * @exception com.wolfram.jlink.MathLinkException
	 */
	public void connect() throws MathLinkException {
		textPane.connect();
	}
	
	/**
	 * Closes the link to Mathematica. The link is not automatically closed anywhere in this class,
	 * so you must arrange for this method to be called.
	 */
	public void closeLink() {
		textPane.closeLink();
	}

}  // End MathSessionPane class


/////////////////////////////////////  MathSessionTextPane  //////////////////////////////////////

	
class MathSessionTextPane extends JTextPane {
	
	private KernelLink ml;
	private String linkArgs;
	private String[] linkArgsArray;
	private int connectTimeout = 100000000;
	
	protected UndoManager undoManager = new UndoManager();

	private PropertyChangeSupport computationPropChangeSupport = new PropertyChangeSupport(this);
	
	private int leftIndent = 20;
	private int fontSize = 12;
	private Color textColor = Color.black;
	private Color promptColor = Color.blue;
	private Color messageColor = Color.red;
	private Color backgroundColor = Color.white;
	private boolean isInputBold = true;
	private boolean fitGraphics = false;
	private boolean feGraphics = false;
	private double lastTiming = 0.;
	
	private Style base, input, output, prompt, print, message, graphics;
	private double charWidth;
	private int lineHeight = 10;  // Will be modified in connect().
			
	// These are all for syntax coloring:
	private boolean useSyntaxColoring = true;
	private boolean colorsHaveChanged = false;
	private Color stringColor = new Color(20, 159, 175);
	private Color commentColor = new Color(94, 206, 11);
	private Color systemColor = new Color(132, 38, 187);
	private MutableAttributeSet attrNormal = new SimpleAttributeSet();
	private MutableAttributeSet attrString = new SimpleAttributeSet();
	private MutableAttributeSet attrSystem = new SimpleAttributeSet();
	private MutableAttributeSet attrComment = new SimpleAttributeSet();
	private HashMap systemSymbols = new HashMap();
	private Vector userSymbols;
	private Vector userColors;
		
	private boolean isInputMode; // Whether we are in state of user editing input
	
	private BracketMatcher bracketMatcher = new BracketMatcher();
		
	private boolean wrap = false;
			
	
	MathSessionTextPane() {
		
		setStyledDocument(new MTDocument());
		setEditable(false); // Until connect() is done.
		setDoubleBuffered(true);
		setOpaque(true);
		
		Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
		base = addStyle("base", def);
			
		input = addStyle("input", base);
		output = addStyle("output", base);
		prompt = addStyle("prompt", base);
		message = addStyle("message", base);
		print = addStyle("print", base);
		graphics = addStyle("graphics", base);

		setBackgroundColor(backgroundColor);
		setTextSize(fontSize);
		setTextColor(textColor);
		setPromptColor(promptColor);
		setMessageColor(messageColor);
		setLeftIndent(leftIndent);
		setInputBoldface(isInputBold);
			
		setStringColor(stringColor);
		setSystemSymbolColor(systemColor);
		setCommentColor(commentColor);

		// This property is immutable, so no public setter:
		StyleConstants.setFontFamily(base, "Monospaced");
		
		int cmdKey = Utils.isMacOSX() ? Event.META_MASK : Event.CTRL_MASK;
		int abortKey = Utils.isMacOSX() ? Event.META_MASK : Event.ALT_MASK;
		
		// Add Action for shift-return eval.
		Keymap keymap = addKeymap(null, getKeymap());
		 Action evalAction = new AbstractAction() {		
			public void actionPerformed(ActionEvent e) {
				evaluateInput();
			}
		};
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Event.SHIFT_MASK), evalAction);
		// Add Action for ctrl-Z undo.
		Action undoAction = new AbstractAction() {		
			public void actionPerformed(ActionEvent e) {
				undo();
			}
		};
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_Z, cmdKey), undoAction);
		// Add Action for ctrl-Y redo.
		Action redoAction = new AbstractAction() {		
			public void actionPerformed(ActionEvent e) {
				redo();
			}
		};
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_Y, cmdKey), redoAction);
		// Add Action for ctrl-B bracket matching.
		Action bracketAction = new AbstractAction() {		
			public void actionPerformed(ActionEvent e) {
				balanceBrackets();
			}
		};
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_B, cmdKey), bracketAction);
		// Add Action for ctrl-L Copy Input From Above.
		Action cifaAction = new AbstractAction() {		
			public void actionPerformed(ActionEvent e) {
				copyInputFromAbove();
			}
		};
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_L, cmdKey), cifaAction);
		// Add Action for alt-. abort.
		Action abortAction = new AbstractAction() {		
			public void actionPerformed(ActionEvent e) {
				abortEval();
			}
		};
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_PERIOD, abortKey), abortAction);
		// Add Action for alt-, interrupt.
		Action interruptAction = new AbstractAction() {		
			public void actionPerformed(ActionEvent e) {
				interruptEval();
			}
		};
		keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, abortKey), interruptAction);
		setKeymap(keymap);
		
		// Add undo support.
		getDocument().addUndoableEditListener(new UndoableEditListener() {
			public void undoableEditHappened(UndoableEditEvent e) {
				if (isInputMode() && getDoc().isRecordUndos()) {
					undoManager.addEdit(e.getEdit());
			   }
			}
		});

	}
		

	///////////////////////////////////  Property Setters/Getters  //////////////////////////////////

	void setLink(KernelLink ml) {
		this.ml = ml;
	}
		
	KernelLink getLink() {
		return ml;
	}
		
	void setLinkArguments(String linkArgs) {
		this.linkArgs = linkArgs;
	}
		
	String getLinkArguments() {
		return linkArgs;
	}
	
	void setLinkArgumentsArray(String[] linkArgs) {
		this.linkArgsArray = linkArgs;
	}
		
	String[] getLinkArgumentsArray() {
		return linkArgsArray;
	}
	
	void setConnectTimeout(int timeoutMillis) {
		connectTimeout = timeoutMillis;
	}
	
	int getConnectTimeout() {
		return connectTimeout;
	}
	
	public boolean isComputationActive() {
		return ml != null && !isInputMode();
	}
	
	int getTextSize() {
		return fontSize;
	}
		
	void setTextSize(int size) {
		fontSize = size;
		StyleConstants.setFontSize(base, size);
	}
	
	Color getTextColor() {
		return textColor;
	}

	void setTextColor(Color c) {
		textColor = c;
		StyleConstants.setForeground(base, c);
		// For syntax coloring:
		StyleConstants.setForeground(attrNormal, textColor);
		colorsHaveChanged = true;
		doSyntaxColor();
		repaint();
	}
		
	Color getStringColor() {
		return stringColor;
	}
		
	void setStringColor(Color c) {
		stringColor = c;
		StyleConstants.setForeground(attrString, stringColor);
		colorsHaveChanged = true;
		doSyntaxColor();
		repaint();
	}
		
	Color getCommentColor() {
		return commentColor;
	}
		
	void setCommentColor(Color c) {
		commentColor = c;
		StyleConstants.setForeground(attrComment, commentColor);
		colorsHaveChanged = true;
		doSyntaxColor();
		repaint();
	}
		
	Color getSystemSymbolColor() {
		return systemColor;
	}
		
	void setSystemSymbolColor(Color c) {
		systemColor = c;
		StyleConstants.setForeground(attrSystem, systemColor);
		colorsHaveChanged = true;
		doSyntaxColor();
		repaint();
	}
		
	Color getBackgroundColor() {
		return backgroundColor;
	}
		
	void setBackgroundColor(Color c) {
		backgroundColor = c;
		setBackground(c);
		// XOR cursor.
		setCaretColor(new Color(backgroundColor.getRed() ^ 255, backgroundColor.getGreen() ^ 255, backgroundColor.getBlue() ^ 255));
	}
		
	Color getPromptColor() {
		return promptColor;
	}
		
	void setPromptColor(Color c) {
		promptColor = c;
		StyleConstants.setForeground(prompt, c);
	}
		
	Color getMessageColor() {
		return messageColor;
	}
		
	void setMessageColor(Color c) {
		messageColor = c;
		StyleConstants.setForeground(message, c);
	}
	
	int getLeftIndent() {
		return leftIndent;
	}
		
	void setLeftIndent(int indent) {
		leftIndent = indent;
		StyleConstants.setLeftIndent(input, indent);
		StyleConstants.setLeftIndent(output, indent);
		StyleConstants.setLeftIndent(message, indent);
		StyleConstants.setLeftIndent(print, indent);
		StyleConstants.setLeftIndent(graphics, indent);
	}
	
	boolean isInputBoldface() {
		return isInputBold;
	}
		
	void setInputBoldface(boolean bold) {
		isInputBold = bold;
		StyleConstants.setBold(input, bold);
	}
		
	void setFitGraphics(boolean fit) {
		fitGraphics = fit;
	}
	
	boolean isFitGraphics() {
		return fitGraphics;
	}

	void setFrontEndGraphics(boolean b) {
		feGraphics = b;
	}
	
	boolean isFrontEndGraphics() {
		return feGraphics;
	}
		
	double getLastTiming() {
		return lastTiming;
	}
	
	boolean isSyntaxColoring() {
		return useSyntaxColoring;
	}
		
	void setSyntaxColoring(boolean b) {
		useSyntaxColoring = b;
		colorsHaveChanged = true;
		doSyntaxColor();
		repaint();
	}
				
	private void setSystemSymbols(String[] syms) {
			
		systemSymbols.clear();
		for (int i = 0; i < syms.length; i++)
			systemSymbols.put(syms[i], syms[i]);
	}

	void addColoredSymbols(String[] syms, Color c) {
			
		if (userSymbols == null)
			userSymbols = new Vector(1);
		if (userColors == null)
			userColors = new Vector(1);
		HashMap h = new HashMap();
		for (int i = 0; i < syms.length; i++)
			h.put(syms[i], syms[i]);
		userSymbols.addElement(h);
		MutableAttributeSet attr = new SimpleAttributeSet();
		StyleConstants.setForeground(attr, c);
		userColors.addElement(attr);
		colorsHaveChanged = true;
		doSyntaxColor();
		repaint();
	}

	void clearColoredSymbols() {
		userSymbols = null;
		userColors = null;
		colorsHaveChanged = true;
		doSyntaxColor();
		repaint();
	}

	void doSyntaxColor() {
		getDoc().doSyntaxColor();
	}

	void undo() {
		if (undoManager.canUndo())
			undoManager.undo();
	}
	
	void redo() {
		if (undoManager.canRedo())
			undoManager.redo();
	}
		
	boolean canRedo() {
		return undoManager.canRedo();
	}

	boolean canUndo() {
		return undoManager.canUndo();
	}

	
	private void setInputMode(boolean isInputMode) {

		boolean oldValue = this.isInputMode;
		this.isInputMode = isInputMode;
		if (oldValue != isInputMode)
			computationPropChangeSupport.firePropertyChange("computationActive", new Boolean(!oldValue), new Boolean(!isInputMode));
	}
				
	private boolean isInputMode() {
		return isInputMode;
	}
	
	public void addComputationPropertyChangeListener(PropertyChangeListener listener) {
		computationPropChangeSupport.addPropertyChangeListener(listener);
	}
	
	public void removeComputationPropertyChangeListener(PropertyChangeListener listener) {
		computationPropChangeSupport.removePropertyChangeListener(listener);
	}
	
	
	void connect() throws MathLinkException {
		
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		setInputMode(false);
		
		try {
			if (ml == null) {
				if (linkArgs != null)
					ml = MathLinkFactory.createKernelLink(linkArgs);
				else if (linkArgsArray != null)
					ml = MathLinkFactory.createKernelLink(linkArgsArray);
			}
			ml.connect(connectTimeout);
		} catch (MathLinkException e) {
			// Rethrow the excpetion so the caller knows there was a problem, but restore the cursor first.
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			throw e;
		}
		
		boolean isStdLink = ml.equals(StdLink.getLink());
		
		if (isStdLink)
			StdLink.requestTransaction();
		ml.evaluate("$Line");
		// This loop handles the cases where an InputNamePacket arrives at the start or not.
		while (ml.waitForAnswer() != MathLink.RETURNPKT)
			ml.newPacket();
		final String firstPrompt = "In[" + Integer.toString(ml.getInteger()) + "]:=\n";
		ml.newPacket();
		
		// For EvaluateToImage:
		if (isStdLink)
			StdLink.requestTransaction();
		ml.evaluate("Needs[\"" + KernelLink.PACKAGE_CONTEXT + "\"]");
		ml.discardAnswer();
		
		if (isStdLink)
			StdLink.requestTransaction();
		ml.evaluate("Names[\"System`*\"]");
		ml.waitForAnswer();
		String[] syms = ml.getStringArray1();
		setSystemSymbols(syms);
		
		// This PacketListener does the work of writing the output to the screen.
		ml.addPacketListener(new PktHandler(this, getDoc()));
		// Install the interrupt dialog (you get this from Alt-comma, like in the front end).
		Frame parentFrame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
		Dialog parentDialog = (Dialog) SwingUtilities.getAncestorOfClass(Dialog.class, this);
		if (parentFrame != null)
			ml.addPacketListener(new InterruptDialog(parentFrame));
		else if (parentDialog != null)
			ml.addPacketListener(new InterruptDialog(parentDialog));
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Insert first prompt.
				MTDocument doc = getDoc();
				doc.setLogicalStyle(doc.getLength(), getStyle("prompt"));
				try {
					doc.insertString(doc.getLength(), firstPrompt, null);
				} catch (BadLocationException e) {}
				// Determine and cache the width of a char and height of a line in the font being used. This is a weird way
				// to do this, but I cannot find another way to accurately determine char widths and heights. AWT-based methods
				// like Font.getMaxCharBounds() don't give sizes that match the fonts used in the JTextPane. There must be a
				// better way to do this, though.
				try {
					// We know we have at least 7 chars (the In[1]:= propmt)
					charWidth = (modelToView(7).getX() - modelToView(0).getX())/7;
					lineHeight = (int) (modelToView(getDoc().getLength()).getY() - modelToView(0).getY());
				} catch (BadLocationException e) {}
				// Prepare pane for use.
				doc.setFirstEditPos(doc.getLength());
				doc.setLogicalStyle(doc.getLength(), getStyle("input"));
				setInputMode(true);
				setEditable(true);
				setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
				getCaret().setVisible(true);
			}
		});
	}


	void closeLink() {
		if (ml != null) {
			ml.close();
			ml = null;
		}
	}


	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)	{
		return lineHeight;
	}
		
	// Next two are overrides that give this pane the ability to turn off word wrap.
	// Got this from some code by Jacob Smullyan for the SkunkDAV project (sourceforge).
		
	public boolean getScrollableTracksViewportWidth() {
			
		if (!wrap) {
	   	Component parent = getParent();
	   	ComponentUI ui = getUI();
	   	int uiWidth = ui.getPreferredSize(this).width;
	   	int parentWidth = parent.getSize().width;
	   	return (parent != null) ? (ui.getPreferredSize(this).width < parent.getSize().width) : true;	
		}
		else
			return super.getScrollableTracksViewportWidth();
	}

	public void setBounds(int x, int y, int width, int height) {
			
		if (wrap) 
	   	super.setBounds(x, y, width, height);
		else {
	   	Dimension size = getPreferredSize();
	   	super.setBounds(x, y, Math.max(size.width, width), Math.max(size.height, height));
		}
	}


	Dimension getVisibleTextBounds() {

		JViewport v = (JViewport) getParent();
		Point startVisible = v.getViewPosition();
		Point endVisible = new Point(startVisible.x + v.getSize().width, startVisible.y + v.getSize().height);
		return new Dimension(viewToModel(startVisible), viewToModel(endVisible));
	}
	
	
	private MTDocument getDoc() {
		return (MTDocument) getDocument();
	}
	
	
	///////////////////////////////  Evaluation Methods  ////////////////////////////////////
	
	void evaluateInput() {
		
		// Even though pane is not editable when not in input mode, we are doing an end-run around
		// the non-editability here, so we need a direct test.
		if (!isInputMode() || ml == null)
			return;
		int firstEditPos = getDoc().getFirstEditPos();
		if (getSelectionStart() >= firstEditPos && getSelectionEnd() >= firstEditPos) {
			// Need to put in the carriage return at end of input, plus another one for spacing.
			try { getDoc().insertString(getDoc().getLength(), "\n\n", null); } catch (BadLocationException exc) {}
			undoManager.discardAllEdits();
			// Would like to call setEditable(false) here, but that causes a beep on Mac OSX.
			// Therefore, we do it in EvalTask.run().
			Runnable evalTask = new EvalTask();
			new Thread(evalTask).start();
		} else {
			getToolkit().beep();
		}
	}
		
	
	private void abortEval() {

		if (!isInputMode()) {
            try {
				ml.putMessage(MathLink.MLABORTMESSAGE);
			} catch (MathLinkException e) {
				e.printStackTrace();
				// Don't call clearError(), as that is synchronized, and the link is locked by another thread.
			}
		}
	}
	
	
	private void interruptEval() {

		if (!isInputMode()) {
			try {
				ml.putMessage(MathLink.MLINTERRUPTMESSAGE);
			} catch (MathLinkException e) {
				e.printStackTrace();
				// Don't call clearError(), as that is synchronized, and the link is locked by another thread.
			}
		}
	}
	
	
	////////////////////////////////////////  UI  /////////////////////////////////////////////
	
	void balanceBrackets() {

		bracketMatcher.setText(getDoc().getEvalInput());
		int inputStart = getDoc().getFirstEditPos();
		Point result = null;
		if (getSelectionStart() >= inputStart)
			result = bracketMatcher.balance(getSelectionStart() - inputStart, getSelectionEnd() - getSelectionStart());
		if (result != null) {
			setSelectionStart(inputStart + result.x);
			setSelectionEnd(inputStart + result.y);
		} else {
			getToolkit().beep();
		}
	}
	

	void copyInputFromAbove() {
		
		if (isInputMode()) {
			MTDocument doc = getDoc();
			int firstEditPos = doc.getFirstEditPos();
			if (getSelectionStart() >= firstEditPos && getSelectionEnd() >= firstEditPos) {
				int inputStyleStart = 0, inputStyleEnd = 0;
				Style inputStyle = getStyle("input");
				for (int pos = firstEditPos - 1; pos >= 0; pos--) {
					if (doc.getLogicalStyle(pos).equals(inputStyle)) {
						if (inputStyleEnd == 0)
							inputStyleEnd = pos + 1;
					} else if (inputStyleEnd != 0) {
						inputStyleStart = pos + 1;
						break;
					}
				}
				if (inputStyleStart != 0 && inputStyleEnd != 0) {
					try {
						String s = getText(inputStyleStart, inputStyleEnd - inputStyleStart);
						// Should always end with two carriage returns inserted during evaluateInput(). Remove them.
						if (s.endsWith("\n\n"))
							s = s.substring(0, s.length() - 2);
						replaceSelection(s);
					} catch (BadLocationException exc) {}
				}
			} else {
				getToolkit().beep();
			}
		}
		
	}
	
	
	///////////////////////////////////  EvalTask  ////////////////////////////////////
	
	// EvalTask is the class whose Run method performs the computation and printing of output.
    // Each computation is handled by a new instance of this class.
	
	class EvalTask implements Runnable {
		
		public void run() {
			
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        setEditable(false);
                        setInputMode(false);
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    }
                });
            } catch (Exception e) {}
			
			long start = System.currentTimeMillis();
			
			try {
				// Have to do this differently if the link to Mathematica is also the StdLink. This will be the case when
				// this pane is being used in a program that is being scripted from Mathematica. This is a relatively unusual case.
				// We must call requestTransaction before each part of the 3-part computation sequence. This means we
				// cannot wrap the entire sequence in a synchronized(ml) block. The cost of that is that weird things could happen
				// if the user tried to use Mathematica for computations while a computation from this pane was in progress,
				// because the settings made in preEval() could still be in effect . It is not very likely that a user would try to do this.
				if (ml.equals(StdLink.getLink())) {
					StdLink.requestTransaction();
					synchronized (ml) {
						preEval();
					}
					StdLink.requestTransaction();
					synchronized (ml) {
						ml.putFunction("EnterTextPacket", 1);
						ml.put(getDoc().getEvalInput());
						ml.discardAnswer();
					}
					StdLink.requestTransaction();
					synchronized (ml) {
						postEval();
					}
				} else {
					// The more typical case. This pane is being used in a standalone program.
					synchronized (ml) {
						preEval();
						ml.putFunction("EnterTextPacket", 1);
						ml.put(getDoc().getEvalInput());
						ml.discardAnswer();
						postEval();
					}
				}
            } catch (MathLinkException e) {
                if (!ml.clearError() || e.getErrCode() == 11)
                    // error 11 is "other side closed the link"
                    closeLink();
                else
                    ml.newPacket();
			}
      	
			lastTiming = (System.currentTimeMillis() - start)/1000.;
      	
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        setCaretPosition(getDoc().getLength());
                        getCaret().setVisible(true);
                        undoManager.discardAllEdits();
                        if (getLink() != null) {
                            setInputMode(true);
                            setEditable(true);
                            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                        } else {
                            // Will get here if an unrecoverable link error caused closeLink() to be called above.
                            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        }
                    }
                });
            } catch (Exception e) {}
		}

		// The idea of preEval and postEval is to allow this in/out window to coexist with other
		// interfaces to the same kernel. Thus, we cannot make any persistent changes to the state
		// of the kernel (other than those initiated by the user, of course). We need to set up some
		// properties and then clear them out after each eval.
			
		private void preEval() throws MathLinkException {

			int paneWidthInChars = (int) ((getParent().getSize().width - getLeftIndent())/charWidth);
			String df = "(LinkWrite[$ParentLink, DisplayPacket[EvaluateToImage[#, " + (feGraphics ? "True" : "False") +
								(fitGraphics ? ", ImageSize->{" + (getParent().getSize().width - getLeftIndent() - 10)+ ", Automatic}" : "") + "]]]; #)&";
			ml.evaluate(// Use of the JLink`Private` context for these temp variables is arbitrary.
			         	"{JLink`Private`cfv, JLink`Private`sopts, JLink`Private`ddf} = {FormatValues[Continuation], Options[\"stdout\"], $DisplayFunction} ; " + 
			         	"Format[Continuation[_], OutputForm] = \"\" ; " +
			         	"SetOptions[\"stdout\", FormatType -> OutputForm, CharacterEncoding -> \"Unicode\", PageWidth -> " + paneWidthInChars + "] ; " + 
			         	"$DisplayFunction = " + df + ";");			
			ml.discardAnswer();
		}
			
		private void postEval() throws MathLinkException {
			
			ml.evaluate("FormatValues[Continuation] = JLink`Private`cfv ; " +
			         	"SetOptions[\"stdout\", JLink`Private`sopts] ; " + 
			         	"$DisplayFunction = JLink`Private`ddf ;");
			ml.discardAnswer();
		}
	
	}
	
	
	///////////////////////////////////  MTDocument  //////////////////////////////////
		
	// Need a custom document class to prevent edits when the selection touches the non-editable
	// portion of the document (anything up to, and including, the last In[] prompt).
			
	class MTDocument extends DefaultStyledDocument {
				
		private int firstEditPos;  // First position at which editing is legal (right after most recent In[] prompt).
		private SyntaxTokenizer tokenizer = new SyntaxTokenizer();
		private int lastStyledStart = Integer.MAX_VALUE, lastStyledEnd = Integer.MIN_VALUE;
		protected boolean recordUndos = true;
		
	
		public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
			
			if (offset < firstEditPos) {
				getToolkit().beep();
			} else {
				super.insertString(offset, str, a);
				lastStyledStart = Integer.MAX_VALUE;
				lastStyledEnd = Integer.MIN_VALUE;				
				doSyntaxColor();
			}
		}
				
		public void remove(int offset, int len) throws BadLocationException {
			
			if (offset < firstEditPos) {
				getToolkit().beep();
			} else {
				super.remove(offset, len);
				lastStyledStart = Integer.MAX_VALUE;
				lastStyledEnd = Integer.MIN_VALUE;				
				doSyntaxColor();
			}
		}

		// This call lets clients know whether doc changes currently in progress are due to internal
		// machinations of the document rather than user events. They can use this information to determine
		// whether to record undoable events that may be generated. If this returns false, then they probably
		// do not want to put the current UndoableEvent into the undo queue.
		public boolean isRecordUndos() {
			return recordUndos;
		}
		
		
		void setFirstEditPos(int pos) {
			firstEditPos = pos;
		}
				
		int getFirstEditPos() {
			return firstEditPos;
		}


		String getEvalInput() {
			try {
				return getText(firstEditPos, getLength() - firstEditPos);
			} catch (BadLocationException e) {
				e.printStackTrace();
				return "";
			}
		}
			
		
		// This is a relatively expensive operation. Actually determining the syntax runs (that is, iterating
		// tokenizer.hasMoreRecords()) is very quick (easily tens of thousands of characters can be scanned
		// every keystroke). What is costly is setting character attributes. Therefore some steps are taken
		// to minimize the amount of text that is actively styled. We do some work to only style text that is
		// currently visible. This means that we must ensure that this method is called when the text is
		// scrolled or the pane is resized.
		private boolean lastPassWasColored = false;
		
		void doSyntaxColor() {

			if (!isInputMode())
				return;

			if (!isSyntaxColoring() && !colorsHaveChanged)
				return;

			// If syntax coloring is off, just set all text to normal attribute and return. We only even get here if
			// syntax coloring was previoulsy on and has now been turned off.
			if (!isSyntaxColoring()) {
				recordUndos = false;
				try {
					setCharacterAttributes(firstEditPos, getLength() - firstEditPos, attrNormal, false);
			   } finally {
			   	recordUndos = false;
			   	colorsHaveChanged = false;
			   }
			   return;
			}
			
			Dimension d = getVisibleTextBounds();
			// All indices are offsets from firstEditPos. These two variables are the starting and ending indices
			// for text that will be actively styled this pass.
			int firstVisibleChar = Math.max(d.width - firstEditPos, 0);
			int lastVisibleChar = d.height - firstEditPos;
			
			// This can happen if we are called from a scroll event and the view has not been updated.
			// firstEditPos has been set, but the viewport has not been moved to show the insertion point yet.
			if (lastVisibleChar < 0)
				return;
			
			int startChar = firstVisibleChar;
			int endChar = lastVisibleChar;
			
			// These tests allow a simple optimization in the case of a scroll: only style text in the newly-revealed area
			// at the top or bottom of the last styled range. None of these branches will be taken if an edit has
			// occurred--the whole visible area will be re-styled.
			if (colorsHaveChanged) {
				// If colors have changed, skip all the machinations withe lastStyledStart and visible region. Will
				// need to redo whole input.
				startChar = 0;
				endChar = getLength() - firstEditPos;
			} else if (firstVisibleChar >= lastStyledStart && lastVisibleChar <= lastStyledEnd) {
				return;
			} else if (firstVisibleChar < lastStyledStart) {
				endChar = lastVisibleChar > lastStyledEnd ? lastVisibleChar : lastStyledStart;
			} else if (lastVisibleChar > lastStyledEnd) {
				startChar = firstVisibleChar < lastStyledStart ? firstVisibleChar : lastStyledEnd;
			}

			colorsHaveChanged = false;
			
			String input = getEvalInput();
			tokenizer.setText(input);
			
			recordUndos = false;
			try {
				setCharacterAttributes(firstEditPos + startChar, endChar - startChar, attrNormal, false);

				AttributeSet curAttr = attrNormal;
				AttributeSet attr = null;
				while (tokenizer.hasMoreRecords()) {
					SyntaxTokenizer.SyntaxRecord rec = tokenizer.getNextRecord();
					switch (rec.type) {
						case SyntaxTokenizer.NORMAL:
							attr = attrNormal;
							break;
						case SyntaxTokenizer.COMMENT:
							attr = attrComment;
							break;
						case SyntaxTokenizer.STRING:
							attr = attrString;
							break;
						case SyntaxTokenizer.SYMBOL: {
							attr = attrNormal;
							String sym = input.substring(rec.start, rec.start + rec.length);
							boolean wasUserSymbol = false;
							if (userSymbols != null) {
								int sz = userSymbols.size();
								for (int i = 0; i < sz; i++) {
									HashMap h = (HashMap) userSymbols.elementAt(i);
									if (h.containsKey(sym)) {
										wasUserSymbol = true;
										attr = (AttributeSet) userColors.elementAt(i);
									}
								}
							}
							if (!wasUserSymbol && systemSymbols.containsKey(sym))
								attr = attrSystem;
							break;
						}
					}
					if (attr != attrNormal && attr != curAttr && intervalsIntersect(rec.start, rec.start + rec.length, startChar, endChar))
						setCharacterAttributes(rec.start + firstEditPos, rec.length, attr, false);
					curAttr = attr;
				}
			} finally {
				recordUndos = true;
			}
			
			lastStyledStart = Math.min(lastStyledStart, Math.min(startChar, firstVisibleChar));
			lastStyledEnd = Math.max(lastStyledEnd, Math.max(endChar, lastVisibleChar));
		}

	}  // End of MTDocument.
	
	
	private static boolean intervalsIntersect(int a_start, int a_end, int b_start, int b_end) {
		return a_start <= b_end && a_end >= b_start;
	}

}  // End of MathSessionTextPane class.


///////////////////////////////////  PktHandler  //////////////////////////////////

// Here is where the actual printing of the output to the pane is performed.

class PktHandler implements PacketListener {
			
	private JTextPane comp;
	private MathSessionTextPane.MTDocument doc;
	private boolean lastWasMessage = false;
			
	PktHandler(JTextPane comp, MathSessionTextPane.MTDocument doc) {
		this.comp = comp;
		this.doc = doc;
	}

	public boolean packetArrived(PacketArrivedEvent evt) {
		// All work involving the link must be done here, not in handleString or handleImage.
		// Those execute on the event-dispatch thread, and we will get deadlock over contention for ml.
		KernelLink ml = (KernelLink) evt.getSource();
		try {
			switch (evt.getPktType()) {
				case MathLink.INPUTNAMEPKT:
				case MathLink.OUTPUTNAMEPKT:
				case MathLink.RETURNTEXTPKT:
				case MathLink.MESSAGEPKT:
				case MathLink.TEXTPKT: {
					String s = ml.getString();
					handleString(evt.getPktType(), s, lastWasMessage);
					break;
				}
				case MathLink.DISPLAYPKT: {
					// DISPLAYPKT is hijacked to supply the result of EvaluateToImage.
					byte[] imageData = ml.getByteString((byte) 0);
					handleImage(imageData);
					break;
				}
				default:
					// e.g. MessagePacket
					break;
			}
		} catch (MathLinkException e) {
			e.printStackTrace();
			ml.clearError();
		} catch (InvocationTargetException ee) {
			ee.printStackTrace();
		} catch (InterruptedException eee) {
			eee.printStackTrace();
		}
		lastWasMessage = evt.getPktType() == MathLink.MESSAGEPKT;
		return true;
	}
   
   
	private void handleString(final int pkt, final String s, final boolean lastWasMessage) throws InvocationTargetException, InterruptedException {
		
        // In typical circumstances, this code is called from the EvalTask thread, not the
        // event dispatch thread, so we must use invokeAndWait(). In some cases, however,
        // this code can be called on the dispatch thread, so we must test for this and if
        // true we just invoke directly.
		if (SwingUtilities.isEventDispatchThread()) {
            // To get this called on the event dispatch thread, use a MathSessionPane in an
            // InstallJava situation. Because we add the packet listener that calls this code to
            // the link itself (the UI link), it will be called for packets coming from a button,
            // not just from comps run via shift-enter in the MathSessionPane.
            handleString0(pkt, s, lastWasMessage);
        } else {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    handleString0(pkt, s, lastWasMessage);
                }
            });
        }
	}
  
    private void handleString0(final int pkt, final String s, final boolean lastWasMessage) {
        
        try {
            switch (pkt) {
                case MathLink.INPUTNAMEPKT:
                    doc.setLogicalStyle(doc.getLength(), comp.getStyle("prompt"));
                    // Make sure there is a blank line before prompt.
                    if (doc.getLength() >= 2 && !doc.getText(doc.getLength() - 2, 2).equals("\n\n"))
                        doc.insertString(doc.getLength(), "\n", null);
                    doc.insertString(doc.getLength(), s + "\n", null);
                    doc.setFirstEditPos(doc.getLength());
                    doc.setLogicalStyle(doc.getLength(), comp.getStyle("input"));
                    break;
                case MathLink.OUTPUTNAMEPKT:
                    doc.setLogicalStyle(doc.getLength(), comp.getStyle("prompt"));
                    doc.insertString(doc.getLength(), s + "\n", null);
                    break;
                case MathLink.RETURNTEXTPKT:
                    doc.setLogicalStyle(doc.getLength(), comp.getStyle("output"));
                    doc.insertString(doc.getLength(), s + "\n\n", null);
                    break;
                case MathLink.TEXTPKT: {
                    String msg = s;
                    if (lastWasMessage) {
                        if (!msg.endsWith("\n"))
                            msg = msg + "\n";
                        if (!msg.endsWith("\n\n"))
                            msg = msg + "\n";
                    } else {
                        doc.setLogicalStyle(doc.getLength(), comp.getStyle("print"));
                    }
                    doc.insertString(doc.getLength(), msg, null);
                    break;
                }
                case MathLink.MESSAGEPKT:
                    doc.setLogicalStyle(doc.getLength(), comp.getStyle("message"));
                    // Don't insert the string--the actual text comes in the subsequent TextPacket.
                    break;
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    
	private void handleImage(final byte[] imageData) throws InvocationTargetException, InterruptedException {
		
        // See comments in handleString for info on this 'if' test.
        if (SwingUtilities.isEventDispatchThread()) {
            handleImage0(imageData);
        } else {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    handleImage0(imageData);
                }
            });
        }
	}
	
    private void handleImage0(final byte[] imageData) {
        
        try {
            doc.setLogicalStyle(doc.getLength(), comp.getStyle("graphics"));
            // Next 5 lines are mostly borrowed from the implementation of insertIcon() in JTextPane.
            // I can't use that method because it works only on the selection (doesn't allow
            // you to specify the location by index).
            MutableAttributeSet inputAttributes = comp.getInputAttributes();
            inputAttributes.removeAttributes(inputAttributes);
            StyleConstants.setIcon(inputAttributes, new ImageIcon(imageData));
            doc.insertString(doc.getLength(), " ", comp.getInputAttributes());
            inputAttributes.removeAttributes(inputAttributes);
            doc.insertString(doc.getLength(), "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
}  // End of PktHandler
