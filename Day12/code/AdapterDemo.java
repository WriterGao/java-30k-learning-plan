/**
 * 适配器模式演示
 * 演示类适配器和对象适配器两种实现方式
 */
public class AdapterDemo {
    
    // ========== 目标接口 ==========
    interface Target {
        void request();
    }
    
    // ========== 适配者（现有类，接口不兼容） ==========
    static class Adaptee {
        public void specificRequest() {
            System.out.println("适配者的方法被调用");
        }
    }
    
    // ========== 类适配器（使用继承） ==========
    static class ClassAdapter extends Adaptee implements Target {
        @Override
        public void request() {
            // 调用父类的方法
            specificRequest();
        }
    }
    
    // ========== 对象适配器（使用组合）⭐推荐 ==========
    static class ObjectAdapter implements Target {
        private Adaptee adaptee;
        
        public ObjectAdapter(Adaptee adaptee) {
            this.adaptee = adaptee;
        }
        
        @Override
        public void request() {
            adaptee.specificRequest();
        }
    }
    
    // ========== 客户端 ==========
    public static void main(String[] args) {
        System.out.println("========== 适配器模式演示 ==========\n");
        
        // 使用类适配器
        System.out.println("1. 类适配器方式：");
        Target classAdapter = new ClassAdapter();
        classAdapter.request();
        
        System.out.println("\n2. 对象适配器方式：");
        Adaptee adaptee = new Adaptee();
        Target objectAdapter = new ObjectAdapter(adaptee);
        objectAdapter.request();
        
        System.out.println("\n========== 演示完成 ==========");
    }
}
