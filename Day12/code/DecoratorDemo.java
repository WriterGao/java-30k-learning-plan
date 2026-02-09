/**
 * 装饰器模式演示：动态扩展功能
 * 
 * 场景：咖啡加料系统
 * 使用继承：3种调料 × 2^3 = 8个类（类爆炸）
 * 使用装饰器：1个基础类 + 3个装饰器 = 4个类（避免类爆炸）
 */
public class DecoratorDemo {
    
    // ===== 抽象组件 =====
    interface Coffee {
        String getDescription();
        double getCost();
    }
    
    // ===== 具体组件 =====
    static class SimpleCoffee implements Coffee {
        @Override
        public String getDescription() {
            return "普通咖啡";
        }
        
        @Override
        public double getCost() {
            return 5.0;
        }
    }
    
    // ===== 抽象装饰器 =====
    static abstract class CoffeeDecorator implements Coffee {
        protected Coffee coffee; // 被装饰的对象
        
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
    
    // ===== 具体装饰器 =====
    static class MilkDecorator extends CoffeeDecorator {
        public MilkDecorator(Coffee coffee) {
            super(coffee);
        }
        
        @Override
        public String getDescription() {
            return coffee.getDescription() + " + 牛奶";
        }
        
        @Override
        public double getCost() {
            return coffee.getCost() + 2.0;
        }
    }
    
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
    
    static class WhipDecorator extends CoffeeDecorator {
        public WhipDecorator(Coffee coffee) {
            super(coffee);
        }
        
        @Override
        public String getDescription() {
            return coffee.getDescription() + " + 奶油";
        }
        
        @Override
        public double getCost() {
            return coffee.getCost() + 3.0;
        }
    }
    
    public static void main(String[] args) {
        System.out.println("========== 装饰器模式演示 ==========\n");
        
        // 普通咖啡
        Coffee coffee = new SimpleCoffee();
        System.out.println(coffee.getDescription() + " = ¥" + coffee.getCost());
        
        // 加牛奶
        coffee = new MilkDecorator(coffee);
        System.out.println(coffee.getDescription() + " = ¥" + coffee.getCost());
        
        // 再加糖
        coffee = new SugarDecorator(coffee);
        System.out.println(coffee.getDescription() + " = ¥" + coffee.getCost());
        
        // 再加奶油
        coffee = new WhipDecorator(coffee);
        System.out.println(coffee.getDescription() + " = ¥" + coffee.getCost());
        
        System.out.println("\n========== 演示结束 ==========");
        System.out.println("\n【要点】");
        System.out.println("1. 使用继承：3种调料 × 2^3 = 8个类（类爆炸）");
        System.out.println("2. 使用装饰器：1个基础类 + 3个装饰器 = 4个类（避免类爆炸）");
        System.out.println("3. 可以动态组合功能，不需要为每种组合创建类");
        System.out.println("4. 装饰器和被装饰者实现同一接口");
        System.out.println("5. 通过链式调用实现功能叠加");
    }
}
