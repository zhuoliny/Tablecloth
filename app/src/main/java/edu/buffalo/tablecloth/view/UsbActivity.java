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
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.androidannotations.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.UUID;

import edu.buffalo.tablecloth.R;
import edu.buffalo.tablecloth.service.TableclothService;
import edu.buffalo.tablecloth.service.TableclothService_;
import edu.buffalo.tablecloth.widget.PseudoColorTablecloth;

@EActivity(R.layout.activity_usb)
@OptionsMenu(R.menu.main_usb)
public class UsbActivity extends Activity {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsbActivity.class);

    @ViewById(R.id.tv_flag)
    protected TextView tvFlag;
    @ViewById(R.id.btn_add)
    protected Button btnAdd;
    @ViewById(R.id.btn_cut)
    protected Button btnCut;

    @OptionsMenuItem(R.id.mniStart)
    protected MenuItem mniStart;
    @OptionsMenuItem(R.id.mniStop)
    protected MenuItem mniStop;

    @ViewById(R.id.view_pt)
    protected PseudoColorTablecloth pseudoColorTablecloth;

    private TableclothService mTableclothService;

    UUID uid;
    private ServiceConnection mTableclothServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LOGGER.debug("connected service {}", name.getClassName());
            uid = UUID.randomUUID();
            mTableclothService = ((TableclothService.LocalBinder) service).getService();
            mTableclothService.initUsb();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LOGGER.debug("disconnected service {}", this);
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

    @Receiver(actions = UsbManager.ACTION_USB_DEVICE_ATTACHED)
    protected void onDeviceAttached(@Receiver.Extra Intent intent) {
        final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        mTableclothService.connectUsbDevice(device);
    }

    @Receiver(actions = UsbManager.ACTION_USB_DEVICE_DETACHED)
    protected void onDeviceDetached() {
        //TODO 关闭连接

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
                edu.buffalo.tablecloth.view.UsbActivity.this, 0, new Intent(TableclothService.ACTION_USB_PERMISSION_REQUEST), 0);
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
        //TODO 连接失败的操作

        showToast("connect fail ! please try again");
    }

    @Receiver(actions = TableclothService.ACTION_CONNECTION_SUCCEED)
    protected void onConnectionSucceed() {
        connected = true;
        showToast("connect success !");
    }


    @Receiver(actions = TableclothService.ACTION_TABLECLOTH_DATA)
    protected void onReceivedData(@Receiver.Extra(TableclothService.EXTRA_DATA) int[] pressures) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        this.appendLog(timeStamp + " " + Arrays.toString(pressures));
        pseudoColorTablecloth.reFresh(pressures);
    }

    private void appendLog(String text) {
        File logFile = new File("sdcard/tablecloth/" + this.uid + ".txt");
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

    private boolean connected = false;

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

    @Click(R.id.btn_add)
    protected void addFlag() {
        if (mTableclothService != null) {
            mTableclothService.add();
            tvFlag.setText(mTableclothService.getFlag() + "");
        }
    }

    @Click(R.id.btn_cut)
    protected void cutFlag() {
        if (mTableclothService != null) {
            mTableclothService.cut();
            tvFlag.setText(mTableclothService.getFlag() + "");
        }
    }

//    private class WriteDataRunnable implements Runnable {
//
//        @Override
//        public void run() {
//            int transfer = connection.bulkTransfer(endpointWrite, buffer, buffer.length,
//                    TIME_OUT);
//            if (transfer < 0) {
//                Log.d(TAG, "transfer is less than zore,is :" + transfer);
//            } else {
//                Log.d(TAG, "transfer is more than zore,is :" + transfer);
////                        sendMessageRefreshUI();
//            }
//        }
//    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

}
