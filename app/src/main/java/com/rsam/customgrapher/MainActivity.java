package com.rsam.customgrapher;

import java.util.Calendar;
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
import android.view.KeyEvent;
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
    private static final int waveListMulti = 2;         // Multiplier for the waveform list size, TODO reduce on release

    private static final int REC_RATE = 44100;
    private static final int REC_CH = AudioFormat.CHANNEL_IN_MONO;
    private static final int REC_AUDIO_ENC = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private int bufferSize = 0;
    private static final int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    private static final int BytesPerElement = 2;       // 2 bytes in 16bit format
    private static long dataNum = 0;                    // Keep the data count, beware it'll overflow so use the difference if necessary

    // Filters, specified based on the note on Readme.md
    // LPF, 111
    private double[] b_aa1 = {-0.000331550026303021, -0.000372265324462671, -0.000418866922856345, -0.000472627729976633, -0.000534420609562197, -0.000604643759698580, -0.000683155014016682, -0.000769217007982752, -0.000861454956952813, -0.000957828553012607, -0.00105561920917520, -0.00115143356771268, -0.00124122385051423, -0.00132032527032998, -0.00138351035005506, -0.00142505962069058, -0.00143884779535902, -0.00141844415486750, -0.00135722553778236, -0.00124850001247768, -0.00108563902734299, -0.000862215594841029, -0.000572145871167061, -0.000209831350731869, 0.000229701192607173, 0.000750664915009880, 0.00135638613018885, 0.00204918833262761, 0.00283028964561548, 0.00369971611828263, 0.00465623303784079, 0.00569729611682538, 0.00681902406595216, 0.00801619367785358, 0.00928225813287758, 0.0106093888034130, 0.0119885403864577, 0.0134095387442971, 0.0148611903892978, 0.0163314121199594, 0.0178073789103221, 0.0192756877819606, 0.0207225350548884, 0.0221339040877727, 0.0234957603850154, 0.0247942507735458, 0.0260159032395069, 0.0271478239670524, 0.0281778881395663, 0.0290949211477954, 0.0298888669983320, 0.0305509409269555, 0.0310737634906417, 0.0314514737344825, 0.0316798193991326, 0.0317562225435901, 0.0316798193991326, 0.0314514737344825, 0.0310737634906417, 0.0305509409269555, 0.0298888669983320, 0.0290949211477954, 0.0281778881395663, 0.0271478239670524, 0.0260159032395069, 0.0247942507735458, 0.0234957603850154, 0.0221339040877727, 0.0207225350548884, 0.0192756877819606, 0.0178073789103221, 0.0163314121199594, 0.0148611903892978, 0.0134095387442971, 0.0119885403864577, 0.0106093888034130, 0.00928225813287758, 0.00801619367785358, 0.00681902406595216, 0.00569729611682538, 0.00465623303784079, 0.00369971611828263, 0.00283028964561548, 0.00204918833262761, 0.00135638613018885, 0.000750664915009880, 0.000229701192607173, -0.000209831350731869, -0.000572145871167061, -0.000862215594841029, -0.00108563902734299, -0.00124850001247768, -0.00135722553778236, -0.00141844415486750, -0.00143884779535902, -0.00142505962069058, -0.00138351035005506, -0.00132032527032998, -0.00124122385051423, -0.00115143356771268, -0.00105561920917520, -0.000957828553012607, -0.000861454956952813, -0.000769217007982752, -0.000683155014016682, -0.000604643759698580, -0.000534420609562197, -0.000472627729976633, -0.000418866922856345, -0.000372265324462671, -0.000331550026303021};
    // LPF, 61
    private double[] b_aa2 = {-0.000508508009580982, -0.000903019197774497, -0.000444894280259791, 0.000660373417926351, 0.00146789782188540, 0.000886317422313322, -0.00109419558995878, -0.00274535224793622, -0.00183573312291301, 0.00177603631600603, 0.00491889824185567, 0.00354183667911377, -0.00264446143357028, -0.00822680861941409, -0.00635400327967241, 0.00361667802495594, 0.0130566204516626, 0.0108449154075161, -0.00459713841200943, -0.0202030322638191, -0.0181732743674832, 0.00548767495926264, 0.0317081384961672, 0.0314018898476090, -0.00619804290227997, -0.0547135280792258, -0.0627723490822906, 0.00665569055734947, 0.139912766990054, 0.271950880625500, 0.327055451258022, 0.271950880625500, 0.139912766990054, 0.00665569055734947, -0.0627723490822906, -0.0547135280792258, -0.00619804290227997, 0.0314018898476090, 0.0317081384961672, 0.00548767495926264, -0.0181732743674832, -0.0202030322638191, -0.00459713841200943, 0.0108449154075161, 0.0130566204516626, 0.00361667802495594, -0.00635400327967241, -0.00822680861941409, -0.00264446143357028, 0.00354183667911377, 0.00491889824185567, 0.00177603631600603, -0.00183573312291301, -0.00274535224793622, -0.00109419558995878, 0.000886317422313322, 0.00146789782188540, 0.000660373417926351, -0.000444894280259791, -0.000903019197774497, -0.000508508009580982};
    // LPF, 26
    private double[] b_chA = {-0.00572225374043716, -0.0180335270633584, -0.0145649419292811, 0.0116973249207995, 0.0183777359746803, -0.0181837880487599, -0.0285567261790708, 0.0300597733591670, 0.0470324226228558, -0.0551731175089687, -0.0904225921812429, 0.143330292058622, 0.453920753391290, 0.453920753391290, 0.143330292058622, -0.0904225921812429, -0.0551731175089687, 0.0470324226228558, 0.0300597733591670, -0.0285567261790708, -0.0181837880487599, 0.0183777359746803, 0.0116973249207995, -0.0145649419292811, -0.0180335270633584, -0.00572225374043716};
    // HPF, 28
    private double[] b_chB = {-0.00164282664975775, 0.00207019168002476, 0.00111890391620409, -0.00977838741500030, 0.0204291864474586, -0.0238175943647322, 0.0107128541634235, 0.0181835143120806, -0.0461294646359265, 0.0458759643436551, 0.00326089656488181, -0.0973168723245041, 0.202989178180561, -0.272918435001176, 0.272918435001176, -0.202989178180561, 0.0973168723245041, -0.00326089656488181, -0.0458759643436551, 0.0461294646359265, -0.0181835143120806, -0.0107128541634235, 0.0238175943647322, -0.0204291864474586, 0.00977838741500030, -0.00111890391620409, -0.00207019168002476, 0.00164282664975775};
    // LPF, 83
    private double[] b_demod = {0.00521307083722403, 0.000698382709468956, 0.000594567165180754, 0.000375656318506526, 3.18131431167444e-05, -0.000438977696559757, -0.00103735715375136, -0.00175457525810610, -0.00258014129427819, -0.00349079605439242, -0.00446055075397255, -0.00545546289730373, -0.00643439860207394, -0.00735233193193724, -0.00816002725051333, -0.00880412495686340, -0.00923301280954544, -0.00939124400075369, -0.00923139737922087, -0.00870375195193797, -0.00777445615697063, -0.00640778349255515, -0.00458841946948496, -0.00230972281863533, 0.000425654267619883, 0.00359756997617825, 0.00717240545217001, 0.0111075306342898, 0.0153409901769187, 0.0198025682849520, 0.0243961979307804, 0.0290575741777475, 0.0336733021199335, 0.0381445832112929, 0.0423816387715489, 0.0462793701104400, 0.0497533865671056, 0.0527158338575922, 0.0551003969180422, 0.0568449034965347, 0.0579099127475191, 0.0582673562111828, 0.0579099127475191, 0.0568449034965347, 0.0551003969180422, 0.0527158338575922, 0.0497533865671056, 0.0462793701104400, 0.0423816387715489, 0.0381445832112929, 0.0336733021199335, 0.0290575741777475, 0.0243961979307804, 0.0198025682849520, 0.0153409901769187, 0.0111075306342898, 0.00717240545217001, 0.00359756997617825, 0.000425654267619883, -0.00230972281863533, -0.00458841946948496, -0.00640778349255515, -0.00777445615697063, -0.00870375195193797, -0.00923139737922087, -0.00939124400075369, -0.00923301280954544, -0.00880412495686340, -0.00816002725051333, -0.00735233193193724, -0.00643439860207394, -0.00545546289730373, -0.00446055075397255, -0.00349079605439242, -0.00258014129427819, -0.00175457525810610, -0.00103735715375136, -0.000438977696559757, 3.18131431167444e-05, 0.000375656318506526, 0.000594567165180754, 0.000698382709468956, 0.00521307083722403};
    // HPF, 51
    private double[] b_last1 = {0.00412053114641298, -0.0312793191356909, -0.0115236109767417, -0.0100213992087269, -0.0108385904761499, -0.0119734717946732, -0.0131685337169099, -0.0143842189071186, -0.0156078845365078, -0.0168299462147875, -0.0180409785596737, -0.0192314427835679, -0.0203917450600907, -0.0215123386063274, -0.0225838323470475, -0.0235970999596655, -0.0245433871199924, -0.0254144153854427, -0.0262024812734020, -0.0269005491468212, -0.0275023365897874, -0.0280023910540817, -0.0283961566788859, -0.0286800303246559, -0.0288514060155109, 0.971091292850008, -0.0288514060155109, -0.0286800303246559, -0.0283961566788859, -0.0280023910540817, -0.0275023365897874, -0.0269005491468212, -0.0262024812734020, -0.0254144153854427, -0.0245433871199924, -0.0235970999596655, -0.0225838323470475, -0.0215123386063274, -0.0203917450600907, -0.0192314427835679, -0.0180409785596737, -0.0168299462147875, -0.0156078845365078, -0.0143842189071186, -0.0131685337169099, -0.0119734717946732, -0.0108385904761499, -0.0100213992087269, -0.0115236109767417, -0.0312793191356909, 0.00412053114641298};
    // LPF, 11
