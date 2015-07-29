package edu.ucla.nesl.android.hrmonitor;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.ucla.nesl.android.hrmonitor.shared.StringKey;

/**
 * Created by cgshen on 7/27/15.
 */
public class DataMapListener implements DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "Mobile/DataMapListener";
    private static final int MAX_HR = 200;

    private GoogleApiClient mGoogleApiClient;
    private ExecutorService executorService;

    public DataMapListener(Context context) {
        Log.i(TAG, "In constructor.");

        // Start the api client for message sending
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        executorService = Executors.newCachedThreadPool();
    }

    public void connect () {
        mGoogleApiClient.connect();
    }

    public void disconnect() {
        Log.i(TAG, "DataMapListener disconnected...");
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged()");
        for (DataEvent dataEvent : dataEvents) {
            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataItem dataItem = dataEvent.getDataItem();
                Uri uri = dataItem.getUri();
                String path = uri.getPath();

                if (path.equals(StringKey.HEARTRATE)) {
                    unpackSensorData(DataMapItem.fromDataItem(dataItem).getDataMap());
                }
                else {
                    Log.w(TAG, "Unknown data path.");
                }
            }
        }
    }

    private void unpackSensorData(DataMap dataMap) {
        long timestamp = dataMap.getLong(StringKey.TIMESTAMP);
        float value = dataMap.getFloat(StringKey.VALUE);
        Log.d(TAG, "Received HR data, ts=" + timestamp + ", value=" + value);

        // Send message to the watch for warning notification
        if (value >= MAX_HR * 0.9) {
            sendMessageAsync(StringKey.NOTIFICATION, StringKey.WARNING);
        }
    }

    private void sendMessageAsync(final String path, final String message) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                    for (Node node : nodes.getNodes()) {
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, message.getBytes()).await();
                        if (result.getStatus().isSuccess()) {
                            Log.i(TAG, "Warning sent to: " + node.getDisplayName());
                        }
                        else {
                            // Log an error
                            Log.e(TAG, "ERROR: failed to send Message");
                        }
                    }
                }
                else {
                    Log.w(TAG, "API client not connected...");
                }
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "API client connection successful.");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Log.d(TAG, "DataApi listener registered");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "API client connection suspended.");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "API client connection failed, result=" + connectionResult.toString());
    }
}