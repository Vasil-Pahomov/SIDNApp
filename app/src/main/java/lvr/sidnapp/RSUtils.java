package lvr.sidnapp;

import android.content.Context;
import android.support.v8.renderscript.*;
import android.util.Log;
import android.util.TimingLogger;

/**
 * Created by Vasil on 01.12.2016.
 */

public class RSUtils {

    private static RenderScript rs;

    private static ScriptC_convolution script;

    public static void Init(Context context) {
        if (rs != null) {
            return;
        }

        rs = RenderScript.create(context);
        script = new ScriptC_convolution(rs);
    }

    public static short[] Convolve(short[] data, short[] kernel) {
        Log.d("SIDN", "conv-prepapred");
        short[] res = new short[data.length];
        float[] resf = new float[data.length];

        float max = Float.MIN_VALUE;


        //this is VERY slow, try FFT convolution there
        for ( int i = 0; i < data.length; i++ ) {
            float sum = 0;
            for (int j = 0; j < kernel.length; j++) {
                if (i >= j) {
                    sum += data[i-j] * kernel[j];
                }
            }
            if (sum > max) {
                max = sum;
            }
            if (sum < -max) {
                max = -sum;
            }
            resf[i] = sum;
        }

        Log.d("SIDN", "conv-convolved");

        for (int i=0; i < res.length; i++) {
            res[i] = (short) Math.round(resf[i] / max * 32767);
        }

        Log.d("SIDN", "conv-done");
        return res;
    }

    public static short[] RsConvolve(short[] data, short[] kernel) {
        Log.d("SIDN","rsconv-start");// TimingLogger logger = new TimingLogger("sidn","conv");
        Element i16elem = Element.I16(rs);

        Allocation aData = Allocation.createTyped(rs, new Type.Builder(rs, i16elem).setX(data.length).create());
        Allocation aKernel = Allocation.createTyped(rs, new Type.Builder(rs, i16elem).setX(kernel.length).create());

        aData.copy1DRangeFrom(0, data.length, data);
        aKernel.copy1DRangeFrom(0, kernel.length, kernel);

        script.bind_data(aData);
        script.bind_kernel(aKernel);

        Allocation aRes = Allocation.createTyped(rs, new Type.Builder(rs, Element.F32(rs)).setX(data.length).create());
        script.bind_res(aRes);

        Log.d("SIDN", "rsconv-prepapred");//logger.addSplit("prepared");

        script.invoke_convolve(data.length, kernel.length);

        Log.d("SIDN", "rsconv-invoked");// logger.addSplit("invoked");

        float[] res = new float[data.length];
        aRes.copy1DRangeTo(0, data.length, res);

        float max = Float.MIN_VALUE;

        for (int i=0; i < res.length; i++) {
            float val = res[i];
            if (val > max) {
                max = val;
            }
            if (val < -max) {
                max = -val;
            }
        }

        short[] ress = new short[res.length];
        for (int i=0; i < res.length; i++) {
            ress[i] = (short) Math.round(res[i] / max * 32767);
        }

        Log.d("SIDN","rsconv-converted");//logger.addSplit("converted");
        //logger.dumpToLog();

        return ress;

    }


}
