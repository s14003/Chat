package jp.ac.it_college.std.s14003.android.chat;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final int MENU_TOGGLE_CONNECT = Menu.FIRST;
    public static final int MENU_QUIT = Menu.FIRST + 1;
    private static final int REQUEST_CONNECT_DEVICE = 1000;
    private static final int REQUEST_ENABLE_BT = 2000;
    final Handler myHandler = new UiHandler(this);
    boolean newDevice;
    private ProgressDialog connectingProgressDialog;
    private boolean connected = false;
    private boolean bt_error_pending = false;
    private BTCommunicator myBTCommunicator = null;
    private Handler btcHandler;
    private Toast mLongToast;
    private Toast mShortToast;
    private final String TAG = ".MainActivity";

    private Menu myMenu;

    static class UiHandler extends Handler {
        private final WeakReference<MainActivity> reference;

        public UiHandler(MainActivity activity) {
            super();
            this.reference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = this.reference.get();
            if (activity == null) {
                return;
            }
            switch (msg.getData().getInt("message")) {
                case BTCommunicator.STATE_CONNECTED:
                    activity.connected = true;
                    activity.connectingProgressDialog.dismiss();

                    activity.updateButtonsAndMenu();
                    activity.showToastLong(activity.getResources().getString(R.string.connect));
                    break;
                case BTCommunicator.STATE_CONNECT_ERROR:
                    activity.connectingProgressDialog.dismiss();
                    break;
                case BTCommunicator.STATE_RECEIVE_ERROR:
                case BTCommunicator.STATE_SEND_ERROR:
                    activity.destroyCommunicator();

                    if (!activity.bt_error_pending) {
                        activity.bt_error_pending = true;
                        DialogFragment newFragment =
                                MyAlertDialogFragment.newInstance(R.string.bt_error_dialog_title,
                                        R.string.bt_error_dialog_message);
                        newFragment.show(activity.getFragmentManager(), "dialog");
                    }

                    break;

            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mLongToast = Toast.makeText(this, "", Toast.LENGTH_LONG);
        mShortToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        findViewById(R.id.send_button).setOnClickListener(MainActivity.this);
    }
    @Override
    protected void onStart() {
        super.onStart();
        if (!(BluetoothAdapter.getDefaultAdapter() == null)) {
            Log.v("Bluetooth", "Bluetooth is Supported");
        } else {
            Log.v("Bluetooth", "Bluetooth isn't Supported");
            finish();
        }
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            showToastStart(getResources().getString(R.string.wait_till_bt_on));
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            selectChat();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:

                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    newDevice = data.getExtras().getBoolean(DeviceListActivity.PAIRING);
                    if (newDevice) {
                        Log.d(TAG,"run_OK");
                        enDisCoverable();
                    }
                    startCommunicator(address);
                }
                break;
            case REQUEST_ENABLE_BT:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        selectChat();
                        break;
                    case Activity.RESULT_CANCELED:
                        finish();
                        break;
                    default:
                        showToastShort(getResources().getString(R.string.problem_at_connecting));
                        finish();
                        break;
                }
        }
    }

    public void startCommunicator(String mac_address) {
        connectingProgressDialog = ProgressDialog.show(this, "", getResources()
                .getString(R.string.connecting_please_wait), true);
        if (myBTCommunicator == null) {

            createCommunicator();
        }
        switch ((myBTCommunicator).getState()) {
            case NEW:
                Log.d(TAG,"NEW_Run");
                myBTCommunicator.setMacAddress(mac_address);
                myBTCommunicator.start();
                break;
            default:
                connected = false;
                myBTCommunicator = null;
                createCommunicator();
                myBTCommunicator.setMacAddress(mac_address);
                myBTCommunicator.start();
                break;
        }
        updateButtonsAndMenu();
    }

    public void doPositiveClick() {
        bt_error_pending = false;
        selectChat();
    }

    public void createCommunicator() {
        myBTCommunicator = new BTCommunicator(this, myHandler, BluetoothAdapter.getDefaultAdapter());
        btcHandler = myBTCommunicator.getHandler();
    }

    private void enDisCoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
        startActivity(discoverableIntent);
    }

    public void destroyCommunicator() {
        if (myBTCommunicator != null) {
            sendMessage(BTCommunicator.NO_DELAY, BTCommunicator.DISCONNECT);
            myBTCommunicator = null;
        }
        connected = false;
    }

    public void sendMessage(int delay, int message) {
        Bundle myBundle = new Bundle();
        myBundle.putInt("message", message);
        Message myMessage = myHandler.obtainMessage();
        myMessage.setData(myBundle);

        if (delay == 0) {
            btcHandler.sendMessage(myMessage);
        } else {
            btcHandler.sendMessageDelayed(myMessage, delay);
        }
    }

    private void selectChat() {
        Intent server = new Intent(this, DeviceListActivity.class);
        startActivityForResult(server, REQUEST_CONNECT_DEVICE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        myMenu = menu;
        //MenuItem actionItem = myMenu.add(
          //      0, MENU_TOGGLE_CONNECT, 1, getResources().getString(R.string.connect));
        myMenu.add(0, MENU_QUIT, 2, getResources().getString(R.string.quit));

        updateButtonsAndMenu();
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        destroyCommunicator();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_TOGGLE_CONNECT:
                if (myBTCommunicator == null || !connected) {
                    selectChat();
                } else {
                    destroyCommunicator();
                    updateButtonsAndMenu();
                }
                return true;
            case MENU_QUIT:
                destroyCommunicator();
                finish();
                return true;
        }
        return false;
    }

    private void updateButtonsAndMenu() {
        if (myMenu == null) {
            return;
        }
        myMenu.removeItem(MENU_TOGGLE_CONNECT);
        if (connected) {
            myMenu.add(0, MENU_TOGGLE_CONNECT, 1, getResources().getString(R.string.disconnect));
        } else {
            myMenu.add(0, MENU_TOGGLE_CONNECT, 1, getResources().getString(R.string.connect));
        }
    }

    @Override
    public void onClick(View v) {
        EditText comment = (EditText) findViewById(R.id.comment);
        TextView comment_view = (TextView) findViewById(R.id.Comment);
        String string = comment.getText().toString();
        comment_view.setText(string);

    }

    private void showToastShort(String textToShow) {
        mShortToast.setText(textToShow);
        mShortToast.show();
    }

    private void showToastLong(String textToShow) {
        mLongToast.setText(textToShow);

    }
    private void showToastStart(String textToShow) {
        mShortToast.setText(textToShow);
        mShortToast.show();
    }
}
