package test;

import android.app.Activity;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.tzutalin.dlibtest.R;


/**
 * Created by guibaozou on 2017/8/17.
 */

public class CustomCameraActivity extends Activity implements View.OnClickListener, Camera.AutoFocusCallback, Camera.PictureCallback ,SensorManger.OnAccelSensorListener{

    private SurfaceView surfaceView;
    private ImageButton swapCameraBtn;
    private ImageButton takePhotoBtn;
    private ImageButton backBtn;
    private ImageButton surePhotoBtn;
    private ImageButton reTakeBtn;
    private FocusView focusView;
    private RelativeLayout rootLayout;

    private OrientationEventListener orientationEventListener;

    private int screenWidth;
    private int screenHeight;
    private String screenRate;


    private SurfaceHolder holder;

    private Handler mHandler = new Handler();



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 全屏显示
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.custom_camera);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        swapCameraBtn = (ImageButton) findViewById(R.id.cameraSwap);
        takePhotoBtn = (ImageButton) findViewById(R.id.takePhoto);
        backBtn = (ImageButton) findViewById(R.id.backBtn);
        surePhotoBtn = (ImageButton) findViewById(R.id.surePhoto);
        reTakeBtn = (ImageButton) findViewById(R.id.reTakePhoto);
        rootLayout = (RelativeLayout) findViewById(R.id.root_layout);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(160, 160);
        focusView = new FocusView(this);
        focusView.setLayoutParams(layoutParams);
        holder = surfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//        holder.setFixedSize(176, 144);//设置分辨率
        holder.setKeepScreenOn(true);//屏幕长亮

        holder.addCallback(new SurfaceCallBack());////为SurfaceView的句柄添加一个回调函数
        swapCameraBtn.setOnClickListener(this);
        takePhotoBtn.setOnClickListener(this);
        backBtn.setOnClickListener(this);
        surePhotoBtn.setOnClickListener(this);
        reTakeBtn.setOnClickListener(this);

        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        //获取屏幕的宽和高
        display.getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        SensorManger.getIntance().initSensorManager(this);
        SensorManger.getIntance().setAccelSensorListener(this);
        screenRate = getSurfaceViewSize(screenWidth, screenHeight);
        setSurfaceViewSize(screenRate);
        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return;  //手机平放时，检测不到有效的角度
                }
                //只检测是否有四个角度的改变
                if (orientation > 350 || orientation < 10) { //0度
                    orientation = 90;
                } else if (orientation > 80 && orientation < 100) { //90度
                    orientation = 0;
                } else if (orientation > 170 && orientation < 190) { //180度
                    orientation = 270;
                } else if (orientation > 260 && orientation < 280) { //270度
                    orientation = 180;
                } else {
                    orientation = 0;
                }
                DataUtils.degree = orientation;
            }
        };
    }

    private float startDis;
    private boolean isTouch;
    /**
     * 记录是拖拉照片模式还是放大缩小照片模式
     */

    private static final int MODE_INIT = 0;
    /**
     * 放大缩小照片模式
     */
    private static final int MODE_ZOOM = 1;
    private int mode = MODE_INIT;// 初始状态
    private Point point;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mode = MODE_INIT;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (event.getPointerCount() < 2)
                    return super.onTouchEvent(event);
                mode = MODE_ZOOM;
                startDis = spacing(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == MODE_ZOOM) {
                    if (event.getPointerCount() < 2)
                        return super.onTouchEvent(event);
                    float endDis = spacing(event);
                    int scale = (int) ((endDis - startDis) / 10f);
                    if (scale >= 1 || scale <= -1) {
                        CameraManger.getInstance().setCameraZoom(scale);
                        startDis = endDis;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mode == MODE_INIT) {
                    point = new Point((int) event.getX(), (int) event.getY());
                    SensorManger.getIntance().lockFocus();
                    isTouch = CameraManger.getInstance().setCameraFocusAreas(point);
                    if (isTouch) {
                        CameraManger.getInstance().setCameraAutoFocus(CustomCameraActivity.this);
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * 两点的距离
     */
    private float spacing(MotionEvent event) {
        if (event == null) {
            return 0;
        }
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);

        return (float) Math.sqrt(x * x + y * y);
    }

    // 提供一个静态方法，用于根据手机方向获得相机预览画面旋转的角度
    private int getPreviewDegree(Activity activity) {
        // 获得手机的方向
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degree = 0;
        // 根据手机的方向计算相机预览画面应该选择的角度
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 90;
                break;
            case Surface.ROTATION_90:
                degree = 0;
                break;
            case Surface.ROTATION_180:
                degree = 270;
                break;
            case Surface.ROTATION_270:
                degree = 180;
                break;
        }
        return degree;
    }

    public String getSurfaceViewSize(int width, int height) {
        if (equalRate(width, height, 1.33f)) {
            return "4:3";
        } else {
            return "16:9";
        }
    }

    public boolean equalRate(int width, int height, float rate) {
        float r = (float) width / (float) height;
        if (Math.abs(r - rate) <= 0.2) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 根据分辨率设置预览SurfaceView的大小以防止变形
     *
     * @param surfaceSize
     */
    private void setSurfaceViewSize(String surfaceSize) {
        ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
        if (surfaceSize.equals("16:9")) {
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        } else if (surfaceSize.equals("4:3")) {
            params.height = 4 * screenWidth / 3;
        }
        surfaceView.setLayoutParams(params);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
        }
        SensorManger.getIntance().registerListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        orientationEventListener.disable();
        SensorManger.getIntance().unRegisterListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.backBtn:
                destroyCamera();
                finish();
                break;
            case R.id.takePhoto:
                //锁定焦点
                SensorManger.getIntance().lockFocus();
                //拍照
                CameraManger.getInstance().takePicture(CustomCameraActivity.this, CustomCameraActivity.this);
                break;
            case R.id.reTakePhoto:
                CameraManger.getInstance().startPreview();
                takePhotoBtn.setVisibility(View.VISIBLE);
                surePhotoBtn.setVisibility(View.GONE);
                backBtn.setVisibility(View.VISIBLE);
                reTakeBtn.setVisibility(View.GONE);
                SensorManger.getIntance().unLockFocus();
                break;
            case R.id.surePhoto:
                destroyCamera();
                setResult(RESULT_OK);
                CameraManger.getInstance().closeShutterSound();
                finish();
                break;
            case R.id.cameraSwap:
                CameraManger.getInstance().turnCamera(holder, CustomCameraActivity.this, getPreviewDegree(CustomCameraActivity.this),screenWidth,screenHeight);
                break;
            default:
                break;
        }
    }

    /**
     * 自定聚焦监听器的方法
     *
     * @param success
     * @param camera
     */
    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if (success) {
            SensorManger.getIntance().unLockFocus();
            rootLayout.removeView(focusView);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) focusView.getLayoutParams();
            if (!isTouch) {
                params.addRule(RelativeLayout.CENTER_IN_PARENT);
            } else {
                isTouch = false;
                if (point != null) {
                    params.leftMargin = point.x - 30;
                    params.topMargin = point.y - 30;
                } else {
                    params.addRule(RelativeLayout.CENTER_IN_PARENT);
                }
            }
            rootLayout.addView(focusView, params);

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    rootLayout.removeView(focusView);
                }
            }, 1000);

        }

    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        CameraManger.getInstance().stopPreview();
        //将拍摄到的照片给自定义的对象
        DataUtils.tempImageData = data;
        takePhotoBtn.setVisibility(View.GONE);
        surePhotoBtn.setVisibility(View.VISIBLE);
        backBtn.setVisibility(View.GONE);
        reTakeBtn.setVisibility(View.VISIBLE);
        SensorManger.getIntance().unLockFocus();
    }

    @Override
    public void onAccelSensor() {
        CameraManger.getInstance().setCameraAutoFocus(this);
    }

    private final class SurfaceCallBack implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            CameraManger.getInstance().openCamera(holder,CustomCameraActivity.this,getPreviewDegree(CustomCameraActivity.this));
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            CameraManger.getInstance().setCameraParameters(screenWidth,screenHeight);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            destroyCamera();
        }
    }

    private void destroyCamera() {
        CameraManger.getInstance().destroyCamera();
        holder.getSurface().release();
        surfaceView = null;

    }
}

