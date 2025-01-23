import java.awt.Color;
import java.awt.image.BufferedImage;

public class TerminalVisualizer extends Thread{
    static final String homeDirectory = System.getenv("HOME");

    static final String imageCacheDirectory = String.format("%s/.cache/jademusicvisualizercache/", homeDirectory);
    static final String lyricsDirectory = String.format("%s/Documents/Lyrics/", homeDirectory);

    //Appearance

    static String buffer = "";
    static int terminalWidth = 100;
    static Exception error = null;

    static boolean enableAdditionalGraphics = false;
    static int fps = 0;

    final static Color successColor = new Color(0, 255, 85);
    final static Color failColor = new Color(255, 0, 85);

    static Color foregroundColor = new Color(0, 170, 255);
    static Color secondaryColor = new Color(255, 255, 255);
    static Color tertiaryColor = new Color(75, 75, 75);
    static Color backgroundColor = new Color(10, 10, 29);

    static Color currentForeground = foregroundColor;
    static Color currentBackground = backgroundColor;

    static int charIndex = 0;

    static void writeOutput(Object output){
        String content = output.toString();
        for (int i = 0;i<content.length();i++){
            if (content.charAt(i) == '\n'){
                charIndex = 0;
                buffer += "\n";
                continue;
            }
            buffer += content.charAt(i);
            charIndex ++;
        }
    }
    static void writeOutputRAW(Object output){
        buffer += output.toString();
    }

    static void writeWaveyColouredOutput(Object output, boolean foregroundApplied){
        writeWaveyColouredOutput(output, foregroundApplied, 1);
    }
    static void writeWaveyColouredOutput(Object output, boolean foregroundApplied, double effectLevel){
        if (enableAdditionalGraphics == false){
            writeOutputRAW(output);
            return;
        }
        String content = output.toString();
        Color selected = currentForeground;
        String code = (char)27 + "[38;2;";
        if (foregroundApplied == false){
            selected = currentBackground;
            code = (char)27 + "[48;2;";
        }
        for (int i = 0;i<content.length();i++){
            if (content.charAt(i) == '\n'){
                charIndex = 0;
                buffer += '\n';
                continue;
            }
            if ((content.charAt(i) == '\t' || content.charAt(i) == ' ') && foregroundApplied){
                charIndex ++;
                buffer += content.charAt(i);
                continue;
            }
            if (effectLevel < 0.5 && charIndex%5 != 0){
                charIndex ++;
                buffer += content.charAt(i);
                continue;
            }
            if (effectLevel == 0){
                charIndex ++;
                buffer += content.charAt(i);
                continue;
            }
            if (charIndex >= terminalWidth){
                buffer += '\n';
                charIndex = 0;
            }
            Color localizedColor = lerpColor(selected, selected.brighter().brighter(), effectLevel * (Math.sin(charIndex/4.0 - System.currentTimeMillis()/250.0)+1)/2);
            buffer += String.format(code + 
                String.format("%d;%d;%dm%s", localizedColor.getRed(), localizedColor.getGreen(), localizedColor.getBlue(), content.charAt(i)));
            charIndex ++;
        }
    }

    static void writeOutputLNRAW(Object output){
        buffer += output.toString() + "\n";
    }
    static void writeOutputLN(Object output){
        charIndex = 0;
        buffer += output.toString() + "\n";
    }
    
    static void flushToConsole(){
        System.out.print(buffer);
        charIndex = 0;
        buffer = "";
    }

    static void clearConsoleStyle(){
        writeOutputRAW((char)27 + "[0m");
    }

    static void setBackgroundColor(Color color){
        currentBackground = color;
        writeOutputRAW((char)27 + String.format("[48;2;%d;%d;%dm", color.getRed(), color.getGreen(), color.getBlue()));
    }
    
    static void setForegroundColor(Color color){
        currentForeground = color;
        writeOutputRAW((char)27 + String.format("[38;2;%d;%d;%dm", color.getRed(), color.getGreen(), color.getBlue()));
    }

    static void hideCursor(){
        writeOutputRAW((char)27 + "[?25l");
    }

    static void clearConsole(){
        writeOutputRAW((char)27 +"[2J");
    }


