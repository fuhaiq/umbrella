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

// A do-nothing Preferences implementation so that we can avoid the hassles
// of the JVM Unix Preference implementations, which throw exceptions for many
// users. Do a search on "Java preferences bug" and you'll find ample discussion.
// This will probably be removed when Sun gets the bugs out (e.g., Java 1.5).
//

public class DisabledPreferences extends java.util.prefs.AbstractPreferences {

    public DisabledPreferences() {
        super(null, "");
    }
    
    protected void putSpi(String key, String value) {}
    
    protected String getSpi(String key) {
        return null;
    }
    
    protected void removeSpi(String key) {}
    
    protected void removeNodeSpi() throws java.util.prefs.BackingStoreException {}
    
    protected String[] keysSpi() throws java.util.prefs.BackingStoreException {
        return new String[0];
    }
    
    protected String[] childrenNamesSpi() throws java.util.prefs.BackingStoreException {
        return new String[0];
    }
    
    protected java.util.prefs.AbstractPreferences childSpi(String name) {
        return null;
    }
    
    protected void syncSpi() throws java.util.prefs.BackingStoreException {}
    
    protected void flushSpi() throws java.util.prefs.BackingStoreException {}
}
