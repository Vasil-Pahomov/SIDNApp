package lvr.sidnapp;

/**
 * Created by Vasil on 31.10.2016.
 * Taken from https://github.com/newventuresoftware/WaveformControl
 */

public interface AudioDataReceivedListener {
    void onAudioDataReceived(short[] data);
}
