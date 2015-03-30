package ru.rbss.harry;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.util.Set;

public class BluetoothService extends IntentService {

    private static final String TAG = "BluetoothService";

    public BluetoothService() {
        super("BluetoothService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String data = intent.getStringExtra("data");

            BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
            if (bt != null) {
                if (!bt.isEnabled()){}
                    //Toast.makeText(this, "no bt device available", Toast.LENGTH_SHORT).show();
                else {
                    Set<BluetoothDevice> devices = bt.getBondedDevices();
                    Log.d(TAG, "searching device");
                    for (BluetoothDevice device : devices)
                        
                        if (device.getName().equals("HC-06")) {
                            Log.d(TAG, "device found");

                            BluetoothConnector bc = new BluetoothConnector(device, true, bt, null );
                            try {
                                BluetoothConnector.BluetoothSocketWrapper wrapper = bc.connect();
                                try{
                                    wrapper.getOutputStream().write(42);
                                    Log.d(TAG, "byte sent");
                                } finally {
                                    Log.d(TAG, "closing socket");
                                    wrapper.close();
                                }
                            } catch(IOException e) {
                                e.printStackTrace();
                            }
                        }


                }

            }
        }

    }

}
