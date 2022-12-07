package me.trive.trivemessage;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class CheckDbService extends Service {

    Intent intent;
    static final public String BROADCAST_ACTION = "me.trive.broadcast";
    private Timer timer;
    private Handler handler;

    private static final int TIMER_VALUE = 1 * 30 * 1000;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        intent = new Intent(BROADCAST_ACTION);

    }

    @Override
    public synchronized int onStartCommand(Intent intent, int flags, int startId) {

        handler = new Handler();
        timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @SuppressWarnings("unchecked")
                    public void run() {
                        try {
                            sendResult("");
                        } catch (Exception e) {
                            Log.e("Error", "Can't load via Fragment");
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, TIMER_VALUE);


        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }


    @Override
    public synchronized void onDestroy() {
        super.onDestroy();

        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
            handler.removeCallbacksAndMessages(null);
        }

    }

    public void sendResult(String message) {
        sendBroadcast(intent);
    }

} // outer class end