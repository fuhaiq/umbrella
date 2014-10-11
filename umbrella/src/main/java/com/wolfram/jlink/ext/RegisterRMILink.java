/****

To use this class:

   Don't forget that a JLink.properties file must be present next to JLink.jar
   with a line like this:
   
       MathLink.rmi=com.wolfram.jlink.ext.MathLink_RMI
   
   
1) Start rmiregistry:

   c:\> rmiregistry

2) Launch RegisterRMILink:

 java -classpath d:\mathdev\addons\jlink\jlink.jar
       -Djava.rmi.server.codebase=file:/d:\mathdev\addons\jlink\jlink.jar
        com.wolfram.jlink.ext.RegisterRMILink -linkmode launch
        -linkname d:\math52\mathkernel -rminame linkserver

3) Use the link from a client:

   c:\> java SampleProgram -linkprotocol RMI -linkname linkserver  (or, linkname can be a full URL:  rmi://localhost/linkserver)


****/

package com.wolfram.jlink.ext;

import java.rmi.*;


public class RegisterRMILink {

	public static void main(String[] argv) {
		
		try {
			String rmiName = determineRMIName(argv);
			if (rmiName == null) {
				System.out.println("Error: -rminame option not specified.");
				return;
			}
			RemoteMathLink ml = new NativeRemoteLink(argv); 
			java.rmi.server.UnicastRemoteObject.exportObject(ml);
			Naming.rebind(rmiName, ml); 
			System.out.println(rmiName + " successfully bound in RMI registry."); 
		} catch (Exception e) { 
			System.out.println("RMITest err: " + e.getMessage()); 
			e.printStackTrace(); 
		}
	}
	
	private static String determineRMIName(String[] argv) {
		
		if (argv != null) {
			for (int i = 0; i < argv.length - 1; i++) {
				if (argv[i].toLowerCase().equals("-rminame"))
					return argv[i+1].toLowerCase();
			}
		}
		return null;
	}

}