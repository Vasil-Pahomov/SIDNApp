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
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AudioDataReceivedListener {

    /* number of signals */
    private static final int NSIG = 4;

    short[][] samples = new short[NSIG][];
    double[][] result_d = new double[NSIG][];

    short[] recBuf;
    double[] recBuf_d;

    int recBufPtr = 0;

    RecordingThread recThread = new RecordingThread(this);

    Button buttonRec;

    WaveformView waveViewSource, waveView1, waveView2;

    TextView textViewStatus, textViewPos;

    Convolution[] conv = new Convolution[NSIG];

    private static final int REQUEST_RECORD_AUDIO = 13;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        samples = new short[NSIG][];

        double[] sd = null;

        int recBufLength = 0;

        for (int i=0;i<NSIG;i++) {
            Resources res = getResources();
            String rname = String.format(Locale.ROOT, "s%d", i+1);
            int rid = res.getIdentifier(rname, "raw", getPackageName());
            samples[i] = Utils.ReadSamples(getResources(), rid);
            if (i>0 && samples[i].length != samples[0].length) {
                Log.e("SIDN", String.format("Sample length mismatch: %d/%d", samples[i].length, samples[0].length));
            }

            if (sd == null) {
                sd = new double[samples[i].length];
                recBufLength = samples[i].length*2;
            }
            for (int j=0;j<sd.length;j++) {
                sd[j] = samples[i][j];
            }

            conv[i] = new Convolution(sd, recBufLength);
            result_d[i]= new double[conv[i].getFrameSize()];
        }

        recBuf = new short[recBufLength];
        recBuf_d = new double[recBuf.length];

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
                private int[] peakPos = new int[NSIG];
                private double[] peakRate = new double[NSIG];

                private short[][] filtered = new short[NSIG][];

                private int peakSearchWindowSize;

                @Override
                protected Void doInBackground(Void... params) {

                    setStatusText("Convolving");

                    for (int i=0;i<recBuf.length;i++) {
                        recBuf_d[i] = recBuf[i];
                    }


                    for (int i=0;i<NSIG;i++) {
                        conv[i].computeConvResult(recBuf_d, result_d[i]);

                        double max = Double.MIN_VALUE, peak = Double.MIN_VALUE, sqsum = 0;
                        peakSearchWindowSize = samples[i].length;
                        int peakpos = -1;

                        for (int j = 0; j < result_d[i].length; j++) {
                            double res = Math.abs(result_d[i][j]);
                            if (res > max) {
                                max = res;
                            }
                            if (i < peakSearchWindowSize) {
                                if (res > peak) {
                                    peak = res;
                                    peakpos = j;
                                }
                                sqsum += res * res;
                            }
                        }

                        peakPos[i] = peakpos;
                        peakRate[i] = peak / Math.sqrt(sqsum / peakSearchWindowSize);

                        filtered[i] = new short[result_d[i].length];
                        for (int j=0; j<filtered[i].length;j++) {
                            filtered[i][j] = (short) Math.round(Math.abs(result_d[i][j]) * 32767 / max);
                        }
                    }


                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    waveView1.setSamples(filtered[0]);
                    waveView2.setSamples(filtered[1]);
                    int pos =
                            (peakPos[1] >= peakPos[0])
                                    ? peakPos[1] - peakPos[0]
                                    : peakPos[1] - peakPos[0] + peakSearchWindowSize;

                    final String posstr = String.format("%d (%d-%d), PF: %f-%f", pos, peakPos[0], peakPos[1], peakRate[0], peakRate[1]);
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            textViewPos.setText(posstr);
                        }
                    });
                    Log.d("SIDN", posstr);

/*                    try {
                        FileOutputStream os = new FileOutputStream(new File(getStorageDir(), "data.csv"));
                        os.write(Arrays.toString(recBuf).replace('[',' ').replace(']',' ').getBytes());
                        os.write("\r\n".getBytes());
                        os.write(Arrays.toString(filtered1).replace('[',' ').replace(']',' ').getBytes());
                        os.write("\r\n".getBytes());
                        os.write(Arrays.toString(filtered2).replace('[',' ').replace(']',' ').getBytes());
                        os.close();
                    }
                    catch (Exception ex) {
                        Log.e("SIDN", "Error writing file!", ex);
                    }
*/
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

    public File getStorageDir() {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "SIDN");
        if (!file.mkdirs()) {
            Log.e("SIDN", "Directory not created");
        }
        return file;
    }

    public void testcomp_click(View view) {
        /*
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
        waveViewSource.setSamples(filtered);*/
    }
}
