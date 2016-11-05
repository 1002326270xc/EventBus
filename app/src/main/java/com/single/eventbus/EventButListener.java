package com.single.eventbus;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import co.library.eventbus.Bus;

/**
 * Created by xiangcheng on 16/9/18.
 */
public class EventButListener {
    private static final String TAG = EventButListener.class.getSimpleName();
    private Context context;

    public EventButListener(Context context) {
        Bus.getDefault().register(this);
        this.context = context;
    }

    public void onEvent(Event event) {
        Log.d(TAG, "title:" + event);
        new AlertDialog.Builder(context).setTitle(event.title).setPositiveButton("确定", null).setNegativeButton("取消", null).show();
    }

    public void unRegist() {
        Bus.getDefault().unregister(this);
    }

}
