package lvr.sidnapp;

import android.app.Dialog;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.newventuresoftware.waveform.WaveformView;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements AudioDataReceivedListener {

    short[] samples1, samples2, recBuf;
    double[] recBuf_d, result1_d, result2_d;

    int recBufPtr = 0;

    RecordingThread recThread = new RecordingThread(this);

    Button buttonRec;

    WaveformView waveViewSource, waveView1, waveView2;

    TextView textViewStatus, textViewPos;

    Convolution conv1, conv2;

    private static final int REQUEST_RECORD_AUDIO = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        samples1 = Utils.ReadSamples(getResources(), R.raw.s1);
        samples2 = Utils.ReadSamples(getResources(), R.raw.s2);


        recBuf = new short[samples1.length * 2];

        double[] s1d = new double[samples1.length];
        for (int i=0;i<samples1.length;i++) {
            s1d[i] = samples1[i];
        }

        conv1 = new Convolution(s1d, recBuf.length);

        double[] s2d = new double[samples2.length];
        for (int i=0;i<samples2.length;i++) {
            s2d[i] = samples2[i];
        }

        conv2 = new Convolution(s2d, recBuf.length);

        recBuf_d = new double[recBuf.length];
        result1_d = new double[conv1.getFrameSize()];
        result2_d = new double[conv2.getFrameSize()];

        buttonRec = (Button) findViewById(R.id.buttonRec);
        waveViewSource = (WaveformView) findViewById(R.id.waveViewSource);
        waveView1 = (WaveformView) findViewById(R.id.waveView1);
        waveView2 = (WaveformView) findViewById(R.id.waveView2);
        textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        textViewPos = (TextView) findViewById(R.id.textViewPos);

        setStatusText("Idle");

    }

    @Override
    public void onAudioDataReceived(short[] data) {
        int lengthToCopy = data.length;
        if (recBufPtr + lengthToCopy > recBuf.length) {
            lengthToCopy = recBuf.length - recBufPtr;
        }
        System.arraycopy(data, 0, recBuf, recBufPtr, lengthToCopy);
        recBufPtr += data.length;

        //todo: tail of recording buffer is lost, think about using it
        if (recBufPtr >= (recBuf.length-1)) {
            recBufPtr = 0;
            stopAudioRecording();
            waveViewSource.setSamples(recBuf);

            new AsyncTask<Void, Void, Void>() {
                private short[] filtered1, filtered2;

                @Override
                protected Void doInBackground(Void... params) {

                    setStatusText("Convolving");

                    //filtered = RSUtils.RsConvolve(recBuf, samples1, samples2);

                    for (int i=0;i<recBuf.length;i++) {
                        recBuf_d[i] = recBuf[i];
                    }

                    conv1.computeConvResult(recBuf_d, result1_d);
                    conv2.computeConvResult(recBuf_d, result2_d);

                    double max1 = Double.MIN_VALUE, max2=Double.MIN_VALUE,
                            peak1 = Double.MIN_VALUE, peak2 = Double.MIN_VALUE,
                            sqsum1 = 0, sqsum2 = 0;
                    int peakSearchWindowSize = Math.max(samples1.length, samples2.length);
                    int peak1pos = -1, peak2pos = -1;
                    //todo: избавится от двойных циклов - приводить samples к единой длине, заполняя нулями

                    for (int i=0; i<result1_d.length;i++) {
                        double res = Math.abs(result1_d[i]);
                        if ( res > max1) {
                            max1 = res;
                        }
                        if (i < peakSearchWindowSize) {
                            if (res > peak1) {
                                peak1 = res;
                                peak1pos = i;
                            }
                        }
                        sqsum1 += res*res;
                    }

                    for (int i=0; i<result2_d.length;i++) {
                        double res = Math.abs(result2_d[i]);
                        if ( res > max2) {
                            max2 = res;
                        }
                        if (i < peakSearchWindowSize) {
                            if (res > peak2) {
                                peak2 = res;
                                peak2pos = i;
                            }
                        }
                        sqsum2 += res*res;
                    }

                    int pos =
                            (peak2pos >= peak1pos)
                            ? peak2pos - peak1pos
                            : peak2pos - peak1pos + peakSearchWindowSize;

                    final String posstr = String.format("%d (%d-%d), PF: %f-%f", pos, peak1pos, peak2pos, peak1/Math.sqrt(sqsum1/samples1.length), peak2/Math.sqrt(sqsum2/samples2.length));
                    Log.d("SIDN", posstr);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textViewPos.setText(posstr);
                        }
                    });

                    filtered1 = new short[result1_d.length];
                    filtered2 = new short[result2_d.length];
                    for (int i=0; i<result1_d.length;i++) {
                        filtered1[i] =  (short)Math.round(Math.abs(result1_d[i]) * 32767 / max1);
                    }
                    for (int i=0; i<result2_d.length;i++) {
                        filtered2[i] =  (short)Math.round(Math.abs(result2_d[i]) * 32767 / max2);
                    }

                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    waveView1.setSamples(filtered1);
                    waveView2.setSamples(filtered2);


                    startAudioRecordingSafe();
                    //setStatusText("Idle");
                }
            }.execute();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        stopAudioRecording();
    }

    public void buttonRecClick(View view) {
        if (recThread.recording()) {
            stopAudioRecording();
        } else {
            startAudioRecordingSafe();
            waveViewSource.setChannels(1);
            waveViewSource.setSampleRate(RecordingThread.SAMPLE_RATE);
            waveView1.setChannels(1);
            waveView1.setSampleRate(RecordingThread.SAMPLE_RATE);
        }
    }

    private void stopAudioRecording() {
        recThread.stopRecording();
        setStatusText("Stopped");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                buttonRec.setText(R.string.buttonRecStartText);
            }
        });
    }

    private void startAudioRecordingSafe() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            recThread.startRecording();
            setStatusText("Recording");
            buttonRec.setText(R.string.buttonRecStopText);
            recBufPtr = 0;
        } else {
            requestMicrophonePermission();
        }
    }

    private void requestMicrophonePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.RECORD_AUDIO)) {
            // Show dialog explaining why we need record audio
            Snackbar.make(waveView1, "Microphone access is required in order to record audio",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
                }
            }).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                    android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
            if (requestCode == REQUEST_RECORD_AUDIO && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            recThread.stopRecording();
        }
    }

    private void setStatusText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewStatus.setText(text);
            }
        });
    }

    public void testcomp_click(View view) {
        for (int i=0;i<samples1.length;i++) {
            recBuf_d[i] = samples1[samples1.length-i-1];
        }

        conv1.computeConvResult(recBuf_d, result1_d);

        double max = Double.MIN_VALUE;
        for (int i=1; i<result1_d.length;i++) {
            if (Math.abs(result1_d[i]) > max) {
                max = Math.abs(result1_d[i]);
            }
        }


        short[] filtered = new short[result1_d.length];
        for (int i=1; i<result1_d.length;i++) {
            filtered[i] =  (short)Math.abs(Math.round(result1_d[i] * 32676 / max));
        }



        waveViewSource.setChannels(1);
        waveViewSource.setSampleRate(RecordingThread.SAMPLE_RATE);
        waveViewSource.setSamples(filtered);
    }
}
