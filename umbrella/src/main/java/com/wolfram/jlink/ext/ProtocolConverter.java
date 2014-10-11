package com.wolfram.jlink.ext;

import com.wolfram.jlink.*;


public class ProtocolConverter {
	
	private KernelLink fe2k, k2fe;
	
	
	public static void main(String[] argv) {
		
		KernelLink fe2me = null;
		KernelLink me2k = null;
		
		try {
			fe2me = MathLinkFactory.createKernelLink(argv);
			fe2me.connect();
			
			fe2me.putFunction("InputNamePacket", 1);
			fe2me.put("In[1]:= ");
			fe2me.flush();
			
		} catch (MathLinkException e) {
			System.out.println("Error establishing link back to the launching program.");
			return;
		}
		
		String specialProtocol = extractProtocol(argv);
		String specialName = extractName(argv);
		if (specialName == null || specialProtocol == null) {
			fe2me.close();
			System.out.println("ProtocolConverter: Invalid specification of -name or -prot options.");
			return;
		}
		
		try {
			me2k = MathLinkFactory.createKernelLink(
						"-linkmode launch -linkname " + specialName + " -linkprotocol " + specialProtocol);
			me2k.connect();
		} catch (MathLinkException e) {
			System.out.println("ProtocolConverter: Error establishing link to the kernel. " + e.toString());
			try {Thread.sleep(4000);} catch (InterruptedException exc) {}
			return;
		}
		
		try {
		    boolean firstTime = true;
			while (true) {
				if (fe2me.ready()) {
					me2k.transferExpression(fe2me);
					me2k.flush();
				} else if (me2k.ready()) {
					Expr e = me2k.getExpr();
				    boolean isFirstINP = false;
					if (firstTime) {
					    try {
					        isFirstINP = e.part(1).stringQ() && e.part(1).asString().startsWith("In[1]:=");
					    } catch (ExprFormatException ee) {}
					}
					firstTime = false;
			        if (!isFirstINP) {
                        fe2me.put(e);
                        fe2me.flush();
					}
				}
				if (fe2me.error() != MathLink.MLEOK || me2k.error() != MathLink.MLEOK)
					throw new MathLinkException(fe2me.error(), fe2me.errorMessage());
   			try {Thread.sleep(20);} catch (InterruptedException e) {}
			}
		} catch (MathLinkException e) {
			System.out.println("ProtocolConverter: Error in main loop: " + e.toString());
			fe2me.close();
			me2k.close();
		}
	}
	
	
	private static String extractProtocol(String[] argv) {
		
		String prot = null;
		for (int i = 0; i < argv.length - 1; i++) {
			if (argv[i].toLowerCase().equals("-prot")) {
				prot = argv[i+1].toUpperCase();
				break;
			}
		}
		return prot;
	}
	
	private static String extractName(String[] argv) {
		
		String name = null;
		for (int i = 0; i < argv.length - 1; i++) {
			if (argv[i].toLowerCase().equals("-name")) {
				name = argv[i+1];
				break;
			}
		}
		return name;
	}
	
}
