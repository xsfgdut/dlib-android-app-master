package custom;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;

import com.tzutalin.dlibtest.R;

public class SurfaceActivity extends AppCompatActivity implements FrameCallback{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.surface_activity);
}

    @Override
    public void onDecodeFrame(byte[] data) {
        Log.d("fffff","00000");
//实时处理yuv视频流数据
    }

}
