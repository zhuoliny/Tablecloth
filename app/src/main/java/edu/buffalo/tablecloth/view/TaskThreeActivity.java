package edu.buffalo.tablecloth.view;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.OptionsMenuItem;
import org.androidannotations.annotations.Receiver;
import org.androidannotations.annotations.ViewById;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.buffalo.tablecloth.R;
import edu.buffalo.tablecloth.service.TableclothService;
import edu.buffalo.tablecloth.service.TableclothService_;
import edu.buffalo.tablecloth.widget.PseudoColorTablecloth;

@EActivity(R.layout.activity_taskone)
@OptionsMenu(R.menu.main_taskone)
public class TaskThreeActivity extends Activity {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskThreeActivity.class);

    private final ExecutorService mExecutorService = Executors.newCachedThreadPool();

    @OptionsMenuItem(R.id.mniStart)
    protected MenuItem mniStart;
    @OptionsMenuItem(R.id.mniStop)
    protected MenuItem mniStop;

    @ViewById(R.id.view_pt)
    protected PseudoColorTablecloth pseudoColorTablecloth;

    private TableclothService mTableclothService;

    private String uid;

    private ServiceConnection mTableclothServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LOGGER.debug("connected service: {}", name.getClass());
            uid = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            mTableclothService = ((TableclothService.LocalBinder) service).getService();
            mTableclothService.initUsb();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LOGGER.debug("disconnected service: {}", this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        bindService(TableclothService_.intent(this).get(), mTableclothServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mTableclothServiceConnection);
    }

    private boolean connected = false;

    @Receiver(actions = UsbManager.ACTION_USB_DEVICE_ATTACHED)
    protected void onDeviceAttached(@Receiver.Extra Intent intent) {
        final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        mTableclothService.connectUsbDevice(device);
    }


    @Receiver(actions = UsbManager.ACTION_USB_DEVICE_DETACHED)
    protected void onDeviceDetached() {
        connected = false;
    }

    @Receiver(actions = TableclothService.ACTION_USBMANAGER_IS_NULL)
    protected void onFailGetUsbManager() {
        showToast("Fail to get UsbManager !");
        finish();
    }

    @Receiver(actions = TableclothService.ACTION_USB_PERMISSION_FAILED)
    protected void onUsbPermissionFailed() {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                edu.buffalo.tablecloth.view.TaskThreeActivity.this, 0, new Intent(TableclothService.ACTION_USB_PERMISSION_REQUEST), 0);
        mTableclothService.requestPermission(pendingIntent);
    }

    @Receiver(actions = TableclothService.ACTION_USB_PERMISSION_REQUEST)
    protected void onUsbPermissionSucceed(@Receiver.Extra Intent intent) {
        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            mTableclothService.connectUsbDevice(device);
        } else {
            showToast("permission denied !");
        }
    }

    @Receiver(actions = TableclothService.ACTION_CONNECTION_FAILED)
    protected void onConnectionFailed() {
        showToast("connect fail ! please try again");
    }

    @Receiver(actions = TableclothService.ACTION_CONNECTION_SUCCEED)
    protected void onConnectionSucceed() {
        connected = true;
        showToast("connect success !");
    }

    private int[] status;

    @Receiver(actions = TableclothService.ACTION_TABLECLOTH_DATA)
    protected void onReceivedData(@Receiver.Extra(TableclothService.EXTRA_DATA) int[] pressures) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd,HHmmss.SSS").format(Calendar.getInstance().getTimeInMillis());
        this.appendLog(timeStamp + " " + Arrays.toString(pressures).replace("[",",").replace("]","").trim());

        boolean pressureSensed = false;

        for (int i=0;i<384;i++) {
            if (pressures[i] > 1) {
                if (i==198 || i==199 || i==214 || i==215)    //At point
                    pressureSensed = true;
                break;
            }
        }

        if (pressureSensed == false) {
            status = new int[]{
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 2, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
            };
            this.appendLog(timeStamp + " " + Arrays.toString(status).replace("[",",").replace("]","").trim());
            mExecutorService.submit(new SendOrderRunnerable(status));
        } else {
            status = new int[]{
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0,
            };
            this.appendLog(timeStamp + " " + Arrays.toString(status).replace("[",",").replace("]","").trim());
            mExecutorService.submit(new SendOrderRunnerable(status));
        }

        pseudoColorTablecloth.reFresh(pressures);
    }

    private void appendLog(String text) {
        File logFile = new File("sdcard/tablecloth/" + this.uid + ".csv");
        if(!logFile.exists()) {
            try {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(logFile, true));
            bufferedWriter.append(text);
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OptionsItem(R.id.mniStart)
    protected void startToRead() {
        if (!connected) {
            showToast("usb disconnected");
            return;
        }
        if (mTableclothService.startReadUsbDeviceData()) {
            mniStart.setVisible(false);
            mniStop.setVisible(true);
        } else {
            showToast("Unknown error");
        }
    }

    @OptionsItem(R.id.mniStop)
    protected void stopReading() {
        if (mTableclothService.stopReadUsbDeviceData()) {
            mniStart.setVisible(true);
            mniStop.setVisible(false);
        }
    }

    private class SendOrderRunnerable implements Runnable {

        private final int[] status;
        private final int LIGHTS_COUNT = 8;
        private final int ORDERS_LENGTH = 25;

        public SendOrderRunnerable(int[] status) {
            this.status = status;
        }

        @Override
        public void run() {
            byte[] command = convertStatusToOrders(status);
            mTableclothService.startWriteUsbDeviceCommand(command);
        }

        private byte[] convertStatusToOrders(int[] status) {
            byte[] command = new byte[ORDERS_LENGTH];
            int[] greenLightStatus = new int[LIGHTS_COUNT];
            int[] redLightStatus = new int[LIGHTS_COUNT];
            int lightLocation = 0, commandLocation = 0;

            for (int i = 0; i < status.length; i++) {
                if (status[i] == 0) {
                    greenLightStatus[lightLocation] = 0;
                    redLightStatus[lightLocation] = 0;
                } else if (status[i] == 1) {
                    greenLightStatus[lightLocation] = 1;
                    redLightStatus[lightLocation] = 0;
                } else if (status[i] == 2) {
                    greenLightStatus[lightLocation] = 0;
                    redLightStatus[lightLocation] = 1;
                } else {
                    LOGGER.debug("get the wrong status {}", status[i]);
                }

                if (lightLocation == 7) {
                    lightLocation = 0;
                    command[commandLocation] = convertIntArrayToByte(redLightStatus);
                    commandLocation++;
                    command[commandLocation] = convertIntArrayToByte(greenLightStatus);
                    commandLocation++;
                } else {
                    lightLocation++;
                }
            }
            Log.d("orders: ", Arrays.toString(command));
            return command;
        }

        private byte convertIntArrayToByte(int[] lightStatus) {
            byte order = 0x00;
            for (int i = 0; i < lightStatus.length; i++) {
                order += (byte) (lightStatus[i] << i);
            }
            return order;
        }
    }


    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
