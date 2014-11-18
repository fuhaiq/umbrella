package com.umbrella.session;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractSession<T> implements Session<T> {

	protected ThreadLocal<T> session = new ThreadLocal<T>();

	@Override
	public void start() {
		T t = session.get();
		if (t != null)
			throw new SessionException.SessionAlreadyStartedException();
		try {
			t = checkNotNull(makeObject(), "The instance started by session is NULL");
		} catch (Throwable e) {
			throw new SessionException.SessionStartException(e.getMessage(), e);
		}
		session.set(t);
	}

	@Override
	public void close() {
		T t = session.get();
		if (t == null)
			throw new SessionException.SessionAlreadyClosedException();
		try {
			returnObject(t);
		} catch (Throwable e) {
			throw new SessionException.SessionCloseException(e.getMessage(), e);
		} finally {
			session.remove();
		}
	}

	@Override
	public T get() {
		T t = session.get();
		if (t == null) {
			throw new SessionException.SessionNotStartedException();
		}
		return t;
	}

	@Override
	public boolean isClosed() {
		return session.get() == null;
	}

	protected abstract T makeObject() throws Exception;

	protected abstract void returnObject(T t) throws Exception;
}
