import java.util.HashMap;
import java.util.Map;

/**
 * 享元模式演示：共享细粒度对象
 * 
 * 场景：围棋棋子系统
 * 围棋有 361 个位置，如果每个位置都创建一个棋子对象，会创建大量对象
 * 但棋子只有两种：黑棋和白棋，可以共享
 */
public class FlyweightDemo {
    
    // ===== 享元接口 =====
    interface ChessPiece {
        void place(int x, int y); // x, y 是外部状态（位置）
    }
    
    // ===== 具体享元（共享内部状态：颜色） =====
    static class BlackChessPiece implements ChessPiece {
        private final String color = "黑色"; // 内部状态（共享）
        
        @Override
        public void place(int x, int y) {
            System.out.println(color + "棋子放在 (" + x + ", " + y + ")");
        }
    }
    
    static class WhiteChessPiece implements ChessPiece {
        private final String color = "白色"; // 内部状态（共享）
        
        @Override
        public void place(int x, int y) {
            System.out.println(color + "棋子放在 (" + x + ", " + y + ")");
        }
    }
    
    // ===== 享元工厂 =====
    static class ChessPieceFactory {
        private static final Map<String, ChessPiece> pieces = new HashMap<>();
        
        public static ChessPiece getChessPiece(String color) {
            ChessPiece piece = pieces.get(color);
            if (piece == null) {
                if ("黑色".equals(color)) {
                    piece = new BlackChessPiece();
                } else if ("白色".equals(color)) {
                    piece = new WhiteChessPiece();
                }
                pieces.put(color, piece);
                System.out.println("创建 " + color + " 棋子（享元对象）");
            }
            return piece;
        }
        
        public static int getPieceCount() {
            return pieces.size();
        }
    }
    
    public static void main(String[] args) {
        System.out.println("========== 享元模式演示 ==========\n");
        
        // 下 10 个黑棋
        System.out.println("下 10 个黑棋：");
        for (int i = 0; i < 10; i++) {
            ChessPiece black = ChessPieceFactory.getChessPiece("黑色");
            black.place(i, i);
        }
        System.out.println();
        
        // 下 10 个白棋
        System.out.println("下 10 个白棋：");
        for (int i = 0; i < 10; i++) {
            ChessPiece white = ChessPieceFactory.getChessPiece("白色");
            white.place(i, 10 - i);
        }
        System.out.println();
        
        System.out.println("总共创建的棋子对象数: " + ChessPieceFactory.getPieceCount());
        System.out.println("（而不是 20 个对象）");
        
        System.out.println("\n========== 演示结束 ==========");
        System.out.println("\n【要点】");
        System.out.println("1. 内部状态（颜色）可以共享，存储在享元对象内部");
        System.out.println("2. 外部状态（位置）不能共享，由客户端传入");
        System.out.println("3. 享元工厂缓存和复用对象，减少对象数量");
        System.out.println("4. 大量相似对象时，可以显著节省内存");
        System.out.println("5. String 常量池、Integer 缓存都是享元模式的应用");
    }
}
