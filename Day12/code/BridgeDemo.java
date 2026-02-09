/**
 * 桥接模式演示
 * 演示不同形状和颜色的图形：将抽象（形状）与实现（颜色）分离
 */
public class BridgeDemo {
    
    // ========== 实现类接口：颜色 ==========
    interface Color {
        void applyColor();
    }
    
    // ========== 具体实现类：红色 ==========
    static class RedColor implements Color {
        @Override
        public void applyColor() {
            System.out.print("红色");
        }
    }
    
    // ========== 具体实现类：蓝色 ==========
    static class BlueColor implements Color {
        @Override
        public void applyColor() {
            System.out.print("蓝色");
        }
    }
    
    // ========== 具体实现类：绿色 ==========
    static class GreenColor implements Color {
        @Override
        public void applyColor() {
            System.out.print("绿色");
        }
    }
    
    // ========== 抽象类：形状 ==========
    static abstract class Shape {
        protected Color color;
        
        public Shape(Color color) {
            this.color = color;
        }
        
        abstract void draw();
    }
    
    // ========== 扩充抽象类：圆形 ==========
    static class Circle extends Shape {
        public Circle(Color color) {
            super(color);
        }
        
        @Override
        void draw() {
            color.applyColor();
            System.out.println("圆形");
        }
    }
    
    // ========== 扩充抽象类：矩形 ==========
    static class Rectangle extends Shape {
        public Rectangle(Color color) {
            super(color);
        }
        
        @Override
        void draw() {
            color.applyColor();
            System.out.println("矩形");
        }
    }
    
    // ========== 扩充抽象类：三角形 ==========
    static class Triangle extends Shape {
        public Triangle(Color color) {
            super(color);
        }
        
        @Override
        void draw() {
            color.applyColor();
            System.out.println("三角形");
        }
    }
    
    // ========== 客户端 ==========
    public static void main(String[] args) {
        System.out.println("========== 桥接模式演示 ==========\n");
        
        // 创建颜色
        Color red = new RedColor();
        Color blue = new BlueColor();
        Color green = new GreenColor();
        
        // 创建形状（可以任意组合）
        System.out.println("形状和颜色的任意组合：\n");
        
        // 红色圆形
        Shape redCircle = new Circle(red);
        redCircle.draw();
        
        // 蓝色矩形
        Shape blueRectangle = new Rectangle(blue);
        blueRectangle.draw();
        
        // 绿色三角形
        Shape greenTriangle = new Triangle(green);
        greenTriangle.draw();
        
        // 蓝色圆形
        Shape blueCircle = new Circle(blue);
        blueCircle.draw();
        
        System.out.println("\n========== 演示完成 ==========");
        System.out.println("桥接模式的优势：");
        System.out.println("- 只需要 3 个形状类 + 3 个颜色类 = 6 个类");
        System.out.println("- 可以组合出 3 × 3 = 9 种不同的图形");
        System.out.println("- 如果使用继承，需要创建 9 个类（类爆炸）！");
    }
}
