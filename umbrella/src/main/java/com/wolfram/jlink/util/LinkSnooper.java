package com.wolfram.jlink.util;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import com.wolfram.jlink.*;
import com.wolfram.jlink.ui.*;

/**
 * LinkSnooper is a program that sits between a front end and kernel (or any two MathLink programs)
 * and displays all traffic that moves across the link between them. It has a special feature that
 * allows it to also monitor the "service link" and "preemptive link" between a Mathematica version
 * 6.x notebook front end and kernel.
 * <p>
 * LinkSnooper uses the J/Link Console Window to display its output. This window is set to store a
 * very large number of lines of output in its scrollable buffer, and writing to the window can slow
 * down noticeably as the buffer fills with output. To speed things up, use the Clear button to
 * delete output you no longer care about. You can also temporarily stop capture by unchecking the
 * System.out radio button.
 * <p>
 * <b>Using LinkSnooper to monitor front end-kernel connections</b>
 * <p>
 * 1) Make sure JLink.jar is on the CLASSPATH. If you do not have it on
 *    the CLASSPATH, you can use a -cp option in the java command lines
 *    below, but beware of quoting issues.
 * <p>
 * 2) In the FrontEnd, go to the Kernel/Kernel Configuration Options dialog and Add a new connection.
 *    Call it LinkSnooper. Click the Advanced Options radio button and in the Arguments to MLOpen field,
 *    put something like this:
 * <pre>
 *   (Windows)
 *   -LinkMode Launch -LinkName "javaw com.wolfram.jlink.util.LinkSnooper -kernelname 'd:/math60/mathkernel'"
 *
 *   (Windows with a classpath spec)
 *   -LinkMode Launch -LinkName "javaw -cp \"c:/path/to/JLink.jar\" com.wolfram.jlink.util.LinkSnooper -kernelname 'd:/math60/mathkernel'"
 *
 *   (Unix)
 *   -LinkMode Launch -LinkName "java com.wolfram.jlink.util.LinkSnooper -kernelname 'math -mathlink'"
 *
 *   (Unix with a classpath spec)
 *   -LinkMode Launch -LinkName "java -cp /usr/local/Wolfram/Mathematica/6.0/SystemFiles/Links/JLink/JLink.jar com.wolfram.jlink.util.LinkSnooper -kernelname 'math -mathlink'"
 *
 *   (OS/X)
 *   -LinkMode Launch -LinkName "java com.wolfram.jlink.util.LinkSnooper -kernelname '/Applications/Mathematica\ 6.0.app/Contents/MacOS/MathKernel -mathlink'"
 *
 *   (OS/X with a classpath spec)
 *   -LinkMode Launch -LinkName "java -cp /Applications/Mathematica\ 6.0.app/SystemFiles/Links/JLink/JLink.jar com.wolfram.jlink.util.LinkSnooper -kernelname '/Applications/Mathematica\ 6.0.app/Contents/MacOS/MathKernel -mathlink'" </pre>
 *
 *   You can modify these according to your kernel path and whether you
 *   need to supply a full path to the java executable.
 * <p>
 *   If you need to specify other options for the kernel connection,
 *   use the following names in the same way that -kernelname is used in
 *   the examples above:
 * <pre>
 *      -kernelname  'path/to/kernel' (for a launch)
 *      -kernelmode  listen, connect, launch
 *      -kernelprot  SharedMemory, TCPIP, etc. </pre>
 *      -kernelhost  ip address, when using TCPIP (like the -LinkHost option). </pre>
 *      -kernelopts  extra info like MLDontInteract (like the -LinkOptions option). </pre>
 *
 *    The FrontEnd sends a lot of initialization traffic across the link after it launches the kernel.
 *    Printing this information can greatly slow the startup process unless you constantly click the
 *    Clear button to keep the window contents small. To avoid printing this initialization traffic,
 *    you can pass the command-line argument:
 * <pre>
 *      -noinit    (don't print any traffic until the first user input) </pre>
 *    LinkSnooper also supports the following command-line arguments that control its output:
 * <pre>
 *      -logfile filename  (write output to specified log file in addition to window)
 *      -nowindow          (don't display output window; useful with -logfile option)
 *      -timestamps        (include timestamps in output)
 * </pre>
 * 3) When the FrontEnd launches the kernel, you will see the J/Link Java console window appear.
 *    This is where the output goes. On some platforms, when you quit the kernel the console window
 *    will stay up until you close the console window, at which point Java will quit.
 * <p>
 * <p>
 * <b>Using LinkSnooper for other programs</b>
 * <p>
 *   Although LinkSnooper has special features for monitoring front end-to-kernel links, you can
 *   use it between any two MathLink programs. The basic idea is that when your program goes to
 *   launch the other MathLink program (e.g., a kernel), you launch LinkSnooper instead, passing
 *   it the information it needs to launch the target program itself. Thus, LinkSnooper sits
 *   between your program and the program it wants to communicate with.
 *   Because LinkSnooper is a standalone program, you can use it from any language, for example:
 *   <pre>
 *   // Java
 *   KernelLink ml = MathLinkFactory.createKernelLink("javaw com.wolfram.jlink.util.LinkSnooper -kernelname 'd:/math60/mathkernel.exe'");
 *
 *   // C
 *   MLINK ml = MLOpenString(env, "javaw com.wolfram.jlink.util.LinkSnooper -kernelname 'd:/math60/mathkernel.exe'", &err);
 *
 *   // C#  (.NET/Link)
 *   IKernelLink ml = MathLinkFactory.CreateKernelLink("javaw com.wolfram.jlink.util.LinkSnooper -kernelname 'd:/math60/mathkernel.exe'");  </pre>
 *
 *   You can also use Listen/Connect modes to manually establish a connection to an already-running kernel.
 *   You do this using the -kernelmode and -kernelprot options to LinkSnooper. Note that your program will
 *   <i>launch</i> LinkSnooper, and it is LinkSnooper that will do the Listen/Connect with the kernel.
 *   Here is an example of this:
 *   <pre>
 *   (* In Mathematica: *)
 *   In[1]:= $ParentLink = LinkCreate["1234"]  (* Open a LinkMode->Listen link *)
 *
 *   // In Java (or C, or C#, etc.)
 *   KernelLink ml = MathLinkFactory.createKernelLink("javaw com.wolfram.jlink.util.LinkSnooper -kernelname 1234 -kernelmode connect"); </pre>
 *
 *   You can also use command-line options to control how the names of the two
 *   sides of the link are represented in the output. By default, their names are FE and K,
 *   so each packet is prefixed with an arrow like this: FE ---> K (or the reverse direction).
 *   If you have LinkSnooper working on a link between programs for which the names FE and K
 *   are not appropriate, you can alter these names so the output is easier to read. By
 *   definition, the "FE side" is the program that launched LinkSnooper (or otherwise
 *   connected to it directly). The "kernel side" is the side that LinkSnooper connects to
 *   based on the -kernelname, -kernelmode, etc. options described above.
 * <pre>
 *      -feSide      Name    (name of FE-side program to print in output)
 *      -kernelSide  Name    (name of kernel-side program to print in output)
 * </pre>
 *
 */