    static Color lerpColor(Color from, Color to, double lerpFactor){
        lerpFactor = Math.max(0, Math.min(1, lerpFactor));
        return new Color(
            (int)(from.getRed() + (to.getRed() - from.getRed()) * lerpFactor),
            (int)(from.getGreen() + (to.getGreen() - from.getGreen()) * lerpFactor),
            (int)(from.getBlue() + (to.getBlue() - from.getBlue()) * lerpFactor)
        );
    }

    static double getTimeFactorLyrics(double currentTimePosition, double firstWordSpoken, double lastWordSpoken, double newLineBeginning){
        double y1Value = 2 * (currentTimePosition - firstWordSpoken) + 1.5f;
        int eNable = (int)Math.max(0, Math.signum(currentTimePosition - (lastWordSpoken + newLineBeginning -0.5)/2));
        double y2Value = - currentTimePosition + lastWordSpoken + 1 + (2*currentTimePosition - lastWordSpoken - newLineBeginning + 0.5) * eNable;

        return 1 - Math.max(Math.min(y1Value, 1), 0) * Math.max(Math.min(y2Value, 1), 0);
    }

    static String parseTime(int time){
        int seconds = time % 60;
        int minutes = time / 60;

        if (seconds < 10){
            return minutes + ":0" + seconds;
        }else{
            return minutes + ":" + seconds;
        }
    }

    //Spotify State
    

    //Spotify player is unable to keep sync with the time position and the song. so, this is fix is needed.
    static boolean aboutToBeginSong = false;
    static boolean spotifyExperiencingBugs = false;
    static boolean leftBeginning = false;

    static SpotifyState currentSpotifyState = null;
    static String songName = "";
    
    static long timeSinceNewSong = System.currentTimeMillis();

    static Color spotifyForegroundColor = new Color(0, 170, 255);
    static Color spotifyBackgroundColor = new Color(10, 10, 29);;


    static long getTimePosition(){
        if (currentSpotifyState.playing == false){
            return (long)(currentSpotifyState.timePosition*1000);
        }else
            if (currentSpotifyState != null){
                if (spotifyExperiencingBugs){
                    return (System.currentTimeMillis() - currentSpotifyState.updatedTime + (int)(currentSpotifyState.timePosition * 1000)) + 150 - 1000;
                }else
                    return (System.currentTimeMillis() - currentSpotifyState.updatedTime + (int)(currentSpotifyState.timePosition * 1000)) + 150;
            }
        return 0l;
    }

    public void run(){

        // System.out.print((char)27 + "[?1049l");
    }

