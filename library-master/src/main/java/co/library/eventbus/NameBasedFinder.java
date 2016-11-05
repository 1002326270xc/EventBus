package co.library.eventbus;

import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chaobin on 1/29/15.
 */
public class NameBasedFinder implements Finder {

    /**
     * public Method[] getMethods()返回某个类的所有公用（public）方法包括其继承类的公用方法，当然也包括它所实现接口的方法。
       public Method[] getDeclaredMethods()返回自身类的所有公用（public）方法包括私有(private)方法,但包括其继承类的方法，当然也包括它所实现接口的方法。
     * 找到某个类的所有的
     * @param subscriber
     * @return
     */
    @Override
    public List<Method> findSubscriber(Class<?> subscriber) {
        List<Method> methods = new ArrayList<>();
        for (Method method : subscriber.getDeclaredMethods()) {
            if (method.getName().startsWith("onEvent") && method.getParameterTypes().length == 1) {
                methods.add(method);
                Log.d("findSubscriber", "add method:" + method.getName());
            }
        }
        return methods;
    }
}
