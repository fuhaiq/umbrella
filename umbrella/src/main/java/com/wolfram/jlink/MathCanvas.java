//////////////////////////////////////////////////////////////////////////////////////
//
//   J/Link source code (c) 1999-2001, Wolfram Research, Inc. All rights reserved.
//
//   Use is governed by the terms of the J/Link license agreement, which can be found at
//   www.wolfram.com/solutions/mathlink/jlink.
//
//   Author: Todd Gayley
//
//////////////////////////////////////////////////////////////////////////////////////

package com.wolfram.jlink;

import java.awt.*;

/**
 * MathCanvas is a class that gives programmers an easy way to display Mathematica graphics
 * or typeset expressions. This class can be used in either Mathematica programs or Java programs.
 * MathCanvas is the AWT counterpart to the Swing-based MathGraphicsJPanel (MathCanvas would
 * have been better named MathGraphicsCanvas). Use a MathCanvas if you are writing an AWT program
 * and a MathGraphicsJPanel if you are writing a Swing program.
 * <p>
 * A typical use is in a Mathematica program where you want to display a window that contains a
 * Mathematica graphic or typeset expression. To do this, you would create a MathCanvas, create a
 * Frame to hold it (perhaps a MathFrame), then use the setMathCommand() method to specify a plotting
 * command that will be used by the MathCanvas to create the image to display. One of the sample
 * programs included with J/Link, and described in detail in the User Guide, demonstrates this.
 * <p>
 * MathCanvas operates in one of two modes: it either evaluates a command in Mathematica to produce
 * the image to display, or it displays a Java Image that you set directly with the setImage() method.
 * In the latter case, the Mathematica command is ignored and no computations are performed.
 * <p>
 * This class is a JavaBean.
 * 
 * @see MathGraphicsJPanel
 */

public class MathCanvas extends Canvas implements java.io.Serializable {

   private MathGraphicsDelegate delegate;
	
	/**
	 * Value to be used in setImageType() to specify that the image to be displayed is
	 * the graphics output of a plotting command, rather than the typeset result of an
	 * arbitrary computation.
	 * 
	 * @see #setImageType(int)
	 */
	public static final int GRAPHICS	= MathGraphicsDelegate.GRAPHICS;

	/**
	 * Value to be used in setImageType() to specify that the image to be displayed is
	 * the typeset result of an arbitrary computation, rather than the graphics output of
	 * a plotting command.
	 * 
	 * @see #setImageType(int)
	 */
	public static final int TYPESET	= MathGraphicsDelegate.TYPESET;


	/**
	 * The constructor that is typically called from Mathematica.
	 */
	
	public MathCanvas() {
		delegate = new MathGraphicsDelegate(this);
	}

	/**
	 * You typically use this constructor when using this class in a Java program,
	 * because you need to specify the KernelLink that will be used. Alternatively, you can
	 * use the no-arg constructor and later call setLink() to specify the link.
	 * 
	 * @param ml The link to which computations will be sent.
	 */
	
	public MathCanvas(KernelLink ml) {
		delegate = new MathGraphicsDelegate(this, ml);
	}
	

	
	/**
	 * Sets the link that will be used for computations. This method is only called in Java programs
	 * (the correct link back to the kernel is automatically established for you when using this
	 * class from Mathematica).
	 * 
	 * @param ml
	 */
	public void setLink(KernelLink ml) {
		delegate.setLink(ml);
	}
	
	/**
	 * Specifies whether the canvas should display an image produced from a graphics function,
	 * or the typeset result of a computation. The possible values are GRAPHICS and TYPESET.
	 * The default is GRAPHICS. This method does not trigger an update of the image--you must call
	 * setMathCommand() or recompute() to see the effects of the new setting.
	 * 
	 * @param type the type of output you want, either GRAPHICS or TYPESET
	 */
	public void setImageType(int type) {
		delegate.setImageType(type);
	}


	/**
	 * Gives the image type this MathCanvas is currently set to display, either GRAPHICS or TYPESET.
	 */
	public int getImageType() {
		return delegate.getImageType();
	}
	
	
	/**
	 * Specifies whether to use the services of the Mathematica front end in rendering the image to display.
	 * The default is false. Some reasons why you might want to set this to true:
	 * <pre>
	 *     - You want your graphics to contain typeset expressions (e.g., in a PlotLabel)
	 *     - The front end generally does a nicer job than the alternative method
	 * Some reasons why you might want to leave this false:
	 *     - It is simpler and more direct to not use the front end if you do not need it
	 *     - The front end needs to be running (usually not a problem if you are using
	 *       this class from a Mathematica program); it will be launched if it is not
	 *       already running. There some implications for this that are discussed in the
	 *       section on Mathematica graphics in Part 2 of the User Guide.</pre>
	 * If you use setImageType() to specify TYPESET, then the front end will always be used, no matter what you
	 * specify in setUsesFE(). This method does not trigger an update of the image--you must call
	 * setMathCommand() or recompute() to see the effects of the new setting.
	 * 
	 * @param useFE
	 */
	public void setUsesFE(boolean useFE) {
		delegate.setUsesFE(useFE);
	}

