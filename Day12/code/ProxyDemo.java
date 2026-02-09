import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 代理模式演示：静态代理、JDK 动态代理
 * 
 * 场景：为 UserService 添加日志和性能监控功能
 * 
 * 注意：CGLIB 示例需要引入 CGLIB 依赖，这里只演示静态代理和 JDK 动态代理
 */
public class ProxyDemo {
    
    // ===== 接口 =====
    interface UserService {
        void saveUser(String user);
        void deleteUser(Long id);
    }
    
    // ===== 真实对象 =====
    static class UserServiceImpl implements UserService {
        @Override
        public void saveUser(String user) {
            System.out.println("保存用户: " + user);
            // 模拟耗时操作
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public void deleteUser(Long id) {
            System.out.println("删除用户: " + id);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    // ===== 静态代理 =====
    static class UserServiceStaticProxy implements UserService {
        private UserService target; // 被代理的对象
        
        public UserServiceStaticProxy(UserService target) {
            this.target = target;
        }
        
        @Override
        public void saveUser(String user) {
            System.out.println("【静态代理】开始保存用户...");
            long start = System.currentTimeMillis();
            
            target.saveUser(user); // 调用真实对象的方法
            
            long end = System.currentTimeMillis();
            System.out.println("【静态代理】保存用户完成，耗时: " + (end - start) + "ms");
        }
        
        @Override
        public void deleteUser(Long id) {
            System.out.println("【静态代理】开始删除用户...");
            long start = System.currentTimeMillis();
            
            target.deleteUser(id); // 调用真实对象的方法
            
            long end = System.currentTimeMillis();
            System.out.println("【静态代理】删除用户完成，耗时: " + (end - start) + "ms");
        }
    }
    
    // ===== JDK 动态代理处理器 =====
    static class LogInvocationHandler implements InvocationHandler {
        private Object target; // 被代理的对象
        
        public LogInvocationHandler(Object target) {
            this.target = target;
        }
        
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            System.out.println("【JDK动态代理】调用方法: " + method.getName());
            long start = System.currentTimeMillis();
            
            // 调用真实对象的方法
            Object result = method.invoke(target, args);
            
            long end = System.currentTimeMillis();
            System.out.println("【JDK动态代理】方法执行完成，耗时: " + (end - start) + "ms");
            return result;
        }
    }
    
    public static void main(String[] args) {
        System.out.println("========== 代理模式演示 ==========\n");
        
        UserService realService = new UserServiceImpl();
        
        // ===== 1. 静态代理 =====
        System.out.println("1. 静态代理：");
        UserService staticProxy = new UserServiceStaticProxy(realService);
        staticProxy.saveUser("张三");
        System.out.println();
        
        // ===== 2. JDK 动态代理 =====
        System.out.println("2. JDK 动态代理：");
        UserService dynamicProxy = (UserService) Proxy.newProxyInstance(
            realService.getClass().getClassLoader(),  // 类加载器
            realService.getClass().getInterfaces(),    // 接口数组
            new LogInvocationHandler(realService)      // 处理器
        );
        dynamicProxy.saveUser("李四");
        dynamicProxy.deleteUser(1L);
        
        System.out.println("\n========== 演示结束 ==========");
        System.out.println("\n【要点】");
        System.out.println("1. 静态代理：需要为每个被代理类创建代理类，类数量翻倍");
        System.out.println("2. JDK 动态代理：运行时生成代理类，只能代理实现了接口的类");
        System.out.println("3. CGLIB：通过继承创建代理，可以代理没有接口的类（需要引入依赖）");
        System.out.println("4. Spring AOP：有接口用 JDK 动态代理，无接口用 CGLIB");
        System.out.println("5. 代理模式的核心：控制对对象的访问，添加横切关注点（日志、事务等）");
    }
}
