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

    short[] samples1;
    short[] samples2;

    short[] recBuf;

    int recBufPtr = 0;

    RecordingThread recThread = new RecordingThread(this);

    Button buttonRec;

    WaveformView waveViewSource, waveView1;

    TextView textViewStatus;

    private static final int REQUEST_RECORD_AUDIO = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        samples1 = Utils.ReadSamples(getResources(), R.raw.s1);
        samples2 = Utils.ReadSamples(getResources(), R.raw.s2);

        recBuf = new short[100000];

        buttonRec = (Button) findViewById(R.id.buttonRec);
        waveViewSource = (WaveformView) findViewById(R.id.waveViewSource);
        waveView1 = (WaveformView) findViewById(R.id.waveView1);
        textViewStatus = (TextView) findViewById(R.id.textViewStatus);

        setStatusText("Idle");

        RSUtils.Init(this);

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

                    filtered = RSUtils.RsConvolve(recBuf, samples1);

                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    waveView1.setSamples(filtered);
                    setStatusText("Idle");
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
}
