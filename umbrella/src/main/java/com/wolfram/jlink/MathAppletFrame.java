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

// This is the class used to host applets, used by the AppletViewer[] Mathematica function.
// Based loosely on MainFrame.java, written by Jef Poskanzer (www.acme.com/java/).

package com.wolfram.jlink;

import java.applet.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.net.*;
import java.io.*;
import java.util.*;

public class MathAppletFrame extends Frame implements Runnable, AppletStub, AppletContext {

	private String[] args = null;
	private String name;
	private Applet applet;
	private Dimension appletSize;
	private URL codeBase;

	public MathAppletFrame(Applet applet, String[] args) {
		
		this.applet = applet;
		this.args = args;
		applet.setStub(this);
		name = applet.getClass().getName();
		setTitle(name);

		if (args != null)
		parseArgs(args, System.getProperties());

		String widthStr = getParameter("width");
		String heightStr = getParameter("height");
		// Default dimensions.
		int width = 300;
		int height = 300;
		if (widthStr != null && heightStr != null) {
			width = Integer.parseInt(widthStr);
			height = Integer.parseInt(heightStr);
		}
		
		setLayout(new BorderLayout());
		add("Center", applet);

		pack();
		validate();
		appletSize = applet.getSize();
		applet.setSize(width, height);
		setResizable(false);
		show();
        enableEvents(-1);
        
		(new Thread(this)).start();
	}

	private static void parseArgs(String[] args, Properties props) {
		
		for (int i = 0; i < args.length; ++i) {
		String arg = args[i];
		int ind = arg.indexOf('=');
		if (ind == -1)
			props.put("parameter." + arg.toLowerCase(), "");
		else
			props.put("parameter." + arg.substring(0, ind).toLowerCase(), arg.substring(ind + 1));
		}
	}

    
    protected void processEvent(AWTEvent evt) {
        
        if (evt.getID() == WindowEvent.WINDOW_CLOSING) {
            setVisible(false);
            remove(applet);
            applet.stop();
            applet.destroy();
            dispose();
        }
        super.processEvent( evt );
    }

    
	public void run() {
		applet.init();
		applet.start();
	}

	public boolean isActive() {
		return true;
	}

	public URL getDocumentBase() {
		String dir = System.getProperty("user.dir");
		String urlDir = dir.replace(File.separatorChar, '/');
		try {
			return new URL("file:" + urlDir + "/");
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public URL getCodeBase()
	{
		// Hack: loop through each item in CLASSPATH, checking if
		// the appropriately named .class file exists there.  But
		// this doesn't account for .zip files.
		if (codeBase == null) {
			String path = System.getProperty( "java.class.path" );
			Enumeration st = new StringTokenizer( path, ":" );
			while ( st.hasMoreElements() ) {
				String dir = (String) st.nextElement();
				String filename = dir + File.separatorChar + name + ".class";
				File file = new File( filename );
				if ( file.exists() ) {
					String urlDir = dir.replace( File.separatorChar, '/' );
					try {
						codeBase = new URL( "file:" + urlDir + "/" );
						return codeBase;
					} catch ( MalformedURLException e ) {
						return null;
					}
				}
			}
			return null;
		} else {
			return codeBase;
		}
	}

	public String getParameter(String name) {
		return System.getProperty("parameter." + name.toLowerCase());
	}

	public void appletResize(int width, int height) {
		Dimension frameSize = getSize();
		frameSize.width += width - appletSize.width;
		frameSize.height += height - appletSize.height;
		setSize(frameSize);
		appletSize = applet.getSize();
	}

	public AppletContext getAppletContext() {
		return this;
	}


	// Methods from AppletContext.

	public AudioClip getAudioClip(URL url) {
		return null;
	}

	public Image getImage(URL url) {
		Toolkit tk = Toolkit.getDefaultToolkit();
		try {
			java.awt.image.ImageProducer prod = (java.awt.image.ImageProducer) url.getContent();
			return tk.createImage(prod);
		} catch (IOException e) {
			return null;
		}
	}

	public Applet getApplet(String name) {
		if (name.equals(this.name))
			return applet;
		return null;
	}

	public Enumeration getApplets() {
		Vector v = new Vector();
		v.addElement(applet);
		return v.elements();
	}

	public void showDocument(URL url) {}

	public void showDocument(URL url, String target) {}

	public void showStatus(String status) {}

	/***  These are new in JDK1.4  ***/
	public void setStream(String key, InputStream stream) throws IOException {}
	public InputStream getStream(String key) { return null; }
	public Iterator getStreamKeys()  { return null; }
}