//    private double[] b_bpm = {0.00968019297323327, -0.0466578697088982, -0.0689306524089743, 0.0580759672488941, 0.302780871640028, 0.432601852726898, 0.302780871640028, 0.0580759672488941, -0.0689306524089743, -0.0466578697088982, 0.00968019297323327};

    // Downsampling rates with the data before/after
    private int fsdev = 1;  // Multiplier to compensate the difference in sampling rate
    // Raw
    private final int DS0 = 1;  // 44100 Hz (1/1 from before)
    // First anti-aliasing result
    private final int DS1 = 30; // 1470 Hz (1/30 from before, 1/30 total)
    // Second anti-aliasing result
    // Anti-aliased data
    private final int DS2 = 3;  // 490 Hz (1/3 from before, 1/90 total)
    // Channel separator result
    private final int DS3 = 2;  // 245 Hz (1/2 from before, 1/180 total)
    // Edge detector result
    private final int DS4 = 5;  // 49 Hz (1/5 from before, 1/900 total)
    // Offset suppressor result
    // Final data
    private final int DS5 = 7;  // 7 Hz (1/7 from before, 1/6300 total)
    // Simplified for BPM calculation

    private final int MAXINDEX = (DS1 * DS2 * DS3 * DS4);       // Used to reset the global iterator, avoiding overflow
    int n = 1;      // Global index

    // The filters, one separate system for each channel
    // Buffer MUST be proportional to sampling rate, with full size BufferElements2Rec is used for REC_RATE
    private Filter filaa1 = new Filter(b_aa1.length, b_aa1, BufferElements2Rec, false);
    private Filter filaa2 = new Filter(b_aa2.length, b_aa2, BufferElements2Rec / (DS1), false);
    private Filter filChA = new Filter(b_chA.length, b_chA, BufferElements2Rec / (DS1 * DS2), false);
    private Filter filChB = new Filter(b_chB.length, b_chB, BufferElements2Rec / (DS1 * DS2), false);
    private Filter filDemodA = new Filter(b_demod.length, b_demod, BufferElements2Rec / (DS1 * DS2 * DS3), true);
    private Filter filDemodB = new Filter(b_demod.length, b_demod, BufferElements2Rec / (DS1 * DS2 * DS3), true);
    private Filter filLast1A = new Filter(b_last1.length, b_last1, BufferElements2Rec / (DS1 * DS2 * DS3 * DS4), false);
    private Filter filLast1B = new Filter(b_last1.length, b_last1, BufferElements2Rec / (DS1 * DS2 * DS3 * DS4), false);
