/**
 * Day11 - 工厂模式演示
 * 
 * 包含三种工厂模式：简单工厂、工厂方法、抽象工厂
 */
public class FactoryDemo {

    // ================================================================
    //  一、产品接口与实现（简单工厂 & 工厂方法共用）
    // ================================================================

    /** 产品接口 */
    interface Shape {
        void draw();
        String getName();
    }

    static class Circle implements Shape {
        @Override public void draw() { System.out.println("    ● 绘制圆形"); }
        @Override public String getName() { return "圆形"; }
    }

    static class Rectangle implements Shape {
        @Override public void draw() { System.out.println("    ■ 绘制矩形"); }
        @Override public String getName() { return "矩形"; }
    }

    static class Triangle implements Shape {
        @Override public void draw() { System.out.println("    ▲ 绘制三角形"); }
        @Override public String getName() { return "三角形"; }
    }

    // ================================================================
    //  二、简单工厂模式（Static Factory Method）
    // ================================================================

    /**
     * 简单工厂：一个工厂类，通过参数决定创建哪种产品
     * 优点：创建逻辑集中管理
     * 缺点：新增产品需修改工厂类（违反开闭原则）
     */
    static class SimpleShapeFactory {
        public static Shape createShape(String type) {
            switch (type.toLowerCase()) {
                case "circle":    return new Circle();
                case "rectangle": return new Rectangle();
                case "triangle":  return new Triangle();
                default:
                    throw new IllegalArgumentException("未知的形状类型: " + type);
            }
        }
    }

    // ================================================================
    //  三、工厂方法模式（Factory Method）
    // ================================================================

    /** 抽象工厂 */
    interface ShapeFactory {
        Shape createShape();
    }

    /** 具体工厂：圆形工厂 */
    static class CircleFactory implements ShapeFactory {
        @Override
        public Shape createShape() {
            return new Circle();
        }
    }

    /** 具体工厂：矩形工厂 */
    static class RectangleFactory implements ShapeFactory {
        @Override
        public Shape createShape() {
            return new Rectangle();
        }
    }

    /** 具体工厂：三角形工厂 */
    static class TriangleFactory implements ShapeFactory {
        @Override
        public Shape createShape() {
            return new Triangle();
        }
    }

    // ================================================================
    //  四、抽象工厂模式（Abstract Factory）
    //  场景：跨数据库的 DAO 层
    // ================================================================

    // --- 产品A：数据库连接 ---
    interface DBConnection {
        void connect();
    }

    static class MySQLConnection implements DBConnection {
        @Override
        public void connect() {
            System.out.println("    建立 MySQL 连接");
        }
    }

    static class OracleConnection implements DBConnection {
        @Override
        public void connect() {
            System.out.println("    建立 Oracle 连接");
        }
    }

    // --- 产品B：数据库命令 ---
    interface DBCommand {
        void execute(String sql);
    }

    static class MySQLCommand implements DBCommand {
        @Override
        public void execute(String sql) {
            System.out.println("    MySQL 执行: " + sql);
        }
    }

    static class OracleCommand implements DBCommand {
        @Override
        public void execute(String sql) {
            System.out.println("    Oracle 执行: " + sql);
        }
    }

    // --- 产品C：事务管理器 ---
    interface DBTransaction {
        void begin();
        void commit();
    }

    static class MySQLTransaction implements DBTransaction {
        @Override
        public void begin() { System.out.println("    MySQL 开启事务"); }
        @Override
        public void commit() { System.out.println("    MySQL 提交事务"); }
    }

    static class OracleTransaction implements DBTransaction {
        @Override
        public void begin() { System.out.println("    Oracle 开启事务"); }
        @Override
        public void commit() { System.out.println("    Oracle 提交事务"); }
    }

    // --- 抽象工厂 ---
    interface DatabaseFactory {
        DBConnection createConnection();
        DBCommand createCommand();
        DBTransaction createTransaction();
    }

    /** MySQL 工厂（生产 MySQL 产品族） */
    static class MySQLFactory implements DatabaseFactory {
        @Override
        public DBConnection createConnection() { return new MySQLConnection(); }
        @Override
        public DBCommand createCommand() { return new MySQLCommand(); }
        @Override
        public DBTransaction createTransaction() { return new MySQLTransaction(); }
    }

    /** Oracle 工厂（生产 Oracle 产品族） */
    static class OracleFactory implements DatabaseFactory {
        @Override
        public DBConnection createConnection() { return new OracleConnection(); }
        @Override
        public DBCommand createCommand() { return new OracleCommand(); }
        @Override
        public DBTransaction createTransaction() { return new OracleTransaction(); }
    }

