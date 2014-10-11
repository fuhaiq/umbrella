package com.umbrella.kernel;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.NoSuchElementException;

import org.apache.commons.pool2.ObjectPool;

import com.google.inject.Inject;
import com.umbrella.session.AbstractSession;
import com.umbrella.session.SessionException;
import com.wolfram.jlink.KernelLink;

public class KernelSession extends AbstractSession<KernelLink> {

	@Override
	public void start() {
		KernelLink t = session.get();
		if (t != null)
			throw new SessionException.SessionAlreadyStartedException();
		try {
			t = checkNotNull(makeObject(), "The instance started by session is NULL");
		} catch (NoSuchElementException e) {
			throw e;
		} catch (Throwable e) {
			throw new SessionException.SessionStartException(e.getMessage(), e);
		}
		session.set(t);
	}

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