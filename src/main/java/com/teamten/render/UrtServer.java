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

// Copyright 2012 Lawrence Kesteloot

package com.teamten.render;

import com.teamten.image.ImageUtils;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * Serves ray-tracing for the URT (universal ray-tracer) infrastructure.
 */
public class UrtServer {
    public static void main(String[] args) throws IOException {
        int port = 12345;

        // Don't log from image library.
        ImageUtils.PRINT_LOG = false;

        if (args.length == 2 && "-port".equals(args[0])) {
            port = Integer.parseInt(args[1]);
        }

        new UrtServer().startServing(port);
    }

    private void startServing(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);

        // Accept connections.
        System.out.printf("Waiting for connection on port %d...%n", port);
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Got connection from " + socket.getRemoteSocketAddress());

            // Start thread to handle the connection.
            new UrtConnection(socket).start();
        }
    }
}

