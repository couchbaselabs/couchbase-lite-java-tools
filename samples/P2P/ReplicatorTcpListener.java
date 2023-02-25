//
// Copyright (c) 2020 Couchbase. All rights reserved.
// COUCHBASE CONFIDENTIAL - part of Couchbase Lite Enterprise Edition
//
import com.couchbase.lite.Database;
import com.couchbase.lite.MessageEndpointListener;
import com.couchbase.lite.MessageEndpointListenerConfiguration;
import com.couchbase.lite.ProtocolType;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


// WebSocket based listener
public final class ReplicatorTcpListener {
    private static final int PORT = 59840;
    private final AtomicReference<ServerSocket> socketRef = new AtomicReference<>();
    private final MessageEndpointListener endpointListener;
    private Thread loopThread;
    private Database database;

    public ReplicatorTcpListener(Database database) {
        this.database = database;
        MessageEndpointListenerConfiguration config =
                new MessageEndpointListenerConfiguration(database, ProtocolType.BYTE_STREAM);
        this.endpointListener = new MessageEndpointListener(config);
    }
    
    public void start() throws IOException {
        if (!socketRef.compareAndSet(null, new ServerSocket(PORT)))
            return;
        
        loopThread = new Thread(new Runnable() {
            @Override
            public void run() { acceptLoop(); }
        });
        loopThread.start();
    }

    public void stop() {
        ServerSocket server = socketRef.getAndSet(null);
        if (server == null)
            return;

        try {
            endpointListener.closeAll();
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getURL() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface i : interfaces) {
                List<InetAddress> addresses = Collections.list(i.getInetAddresses());
                for (InetAddress a : addresses) {
                    if (!a.isLoopbackAddress() && a instanceof Inet4Address) {
                        return "ws://" + a.getHostAddress().toUpperCase() +
                                ":" + PORT + "/" + this.database.getName();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void acceptLoop() {
        while(true) {
            ServerSocket server = socketRef.get();
            if (server == null)
                break;

            Socket socket = null;
            try {
                socket = server.accept();
                ReplicatorTcpServerConnection connection =
                        new ReplicatorTcpServerConnection(socket);
                endpointListener.accept(connection);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