public class LinkSnooper extends Thread {

    private PrintStream strm;  // If null, use System.out.
    private PrintStream logFileStrm;

    private KernelLink kernelMain = null, feMain = null;
    private KernelLink kernelService = null, feService = null;
    private KernelLink kernelPreemptive = null, fePreemptive = null;

    private String feSideName = "FE";
    private String kernelSideName = "K";
    private String toKernelArrow;
    private String toFEArrow;

    private String preemptiveLinkPrefix = "   -Pre-  ";
    private String serviceLinkPrefix = "      -Serv-  ";

    private boolean skipInitTraffic = false;
    private boolean doPrint = true;
    private boolean timestamps = false;
    private SimpleDateFormat timestampFormatter = new SimpleDateFormat("HH:mm:ss.SSS");

    private volatile boolean captureMain = true;
    private volatile boolean capturePre = true;
    private volatile boolean captureServ = true;

    private int pollInterval = 10; // Millis to wait in main loop

    private boolean useWindow = true;


    public static void main(String[] argv) throws MathLinkException {

        LinkSnooper snooper = new LinkSnooper(argv);
        snooper.start();
    }



    public LinkSnooper(String[] argv) throws MathLinkException {
        this(argv, null);
    }


    public LinkSnooper(String[] argv, PrintStream pstrm) throws MathLinkException {

        strm = pstrm;

        for (int i = 0; i < argv.length - 1; i++) {
            if (argv[i].equalsIgnoreCase("-logfile")) {
                try {
                    logFileStrm = new PrintStream(new FileOutputStream(argv[i+1]));
                } catch (Exception e) {
                    System.err.println("Could not open file " + argv[i] + " for writing.");
                }
            } else if (argv[i].equalsIgnoreCase("-timestamps")) {
                timestamps = true;
            } else if (argv[i].equalsIgnoreCase("-pollinterval")) {
                try {
                    pollInterval = Integer.parseInt(argv[i+1]);
                } catch (Exception e) {}
            } else if (argv[i].equalsIgnoreCase("-links")) {
                String links = argv[i+1].toLowerCase();
                captureMain = links.contains("main");
                capturePre = links.contains("pre");
                captureServ = links.contains("serv");
            }
        }

        // Only do ConsoleWindow if passed-in stream is null. You can always have
        // a logfile whether you pass in a stream or not, but you can only have one
        // of either the ConsoleWindow or the passed-in stream.
        if (strm == null) {
            for (int i = 0; i < argv.length; i++) {
                if (argv[i].equalsIgnoreCase("-nowindow"))
                    useWindow = false;
            }
            if (useWindow) {
                ConsoleWindow cw = ConsoleWindow.getInstance();
                cw.setMaxLines(15000);
                cw.setCapture(ConsoleWindow.STDOUT | ConsoleWindow.STDERR);
                cw.setTitle("LinkSnooper");
                cw.setSize(700, 600);
                cw.setVisible(true);
            }
        }

        output("LinkSnooper command-line params:");
        for (int i = 0; i < argv.length; i++) {
            output("   " + argv[i]);
        }

        feMain = MathLinkFactory.createKernelLink(argv);
        feMain.connect();
        // If possible, make Java look to the kernel like the fe program that launched Java.
        // This will typically set Java's env ID to "Mathematica Notebook Front End".
        if (feMain instanceof WrappedKernelLink) {
            MathLink ml = ((WrappedKernelLink) feMain).getMathLink();
            if (ml instanceof NativeLink) {
                String linkedEnvId = ((NativeLink) ml).getLinkedEnvID();
                NativeLink.setEnvID(linkedEnvId);
            }
        }

        // Now process args for "kernel" link.
        ArrayList kernelLinkArgs = new ArrayList();
        for (int i = 0; i < argv.length; i++) {
            if (argv[i].equalsIgnoreCase("-kernelname")) {
                kernelLinkArgs.add("-linkname");
                String kname = argv[i+1];
                // For windows users, it is convenient to wrap the -kernelname value in '' to cause the Java
                // command line to treat it like a single arg. But they must be stripped off before handing
                // the name to MathLink (on Windows, only " are appropriate path-enclosing chars).
                if (Utils.isWindows() && kname.startsWith("'") && kname.endsWith("'"))
                    kname = kname.substring(1, kname.length() - 1);
                kernelLinkArgs.add(kname);
            } else if (argv[i].equalsIgnoreCase("-kernelmode")) {
                kernelLinkArgs.add("-linkmode");
                kernelLinkArgs.add(argv[++i]);
            } else if (argv[i].equalsIgnoreCase("-kernelprot")) {
                kernelLinkArgs.add("-linkprotocol");
                kernelLinkArgs.add(argv[++i]);
            } else if (argv[i].equalsIgnoreCase("-kernelhost")) {
                kernelLinkArgs.add("-linkhost");
                kernelLinkArgs.add(argv[++i]);
            } else if (argv[i].equalsIgnoreCase("-kernelopts")) {
                kernelLinkArgs.add("-linkoptions");
                kernelLinkArgs.add(argv[++i]);
            } else if (argv[i].equalsIgnoreCase("-kernelside")) {
                kernelSideName = argv[++i];
            } else if (argv[i].equalsIgnoreCase("-feside")) {
                feSideName = argv[++i];
            } else if (argv[i].equalsIgnoreCase("-noinit")) {
                skipInitTraffic = true;
            }
        }
        // If no -kernelmode was specified, default to launch.
        if (!kernelLinkArgs.contains("-linkmode")) {
            kernelLinkArgs.add("-linkmode");
            kernelLinkArgs.add("launch");
        }

        kernelMain = MathLinkFactory.createKernelLink((String[]) kernelLinkArgs.toArray(new String[0]));
        kernelMain.connect();

        toKernelArrow = feSideName + " ---> " + kernelSideName + ": ";
        toFEArrow = feSideName + " <--- " + kernelSideName + ": ";

        feMain.addMessageHandler(LinkSnooper.class, this, "feMainMessageHandler");
        kernelMain.addMessageHandler(LinkSnooper.class, this, "kernelMainMessageHandler");
    }


