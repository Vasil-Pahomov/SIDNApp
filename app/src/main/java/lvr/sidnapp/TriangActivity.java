package lvr.sidnapp;

import android.os.Bundle;

public class TriangActivity extends BaseLocationActivity {

    private PositionView posView;

    private final double SOUND_VELOCITY = 331;//meters per second

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_triang);

        posView = (PositionView)findViewById(R.id.posView);
        posView.setRoom(1.3, 3);

        startLocationUpdate();
    }

    @Override
    protected void onLocationUpdate(short[] rawData, short[][] convData, BeaconData[] data) {
        super.onLocationUpdate(rawData, convData, data);
        int maxPos = 0, minPos = rawData.length;
        double[] radiuses = new double[NSIG];

        for (int i=0; i<NSIG; i++) {
            int peak = data[i].getPeakPosition();
            if (data[i].getPeakPosition() < minPos) {
                minPos = data[i].getPeakPosition();
            }
            if (peak < minPos) {
                minPos = peak;
            }
            if (peak > maxPos) {
                maxPos = peak;
            }
        }

        if (maxPos - minPos > samples[0].length) {
            // все пики рядом в окне обнаружения - просто выравниваем по самому раннему и вычисляем расстояния до маяков
            for (int i=0; i<NSIG; i++) {
                radiuses[i] = indToMeters(data[i].getPeakPosition()- minPos);
            }
        } else {
            // пики пересекают границу окна обнаружения - самые ранние на самом деле самые поздние
            // тут надо хитро обсчитать
        }

        posView.setPos(0,0, radiuses[0], radiuses[1], 0, 0);

        startLocationUpdate();
    }

    //convert sample index to distance in meters
    private double indToMeters(int index) {
        return index / RecordingThread.SAMPLE_RATE * SOUND_VELOCITY;
    }

}
