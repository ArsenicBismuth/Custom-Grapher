package com.rsam.customgrapher;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;

import android.Manifest;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.rsam.customgrapher.permissions.PermissionsActivity;
import com.rsam.customgrapher.permissions.PermissionsChecker;

public class MainActivity extends AppCompatActivity /*implements Visualizer.OnDataCaptureListener*/{

    private static final int REQUEST_CODE = 0;
    static final String[] PERMISSIONS = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS};

    private static final int MAX_AMPLITUDE = 16384;
    SimpleWaveform simpleWaveform;
    RecyclerView recyclerView;
    LinearLayoutManager linearLayoutManager;

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
    private Thread recordingThread = null;
//    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private static final int BufferElements2Rec = 1024;
    private static final int BytesPerElement = 2; // 2 bytes in 16bit format
    private static final int downSample = 70; // Get every x-th sample

    // Debugging for LOGCAT
    private static final String TAG = "MainActivity";

    // Variables for settings menu and their initial conditions
    public static boolean setBPM = true;    // Settings for BPM calculation
    public static boolean setSPO2 = true;   // Settings for BPM calculation
    public static boolean setDebug = false; // Settings for debug message
//	public static float zoomHor = 1;		// Settings for waveform horizontal scale
//	public static float zoomVer = 1;		// Settings for waveform vertical scale
    public static boolean doRun = true;     // Run/Pause functionality

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

        applySettings();    // Apply settings variables to layout

        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (doRun) {
                    doRun = false;
                    stopRecording();
                    Toast.makeText(MainActivity.this, getString(R.string.toast_pause),
                            Toast.LENGTH_SHORT).show();
                    fab.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    doRun = true;
                    startRecording();
                    Toast.makeText(MainActivity.this, getString(R.string.toast_resume),
                            Toast.LENGTH_SHORT).show();
                    fab.setImageResource(android.R.drawable.ic_media_pause);
                }
            }
        });

//        recyclerView = (RecyclerView)findViewById(R.id.recycler);
//        recyclerView.setVisibility(View.GONE);

        simpleWaveform = (SimpleWaveform) findViewById(R.id.simpleWaveform);
        simpleWaveform.setVisibility(View.VISIBLE);

        amplitudeWave();

        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

