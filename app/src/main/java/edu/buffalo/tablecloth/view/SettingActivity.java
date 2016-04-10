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
import android.widget.Button;
import android.widget.Toast;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.OptionsMenuItem;
import org.androidannotations.annotations.Receiver;
import org.androidannotations.annotations.ViewById;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.buffalo.tablecloth.R;
import edu.buffalo.tablecloth.listenner.OrderListenner;
import edu.buffalo.tablecloth.service.TableclothService;
import edu.buffalo.tablecloth.service.TableclothService_;
import edu.buffalo.tablecloth.widget.LightsLayout;
import edu.buffalo.tablecloth.widget.OrderLayout;

@EActivity(R.layout.activity_setting)
@OptionsMenu(R.menu.main_order)
public class SettingActivity extends Activity implements OrderListenner {
    private static final Logger LOGGER = LoggerFactory.getLogger(UsbActivity.class);

    private final ExecutorService mExecutorService = Executors.newCachedThreadPool();

    @OptionsMenuItem(R.id.mniAdd)
    protected MenuItem mniAdd;
    @OptionsMenuItem(R.id.mniRemove)
    protected MenuItem mniRemove;

    @ViewById(R.id.view_lights)
    protected LightsLayout mLightsLayout;
    @ViewById(R.id.view_order)
    protected OrderLayout mOrderLayout;

    @ViewById(R.id.btn_send)
    protected Button btnSend;
    @ViewById(R.id.btn_save)
    protected Button btnSave;

    private TableclothService mTableclothService;

    private ServiceConnection mTableclothServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LOGGER.debug("connected service {}", name.getClassName());
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

        bindService(TableclothService_.intent(this).get(), mTableclothServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mExecutorService.shutdown();
        unbindService(mTableclothServiceConnection);
    }

    @AfterViews
    protected void initViews() {
        mOrderLayout.setmOrderListenner(this);
    }

    @Receiver(actions = UsbManager.ACTION_USB_DEVICE_ATTACHED)
    protected void onDeviceAttached(@Receiver.Extra Intent intent) {
        final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        mTableclothService.connectUsbDevice(device);
    }

    @Receiver(actions = UsbManager.ACTION_USB_DEVICE_DETACHED)
    protected void onDeviceDetached() {
        //TODO 关闭连接
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

    @Receiver(actions = TableclothService.ACTION_USB_PERMISSION_FAILED)
    protected void onUsbPermissionFailed() {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                edu.buffalo.tablecloth.view.SettingActivity.this, 0, new Intent(TableclothService.ACTION_USB_PERMISSION_REQUEST), 0);
        mTableclothService.requestPermission(pendingIntent);
    }

    @Receiver(actions = TableclothService.ACTION_CONNECTION_SUCCEED)
    protected void onConnectionSucceed() {
        showToast("connect success !");
    }

    private int currentOrderNumber = -1;

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

    @Override
    public void showOrder(int[] status, int num) {
        currentOrderNumber = num;
        mLightsLayout.restoreLightsStatus();
        mLightsLayout.refreshLights(status);
    }

    @Click(R.id.btn_send)
    protected void sendOrders() {
        int[] status = mLightsLayout.getLightsStatus();
        Log.d("status", Arrays.toString(status));
        mExecutorService.submit(new SendOrderRunnerable(status));
    }

    @Click(R.id.btn_save)
    protected void saveOrder() {
        mOrderLayout.saveLightsStatus(mLightsLayout.getLightsStatus(), currentOrderNumber);
    }

    @OptionsItem(R.id.mniAdd)
    protected void addOrder() {
        mOrderLayout.addOrderView();
    }

    @OptionsItem(R.id.mniRemove)
    protected void removeOrder() {
        mOrderLayout.removeOrderView(currentOrderNumber);
        currentOrderNumber = -1;
    }


    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
