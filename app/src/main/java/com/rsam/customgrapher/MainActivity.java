package com.rsam.customgrapher;

import java.util.Collections;
import java.util.LinkedList;

import android.Manifest;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.rsam.customgrapher.permissions.PermissionsActivity;
import com.rsam.customgrapher.permissions.PermissionsChecker;

public class MainActivity extends AppCompatActivity /*implements Visualizer.OnDataCaptureListener*/ {

    private static final int REQUEST_CODE = 0;
    static final String[] PERMISSIONS = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS};

    private static final int MAX_AMPLITUDE = 16384;
    SimpleWaveform simpleWaveform;

    Paint barPencilFirst = new Paint();
    Paint barPencilSecond = new Paint();
    Paint peakPencilFirst = new Paint();
    Paint peakPencilSecond = new Paint();

    Paint xAxisPencil = new Paint();

    LinkedList<Integer> ampList = new LinkedList<>();

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private static Thread recordingThread = null;
    //    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private static final int BufferElements2Rec = 1024;
    private static final int BytesPerElement = 2;   // 2 bytes in 16bit format
    private static final int downSample = 140;      // Get every x-th sample

    private static long dataNum = 0;         // Keep the data count, beware it'll overflow

    // Debugging for LOGCAT
    private static final String TAG = "MainActivity";

    // Variables for settings menu and their initial conditions
    public static boolean setBPM = true;    // Settings for BPM calculation
    public static boolean setSPO2 = true;   // Settings for BPM calculation
    public static boolean setDebug = false; // Settings for debug message
    //	public static float zoomHor = 1;		// Settings for waveform horizontal scale
//	public static float zoomVer = 1;		// Settings for waveform vertical scale
    public static boolean doRun = true;     // Run/Pause functionality
    public static boolean fabState = true;  // Preserving floating button state on pause

    public static TimeDiff timeScreen;

    TextView tBPM;
    TextView tSPO2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tBPM = findViewById(R.id.textBPM);
        tSPO2 = findViewById(R.id.textSPO2);

        setDebugMessages("",0); // Empty values
        applySettings();                        // Apply settings variables to layout

        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (doRun) {
                    doRun = false;
                    fabState = false;
                    stopRecording();
                    Toast.makeText(MainActivity.this, getString(R.string.toast_pause),
                            Toast.LENGTH_SHORT).show();
                    fab.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    doRun = true;
                    fabState = true;
                    startRecording();
                    Toast.makeText(MainActivity.this, getString(R.string.toast_resume),
                            Toast.LENGTH_SHORT).show();
                    fab.setImageResource(android.R.drawable.ic_media_pause);
                }
            }
        });

        simpleWaveform = (SimpleWaveform) findViewById(R.id.simpleWaveform);
        simpleWaveform.setVisibility(View.VISIBLE);

        amplitudeWave();

        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

        timeScreen = new TimeDiff();

//        startRecording(); Already in onResume()
    }

    @Override
    protected void onResume() {
        super.onResume();
        PermissionsChecker checker = new PermissionsChecker(this);

        if (checker.lacksPermissions(PERMISSIONS)) {
            startPermissionsActivity();
        } else {
            doRun = fabState;
            if (doRun) startRecording();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        doRun = false;
        stopRecording();
    }

    private void startPermissionsActivity() {
        PermissionsActivity.startActivityForResult(this, REQUEST_CODE, PERMISSIONS);
    }

//    private void startRecording() {
//        try {
//            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//            recorder.setOutputFile("/dev/null");
//            recorder.prepare();
//            recorder.start();
//        } catch (IllegalStateException | IOException ignored) {
//        }
//    }

    private void startRecording() {

        final short sData[] = new short[BufferElements2Rec];
        try {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

            recorder.startRecording();
            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (doRun) {
                        try {
                            Thread.sleep(1);
                        } catch (Exception e) {
                        }

                        recorder.read(sData, 0, BufferElements2Rec);
                        Log.d("", "ValA " + sData[0]);

                        // New, separate, UI Thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                Log.d("", "ValA " + sData[0]);
//                                addData(sData[0]);  // First data with size BufferElements2Rec, thus rate: RECORDER_SAMPLERATE / that size

                                addArray(sData);
                                setDebugMessages(String.valueOf(Collections.max(ampList)),1);
                                setDebugMessages(String.valueOf(dataNum),3);
                                simpleWaveform.refresh();
                            }
                        });
                    }
                }
            }, "AudioRecorder Thread");
            recordingThread.start();

