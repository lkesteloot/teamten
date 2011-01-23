// Copyright 2011 Lawrence Kesteloot

import lejos.nxt.Motor;
import lejos.nxt.remote.NXTCommand;
import lejos.nxt.SensorPort;
import lejos.nxt.TouchSensor;
import lejos.pc.comm.NXTComm;
import lejos.pc.comm.NXTCommLogListener;
import lejos.pc.comm.NXTConnector;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;

/**
 * Connects to a brick and presents a web page for remote control.
 */
public class NxtController {
    private static final boolean USE_USB = true;
    private static final int SERVER_SHUTDOWN_TIME_MS = 1000;

    public static void main(String[] args) throws Exception {
        new NxtController().run();
    }

    private void run() throws Exception {
        /// connectToBrick();
        /// runTest();
        runWebServer();
    }

    private void connectToBrick() {
        NXTConnector conn = new NXTConnector();
        conn.addLogListener(new NXTCommLogListener() {
            public void logEvent(String message) {
                System.out.println(message);				
            }

            public void logEvent(Throwable throwable) {
                System.err.println(throwable.getMessage());			
            }			
        });
        conn.setDebug(true);
        boolean success;
        if (USE_USB) {
            success = conn.connectTo("usb://");
        } else {
            success = conn.connectTo("btspp://NXT", NXTComm.LCP);
        }
        if (!success) {
            System.err.println("Failed to connect");
            System.exit(1);
        }
        NXTCommand.getSingleton().setNXTComm(conn.getNXTComm());
    }

    private void runWebServer() throws Exception {
        Server server = new Server(8080);

        // Bug 24: See http://docs.codehaus.org/display/JETTY/How+to+gracefully+shutdown
        server.setGracefulShutdown(SERVER_SHUTDOWN_TIME_MS);
        server.setStopAtShutdown(true);
        server.setHandler(getJettyHandler());

        server.start();
        System.out.println("Web server is running...");

        // Block until it quits.
        server.join();
    }

    private Handler getJettyHandler() {
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        Context context;

        context = new Context(contexts, "/");
        ServletHolder staticServletHolder = new ServletHolder(new DefaultServlet());
        staticServletHolder.setInitParameter("dirAllowed", "false");
        staticServletHolder.setInitParameter("gzip", "true");
        staticServletHolder.setInitParameter("resourceBase", "./static/");
        context.addServlet(staticServletHolder, "/*");

        context = new Context(contexts, "/api");
        context.addServlet(new ServletHolder(new ApiServlet()), "/*");

        return contexts;
    }

    private void runTest() throws InterruptedException {
        TouchSensor touch = new TouchSensor(SensorPort.S1);

        Motor.A.resetTachoCount();
        Motor.C.resetTachoCount();

        for (int i = 0; i < 10; i++) {
            System.out.println("Waiting...");
            while (!touch.isPressed()) {
                Thread.sleep(50);
            }
            Motor.A.forward();
            Motor.C.forward();
            System.out.println("Waiting to release...");
            while (touch.isPressed()) {
                Thread.sleep(50);
            }
            System.out.println("Stopping...");
            Motor.A.stop();
            Motor.C.stop();
        }
    }	
}
