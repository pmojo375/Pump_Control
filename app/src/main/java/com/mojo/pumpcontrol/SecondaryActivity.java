package com.mojo.pumpcontrol;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;


public class SecondaryActivity extends ActionBarActivity {

    private TextView surgeryTimer;
    private int surgeryTime = 360000;
    private Button startButton;
    private EditText duration;
    private int durationInt;
    private long pauseTime;
    private Button stopButton;
    public AlertDialog alert;
    private Button testButton;
    private Long seconds = (long) 0;
    private Long minutes = (long) 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        final Context context = SecondaryActivity.this;

        surgeryTimer = (TextView) findViewById(R.id.timer);
        startButton = (Button) findViewById(R.id.begin_button);
        stopButton = (Button) findViewById(R.id.stop_button);
        testButton = (Button) findViewById(R.id.test);
        duration = (EditText) findViewById(R.id.duration);

        stopButton.setEnabled(false);

        // sets the toggle button to turn on and off the pump
        testButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(testButton.isActivated()) {
                    // not sure if this is the best way to access the service
                    MainActivity.sendData(new byte[]{5});
                } else {
                    MainActivity.sendData(new byte[]{6});
                }
            }
        });



        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(startButton.getText().equals("Start")) {
                    /*
                    try {
                        durationInt = Integer.parseInt(duration.getText().toString())*60000;
                        stopButton.setEnabled(true);
                        startTimer(durationInt);
                        startButton.setText("Pause");
                    } catch (NumberFormatException e) {
                         new AlertDialog.Builder(context)
                                .setTitle("Error!")
                                .setMessage("You need to enter a time.")
                                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                        Log.e("WARNING", "No time entered");
                    }
                    */
                    stopButton.setEnabled(true);
                    startButton.setText("Pause");
                    startTimer();
                } else if(startButton.getText().equals("Pause")){
                    startButton.setText("Resume");
                    timer.cancel();
                } else {
                    startTimer(); //(int) pauseTime
                    startButton.setText("Pause");
                }


            }
        });

        stopButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                surgeryTimer.setText("0:00");
                startButton.setText("Start");
                timer.cancel();
                minutes = (long) 0;
                seconds = (long) 0;
            }
        });

    }

    private CountDownTimer timer;

    public void startTimer() { //int d
        // changed to count up timer
        timer = new CountDownTimer(1000*60*60*6, 1000) { // total time is 6 hours
            @Override
            public void onTick(long millisUntilFinished) {
                // do something with sensorData
                // send to RFduino

                //Long seconds = millisUntilFinished/1000;
                //Long minutes = millisUntilFinished/60000;

                seconds++;
                seconds = seconds%60;

                // increment the minutes if seconds has 0 remainder
                if(seconds == 0) {
                    minutes++;
                }

                pauseTime = millisUntilFinished;

                surgeryTimer.setText(String.format("%d:%02d",minutes, seconds));
            }

            @Override
            public void onFinish() {
                surgeryTimer.setText("0:00");


            }
        }.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_secondary, menu);
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
