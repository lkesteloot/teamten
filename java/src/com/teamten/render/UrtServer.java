// Copyright 2012 Lawrence Kesteloot

package com.teamten.render;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Serves ray-tracing for the URT (universal ray-tracer) infrastructure.
 */
public class UrtServer {
    public static void main(String[] args) throws IOException {
        int port = 12345;

        if (args.length == 2 && "-port".equals(args[0])) {
            port = Integer.parseInt(args[1]);
        }

        new UrtServer().startServing(port);
    }

    private void startServing(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);

        // Accept connections.
        System.out.println("Waiting for connection...");
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Got connection from " + socket.getRemoteSocketAddress());

            // Start thread to handle the connection.
            new UrtConnection(socket).start();
        }
    }
}

