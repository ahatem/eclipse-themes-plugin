package com.github.eclipsethemes.core.parser;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/** Bounded monitor that answers immediately, so a security regression cannot hang on HTTP input. */
final class LoopbackCanary implements AutoCloseable {
    final AtomicInteger connections = new AtomicInteger();
    private final ServerSocket server = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"));
    private final Thread listener;
    private volatile boolean running = true;
    private volatile IOException failure;

    LoopbackCanary() throws IOException {
        server.setSoTimeout(100);
        listener = new Thread(() -> {
            while (true) {
                try (var socket = server.accept()) {
                    connections.incrementAndGet();
                    byte[] body = "<background color='#13579b'/>".getBytes(StandardCharsets.UTF_8);
                    socket.getOutputStream().write(("HTTP/1.0 200 OK\r\nContent-Length: " + body.length
                            + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().write(body);
                } catch (SocketTimeoutException expected) {
                    // Drain queued connections even if finish preceded thread scheduling.
                    if (!running) break;
                } catch (IOException e) {
                    if (!running) break;
                    failure = e;
                }
            }
        }, "xml-loopback-canary");
        listener.setDaemon(true);
        listener.start();
    }

    String url() { return "http://127.0.0.1:" + server.getLocalPort() + "/canary"; }

    void finish() throws Exception {
        running = false;
        listener.join(1000);
        if (listener.isAlive()) throw new AssertionError("Loopback listener did not terminate");
        if (failure != null) throw failure;
    }

    @Override public void close() throws IOException {
        running = false;
        server.close();
        try { listener.join(1000); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException(e); }
        if (listener.isAlive()) throw new IOException("Loopback listener did not terminate");
    }
}
