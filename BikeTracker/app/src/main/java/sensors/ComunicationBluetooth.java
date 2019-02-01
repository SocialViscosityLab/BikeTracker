package sensors;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ComunicationBluetooth extends Thread{
    private BluetoothDevice device;
    private BluetoothSocket bSocket;
    //Debugging string
    private static String TAG = "DEBUGGING";


    public ComunicationBluetooth(BluetoothDevice device) {
        this.device = device;
        try {
            //bSocket = device.createRfcommSocketToServiceRecord(UUID);
            bSocket.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
    @Override
    public void run() {
        super.run();
    }
}
