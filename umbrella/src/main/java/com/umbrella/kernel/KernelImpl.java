package com.umbrella.kernel;

import com.alibaba.fastjson.JSONArray;
import com.google.inject.Inject;
import com.umbrella.UmbrellaConfig;
import com.umbrella.session.Session;
import com.umbrella.session.SessionException;
import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLinkException;

public class KernelImpl implements Kernel {
	
	@Inject
	private Session<KernelLink> session;
	
	@Inject private UmbrellaConfig umbrella;

	@Override
	public JSONArray evaluate(String dir, String expression) throws MathLinkException, SessionException {
		expression = expression.replace((char)160, (char)32);
		KernelLink kernelLink = session.get();
		kernelLink.putFunction("TimeConstrained", 2);
		kernelLink.putFunction("Umbrella", 2);
		kernelLink.put(dir);
		kernelLink.put(expression);
		kernelLink.put(umbrella.getKernel().getTimeConstrained());
		kernelLink.endPacket();
		kernelLink.discardAnswer();
		return kernelLink.result();
	}
	
}
