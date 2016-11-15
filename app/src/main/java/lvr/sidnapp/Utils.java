package lvr.sidnapp;

import android.content.res.Resources;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

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

}
