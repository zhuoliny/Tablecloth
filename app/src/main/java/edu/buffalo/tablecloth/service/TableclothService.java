package edu.buffalo.tablecloth.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.androidannotations.annotations.EService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


@EService
public class TableclothService extends Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(TableclothService.class);

    public static final String ACTION_USBMANAGER_IS_NULL = "edu.buffalo.tablecloth.service.TableclothService.ACTION_USBMANAGER_IS_NULL";
    public static final String ACTION_USB_PERMISSION_FAILED = "edu.buffalo.tablecloth.service.TableclothService.ACTION_USB_PERMISSION_FAILED";
    public static final String ACTION_USB_PERMISSION_REQUEST = "edu.buffalo.tablecloth.service.TableclothService.ACTION_USB_PERMISSION_REQUEST";
    public static final String ACTION_CONNECTION_FAILED = "edu.buffalo.tablecloth.service.TableclothService.ACTION_CONNECTION_FAILED";
    public static final String ACTION_CONNECTION_SUCCEED = "edu.buffalo.tablecloth.service.TableclothService.ACTION_CONNECTION_SUCCEED";
    public static final String ACTION_TABLECLOTH_DATA = "edu.buffalo.tablecloth.service.TableclothService.ACTION_TABLECLOTH_DATA";
    public static final String EXTRA_DATA = "edu.buffalo.tablecloth.service.TableclothService.EXTRA_DATA";

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
    private ScheduledFuture<?> readDataScheduledFuture;
    private ScheduledFuture<?> writeDataScheduledFuture;
    private ScheduledFuture<?> takeDataScheduledFuture;

    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;
    private UsbDeviceConnection mUsbDeviceConnection;
    private UsbEndpoint mUsbEndpointRead, mUsbEndpointWrite;

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        Log.e("IBinder", "bind service success");
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public TableclothService getService() {
            return TableclothService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LOGGER.debug("unbind service");
        close();
        return super.onUnbind(intent);
    }


    @Override
    public void onCreate() {
        super.onCreate();

        takeDataScheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
                new TakeDataRunnable(), 0, 1, TimeUnit.MILLISECONDS);
    }

    private void close() {
        if (readDataScheduledFuture != null) {
            readDataScheduledFuture.cancel(true);
            readDataScheduledFuture = null;
        }
        if (writeDataScheduledFuture != null) {
            writeDataScheduledFuture.cancel(true);
            writeDataScheduledFuture = null;
        }
        if (takeDataScheduledFuture != null) {
            takeDataScheduledFuture.cancel(true);
            takeDataScheduledFuture = null;
        }
        scheduledExecutorService.shutdown();
    }

    public void initUsb() {
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (mUsbManager == null) {
            sendBroadcast(new Intent(ACTION_USBMANAGER_IS_NULL));
            return;
        }
    }

    public void connectUsbDevice(UsbDevice usbDevice) {
        this.mUsbDevice = usbDevice;
        if (mUsbManager.hasPermission(usbDevice)) {
            startConnectDevice(usbDevice);
        } else {
            sendBroadcast(new Intent(ACTION_USB_PERMISSION_FAILED));
            return;
        }
    }

    public void requestPermission(PendingIntent pendingIntent) {
        mUsbManager.requestPermission(mUsbDevice, pendingIntent);
    }

    private void startConnectDevice(UsbDevice device) {
        if (device == null) {
            sendConnectionMessage("device is null !");
            return;
        }

        mUsbDeviceConnection = mUsbManager.openDevice(device);
        if (mUsbDeviceConnection == null) {
            sendConnectionMessage("mUsbDeviceConnection is null !");
            return;
        }

        UsbInterface usbInterface = null;

        for (int i = 0; i < device.getInterfaceCount(); i++) {
            if (device.getInterface(i).getInterfaceClass() == 0x0a) {
                usbInterface = device.getInterface(i);
                LOGGER.debug("get the target usbInterface in place {}", i);
                break;
            }
        }

        if (usbInterface == null) {
            sendConnectionMessage("usbInterface is null !");
            return;
        }

        if (!mUsbDeviceConnection.claimInterface(usbInterface, true)) {
            sendConnectionMessage("fail to claimInterface !");
            return;
        }

        if (!getUsbEndpoint(usbInterface)) {
            sendConnectionMessage("fail to get the both UsbEndpoint !");
        } else {
            sendBroadcast(new Intent(ACTION_CONNECTION_SUCCEED));
        }
    }

    private boolean getUsbEndpoint(UsbInterface usbInterface) {
        for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
            UsbEndpoint usbEndpoint = usbInterface.getEndpoint(i);
            if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                mUsbEndpointRead = usbEndpoint;
                LOGGER.debug("get the target usbEndpointRead");
                if (mUsbEndpointWrite != null) {
                    return true;
                }
            } else if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                mUsbEndpointWrite = usbEndpoint;
                LOGGER.debug("get the target usbEndpointWrite");
                if (mUsbEndpointRead != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private void sendConnectionMessage(String message) {
        sendBroadcast(new Intent(ACTION_CONNECTION_FAILED));
        LOGGER.debug("fail to connected device because :{}", message);
    }

    public boolean startReadUsbDeviceData() {
        if (readDataScheduledFuture != null) {
            readDataScheduledFuture.cancel(true);
            readDataScheduledFuture = null;
        }
        readDataScheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
                new ReadDataRunnable(), 0, 1, TimeUnit.MILLISECONDS);                                                   //默认循环周期是1毫秒
        return true;
    }

    public boolean stopReadUsbDeviceData() {
        if (readDataScheduledFuture != null) {
            readDataScheduledFuture.cancel(true);
            readDataScheduledFuture = null;
        }
        return true;
    }

    public void startWriteUsbDeviceCommand(byte[] buffer) {
        scheduledExecutorService.submit(new WriteDataRunnable(buffer));
    }

    private final LinkedBlockingQueue<byte[]> mQueue = new LinkedBlockingQueue<byte[]>();
    private final int TIME_OUT_MILLISECONDS = 2_000;
    private final int DATA_LENGTH = 768;

    private class ReadDataRunnable implements Runnable {

        @Override
        public void run() {

            if (mUsbEndpointRead == null) {
                LOGGER.debug("fail to read device data because mUsbEndpointRead is null");
                return;
            }

            byte[] buffer = new byte[DATA_LENGTH];
            int transfer = mUsbDeviceConnection.bulkTransfer(mUsbEndpointRead, buffer, buffer.length,
                    TIME_OUT_MILLISECONDS);

            if (transfer < 0) {
                LOGGER.debug("transfer is less than zore,is {}", transfer);
            } else {
                try {
                    mQueue.put(buffer);
                } catch (InterruptedException e) {
                    LOGGER.error("put data to queue error {}", e);
                }
                LOGGER.debug("transfer is more than zore,is {}", transfer);
            }
        }
    }

    private class WriteDataRunnable implements Runnable {

        private byte[] buffer;

        public WriteDataRunnable(byte[] buffer) {
            this.buffer = buffer;
        }

        @Override
        public void run() {
            if (mUsbEndpointWrite == null) {
                return;
            }
            int transfer = mUsbDeviceConnection.bulkTransfer(mUsbEndpointWrite, buffer, buffer.length,
                    TIME_OUT_MILLISECONDS);
            if (transfer < 0) {
                LOGGER.debug("transfer is less than zore,is :{}", transfer);
            } else {
                LOGGER.debug("transfer is more than zore,is :{}", transfer);
            }
        }
    }

    private class TakeDataRunnable implements Runnable {
        @Override
        public void run() {
            byte[] data = null;
            try {
                data = mQueue.take();
            } catch (InterruptedException e) {
                LOGGER.error("Take Data from mQueue error :{}", e);
            }
            if (data == null && data.length != DATA_LENGTH) {
                LOGGER.debug("get the wrong data which length is {}", data.length);
                return;
            }
            LOGGER.debug("native 768 data : {}", Arrays.toString(data));
            int[] pressures = convertData(data);
            //Toast.makeText(getApplicationContext(), Arrays.toString(data), Toast.LENGTH_SHORT).show();
            Log.i("Pressure", Arrays.toString(pressures));
            sendBroadcast(new Intent(ACTION_TABLECLOTH_DATA).putExtra(EXTRA_DATA, pressures));
        }
    }

    int flag = 1024;

    private int[] convertData(byte[] data) {
        int[] pressure = new int[data.length / 2];
        int j = 0;
        for (int i = 0; i < data.length; i += 2) {
            int p = (data[i + 1] << 8 | data[i] & 255);
            if (p > flag) {
                pressure[j] = 1;
            } else {
                pressure[j] = p;
            }
            j++;
        }
        LOGGER.debug("pressure 384 data: " + Arrays.toString(pressure));
        return pressure;
    }

    public void add() {
        flag += 100;
    }

    public void cut() {
        flag -= 100;
    }

    public int getFlag() {
        return flag;
    }
}
