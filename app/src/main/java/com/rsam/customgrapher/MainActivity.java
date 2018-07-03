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
    // LPF, 100
    private double[] b_chA = {-0.00128071641519985, -0.00484060561133978, -0.00820466685680204, -0.0100522906737678, -0.00764770663114648, -0.00200808465277356, 0.00359346521230649, 0.00516132950026595, 0.00188198831851226, -0.00300957488915340, -0.00476366844953760, -0.00159998079892119, 0.00340533137807716, 0.00509456038645052, 0.00142249659970227, -0.00412867737003608, -0.00573127395317837, -0.00123409085034894, 0.00512325791365459, 0.00659737929231598, 0.00100007931408894, -0.00637492693795900, -0.00764192147823131, -0.000648917551749832, 0.00794366620901966, 0.00886758459648604, 0.000106595707399882, -0.00993813331766532, -0.0103247712781287, 0.000703362593521595, 0.0125128452314292, 0.0120898188800701, -0.00191022519336351, -0.0159447250227631, -0.0143207695216435, 0.00373219028607513, 0.0207549358630002, 0.0173405833297656, -0.00660418436919010, -0.0280625490404788, -0.0218879456947641, 0.0115735781446730, 0.0408004603838685, 0.0301066502992276, -0.0219494001717432, -0.0697377600114892, -0.0515309165055164, 0.0569135793718046, 0.211871528145629, 0.324745780284963, 0.324745780284963, 0.211871528145629, 0.0569135793718046, -0.0515309165055164, -0.0697377600114892, -0.0219494001717432, 0.0301066502992276, 0.0408004603838685, 0.0115735781446730, -0.0218879456947641, -0.0280625490404788, -0.00660418436919010, 0.0173405833297656, 0.0207549358630002, 0.00373219028607513, -0.0143207695216435, -0.0159447250227631, -0.00191022519336351, 0.0120898188800701, 0.0125128452314292, 0.000703362593521595, -0.0103247712781287, -0.00993813331766532, 0.000106595707399882, 0.00886758459648604, 0.00794366620901966, -0.000648917551749832, -0.00764192147823131, -0.00637492693795900, 0.00100007931408894, 0.00659737929231598, 0.00512325791365459, -0.00123409085034894, -0.00573127395317837, -0.00412867737003608, 0.00142249659970227, 0.00509456038645052, 0.00340533137807716, -0.00159998079892119, -0.00476366844953760, -0.00300957488915340, 0.00188198831851226, 0.00516132950026595, 0.00359346521230649, -0.00200808465277356, -0.00764770663114648, -0.0100522906737678, -0.00820466685680204, -0.00484060561133978, -0.00128071641519985};
    // HPF, 105
    private double[] b_chB = {0.00445106168062990, -0.00584055296831605, -0.00567395687276087, 0.0139838764093434, -0.00462436930294187, -0.00108265853947083, -0.00534624102257943, 0.00268865170986349, 0.00290302257842915, 0.00293878407130534, -0.00238413353378798, -0.00382053168889934, -0.00196928984898466, 0.00297870010889952, 0.00448638044774979, 0.00119489360895104, -0.00405506617996496, -0.00496819549466891, -0.000249162227995355, 0.00540227096290473, 0.00529725381955214, -0.00106601504892039, -0.00696057121219179, -0.00532252901862865, 0.00281270455497150, 0.00863948145021063, 0.00496446661858777, -0.00506678774868126, -0.0103911881926513, -0.00408153973492650, 0.00791657165539271, 0.0121467689486580, 0.00250521647983623, -0.0114967452940243, -0.0138316768817759, 1.18107776128977e-05, 0.0160270315718867, 0.0153979574465008, -0.00390421338980133, -0.0219775880776405, -0.0167771163225775, 0.0100284762112479, 0.0304174704860984, 0.0179143539518900, -0.0205033758058906, -0.0443562688925410, -0.0187601856295152, 0.0425506567701409, 0.0759975021653204, 0.0192783370491577, -0.126885792694953, -0.284758115343217, 0.647214161529456, -0.284758115343217, -0.126885792694953, 0.0192783370491577, 0.0759975021653204, 0.0425506567701409, -0.0187601856295152, -0.0443562688925410, -0.0205033758058906, 0.0179143539518900, 0.0304174704860984, 0.0100284762112479, -0.0167771163225775, -0.0219775880776405, -0.00390421338980133, 0.0153979574465008, 0.0160270315718867, 1.18107776128977e-05, -0.0138316768817759, -0.0114967452940243, 0.00250521647983623, 0.0121467689486580, 0.00791657165539271, -0.00408153973492650, -0.0103911881926513, -0.00506678774868126, 0.00496446661858777, 0.00863948145021063, 0.00281270455497150, -0.00532252901862865, -0.00696057121219179, -0.00106601504892039, 0.00529725381955214, 0.00540227096290473, -0.000249162227995355, -0.00496819549466891, -0.00405506617996496, 0.00119489360895104, 0.00448638044774979, 0.00297870010889952, -0.00196928984898466, -0.00382053168889934, -0.00238413353378798, 0.00293878407130534, 0.00290302257842915, 0.00268865170986349, -0.00534624102257943, -0.00108265853947083, -0.00462436930294187, 0.0139838764093434, -0.00567395687276087, -0.00584055296831605, 0.00445106168062990};
    // LPF, 44
    private double[] b_demod = {-0.00107138823080546, -0.00179259606858309, -0.00306017913766237, -0.00465037742574387, -0.00644980403519705, -0.00825662052642903, -0.00978155709649607, -0.0106616343472280, -0.0104937710169349, -0.00887433062091741, -0.00545072914918859, 2.28994322808451e-05, 0.00763674491720588, 0.0172886693779846, 0.0286646011880287, 0.0412433419504978, 0.0543273395569890, 0.0670963815398339, 0.0786800291280802, 0.0882408034725863, 0.0950587075846060, 0.0986066955647925, 0.0986066955647925, 0.0950587075846060, 0.0882408034725863, 0.0786800291280802, 0.0670963815398339, 0.0543273395569890, 0.0412433419504978, 0.0286646011880287, 0.0172886693779846, 0.00763674491720588, 2.28994322808451e-05, -0.00545072914918859, -0.00887433062091741, -0.0104937710169349, -0.0106616343472280, -0.00978155709649607, -0.00825662052642903, -0.00644980403519705, -0.00465037742574387, -0.00306017913766237, -0.00179259606858309, -0.00107138823080546};
    // BPF, 15
    private double[] b_last1 = {0.00955613205306125, 0.0255097891878899, 0.0115748202481036, -0.0488438235882847, -0.0711802934651547, 0.0610361304309456, 0.301039202146836, 0.425727225185302, 0.301039202146836, 0.0610361304309456, -0.0711802934651547, -0.0488438235882847, 0.0115748202481036, 0.0255097891878899, 0.00955613205306125};
    // BPF, 51
    private double[] b_last2 = {0.00412053114641298, -0.0312793191356909, -0.0115236109767417, -0.0100213992087269, -0.0108385904761499, -0.0119734717946732, -0.0131685337169099, -0.0143842189071186, -0.0156078845365078, -0.0168299462147875, -0.0180409785596737, -0.0192314427835679, -0.0203917450600907, -0.0215123386063274, -0.0225838323470475, -0.0235970999596655, -0.0245433871199924, -0.0254144153854427, -0.0262024812734020, -0.0269005491468212, -0.0275023365897874, -0.0280023910540817, -0.0283961566788859, -0.0286800303246559, -0.0288514060155109, 0.971091292850008, -0.0288514060155109, -0.0286800303246559, -0.0283961566788859, -0.0280023910540817, -0.0275023365897874, -0.0269005491468212, -0.0262024812734020, -0.0254144153854427, -0.0245433871199924, -0.0235970999596655, -0.0225838323470475, -0.0215123386063274, -0.0203917450600907, -0.0192314427835679, -0.0180409785596737, -0.0168299462147875, -0.0156078845365078, -0.0143842189071186, -0.0131685337169099, -0.0119734717946732, -0.0108385904761499, -0.0100213992087269, -0.0115236109767417, -0.0312793191356909, 0.00412053114641298};

    // Downsampling rates
    private final int DS0 = 90; // 490 Hz (1/90 from before)
    private final int DS1 = 1;  // 490 Hz (1/1 from before, 1/90 total)
    private final int DS2 = 10;  // 70 Hz (1/7 from before, 1/630 total)
    private final int DS3 = 1;  // 70 Hz (1/1 from before, 1/630 total)

    // The filters, one separate system for each channel
    // Buffer MUST be proportional to sampling rate, with full size BufferElements2Rec is used for REC_RATE
    private Filter filChA = new Filter(b_chA.length, b_chA, BufferElements2Rec / (DS0), false);
    private Filter filChB = new Filter(b_chB.length, b_chB, BufferElements2Rec / (DS0), false);
    private Filter filDemodA = new Filter(b_demod.length, b_demod, BufferElements2Rec / (DS0 * DS1), true);
    private Filter filDemodB = new Filter(b_demod.length, b_demod, BufferElements2Rec / (DS0 * DS1), true);
    private Filter filLast1A = new Filter(b_last1.length, b_last1, BufferElements2Rec / (DS0 * DS1 * DS2), false);
    private Filter filLast1B = new Filter(b_last1.length, b_last1, BufferElements2Rec / (DS0 * DS1 * DS2), false);
    private Filter filLast2A = new Filter(b_last2.length, b_last2, BufferElements2Rec / (DS0 * DS1 * DS2), false);
    private Filter filLast2B = new Filter(b_last2.length, b_last2, BufferElements2Rec / (DS0 * DS1 * DS2), false);

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
                        filChA.addArray(sData, DS0);     // HbO2
                        filChB.addArray(sData, DS0);     // Hb

                        // Rectify then clear carrier
                        filDemodA.addArray(filChA.getBuffer(), DS1);
                        filDemodB.addArray(filChB.getBuffer(), DS1);

                        // Precise filters, clearing
                        filLast1A.addArray(filDemodA.getBuffer(), DS2);
                        filLast1B.addArray(filDemodB.getBuffer(), DS2);

                        filLast2A.addArray(filLast1A.getBuffer(), DS3);
                        filLast2B.addArray(filLast1B.getBuffer(), DS3);

                        // New, separate, UI Thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // TODO Just a marking
//                                addWaveArray(filLast2A.getBuffer(), simpleWaveformA, downSample);
//                                addWaveArray(filLast2B.getBuffer(), simpleWaveformB, downSample);

                                addWaveArray(filChA.getBuffer(), simpleWaveformA, downSample);
                                addWaveArray(filLast1A.getBuffer(), simpleWaveformB, downSample);
                                
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