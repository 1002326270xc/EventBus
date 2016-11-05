package co.library.eventbus;

import android.os.Looper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by chaobin on 1/29/15.
 */
public class Bus {

    static volatile Bus sInstance;

    Finder mFinder;
    //CopyOnWriteArrayList一般用在读和写同时存在的情况下使用
    Map<Class<?>, CopyOnWriteArrayList<Subscriber>> mSubscriberMap;

    PostHandler mPostHandler;

    private Bus() {
        mFinder = new NameBasedFinder();
        mSubscriberMap = new HashMap<>();
        mPostHandler = new PostHandler(Looper.getMainLooper(), this);
    }

    /**
     * 得到一个单例的上下文对象
     *
     * @return
     */
    public static Bus getDefault() {
        if (sInstance == null) {
            synchronized (Bus.class) {
                if (sInstance == null) {
                    sInstance = new Bus();
                }
            }
        }
        return sInstance;
    }

    public void register(Object subscriber) {
        /**
         * 找到该类下面的所有以onEvent开头的方法
         */
        List<Method> methods = mFinder.findSubscriber(subscriber.getClass());
        if (methods == null || methods.size() < 1) {
            return;
        }
        CopyOnWriteArrayList<Subscriber> subscribers = mSubscriberMap.get(subscriber.getClass());
        if (subscribers == null) {
            subscribers = new CopyOnWriteArrayList<>();
            /**
             * 该map用来存储onevent开头的方法的第一个参数为key和subscribers的集合
             */
            mSubscriberMap.put(methods.get(0).getParameterTypes()[0], subscribers);
        }
        for (Method method : methods) {
            /**
             * 封装了该类和该类带有onevent的方法
             */
            Subscriber newSubscriber = new Subscriber(subscriber, method);
            subscribers.add(newSubscriber);
        }
    }

    public void unregister(Object subscriber) {
        CopyOnWriteArrayList<Subscriber> subscribers = mSubscriberMap.remove(subscriber.getClass());
        if (subscribers != null) {
            for (Subscriber s : subscribers) {
                s.mMethod = null;
                s.mSubscriber = null;
            }
        }
    }

    public void post(Object event) {
        //TODO post with handler
        mPostHandler.enqueue(event);
    }
}
