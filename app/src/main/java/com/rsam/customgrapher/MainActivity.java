package com.rsam.customgrapher;

import java.util.Collections;
import java.util.LinkedList;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.rsam.customgrapher.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity /*implements Visualizer.OnDataCaptureListener*/ {

    private static final int REQUEST_CODE = 0;
    static final String[] PERMISSIONS = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS};

    private static final int MAX_AMPLITUDE = 65536;
    SimpleWaveform simpleWaveformA;
    SimpleWaveform simpleWaveformB;

    Paint barPencilFirst = new Paint();
    Paint barPencilSecond = new Paint();
    Paint peakPencilFirst = new Paint();
    Paint peakPencilSecond = new Paint();
    Paint background = new Paint();

    Paint xAxisPencil = new Paint();

    LinkedList<Integer> ampListA = new LinkedList<>();  // Data used by Waveform
    LinkedList<Integer> ampListB = new LinkedList<>();

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private static final int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private static final int BytesPerElement = 2;       // 2 bytes in 16bit format
    private static int downSample = 1;                  // Get every x-th sample, also a [settings]

    private static long dataNum = 0;                    // Keep the data count, beware it'll overflow so use the difference if necessary

    // Filters, specified based on the note on Readme.md
    private double[] b_lpf_ch1 = {-0.000472124578466290, 0.000704149548352154, 0.00274189310474332, 0.00591248887896805, 0.00921257087324632, 0.0109695262713906, 0.00953038922526930, 0.00428143311355899, -0.00353878822093340, -0.0108376853965620, -0.0137750375665968, -0.00969532462583917, 0.000997797813952307, 0.0141295296141812, 0.0229572420560878, 0.0211513757921862, 0.00639861039263159, -0.0171381544555205, -0.0390535049402061, -0.0460804182276977, -0.0275733801766756, 0.0191691643281890, 0.0862485232208061, 0.156732800488558, 0.210190627040545, 0.230113426289257, 0.210190627040545, 0.156732800488558, 0.0862485232208061, 0.0191691643281890, -0.0275733801766756, -0.0460804182276977, -0.0390535049402061, -0.0171381544555205, 0.00639861039263159, 0.0211513757921862, 0.0229572420560878, 0.0141295296141812, 0.000997797813952307, -0.00969532462583917, -0.0137750375665968, -0.0108376853965620, -0.00353878822093340, 0.00428143311355899, 0.00953038922526930, 0.0109695262713906, 0.00921257087324632, 0.00591248887896805, 0.00274189310474332, 0.000704149548352154, -0.000472124578466290}; // 51
    private double[] b_hpf_ch2 = {0.00699933507837955, -0.0149678167519151, 0.00311607835569475, 0.00803885630276995, 0.00422418129212178, -0.00398720213516828, -0.00896571444921871, -0.00466568580611676, 0.00613277494422169, 0.0123814810399314, 0.00538303699854521, -0.0100357111631056, -0.0176506668652045, -0.00608382389356992, 0.0161895058352412, 0.0255041559244084, 0.00662759144169743, -0.0267030901037898, -0.0389968229048643, -0.00706386098623202, 0.0487314683840281, 0.0702835890365723, 0.00732055553146396, -0.133003290106314, -0.278800975941692, 0.659254180333660, -0.278800975941692, -0.133003290106314, 0.00732055553146396, 0.0702835890365723, 0.0487314683840281, -0.00706386098623202, -0.0389968229048643, -0.0267030901037898, 0.00662759144169743, 0.0255041559244084, 0.0161895058352412, -0.00608382389356992, -0.0176506668652045, -0.0100357111631056, 0.00538303699854521, 0.0123814810399314, 0.00613277494422169, -0.00466568580611676, -0.00896571444921871, -0.00398720213516828, 0.00422418129212178, 0.00803885630276995, 0.00311607835569475, -0.0149678167519151, 0.00699933507837955}; // 51
    private double[] b_lpf_demod = {0.00132179296512823, 0.00280744757268396, 0.00545831784089734, 0.00935169596603919, 0.0146337919246326, 0.0213226685708710, 0.0292842870857679, 0.0382122279876832, 0.0476392084864076, 0.0569720191710138, 0.0655455088410666, 0.0726938328338288, 0.0778301498374599, 0.0805161101633598, 0.0805161101633598, 0.0778301498374599, 0.0726938328338288, 0.0655455088410666, 0.0569720191710138, 0.0476392084864076, 0.0382122279876832, 0.0292842870857679, 0.0213226685708710, 0.0146337919246326, 0.00935169596603919, 0.00545831784089734, 0.00280744757268396, 0.00132179296512823}; // 28
    private double[] b_bpf_last = {1.86063134172615e-10, 0, -2.79094701258923e-09, 0, 1.95366290881246e-08, 0, -8.46587260485400e-08, 0, 2.53976178145620e-07, 0, -5.58747591920364e-07, 0, 9.31245986533939e-07, 0, -1.19731626840078e-06, 0, 1.19731626840078e-06, 0, -9.31245986533939e-07, 0, 5.58747591920364e-07, 0, -2.53976178145620e-07, 0, 8.46587260485400e-08, 0, -1.95366290881246e-08, 0, 2.79094701258923e-09, 0, -1.86063134172615e-10}; // 31
    private double[] a_bpf_last = {1, -27.2636248902419, 359.355963755147, -3049.63556305642, 18722.0383212545, -88567.6151612900, 335815.797056573, -1048026.96523334, 2743410.89548105, -6107670.76235757, 11684901.8045721, -19360632.8652062, 27943285.3523427, -35279454.6574221, 39074092.0806992, -38027328.1408908, 32537066.9404145, -24462618.8676193, 16134454.4910529, -9308899.60870611, 4678581.30276138, -2036505.10358459, 761787.956244342, -242361.595239282, 64678.1361217432, -14207.4289461239, 2501.49261337377, -339.408925432695, 33.3161939507062, -2.10570576573189, 0.0643470164730020}; // 31

