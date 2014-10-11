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
public class DisabledPreferencesFactory implements java.util.prefs.PreferencesFactory {

    public java.util.prefs.Preferences systemRoot() {
        return new DisabledPreferences();
    }
    
    public java.util.prefs.Preferences userRoot() {
        return new DisabledPreferences();
    }
}
