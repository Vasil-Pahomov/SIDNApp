package lvr.sidnapp;

import android.content.res.Resources;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 * Created by Vasil on 11.11.2016.
 */

public class Utils {

    public static short[] ReadSamples(Resources resources, int resourceId) {
        InputStream pcmStream = resources.openRawResource(resourceId);

        byte[] data = null;
        try {
            try {
                data = IOUtils.toByteArray(pcmStream);
            } finally {
                if (pcmStream != null) {
                    pcmStream.close();
                }
            }
        } catch (IOException ex) {
        }

        ShortBuffer sb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
        short[] samples;
        samples = new short[sb.limit()];
        sb.get(samples);
        return samples;
    }

    //http://the-lost-beauty.blogspot.com.by/2009/04/moore-penrose-pseudoinverse-in-jama.html
    /**
     * The difference between 1 and the smallest exactly representable number
     * greater than one. Gives an upper bound on the relative error due to
     * rounding of floating point numbers.
     */
    public static double MACHEPS = 2E-16;

    /**
     * Updates MACHEPS for the executing machine.
     */
    private static void updateMacheps() {
        MACHEPS = 1;
        do
            MACHEPS /= 2;
        while (1 + MACHEPS / 2 != 1);
    }

    /**
     * Computes the Moore–Penrose pseudoinverse using the SVD method.
     *
     * Modified version of the original implementation by Kim van der Linde.
     */
    public static Matrix MatrixPinv(Matrix x) {
        int rows = x.getRowDimension();
        int cols = x.getColumnDimension();
        if (rows < cols) {
            Matrix result = MatrixPinv(x.transpose());
            if (result != null)
                result = result.transpose();
            return result;
        }
        SingularValueDecomposition svdX = new SingularValueDecomposition(x);
        if (svdX.rank() < 1)
            return null;
        double[] singularValues = svdX.getSingularValues();
        double tol = Math.max(rows, cols) * singularValues[0] * MACHEPS;
        double[] singularValueReciprocals = new double[singularValues.length];
        for (int i = 0; i < singularValues.length; i++)
            if (Math.abs(singularValues[i]) >= tol)
                singularValueReciprocals[i] =  1.0 / singularValues[i];
        double[][] u = svdX.getU().getArray();
        double[][] v = svdX.getV().getArray();
        int min = Math.min(cols, u[0].length);
        double[][] inverse = new double[cols][rows];
        for (int i = 0; i < cols; i++)
            for (int j = 0; j < u.length; j++)
                for (int k = 0; k < min; k++)
                    inverse[i][j] += v[i][k] * singularValueReciprocals[k] * u[j][k];
        return new Matrix(inverse);
    }

    //Lorentz inner product
    public static double LIP(Matrix a, Matrix b) {
        if (a.getColumnDimension() != 1 && a.getRowDimension() != 4) {
            Log.d("SIDN", String.format("Invalid A size: %dx%d, expected 4x1", a.getColumnDimension(), a.getRowDimension()));
        }
        if (b.getColumnDimension() != 1 && b.getRowDimension() != 4) {
            Log.d("SIDN", String.format("Invalid B size: %dx%d, expected 4x1", b.getColumnDimension(), b.getRowDimension()));
        }

        return a.get(0,0) * b.get(0,0)
                + a.get(1,0) * b.get(1,0)
                + a.get(2,0) * b.get(2,0)
                - a.get(3,0) * b.get(3,0);
    }

    public static double LIP(double[] a, double[] b) {
        if (a.length != 4) {
            Log.d("SIDN", String.format("Invalid A length: %d, expected 4", a.length));
        }
        if (b.length != 4) {
            Log.d("SIDN", String.format("Invalid B length: %d, expected 4", b.length));
        }

        return a[0] * b[0]
                + a[1] * b[1]
                + a[2] * b[2]
                - a[3] * b[3];
    }

}
