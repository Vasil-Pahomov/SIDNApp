package lvr.sidnapp;

import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.util.Locale;

/**
 * Created by Vasil on 03.02.2017.
 */

public class BaseLocationActivity extends AppCompatActivity implements AudioDataReceivedListener {
    /* number of signals */
    protected static final int NSIG = 4;

    short[][] samples = new short[NSIG][];
    double[][] result_d = new double[NSIG][];

    short[] recBuf;
    double[] recBuf_d;

    int recBufPtr = 0;

    RecordingThread recThread = new RecordingThread(this);

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
            stopLocationUpdate();

            new AsyncTask<Void, Void, Void>() {
                private int[] peakPos = new int[NSIG];
                private double[] peakRate = new double[NSIG];

                private short[][] filtered = new short[NSIG][];

                private int peakSearchWindowSize;

                @Override
                protected Void doInBackground(Void... params) {

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
                    BeaconData[] data = new BeaconData[NSIG];
                    for (int i=0;i<NSIG;i++){
                        data[i] = new BeaconData(i, peakPos[i], peakRate[i]);
                    }
                    onLocationUpdate(recBuf, filtered, data);
                }
            }.execute();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        stopLocationUpdate();
    }

    //плохая идея передавать свёрнутые данные уже приведёнными к short[]. По хорошему их надо передавать в double[], какими собственно они и получаются при свёртке
    protected void onLocationUpdate(short[] rawData, short[][] convData, BeaconData[] data) {

    }

    protected void stopLocationUpdate() {
        recThread.stopRecording();
    }

    protected void startLocationUpdate() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            recThread.startRecording();
            recBufPtr = 0;
        } else {
            requestMicrophonePermission();
        }
    }

    private void requestMicrophonePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.RECORD_AUDIO)) {
            // Show dialog explaining why we need record audio
            Snackbar.make(findViewById(android.R.id.content), "Microphone access is required in order to record audio",
                    Snackbar.LENGTH_INDEFINITE).setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityCompat.requestPermissions(BaseLocationActivity.this, new String[]{
                            android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
                }
            }).show();
        } else {
            ActivityCompat.requestPermissions(BaseLocationActivity.this, new String[]{
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

}