//    private Filter lpf1 = new Filter(256, b1, BufferElements2Rec, true);
    private Filter filCh1 = new Filter(51, b_lpf_ch1, BufferElements2Rec / 21, false);      // 2100 Hz (1/21)
    private Filter filCh2 = new Filter(51, b_hpf_ch2, BufferElements2Rec / 21, false);      // 2100 Hz (1/21)
    private Filter filDemod1 = new Filter(28, b_lpf_demod, BufferElements2Rec / 21, true);  // 2100 Hz (1/1)
    private Filter filDemod2 = new Filter(28, b_lpf_demod, BufferElements2Rec / 21, true);  // 2100 Hz (1/1)
    private Filter filLast1 = new Filter(31, b_bpf_last, a_bpf_last, BufferElements2Rec / 210, false); // 210 Hz (1/10)
    private Filter filLast2 = new Filter(31, b_bpf_last, a_bpf_last, BufferElements2Rec / 210, false); // 210 Hz (1/10)

//    private Filter hpf1 = new Filter(255, b2, BufferElements2Rec, true);
//    private Rectifier rect1 = new Rectifier(BufferElements2Rec);

//    private double[] b = {1};
//    private Filter lpf1 = new Filter(1, b);

    // Debugging for LOGCAT
    private static final String TAG = "MainActivity";

    // Variables for settings menu and their initial conditions
    public static boolean setBPM = true;    // Settings for BPM calculation
    public static boolean setSPO2 = true;   // Settings for BPM calculation
    public static int setOrientation = 0;   // Settings for orientation
    public static boolean setDebug = false; // Settings for debug message
    public static boolean setReset = false; // Settings for reset settings
//	public static float zoomHor = 1;		// Settings for waveform horizontal scale
//	public static float zoomVer = 1;		// Settings for waveform vertical scale
    public static boolean doRun = true;     // Run/Pause functionality
    public static boolean fabState = true;  // Preserving floating button state on pause

