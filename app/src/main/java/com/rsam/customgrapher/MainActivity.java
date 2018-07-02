package com.rsam.customgrapher;

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
    private static final int waveListMulti = 5;         // Multiplier for the waveform list size, TODO reduce on release

    private static final int REC_RATE = 44100;
    private static final int REC_CH = AudioFormat.CHANNEL_IN_MONO;
    private static final int REC_AUDIO_ENC = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private static final int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private static final int BytesPerElement = 2;       // 2 bytes in 16bit format
    private static long dataNum = 0;                    // Keep the data count, beware it'll overflow so use the difference if necessary

    // Filters, specified based on the note on Readme.md
    // LPF
    private double[] b_chA = {-0.000472124578466290, 0.000704149548352154, 0.00274189310474332, 0.00591248887896805, 0.00921257087324632, 0.0109695262713906, 0.00953038922526930, 0.00428143311355899, -0.00353878822093340, -0.0108376853965620, -0.0137750375665968, -0.00969532462583917, 0.000997797813952307, 0.0141295296141812, 0.0229572420560878, 0.0211513757921862, 0.00639861039263159, -0.0171381544555205, -0.0390535049402061, -0.0460804182276977, -0.0275733801766756, 0.0191691643281890, 0.0862485232208061, 0.156732800488558, 0.210190627040545, 0.230113426289257, 0.210190627040545, 0.156732800488558, 0.0862485232208061, 0.0191691643281890, -0.0275733801766756, -0.0460804182276977, -0.0390535049402061, -0.0171381544555205, 0.00639861039263159, 0.0211513757921862, 0.0229572420560878, 0.0141295296141812, 0.000997797813952307, -0.00969532462583917, -0.0137750375665968, -0.0108376853965620, -0.00353878822093340, 0.00428143311355899, 0.00953038922526930, 0.0109695262713906, 0.00921257087324632, 0.00591248887896805, 0.00274189310474332, 0.000704149548352154, -0.000472124578466290}; // 100
    // HPF
    private double[] b_chB = {0.00699933507837955, -0.0149678167519151, 0.00311607835569475, 0.00803885630276995, 0.00422418129212178, -0.00398720213516828, -0.00896571444921871, -0.00466568580611676, 0.00613277494422169, 0.0123814810399314, 0.00538303699854521, -0.0100357111631056, -0.0176506668652045, -0.00608382389356992, 0.0161895058352412, 0.0255041559244084, 0.00662759144169743, -0.0267030901037898, -0.0389968229048643, -0.00706386098623202, 0.0487314683840281, 0.0702835890365723, 0.00732055553146396, -0.133003290106314, -0.278800975941692, 0.659254180333660, -0.278800975941692, -0.133003290106314, 0.00732055553146396, 0.0702835890365723, 0.0487314683840281, -0.00706386098623202, -0.0389968229048643, -0.0267030901037898, 0.00662759144169743, 0.0255041559244084, 0.0161895058352412, -0.00608382389356992, -0.0176506668652045, -0.0100357111631056, 0.00538303699854521, 0.0123814810399314, 0.00613277494422169, -0.00466568580611676, -0.00896571444921871, -0.00398720213516828, 0.00422418129212178, 0.00803885630276995, 0.00311607835569475, -0.0149678167519151, 0.00699933507837955}; // 105
    // LPF
    private double[] b_demod = {0.0361709582435732, 0.167378115547300, 0.320696613312794, 0.320696613312794, 0.167378115547300, 0.0361709582435732}; // 6
    // BPF
    private double[] b_last = {-0.0103816324997861, 0.00941733023553125, 0.00778074474363915, 0.00630257546869511, 0.00391737263024000, 0.000533098936603738, -0.00308943184561313, -0.00583639990674106, -0.00682049074815435, -0.00589222409738391, -0.00374070692133041, -0.00159662966009040, -0.000627075974684462, -0.00136492785376063, -0.00342071922504081, -0.00569960005133977, -0.00694574170366219, -0.00643750604269774, -0.00436843725958440, -0.00180497662647091, -0.000128305799069152, -0.000325820301048316, -0.00242102351761146, -0.00543400981049922, -0.00783472110229037, -0.00836160667306464, -0.00668527131179506, -0.00366037873617713, -0.000902881462672520, 1.38978607706233e-05, -0.00157453005047104, -0.00503458083777731, -0.00868549410949335, -0.0106598504896305, -0.00987037359707693, -0.00666580639621833, -0.00269760069998585, -0.000137868695786707, -0.000539251521013433, -0.00391698293106959, -0.00877751068092712, -0.0126106530482791, -0.0133984194752545, -0.0106147240106826, -0.00558536104815296, -0.000954611886443581, 0.000678529389677889, -0.00185685139822272, -0.00755996540877891, -0.0136516417282856, -0.0169464070939377, -0.0155294562032163, -0.00987595253977428, -0.00278924338550945, 0.00194541894538892, 0.00149268532684179, -0.00440325310498845, -0.0130893422998723, -0.0201558338694301, -0.0216721959882212, -0.0163277122105223, -0.00640641264924913, 0.00310607054795360, 0.00690264001277833, 0.00220086088236234, -0.00950435363951841, -0.0227128150735150, -0.0304006121638387, -0.0275647711496575, -0.0142000467793228, 0.00398163823690016, 0.0176726744492332, 0.0183096860325943, 0.00267444934798747, -0.0243643895441033, -0.0506438672341385, -0.0609979069710528, -0.0435787139376426, 0.00444859206400019, 0.0744348519789470, 0.148222765621028, 0.204201489646091, 0.225064221501081, 0.204201489646091, 0.148222765621028, 0.0744348519789470, 0.00444859206400019, -0.0435787139376426, -0.0609979069710528, -0.0506438672341385, -0.0243643895441033, 0.00267444934798747, 0.0183096860325943, 0.0176726744492332, 0.00398163823690016, -0.0142000467793228, -0.0275647711496575, -0.0304006121638387, -0.0227128150735150, -0.00950435363951841, 0.00220086088236234, 0.00690264001277833, 0.00310607054795360, -0.00640641264924913, -0.0163277122105223, -0.0216721959882212, -0.0201558338694301, -0.0130893422998723, -0.00440325310498845, 0.00149268532684179, 0.00194541894538892, -0.00278924338550945, -0.00987595253977428, -0.0155294562032163, -0.0169464070939377, -0.0136516417282856, -0.00755996540877891, -0.00185685139822272, 0.000678529389677889, -0.000954611886443581, -0.00558536104815296, -0.0106147240106826, -0.0133984194752545, -0.0126106530482791, -0.00877751068092712, -0.00391698293106959, -0.000539251521013433, -0.000137868695786707, -0.00269760069998585, -0.00666580639621833, -0.00987037359707693, -0.0106598504896305, -0.00868549410949335, -0.00503458083777731, -0.00157453005047104, 1.38978607706233e-05, -0.000902881462672520, -0.00366037873617713, -0.00668527131179506, -0.00836160667306464, -0.00783472110229037, -0.00543400981049922, -0.00242102351761146, -0.000325820301048316, -0.000128305799069152, -0.00180497662647091, -0.00436843725958440, -0.00643750604269774, -0.00694574170366219, -0.00569960005133977, -0.00342071922504081, -0.00136492785376063, -0.000627075974684462, -0.00159662966009040, -0.00374070692133041, -0.00589222409738391, -0.00682049074815435, -0.00583639990674106, -0.00308943184561313, 0.000533098936603738, 0.00391737263024000, 0.00630257546869511, 0.00778074474363915, 0.00941733023553125, -0.0103816324997861}; // 165

    // Downsampling rates
    private final int DS0 = 90; // 490 Hz (1/90 from before)
    private final int DS1 = 1;  // 490 Hz (1/1 from before, 1/90 total)
    private final int DS2 = 7;  // 70 Hz (1/7 from before, 1/630 total)

    // The filters, one separate system for each channel
    // Buffer MUST be proportional to sampling rate, with full size BufferElements2Rec is used for REC_RATE
    private Filter filChA = new Filter(100, b_chA, BufferElements2Rec / (DS0), false);
    private Filter filChB = new Filter(105, b_chB, BufferElements2Rec / (DS0), false);
    private Filter filDemodA = new Filter(6, b_demod, BufferElements2Rec / (DS0 * DS1), true);
    private Filter filDemodB = new Filter(6, b_demod, BufferElements2Rec / (DS0 * DS1), true);
    private Filter filLastA = new Filter(165, b_last, BufferElements2Rec / (DS0 * DS1 * DS2), false);
    private Filter filLastB = new Filter(165, b_last, BufferElements2Rec / (DS0 * DS1 * DS2), false);

    // Debugging for LOGCAT
    private static final String TAG = "MainActivity";

    // Variables for settings menu and their initial conditions
    private static int downSample = 1;                  // Get every x-th sample, also a [settings]
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
    // Near-perfect demodulation scheme
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

        int bufferSize = AudioRecord.getMinBufferSize(REC_RATE,
                REC_CH, REC_AUDIO_ENC);

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
                toClipboard(filDemodA.getBuffer(), false);
                toClipboard(filDemodB.getBuffer(), true);
                doRun = true;
                startRecording();
            } else {
                toClipboard(filDemodA.getBuffer(), false);
                toClipboard(filDemodB.getBuffer(), true);
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
                    REC_RATE, REC_CH,
                    REC_AUDIO_ENC, BufferElements2Rec * BytesPerElement);

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

                        // Full scheme written on Readme.md
                        filChA.addArray(sData, 21);     // HbO2
                        filChB.addArray(sData, 21);     // Hb

                        // Rectify then clear carrier
                        filDemodA.addArray(filChA.getBuffer());
                        filDemodB.addArray(filChB.getBuffer());

                        // Precise filters, clearing
                        filLastA.addArray(filDemodA.getBuffer(), 30);
                        filLastB.addArray(filDemodB.getBuffer(), 30);

                        // New, separate, UI Thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // TODO Just a marking
//                                addWaveArray(filLastA.getBuffer(), simpleWaveformA, downSample);
//                                addWaveArray(filLastB.getBuffer(), simpleWaveformB, downSample);

                                addWaveArray(sData, simpleWaveformA, downSample);
                                addWaveArray(filDemodA.getBuffer(), simpleWaveformB, downSample);
                                
                                setDebugMessages(String.valueOf(simpleWaveformB.absMax) + " / " +
                                                        String.valueOf(simpleWaveformB.absMaxIndex), 1);
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
        value = value * (simpleWaveform.height - 1) / MAX_AMPLITUDE;    // Normalize audio max-min to waveform height
        simpleWaveform.dataList.addFirst(value);

        while (simpleWaveform.dataList.size() > simpleWaveform.width / simpleWaveform.barGap * waveListMulti + 2) {
            // Wave list multi used to make the list contains more data than necessary, debugging & data acquiring purpose
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

        //define the full height range normalization
        simpleWaveform.modeNormal = SimpleWaveform.MODE_NORMAL_MAX; // Set full height as 2*amplitude

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
//    private static final String SEPARATOR = ", ";
    private static final String SEPARATOR = ",";
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