package com.umbrella.kernel;

import com.alibaba.fastjson.JSONArray;
import com.google.inject.Inject;
import com.umbrella.session.Session;
import com.umbrella.session.SessionException;
import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLinkException;

public class KernelImpl implements Kernel {
	
	@Inject
	private Session<KernelLink> session;
	
	@Override
	public JSONArray evaluate(String expression) throws MathLinkException, SessionException {
		expression = expression.replace((char)160, (char)32);
		KernelLink kernelLink = session.get();
		kernelLink.putFunction("EnterTextPacket", 1);
		kernelLink.put(expression);
		kernelLink.endPacket();
		kernelLink.flush();
		kernelLink.discardAnswer();
		return kernelLink.result();
	}
	
}
