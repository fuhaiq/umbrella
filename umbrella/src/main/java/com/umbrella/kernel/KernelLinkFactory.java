package com.umbrella.kernel;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;
import com.umbrella.UmbrellaConfig;
import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLinkFactory;
import com.wolfram.jlink.PacketListener;

public class KernelLinkFactory extends BasePooledObjectFactory<KernelLink> {
	
	private final Logger LOG = LogManager.getLogger("KernelLinkFactory");

	@Inject private PacketListener listener;
	
	@Inject private UmbrellaConfig umbrella;
	
	@Override
	public KernelLink create() throws Exception {
		KernelConfig config = umbrella.getKernel();
		KernelLink kernelLink = MathLinkFactory.createKernelLink(config.getUrl());
		kernelLink.connect();
		kernelLink.discardAnswer();
		kernelLink.addPacketListener(listener);
//		kernelLink.evaluate("Needs[\"" + KernelLink.PACKAGE_CONTEXT + "\"]");
//		kernelLink.discardAnswer();
		kernelLink.evaluate("Needs[\"Umbrella`\"]");
		kernelLink.discardAnswer();
//		kernelLink.evaluate("$PrePrint = With[{expr = #}, If[MatchQ[expr, _Graphics | _Graphics3D | _Graph | _Manipulate] || MemberQ[expr, _Graphics | _Graphics3D | _Graph | _Manipulate, ∞], LinkWrite[$ParentLink, DisplayPacket[EvaluateToTypeset[expr, TraditionalForm, "+config.getPageWidth()+"]]], MathMLForm[expr]]] &;");
//		kernelLink.discardAnswer();
		LOG.info("Create the Kernel [" + kernelLink.toString() + "] to pool");
		return kernelLink;
	}

	@Override
	public void destroyObject(PooledObject<KernelLink> p) throws Exception {
		KernelLink kernelLink = p.getObject();
		kernelLink.close();
		LOG.info("Destory the Kernel [" + kernelLink.toString() + "] out of pool");
		kernelLink = null;
	}

	@Override
	public void passivateObject(PooledObject<KernelLink> p) throws Exception {
		KernelLink kernelLink = p.getObject();
		kernelLink.evaluate("Clear[Evaluate[Context[] <> \"*\"]]");
		kernelLink.discardAnswer();
		LOG.info("Return the Kernel [" + kernelLink.toString() + "] back to pool");
	}

	@Override
	public void activateObject(PooledObject<KernelLink> p) throws Exception {
		KernelLink kernelLink = p.getObject();
		kernelLink.result().clear();
	}
	
	@Override
	public PooledObject<KernelLink> wrap(KernelLink obj) {
		return new DefaultPooledObject<KernelLink>(obj);
	}
}
