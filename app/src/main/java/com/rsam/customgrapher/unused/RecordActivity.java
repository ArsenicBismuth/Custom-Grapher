package com.rsam.customgrapher;

import android.app.Activity;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;

import com.rsam.customgrapher.R;

import java.io.IOException;

public class RecordActivity extends Activity {
//    private WaveformView waveformViewView;
//    private MediaRecorder recorder = new MediaRecorder();
//    private Handler handler = new Handler();
//    final Runnable updater = new Runnable() {
//        public void doRun() {
//            handler.postDelayed(this, 1);
//            int maxAmplitude = record
// er.getMaxAmplitude();
//            if (maxAmplitude != 0) {
//                waveformViewView.addAmplitude(maxAmplitude);
//            }
//        }
//    };
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.content_main);
//        waveformViewView = (WaveformView) findViewById(R.id.waveformView);
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
//
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
}
