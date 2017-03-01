package lvr.sidnapp;

import android.os.Bundle;
import android.renderscript.Double2;
import android.util.Log;
import android.view.View;

import Jama.Matrix;

public class TriangActivity extends BaseLocationActivity {

    private PositionView posView;

    private final double SOUND_VELOCITY = 331;//meters per second

    private final double roomWidth = 5, roomHeight = 5;


    private Position beaconPositions[] = new Position[NSIG];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_triang);

        posView = (PositionView)findViewById(R.id.posView);

        beaconPositions[0] = new Position(0, 0, 1);
        beaconPositions[1] = new Position(roomWidth, 0 ,1);
        beaconPositions[2] = new Position(roomWidth, roomHeight, 1);
        beaconPositions[3] = new Position(0, roomHeight, 1);

        posView.setRoom(roomWidth, roomHeight, beaconPositions);

        /*BeaconData[] data = new BeaconData[4];
        data[0] = new BeaconData(0, (int) Math.round(1.1021 / SOUND_VELOCITY * RecordingThread.SAMPLE_RATE), 0);
        data[1] = new BeaconData(1, (int) Math.round(1.1590 / SOUND_VELOCITY * RecordingThread.SAMPLE_RATE), 0);
        data[2] = new BeaconData(2, (int) Math.round(1.1510 / SOUND_VELOCITY * RecordingThread.SAMPLE_RATE), 0);
        data[3] = new BeaconData(3, (int) Math.round(1.0937 / SOUND_VELOCITY * RecordingThread.SAMPLE_RATE), 0);
        Position pos = trilaterate(data, beaconPositions);
        Log.d("SIDN", String.format("x=%f, y=%f", pos.getX(), pos.getY()));
        */

        //startLocationUpdate();
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

        //posView.setPos(0,0, radiuses[0], radiuses[1], 0, 0);

        startLocationUpdate();
    }

    //convert sample index to distance in meters
    private double indToMeters(int index) {
        return (double) index * SOUND_VELOCITY / RecordingThread.SAMPLE_RATE;
    }

    private Position trilaterate(BeaconData[] data, Position[] pos)
    {
        double[][] aA = new double[NSIG][4];
        double[] aR = new double[NSIG];
        for (int i=0; i<NSIG; i++) {
            int bi = data[i].getBeaconIndex();
            aA[i][0] = pos[bi].getX();
            aA[i][1] = pos[bi].getY();
            aA[i][2] = pos[bi].getZ();

            aA[i][3] = indToMeters(data[i].getPeakPosition());
            aR[i] = 0.5 * Utils.LIP(aA[i], aA[i]);
        }

        Matrix  A = new Matrix(aA),
                I0 = new Matrix(4,1,1),
                R = new Matrix(aR, NSIG),

                B = Utils.MatrixPinv(A),
                u = B.times(I0),
                v = B.times(R);

        double  E = Utils.LIP(u,u),
                F = Utils.LIP(u,v) - 1,
                G = Utils.LIP(v,v),
                l1 = (-2*F + Math.sqrt(4*F*F-4*E*G))/(2*E),
                l2 = (-2*F - Math.sqrt(4*F*F-4*E*G))/(2*E);

        if (Double.isNaN(l1)) {
            //иногда получается, что квадратное уравнение не решается. Дискриминант оказывается слегка меньше нуля
            //считаем, что это из-за погрешности и на самом деле он равен нулю
            l1 = (-2*F)/(2*E);
            l2 = l1;
            Log.d("SIDN", "Approximation when trilaterating!!!");
        }

        Matrix  y1 = u.times(l1).plus(v),
                y2 = v.times(l2).plus(v);

        Position res = posFromMatrixIfMatches(y2);
        if (res == null) {
            res = posFromMatrixIfMatches(y1);
        }

        return res;

    }

    private Position posFromMatrixIfMatches(Matrix y) {
        Position res = new Position(y.get(0,0), y.get(1,0), y.get(2,0));
        if (res.getX() >= 0 && res.getX() <= roomWidth && res.getY() >= 0 && res.getY() <= roomHeight) {
            return res;
        } else {
            return null;
        }
    }

    public void posViewClick(View view) {
        double x = 2.43, y = 2.51, z = 1.0;
        BeaconData[] data = new BeaconData[NSIG];
        double rad[] = new double[NSIG];
        double corr = (Math.random() -0.5);
        for (int i=0; i<NSIG; i++) {
            rad[i] = Math.sqrt(
                    (beaconPositions[i].getX() - x) * (beaconPositions[i].getX() - x)
                            + (beaconPositions[i].getY() - y) * (beaconPositions[i].getY() - y)
                            + (beaconPositions[i].getZ() - z) * (beaconPositions[i].getZ() - z)) + corr + (Math.random() - 0.5) / 10;

            data[i] = new BeaconData(i, (int)
                    Math.round(rad[i] / SOUND_VELOCITY * RecordingThread.SAMPLE_RATE), 0);
        }

        Position pos = trilaterate(data, beaconPositions);

        if (pos == null) {
            posView.setPos(0,0,rad);
        } else {
            posView.setPos(pos.getX(), pos.getY(), rad);
        }
    }
}