	/**
	 * Indicates whether this MathCanvas is currently set to use the notebook front end to assist in graphics rendering.
	 */
	public boolean getUsesFE() {
		return delegate.getUsesFE();
	}

	/**
	 * Specifies whether typeset output is to be rendered in TraditionalForm or StandardForm.
	 * The default is false (uses StandardForm). This is only relevant when setImageType() is
	 * used to specify typeset output (as opposed to graphics output). This method does not
	 * trigger an update of the image--you must call setMathCommand() or recompute() to see the
	 * effects of the new setting.
	 * 
	 * @param useTradForm
	 */
	public void setUsesTraditionalForm(boolean useTradForm) {
		delegate.setUsesTraditionalForm(useTradForm);
	}

	/**
	 * Indicates whether this MathCanvas is currently set to return typeset results in TraditionalForm
	 * (vs. the default StandardForm). This is only relevant when setImageType() has been
	 * used to specify typeset output (as opposed to graphics output).
	 */
	public boolean getUsesTraditionalForm() {
		return delegate.getUsesTraditionalForm();
	}


	/**
	 * Specifies the Mathematica command that is used to generate the image to display. For graphics
	 * output, this will typically be a plotting command, such as "Plot[x,{x,0,1}]". For typeset output,
	 * any expression can be given; its result will be typeset and displayed. Note that it is the <i>result</i>
	 * of the expression that is displayed, so do not make the mistake of ending the expression with a
	 * semicolon, as this will make the expression evaluate to Null. This is especially important with
	 * graphics--many Mathematica expressions will produce plots as a side effect, but you must supply
	 * an expression that <i>evaluates to</i> a Graphics expression (or Graphics3D, SurfaceGraphics, etc.)
	 * <p>
	 * You may find it more convenient to define the command in Mathematica as a function and then
	 * specify only the function call in setMathCommand(). For example, when using this class from
	 * a Mathematica program, you might do:
	 * <pre>
	 *     plotFunc[] := Plot[...complex plot command...];
	 *     myMathCanvas@setMathCommand["plotFunc[]"];</pre>
	 * 
	 * @param cmd
	 */
	public void setMathCommand(String cmd) {
		delegate.setMathCommand(cmd);
	}

	/**
	 * Returns the Mathematica command that is used to generate the image to display.
	 */
	public String getMathCommand() {
		return delegate.getMathCommand();
	}


	/**
	 * Allows you to directly specify an Image to display, rather than evaluating the
	 * mathCommand. Once setImage() is called, the mathCommand is ignored and no computations
	 * are performed until the next call to setMathCommand().
	 * <p>
	 * Use setImage() when you have created an Image in Mathematica or Java yourself. An example
	 * of this is if you manually create a Java bitmap image from a Mathematica array. This is
	 * demonstrated in the User Guide.
	 * 
	 * @param im the Image to display
	 * @see #setMathCommand(String)
	 */
	public void setImage(Image im) {
		delegate.setImage(im);
	}
	
	/**
	 * Returns the image that is currently being displayed.
	 */
	public Image getImage() {
		return delegate.getImage();		
	}
	
	
	/**
	 * If a mathCommand is being used to create the image to display, this method causes it to
	 * be recomputed to produce a new image. Call recompute() if your mathCommand depends
	 * on values in Mathematica that have changed since the last time you called setMathCommand()
	 * or recompute().
	 */
	public void recompute() {
		delegate.recompute();
	}
	
	
	/**
	 * Forces an immediate repainting of the image. Similar to the standard repaint() method, except that
	 * with repaint(), the painting may be delayed if the user-interface thread is very busy. 
	 * This method is intended to be called from Mathematica code, most likely after setMathCommand(), setImage(),
	 * or recompute(). Because several calls between Mathematica and Java are typically required to establish
	 * a new image to display, and these calls are comparatively time-consuming, on slower machines it may be
	 * the case that calls to repaint() do not trigger painting to occur often enough to provide smooth visual
	 * feedback (for example, when the image is being recomputed as the mouse is being dragged). The repaintNow()
	 * method is provided for such circumstances.
	 */
	public void repaintNow() {
		
		delegate.ensureImageReady();

		// We will eventually just call paintImmediately() here, when that method is migrated into Component.
		Graphics g = getGraphics();
		if (g != null) {
			update(g);
			g.dispose();
		}
		// You are not really supposed to call update yourself--it should percolate up from the guts of AWT.
		// In case our faked call to update doesn't do exactly what it is supposed to do, we queue up a normal
		// repaint, not really caring when the system gets around to doing it.
		repaint();
	}
	
	
	public void update(Graphics g) {
		paint(g);
	}
	
	public void paint(Graphics g) {

		if (delegate.getImage() != null) {
			delegate.paintImage(g);
		} else {
			super.paint(g);
		}
	}

}