//    public static TimeDiff timeScreen;

    TextView tBPM;
    TextView tSPO2;

    // Layout editor compatibility with simpleWaveform
        // Or at least change the bg canvas to darker white like the original
    // General filter implementation, still not even filtering, and data is somehow cut to a 1/4
        // Synchronized data, or at least matching pace
    // Independent waveform management
        // Dual waveform
    // Demodulation
    // Performance
        // Demodulation is great, only it requires a very high-ordered LPF.
        // 256th order filter is prefect, while 128th order still too noisy, and 1024th too slow.
    // TODO near-perfect demodulation scheme
        // Basically:
        //  1. LPF/HPF to separate 2 signals (not narrow)
        //  2. Rectify & LPF (not narrow)
        //  3. Downsample a lot to be able to implement very narrow BPF or low-freq HPF
    // TODO BPM calculation
    // TODO SPO2 calculation
    // TODO cleaning up on published version
        // Remove advanced profiler & logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Main GUIs initialization
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tBPM = findViewById(R.id.textBPM);
        tSPO2 = findViewById(R.id.textSPO2);

        setDebugMessages("",0); // Empty values

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

        // Settings initialization
        // Making sure Shared Pref file initialized with default values.
        // Last parameter specifies to read the data if the method has been called before,
        // but it won't reset; just an efficiency flag.
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
            // Add other headers' pref_ if available/used

        // Waveforms initialization
        simpleWaveformA = findViewById(R.id.simpleWaveformA);
        simpleWaveformA.setVisibility(View.VISIBLE);

        simpleWaveformB = findViewById(R.id.simpleWaveformB);
        simpleWaveformB.setVisibility(View.VISIBLE);

        amplitudeWave(simpleWaveformA, ampListA);
        amplitudeWave(simpleWaveformB, ampListB);

//        // Common problem but pretty difficult to find the example, basically check if a layout has been drawn.
//        // This is done since the layout is drawn way later than onCreate, or even onResume in the initial launch.
//        ViewTreeObserver vto = simpleWaveformB.getViewTreeObserver();
//        vto.addOnGlobalLayoutListener (new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                simpleWaveformB.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                int width  = simpleWaveformB.getMeasuredWidth();
//                int height = simpleWaveformB.getMeasuredHeight();
//
//                // Do something with the layout data
//            }
//        });

        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

//        lpf1.initBuffer(BufferElements2Rec);  // Initialized above
//        rect1.initBuffer(BufferElements2Rec);

        Log.d(TAG, "minBufferSize " + String.valueOf(bufferSize));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Also part of the method called on launch in the Activity Lifecycle

        PermissionsChecker checker = new PermissionsChecker(this);
        if (checker.lacksPermissions(PERMISSIONS)) {
            startPermissionsActivity();
        } else {
            doRun = fabState;
            if (doRun) startRecording();
        }

        // Apply settings
        applySettings();
    }

    @Override
    protected void onPause() {
        super.onPause();
        doRun = false;
        stopRecording();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Action on settings
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
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

        if (id == R.id.action_copy) {
            if (doRun) {
                doRun = false;
                stopRecording();
                toClipboard(ampListA, false);
                toClipboard(ampListB, true);
                doRun = true;
                startRecording();
            } else {
                toClipboard(ampListA, false);
                toClipboard(ampListB, true);
            }

            Toast.makeText(MainActivity.this, getString(R.string.toast_copy),
                    Toast.LENGTH_SHORT).show();

            return true;
        }

        if (id == R.id.action_copy_cust) {
            if (doRun) {
                doRun = false;
                stopRecording();
                toClipboard(filDemod1.getBuffer(), false);
                toClipboard(filDemod2.getBuffer(), true);
                doRun = true;
                startRecording();
            } else {
                toClipboard(filDemod1.getBuffer(), false);
                toClipboard(filDemod2.getBuffer(), true);
            }

            Toast.makeText(MainActivity.this, getString(R.string.toast_copy),
                    Toast.LENGTH_SHORT).show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void applySettings() {
        Log.d("", "applySettings");

        // Get Shared Preferences under the default name "com.example.something_preferences"
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);

        // Get the values
        // Def value here is the value if no value found for specified key (including xml default val)
        setBPM = sharedPref.getBoolean("switch-bpm", true);
        setSPO2 = sharedPref.getBoolean("switch-spo2", true);
        setOrientation = Integer.parseInt(sharedPref.getString("list-orientation", "0"));
        setDebug = sharedPref.getBoolean("switch-debug", false);
        downSample = Integer.parseInt(sharedPref.getString("value-downsample", "10"));
        setReset = sharedPref.getBoolean("switch-reset", false);

        if (setReset) {
            // Force the settings to revert to xml default values
            Log.d("", "Reset");
//            sharedPref.edit().clear().apply();  // Apply() returns no value, faster because asynchronous
            sharedPref.edit().clear().commit();  // But Apply() might make the settings not fully reset

            PreferenceManager.
                    setDefaultValues(this, R.xml.pref_general, false);    // Reread def values

            setReset = false;   // Force change, despite default value is already false. Prevent human error.
            applySettings();    // Reapply settings
            return;
        }

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

        switch (setOrientation) {
            case 2 :  setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); break;
            case 1 :  setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); break;
            default : setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        if (downSample < 1) downSample = 1;

        layout = findViewById(R.id.layoutDebug);
        if (setDebug) {
            layout.setVisibility(View.VISIBLE);
        } else {
            layout.setVisibility(View.INVISIBLE);
        }

//        setPower(valAvg, valPeak); // Refresh the power texts
    }

    private void startPermissionsActivity() {
        PermissionsActivity.startActivityForResult(this, REQUEST_CODE, PERMISSIONS);
    }

