package edu.buffalo.tablecloth.view;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import edu.buffalo.tablecloth.service.TableclothService_;

public class WayPointActivity extends ListActivity {
    private static final Item[] samplesConfig = new Item[]{

            new Item("SettingActivity", SettingActivity_.class),
            new Item("UsbActivity", UsbActivity_.class),
            new Item("TaskOne", TaskOneActivity_.class),
            new Item("TaskOneFourLED", TaskOneFourLEDActivity_.class),
            new Item("TaskTwo", TaskTwoActivity_.class),
            new Item("TaskTwoFourLED", TaskTwoFourLEDActivity_.class),
            new Item("TaskThree", TaskThreeActivity_.class),
            new Item("TaskThreeFourLED", TaskThreeFourLEDActivity_.class),
            new Item("TaskFourInOrder", TaskFourStaticActivty_.class),
            new Item("TaskFourInOrderFourLED", TaskFourStaticFourLEDActivity_.class),
            new Item("IMU",BluetoothCanActivity.class)
    };

    private ServiceConnection mTableclothServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, getTitlesList()));
        bindService(TableclothService_.intent(this).get(), mTableclothServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mTableclothServiceConnection);
    }

    private List<String> getTitlesList() {

        List<String> titles = new ArrayList<String>();
        for (Item config : samplesConfig) {
            titles.add(config.title);
        }
        return titles;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (position >= 0 && position < samplesConfig.length)
            startActivity(new Intent(this, samplesConfig[position].targetClass));
    }

    private static class Item {

        private final String title;
        private final Class targetClass;

        Item(String title, Class targetClass) {
            this.title = title;
            this.targetClass = targetClass;
        }
    }
}
