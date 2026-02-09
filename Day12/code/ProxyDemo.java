import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 代理模式演示
 * 演示静态代理、JDK动态代理、CGLIB动态代理
 * 
 * 注意：CGLIB 需要引入依赖，如果不想引入，可以注释掉 CGLIB 相关代码
 */
public class ProxyDemo {
    
    // ========== 抽象主题 ==========
    interface Subject {
        void request();
        void doSomething();
    }
    
    // ========== 真实主题 ==========
    static class RealSubject implements Subject {
        @Override
        public void request() {
            System.out.println("真实对象的请求");
        }
        
        @Override
        public void doSomething() {
            System.out.println("真实对象做某事");
        }
    }
    
    // ========== 静态代理 ==========
    static class StaticProxy implements Subject {
        private RealSubject realSubject;
        
        public StaticProxy(RealSubject realSubject) {
            this.realSubject = realSubject;
        }
        
        @Override
        public void request() {
            System.out.println("【静态代理】请求前处理");
            realSubject.request();
            System.out.println("【静态代理】请求后处理");
        }
        
        @Override
        public void doSomething() {
            System.out.println("【静态代理】请求前处理");
            realSubject.doSomething();
            System.out.println("【静态代理】请求后处理");
        }
    }
    
    // ========== JDK 动态代理处理器 ==========
    static class DynamicProxyHandler implements InvocationHandler {
        private Object target;
        
        public DynamicProxyHandler(Object target) {
            this.target = target;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("【JDK动态代理】调用方法前 - " + method.getName());
            Object result = method.invoke(target, args);
            System.out.println("【JDK动态代理】调用方法后 - " + method.getName());
            return result;
        }
    }
    
    // ========== 客户端 ==========
    public static void main(String[] args) {
        System.out.println("========== 代理模式演示 ==========\n");
        
        RealSubject realSubject = new RealSubject();
        
        // 1. 静态代理
        System.out.println("1. 静态代理：");
        Subject staticProxy = new StaticProxy(realSubject);
        staticProxy.request();
        staticProxy.doSomething();
        
        System.out.println("\n2. JDK 动态代理：");
        // 2. JDK 动态代理
        Subject jdkProxy = (Subject) Proxy.newProxyInstance(
            realSubject.getClass().getClassLoader(),  // 类加载器
            realSubject.getClass().getInterfaces(),   // 接口数组
            new DynamicProxyHandler(realSubject)       // 调用处理器
        );
        jdkProxy.request();
        jdkProxy.doSomething();
        
        System.out.println("\n3. CGLIB 动态代理：");
        System.out.println("（需要引入 CGLIB 依赖，此处仅作说明）");
        System.out.println("CGLIB 可以代理类，不需要实现接口");
        
        System.out.println("\n========== 演示完成 ==========");
        System.out.println("JDK 动态代理：只能代理接口");
        System.out.println("CGLIB 动态代理：可以代理类，但不能代理 final 类和方法");
    }
}