    /** 使用抽象工厂的客户端（与具体数据库完全解耦） */
    static class DatabaseClient {
        private DBConnection connection;
        private DBCommand command;
        private DBTransaction transaction;

        public DatabaseClient(DatabaseFactory factory) {
            this.connection = factory.createConnection();
            this.command = factory.createCommand();
            this.transaction = factory.createTransaction();
        }

        public void executeBusinessLogic() {
            connection.connect();
            transaction.begin();
            command.execute("SELECT * FROM users");
            command.execute("UPDATE users SET status = 1 WHERE id = 1");
            transaction.commit();
        }
    }

    // ================================================================
    //  主方法
    // ================================================================

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  Day11 - 工厂模式演示");
        System.out.println("========================================\n");

        // 一、简单工厂
        testSimpleFactory();

        // 二、工厂方法
        testFactoryMethod();

        // 三、抽象工厂
        testAbstractFactory();

        // 四、对比总结
        printComparison();
    }

    static void testSimpleFactory() {
        System.out.println("=== 一、简单工厂模式 ===\n");
        System.out.println("  特点：一个工厂类，通过参数决定创建哪种产品");
        System.out.println("  优点：创建逻辑集中；缺点：违反开闭原则\n");

        String[] types = {"circle", "rectangle", "triangle"};
        for (String type : types) {
            Shape shape = SimpleShapeFactory.createShape(type);
            System.out.println("  创建了 " + shape.getName() + "：");
            shape.draw();
        }

        // 演示异常情况
        try {
            SimpleShapeFactory.createShape("hexagon");
        } catch (IllegalArgumentException e) {
            System.out.println("  尝试创建未知类型: " + e.getMessage());
        }

        System.out.println();
    }

    static void testFactoryMethod() {
        System.out.println("=== 二、工厂方法模式 ===\n");
        System.out.println("  特点：每种产品对应一个工厂，通过子类决定创建哪种产品");
        System.out.println("  优点：遵循开闭原则（新增产品只需新增工厂）\n");

        ShapeFactory[] factories = {
            new CircleFactory(),
            new RectangleFactory(),
            new TriangleFactory()
        };

        for (ShapeFactory factory : factories) {
            Shape shape = factory.createShape();
            System.out.println("  " + factory.getClass().getSimpleName() + " 创建了 " + shape.getName() + "：");
            shape.draw();
        }

        System.out.println("  ✅ 新增产品（如五边形）只需：");
        System.out.println("     1. 新增 Pentagon implements Shape");
        System.out.println("     2. 新增 PentagonFactory implements ShapeFactory");
        System.out.println("     无需修改任何已有代码！\n");
    }

    static void testAbstractFactory() {
        System.out.println("=== 三、抽象工厂模式 ===\n");
        System.out.println("  特点：创建一整个产品族（连接+命令+事务必须配套）");
        System.out.println("  优点：保证产品族一致性，切换产品族方便\n");

        // 使用 MySQL 产品族
        System.out.println("  --- 使用 MySQL 产品族 ---");
        DatabaseClient mysqlClient = new DatabaseClient(new MySQLFactory());
        mysqlClient.executeBusinessLogic();

        System.out.println();

        // 切换到 Oracle 产品族（只需更换工厂实例）
        System.out.println("  --- 切换到 Oracle 产品族（只改一行代码）---");
        DatabaseClient oracleClient = new DatabaseClient(new OracleFactory());
        oracleClient.executeBusinessLogic();

        System.out.println();
        System.out.println("  ✅ 切换数据库只需要将 new MySQLFactory() 改为 new OracleFactory()");
        System.out.println("     保证了连接、命令、事务都是同一数据库的，不会混搭\n");
    }

    static void printComparison() {
        System.out.println("========================================");
        System.out.println("  三种工厂模式对比");
        System.out.println("========================================");
        System.out.println("  | 维度     | 简单工厂     | 工厂方法       | 抽象工厂         |");
        System.out.println("  |---------|-------------|--------------|----------------|");
        System.out.println("  | 复杂度   | 低          | 中            | 高              |");
        System.out.println("  | 产品数量 | 一种         | 一种           | 多种(产品族)     |");
        System.out.println("  | 新增产品 | 修改工厂(违OCP)| 增加工厂类(遵OCP)| 修改抽象工厂(违OCP)|");
        System.out.println("  | 新增产品族| -           | -             | 增加工厂类(遵OCP) |");
        System.out.println("  | 适用场景 | 产品少且稳定  | 产品多经常扩展   | 需创建配套产品    |");
    }
}
