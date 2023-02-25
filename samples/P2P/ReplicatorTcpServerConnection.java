//
// Copyright (c) 2020 Couchbase. All rights reserved.
// COUCHBASE CONFIDENTIAL - part of Couchbase Lite Enterprise Edition
//
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

// Websocket based server connection
public final class ReplicatorTcpServerConnection extends ReplicatorTcpConnection {
    protected static final int RECEIVE_BUFFER_SIZE = 8192;

    private Socket client;

    public ReplicatorTcpServerConnection(Socket client) throws IOException {
        this.client = client;
    }

    @Override
    boolean openConnection() throws Exception {
        return performWebSocketHandshake();
    }

    private boolean performWebSocketHandshake() throws IOException {
        setSocket(this.client);

        byte[] buffer = new byte[RECEIVE_BUFFER_SIZE];
        InputStream in = getInputStream();
        int n = in.read(buffer);
        String data = new String(buffer, 0, n, StandardCharsets.UTF_8);
        if(data.startsWith("GET ")) {
            String key = getHTTPHeader(data, "Sec-WebSocket-Key");
            if(key == null) {
                return false;
            }

            String protocol = getHTTPHeader(data, "Sec-WebSocket-Protocol");
            if(protocol == null) {
                return false;
            }

            String version = getHTTPHeader(data, "Sec-WebSocket-Version");
            if(version == null) {
                return false;
            }

            String accept = getWebSocketAcceptKey(key);
            if(accept == null) {
                return false;
            }

            byte[] response = new StringBuilder()
                    .append("HTTP/1.1 101 Switching Protocols").append("\r\n")
                    .append("Connection: Upgrade").append("\r\n")
                    .append("Upgrade: Websocket").append("\r\n")
                    .append("Sec-WebSocket-Version: ").append(version.trim()).append("\r\n")
                    .append("Sec-WebSocket-Protocol: ").append(protocol.trim()).append("\r\n")
                    .append("Sec-WebSocket-Accept: ").append(accept).append("\r\n")
                    .append("\r\n")
                    .toString().getBytes(StandardCharsets.UTF_8);
            OutputStream out = getOutputStream();
            out.write(response);
            out.flush();
            return true;
        }

        return false;
    }
}