    public void run() {

        if (feMain == null || kernelMain == null) {
            output("Broken Link. Cannot Relay.");
            return;
        }

        output("Start Monitoring...");

        doPrint = skipInitTraffic == false;

        Expr expr = null;
        while (true) {
            // Check links and relay incoming expressions.
            try {
                if (feMain.ready()) {
                    // from frontend to kernel on main
                    expr = feMain.peekExpr();
                    if (skipInitTraffic && !doPrint) {
                        // Turn on printing after first user input if -noinit flag was specified.
                        String head = expr.head().toString();
                        if (head.equals("EnterTextPacket") || head.equals("EnterExpressionPacket"))
                            doPrint = true;
                    }
                    if (captureMain)
                        output(toKernelArrow + expr.toString());
                    kernelMain.transferExpression(feMain);
                    kernelMain.flush();
                    expr.dispose();
                }
                if (kernelMain.ready()) {
                    // from kernel to frontend on main
                    expr = kernelMain.peekExpr();
                    String head = expr.head().toString();
                    String exprToPrint;
                    if (head.equals("CallPacket")) {
                        String func = expr.part(1).head().toString();
                        if (func.equals("FrontEnd`SetKernelSymbolContexts") ||
                               func.equals("FrontEnd`AddFunctionTemplateInformationToFunctions") ||
                                  func.equals("FrontEnd`SetFunctionInformation")) {
                            exprToPrint = "CallPacket[" + expr.part(1).head().toString() + "[ -- large contents not printed -- ]]";
                        } else {
                            exprToPrint = expr.toString();
                        }
                    } else {
                        exprToPrint = expr.toString();
                    }
                    if (captureMain)
                        output(toFEArrow + exprToPrint);
                    if (fePreemptive == null &&
                            head.equals("CallPacket") &&
                                    expr.part(1).head().toString().equals("FrontEnd`OpenParallelLinksPacket")) {
                        // Now need to read the expr off the link. Throw it away since we already peeked it as expr.
                        kernelMain.getExpr().dispose();
                        // Snoop looking for CallPacket[FrontEnd`OpenParallelLinksPacket[...]] coming from the kernel to the FE.
                        // We intercept that packet and interpose links of our own for the preemptive and service links.
                        // This code is a bit hackish and depends on implementation details in the FE and kernel that
                        // might change. For example, it depends on the structure of the FrontEnd`OpenParallelLinksPacket
                        // (assumes {serviceLinkName, preemptiveLinkName, protocol}), and it depends on details of how the
                        // FE handles that packet, such as that it connects the links in the order (service, preemptive).
                        output(" --- Opening special FE links (no further output will appear for this transaction, including the final ReturnPacket) --- ");
                        Expr links = expr.part(new int[]{1,1});
                        String serviceName = links.part(1).toString();
                        String preemptiveName = links.part(2).toString();
                        String protocol = links.part(3).toString();

                        String protString = protocol.equals("Automatic") ? "" : (" -linkprotocol " + protocol);

                        // These are the links back to the kernel that represent the kernel half of the service and preemptive links.
                        kernelService = MathLinkFactory.createKernelLink("-linkmode connect -linkname " + serviceName + protString);
                        kernelPreemptive = MathLinkFactory.createKernelLink("-linkmode connect -linkname " + preemptiveName + protString);

                        kernelService.addMessageHandler(LinkSnooper.class, this, "kernelServiceMessageHandler");
                        kernelPreemptive.addMessageHandler(LinkSnooper.class, this, "kernelPreemptiveMessageHandler");

                        // Tell the kernel to connect the links and then connect our side.
                        kernelMain.putFunction("EvaluatePacket", 1);
                        kernelMain.putFunction("LinkConnect", 1);
                        kernelMain.putSymbol("MathLink`$ServiceLink");
                        kernelMain.flush();
                        kernelService.connect();
                        kernelMain.discardAnswer();

                        kernelMain.putFunction("EvaluatePacket", 1);
                        kernelMain.putFunction("LinkConnect", 1);
                        kernelMain.putSymbol("MathLink`$PreemptiveLink");
                        kernelMain.flush();
                        kernelPreemptive.connect();
                        kernelMain.discardAnswer();

                        // This fakes the result from the FE back to the kernel for the CallPacket[FrontEnd`OpenParallelLinksPacket[...]].
                        kernelMain.putFunction("ReturnPacket", 1);
                        kernelMain.putSymbol("Null");
                        kernelMain.flush();

                        // These are the links to the fe that represent the fe half of the service and preemptive links.
                        feService = MathLinkFactory.createKernelLink("-linkmode listen " + protString + " -linkoptions MLDontInteract");
                        fePreemptive = MathLinkFactory.createKernelLink("-linkmode listen " + protString + " -linkoptions MLDontInteract");

                        feService.addMessageHandler(LinkSnooper.class, this, "feServiceMessageHandler");
                        fePreemptive.addMessageHandler(LinkSnooper.class, this, "fePreemptiveMessageHandler");

                        // Now we have to compose a new FrontEnd`OpenParallelLinksPacket to send to the fe with the links we have created.
                        expr = new Expr(new Expr(Expr.SYMBOL, "List"), new Expr[]{new Expr(feService.name()), new Expr(fePreemptive.name()), links.part(3)});
                        expr = new Expr(new Expr(Expr.SYMBOL, "FrontEnd`OpenParallelLinksPacket"), new Expr[]{expr});
                        expr = new Expr(new Expr(Expr.SYMBOL, "CallPacket"), new Expr[]{expr});

                        feMain.put(expr);
                        feMain.flush();
                        expr.dispose();

                        feService.connect();
                        // Handle the EvaluatePacket the fe sends that calls LinkConnect on the service link.
                        feMain.nextPacket();
                        feMain.newPacket();
                        feMain.putFunction("ReturnPacket", 1);
                        feMain.putSymbol("Null");
                        feMain.flush();

                        fePreemptive.connect();
                        // Handle the EvaluatePacket the fe sends that calls LinkConnect on the preemptive link.
                        feMain.nextPacket();
                        feMain.newPacket();
                        feMain.putFunction("ReturnPacket", 1);
                        feMain.putSymbol("Null");

                        // This throws away the ReturnPacket coming from the fe as the last thing from the faked
                        // CallPacket[FrontEnd`OpenParallelLinksPacket[...]] we sent it.
                        feMain.discardAnswer();

                        addLinkCheckboxes();
                   } else {
                        // Normal kernel -> fe result from computation, or a CallPacket.
                        feMain.transferExpression(kernelMain);
                        feMain.flush();
                        expr.dispose();
                    }
                }
                if (fePreemptive != null && fePreemptive.ready()) {
                    // from frontend to kernel on preemptive
                    if (capturePre) {
                        expr = fePreemptive.peekExpr();
                        output(preemptiveLinkPrefix + toKernelArrow + expr.toString());
                        expr.dispose();
                    }
                    kernelPreemptive.transferExpression(fePreemptive);
                    kernelPreemptive.flush();
                }
                if (kernelPreemptive != null && kernelPreemptive.ready()) {
                    // from kernel to frontend on preemptive
                    if (capturePre) {
                        expr = kernelPreemptive.peekExpr();
                        output(preemptiveLinkPrefix + toFEArrow + expr.toString());
                        expr.dispose();
                    }
                    fePreemptive.transferExpression(kernelPreemptive);
                    fePreemptive.flush();
                }
                if (feService != null && feService.ready()) {
                    // from frontend to kernel on service
                    if (captureServ) {
                        expr = feService.peekExpr();
                        output(serviceLinkPrefix + toKernelArrow + expr.toString());
                        expr.dispose();
                    }
                    kernelService.transferExpression(feService);
                    kernelService.flush();
                }
                if (kernelService != null && kernelService.ready()) {
                    // from kernel to frontend on service
                    if (captureServ) {
                        expr = kernelService.peekExpr();
                        output(serviceLinkPrefix + toFEArrow + expr.toString());
                        expr.dispose();
                    }
                    feService.transferExpression(kernelService);
                    feService.flush();
                }

                try { Thread.sleep(pollInterval); } catch (InterruptedException err1) { }

            } catch (MathLinkException err) {
                int errCode = err.getErrCode();
                output("MathLinkException: Code " + errCode + " : " + err.getMessage());
                // Too bad the exception doesn't have some sort of link ID in it, which would
                // simplify the determination of which link.
                if (errCode == kernelMain.error())
                    output("Exception was from the " + kernelSideName + " side.");
                else if (errCode == feMain.error())
                    output("Exception was from the " + feSideName + " side.");
                else if (kernelPreemptive != null && errCode == kernelPreemptive.error())
                    output("Exception was from the " + kernelSideName + " side, on the Preemptive link.");
                else if (fePreemptive != null && errCode == fePreemptive.error())
                    output("Exception was from the " + feSideName + " side, on the Preemptive link.");
                else if (kernelService != null && errCode == kernelService.error())
                    output("Exception was from the " + kernelSideName + " side, on the Service link.");
                else if (feService != null && errCode == feService.error())
                    output("Exception was from the " + feSideName + " side, on the Service link.");
                if (logFileStrm != null)
                    logFileStrm.close();
                feMain.close();
                kernelMain.close();
                if (kernelService != null) kernelService.close();
                if (feService != null) feService.close();
                if (kernelPreemptive != null) kernelPreemptive.close();
                if (fePreemptive != null) fePreemptive.close();
                return;
            }
        }
    }