//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//
//        // Checks the orientation of the screen, if landscape make the waveform side-by-side
////        LinearLayout layoutWave = findViewById(R.id.layoutWave);
////        LinearLayout separator = findViewById(R.id.separator);
//        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) { // Default condition, which is the one currently used on the main layout
////            layoutWave.setOrientation(LinearLayout.VERTICAL);
////            Log.d("", "portrait");
//            // Since the height is based on weight, height must be zero, width MATCH_PARENT
//        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
////            layoutWave.setOrientation(LinearLayout.HORIZONTAL);
////            Log.d("", "landscape");
//            // Since the the one based on weight, width must be zero, height MATCH_PARENT
//        }
//    }
//
//    private void resizeView(View view, int newWidth, int newHeight) {
//        try {
//            // If only view width and height needed, use a ViewGroup.LayoutParams, as all the other ones inherit from this one.
//            Constructor<? extends ViewGroup.LayoutParams> ctor = view.getLayoutParams().getClass().getDeclaredConstructor(int.class, int.class);
//            view.setLayoutParams(ctor.newInstance(newWidth, newHeight));
//        } catch (Exception e) {
//            e.printStackTrace();
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
                            // Thread waking up earlier due to an interrupt and able to be relocated
                        }

                        recorder.read(sData, 0, BufferElements2Rec);
//                        Log.d("", "ValA " + sData[0]);
//                        rect1.addArray(sData);   // Add data and calculate, resulting in only 1 output at a time for FIR
//                        lpf1.addArray(rect1.getBuffer());   // Add data and calculate, resulting in only 1 output at a time for FIR
//                        lpf1.addArray(sData);
//                        hpf1.addArray(lpf1.getBuffer());

                        // Full scheme written on Readme.md
                        filCh1.addArray(sData, 21);     // HbO2
                        filCh2.addArray(sData, 21);     // Hb

//                        // Rectify then clear carrier
                        filDemod1.addArray(filCh1.getBuffer());
                        filDemod2.addArray(filCh2.getBuffer());
//
//                        // Precise filters, clearing
                        filLast1.addArray(filDemod1.getBuffer(), 10);
                        filLast2.addArray(filDemod2.getBuffer(), 10);

                        // New, separate, UI Thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // TODO Just marking
//                                addWaveArray(sData, simpleWaveformA, downSample);   // Remember addWaveArray will do zeroing

//                                addWaveArray(filCh2.getBuffer(), simpleWaveformA, downSample);
                                addWaveArray(filDemod1.getBuffer(), simpleWaveformA, downSample);
                                addWaveArray(filDemod2.getBuffer(), simpleWaveformB, downSample);

