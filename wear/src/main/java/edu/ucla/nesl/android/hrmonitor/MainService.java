package edu.ucla.nesl.android.hrmonitor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;

/**
 * Created by cgshen on 7/25/15.
 */
public class MainService extends Service implements SensorEventListener {
    private static final String TAG = "mobile/MainService";
    private static final int MAX_HR = 200;
    private static final boolean EXECUTE_LOCAL = true;

    private static Sensor mHRSensor = null;
    private static SensorManager mSensorManager = null;
    private static PowerManager.WakeLock mWakeLock = null;
    private static Vibrator mVibrator = null;

    public class LocalBinder extends Binder {
        public MainService getService() {
            return MainService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void init() {
        if (mSensorManager == null) {
            mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        }
        if (mHRSensor == null) {
            mHRSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        }
        if (mVibrator == null) {
            mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    public void startMonitor() {
        Log.i(TAG, "in startMonitor()");

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HRMonitor");
        if (mWakeLock != null) {
            mWakeLock.acquire();
        }
        else {
            Log.w(TAG, "Wakelock null.");
        }

        if (mSensorManager != null) {
            mSensorManager.registerListener(this, mHRSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        else {
            Log.w(TAG, "SensorManager null.");
        }
    }

    public void stopMonitor() {
        Log.i(TAG, "in stopMonitor()");
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
        else {
            Log.w(TAG, "Wakelock null.");
        }
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this, mHRSensor);
        }
        else {
            Log.w(TAG, "SensorManager null.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.i(TAG, "New HR reading, value=" + sensorEvent.values[0]);

        if (EXECUTE_LOCAL) {
            // Vibrate to warn the user when HR exceeds 90% of max HR
            if (sensorEvent.values[0] >= MAX_HR * 0.9) {
                if (mVibrator != null) {
                    mVibrator.vibrate(100);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mVibrator.vibrate(100);
                }
            }
        }
        else {
            // TODO: send data to phone, get notification back
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