//            waveThread = new Thread(new Runnable() {
//                @Override
//                public void run() {
//
//                    while (doRun) {
//                        try {
//                            Thread.sleep(1);
//                        } catch (Exception e) {
//                        }
//
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
////                                Log.d("", "ValA " + sData[0]);
////                                addData(sData[0]);  // First data with size BufferElements2Rec, thus rate: RECORDER_SAMPLERATE / that size
//
//                                addArray(sData);
//                                if (ampList.size() > simpleWaveform.width / simpleWaveform.barGap + 2) {
//                                    ampList.removeLast();
//                                    Log.d("", "SimpleWaveform: ampList remove last node, total " + ampList.size());
//                                }
//                                simpleWaveform.refresh();
//                            }
//                        });
//                    }
//                }
//            }, "Waveform Thread");
//            waveThread.start();

        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }

    public void addArray(short[] arr) {
        int arrSize = arr.length;
        for (int i = 0; i < arrSize; i++) {
            if (i % downSample == 0) addData(arr[i]);    // Add every x data
            arr[i] = 0;
        }
//        simpleWaveform.postInvalidate();    // Refresh only every every batch
    }

    public void addData(int value) {
        value = value * (simpleWaveform.height - 1) / MAX_AMPLITUDE;
        ampList.addFirst(value);
        if (ampList.size() > simpleWaveform.width / simpleWaveform.barGap + 2) {
            ampList.removeLast();
        }
        dataNum++;  // Increment data count
//        simpleWaveform.postInvalidate();
//        simpleWaveform.refresh();
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        handler.removeCallbacks(updater);
//        recorder.stop();
//        recorder.reset();
//        recorder.release();
//    }
//
//    @Override
//    public void onWindowFocusChanged(boolean hasFocus) {
//        super.onWindowFocusChanged(hasFocus);
//        handler.post(updater);
//    }

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

        // Action on settings (which basically a list of multiple CheckBoxes)
        if (id == R.id.action_settings) {
            final boolean[] selectedItems = {setBPM, setSPO2, setDebug}; // Map setting items to their corresponding variables

            // Trust me, this is the best template out there. Browsing again won't give better stuffs.
            // Just need to change mapping above and the corresponding effects below, inside "onClick" & applySettings().
            // Also, see strings.xml to see the settings content (R.array.settings_content)
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.action_settings))
                    .setMultiChoiceItems(R.array.settings_content, selectedItems, new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {
                            if (isChecked) {
                                // If the user checked the item, set it to true in the items array
                                selectedItems[indexSelected] = true;
                            } else {
                                // Else, if the item is unchecked, set it to false in the items array
                                selectedItems[indexSelected] = false;
                            }
                        }
                    })
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            // Apply settings to variables
                            setBPM = selectedItems[0];
                            setSPO2 = selectedItems[1];
                            setDebug = selectedItems[2];

                            // Apply settings variables to layout
                            applySettings();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            // Don't apply settings
                        }
                    })
                    .show();
            return true;
        }

        if (id == R.id.action_about) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.action_about))
                    .setMessage(getString(R.string.about_content))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with delete
                        }
                    })
                    .show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void applySettings() {

        // Apply settings variables to layout
        View layout = findViewById(R.id.layoutBPM);
        if (setBPM) {
            layout.setVisibility(View.VISIBLE);
            // Also enable calculation
        } else {
            // Remove as a layout rather than just changing visibility
            layout.setVisibility(View.GONE);
            // Also disable calculation
        }

        layout = findViewById(R.id.layoutSPO2);
        if (setSPO2) {
            layout.setVisibility(View.VISIBLE);
            // Also enable calculation
        } else {
            layout.setVisibility(View.GONE);
            // Also disable calculation
        }

        layout = findViewById(R.id.layoutDebug);
        if (setDebug) {
            layout.setVisibility(View.VISIBLE);
        } else {
            layout.setVisibility(View.INVISIBLE);
        }

//        setPower(valAvg, valPeak); // Refresh the power texts
    }

    private int calculateBPM(int values) {
        return 0;
    }

