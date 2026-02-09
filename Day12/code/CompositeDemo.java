import java.util.ArrayList;
import java.util.List;

/**
 * 组合模式演示：树形结构处理
 * 
 * 场景：文件系统（文件 + 文件夹）
 * 统一处理单个对象（文件）和组合对象（文件夹）
 */
public class CompositeDemo {
    
    // ===== 抽象组件 =====
    static abstract class FileSystemNode {
        protected String name;
        
        public FileSystemNode(String name) {
            this.name = name;
        }
        
        public abstract void display(int depth);
        
        // 组合节点的方法（叶子节点可以抛出异常或空实现）
        public void add(FileSystemNode node) {
            throw new UnsupportedOperationException("叶子节点不支持添加");
        }
        
        public void remove(FileSystemNode node) {
            throw new UnsupportedOperationException("叶子节点不支持删除");
        }
    }
    
    // ===== 叶子节点（文件） =====
    static class File extends FileSystemNode {
        private long size;
        
        public File(String name, long size) {
            super(name);
            this.size = size;
        }
        
        @Override
        public void display(int depth) {
            String indent = "  ".repeat(depth);
            System.out.println(indent + "📄 " + name + " (" + size + " bytes)");
        }
    }
    
    // ===== 组合节点（文件夹） =====
    static class Directory extends FileSystemNode {
        private List<FileSystemNode> children = new ArrayList<>();
        
        public Directory(String name) {
            super(name);
        }
        
        @Override
        public void display(int depth) {
            String indent = "  ".repeat(depth);
            System.out.println(indent + "📁 " + name);
            // 递归显示子节点
            for (FileSystemNode child : children) {
                child.display(depth + 1);
            }
        }
        
        @Override
        public void add(FileSystemNode node) {
            children.add(node);
        }
        
        @Override
        public void remove(FileSystemNode node) {
            children.remove(node);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("========== 组合模式演示 ==========\n");
        
        // 构建文件树
        Directory root = new Directory("根目录");
        
        Directory dir1 = new Directory("文档");
        dir1.add(new File("readme.txt", 1024));
        dir1.add(new File("notes.txt", 2048));
        
        Directory dir2 = new Directory("图片");
        dir2.add(new File("photo1.jpg", 512000));
        dir2.add(new File("photo2.jpg", 768000));
        
        Directory subDir = new Directory("子文件夹");
        subDir.add(new File("subfile.txt", 512));
        dir1.add(subDir);
        
        root.add(dir1);
        root.add(dir2);
        root.add(new File("config.ini", 512));
        
        // 统一方式显示（客户端不需要区分文件和文件夹）
        root.display(0);
        
        System.out.println("\n========== 演示结束 ==========");
        System.out.println("\n【要点】");
        System.out.println("1. 组合模式形成树形结构（部分-整体层次）");
        System.out.println("2. 统一处理叶子节点（文件）和组合节点（文件夹）");
        System.out.println("3. 客户端不需要区分是文件还是文件夹，统一调用 display()");
        System.out.println("4. 通过递归实现树形结构的显示");
    }
}
