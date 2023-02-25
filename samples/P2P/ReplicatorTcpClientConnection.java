//
// Copyright (c) 2020 Couchbase. All rights reserved.
// COUCHBASE CONFIDENTIAL - part of Couchbase Lite Enterprise Edition
//
import android.util.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Random;

// Websocket based client connection
public final class ReplicatorTcpClientConnection extends ReplicatorTcpConnection {
    private URI uri;

    public ReplicatorTcpClientConnection(URI uri) {
        super();
        if (uri == null)
            throw new IllegalArgumentException("uri cannot be null");
        this.uri = uri;
    }

    @Override
    protected boolean openConnection() throws Exception {
        return sendWebSocketRequest();
    }

    private boolean sendWebSocketRequest() throws IOException {
        String host = uri.getHost();
        int port = uri.getPort();
        setSocket(new Socket(host, port));

        if (port != 80) {
            host = host + ":" + port;
        }

        String key = generateWebSocketKey();
        String path = uri.getPath() + "/_blipsync";

        byte[] request = new StringBuilder().append("GET ").append(path).append(" HTTP/1.1").append("\r\n")
                .append("Sec-WebSocket-Version: 13").append("\r\n")
                .append("Sec-WebSocket-Protocol: BLIP_3+CBMobile_2").append("\r\n")
                .append("Sec-WebSocket-Key: ").append(key).append("\r\n")
                .append("Upgrade: websocket").append("\r\n")
                .append("Connection: Upgrade").append("\r\n")
                .append("Host: ").append(host).append("\r\n")
                .append("\r\n")
                .toString().getBytes(StandardCharsets.UTF_8);

        OutputStream out = getOutputStream();
        out.write(request);
        out.flush();

        byte[] buffer = new byte[RECEIVE_BUFFER_SIZE];
        InputStream in = getInputStream();
        int n = in.read(buffer);
        String data = new String(buffer, 0, n, StandardCharsets.UTF_8);

        if(!data.startsWith("HTTP/1.1 101 ")) {
            return false;
        }

        String connection = getHTTPHeader(data, "Connection");
        if(!"Upgrade".equalsIgnoreCase(connection)) {
            return false;
        }

        String upgrade = getHTTPHeader(data, "Upgrade");
        if(!"Websocket".equalsIgnoreCase(upgrade)) {
            return false;
        }

        String accept = getHTTPHeader(data, "Sec-WebSocket-Accept");
        if (!getWebSocketAcceptKey(key).equals(accept)) {
            return false;
        }
        return true;
    }

    private String generateWebSocketKey() {
        byte[] keyBytes = new byte[16];
        Random random = new Random();
        random.nextBytes(keyBytes);
        return Base64.encodeToString(keyBytes, Base64.NO_WRAP);
    }

}
