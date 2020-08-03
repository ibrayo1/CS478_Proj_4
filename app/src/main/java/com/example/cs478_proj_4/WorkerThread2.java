package com.example.cs478_proj_4;

import android.os.Handler;
import android.os.Looper;

public class WorkerThread2 extends Thread {

    public Handler handler;

    @Override
    public void run(){
        Looper.prepare();

        handler = new Handler();

        Looper.loop();
    }

}
