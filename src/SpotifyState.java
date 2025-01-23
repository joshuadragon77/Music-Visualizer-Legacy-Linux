import java.io.IOException;
import java.lang.ProcessBuilder;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import network.Responder;

class SpotifyState {
    final private static Pattern localSpotifyTrackPattern = Pattern.compile("^spotify:local");

    static double globalTimePosition = 0;

    String trackName = "";
    String artist = "";
    double timePosition = 0;
    double timeLength = 0;
    boolean playing = false;
    String artworkURL = "";
    String id = "";
    String albumName = "";
    long updatedTime = 0;
    boolean localTrack = false;

    static Responder remoteConnection;
    static String currentRemoteStatus = "";

    static String getRawSpotifyStatus() throws IOException, InterruptedException{
        ProcessBuilder pb = new ProcessBuilder(
            "dbus-send", 
            "--print-reply",
            "--type=method_call",
            "--session",
            "--dest=org.mpris.MediaPlayer2.spotify",
            "/org/mpris/MediaPlayer2",
            "org.freedesktop.DBus.Properties.GetAll",
            "string:org.mpris.MediaPlayer2.Player"
        );
        Process process = pb.start();
        process.getOutputStream().write(1);
        process.waitFor();
        String outputResults = new String(process.getInputStream().readAllBytes());
        return outputResults;
    }

    final private static Pattern trackNamePattern = Pattern.compile("xesam:title\"\\n.+string \"(.+)\"(?:\\n.+)");
    final private static Pattern artistNamePattern = Pattern.compile("xesam:artist\"\\n.+\\n.+string \"(.+)\"(?:\\n.+)");
    final private static Pattern timePositionPattern = Pattern.compile("Position\"\\n.+int64 (\\d+)(?:\\n.+)");
    final private static Pattern playbackStatusPattern = Pattern.compile("PlaybackStatus\"\\n.+string \"(Playing|Paused)\"(?:\\n.+)");
    final private static Pattern timeLengthPattern = Pattern.compile("mpris:length\"\\n.+uint64 (\\d+)(?:\\n.+)");
    final private static Pattern artworkUrlPattern = Pattern.compile("mpris:artUrl\"\\n.+string \"(.+)\"(?:\\n.+)");
    final private static Pattern idPattern = Pattern.compile("mpris:trackid\"\\n.+string \"(.+)\"(?:\\n.+)");
    final private static Pattern albumNamePattern = Pattern.compile("xesam:album\"\\n.+string \"(.+)\"(?:\\n.+)");

    static SpotifyState getSpotifyStatus() throws IOException, InterruptedException{

        SpotifyState state = new SpotifyState();

        String rawOutput = getRawSpotifyStatus();
        state.updatedTime = System.currentTimeMillis();

        Matcher matcher = trackNamePattern.matcher(rawOutput);
        if (matcher.find()) state.trackName = matcher.group(1);
        
        matcher = artistNamePattern.matcher(rawOutput);
        if (matcher.find()) state.artist = matcher.group(1);

        if (state.artist.equals(""))
            state.artist = "Unknown Artist";

        matcher = timePositionPattern.matcher(rawOutput);
        if (matcher.find()) state.timePosition = Double.parseDouble(matcher.group(1)) / 1000000;
        globalTimePosition = state.timePosition;

        matcher = playbackStatusPattern.matcher(rawOutput);
        if (matcher.find()) state.playing = matcher.group(1).equals("Playing");

        matcher = timeLengthPattern.matcher(rawOutput);
        if (matcher.find()) state.timeLength = Double.parseDouble(matcher.group(1)) / 1000000;

        matcher = artworkUrlPattern.matcher(rawOutput);
        if (matcher.find()) state.artworkURL = matcher.group(1);

        matcher = idPattern.matcher(rawOutput);
        if (matcher.find()) state.id = matcher.group(1);

        matcher = albumNamePattern.matcher(rawOutput);
        if (matcher.find()) state.albumName = matcher.group(1);

        state.localTrack = localSpotifyTrackPattern.matcher(state.id).find();

        return state;
    }
    static void pausePlaySpotify() throws IOException{
        ProcessBuilder pb = new ProcessBuilder(
            "dbus-send", 
            "--type=method_call", 
            "--session", 
            "--dest=org.mpris.MediaPlayer2.spotify", 
            "/org/mpris/MediaPlayer2", 
            "org.mpris.MediaPlayer2.Player.PlayPause"
        );
        pb.start();
    }
    static void offsetSpotify(double timeOffset) throws IOException{
        ProcessBuilder pb = new ProcessBuilder(
            "dbus-send", 
            "--type=method_call", 
            "--session", 
            "--dest=org.mpris.MediaPlayer2.spotify", 
            "/org/mpris/MediaPlayer2", 
            "org.mpris.MediaPlayer2.Player.Seek",
            String.format("int64:%d", (int)(timeOffset * 1000000))
        );
        pb.start();
    }
    static void seekSpotify(double timePosition) throws IOException{
        offsetSpotify(timePosition - globalTimePosition);
    }
    static void playSpotifyTrack(String trackId) throws IOException{
        System.out.println("Play Spotify Track is an unsupported method on Linux.");
    //     ProcessBuilder pb = new ProcessBuilder(
    //         "dbus-send", 
    //         "--type=method_call", 
    //         "--session", 
    //         "--dest=org.mpris.MediaPlayer2.spotify", 
    //         "/org/mpris/MediaPlayer2", 
    //         "org.mpris.MediaPlayer2.Player.PlayPause"
    //     );
    //     pb.start();
    }
    static void previousTrack() throws IOException{
        ProcessBuilder pb = new ProcessBuilder(
            "dbus-send", 
            "--type=method_call", 
            "--session", 
            "--dest=org.mpris.MediaPlayer2.spotify", 
            "/org/mpris/MediaPlayer2", 
            "org.mpris.MediaPlayer2.Player.Previous"
        );
        pb.start();
    }
    static void skipTrack() throws IOException{
        ProcessBuilder pb = new ProcessBuilder(
            "dbus-send", 
            "--type=method_call", 
            "--session", 
            "--dest=org.mpris.MediaPlayer2.spotify", 
            "/org/mpris/MediaPlayer2", 
            "org.mpris.MediaPlayer2.Player.Next"
        );
        pb.start();
    }
    public static void main(String[] args) throws Exception{
        SpotifyState a = getSpotifyStatus();
        System.out.println(a);
    }
}