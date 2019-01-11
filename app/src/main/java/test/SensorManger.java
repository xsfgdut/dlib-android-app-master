package test;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by zouguibao on 2017/11/23.
 */

public class SensorManger implements SensorEventListener{
    public static SensorManger intance;
    private boolean autoFocus;
    private OnAccelSensorListener accelSensorListener;
    private SensorManager sensorManager;//使用传感器实现连续自动调整焦点
    private Sensor mAccelSensor;//重力感应
    private boolean mInvalidate = false;
    private boolean mInitialized = false;
    private float mLastX = 0;
    private float mLastY = 0;
    private float mLastZ = 0;

    private SensorManger(){

    }

    public static SensorManger getIntance(){
        if(intance == null){
            intance = new SensorManger();
        }
        return intance;
    }

    public void initSensorManager(Context context){
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void registerListener(){
        sensorManager.registerListener(this, mAccelSensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void unRegisterListener(){
        sensorManager.unregisterListener(this);
    }

    public void lockFocus(){
        autoFocus = false;
    }

    public void unLockFocus(){
        autoFocus = true;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (mInvalidate == true) {
//            mView.invalidate();
                mInvalidate = false;
            }
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            if (!mInitialized) {
                mLastX = x;
                mLastY = y;
                mLastZ = z;
                mInitialized = true;
            }
            float deltaX = Math.abs(mLastX - x);
            float deltaY = Math.abs(mLastY - y);
            float deltaZ = Math.abs(mLastZ - z);

            if (deltaX > .5 && autoFocus) {
                autoFocus = false;
                if(accelSensorListener != null){
                    accelSensorListener.onAccelSensor();
                }
            }
            if (deltaY > .5 && autoFocus) {
                autoFocus = false;
                if(accelSensorListener != null){
                    accelSensorListener.onAccelSensor();
                }
            }
            if (deltaZ > .5 && autoFocus) {
                //AUTOFOCUS (while it is not autofocusing) */
                autoFocus = false;
                if(accelSensorListener != null){
                    accelSensorListener.onAccelSensor();
                }
            }

            mLastX = x;
            mLastY = y;
            mLastZ = z;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void setAccelSensorListener(OnAccelSensorListener accelSensorListener) {
        this.accelSensorListener = accelSensorListener;
    }

    public interface OnAccelSensorListener{
        void onAccelSensor();
    }
}

