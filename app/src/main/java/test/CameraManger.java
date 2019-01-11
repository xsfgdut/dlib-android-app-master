package test;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by zouguibao on 2017/11/23.
 */

public class CameraManger {
    private static CameraManger instance;
    private Camera camera;
    private MediaPlayer mShootSound;

    public static final int TYPE_PREVIEW = 0;
    public static final int TYPE_PICTURE = 1;
    public static final int ALLOW_PIC_LEN = 2000;       //最大允许的照片尺寸的长度   宽或者高

    private int cameraPosition;

    private CameraManger() {

    }

    public static CameraManger getInstance() {
        if (instance == null) {
            instance = new CameraManger();
        }
        return instance;
    }

    /**
     * 打开摄像头
     *
     * @param holder
     * @param autoFocusCallback
     * @param degree
     */
    public void openCamera(SurfaceHolder holder, Camera.AutoFocusCallback autoFocusCallback, int degree) {
        try {
            //初始化摄像头
            cameraPosition = Camera.CameraInfo.CAMERA_FACING_BACK;
            // 打开摄像头
            camera = Camera.open(cameraPosition);
            // 设置用于显示拍照影像的SurfaceHolder对象
            camera.setPreviewDisplay(holder);
            camera.setDisplayOrientation(degree);
            camera.autoFocus(autoFocusCallback);

        } catch (Exception e) {
//                e.printStackTrace();
            camera.release();
            camera = null;
        }
    }

