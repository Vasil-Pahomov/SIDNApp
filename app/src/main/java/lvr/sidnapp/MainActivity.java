package lvr.sidnapp;

import android.os.Environment;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.newventuresoftware.waveform.WaveformView;

import org.w3c.dom.Text;

import java.io.File;

public class MainActivity extends BaseLocationActivity {

    Button buttonRec;

    WaveformView waveViewSource, waveView1, waveView2, waveView3, waveView4;

    TextView textViewStatus, textViewPos, textViewPos1, textViewPos2, textViewPos3, textViewPos4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        waveViewSource = (WaveformView) findViewById(R.id.waveViewSource);
        waveView1 = (WaveformView) findViewById(R.id.waveView1);
        waveView2 = (WaveformView) findViewById(R.id.waveView2);
        waveView3 = (WaveformView) findViewById(R.id.waveView3);
        waveView4 = (WaveformView) findViewById(R.id.waveView4);

        textViewPos1 = (TextView) findViewById(R.id.textViewPos1);
        textViewPos2 = (TextView) findViewById(R.id.textViewPos2);
        textViewPos3 = (TextView) findViewById(R.id.textViewPos3);
        textViewPos4 = (TextView) findViewById(R.id.textViewPos4);

        textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        textViewPos = (TextView) findViewById(R.id.textViewPos);

        startLocationUpdate();
        setStatusText("Recording");

        waveViewSource.setChannels(1);
        waveViewSource.setSampleRate(RecordingThread.SAMPLE_RATE);
        waveView1.setChannels(1);
        waveView1.setSampleRate(RecordingThread.SAMPLE_RATE);
    }

    private void setStatusText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewStatus.setText(text);
            }
        });
    }

    @Override
    protected void onLocationUpdate(short[] rawData, short[][] convData, BeaconData[] data) {
        super.onLocationUpdate(rawData, convData, data);
        waveViewSource.setSamples(rawData);
        waveView1.setSamples(convData[0]);
        waveView2.setSamples(convData[1]);
        waveView3.setSamples(convData[2]);
        waveView4.setSamples(convData[3]);
        int pos =
                (data[1].getPeakPosition() >= data[0].getPeakPosition())
                        ? data[1].getPeakPosition() - data[0].getPeakPosition()
                        : data[1].getPeakPosition() - data[0].getPeakPosition() + convData[0].length;

        final String posstr = String.format("%d (%d-%d), PF: %f-%f", pos, data[0].getPeakPosition(),  data[1].getPeakPosition(), data[0].getPeakFactor(), data[1].getPeakFactor());
        final BeaconData[] tdata = data;
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                textViewPos.setText(posstr);
                textViewPos1.setText(String.format("%f",tdata[0].getPeakFactor()));
                textViewPos2.setText(String.format("%f",tdata[1].getPeakFactor()));
                textViewPos3.setText(String.format("%f",tdata[2].getPeakFactor()));
                textViewPos4.setText(String.format("%f",tdata[3].getPeakFactor()));
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
        startLocationUpdate();
        //setStatusText("Idle");

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
