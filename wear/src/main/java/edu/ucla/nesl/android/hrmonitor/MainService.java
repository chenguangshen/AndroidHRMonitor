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
    private static final String TAG = "Wear/HRMainService";
    private static final int MAX_HR = 200;

    private static Sensor mHRSensor = null;
    private static SensorManager mSensorManager = null;
    private static PowerManager.WakeLock mWakeLock = null;
    private static Vibrator mVibrator = null;
    private static DataMapClient mDataMapClient = null;
    private static boolean executeLocal = true;
    private static boolean init = false;

    public class LocalBinder extends Binder {
        public MainService getService() {
            return MainService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        if (!init) {
            init = true;
            init();
        }
        return mBinder;
    }

    private void init() {
        if (mSensorManager == null) {
            mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        }
        if (mHRSensor == null) {
            mHRSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        }
        if (mVibrator == null) {
            mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (mDataMapClient == null) {
            mDataMapClient = DataMapClient.getInstance(this);
        }
    }

    public void startMonitor() {
        Log.i(TAG, "in startMonitor()");

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HRMonitor");
        mWakeLock.acquire();

        if (mSensorManager != null) {
            mSensorManager.registerListener(MainService.this, mHRSensor, SensorManager.SENSOR_DELAY_NORMAL);
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
            Log.i(TAG, "Wakelock null.");
        }
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(MainService.this, mHRSensor);
        }
        else {
            Log.i(TAG, "SensorManager null.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.i(TAG, "New HR reading, value=" + sensorEvent.values[0]);

        if (executeLocal) {
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
            // Send data to phone in order to get notification back
            mDataMapClient.sendSensorData(sensorEvent.timestamp, sensorEvent.values[0] + ((float) Math.random() * 100.0f));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
