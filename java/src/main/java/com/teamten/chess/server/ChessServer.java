// Copyright 2011 Lawrence Kesteloot

package com.teamten.chess.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.server.nio.SelectChannelConnector;

/**
 * Provides an HTTP chess server for interactive play.
 */
public class ChessServer {
    private static final int PORT = 8080;

    public void start() {
        // Create Jetty server.
        Server server = new Server();

        // Connectors.
        SelectChannelConnector nioConnector = new SelectChannelConnector();
        nioConnector.setPort(PORT);
        server.addConnector(nioConnector);

        // Contexts.
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        ServletContextHandler context;

        context = new ServletContextHandler(contexts, "/chess");
        ChessServlet servlet = new ChessServlet();
        context.addServlet(new ServletHolder(servlet), "/*");

        server.setHandler(contexts);

        try {
            server.start();
        } catch (Exception e) {
            System.err.println("Problem starting chess server.");
            e.printStackTrace();
        }
    }
}
