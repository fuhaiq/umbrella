package com.umbrella.session;

public class SessionException extends RuntimeException {

	private static final long serialVersionUID = -3602621114778580646L;

	public SessionException() {
		super();
	}

	public SessionException(String message) {
		super(message);
	}

	public SessionException(Throwable cause) {
		super(cause);
	}

	public SessionException(String message, Throwable cause) {
		super(message, cause);
	}

	public static class SessionNotStartedException extends SessionException {

		private static final long serialVersionUID = 8454622077128866626L;

		public SessionNotStartedException() {
			super("Error:  Cannot start.  No managed session is started.");
		}
	}
	
	public static class SessionAlreadyStartedException extends SessionException {

		private static final long serialVersionUID = -8971649312301306630L;

		public SessionAlreadyStartedException() {
			super("Error:  Cannot start.  The managed session is already started.");
		}
	}
	
	public static class SessionAlreadyClosedException extends SessionException {

		private static final long serialVersionUID = -1910225112451257333L;

		public SessionAlreadyClosedException() {
			super("Error:  Cannot close.  The managed session is already closed.");
		}
	}
	
	public static class SessionStartException extends SessionException {

		private static final long serialVersionUID = -4277050718219917220L;

		public SessionStartException(Throwable e) {
			super(e);
		}
		public SessionStartException(String message, Throwable e) {
			super(message, e);
		}
	}
	
	public static class SessionCloseException extends SessionException {

		private static final long serialVersionUID = 7603403909235917960L;

		public SessionCloseException(Throwable e) {
			super(e);
		}
		public SessionCloseException(String message, Throwable e) {
			super(message, e);
		}
	}
}
