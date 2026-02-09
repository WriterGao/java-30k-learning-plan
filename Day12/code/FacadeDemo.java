/**
 * 外观模式演示
 * 演示家庭影院系统：一键看电影，简化复杂子系统的使用
 */
public class FacadeDemo {
    
    // ========== 子系统1：DVD 播放器 ==========
    static class DVDPlayer {
        public void on() {
            System.out.println("DVD 播放器打开");
        }
        
        public void play(String movie) {
            System.out.println("DVD 播放器播放: " + movie);
        }
        
        public void off() {
            System.out.println("DVD 播放器关闭");
        }
    }
    
    // ========== 子系统2：投影仪 ==========
    static class Projector {
        public void on() {
            System.out.println("投影仪打开");
        }
        
        public void off() {
            System.out.println("投影仪关闭");
        }
    }
    
    // ========== 子系统3：音响 ==========
    static class SoundSystem {
        public void on() {
            System.out.println("音响打开");
        }
        
        public void setVolume(int volume) {
            System.out.println("音响音量设置为: " + volume);
        }
        
        public void off() {
            System.out.println("音响关闭");
        }
    }
    
    // ========== 外观：家庭影院 ==========
    static class HomeTheaterFacade {
        private DVDPlayer dvdPlayer;
        private Projector projector;
        private SoundSystem soundSystem;
        
        public HomeTheaterFacade(DVDPlayer dvd, Projector proj, SoundSystem sound) {
            this.dvdPlayer = dvd;
            this.projector = proj;
            this.soundSystem = sound;
        }
        
        // 一键看电影
        public void watchMovie(String movie) {
            System.out.println("========== 准备看电影 ==========");
            projector.on();
            dvdPlayer.on();
            soundSystem.on();
            soundSystem.setVolume(10);
            dvdPlayer.play(movie);
            System.out.println("========== 开始观看 ==========\n");
        }
        
        // 一键关闭
        public void endMovie() {
            System.out.println("========== 关闭家庭影院 ==========");
            dvdPlayer.off();
            projector.off();
            soundSystem.off();
            System.out.println("========== 已关闭 ==========\n");
        }
    }
    
    // ========== 客户端 ==========
    public static void main(String[] args) {
        System.out.println("========== 外观模式演示 ==========\n");
        
        // 创建子系统
        DVDPlayer dvd = new DVDPlayer();
        Projector projector = new Projector();
        SoundSystem sound = new SoundSystem();
        
        // 创建外观
        HomeTheaterFacade facade = new HomeTheaterFacade(dvd, projector, sound);
        
        // 使用外观：一键看电影
        facade.watchMovie("复仇者联盟");
        
        // 使用外观：一键关闭
        facade.endMovie();
        
        System.out.println("========== 演示完成 ==========");
        System.out.println("外观模式的优势：简化复杂子系统的使用，客户端只需要调用外观的方法！");
    }
}
