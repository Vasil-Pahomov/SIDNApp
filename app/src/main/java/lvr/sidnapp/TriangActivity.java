package lvr.sidnapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class TriangActivity extends AppCompatActivity {

    private PositionView posView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_triang);

        posView = (PositionView)findViewById(R.id.posView);
        posView.setRoom(1,2);
        posView.setPos(0.3,0.7,0.1,0.2,0.3,0.4);

    }
}
