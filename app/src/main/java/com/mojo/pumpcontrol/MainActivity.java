package com.mojo.pumpcontrol;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {

    // State machine
    final private static int STATE_BLUETOOTH_OFF = 1;
    final private static int STATE_DISCONNECTED = 2;
    final private static int STATE_CONNECTING = 3;
    final private static int STATE_CONNECTED = 4;

    private int state;

    private android.view.ViewGroup dataLayout;
    private boolean scanStarted;
    private CountDownTimer timer;
    private boolean scanning;
    private static RFduinoService rfduinoService;
    private TextView connectionStatusText;
    private TextView surgeryTimer;
    private Button startButton;
    private Button stopButton;
    private Button testButton;
    private Button connectButton;
    private Long seconds = (long) 0;
    private Long minutes = (long) 0;
    private String startOn = "1";
    private String startOff = "0";
    private List<ScanFilter> filterList = new ArrayList<ScanFilter>();
    private boolean connected = false;
    private boolean running = false;

    private final static String TAG = MainActivity.class.getSimpleName();
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);

            if (state == BluetoothAdapter.STATE_CONNECTED) {
                updateState(STATE_CONNECTED);
            } else if (state == BluetoothAdapter.STATE_CONNECTING) {
                updateState(STATE_CONNECTING);
            } else if (state == BluetoothAdapter.STATE_DISCONNECTED) {
                updateState(STATE_DISCONNECTED);
            } else if (state == BluetoothAdapter.STATE_OFF) {
                updateState(STATE_BLUETOOTH_OFF);
            }
        }
    };

    private final ServiceConnection rfduinoServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // set the local service variable to the running service
            rfduinoService = ((RFduinoService.LocalBinder) service).getService();

            // if the service initializes okay (references the BLE adapter) then connect to the device
            if (rfduinoService.initialize()) {
                try {
                    if (rfduinoService.connect(bluetoothDevice.getAddress())) {
                        updateState(STATE_CONNECTING);
                        Log.v(TAG, "Service Connected");
                    }
                } catch (NullPointerException e) {
                    Log.e(TAG, "Service Connection Error");
                }

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            rfduinoService = null;
            updateState(STATE_DISCONNECTED);

            Log.v(TAG, "Service Disconnected");
        }
    };

    public void sendData(byte[] data) {
        if(!rfduinoService.send(data)) {
            scan();
        }
    }

    private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (RFduinoService.ACTION_CONNECTED.equals(action)) {
                connectButton.setEnabled(false);
                updateState(STATE_CONNECTED);
            } else if (RFduinoService.ACTION_DISCONNECTED.equals(action)) {
                connectButton.setEnabled(true);
                connected = false;
                updateState(STATE_DISCONNECTED);
            } else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action)) {
                addData(intent.getByteArrayExtra(RFduinoService.EXTRA_DATA));
            }
        }
    };

    public void scan() {
        scanStarted = true;
        updateUi();

        ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                //super.onScanResult(callbackType, result);
                bluetoothAdapter.getBluetoothLeScanner().stopScan(this);
                bluetoothDevice = result.getDevice();

                if(!connected) {
                    connectionStatusText.setText("Device found, connecting...");
                    Intent rfduinoIntent = new Intent(MainActivity.this, RFduinoService.class);
                    bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
                    connected = true;
                }
            }
        };

        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build();

        bluetoothAdapter.getBluetoothLeScanner().startScan(filterList, scanSettings, scanCallback);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initializes Bluetooth adapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // clear and add a filter for the device scan
        filterList.clear();
        filterList.add(new ScanFilter.Builder().setDeviceName("ECE480").build());

        connectionStatusText = (TextView) findViewById(R.id.connectionStatus);

        // run the device scan
        scan();

        // get UI components
        surgeryTimer = (TextView) findViewById(R.id.timer);
        startButton = (Button) findViewById(R.id.begin_button);
        stopButton = (Button) findViewById(R.id.stop_button);
        testButton = (Button) findViewById(R.id.test);
        connectButton = (Button) findViewById(R.id.connect);
        connectButton.setEnabled(true);

        connectButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scan();
            }
        });

        // set stop button to disabled
        stopButton.setEnabled(false);

        // sets the toggle button to turn on and off the sensor reading
        testButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(testButton.getText().equals("OFF")) {
                    // not sure if this is the best way to access the service
                    sendData(startOff.getBytes());
                    running = true;
                } else {
                    sendData(startOn.getBytes());
                    running = false;
                }
            }
        });

        // start button on click listener
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(startButton.getText().equals("Start")) {
                    stopButton.setEnabled(true);
                    startButton.setText("Pause");
                    startTimer();
                    if(!running) {
                        sendData(startOn.getBytes());
                        running = true;
                    } else {
                        sendData(startOff.getBytes());
                        running = false;
                    }
                } else if(startButton.getText().equals("Pause")){
                    startButton.setText("Resume");
                    if(!running) {
                        sendData(startOn.getBytes());
                        running = true;
                    } else {
                        sendData(startOff.getBytes());
                        running = false;
                    }
                    timer.cancel();
                } else {
                    if(!running) {
                        sendData(startOn.getBytes());
                        running = true;
                    } else {
                        sendData(startOff.getBytes());
                        running = false;
                    }
                    startTimer(); //(int) pauseTime
                    startButton.setText("Pause");
                }


            }
        });

        // stop button on click listener
        stopButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(running) {
                    sendData(startOff.getBytes());
                    running = false;
                }
                surgeryTimer.setText("0:00");
                startButton.setText("Start");
                timer.cancel();
                minutes = (long) 0;
                seconds = (long) 0;
            }
        });

    }

    public void startTimer() {
        // counts up
        timer = new CountDownTimer(1000*60*60*6, 1000) { // total time is 6 hours
            @Override
            public void onTick(long millisUntilFinished) {
                seconds++;
                seconds = seconds%60;

                // increment the minutes if seconds has 0 remainder
                if(seconds == 0) {
                    minutes++;
                }

                surgeryTimer.setText(String.format("%d:%02d",minutes, seconds));
            }

            @Override
            public void onFinish() {
                surgeryTimer.setText("0:00");


            }
        }.start();
    }


    @Override
    protected void onStart() {
        super.onStart();

        // register the broadcast recievers
        registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(rfduinoReceiver, RFduinoService.getIntentFilter());

        // figure out what this does
        updateState(bluetoothAdapter.isEnabled() ? STATE_DISCONNECTED : STATE_BLUETOOTH_OFF);
    }

    private void updateState(int newState) {
        state = newState;
        updateUi();
    }


    @Override
    protected void onStop() {
        super.onStop();

        // scan callback for stopScan() method
        ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        };

        // stop the scanner to free the resources
        bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        rfduinoService.close();
        rfduinoService.disconnect();

        // unregister the broadcast receivers
        unregisterReceiver(bluetoothStateReceiver);
        unregisterReceiver(rfduinoReceiver);
    }

    private void updateUi() {
        //boolean on = state > STATE_BLUETOOTH_OFF;

        // update the connection text
        if (state == STATE_CONNECTING) {
            connectionStatusText.setText("Connecting...");
        } else if (state == STATE_CONNECTED) {
            connected = true;
            connectionStatusText.setText("Connected");
        } else {
            connectionStatusText.setText("Disconnected");
        }
    }

    // change to a method that takes the data and process it like I need
    private void addData(byte[] data) {
        if(data.toString().equals("0")) {
            running = false;
            Log.v(TAG, "device is not running");
        } else {
            running = true;
            Log.v(TAG, "device is running");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
