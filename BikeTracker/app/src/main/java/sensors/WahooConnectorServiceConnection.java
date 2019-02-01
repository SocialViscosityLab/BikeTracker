package sensors;

import java.util.ArrayList;
import java.util.Collection;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.wahoofitness.connector.HardwareConnector;
import com.wahoofitness.connector.conn.connections.SensorConnection;
import com.wahoofitness.connector.conn.connections.params.ConnectionParams;

public class WahooConnectorServiceConnection {

    private static String TAG = "DEBUGGING";

    public interface Listener {

        void onHardwareConnectorServiceConnected(WahooConnectorService hardwareConnectorService);

        void onHardwareConnectorServiceDisconnected();

    }

    private final Context mContext;
    private WahooConnectorService mHardwareConnectorService = null;
    private final Listener mListener;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            WahooConnectorService.WahooConnectorServiceBinder hardwareConnectorServiceBinder = (WahooConnectorService.WahooConnectorServiceBinder) binder;
            WahooConnectorService hardwareConnectorService = hardwareConnectorServiceBinder
                    .getService();
            mHardwareConnectorService = hardwareConnectorService;
            mListener.onHardwareConnectorServiceConnected(mHardwareConnectorService);
            Log.d(TAG, "WCSC on service connected");

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mHardwareConnectorService = null;
            mListener.onHardwareConnectorServiceDisconnected();
            Log.d(TAG, "WCSC on service disconnected");
        }
    };

    public WahooConnectorServiceConnection(Context context, Listener listener) {
        mContext = context;
        mListener = listener;


        mContext.startService(new Intent(mContext, WahooConnectorService.class));

        Intent intent = new Intent(mContext, WahooConnectorService.class);
        context.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public SensorConnection connectSensor(ConnectionParams params) {
        Log.d(TAG, "WCSC on sensor connection");

        if (mHardwareConnectorService != null) {
            return mHardwareConnectorService.connectSensor(params);
        } else {
            return null;
        }
    }

    public void disconnectSensor(ConnectionParams params) {
        if (mHardwareConnectorService != null) {
            mHardwareConnectorService.disconnectSensor(params);
        }

    }

    public boolean enableDiscovery(boolean enable) {
        if (mHardwareConnectorService != null) {
            return mHardwareConnectorService.enableDiscovery(enable);

        } else {
            return false;
        }
    }

    public Collection<ConnectionParams> getDiscoveredConnectionParams() {
        if (mHardwareConnectorService != null) {
            return mHardwareConnectorService.getDiscoveredConnectionParams();
        } else {
            return new ArrayList<ConnectionParams>();
        }
    }

    public HardwareConnector getHardwareConnector() {
        if (mHardwareConnectorService != null) {
            return mHardwareConnectorService.getHardwareConnector();

        } else {
            return null;
        }
    }

    public WahooConnectorService getHardwareConnectorService() {
        return mHardwareConnectorService;
    }

    public SensorConnection getSensorConnection(ConnectionParams params) {
        if (mHardwareConnectorService != null) {
            return mHardwareConnectorService.getSensorConnection(params);
        } else {
            return null;
        }
    }

    public Collection<SensorConnection> getSensorConnections() {
        if (mHardwareConnectorService != null) {
            return mHardwareConnectorService.getSensorConnections();
        } else {
            return new ArrayList<SensorConnection>();
        }
    }

    public boolean isBound() {
        return (mHardwareConnectorService != null)
                && (mHardwareConnectorService.getHardwareConnector() != null);
    }

    public boolean isDiscovering() {
        if (mHardwareConnectorService != null) {
            return mHardwareConnectorService.isDiscovering();

        } else {
            return false;
        }
    }

    public void unbind() {
        mContext.unbindService(mServiceConnection);

    }
}
