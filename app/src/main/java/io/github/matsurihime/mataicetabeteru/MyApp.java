package io.github.matsurihime.mataicetabeteru;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

public class MyApp extends Application {

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        MultiDex.install(this);
    }
    @Override
    public void onCreate() {
        super.onCreate();
    }
}