    private void output(String s) {
        if (doPrint) {
            String outputLine = timestamps ? "[" + timestampFormatter.format(new Date()) + "] " + s : s;
            PrintStream p = strm != null ? strm : System.out;
            p.println(outputLine);
            if (logFileStrm != null)
                logFileStrm.println(outputLine);
        }
    }


    private void addLinkCheckboxes() {

        if (useWindow) {
            ConsoleWindow cw = ConsoleWindow.getInstance();
            JPanel p = new JPanel();
            p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.black));
            Checkbox mainBox = new Checkbox("Main", captureMain);
            Checkbox preBox = new Checkbox("Preemptive", capturePre);
            Checkbox servBox = new Checkbox("Service", captureServ);
            p.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 4));
            p.add(new Label(""));
            p.add(new Label("Monitor Front End links:"));
            p.add(mainBox);
            p.add(preBox);
            p.add(servBox);
            mainBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    Checkbox b = (Checkbox) e.getItemSelectable();
                    captureMain = b.getState();
                }
            });
            preBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    Checkbox b = (Checkbox) e.getItemSelectable();
                    capturePre = b.getState();
                }
            });
            servBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    Checkbox b = (Checkbox) e.getItemSelectable();
                    captureServ = b.getState();
                }
            });

            GridBagLayout gbl = (GridBagLayout) cw.getContentPane().getLayout();
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 0;
            gbc.gridy = GridBagConstraints.RELATIVE;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weighty = 0.0;

            gbl.setConstraints(p, gbc);
            cw.getContentPane().add(p);
            cw.setSize(700, 610);
        }
    }


    // Message handlers forward messages "across the gap".

    public void feMainMessageHandler(int msgType, int ignore) throws MathLinkException {
        output("****** Message " + toKernelArrow + " on Main: " + msgType);
        kernelMain.putMessage(msgType);
    }
    public void kernelMainMessageHandler(int msgType, int ignore) throws MathLinkException {
        output("****** Message " + toFEArrow + " on Main: " + msgType);
        feMain.putMessage(msgType);
    }
    public void fePreemptiveMessageHandler(int msgType, int ignore) throws MathLinkException {
        output(preemptiveLinkPrefix + "****** Message " + toKernelArrow + " on Preemptive: " + msgType);
        kernelPreemptive.putMessage(msgType);
    }
    public void kernelPreemptiveMessageHandler(int msgType, int ignore) throws MathLinkException {
        output(preemptiveLinkPrefix + "****** Message " + toFEArrow + " on Preemptive: " + msgType);
        fePreemptive.putMessage(msgType);
    }
    public void feServiceMessageHandler(int msgType, int ignore) throws MathLinkException {
        output(serviceLinkPrefix + "****** Message " + toKernelArrow + " on Service: " + msgType);
        kernelService.putMessage(msgType);
    }
    public void kernelServiceMessageHandler(int msgType, int ignore) throws MathLinkException {
        output(serviceLinkPrefix + "****** Message " + toFEArrow + " on Service: " + msgType);
        feService.putMessage(msgType);
    }

}