    public static void main(String[] args) throws Exception{
        Runtime.getRuntime().addShutdownHook(new TerminalVisualizer());
        try{
            fps = Integer.parseInt(args[0]);
        }
        catch(Exception er){
            System.out.println("Command Inspect: TerminalVisualizer.jar fps(invalid input) enableAdditionalGraphics");
            System.out.println("Command Example: TerminalVisualizer.jar 60 1");
            System.exit(1);
        };
        try{
            int input = Integer.parseInt(args[1]);
            enableAdditionalGraphics = input == 1;
        }
        catch(Exception er){
            System.out.println("Command Inspect: TerminalVisualizer.jar fps enableAdditionalGraphics(invalid input)");
            System.out.println("Command Example: TerminalVisualizer.jar 60 1");
            System.exit(1);
        };
        /*if (true){
            setBackgroundColor(foregroundColor);
            writeWaveyColouredOutput(
                "[ ♦ Spotify Connected]\nwegwegwegwegweg", false);
            flushToConsole();
            return;
        }*/

        System.out.print((char)27 + "[?1049h");
        //System.out.println((char)27 + "[");

        JadeLyricsManager.parseAllLyrics(lyricsDirectory);
        SpotifyCoverCache.imageCacheLocation = imageCacheDirectory;

        currentSpotifyState = SpotifyState.getSpotifyStatus();
        new Thread(){
            public void run(){
                ProcessBuilder script = new ProcessBuilder("bash", "-c", "tput cols 2> /dev/tty");
                while (true){
                    try{
                        Process process = script.start();
                        byte[] data = process.getInputStream().readAllBytes();
                        terminalWidth = Integer.parseInt(new String(data).split("\n")[0]);
                        Thread.sleep(500);

                        SpotifyState previousState = currentSpotifyState;

                        currentSpotifyState = SpotifyState.getSpotifyStatus();
                        if (currentSpotifyState.timePosition > currentSpotifyState.timeLength - 4){
                            aboutToBeginSong = true;
                        }

                        if (previousState.playing != currentSpotifyState.playing)
                            spotifyExperiencingBugs = false;

                        String spotifyTrackname = "";
                        if (currentSpotifyState != null)
                            spotifyTrackname = currentSpotifyState.trackName;
                            
                        if (songName.equals(spotifyTrackname) == false){
                            spotifyExperiencingBugs = false;
                            songName = spotifyTrackname;
                            BufferedImage image;
                            if (currentSpotifyState.localTrack){
                                image = SpotifyCoverCache.fetchLocalImage(currentSpotifyState.albumName);
                            }else{
                                image = SpotifyCoverCache.fetchImage(currentSpotifyState.artworkURL);
                            }
        
                            if (image != null){
                                spotifyForegroundColor = SpotifyCoverCache.getPrimaryColors(image, SpotifyCoverCache.foregroundParameters);
                                spotifyBackgroundColor = SpotifyCoverCache.getPrimaryColors(image, SpotifyCoverCache.backgroundParameters);
                            }else{
                                spotifyForegroundColor = new Color(0, 170, 255);
                                spotifyBackgroundColor = new Color(10, 10, 29);
                            }
                            
                            /*if (SpotifyCoverCache.measureBrightness(spotifyForegroundColor) < 0.5 && 
                                SpotifyCoverCache.measureBrightness(spotifyBackgroundColor) < 0.5){
                                    spotifyForegroundColor = spotifyForegroundColor.brighter();
                                    spotifyBackgroundColor = spotifyBackgroundColor.darker();
                            }*/
                            /*if (SpotifyCoverCache.measureRelvance(spotifyForegroundColor, spotifyBackgroundColor) > 0){
                                if (SpotifyCoverCache.measureBrightness(spotifyBackgroundColor) > 0.5){
                                    spotifyBackgroundColor = spotifyBackgroundColor.darker().darker().darker().darker();
                                }else
                                    spotifyBackgroundColor = spotifyBackgroundColor.brighter().brighter().brighter().brighter();
                            }*/
                            if (SpotifyCoverCache.measureRelvance(spotifyForegroundColor, spotifyBackgroundColor) > 0.75){
                                spotifyBackgroundColor = spotifyForegroundColor;
                                if (SpotifyCoverCache.measureBrightness(spotifyForegroundColor) > 0.5){
                                    spotifyForegroundColor = new Color(29, 29, 29);
                                }else
                                    spotifyForegroundColor = new Color(255, 255, 255);
                            }else
                                if (SpotifyCoverCache.measureRelvance(spotifyForegroundColor, spotifyBackgroundColor) > 0.25){
                                    if (SpotifyCoverCache.measureBrightness(spotifyBackgroundColor) > 0.5){
                                        spotifyBackgroundColor = spotifyBackgroundColor.darker().darker();
                                    }else
                                        spotifyBackgroundColor = spotifyBackgroundColor.brighter().brighter();
                                }
        
                            timeSinceNewSong = System.currentTimeMillis();
                            image.flush();
                        }

                        if (currentSpotifyState.timePosition < 4 && aboutToBeginSong){
                            aboutToBeginSong = false;
                            spotifyExperiencingBugs = true;
                            leftBeginning = false;
                        }
                        if (currentSpotifyState.timePosition < 4 && leftBeginning){
                            leftBeginning = false;
                            spotifyExperiencingBugs = false; 
                        }
                        if (currentSpotifyState.timePosition > 4 && spotifyExperiencingBugs)
                            leftBeginning = true;
        
                    }
                    catch(Exception er){
                        error = er;
                    }
                }
            }
        }.start();

        new Thread(){
            public void run(){
                while (true){
                    try{
                        int character = System.in.read();
                        System.out.println(character);
                    }
                    catch(Exception er){
                        error = er;
                    }
                }
            }
        }.start();

        /*if (true){
            return;
        }*/

        if (false && currentSpotifyState.playing){
            SpotifyState.pausePlaySpotify();
            Thread.sleep(500);
            SpotifyState.pausePlaySpotify();
        }

        //if (true)
        //    return;
        while (true){
            setBackgroundColor(new Color(10, 10, 29));
            for (int i = 0;currentSpotifyState == null;i = (i + 1)%4){
                setForegroundColor(backgroundColor);
                setBackgroundColor(foregroundColor);
                clearConsole();
                hideCursor();
                writeOutputLN(String.format("Attempting to connect to Spotify%s", ".".repeat(i)) 
                    + " ".repeat(terminalWidth - String.format("Attempting to connect to Spotify%s", ".".repeat(i)).length()));
                flushToConsole();
                Thread.sleep(500);
                currentSpotifyState = SpotifyState.getSpotifyStatus();
            }
            setBackgroundColor(new Color(10, 10, 29));
            clearConsole();
            hideCursor();
            setForegroundColor(backgroundColor);
            setBackgroundColor(successColor);
            writeOutputLN("Successfully connected to Spotify!" 
                + " ".repeat(terminalWidth - "Successfully connected to Spotify!".length()));
            flushToConsole();
            Thread.sleep(1000);
            clearConsoleStyle();
            while (currentSpotifyState != null){

                SpotifyState usedSpotifyState = currentSpotifyState;

                writeOutputLN("");
                setBackgroundColor(backgroundColor);
                setForegroundColor(foregroundColor);
                clearConsole();
                hideCursor();

                /*writeOutput(aboutToBeginSong);
                writeOutput(spotifyExperiencingBugs);*/
                JadeLyricsManager.JadeLyrics jadeLyrics = 
                    JadeLyricsManager.hashedJadeLyrics.get(usedSpotifyState.id.toLowerCase());

                if (jadeLyrics == null){
                    jadeLyrics = JadeLyricsManager.hashedJadeLyrics.get(String.format("%s of %s by %s", usedSpotifyState.trackName, usedSpotifyState.albumName, usedSpotifyState.artist).toLowerCase());
                }
                if (jadeLyrics == null){
                    jadeLyrics = JadeLyricsManager.hashedJadeLyrics.get(String.format("%s by %s", usedSpotifyState.trackName, usedSpotifyState.artist).toLowerCase());
                }
                if (jadeLyrics == null){
                    jadeLyrics = JadeLyricsManager.hashedJadeLyrics.get(songName.toLowerCase());
                }

                {
                    double timeFactor = Math.min(1, (System.currentTimeMillis() - timeSinceNewSong)/1000.0);
                    double animationFactor = Math.pow(timeFactor, 2);
                    foregroundColor = lerpColor(foregroundColor, spotifyForegroundColor, animationFactor);
                    backgroundColor = lerpColor(backgroundColor, spotifyBackgroundColor, animationFactor);
                }

                double timePosition = getTimePosition()/1000.0;

                if (jadeLyrics != null){
                    LyricsPlayer.LyricalState lyricalState = LyricsPlayer.getLyricsState(jadeLyrics, timePosition - 0.1);

                    String[] historicalLines = lyricalState.historicalLines;

                    double actualEndTime = lyricalState.lineEndTime;
                    if (lyricalState.endOflyrics){
                        actualEndTime = usedSpotifyState.timeLength;
                    }

                    double fadeAwayTimeFactor = getTimeFactorLyrics(timePosition, 
                        lyricalState.wordStartTime, lyricalState.wordEndTime + 4, actualEndTime);
                    double animationFactor = (double)Math.pow(fadeAwayTimeFactor-1, 5) + 1;

                    setBackgroundColor(backgroundColor);
                    for (int i = 0;i<historicalLines.length;i++){
                        setForegroundColor(lerpColor(backgroundColor, foregroundColor, (1-animationFactor)*(double)i/historicalLines.length));
                        writeWaveyColouredOutput("   " + historicalLines[i] + "\t".repeat((terminalWidth-historicalLines[i].length()-3)/8+2) + "\n", true, (1-animationFactor)*(double)i/historicalLines.length);
                    }
                    clearConsoleStyle();
                    setForegroundColor(lerpColor(backgroundColor, foregroundColor, 1-animationFactor));
                    setBackgroundColor(backgroundColor);

                    String mainLine = lyricalState.completedLine.substring(0, Math.max(0, lyricalState.completedLine.length() - 1));
                    writeWaveyColouredOutput(" > " + mainLine, true, 1-animationFactor);

                    Color completedLineColor;
                    if (SpotifyCoverCache.measureBrightness(foregroundColor) > 0.5){               
                        completedLineColor = foregroundColor.darker().darker();         
                        
                    }else                        
                        completedLineColor = foregroundColor.brighter().brighter();

                    setForegroundColor(lerpColor(backgroundColor, completedLineColor, 1-animationFactor));
                    writeOutput(lyricalState.completedLine.substring(Math.max(0, lyricalState.completedLine.length() - 1)));
                    setForegroundColor(lerpColor(foregroundColor, backgroundColor, (animationFactor)*0.25 +0.75));
                    writeOutputLN(lyricalState.mainLine.substring(lyricalState.completedLine.length()) + "\t".repeat((terminalWidth - lyricalState.mainLine.length() - 3)/8+2));
                    writeOutputLN("   " + lyricalState.futureLine + "\t".repeat((terminalWidth-lyricalState.futureLine.length()-3)/8+2) + 
                        "\n" + "\t".repeat(terminalWidth/8+2));
                }else{
                    writeWaveyColouredOutput(String.format("Unable to display lyrics.\nThis Song Does not Support Jade Lyrics!\n%s\n", 
                        " ".repeat(terminalWidth)), true);
                }

                setBackgroundColor(foregroundColor);
                setForegroundColor(backgroundColor);

                String status = "";
                String songStatus = String.format("\"%s\" by %s", usedSpotifyState.trackName, usedSpotifyState.artist);

                status += String.format("[%s / %s]", parseTime((int)timePosition), parseTime((int)usedSpotifyState.timeLength));
                status += String.format(" [ ♦ Spotify Connected] ");

                if ((status + songStatus).length() > terminalWidth){
                    status += " ".repeat(Math.max(0, terminalWidth - status.length()));
                    status += "" + songStatus + " ".repeat(Math.max(0, terminalWidth-songStatus.length()-1));
                }else{
                    status += songStatus;
                    status += " ".repeat(Math.max(0, terminalWidth - status.length()));
                }

                writeWaveyColouredOutput(status + "\n", false);

                int progressBarWidth = terminalWidth - 2;
                String progressBar = "[" + "|".repeat(Math.max(0, Math.min(progressBarWidth, (int)(timePosition/usedSpotifyState.timeLength*progressBarWidth + 0.5)-1)));
                writeWaveyColouredOutput(progressBar, false);
                int length = progressBar.length();//(0.8521*50-floor(0.8521*50))
                setForegroundColor(lerpColor(foregroundColor, backgroundColor, 
                    ((timePosition/usedSpotifyState.timeLength*progressBarWidth + 0.5)-(int)(timePosition/usedSpotifyState.timeLength*progressBarWidth + 0.5))));
                writeWaveyColouredOutput("|", false);
                setForegroundColor(lerpColor(foregroundColor, backgroundColor, 0.5));
                if (jadeLyrics != null){
                    for (int i = length;i<progressBarWidth;i++){
                        double timePos = (double)i/progressBarWidth*usedSpotifyState.timeLength;
                        int timeIndex = (int)(timePos*JadeLyricsManager.detailFactor+0.5)+8;
                        JadeLyricsManager.LyricalLine lyricalLine = null;
                        if (timeIndex < jadeLyrics.lyricalLinesTimeReferences.length)
                            lyricalLine = jadeLyrics.lyricalLinesTimeReferences[timeIndex];
                        if (lyricalLine != null && lyricalLine.line.equals(" ") == false){
                            double startTime = lyricalLine.lyricalInstructions[0].time-1;
                            double endTime = lyricalLine.lyricalInstructions[Math.max(0, lyricalLine.lyricalInstructions.length-1)].time + 2; 
                            if (startTime < timePos && endTime > timePos){
                                writeWaveyColouredOutput("-", false);
                            }else
                            writeWaveyColouredOutput(" ", false);
                        }else
                        writeWaveyColouredOutput(" ", false);
                    }
                }else
                    writeWaveyColouredOutput(" ".repeat(progressBarWidth-length), false);
                setForegroundColor(backgroundColor);
                writeWaveyColouredOutput("]", false);

                if (error != null){
                    setBackgroundColor(failColor);
                    setForegroundColor(backgroundColor);

                    writeOutput(String.format("\n[Error][An oopsies has occured]: " + error.getMessage()));
                    error.printStackTrace();
                    error = null;
                    Thread.sleep(1000);
                }

                flushToConsole();
                Thread.sleep(1000/fps);
            }
            flushToConsole();
        }
    }    
}
