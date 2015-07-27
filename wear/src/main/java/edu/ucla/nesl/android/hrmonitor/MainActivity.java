package edu.ucla.nesl.android.hrmonitor;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String TAG = "mobile/MainActivity";
    private boolean mBound = false;
    private MainService mService;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MainService.LocalBinder binder = (MainService.LocalBinder) iBinder;
            mService = binder.getService();
            mBound = true;
            Log.i(TAG, "bound to service: " + mService.toString());
            mService.init();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause() called.");
        if (mBound) {
            unbindService(mServiceConnection);
            mBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume() called.");
        Intent intent = new Intent(this, MainService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void startMonitor(View view) {
        Log.i(TAG, "start clicked");
        if (mBound) {
            mService.startMonitor();
        }
    }

    public void stopMonitor(View view) {
        Log.i(TAG, "stop clicked");
        if (mBound) {
            mService.stopMonitor();
        }
    }
}
