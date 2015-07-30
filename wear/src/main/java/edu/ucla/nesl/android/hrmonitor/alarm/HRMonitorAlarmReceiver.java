package edu.ucla.nesl.android.hrmonitor.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import edu.ucla.nesl.android.hrmonitor.DataMapClient;

/**
 * Created by cgshen on 7/29/15.
 */
public class HRMonitorAlarmReceiver extends BroadcastReceiver {
    private final static String TAG = "Wear/HRAlarmReceiver";
    private final static int SENSING_PERIOD = 1000 * 90;
    private final static int ALARM_INTERVAL = 1000 * 60 * 5; // Millisec * Second * Minute
    private final static int MAX_HR = 200;
    private final static boolean executeLocal = false;
    private final static boolean saveToFile = false;

    private static Sensor mHRSensor = null;
    private static SensorManager mSensorManager = null;
    private static PowerManager.WakeLock mWakeLock = null;
    private static Vibrator mVibrator = null;
    private static DataMapClient mDataMapClient = null;
    private HRMonitorListener mListener;
    private Context mContext;

    private BufferedWriter output = null;

    public void setAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, HRMonitorAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * 20, ALARM_INTERVAL, pi);
        Log.i(TAG, "Alarm set.");
    }

    public void cancelAlarm(Context context) {
        Intent intent = new Intent(context, HRMonitorAlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
        Log.i(TAG, "Alarm cancelled.");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        PowerManager mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "hr_wakelock");
        wl.acquire();

        mContext = context;
        Log.i(TAG, "InferenceAlarmReceiver received, executeLocal=" + executeLocal + ", saveToFile=" + saveToFile);

        // Init
        if (mSensorManager == null) {
            mSensorManager = ((SensorManager) context.getSystemService(Context.SENSOR_SERVICE));
        }
        if (mHRSensor == null) {
            mHRSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        }
        if (mVibrator == null) {
            mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (mDataMapClient == null) {
            mDataMapClient = DataMapClient.getInstance(context);
        }

        if (saveToFile) {
            try {
                output = new BufferedWriter(new FileWriter("/sdcard/hr_data.txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Start monitoring
        mListener = new HRMonitorListener();
        if (mSensorManager != null) {
            mSensorManager.registerListener(mListener, mHRSensor, SensorManager.SENSOR_DELAY_NORMAL);
            mVibrator.vibrate(100);
        }
        else {
            Log.w(TAG, "SensorManager null.");
        }

        // Stop after SENSING_PERIOD
        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mSensorManager != null) {
                    mSensorManager.unregisterListener(mListener, mHRSensor);
                    mVibrator.vibrate(1500);
                    mSensorManager = null;
                } else {
                    Log.i(TAG, "SensorManager null.");
                }
                mHRSensor = null;
                mVibrator = null;
                mDataMapClient = null;
                if (saveToFile) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Log.i(TAG, "InferenceAlarmReceiver execution finished.");
                // Cancel alarm after one trigger
                cancelAlarm(mContext);
            }
        }, SENSING_PERIOD);

        wl.release();
    }

    private class HRMonitorListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            // Log.i(TAG, "New HR reading, value=" + sensorEvent.values[0]);

            float realHR = ((float) Math.random() * 100.0f);
            if (executeLocal) {
                // Vibrate to warn the user when HR exceeds 90% of max HR
                if (realHR >= MAX_HR * 0.475) {
                    Log.i(TAG, "Alerting - max HR");
                    if (mVibrator != null) {
                        mVibrator.vibrate(100);
                    }
                }
                if (saveToFile && output != null) {
                    try {
                        output.write(sensorEvent.timestamp + "," + realHR + "\n");
                        output.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else {
                // Send data to phone in order to get notification back
                mDataMapClient.sendSensorData(sensorEvent.timestamp, realHR);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }
}