//                                addWaveData((int) lpf1.getVal(), simpleWaveformB);
                                setDebugMessages(String.valueOf(Collections.max(ampListA)), 1);
                                setDebugMessages(String.valueOf(ampListB.peekFirst()), 2);
                                setDebugMessages(String.valueOf(dataNum), 3);
                            }
                        });
                    }
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
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }

    public void addWaveArray(short[] arr, SimpleWaveform simpleWaveform, int downSample) {
        int arrSize = arr.length;
        Log.d("", "dataLength: " + String.valueOf(arrSize));
        for (int i = 0; i < arrSize; i++) {
            if (i % downSample == 0) addWaveData(arr[i], simpleWaveform);    // Add every x data
//            arr[i] = 0;     // Zeroing, thus destructive. Not really necessary based on previous tests, but create a possible problem
        }
    }

    public void addWaveArray(int[] arr, SimpleWaveform simpleWaveform, int downSample) {
        int arrSize = arr.length;
        Log.d("", "dataLength: " + String.valueOf(arrSize));
        for (int i = 0; i < arrSize; i++) {
            if (i % downSample == 0) addWaveData(arr[i], simpleWaveform); // Add every x data
        }
    }

    public void addWaveArray(double[] arr, SimpleWaveform simpleWaveform, int downSample) {
        int arrSize = arr.length;
        Log.d("", "dataLength: " + String.valueOf(arrSize));
        for (int i = 0; i < arrSize; i++) {
            if (i % downSample == 0) addWaveData((int) arr[i], simpleWaveform); // Add every x data
        }
    }

    public void addWaveData(int value, SimpleWaveform simpleWaveform) {
        // Should be called inside an UI Thread since contains View.invalidate()
        value = value * (simpleWaveform.height - 1) / MAX_AMPLITUDE;
        simpleWaveform.dataList.addFirst(value);
        if (simpleWaveform.dataList.size() > simpleWaveform.width / simpleWaveform.barGap + 2) {
            simpleWaveform.dataList.removeLast();
        }
        dataNum++;  // Increment data count
        simpleWaveform.refresh();
//        simpleWaveform.postInvalidate();  // Allow update view outside an UI Thread
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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

    // Specify the theme of every waveform
    private void amplitudeWave(SimpleWaveform simpleWaveform, LinkedList<Integer> ampList) {
        // Receive which waveform and data list

        simpleWaveform.init();

        simpleWaveform.setDataList(ampList);

        //define background
        background.setColor(Color.LTGRAY);
        simpleWaveform.background = background;

        //define bar gap
        simpleWaveform.barGap = 2;

        //define x-axis direction
        simpleWaveform.modeDirection = SimpleWaveform.MODE_DIRECTION_RIGHT_LEFT;

        //define if draw opposite pole when show bars. Doing so will make negatives as absolutes.
        simpleWaveform.modeAmp = SimpleWaveform.MODE_AMP_ORIGIN;
        //define if the unit is px or percent of the view's height
        simpleWaveform.modeHeight = SimpleWaveform.MODE_HEIGHT_PX;
        //define where is the x-axis in y-axis
        simpleWaveform.modeZero = SimpleWaveform.MODE_ZERO_CENTER;
        //if show bars?
        simpleWaveform.showBar = false;

        //define how to show peaks outline
        simpleWaveform.modePeak = SimpleWaveform.MODE_PEAK_ORIGIN;
        //if show peaks outline?
        simpleWaveform.showPeak = true;

        //show x-axis
        simpleWaveform.showXAxis = true;
        xAxisPencil.setStrokeWidth(1);
//        xAxisPencil.setColor(0x88ffffff);
        xAxisPencil.setColor(getResources().getColor(R.color.divider));
        simpleWaveform.xAxisPencil = xAxisPencil;
        //show x-axis on top of outline or under
        simpleWaveform.modePriority = SimpleWaveform.MODE_AXIS_UNDER_AMP;

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

    }

    // Copy a bunch of data into clipboard
    private static final String SEPARATOR = ", ";
    private static final String START = "[";
    private static final String END = "]";


    private void toClipboard(double[] values) {
        toClipboard(values, false);
    }

    private void toClipboard(double[] values, boolean append) {
        // Convert to linked list
        LinkedList<Integer> ll = new LinkedList<Integer>();

        int size = values.length;

        for (int i = 0; i < size; i++) {
            ll.add((int) values[i]);
        }

        toClipboard(ll, append);
    }

    private void toClipboard(LinkedList<Integer> values) {
        toClipboard(values, false);
    }

    private void toClipboard(LinkedList<Integer> values, boolean append) {
        StringBuilder textBuilder = new StringBuilder();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (append) {
            try {
                String ptext = clipboard.getPrimaryClip().getItemAt(0).getText().toString();
                textBuilder.append(ptext);
                textBuilder.append(SEPARATOR);
            } catch (NullPointerException e) {
                Log.e("", e.toString());
            }
        }

        textBuilder.append(START);

        for(int val : values) {
            textBuilder.append(String.valueOf(val));
            textBuilder.append(SEPARATOR);
        }

        textBuilder = new StringBuilder(textBuilder.substring(0, textBuilder.length() - SEPARATOR.length()));     // Remove last separator
        textBuilder.append(END);

        String text = textBuilder.toString();

        ClipData clip = ClipData.newPlainText("values", text);
        clipboard.setPrimaryClip(clip);
    }
}