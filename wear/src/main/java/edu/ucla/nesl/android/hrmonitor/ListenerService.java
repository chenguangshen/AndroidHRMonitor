package edu.ucla.nesl.android.hrmonitor;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import edu.ucla.nesl.android.hrmonitor.shared.StringKey;

/**
 * Created by cgshen on 7/27/15.
 */
public class ListenerService extends WearableListenerService {
    private static final String TAG = "Wear/ListenerService";
    private static Vibrator mVibrator = null;

    public void onCreate() {
        super.onCreate();
        if (mVibrator == null) {
            mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i(TAG, "Message received.");
        if (messageEvent.getPath().equals(StringKey.NOTIFICATION)) {
            String res = new String(messageEvent.getData());
            if (res.equals(StringKey.WARNING)) {
                Log.i(TAG, "Warning received.");
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
            else {
                Log.i(TAG, "Message content: " + res);
            }
        }
        else {
            Log.w(TAG, "Unknown message path.");
        }
    }

}
