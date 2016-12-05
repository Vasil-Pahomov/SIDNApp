package lvr.sidnapp;

import android.app.Dialog;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements AudioDataReceivedListener {

    short[] samples1, samples2, recBuf;
    double[] recBuf_d, result_d;

    int recBufPtr = 0;

    RecordingThread recThread = new RecordingThread(this);

    Button buttonRec;

    WaveformView waveViewSource, waveView1, waveView2;

    TextView textViewStatus;

    Convolution conv1;

    private static final int REQUEST_RECORD_AUDIO = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        samples1 = Utils.ReadSamples(getResources(), R.raw.s1);
        samples2 = Utils.ReadSamples(getResources(), R.raw.s2);


        recBuf = new short[50000];

        double[] s1d = new double[samples1.length];
        for (int i=0;i<samples1.length;i++) {
            s1d[i] = samples1[i];
        }

        conv1 = new Convolution(s1d, recBuf.length);

        recBuf_d = new double[recBuf.length];
        result_d = new double[conv1.getFrameSize()];

        buttonRec = (Button) findViewById(R.id.buttonRec);
        waveViewSource = (WaveformView) findViewById(R.id.waveViewSource);
        waveView1 = (WaveformView) findViewById(R.id.waveView1);
        waveView2 = (WaveformView) findViewById(R.id.waveView2);
        textViewStatus = (TextView) findViewById(R.id.textViewStatus);

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
                private short[] filtered;

                @Override
                protected Void doInBackground(Void... params) {

                    setStatusText("Convolving");

                    //filtered = RSUtils.RsConvolve(recBuf, samples1, samples2);

                    for (int i=0;i<recBuf.length;i++) {
                        recBuf_d[i] = recBuf[i];
                    }

                    conv1.computeConvResult(recBuf_d, result_d);

                    double max = Double.MIN_VALUE;
                    for (int i=1; i<result_d.length;i++) {
                        if (Math.abs(result_d[i]) > max) {
                            max = Math.abs(result_d[i]);
                        }
                    }


                    filtered = new short[result_d.length];
                    for (int i=1; i<result_d.length;i++) {
                        filtered[i] =  (short)Math.round(result_d[i] * 32676 / max);
                    }

                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    waveView1.setSamples(filtered);
                    startAudioRecordingSafe();//setStatusText("Idle");
                }
            }.execute();
        }
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

        conv1.computeConvResult(recBuf_d, result_d);

        double max = Double.MIN_VALUE;
        for (int i=1; i<result_d.length;i++) {
            if (Math.abs(result_d[i]) > max) {
                max = Math.abs(result_d[i]);
            }
        }


        short[] filtered = new short[result_d.length];
        for (int i=1; i<result_d.length;i++) {
            filtered[i] =  (short)Math.round(result_d[i] * 32676 / max);
        }



        waveViewSource.setChannels(1);
        waveViewSource.setSampleRate(RecordingThread.SAMPLE_RATE);
        waveViewSource.setSamples(filtered);
    }
}
