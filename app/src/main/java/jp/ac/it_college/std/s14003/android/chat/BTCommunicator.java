package jp.ac.it_college.std.s14003.android.chat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.UUID;

public class BTCommunicator extends Thread {
    public static final int DISCONNECT = 99;
    public static final int DISPLAY_TOAST = 1000;
    public static final int STATE_CONNECTED = 1001;
    public static final int STATE_CONNECT_ERROR = 1002;
    public static final int STATE_RECEIVE_ERROR = 1004;
    public static final int STATE_SEND_ERROR = 1005;
    public static final int NO_DELAY = 0;
    public static final String OUI_LEGO = "00:16:53";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothAdapter btAdapter;
    private DataOutputStream Dos = null;
    private DataInputStream Din = null;
    private BluetoothSocket nxtBTSocket = null;
    private boolean connected = false;
    private Handler handler;
    private String mMacAddress;
    private MainActivity mainActivity;
    private final String TAG = "BTCommunicator";


    public BTCommunicator(MainActivity mainActivity, Handler myHandler, BluetoothAdapter defaultAdapter) {
        this.mainActivity = mainActivity;
        this.handler = myHandler;
        this.btAdapter = defaultAdapter;
    }

    public Handler getHandler() {
        return handler;
    }

    @Override
    public void run() {
        createConnection();
    }

    private void createConnection() {
        try {
            BluetoothSocket nxtBTSocketTEMPORARY;
            BluetoothDevice nxtDevice;
            nxtDevice = btAdapter.getRemoteDevice(mMacAddress);

            if (nxtDevice == null) {
                sendToast(mainActivity.getResources().getString(R.string.no_pairing_Nxt));
                sendState(STATE_CONNECT_ERROR);
                return;
            }

            nxtBTSocketTEMPORARY = nxtDevice.createRfcommSocketToServiceRecord(MY_UUID);
            nxtBTSocketTEMPORARY.connect();
            nxtBTSocket = nxtBTSocketTEMPORARY;

            Din = new DataInputStream(nxtBTSocket.getInputStream());
            Dos = new DataOutputStream(nxtBTSocket.getOutputStream());

            connected = true;

        } catch (IOException e) {
            Log.d("BTCommunicator", "error createNXTConnection()", e);
            if (mainActivity.newDevice) {
                sendToast(mainActivity.getResources().getString(R.string.pairing_message));
                sendState(STATE_CONNECT_ERROR);
            } else {
                Log.d(TAG, "run");
                sendState(STATE_CONNECT_ERROR);
            }
            return;
        }
        sendState(STATE_CONNECTED);
    }

    private void destoryNXTconnection() {
        try {
            if (nxtBTSocket != null) {
                connected = false;
                nxtBTSocket.close();
                nxtBTSocket = null;
            }
            Din = null;
            Dos = null;

        } catch (IOException e) {
            e.printStackTrace();
            sendToast(mainActivity.getResources().getString(R.string.problem_at_closing));
        }
    }

    private void sendToast(String toastText) {
        Bundle myBundle = new Bundle();
        myBundle.putInt("message", DISPLAY_TOAST);
        myBundle.putString("toastText", toastText);
        sendBundle(myBundle);
    }

    private void sendState(int message) {
        Bundle myBundle = new Bundle();
        myBundle.putInt("message", message);
        sendBundle(myBundle);
    }


    private void sendBundle(Bundle myBundle) {
        Message myMessage = handler.obtainMessage();
        myMessage.setData(myBundle);
        handler.sendMessage(myMessage);
    }

    public void setMacAddress(String mMacAddress) {
        this.mMacAddress = mMacAddress;
    }

    public static class MyHandler extends Handler {
        private final WeakReference<BTCommunicator> reference;

        public MyHandler(BTCommunicator communicator) {
            reference = new WeakReference<>(communicator);
        }

        @Override
        public void handleMessage(Message myMessage) {
            BTCommunicator communicator = reference.get();
            if (communicator == null) {
                return;
            }
            switch (myMessage.getData().getInt("message")) {
                case DISCONNECT:
                    communicator.destoryNXTconnection();
                    break;
            }

        }
    }


}
