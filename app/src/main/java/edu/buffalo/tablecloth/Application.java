package edu.buffalo.tablecloth;

import android.content.Context;
import android.os.Environment;
import android.view.Display;
import android.view.WindowManager;

import org.androidannotations.annotations.EApplication;


@EApplication
public class Application extends android.app.Application {

    private static Application instance;

    public static int width;
    public static int height;


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        getWindowSize();
    }

    private void getWindowSize() {
        WindowManager manager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        width = display.getWidth();
        height = display.getHeight();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public static Application instance() {
        return instance;
    }
}
