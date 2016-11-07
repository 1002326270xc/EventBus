项目上线有一段时间了，自己总结了下项目。项目中的`EventBus`真的是到处都有它的影子，说实话，用上`EventBus`后，个人觉得项目中的业务变得很轻松，不用考虑哪个类发的，哪个类去回调。别人都说`EventBus`是一个"事务总线",就像一个公司的项目小组一样。项目经理给各个模块的分配好任务了，然后各人模块的人员处理自己的事情，最后完成整个事务。

**Bus类:**
<pre><code>
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
              * 该map用来存储onevent开头的方法的第一个参数为key和          
              * subscribers的集合          
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
</code></pre>

`Bus`类主要是消息的注册、发送、注销几个动作。
在注册中，首先找到要注册类中`onEvent`方法，这里是怎么做到的呢。大家找找成员变量`mFinder`是怎么生成的。其实它是一个接口：
<pre><code>
public interface Finder {   
    List<Method> findSubscriber(Class<?> subscriber);
}

</code></pre>
再来看看它的实现类吧:
![实现类.png](http://upload-images.jianshu.io/upload_images/2528336-7c38b45679e8fba9.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
<pre><code>
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
           if (method.getName().startsWith("onEvent") && 
              method.getParameterTypes().length == 1) {                
              methods.add(method);                
           }   
       }     
       return methods; 
    }
}
</code></pre>
看到了没，该实现类只是简单地将`onEvent`开头的方法给封装到一个集合里面去了。
接着看注册里面，获取到以`onEvent`开头的方法后，将该method集合添加到一个`mSubscriberMap`中，该map中的key就是`onEvent`开头的方法的第一个参数，value就是Subscriber的集合。
那就再来看看`Subscriber`类吧:
<pre><code>
public class Subscriber {    
   Object mSubscriber;    
   Method mMethod;   
   Class<?> mEventType; 
   public Subscriber(Object subscriber, Method method) {        
      mSubscriber = subscriber;    
      mMethod = method;   
     mEventType = method.getParameterTypes()[0];  
   }
}

</code></pre>

该类有三个成员变量，可能现在有人不知道为啥要这么定义，到了后面用到的时候就知道为啥这么定义了。
好了，整个注册的过程就这样，最终的目的就是得到一个map对象。
下面来看看发送的过程吧:
`mPostHandler.enqueue(event); `
就这一行代码，下面就看看`mPostHandler`类是怎么得来的吧。
` mPostHandler = new PostHandler(Looper.getMainLooper(), this); `
下面来看看PostHandler类吧:
<pre><code>
public class PostHandler extends Handler {  
  final Bus mBus;   
  public PostHandler(Looper looper, Bus bus) {        
    super(looper);    
    mBus = bus; 
  }  
  @Override  
  public void handleMessage(Message msg) {        
    CopyOnWriteArrayList<Subscriber> subscribers = mBus.mSubscriberMap.get(msg.obj.getClass()); 
       for (Subscriber subscriber : subscribers) {            
          subscriber.mMethod.setAccessible(true);      
          try {           
              /**           
               * 第二个参数是方法的参数，第-个参数是该类的对象                 
               */                
              subscriber.mMethod.invoke(subscriber.mSubscriber, msg.obj);     
          } catch (Exception e) {    
             e.printStackTrace();      
          }      
       }  
  }  

  void enqueue(Object event) {   
     Message message = obtainMessage();    
     message.obj = event;    
     sendMessage(message);   
  } 

}
</code></pre>

看到该类了没，还是逃不过handler通过handleMessage方法来接收处理消息了，这个地方就是核心的地方了。这里实际上bus类的post方法的参数就是这里的bus类中`mSubscriberMap`的key了，通过该key可以获取到`mSubscriberMap`中的value值了，也就是Subscriber集合了，现在知道Subscriber类的定义了吧，`mMethod`是方法名，`mSubscriber`是该类对象，实际上它的第三个成员变量没用到。最后通过反射来调用`onEvent`开头的方法，整个过程就这么简单，是不是现在一目了然了呢。
最后就是注销了：
<pre><code>
public void unregister(Object subscriber) {       
   CopyOnWriteArrayList<Subscriber> subscribers = mSubscriberMap.remove(subscriber.getClass());  
   if (subscribers != null) {        
        for (Subscriber s : subscribers) {       
            s.mMethod = null;             
            s.mSubscriber = null;       
        }    
    }   
} 
</code></pre>

这里没什么好说的吧，就是将map中的对象给至空了。
好了，整个过程就这样了，是不是几个过程都清楚了。

下面用一个demo来测试该事例吧:

![demo.gif](https://github.com/1002326270xc/EventBus/blob/master/photo/demo.gif)

整个的内容就这么多了，完全是靠自己的理解。如果有什么说得不清楚的地方，还需大家指出来。

**借鉴的项目:**https://github.com/avenwu/support

##关于我:
  - email:a1002326270@163.com
  
  - 简书:http://www.jianshu.com/users/7b186b7247c1/latest_articles
