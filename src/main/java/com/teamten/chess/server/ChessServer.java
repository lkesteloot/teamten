/*
 *
 *    Copyright 2016 Lawrence Kesteloot
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

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
