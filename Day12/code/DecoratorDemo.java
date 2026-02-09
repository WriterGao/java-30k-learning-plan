/**
 * 装饰器模式演示
 * 演示咖啡加料系统：基础咖啡 + 糖装饰器 + 奶装饰器
 */
public class DecoratorDemo {
    
    // ========== 抽象组件 ==========
    interface Coffee {
        String getDescription();
        double getCost();
    }
    
    // ========== 具体组件 ==========
    static class SimpleCoffee implements Coffee {
        @Override
        public String getDescription() {
            return "简单咖啡";
        }
        
        @Override
        public double getCost() {
            return 5.0;
        }
    }
    
    // ========== 抽象装饰器 ==========
    static abstract class CoffeeDecorator implements Coffee {
        protected Coffee coffee;
        
        public CoffeeDecorator(Coffee coffee) {
            this.coffee = coffee;
        }
        
        @Override
        public String getDescription() {
            return coffee.getDescription();
        }
        
        @Override
        public double getCost() {
            return coffee.getCost();
        }
    }
    
    // ========== 具体装饰器：加糖 ==========
    static class SugarDecorator extends CoffeeDecorator {
        public SugarDecorator(Coffee coffee) {
            super(coffee);
        }
        
        @Override
        public String getDescription() {
            return coffee.getDescription() + " + 糖";
        }
        
        @Override
        public double getCost() {
            return coffee.getCost() + 1.0;
        }
    }
    
    // ========== 具体装饰器：加奶 ==========
    static class MilkDecorator extends CoffeeDecorator {
        public MilkDecorator(Coffee coffee) {
            super(coffee);
        }
        
        @Override
        public String getDescription() {
            return coffee.getDescription() + " + 奶";
        }
        
        @Override
        public double getCost() {
            return coffee.getCost() + 2.0;
        }
    }
    
    // ========== 客户端 ==========
    public static void main(String[] args) {
        System.out.println("========== 装饰器模式演示 ==========\n");
        
        // 简单咖啡
        Coffee coffee = new SimpleCoffee();
        System.out.println(coffee.getDescription() + " 价格: " + coffee.getCost());
        
        // 加糖
        coffee = new SugarDecorator(coffee);
        System.out.println(coffee.getDescription() + " 价格: " + coffee.getCost());
        
        // 加糖 + 加奶
        coffee = new MilkDecorator(coffee);
        System.out.println(coffee.getDescription() + " 价格: " + coffee.getCost());
        
        // 重新开始：简单咖啡 + 加奶 + 加糖（顺序不同）
        System.out.println("\n--- 不同的组合顺序 ---");
        Coffee coffee2 = new SimpleCoffee();
        coffee2 = new MilkDecorator(coffee2);
        coffee2 = new SugarDecorator(coffee2);
        System.out.println(coffee2.getDescription() + " 价格: " + coffee2.getCost());
        
        System.out.println("\n========== 演示完成 ==========");
        System.out.println("装饰器模式的优势：可以动态组合功能，不需要创建新类！");
    }
}
