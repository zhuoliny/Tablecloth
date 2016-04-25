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
public class TaskFourStaticFourLEDActivity extends Activity {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskFourStaticFourLEDActivity.class);

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
                edu.buffalo.tablecloth.view.TaskFourStaticFourLEDActivity.this, 0, new Intent(TableclothService.ACTION_USB_PERMISSION_REQUEST), 0);
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

    static private int ledIndex0 = 0;
    static private int ledIndex1 = 1;
    static private int ledIndex2 = 8;
    static private int ledIndex3 = 9;

    static private int[] status = new int[]{
            1, 1, 0, 0, 0, 0, 0, 0,
            1, 1, 0, 0, 0, 0, 0, 0,
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

    static private int pressure0 = 0;
    static private int pressure1 = 1;
    static private int pressure2 = 2;
    static private int pressure3 = 3;
    static private int pressure4 = 16;
    static private int pressure5 = 17;
    static private int pressure6 = 18;
    static private int pressure7 = 19;
    static private int pressure8 = 32;
    static private int pressure9 = 33;
    static private int pressure10 = 34;
    static private int pressure11 = 35;
    static private int pressure12 = 48;
    static private int pressure13 = 49;
    static private int pressure14 = 50;
    static private int pressure15 = 51;
    static private int[] pressureSensedIndex = new int[]{
            pressure0, pressure1, pressure2, pressure3,
            pressure4, pressure5, pressure6, pressure7,
            pressure8, pressure9, pressure10, pressure11,
            pressure12, pressure13, pressure14, pressure15
    };

    static boolean pressureSensed = false;

    @Receiver(actions = TableclothService.ACTION_TABLECLOTH_DATA)
    protected void onReceivedData(@Receiver.Extra(TableclothService.EXTRA_DATA) int[] pressures) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd,HHmmss.SSS").format(Calendar.getInstance().getTimeInMillis());

        pressureSensed = false;

        for (int i=0;i<16;i++) {
            if (pressures[pressureSensedIndex[i]] > 1) {
                pressureSensed = true;
                break;
            }
        }

        if (pressureSensed == false) {
            for (int i=0;i<96;i++) {
                if (i==ledIndex0 || i==ledIndex1 || i==ledIndex2 || i==ledIndex3) status[i] = 1;
                else status[i] = 0;
            }
        } else {
            pressureSensed = false;
            if (ledIndex0==6 || ledIndex0==22 || ledIndex0==38 || ledIndex0==54 || ledIndex0 ==70) ledIndex0 += 8;
            if (ledIndex1==7 || ledIndex1==23 || ledIndex1==39 || ledIndex1==55 || ledIndex1 ==71) ledIndex1 += 8;
            if (ledIndex2==14 || ledIndex2==30 || ledIndex2==46 || ledIndex2==62 || ledIndex2 ==78) ledIndex2 += 8;
            if (ledIndex3==15 || ledIndex3==31 || ledIndex3==47 || ledIndex3==63 || ledIndex3 == 79) ledIndex3 += 8;
            ledIndex0 += 2;
            ledIndex1 += 2;
            ledIndex2 += 2;
            ledIndex3 += 2;
            if (pressure0==12 || pressure0==76 || pressure0==140 || pressure0==204 || pressure0==268
                    || pressure0==348 ) pressure0 += 48;
            if (pressure1==13 || pressure1==77 || pressure1==141 || pressure1==205 || pressure1==273
                    || pressure1==337 ) pressure1 += 48;
            if (pressure2==14 || pressure2==78 || pressure2==142 || pressure2==206 || pressure2==274
                    || pressure2==338 ) pressure2 += 48;
            if (pressure3==15 || pressure3==79 || pressure3==143 || pressure3==207 || pressure3==275
                    || pressure3==339 ) pressure3 += 48;
            if (pressure4==28 || pressure4==92 || pressure4==156 || pressure4==220 || pressure4==284
                    || pressure4==348 ) pressure4 += 48;
            if (pressure5==29 || pressure5==93 || pressure5==157 || pressure5==221 || pressure5==285
                    || pressure5==349 ) pressure5 += 48;
            if (pressure6==30 || pressure6==94 || pressure6==158 || pressure6==222 || pressure6==286
                    || pressure6==350 ) pressure6 += 48;
            if (pressure7==31 || pressure7==95 || pressure7==159 || pressure7==223 || pressure7==287
                    || pressure7==351 ) pressure7 += 48;
            if (pressure8==44 || pressure8==108 || pressure8==172 || pressure8==236 || pressure8==304
                    || pressure8==368 ) pressure8 += 48;
            if (pressure9==45 || pressure9==109 || pressure9==173 || pressure9==237 || pressure9==305
                    || pressure9==369 ) pressure9 += 48;
            if (pressure10==46 || pressure10==110 || pressure10==174 || pressure10==238 || pressure10==306
                    || pressure10==370 ) pressure10 += 48;
            if (pressure11==47 || pressure11==111 || pressure11==175 || pressure11==239 || pressure11==307
                    || pressure11==371 ) pressure11 += 48;
            if (pressure12==60 || pressure12==124 || pressure12==188 || pressure12==252 || pressure12==316
                    || pressure12==380 ) pressure12 += 48;
            if (pressure13==61 || pressure13==125 || pressure13==189 || pressure13==253 || pressure13==317
                    || pressure13==381 ) pressure13 += 48;
            if (pressure14==62 || pressure14==126 || pressure14==190 || pressure14==254 || pressure14==318
                    || pressure14==382 ) pressure14 += 48;
            if (pressure15==63 || pressure15==127 || pressure15==191 || pressure15==255 || pressure15==319
                    || pressure15==383 ) pressure15 += 48;
            pressure0 += 4; pressure1 += 4; pressure2 += 4; pressure3 += 4;
            pressure4 += 4; pressure5 += 4; pressure6 += 4; pressure7 += 4;
            pressure8 += 4; pressure9 += 4; pressure10 += 4; pressure11 += 4;
            pressure12 += 4; pressure13 += 4; pressure14 += 4; pressure15 += 4;
            pressureSensedIndex[0] = pressure0; pressureSensedIndex[1] = pressure1;
            pressureSensedIndex[2] = pressure2; pressureSensedIndex[3] = pressure3;
            pressureSensedIndex[4] = pressure4; pressureSensedIndex[5] = pressure5;
            pressureSensedIndex[6] = pressure6; pressureSensedIndex[7] = pressure7;
            pressureSensedIndex[8] = pressure8; pressureSensedIndex[9] = pressure9;
            pressureSensedIndex[10] = pressure10; pressureSensedIndex[11] = pressure11;
            pressureSensedIndex[12] = pressure12; pressureSensedIndex[13] = pressure13;
            pressureSensedIndex[14] = pressure14; pressureSensedIndex[15] = pressure15;
            for (int i=0;i<96;i++) {
                if (i==ledIndex0 || i==ledIndex1 || i==ledIndex2 || i==ledIndex3) status[i] = 1;
                else status[i] = 0;
            }
        }

        this.appendLog(timeStamp + " " + Arrays.toString(status).replace("[",",").replace("]","").trim());
        pressureSensed = false;
        mExecutorService.submit(new SendOrderRunnerable(status));
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
