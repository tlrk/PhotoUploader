package com.magic.photouploader;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;


public class DemoApplication extends Application
{
    @Override
    public void onCreate() {
        super.onCreate();

        // the following line is important
        Fresco.initialize(getApplicationContext());
    }
}
