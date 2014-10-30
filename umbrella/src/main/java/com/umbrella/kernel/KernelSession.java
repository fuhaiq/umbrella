package com.umbrella.kernel;

import org.apache.commons.pool2.ObjectPool;

import com.google.inject.Inject;
import com.umbrella.session.AbstractSession;
import com.wolfram.jlink.KernelLink;

public class KernelSession extends AbstractSession<KernelLink> {

	@Inject
	private ObjectPool<KernelLink> pool;

	@Override
	protected KernelLink makeObject() throws Exception {
		return pool.borrowObject();
	}

	@Override
	protected void returnObject(KernelLink t) throws Exception {
		pool.returnObject(t);
	}
}