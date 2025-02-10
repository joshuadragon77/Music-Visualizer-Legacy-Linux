import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

class JadeLyricsManager {
    final static int detailFactor = 8;
    
    final private static Pattern jadeLyricsPattern = Pattern.compile("(?:waittill (\\d*\\.\\d*);\necho -n \"(.*? )\")|(?:echo -n \"\n\\[2K\")");
    final private static Pattern jadeLyricsNamePattern = Pattern.compile("# Jade's Lyrics Generator for the song (.*)\n");

    static LinkedList<JadeLyrics> parsedJadeLyrics = new LinkedList<JadeLyrics>();
    static HashMap<String, JadeLyrics> hashedJadeLyrics = new HashMap<String, JadeLyrics>();

    /*static class LyricsInstruction{
        String newString;
        double time;
        LyricsLine line;
        LyricsInstruction(String newString, double time, LyricsLine associatedLine){
            this.time = time;
            this.newString = newString;
            this.line = associatedLine;
        }
    }*/

    static class LyricalInstruction{
        String newString;
        double time;
    }

    static class LyricalLine{
        String line = "";
        double startTime;
        double endTime;
        LyricalInstruction[] lyricalInstructions;
        int index;
        double disapparenceThresholdTime = 10000000;
    }

    static class JadeLyrics{
        String songName;
        double timeLength = 0;
        LyricalLine[] lyricalLinesTimeReferences;
        LyricalLine[] lyricalLines;
    }

    static JadeLyrics parseLyricsFile(File file) throws IOException{
        FileReader fr = new FileReader(file);

        char[] newReadStream = new char[256];
        int readCharacter = fr.read(newReadStream);
        
        String readStream = String.valueOf(newReadStream);

        Matcher nameMatcher = jadeLyricsNamePattern.matcher(readStream);

        if (nameMatcher.find() == false){
            fr.close();
            return null;
        }


        while (true){
            newReadStream = new char[1024];
            readCharacter = fr.read(newReadStream);
            
            readStream += String.valueOf(newReadStream);

            if (readCharacter == -1)
                break;
        }
        fr.close();

        
        Matcher matcher = jadeLyricsPattern.matcher(readStream);
        double currentTime = 0;
        boolean newLined = false;

        JadeLyrics jl = new JadeLyrics();
        jl.songName = nameMatcher.group(1);

        LyricalLine line = new LyricalLine();
        line.startTime = currentTime;

        LinkedList<LyricalLine> compiledLines = new LinkedList<LyricalLine>();
        LinkedList<LyricalInstruction> compiledInstructions = new LinkedList<LyricalInstruction>();

        int disappearenceIndex = 0;
        boolean justAddedNewLine = false;

        while (true){

            if (matcher.find() == false)
                break;
            
            String parsedTime = matcher.group(1);
            String parsedString = matcher.group(2);

            if (parsedTime == null && parsedString == null){
                newLined = true;
                continue;
            }
            currentTime = Double.parseDouble(parsedTime);
            if (newLined){
                newLined = false;

                line.lyricalInstructions = new LyricalInstruction[compiledInstructions.size()];
                for (int i = 0;i<compiledInstructions.size();i++){
                    line.lyricalInstructions[i] = compiledInstructions.get(i);
                }

                justAddedNewLine = true;

                line.endTime = currentTime;
                compiledInstructions.clear();
                compiledLines.add(line);

                line = new LyricalLine();
                line.startTime = currentTime;
            }

            if (justAddedNewLine){
                justAddedNewLine = false;

                LyricalLine previousLine = compiledLines.getLast();
                double initialTime = previousLine.lyricalInstructions[previousLine.lyricalInstructions.length-1].time;
                double finalTime = currentTime;
                if (finalTime - 0.865 - initialTime > 4.365){
                    for (int i = disappearenceIndex;i<compiledLines.size();i++){
                        compiledLines.get(i).disapparenceThresholdTime = initialTime + 4.365;
                    }
                    disappearenceIndex = compiledLines.size();
                }
            }

            String word = parsedString.replaceAll("\\\\\"", "\"");
            line.line += word;
            
            LyricalInstruction lyricalInstruction = new LyricalInstruction();
            lyricalInstruction.newString = word;
            lyricalInstruction.time = currentTime;
            compiledInstructions.add(lyricalInstruction);
        }
        if (compiledLines.size() == 0)
            return null;

        line.lyricalInstructions = new LyricalInstruction[compiledInstructions.size()];
        for (int i = 0;i<compiledInstructions.size();i++){
            line.lyricalInstructions[i] = compiledInstructions.get(i);
        }

        line.endTime = currentTime;
        compiledInstructions.clear();
        compiledLines.add(line);

        line = new LyricalLine();
        line.startTime = currentTime;

        jl.timeLength = currentTime + 4;
        jl.lyricalLinesTimeReferences = new LyricalLine[(int)(jl.timeLength)*detailFactor];
        jl.lyricalLines = new LyricalLine[compiledLines.size()];
        for (int i = 0;i<compiledLines.size();i++){
            LyricalLine selectedLine = compiledLines.get(i);
            selectedLine.index = i;
            jl.lyricalLines[i] = selectedLine;
            for (int time = (int)((selectedLine.startTime+.5f)*detailFactor);time<(int)((selectedLine.endTime+4))*detailFactor;time++){
                jl.lyricalLinesTimeReferences[time] = selectedLine;
            }
        }

        
        return jl;
    }

    static void parseAllLyrics(String filePath) throws IOException{
        File directory = new File(filePath);

        if (directory.exists() == false){
            directory.mkdir();
        }

        File[] lyricalFiles = directory.listFiles();

        parsedJadeLyrics.clear();

        int successful = 0;

        for (int i = 0;i<lyricalFiles.length;i++){
            File lyricalFile = lyricalFiles[i];
            if (lyricalFile.isFile()){
                JadeLyrics jl = parseLyricsFile(lyricalFile);
                if (jl != null){
                    successful ++;
                    parsedJadeLyrics.add(jl);
                    hashedJadeLyrics.put(jl.songName.toLowerCase(), jl);
                }
            }
        }
        System.out.println("Successfully parsed " + successful + " lyrical files out of " + lyricalFiles.length);
    }
    
    static void main(String[] args) throws Exception{
        //parseAllLyrics();
        parseLyricsFile(new File("/Users/joshuaounalom/Documents/Scripts/Lyrics/Starlight"));
    }
}