    /**
     * 设置参数
     */
    public void setCameraParameters(int screenWidth, int screenHeight) {
        try {
            if (camera != null) {
                Camera.Parameters parameters = camera.getParameters();//获取各项参数
                Camera.Size previewSize = findFitPreResolution(parameters);
                parameters.setPreviewSize(previewSize.width, previewSize.height);// 设置预览大小

                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                parameters.setPictureFormat(PixelFormat.JPEG);//设置图片格式
                //不能与setPreviewSize一起使用，否则setParamters会报错
//                    parameters.setPreviewFrameRate(5);//设置每秒显示4帧
                parameters.setJpegQuality(80);// 设置照片质量
                Camera.Size pictureSize = null;
                if (equalRate(screenWidth, screenHeight, 1.33f)) {
                    pictureSize = findFitPicResolution(parameters, (float) 4 / 3);
                } else {
                    pictureSize = findFitPicResolution(parameters, (float) 16 / 9);
                }

                parameters.setPictureSize(pictureSize.width, pictureSize.height);// 设置保存的图片尺寸
                camera.setParameters(parameters);
                camera.startPreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean equalRate(int width, int height, float rate) {
        float r = (float) width / (float) height;
        if (Math.abs(r - rate) <= 0.2) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 摄像头切换
     *
     * @param holder
     * @param autoFocusCallback
     * @param degree
     */
    public void turnCamera(SurfaceHolder holder, Camera.AutoFocusCallback autoFocusCallback, int degree, int screenWidth, int screenHeight) {
        //切换前后摄像头
        //现在是后置，变更为前置
        if (camera != null && cameraPosition == Camera.CameraInfo.CAMERA_FACING_BACK) {
            camera.stopPreview();//停掉原来摄像头的预览
            camera.release();//释放资源
            camera = null;//取消原来摄像头
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);//打开当前选中的摄像头
            try {
                camera.setPreviewDisplay(holder);//通过surfaceview显示取景画面
                camera.setDisplayOrientation(degree);
                camera.autoFocus(autoFocusCallback);
                setCameraParameters(screenWidth, screenHeight);
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.startPreview();//开始预览
            cameraPosition = Camera.CameraInfo.CAMERA_FACING_FRONT;
            DataUtils.isBackCamera = false;
        } else if (cameraPosition == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            //代表摄像头的方位，CAMERA_FACING_FRONT前置
            // CAMERA_FACING_BACK后置
            //现在是前置， 变更为后置
            camera.stopPreview();//停掉原来摄像头的预览
            camera.release();//释放资源
            camera = null;//取消原来摄像头
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);//打开当前选中的摄像头
            try {
                camera.setPreviewDisplay(holder);//通过surfaceview显示取景画面
                camera.setDisplayOrientation(degree);
                camera.autoFocus(autoFocusCallback);
                setCameraParameters(screenWidth, screenHeight);
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.startPreview();//开始预览
            cameraPosition = Camera.CameraInfo.CAMERA_FACING_BACK;
            DataUtils.isBackCamera = true;
        }
    }

    public void setCameraZoom(int scale) {
        Camera.Parameters parameters = camera.getParameters();
        int zoom = parameters.getZoom() + scale;
        if (zoom < 0) zoom = 0;
        if (zoom > parameters.getMaxZoom())
            zoom = parameters.getMaxZoom();
        parameters.setZoom(zoom);
        camera.setParameters(parameters);
    }

    public boolean setCameraFocusAreas(Point point) {
        if (camera == null) {
            return false;
        }

        Camera.Parameters parameters = null;
        try {
            parameters = camera.getParameters();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        //不支持设置自定义聚焦，则使用自动聚焦，返回
        if (parameters.getMaxNumFocusAreas() <= 0) {
            return false;
        }
        List<Camera.Area> areas = new ArrayList<Camera.Area>();

        int left = point.x - 300;
        int top = point.y - 300;
        int right = point.x + 300;
        int bottom = point.y + 300;

        left = left < -1000 ? -1000 : left;
        top = top < -1000 ? -1000 : top;
        right = right > 1000 ? 1000 : right;
        bottom = bottom > 1000 ? 1000 : bottom;

        areas.add(new Camera.Area(new Rect(left, top, right, bottom), 100));

        parameters.setFocusAreas(areas);
        try {
            //本人使用的小米手机在设置聚焦区域的时候经常会出异常，看日志发现是框架层的字符串转int的时候出错了，
            //目测是小米修改了框架层代码导致，在此try掉，对实际聚焦效果没影响
            camera.setParameters(parameters);
        } catch (Exception e) {
//            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void setCameraAutoFocus(Camera.AutoFocusCallback autoFocusCallback) {
        try {
            if (camera != null)
                camera.autoFocus(autoFocusCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 拍照
     *
     * @param context
     * @param pictureCallback
     */
    public void takePicture(final Context context, Camera.PictureCallback pictureCallback) {
        if (camera != null) {
            //拍照
            try{
                camera.takePicture(new Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {
                        try {
                            AudioManager meng = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                            int volume = meng.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
                            if (volume != 0) {
                                if (mShootSound == null) {
                                    mShootSound = MediaPlayer.create(context, Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
                                }
                                if (mShootSound != null) {
                                    mShootSound.start();
                                }
                            }
                        } catch (Exception e) {
                        }
                    }
                }, null, pictureCallback);
            }catch (Exception e){
                Toast.makeText(context,"拍照出现异常，请退出重新进入拍照",Toast.LENGTH_SHORT).show();
            }

        }
    }

    public void closeShutterSound() {
        if (mShootSound != null) {
            mShootSound.release();
        }
    }

    /**
     * 重拍
     */
    public void startPreview() {
        if (camera != null) {
            camera.startPreview();
        }
    }

    /**
     * 停止拍摄
     */
    public void stopPreview() {
        camera.stopPreview();
    }

    /**
     * 销毁摄像头
     */
    public void destroyCamera() {
        if (camera != null) {
            //当surfaceview关闭时，关闭预览并释放资源
            camera.stopPreview();
            camera.release();//释放相机
            camera = null;
        }
    }

    /**
     * 返回最小的预览尺寸
     *
     * @param cameraInst
     * @param type
     * @return
     */
    private Camera.Size findMinResolution(Camera cameraInst, int type) throws Exception {
        Camera.Parameters cameraParameters = cameraInst.getParameters();
        List<Camera.Size> supportedPicResolutions = type == TYPE_PREVIEW ? cameraParameters.getSupportedPreviewSizes() : cameraParameters.getSupportedPictureSizes(); // 至少会返回一个值

        if (supportedPicResolutions == null) {
            return null;
        }

        Camera.Size resultSize = supportedPicResolutions.get(0);
        for (Camera.Size size : supportedPicResolutions) {
            if (size.width < resultSize.width) {
                resultSize = size;
            }
        }
        return resultSize;
    }

    /**
     * 找到合适的尺寸
     *
     * @param cameraParameters
     * @param maxDistortion    最大允许的宽高比
     * @return
     * @type 尺寸类型 0：preview  1：picture
     */
    public Camera.Size findBestResolution(Camera.Parameters cameraParameters, double maxDistortion, int type) throws Exception {
        List<Camera.Size> supportedPicResolutions = type == TYPE_PREVIEW ? cameraParameters.getSupportedPreviewSizes() : cameraParameters.getSupportedPictureSizes(); // 至少会返回一个值

//        StringBuilder picResolutionSb = new StringBuilder();
//        for (Camera.Size supportedPicResolution : supportedPicResolutions) {
//            picResolutionSb.append(supportedPicResolution.width).append('x')
//                    .append(supportedPicResolution.height).append(" ");
//        }
////        Log.d(TAG, "Supported picture resolutions: " + picResolutionSb);
//
//        Camera.Size defaultPictureResolution = cameraParameters.getPictureSize();
//        Log.d(TAG, "default picture resolution " + defaultPictureResolution.width + "x"
//                + defaultPictureResolution.height);

        // 排序
        List<Camera.Size> sortedSupportedPicResolutions = new ArrayList<Camera.Size>(
                supportedPicResolutions);
        Collections.sort(sortedSupportedPicResolutions, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aRatio = a.width / a.height;
                int bRatio = b.width / a.height;

                if (Math.abs(aRatio - 1) <= Math.abs(bRatio - 1)) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

        //返回最合适的
        return sortedSupportedPicResolutions.get(sortedSupportedPicResolutions.size() - 1);
    }

    /**
     * 返回合适的照片尺寸参数
     *
     * @param cameraParameters
     * @param bl
     * @return
     */
    private Camera.Size findFitPicResolution(Camera.Parameters cameraParameters, float bl) throws Exception {
        List<Camera.Size> supportedPicResolutions = cameraParameters.getSupportedPictureSizes();

        Camera.Size resultSize = null;
        for (Camera.Size size : supportedPicResolutions) {
            if ((float) size.width / size.height == bl && size.width <= ALLOW_PIC_LEN && size.height <= ALLOW_PIC_LEN) {
                if (resultSize == null) {
                    resultSize = size;
                } else if (size.width > resultSize.width) {
                    resultSize = size;
                }
            }
        }
        if (resultSize == null) {
            return supportedPicResolutions.get(0);
        }
        return resultSize;
    }

    /**
     * 返回合适的预览尺寸参数
     *
     * @param cameraParameters
     * @return
     */
    private Camera.Size findFitPreResolution(Camera.Parameters cameraParameters) throws Exception {
        List<Camera.Size> supportedPicResolutions = cameraParameters.getSupportedPreviewSizes();

        Camera.Size resultSize = null;
        for (Camera.Size size : supportedPicResolutions) {
            if (size.width <= ALLOW_PIC_LEN) {
                if (resultSize == null) {
                    resultSize = size;
                } else if (size.width > resultSize.width) {
                    resultSize = size;
                }
            }
        }
        if (resultSize == null) {
            return supportedPicResolutions.get(0);
        }
        return resultSize;
    }

}

