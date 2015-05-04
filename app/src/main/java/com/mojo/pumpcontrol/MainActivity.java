package com.mojo.pumpcontrol;

import android.app.ActivityManager;
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
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private CountDownTimer timer;
    private static RFduinoService rfduinoService;
    private TextView connectionStatusText;
    private TextView surgeryTimer;
    private Button startButton;
    private Button stopButton;
    private Button connectButton;
    private Long seconds = (long) 0;
    private Long minutes = (long) 0;
    private String startOn = "1";
    private String startOff = "0";
    private List<ScanFilter> filterList = new ArrayList<ScanFilter>();
    private boolean connected = false;
    private boolean running = false;
    private Intent rfduinoIntent;

    private final static String TAG = MainActivity.class.getSimpleName();
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (RFduinoService.ACTION_CONNECTED.equals(action)) {
                connected = true;
                connectButton.setEnabled(false);
                startButton.setEnabled(true);
                if(running) {
                    stopButton.setEnabled(true);
                }
                updateState("Connected");
                invalidateOptionsMenu();
                getRemoteState();
            } else if (RFduinoService.ACTION_DISCONNECTED.equals(action)) {
                connected = false;
                startButton.setEnabled(false);
                stopButton.setEnabled(false);
                startButton.setText("START");
                connectButton.setEnabled(true);
                updateState("Disconnected");
                invalidateOptionsMenu();
            } else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action)) {
                getData(intent.getByteArrayExtra(RFduinoService.EXTRA_DATA));
            }
        }
    };
    private String startPause = "2";
    private Button lowButton;
    private Button midButton;
    private Button highButton;
    private TextView mode;

    private void getRemoteState() {
        rfduinoService.send("3".getBytes()); // '3' is the code to ask for RFduino state
    }

    private boolean mBinded;
    private final ServiceConnection rfduinoServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinded = true;
            Log.i(TAG, "RFduino Bounded");
            // set the local service variable to the running service
            rfduinoService = ((RFduinoService.LocalBinder) service).getService();

            // if the service initializes okay (references the BLE adapter) then connect to the device
            if (rfduinoService.initialize()) {
                try {
                    if (rfduinoService.connect(bluetoothDevice.getAddress())) {
                        updateState("Connecting");
                    }
                } catch (NullPointerException e) {
                    Log.e(TAG, "Service Connection Error");
                }

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBinded = false;
        }
    };

    public void sendData(byte[] data) {
        if (!rfduinoService.send(data)) {
            scan();
        }
    }

    public void scan() {

        ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                //super.onScanResult(callbackType, result);
                bluetoothAdapter.getBluetoothLeScanner().stopScan(this);
                bluetoothDevice = result.getDevice();

                if (!connected) {
                    connectionStatusText.setText("Connecting");
                    startService(rfduinoIntent);
                    bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
                }
            }
        };

        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build();

        bluetoothAdapter.getBluetoothLeScanner().startScan(filterList, scanSettings, scanCallback);
        // }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mBinded) {
            unbindService(rfduinoServiceConnection);
            Log.i(TAG, "RFduino Unbounded");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rfduinoIntent = new Intent(getApplicationContext(), RFduinoService.class);

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
        connectButton = (Button) findViewById(R.id.connect);
        lowButton = (Button) findViewById(R.id.low);
        midButton = (Button) findViewById(R.id.mid);
        highButton = (Button) findViewById(R.id.high);
        mode = (TextView) findViewById(R.id.mode);

        lowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("4".getBytes());
                mode.setText("70-100 mbar");
            }
        });

        midButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("5".getBytes());
                mode.setText("80-110 mbar");
            }
        });

        highButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData("6".getBytes());
                mode.setText("90-120 mbar");
            }
        });

        connectButton.setEnabled(true);
        stopButton.setEnabled(false);

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scan();
            }
        });

        // start button on click listener
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startButton.getText().equals("Start")) {
                    stopButton.setEnabled(true);
                    startButton.setText("PAUSE");
                    startTimer();
                    if (!running) {
                        sendData(startOn.getBytes());
                        running = true;
                    }
                } else if (startButton.getText().equals("PAUSE")) {
                    startButton.setText("RESUME");
                    if (running) {
                        sendData(startPause.getBytes());
                        running = false;
                    }
                    timer.cancel();
                } else {
                    if (!running) {
                        sendData(startOn.getBytes());
                        running = true;
                    }
                    startTimer(); //(int) pauseTime
                    startButton.setText("PAUSE");
                }
            }
        });

        // stop button on click listener
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (running) {
                    running = false;
                }
                sendData(startOff.getBytes());
                surgeryTimer.setText("0:00");
                startButton.setText("START");
                if(timer != null) {
                    timer.cancel();
                }
                minutes = (long) 0;
                seconds = (long) 0;
            }
        });

        if(isMyServiceRunning(RFduinoService.class)) {
            rfduinoService.send("3".getBytes());
        }

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void setTime(int s) {
        Log.v(TAG, Integer.toString(s));
        minutes = Long.valueOf(s / 60);
        seconds = Long.valueOf(s % 60);
    }

    public void startTimer() {
        // counts up
        timer = new CountDownTimer(1000 * 60 * 60 * 6, 1000) { // total time is 6 hours
            @Override
            public void onTick(long millisUntilFinished) {
                seconds++;
                seconds = seconds % 60;

                // increment the minutes if seconds has 0 remainder
                if (seconds == 0) {
                    minutes++;
                }

                surgeryTimer.setText(String.format("%d:%02d", minutes, seconds));
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

        // register the broadcast receiver
        registerReceiver(bluetoothStateReceiver, RFduinoService.getIntentFilter());

        if(isMyServiceRunning(RFduinoService.class)) {
            updateState(rfduinoService.getConnectionState());
            bindService(rfduinoIntent, rfduinoServiceConnection, BIND_AUTO_CREATE);
            if(connectionStatusText.getText().equals("Connected")) {
                connectButton.setEnabled(false);
            } else {
                connectButton.setEnabled(true);
            }
        }
    }

    private void updateState(String state) {
        connectionStatusText.setText(state);
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
    }


    protected void onDestroy() {
        super.onDestroy();

        // unregister the broadcast receiver
        unregisterReceiver(bluetoothStateReceiver);
    }

    // change to a method that takes the data and process it like I need
    private void getData(byte[] data) {
        if (fromByteArray(data) == 1 || fromByteArray(data) == 0) {
            running = false;
            stopButton.setEnabled(false);
        } else if(fromByteArray(data) > 40000){
            running = false;
            startButton.setText("RESUME");
            stopButton.setEnabled(true);
            setTime(fromByteArray(data)-40000);
            surgeryTimer.setText(String.format("%d:%02d", minutes, seconds));
        } else {
            running = true;
            setTime(fromByteArray(data));
            startTimer();
            startButton.setText("PAUSE");
            stopButton.setEnabled(true);
        }
    }

    public int fromByteArray(byte[] bytes) {
        // create a byte buffer and wrap the array
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        bb.order(ByteOrder.LITTLE_ENDIAN);

        // read your integers using ByteBuffer's getInt().
        // four bytes converted into an integer!
        return bb.getInt();
    }
}