//    public void setPower(int valAvg, int valPeak) {
//        // Power texts controller
//        TextView text;
//        String empty = "-";
//
//        // Store to local class variable
//        this.valAvg = valAvg;
//        this.valPeak = valPeak; // Unused, removed feature
//
//        // Check if highest value
//        valHigh = (valAvg > valHigh) ? valAvg : valHigh;
//
//        text = findViewById(R.id.textAvg);
//
//        if (valAvg == -1) text.setText(empty);
//        else if (setHp) text.setText(String.format("%.3f", valAvg * 0.00134102));
//        else text.setText(String.valueOf(valAvg));
//
//
//        text = findViewById(R.id.textHigh);
//
//        if (valHigh == -1) text.setText(empty);
//        else if (setHp) text.setText(String.format("%.3f", valHigh * 0.00134102));
//        else text.setText(String.valueOf(valHigh));
//    }

    public void setDebugMessages(String message, int index) {
        TextView text;

        // Setting debug messages
        switch (index) {
            case 0:
                // Clear
                text = findViewById(R.id.textDebug1);
                text.setText(String.format(getString(R.string.debug_content_1),""));
                text = findViewById(R.id.textDebug2);
                text.setText(String.format(getString(R.string.debug_content_2),""));
                text = findViewById(R.id.textDebug3);
                text.setText(String.format(getString(R.string.debug_content_3),""));
                break;
            case 1:
                // Immediately stop if no debugging, better performance
                if (!setDebug) return;
                text = findViewById(R.id.textDebug1);
                text.setText(String.format(getString(R.string.debug_content_1), message));
                break;
            case 2:
                // Immediately stop if no debugging, better performance
                if (!setDebug) return;
                text = findViewById(R.id.textDebug2);
                text.setText(String.format(getString(R.string.debug_content_2), message));
                break;
            case 3:
                text = findViewById(R.id.textDebug3);
                text.setText(String.format(getString(R.string.debug_content_3), message));
                break;
        }
    }

    private void amplitudeWave() {

        simpleWaveform.init();

        simpleWaveform.setDataList(ampList);

        //define background
        simpleWaveform.background.setColor(getResources().getColor(R.color.White));

        //define bar gap
        simpleWaveform.barGap = 2;

        //define x-axis direction
        simpleWaveform.modeDirection = SimpleWaveform.MODE_DIRECTION_LEFT_RIGHT;

        //define if draw opposite pole when show bars. Doing so will make negatives as absolutes.
        simpleWaveform.modeAmp = SimpleWaveform.MODE_AMP_ORIGIN;
        //define if the unit is px or percent of the view's height
        simpleWaveform.modeHeight = SimpleWaveform.MODE_HEIGHT_PX;
        //define where is the x-axis in y-axis
        simpleWaveform.modeZero = SimpleWaveform.MODE_ZERO_CENTER;
        //if show bars?
        simpleWaveform.showBar = false;

        //define how to show peaks outline
        simpleWaveform.modePeak = SimpleWaveform.MODE_PEAK_PARALLEL;
        //if show peaks outline?
        simpleWaveform.showPeak = true;

        //show x-axis
        simpleWaveform.showXAxis = true;
        xAxisPencil.setStrokeWidth(1);
//        xAxisPencil.setColor(0x88ffffff);
        xAxisPencil.setColor(getResources().getColor(R.color.Black));
        simpleWaveform.xAxisPencil = xAxisPencil;

        //define pencil to draw bar
        barPencilFirst.setStrokeWidth(1);
        barPencilFirst.setColor(0xff1dcf0f);
        simpleWaveform.barPencilFirst = barPencilFirst;
        barPencilSecond.setStrokeWidth(1);
        barPencilSecond.setColor(0xff1dcfcf);
        simpleWaveform.barPencilSecond = barPencilSecond;

        //define pencil to draw peaks outline
        peakPencilFirst.setStrokeWidth(1);
        peakPencilFirst.setColor(getResources().getColor(R.color.colorPrimary));
        simpleWaveform.peakPencilFirst = peakPencilFirst;
        peakPencilSecond.setStrokeWidth(1);
        peakPencilSecond.setColor(getResources().getColor(R.color.colorPrimary));
        simpleWaveform.peakPencilSecond = peakPencilSecond;

        //the first part will be draw by PencilFirst
        simpleWaveform.firstPartNum = 20;

        //define how to clear screen
        simpleWaveform.clearScreenListener = new SimpleWaveform.ClearScreenListener() {
            @Override
            public void clearScreen(Canvas canvas) {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            }
        };

        // Important example, don't remove
//        simpleWaveform.progressTouch = new SimpleWaveform.ProgressTouch() {
//            @Override
//            public void progressTouch(int progress, MotionEvent event) {
//                Log.d("", "you touch at: " + progress);
//                simpleWaveform.firstPartNum = progress;
//                simpleWaveform.refresh();
//            }
//        };

//        //loop
//        new Thread(new Runnable() {
//            @Override
//            public void doRun() {
//                while (true) {
//                    try {
//                        Thread.sleep(200);
//                    } catch (Exception e) {
//                    }
//
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void doRun() {
//                            ampList.addFirst(randomInt(-50, 50));
//                            if (ampList.size() > simpleWaveform.width / simpleWaveform.barGap + 2) {
//                                ampList.removeLast();
//                                Log.d("", "SimpleWaveform: ampList remove last node, total " + ampList.size());
//                            }
//                            simpleWaveform.refresh();
//                        }
//                    });
//                }
//            }
//        }).start();
    }

}