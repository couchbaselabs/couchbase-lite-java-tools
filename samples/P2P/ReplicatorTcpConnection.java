//
// Copyright (c) 2020 Couchbase. All rights reserved.
// COUCHBASE CONFIDENTIAL - part of Couchbase Lite Enterprise Edition
//
import android.util.Base64;

import com.couchbase.lite.Message;
import com.couchbase.lite.MessageEndpointConnection;
import com.couchbase.lite.MessagingCloseCompletion;
import com.couchbase.lite.MessagingCompletion;
import com.couchbase.lite.MessagingError;
import com.couchbase.lite.ReplicatorConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Base ReplicatorTcpConnection that implements MessageEndpointConnection
public abstract class ReplicatorTcpConnection implements MessageEndpointConnection {
    protected static final int RECEIVE_BUFFER_SIZE = 8192;

    private Socket socket;
    // used from multiple threads; guarded by 'this'
    private InputStream inputStream;
    private OutputStream outputStream;
    // used from multiple threads; guarded by 'this'
    private ReplicatorConnection replicatorConnection;

    private Thread receiveThread;
    private boolean connected;


    protected synchronized InputStream getInputStream() { return inputStream; }

    protected synchronized OutputStream getOutputStream() { return outputStream; }

    // Called by subclasses to set the socket when ready:
    protected synchronized void setSocket(Socket socket) throws IOException {
        if (this.socket != null) {
            throw new IllegalStateException("Socket has already been set.");
        }
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
    }

    // Called by open(ReplicatorConnection, MessagingCompletion) for subclasses
    // to establish a connection to the remote peer:
    abstract boolean openConnection() throws Exception;

    // MessageEndpointConnection:

    @Override
    public void open(ReplicatorConnection connection, MessagingCompletion completion) {
        if(connected) {
            return;
        }

        MessagingError error = null;
        try {
            if (!openConnection()) {
                error = new MessagingError(new RuntimeException("Failed to open connection"), false);
            }
        } catch (Exception e) {
            error = new MessagingError(e, false);
        }

        completion.complete(error == null, error);

        if (error != null) {
            closeSocket();
            return;
        }

        connected = true;
        setReplicatorConnection(connection);
        receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                receiveLoop();
            }
        });
        receiveThread.start();
    }

    @Override
    public void close(Exception error, final MessagingCloseCompletion completion) {
        if(!connected) {
            return;
        }
        connected = false;
        closeSocket();
        completion.complete();
    }

    @Override
    public void send(Message message, MessagingCompletion completion) {
        byte[] bytes = message.toData();
        try {
            outputStream.write(bytes);
            completion.complete(true, null);
        } catch(IOException e) {
            completion.complete(false, new MessagingError(e, false));
        }
    }

    // Private Methods:

    private void closeSocket() {
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveLoop() {
        Exception error = null;
        InputStream in = getInputStream();
        ReplicatorConnection repCon = getReplicatorConnection();
        byte[] buffer = new byte[RECEIVE_BUFFER_SIZE];
        try {
            int length;
            while ((length = in.read(buffer)) > 0) {
                repCon.receive(Message.fromData(Arrays.copyOfRange(buffer, 0, length)));
            }
        } catch(Exception e) {
            if (!(e instanceof InterruptedException)) {
                error = e;
            }
        }
        repCon.close(error != null ? new MessagingError(error, false) : null);
    }

    // WebSocket utilities used by subclasses:

    protected String getHTTPHeader(String data, String key) {
        Matcher m = Pattern.compile(key + ": (.*)").matcher(data);
        if (m.find())
            return m.group(1);
        else
            return null;
    }

    protected String getWebSocketAcceptKey(String key) {
        String longKey = key.trim() + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch(NoSuchAlgorithmException e) {
            return null;
        }

        byte[] hashBytes = md.digest(longKey.getBytes(StandardCharsets.US_ASCII));
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP);
    }

    private synchronized ReplicatorConnection getReplicatorConnection() {
        return replicatorConnection;
    }

    private synchronized void setReplicatorConnection(ReplicatorConnection replicatorConnection) {
        this.replicatorConnection = replicatorConnection;
    }
}
