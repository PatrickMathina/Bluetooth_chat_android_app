package com.pat254.BluChat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ChatManager {

    // Member fields
    private final Context context;
    private final Handler handler;
    private final BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;
    private int State;

    // Unique UUID for this application
    private static final UUID APP_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66"); // fa87c0d0-afac-11de-8a39-0800200c9a66 //31cc6afa-e582-4a07-affa-27dde939cdb// ?001101-0000-1000-8000-00805F9B34FB

    // Name when creating server socket
    private static final String APP_NAME = "BluChat";

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0; // doing nothing
    public static final int STATE_LISTEN = 1; // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3; // now connected to a remote device

    public ChatManager(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;

        State = STATE_NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // Initializing bluetoothAdapter
    }

    public synchronized void setState(int state) {
        this.State = state;
        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGED, state, -1).sendToTarget(); // Give the new state to the Handler so the UI Activity can update
    }

    // Cancel any thread attempting to make a connection
    public synchronized void start() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
    }

    public synchronized void connect(BluetoothDevice device) {

        // Cancel any thread attempting to make a connection
        if (State == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Start the thread to connect with the given device
        connectThread = new ConnectThread(device);
        connectThread.start();

        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {

        // Cancel the thread that completed the connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message message = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setState(STATE_CONNECTED);
    }


    //STOP ALL THREADS

    public synchronized void stop() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }
        setState(STATE_NONE);
    }


    // Write to the ConnectedThread in an unsynchronized manner

    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (State != STATE_CONNECTED)
                return;
            r = connectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    // Indicate that the connection attempt failed and notify the UI Activity.
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Connection failed.");
        message.setData(bundle);
        handler.sendMessage(message);

        // Start the service over to restart listening mode
        ChatManager.this.start();
    }

    // Indicate that the connection was lost and notify the UI Activity.
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Connection Lost");
        message.setData(bundle);
        handler.sendMessage(message);

        // Start the service over to restart listening mode
        ChatManager.this.start();
    }


    // This thread runs while listening for incoming connections. It behaves
    // like a server-side client. It runs until a connection is accepted (or
    // until cancelled).

    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
            } catch (IOException e) {
                Log.e("Accept-> Constructor", e.toString());
            }
            serverSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket;

            // Listen to the server socket if we're not connected
            while (State != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = serverSocket.accept();
                } catch (IOException e) {
//                    Log.e("Accept-> Constructor", e.toString());
                    Log.e("Accept-> Run", e.toString());
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (ChatManager.this) {
                        switch (State) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate
                                // new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
//                                    Log.e("Accept-> CloseServer", e.toString());
                                    Log.e("Accept-> CloseSocket", e.toString());
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e("Accept-> CloseServer", e.toString());
            }
        }
    }

    // This thread runs while attempting to make an outgoing connection with a
    // device. It runs straight through; the connection either succeeds or
    // fails.

    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e("Connect->Constructor", e.toString());
            }
            socket = tmp;
        }

        public void run() {

            // Always cancel discovery because it will slow down a connection
            bluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                socket.connect();
            } catch (IOException e) {
                // Close the socket
//                Log.e("Connect->Run", e.toString());
                try {
                    socket.close();
                } catch (IOException e1) {
                    Log.e("Connect->CloseSocket", e.toString());
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (ChatManager.this) {
                connectThread = null;
            }

            // Start the connected thread
            connected(socket, device);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("Connect->Cancel", e.toString());
            }
        }
    }

    // This thread runs during a connection with a remote device. It handles all
    // incoming and outgoing transmissions.

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
      /*
       Log.e(TAG, "temp sockets not created", e);
       Log.e("Connect->Cancel", e.toString());
      */
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = inputStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    handler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    connectionLost();
                    // Start the service over to restart listening mode
                    ChatManager.this.start();
                    break;
                }
            }
        }

        // Write to the connected OutStream.
        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
                handler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//
//        // Performing this check in onResume() covers the case in which BT was
//        // not enabled during onStart(), so we were paused to enable it...
//        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
//        if (chatManager != null) {
//            // Only if the state is STATE_NONE, do we know that we haven't started already
//            if (chatManager.getState() == ChatManager.STATE_NONE) {
//                // Start the Bluetooth chat services
//                ChatManager.this.start();
//            }
//
//        }
//    }
}