//    private Filter filLast2A = new Filter(b_last2.length, b_last2, BufferElements2Rec / (DS1 * DS2 * DS3 * DS4 * DS5), false);
//    private Filter filLast2B = new Filter(b_last2.length, b_last2, BufferElements2Rec / (DS1 * DS2 * DS3 * DS4 * DS5), false);
//    private Filter filBPM = new Filter(b_bpm.length, b_bpm, BufferElements2Rec / (DS1 * DS2 * DS3 * DS4 * DS5), false);

    // Debugging for LOGCAT
    private static final String TAG = "MainActivity";

    // Variables for settings menu and their initial conditions
    private static int downSample = 1;      // Get every x-th sample, also a [settings]
    private static int setGap = 2;          // The gap between samples in the waveform
    public static boolean setBPM = true;    // Settings for BPM calculation
    public static boolean setSPO2 = true;   // Settings for BPM calculation
    public static int setOrientation = 0;   // Settings for orientation
    public static boolean setDebug = false; // Settings for debug message
    public static boolean setReset = false; // Settings for reset settings
//	public static float zoomHor = 1;		// Settings for waveform horizontal scale
//	public static float zoomVer = 1;		// Settings for waveform vertical scale
    public static boolean doRun = true;     // Run/Pause functionality
    public static boolean fabState = true;  // Preserving floating button state on pause

    TextView tBPM;
    TextView tSPO2;

    private static final int BPMTH = -1000; // Cross-over threshold for BPM calculation
    private int cross = 0;              // Zero-crossing counter
    private long peakTime = 0;          // Time tracker
    private double peakVal = -999;      // Peak of a single wave
    private double pvalue = 0;
    private int bpm = 0;

    private boolean rising = true;      // Rising past a certain threshold (default at zero)

    private String empty = "-";

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
        simpleWaveformA.id = 1;

        simpleWaveformB = findViewById(R.id.simpleWaveformB);
        simpleWaveformB.setVisibility(View.VISIBLE);
        simpleWaveformB.id = 2;

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

        bufferSize = AudioRecord.getMinBufferSize(REC_RATE,
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
                toClipboard(filChA.getBuffer(), false);
                toClipboard(filDemodA.getBuffer(), true);
                toClipboard(filLast1A.getBuffer(), true);
                toClipboard(filLast1B.getBuffer(), true);
                doRun = true;
                startRecording();
            } else {
                toClipboard(filChA.getBuffer(), false);
                toClipboard(filDemodA.getBuffer(), true);
                toClipboard(filLast1A.getBuffer(), true);
                toClipboard(filLast1B.getBuffer(), true);
            }

            Toast.makeText(MainActivity.this, getString(R.string.toast_copy),
                    Toast.LENGTH_SHORT).show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void applySettings() {
        Log.d(TAG, "applySettings");

        // Get Shared Preferences under the default name "com.example.something_preferences"
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);

        // Get the values
        // Def value here is the value if no value found for specified key (including xml default val)
        setBPM = sharedPref.getBoolean("switch-bpm", true);
        setSPO2 = sharedPref.getBoolean("switch-spo2", true);
        setOrientation = Integer.parseInt(sharedPref.getString("list-orientation", "0"));
        setDebug = sharedPref.getBoolean("switch-debug", false);
        downSample = Integer.parseInt(sharedPref.getString("value-downsample", "1"));
        setReset = sharedPref.getBoolean("switch-reset", false);
        setGap = Integer.parseInt(sharedPref.getString("value-gap", "2"));

        // Validation
        if (setGap <= 0) setGap = 1;
        if (downSample <= 0) downSample = 1;


        simpleWaveformA.barGap = setGap;
        simpleWaveformB.barGap = setGap;

        if (setReset) {
            // Force the settings to revert to xml default values
            Log.d(TAG, "Reset");
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
////            Log.d(TAG, "portrait");
//            // Since the height is based on weight, height must be zero, width MATCH_PARENT
//        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
////            layoutWave.setOrientation(LinearLayout.HORIZONTAL);
////            Log.d(TAG, "landscape");
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


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
//                if (action == KeyEvent.ACTION_DOWN) {
//                    // Do something on press
//                }
                // Do nothing
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
//                if (action == KeyEvent.ACTION_DOWN) {
//                    // Do something on press
//                }
                // Do nothing
                return true;
            case KeyEvent.KEYCODE_HOME:
//                if (action == KeyEvent.ACTION_DOWN) {
//                    // Do something on press
//                }
                // Do nothing
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    private void startRecording() {

        final short sData[] = new short[BufferElements2Rec];

        try {
            // Default configuration which guaranteed to work by the docs (though actually not for low-ends)
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    REC_RATE, REC_CH,
                    REC_AUDIO_ENC, BufferElements2Rec * BytesPerElement);

            // Not compatible, decided to seek different configs
            if (recorder.getState() == AudioRecord.STATE_UNINITIALIZED) {
                recorder = findAudioRecord();
            }

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
                        // Anti-aliasing, dual stage
                        filaa1.addArray(sData, n, DS0 / fsdev);
                        filaa2.addArray(filaa1.getBuffer(), n, DS1);

                        filChA.addArray(filaa2.getBuffer(), n, DS2);     // HbO2
                        filChB.addArray(filaa2.getBuffer(), n, DS2);     // Hb

                        // Rectify then clear carrier
                        filDemodA.addArray(filChA.getBuffer(), n, DS3);
                        filDemodB.addArray(filChB.getBuffer(), n, DS3);

                        // Precise filters, cleaning and/or removing offset
                        filLast1A.addArray(filDemodA.getBuffer(), n, DS4);
                        filLast1B.addArray(filDemodB.getBuffer(), n, DS4);

//                        filLast2A.addArray(filLast1A.getBuffer(), DS5);
//                        filLast2B.addArray(filLast1B.getBuffer(), DS5);

                        // Very simple waveform for BPM calculation
//                        filBPM.addArray(filLast1A.getBuffer(), DS5);
//                        calculateBPM(filLast1A.getBuffer());

                        // Iterate global index and reset continuously
                        // The unawareness of global index will affect greatly on filters acquiring short buffers from prev one
                        // Ex: Input as a buffer with size of 1 means every data is sampled without factoring downsample ratio
                        if (n < (BufferElements2Rec * MAXINDEX)) {
                            // Since it'll be assured that for every loop, sData'll be fully processed
                            n += sData.length;
                        } else {
                            n = 1;
                        }

                        // New, separate, UI Thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // TODO Just a marking
//                                addWaveArray(filLast2A.getBuffer(), simpleWaveformA, downSample);
//                                addWaveArray(filLast2B.getBuffer(), simpleWaveformB, downSample);

//                                addWaveArray(filLast1A.getBuffer(), simpleWaveformA, downSample);
//                                addWaveArray(filLast1B.getBuffer(), simpleWaveformB, downSample);

                                addWaveArray(filLast1A.getBuffer(), simpleWaveformA, downSample);
                                addWaveData(bpm * 1000, simpleWaveformB);
//                                addWaveArray(sData, simpleWaveformB, downSample);

                                setSPO2((int) peakVal);
                                setBPM(bpm);

                                setDebugMessages(String.valueOf(simpleWaveformA.absMax) + " / " +
                                                        String.valueOf(simpleWaveformA.absMaxIndex), 1);
                                setDebugMessages(String.valueOf(ampListA.peekFirst()), 2);
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
//        Log.d(TAG, "dataLength: " + String.valueOf(arrSize));

        for (int i = 0; i < arrSize; i++) {
            if (i % downSample == 0) addWaveData(arr[i] * 128, simpleWaveform);    // Add every x data
//            arr[i] = 0;     // Zeroing, thus destructive. Not really necessary based on previous tests, but create a possible problem
        }
    }

    public void addWaveArray(int[] arr, SimpleWaveform simpleWaveform, int downSample) {
        int arrSize = arr.length;
//        Log.d(TAG, "dataLength: " + String.valueOf(arrSize));

        for (int i = 0; i < arrSize; i++) {
            if (i % downSample == 0) addWaveData(arr[i] * 128, simpleWaveform); // Add every x data
        }
    }

    public void addWaveArray(double[] arr, SimpleWaveform simpleWaveform, int downSample) {
        int arrSize = arr.length;
//        Log.d(TAG, "id " + simpleWaveform.id);
//        Log.d(TAG, "dataLength: " + String.valueOf(arrSize));

        for (int i = 0; i < arrSize; i++) {
            if (i % downSample == 0) {
                addWaveData((int) arr[i] * 128, simpleWaveform); // Add every x data
                if ((simpleWaveform.id == 1) && (setBPM)) { calculateBPM(arr[i]); }
            }
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

    private long tempTime = -1;
    private double tempVal = -1;

    // TODO editable moving average for BPM
    private void calculateBPM(double value) {
        // Check if rising past a certain threshold
//        Log.d(TAG, String.valueOf(pvalue));

        if ((pvalue < BPMTH) && (value > BPMTH)) {
//            Log.d(TAG, "Wav Rise");
            rising = true;
        }

        // Search for peak in a single wave
        if (rising) {
            if (value > tempVal) {
                tempVal = value;
                tempTime = Calendar.getInstance().getTimeInMillis();
                // At the end, value & time of current wave peak acquired
            }
        }

        // Check if falling past a certain threshold
        if ((pvalue > BPMTH) && (value < BPMTH)) {
            // Finalize data, which is displayed on GUI
//            Log.d(TAG, "Wav Fall");
            rising = false;

            peakVal = tempVal;
            tempVal = -1;

            if (tempTime != peakTime) {
                bpm = 60000 / (int)(tempTime - peakTime);    // Calculate using prev peakTime
            }
            peakTime = tempTime;

//            Log.d(TAG, "Wav peak" + String.valueOf(peakVal) + " " + String.valueOf(bpm));
        }

        // Only show empty if invalid
        if ((bpm < 0) || (bpm > 200)) {
            bpm = -1;
        }

        if ((peakVal < 0) || (peakVal > 9999)) {
            peakVal = -1;
        }

        pvalue = value;
    }

    private void setBPM(int value) {
        if (value == -1) {
            // Invalid calculation
            tBPM.setText(empty);
        } else {
            tBPM.setText(String.format("%d", value));
        }
    }

    private void setSPO2(int value) {
        if (value == -1) {
            // Invalid calculation
            tSPO2.setText(empty);
        } else {
            tSPO2.setText(String.format("%d", value));
        }
    }

//    public void setPower(int valAvg, int valPeak) {
//        // Power texts controller
//        TextView text;
//
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
        simpleWaveform.barGap = setGap;

        //define the full height range normalization
        simpleWaveform.modeNormal = SimpleWaveform.MODE_NORMAL_VALUE_MAX; // Set full height as 2*amplitude

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

    private void toClipboard(short[] values, boolean append) {
        // Convert to linked list
        LinkedList<Integer> ll = new LinkedList<Integer>();

        int size = values.length;

        for (int i = 0; i < size; i++) {
            ll.add((int) values[i]);
        }

        toClipboard(ll, append);
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
                Log.e(TAG, e.toString());
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

    private static int[] mSampleRates = new int[] {44100, 22050, 11025, 8000};
    public AudioRecord findAudioRecord() {
        for (int rate : mSampleRates) {
            for (short audioFormat : new short[] { AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT }) {
                for (short channelConfig : new short[] { AudioFormat.CHANNEL_IN_MONO /*, AudioFormat.CHANNEL_IN_STEREO*/ }) {
                    try {
                        Log.d(TAG, "REC Attempt, rate: " + rate + "Hz, bits: " + audioFormat + ", ch: "
                                + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                fsdev = REC_RATE / rate;    // Deviate the initial sample rate
                                return recorder;
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "REC " + rate + "Exception, keep trying.",e);
                    }
                }
            }
        }
        return null;
    }

}