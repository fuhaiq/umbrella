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


// This class contains shared code used by MathCanvas and MathGraphicsJPanel. They implement virtually
// all their functionality by delegating calls to an instance of this class.


public class MathGraphicsDelegate implements java.io.Serializable {

	private transient KernelLink ml;
    private boolean useStdLink;

	private Component comp;  // The Component that is using this class to delegate functionality to.
	private boolean useFE;
	private boolean useTradForm;
	private int imageType = GRAPHICS;
	private String mathCommand;
	private Image im;
	private MediaTracker tracker;
	
	
	public static final int GRAPHICS	= 0;
	public static final int TYPESET	= 1;
	
	
	public MathGraphicsDelegate(Component comp) {
		this(comp, null);
        useStdLink = true;
	}

	public MathGraphicsDelegate(Component comp, KernelLink ml) {
		this.comp = comp;
		this.ml = ml;
        useStdLink = false;
		tracker = new MediaTracker(comp);
	}
	
	public void setLink(KernelLink ml) {
		this.ml = ml;
        useStdLink = false;
	}
	
	public void setImageType(int type) {
		imageType = type;
	}

	public int getImageType() {
		return imageType;
	}

	public void setUsesFE(boolean useFE) {
		this.useFE = useFE;
	}

	public boolean getUsesFE() {
		return useFE;
	}

	public void setUsesTraditionalForm(boolean useTradForm) {
		this.useTradForm = useTradForm;
	}

	public boolean getUsesTraditionalForm() {
		return useTradForm;
	}


	public String getMathCommand() {
		return mathCommand;
	}

	public void setMathCommand(String cmd) {

		mathCommand = cmd;
		byte[] gifData = null;
        KernelLink link = useStdLink ? StdLink.getLink() : ml;
		if (link != null) {
			if (link.equals(StdLink.getLink()))
				StdLink.requestTransaction();
			if (imageType == GRAPHICS)
				// Graphics rendered by Mathematica often spill out of their bounding box a bit, so we'll compensate
				// a bit by subtracting from the true width and height of the hosting component.
				gifData = link.evaluateToImage(mathCommand, comp.getSize().width - 6, comp.getSize().height - 6, 0, useFE);
			else
				gifData = link.evaluateToTypeset(mathCommand, comp.getSize().width - 4, !useTradForm);
		}
		im = gifData != null ? comp.getToolkit().createImage(gifData) : null;
		if (!ensureImageReady())
			im = null;
		comp.repaint();
	}


	public void setImage(Image im) {
		
		this.im = im;
		mathCommand = null;
		// Don't call ensureImageReady() for all images, in case the user is setting an image that does not have its pixels handy.
		// It may be that ensureImageReady() is a no-op for buffered images, but I don't know for sure, so I'll call it anyway.
		if (im instanceof java.awt.image.BufferedImage)
			if (!ensureImageReady())
				im = null;
		comp.repaint();
	}
	
	public Image getImage() {
		return im;		
	}
	
	
	public void recompute() {
		// Ignore if mathCommand is null (we are in "manual" setImage mode).
		if (mathCommand != null)
			setMathCommand(mathCommand);
	}
	
	
	public boolean ensureImageReady() {
		
		// Wait until pixels are fully loaded before proceeding. A call to this method means we want the
		// image drawn now, so we make sure it is all there.
		tracker.addImage(im, 0);
		try {
			tracker.waitForID(0);
		} catch (Exception e) {}
		boolean wasOK = tracker.isErrorID(0) == false;
		tracker.removeImage(im, 0);
		return wasOK;
	}
	
	
	public void paintImage(Graphics g) {

		// Caller must have ensured that im != null.
		int imageHeight = im.getHeight(comp);
		int imageWidth = im.getWidth(comp);
		// ensureImageReady() has probably been called, so it is unlikely that the width and height would
		// be reported as -1.
		if (imageWidth != -1 && imageHeight != -1) {
			Dimension d = comp.getSize();
			int compWidth = d.width;
			int compHeight = d.height;
			Insets insets = (comp instanceof Container) ? ((Container) comp).getInsets() : new Insets(0, 0, 0, 0);
			int paintWidth = compWidth - insets.left - insets.right;
			int paintHeight = compHeight - insets.top - insets.bottom;
			int left = (compWidth - imageWidth)/2;
			int top = (compHeight - imageHeight)/2;
			g.drawImage(im, left, top, comp);
			// Paint any area outside the image with the background color. Break up border into 4 rects.
			g.clearRect(insets.left, insets.top, paintWidth, top - insets.top);
			g.clearRect(insets.left, top, left - insets.left, imageHeight);
			g.clearRect(left + imageWidth, top, compWidth - imageWidth - left - insets.right, imageHeight);
			g.clearRect(insets.left, top + imageHeight, paintWidth, compHeight - imageHeight - top - insets.bottom);
		}
	}
	
}
