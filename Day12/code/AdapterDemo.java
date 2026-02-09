/**
 * 适配器模式演示：对象适配器
 * 
 * 场景：音乐播放系统需要支持多种播放器，但它们的接口不同
 * 使用适配器模式将第三方播放器适配到统一的接口
 */
public class AdapterDemo {
    
    // ===== 目标接口（我们系统需要的接口） =====
    interface MediaPlayer {
        void play(String audioType, String fileName);
    }
    
    // ===== 已有的第三方播放器（不兼容的接口） =====
    static class VlcPlayer {
        public void playVlc(String fileName) {
            System.out.println("VLC 播放器播放: " + fileName);
        }
    }
    
    static class Mp4Player {
        public void playMp4(String fileName) {
            System.out.println("MP4 播放器播放: " + fileName);
        }
    }
    
    // ===== 适配器：将第三方播放器适配到我们的接口 =====
    static class MediaAdapter implements MediaPlayer {
        private VlcPlayer vlcPlayer;
        private Mp4Player mp4Player;
        
        public MediaAdapter(String audioType) {
            if ("vlc".equalsIgnoreCase(audioType)) {
                vlcPlayer = new VlcPlayer();
            } else if ("mp4".equalsIgnoreCase(audioType)) {
                mp4Player = new Mp4Player();
            }
        }
        
        @Override
        public void play(String audioType, String fileName) {
            if ("vlc".equalsIgnoreCase(audioType)) {
                vlcPlayer.playVlc(fileName);
            } else if ("mp4".equalsIgnoreCase(audioType)) {
                mp4Player.playMp4(fileName);
            }
        }
    }
    
    // ===== 客户端使用 =====
    static class AudioPlayer implements MediaPlayer {
        private MediaAdapter adapter;
        
        @Override
        public void play(String audioType, String fileName) {
            if ("mp3".equalsIgnoreCase(audioType)) {
                System.out.println("内置 MP3 播放器播放: " + fileName);
            } else if ("vlc".equalsIgnoreCase(audioType) || "mp4".equalsIgnoreCase(audioType)) {
                adapter = new MediaAdapter(audioType);
                adapter.play(audioType, fileName);
            } else {
                System.out.println("不支持的格式: " + audioType);
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("========== 适配器模式演示 ==========\n");
        
        AudioPlayer player = new AudioPlayer();
        
        // 播放 MP3（内置支持）
        player.play("mp3", "song.mp3");
        System.out.println();
        
        // 播放 VLC（通过适配器）
        player.play("vlc", "movie.vlc");
        System.out.println();
        
        // 播放 MP4（通过适配器）
        player.play("mp4", "video.mp4");
        System.out.println();
        
        // 不支持的格式
        player.play("avi", "movie.avi");
        
        System.out.println("\n========== 演示结束 ==========");
        System.out.println("\n【要点】");
        System.out.println("1. 适配器通过组合（而非继承）实现接口转换");
        System.out.println("2. 客户端使用统一的接口，不需要知道具体实现");
        System.out.println("3. 新增播放器类型时，只需新增适配器，不需要修改客户端代码");
    }
}