//        startRecording(); Already in onResume()
    }

    @Override
    protected void onResume() {
        super.onResume();
        PermissionsChecker checker = new PermissionsChecker(this);

        if (checker.lacksPermissions(PERMISSIONS)) {
            startPermissionsActivity();
        } else {
            startRecording();
        }
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
//        for (int i = 0; i < BufferElements2Rec; i++) {
//            addData(randomInt(0,4096));
//        }
        final short sData[] = new short[BufferElements2Rec];
        try {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

            recorder.startRecording();
//            doRun = true;
            recordingThread = new Thread(new Runnable() {
                public void run() {
//                    synchronized (this) {
                        while (doRun) {
//                        Log.d("","RECORDING RECORDING ");
                            recorder.read(sData, 0, BufferElements2Rec);
//                        Log.d("", "ValA " + sData[0]);
//                        addData(sData[0]);  // First data with size BufferElements2Rec, thus rate: RECORDER_SAMPLERATE / that size
                            addArray(sData);
                        }
//                    }
                }
            }, "AudioRecorder Thread");
            recordingThread.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
//            doRun = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
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
        //TextView text = findViewById(R.id.textBPM);
        if (setBPM) {
            tBPM.setVisibility(View.VISIBLE);
            // Also enable calculation
        } else {
            tBPM.setVisibility(View.INVISIBLE);
            // Also disable calculation
        }

        //text = findViewById(R.id.textSPO2);
        if (setSPO2) {
            tSPO2.setVisibility(View.VISIBLE);
            // Also enable calculation
        } else {
            tSPO2.setVisibility(View.INVISIBLE);
            // Also disable calculation
        }

        View debugLayout = findViewById(R.id.layoutDebug);
        if (setDebug) {
            debugLayout.setVisibility(View.VISIBLE);
        } else {
            debugLayout.setVisibility(View.INVISIBLE);
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
                text.setText(getString(R.string.debug_content_1));
                text = findViewById(R.id.textDebug2);
                text.setText(getString(R.string.debug_content_2));
                text = findViewById(R.id.textDebug3);
                text.setText(getString(R.string.debug_content_3));
                break;
            case 1:
                // Immediately stop if no debugging, better performance
                if (!setDebug) return;
                text = findViewById(R.id.textDebug1);
                text.setText(getString(R.string.debug_content_1) + message);
                break;
            case 2:
                // Immediately stop if no debugging, better performance
                if (!setDebug) return;
                text = findViewById(R.id.textDebug2);
                text.setText(getString(R.string.debug_content_2) + message);
                break;
            case 3:
                text = findViewById(R.id.textDebug3);
                text.setText(getString(R.string.debug_content_3) + message);
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

    public void addArray(short[] arr) {
        int arrSize = arr.length;
        for (int i = 0; i < arrSize; i++) {
//            Log.d("","ValA " + arr[i]);
            if (i % downSample == 0) addData(arr[i]);    // Add every x data
            arr[i] = 0;
        }
        simpleWaveform.postInvalidate();
    }

    public void addData(int value) {
        value = value * (simpleWaveform.height - 1) / MAX_AMPLITUDE;
        ampList.addFirst(value);
//        Log.d("","Size " + ampList.size());
        if (ampList.size() > simpleWaveform.width / simpleWaveform.barGap + 2) {
            synchronized (this) {
                ampList.removeLast();
//                Log.d("", "SimpleWaveform: ampList remove last node, total " + ampList.size());
            }
        }
//        simpleWaveform.postInvalidate();
//        simpleWaveform.refresh();
    }

    private void recyclerWave() {

        LinkedList<LinkedList<Integer>> amp_list_list = new LinkedList();
        for (int i = 0; i < 6; i++) {

            LinkedList<Integer> ampList = new LinkedList<>();
            amp_list_list.add(ampList);

            for (int j = 0; j < 200; j++) {
                ampList.add(randomInt(-50, 50));
            }

        }

        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        RecyclerViewAdapter waveAdapter = new RecyclerViewAdapter(amp_list_list);
        recyclerView.setAdapter(waveAdapter);

//        recycler_view.scrollBy(100, 10);
        recyclerView.scrollToPosition(2);
//        recycler_view.smoothScrollBy(100, 10);
    }

    private int randomInt(int min, int max) {

        Random rand = new Random();
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }

    private class RecyclerViewAdapter extends
            RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {
        // List<LvRowFile> listItems;
        LinkedList<LinkedList<Integer>> amp_list_list;

        public RecyclerViewAdapter(LinkedList<LinkedList<Integer>> amp_list_list) {
            this.amp_list_list = amp_list_list;
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public SimpleWaveform simpleWaveform;

            public ViewHolder(View itemView) {
                super(itemView);
                this.simpleWaveform = (SimpleWaveform) itemView
                        .findViewById(R.id.simpleWaveformRow);
//                this.simpleWaveform.clearScreen();
            }
        }

        @Override
        public int getItemCount() {
            Log.d("","SimpleWaveform: amp_list_list.size() "+amp_list_list.size());
            return amp_list_list.size();
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Log.d("", "SimpleWaveform: position " + position);
            holder.simpleWaveform.setDataList(amp_list_list.get(position));

            holder.simpleWaveform.barPencilSecond.setStrokeWidth(15);
            holder.simpleWaveform.barPencilSecond.setColor(0xff1dcfcf);

            holder.simpleWaveform.peakPencilSecond.setStrokeWidth(5);
            holder.simpleWaveform.peakPencilSecond.setColor(0xfffeef3f);

            //show x-axis
            holder.simpleWaveform.showXAxis = true;
            holder.simpleWaveform.xAxisPencil.setStrokeWidth(1);
            holder.simpleWaveform.xAxisPencil.setStrokeWidth(0x88ffffff);

            holder.simpleWaveform.refresh();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // View view = View.inflate(parent.getContext(),
            // R.layout.gridview_pic, null);
            View view = View.inflate(parent.getContext(),
                    R.layout.row_recycler, null);
            ViewHolder holder = new ViewHolder(view);
            Log.d("","SimpleWaveform: onCreateViewHolder ");
            return holder;
        }

    }

}
