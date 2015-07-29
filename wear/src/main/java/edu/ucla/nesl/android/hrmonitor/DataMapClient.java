package edu.ucla.nesl.android.hrmonitor;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import edu.ucla.nesl.android.hrmonitor.shared.StringKey;

public class DataMapClient {
    private static final String TAG = "Wear/DataMapClient";
    private static final int CLIENT_CONNECTION_TIMEOUT = 15000;

    public static DataMapClient instance;

    public static DataMapClient getInstance(Context context) {
        if (instance == null) {
            instance = new DataMapClient(context.getApplicationContext());
        }

        return instance;
    }

    private GoogleApiClient googleApiClient;
    private ExecutorService executorService;

    private DataMapClient(Context context) {
        googleApiClient = new GoogleApiClient.Builder(context).addApi(Wearable.API).build();
        executorService = Executors.newCachedThreadPool();
    }

    public void sendSensorData(final long timestamp, final float value) {
        // sendSensorDataInBackground(timestamp, value);
        // Log.i(TAG, "In sendSensorData");
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                sendSensorDataInBackground(timestamp, value);
            }
        });
    }

    private void sendSensorDataInBackground(long timestamp, float value) {
        PutDataMapRequest dataMap = PutDataMapRequest.create(StringKey.HEARTRATE);
        dataMap.getDataMap().putLong(StringKey.TIMESTAMP, timestamp);
        dataMap.getDataMap().putFloat(StringKey.VALUE, value);

        PutDataRequest putDataRequest = dataMap.asPutDataRequest();
        Log.i(TAG, "value=" + dataMap.getDataMap().getFloat(StringKey.VALUE));
        send(putDataRequest);
    }

    private boolean validateConnection() {
        if (googleApiClient.isConnected()) {
            return true;
        }

        ConnectionResult result = googleApiClient.blockingConnect(CLIENT_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
        return result.isSuccess();
    }

    private void send(PutDataRequest putDataRequest) {
        if (validateConnection()) {
            // Log.i(TAG, "Sending PutDataRequest...");
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    Log.v(TAG, "Sending data message: " + dataItemResult.getStatus().isSuccess());
                }
            });
        }
        else {
            Log.w(TAG, "Connection not valid");
        }
    }
}
