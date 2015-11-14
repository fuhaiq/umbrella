/*
 * Created on Jan 19, 2007
 *
 */
package com.wolfram.jlink.ui;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;


// The code in this class is a simple hack to help the ConsoleWindow class be as useful as possible.
// This is not part of the J/Link public API, and is accordingly not in the JavaDocs.
// It was written for one purpose--to accommodate Java 1.4 console logging, but it might have other
// uses. When Java logging initializes a ConsoleHandler from a properties file, it grabs the current
// System.err stream value, and that stream will forever be used, even if later someone calls System.setErr()
// on a different stream. That is precisely what the ConsoleWindow class does when it is initialized,
// and thus Java 1.4 logging will not display in the ConsoleWindow if the loggers are created before
// the ConsoleWindow is displayed. Java loggers are typically created statically at class-loading time,
// so it is easy for this to happen before the ConsoleWindow is constructed. When a user opens up
// the ConsoleWindow, they want to see logging output displayed from then on, as if the logging code
// was doing the sensible thing of writing to System.err every time instead of relying on the cached
// value.
//
// The workaround, as in so many programming problems, is to provide an extra level of indirection.
// We create a special OutputStream (ConsoleStream) that wraps another OutputStream and forwards
// all calls to this wrapped stream. We then assign this to System.err (and System.out) at J/Link
// startup. Now when Java logging caches System.err, it holds a reference to this wrapping ConsoleStream,
// and rather than changing the value of System.err we simply swap wrapped streams in and out
// to change the actual destination of output. The System.out and System.err ConsoleStream objects
// are singletons, so we can obtain references to them whenever we want to swap out their guts.
// The cached value of System.err held by the logging code changes its behavior on the fly.
//
// Things are slightly more complicated than described above, as there needs to be another wrapper
// class, ConsolePrintStream, which is a higher-level PrintStream and is the actual object assigned
// to System.out and System.err. This class has no implementation and exists only to server as a marker
// that the current value of System.out is based on a ConsoleStream. We wouldn't need it if PrintStream
// (which is always a wrapper for an OutputStream) had a getOutputStream() accessor.
//
// To behave nicely in this system, client code needs to call setSystemStdoutStream()/setSystemStderrStream()
// (defined below) and not System.setOut()/System.setErr(). Those two static methods are the only
// interface to this whole component. Everything else is just implementation details that are private
// to this file. If anyone calls, say, System.setOut() then that will blow away this whole system. This
// whole business is _not_ about preventing someone from blowing away a desired value by calling
// System.setOut(), but rather about making sure that if someone caches the value of System.out, that
// cached value will have its behavior changed on the fly as others call setSystemStdoutStream() to
// redirect output. There would be no need for any of this if no one ever cached the value of
// System.out/err, but as noted above Java 1.4 logging does exactly that.

public class ConsoleStream extends OutputStream {

    // These two methods are the public API. Callers should use them instead of System.setOut(), setErr().
    
    public static void setSystemStdoutStream(OutputStream strm) {
        System.setOut(new ConsolePrintStream(ConsoleStream.getStdoutStream().wrapStream(strm)));
    }

    public static void setSystemStderrStream(OutputStream strm) {
        System.setErr(new ConsolePrintStream(ConsoleStream.getStderrStream().wrapStream(strm)));
    }

    //// Package/Private stuff from here on down  ////
    
    // Just a simple PrintStream that acts as an identifier that we are using a ConsoleStream for its internals.
    static class ConsolePrintStream extends PrintStream {
        ConsolePrintStream(OutputStream strm) {super(strm);}
    }
    
    private OutputStream wrappedStream;
    private static ConsoleStream stdout, stderr; // The singleton instances, one for each System stream.
 
    private ConsoleStream() {
        wrappedStream = null;
    }
    
    static synchronized ConsoleStream getStdoutStream() {
        if (stdout == null)
            stdout = new ConsoleStream();
        return stdout;
    }
    
    static synchronized ConsoleStream getStderrStream() {
        if (stderr == null)
            stderr = new ConsoleStream();
        return stderr;
    }
    
    OutputStream getWrappedStream() {
        return wrappedStream;
    }
    
    private OutputStream wrapStream(OutputStream wrappedStream) {
        this.wrappedStream = wrappedStream;
        return this;
    }
    
    // OutputStream implementation by forwarding begins here.
    
    public void write(int b) throws IOException {
        if (wrappedStream != null)
            wrappedStream.write(b);
    }

    public void flush() throws IOException {
        if (wrappedStream != null)
            wrappedStream.flush();
    }
    
    public void close() throws IOException {
        if (wrappedStream != null)
            wrappedStream.close();
    }

}
