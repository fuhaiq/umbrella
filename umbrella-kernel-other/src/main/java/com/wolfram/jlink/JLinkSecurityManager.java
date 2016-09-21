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

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;


/**
 * This is a security manager that prevents code called from Mathematica from calling System.exit().
 * This manager makes no attempt to be "secure". For example, it does not prevent code from
 * installing a different security manager. Its only purpose is to prevent "accidental" calls to System.exit(),
 * which sometimes appear in code that people want to call using J/Link (for example, in main() functions).
 */
public class JLinkSecurityManager extends SecurityManager {
	
	protected boolean allowExit = false;
	
	public static void setAllowExit(boolean allow) {
		
	    SecurityManager sm = System.getSecurityManager();
	    if (sm instanceof JLinkSecurityManager) {
	        JLinkSecurityManager jsm = (JLinkSecurityManager) sm;
	        jsm.allowExit = allow;
	    }
	}
	

    public void checkPermission(Permission perm) { 
        
        if (perm instanceof RuntimePermission) {
            if (perm.getName().startsWith("exitVM") && !allowExit)
                throw new SecurityException("J/Link does not allow code called from Mathematica to call System.exit().");                
        }
        // Allow everything else.
    }
    
    public void checkPermission(Permission perm, Object context) { 
        checkPermission(perm);
    }
    
}	
