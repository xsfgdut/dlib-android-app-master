package custom;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class CameraSurfaceViews extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    Context mContext;
    SurfaceHolder mSurfaceHolder;//surface的控制器
    Camera mCamera;//相机类
    FrameCallback mCb;//数据回调接口

    public void setmCb(FrameCallback mCb) {
        this.mCb = mCb;
    }

    //获取surfaceView的SurfaceHolder对象和接口
    public CameraSurfaceViews(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
    }

    //寻找相机
    private int findCamera(boolean isfront) {
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();

        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (isfront) {
                // CAMERA_FACING_FRONT前置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    return camIdx;
                }
            } else {
                // CAMERA_FACING_BACK后置
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    return camIdx;
                }
            }

        }
        return -1;
    }

    //打开相机
    private Camera getCamera() {
        Camera camera = null;
        int cameraId = findCamera(true);
        try {
            if (cameraId == 1) {
                camera = Camera.open(cameraId);
            } else if (cameraId == 0) {
                camera = Camera.open(0);
            }
        }catch (Exception e) {
            camera = null;
        }
            return camera;
    }

    //surface被创建时调用
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = getCamera();
    }

    //surface大小被改变时调用
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        setStartPreview(mCamera, mSurfaceHolder);
    }

    //surface被销毁时调用
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    //开启相机预览
    private void setStartPreview(Camera camera, SurfaceHolder holder) {
        try {

            mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(this);
            camera.startPreview();
        } catch (IOException e) {
        }
    }

    //释放Camera
    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();// 停掉摄像头的预览
            mCamera.release();
            mCamera = null;
        }
    }

    //预览回调，传递yuv视频流数据
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
            Log.i("onPreviewFrame",""+ bytes);
        if (mCb != null) {
            mCb.onDecodeFrame(bytes);
        }
    }
}
