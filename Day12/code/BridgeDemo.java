/**
 * 桥接模式演示：将抽象与实现分离，避免类爆炸
 * 
 * 场景：图形系统支持多种形状和多种颜色
 * 使用继承：3种形状 × 3种颜色 = 9个类（类爆炸）
 * 使用桥接：3种形状 + 3种颜色 = 6个类（避免类爆炸）
 */
public class BridgeDemo {
    
    // ===== 实现部分（颜色） =====
    interface Color {
        void applyColor();
    }
    
    static class Red implements Color {
        @Override
        public void applyColor() {
            System.out.print("红色");
        }
    }
    
    static class Blue implements Color {
        @Override
        public void applyColor() {
            System.out.print("蓝色");
        }
    }
    
    static class Green implements Color {
        @Override
        public void applyColor() {
            System.out.print("绿色");
        }
    }
    
    // ===== 抽象部分（形状） =====
    static abstract class Shape {
        protected Color color; // 桥接：通过组合而非继承
        
        public Shape(Color color) {
            this.color = color;
        }
        
        public abstract void draw();
    }
    
    static class Circle extends Shape {
        public Circle(Color color) {
            super(color);
        }
        
        @Override
        public void draw() {
            System.out.print("画圆形，颜色：");
            color.applyColor();
            System.out.println();
        }
    }
    
    static class Rectangle extends Shape {
        public Rectangle(Color color) {
            super(color);
        }
        
        @Override
        public void draw() {
            System.out.print("画矩形，颜色：");
            color.applyColor();
            System.out.println();
        }
    }
    
    static class Triangle extends Shape {
        public Triangle(Color color) {
            super(color);
        }
        
        @Override
        public void draw() {
            System.out.print("画三角形，颜色：");
            color.applyColor();
            System.out.println();
        }
    }
    
    public static void main(String[] args) {
        System.out.println("========== 桥接模式演示 ==========\n");
        
        // 红色圆形
        Shape redCircle = new Circle(new Red());
        redCircle.draw();
        
        // 蓝色矩形
        Shape blueRectangle = new Rectangle(new Blue());
        blueRectangle.draw();
        
        // 绿色三角形
        Shape greenTriangle = new Triangle(new Green());
        greenTriangle.draw();
        
        // 蓝色圆形
        Shape blueCircle = new Circle(new Blue());
        blueCircle.draw();
        
        System.out.println("\n========== 演示结束 ==========");
        System.out.println("\n【要点】");
        System.out.println("1. 使用继承：3种形状 × 3种颜色 = 9个类（类爆炸）");
        System.out.println("2. 使用桥接：3种形状 + 3种颜色 = 6个类（避免类爆炸）");
        System.out.println("3. 新增形状：只需新增形状类，不需要为每种颜色创建子类");
        System.out.println("4. 新增颜色：只需新增颜色类，不需要为每种形状创建子类");
        System.out.println("5. 抽象（形状）和实现（颜色）可以独立变化");
    }
}
