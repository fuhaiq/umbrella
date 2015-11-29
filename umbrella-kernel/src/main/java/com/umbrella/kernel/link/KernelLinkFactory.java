package com.umbrella.kernel.link;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.wolfram.jlink.KernelLink;
import com.wolfram.jlink.MathLinkFactory;
import com.wolfram.jlink.PacketListener;

@Component
public class KernelLinkFactory extends BasePooledObjectFactory<KernelLink> {
	
	private final Logger LOG = LoggerFactory.getLogger(KernelLinkFactory.class);

	@Autowired
	private PacketListener listener;
	
	@Autowired
	private KernelConfig config;
	
	@Override
	public KernelLink create() throws Exception {
		KernelLink kernelLink = MathLinkFactory.createKernelLink(config.getUrl());
		kernelLink.connect();
		kernelLink.discardAnswer();
		kernelLink.addPacketListener(listener);
		kernelLink.evaluate("Needs[\"Umbrella`\"]");
		kernelLink.discardAnswer();
		LOG.info("创建Mathematica内核 [" + kernelLink.toString() + "] 入池");
		return kernelLink;
	}

	@Override
	public void destroyObject(PooledObject<KernelLink> p) throws Exception {
		KernelLink kernelLink = p.getObject();
		kernelLink.close();
		LOG.info("销毁Mathematica内核 [" + kernelLink.toString() + "] 出池");
		kernelLink = null;
	}

	@Override
	public void passivateObject(PooledObject<KernelLink> p) throws Exception {
		KernelLink kernelLink = p.getObject();
		kernelLink.evaluate("Clear[Evaluate[Context[] <> \"*\"]]");
		kernelLink.discardAnswer();
		LOG.info("归还Mathematica内核 [" + kernelLink.toString() + "] 回池");
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
