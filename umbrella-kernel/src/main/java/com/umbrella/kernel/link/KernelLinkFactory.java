package com.umbrella.kernel.link;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.wolfram.jlink.JLinkClassLoader;
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
		kernelLink.setClassLoader(new JLinkClassLoader(KernelLink.class.getClassLoader()));
		kernelLink.enableObjectReferences();
		kernelLink.evaluate("Needs[\"Umbrella`\"]");
		kernelLink.discardAnswer();
		LOG.info("Create Mathematica kernel [" + kernelLink.toString() + "] into Pool");
		return kernelLink;
	}

	@Override
	public void destroyObject(PooledObject<KernelLink> p) throws Exception {
		KernelLink kernelLink = p.getObject();
		kernelLink.close();
		LOG.info("Destroy Mathematica kernel [" + kernelLink.toString() + "] out of Pool");
		kernelLink = null;
	}

	@Override
	public void passivateObject(PooledObject<KernelLink> p) throws Exception {
		KernelLink kernelLink = p.getObject();
		kernelLink.evaluate("Clear[Evaluate[Context[] <> \"*\"]]");
		kernelLink.discardAnswer();
		LOG.info("Return Mathematica kernel [" + kernelLink.toString() + "] back to Pool");
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
