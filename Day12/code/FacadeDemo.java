/**
 * 外观模式演示：简化复杂子系统
 * 
 * 场景：家庭影院系统
 * 多个子系统组件（DVD、投影仪、音响、灯光）
 * 外观类提供简单接口，隐藏复杂性
 */
public class FacadeDemo {
    
    // ===== 子系统组件 =====
    static class DVDPlayer {
        public void on() { System.out.println("DVD 播放器打开"); }
        public void off() { System.out.println("DVD 播放器关闭"); }
        public void play(String movie) { System.out.println("播放电影: " + movie); }
        public void stop() { System.out.println("停止播放"); }
    }
    
    static class Projector {
        public void on() { System.out.println("投影仪打开"); }
        public void off() { System.out.println("投影仪关闭"); }
        public void wideScreenMode() { System.out.println("投影仪设置为宽屏模式"); }
    }
    
    static class SoundSystem {
        public void on() { System.out.println("音响系统打开"); }
        public void off() { System.out.println("音响系统关闭"); }
        public void setVolume(int level) { System.out.println("音量设置为: " + level); }
    }
    
    static class TheaterLights {
        public void on() { System.out.println("灯光打开"); }
        public void off() { System.out.println("灯光关闭"); }
        public void dim(int level) { System.out.println("灯光调暗到: " + level + "%"); }
    }
    
    // ===== 外观类（简化接口） =====
    static class HomeTheaterFacade {
        private DVDPlayer dvdPlayer;
        private Projector projector;
        private SoundSystem soundSystem;
        private TheaterLights lights;
        
        public HomeTheaterFacade(DVDPlayer dvd, Projector proj, 
                                 SoundSystem sound, TheaterLights lights) {
            this.dvdPlayer = dvd;
            this.projector = proj;
            this.soundSystem = sound;
            this.lights = lights;
        }
        
        // 一键看电影（简化了复杂的操作流程）
        public void watchMovie(String movie) {
            System.out.println("========== 准备看电影 ==========");
            lights.dim(10);              // 调暗灯光
            projector.on();              // 打开投影仪
            projector.wideScreenMode();  // 设置宽屏
            soundSystem.on();            // 打开音响
            soundSystem.setVolume(5);    // 设置音量
            dvdPlayer.on();              // 打开 DVD
            dvdPlayer.play(movie);       // 播放电影
            System.out.println("========== 开始观看 ==========");
        }
        
        // 一键结束
        public void endMovie() {
            System.out.println("========== 结束观看 ==========");
            dvdPlayer.stop();
            dvdPlayer.off();
            soundSystem.off();
            projector.off();
            lights.on();
            System.out.println("========== 已关闭所有设备 ==========");
        }
    }
    
    public static void main(String[] args) {
        System.out.println("========== 外观模式演示 ==========\n");
        
        // 创建子系统组件
        DVDPlayer dvd = new DVDPlayer();
        Projector projector = new Projector();
        SoundSystem sound = new SoundSystem();
        TheaterLights lights = new TheaterLights();
        
        // 创建外观
        HomeTheaterFacade facade = new HomeTheaterFacade(dvd, projector, sound, lights);
        
        // 客户端只需要调用简单的方法
        facade.watchMovie("复仇者联盟");
        System.out.println();
        facade.endMovie();
        
        System.out.println("\n========== 演示结束 ==========");
        System.out.println("\n【要点】");
        System.out.println("1. 外观类封装了多个子系统的复杂操作");
        System.out.println("2. 客户端通过简单接口使用复杂子系统");
        System.out.println("3. 隐藏了子系统的复杂性，降低了耦合度");
        System.out.println("4. 如果子系统改变，只需要修改外观类");
    }
}
