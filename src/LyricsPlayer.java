import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LyricsPlayer {
    final static int detailFactor = JadeLyricsManager.detailFactor;

    static Pattern dashLinePattern = Pattern.compile("(?:waittill (\\d*\\.\\d*);\necho -n \"(.*? )\")|(?:echo -n \"\n\\[2K\")");

    static class LyricalState{
        String completedLine = "";
        String mainLine = "";
        String[] historicalLines = new String[]{"","","","","","","","","",""};
        String futureLine = "";
        
        double lineStartTime;
        double wordEndTime;
        double lineEndTime;
        double wordStartTime;

        double currentWordStartTime = 0;
        double currentWordEndTime = 0;
        
        double lineProgresPercentage = 0;

        boolean endOflyrics = false;
    }

    final static double initialLyricalTiming = 0.2;

    static LyricalState getLyricsState(JadeLyricsManager.JadeLyrics jadeLyrics, double timePosition){

        timePosition += LyricsPlayer.initialLyricalTiming;
        timePosition = Math.max(Math.min(timePosition, jadeLyrics.timeLength-2), 0);

        int currentTimeIndex = (int)((timePosition + 0.5) * LyricsPlayer.detailFactor);
        JadeLyricsManager.LyricalLine nearestLyricalLine = jadeLyrics.lyricalLinesTimeReferences[currentTimeIndex];
        
        if (nearestLyricalLine.endTime < timePosition){
            currentTimeIndex ++;
        }else
            if (nearestLyricalLine.startTime > timePosition){
                currentTimeIndex --;
            }

        currentTimeIndex = Math.min(Math.max(currentTimeIndex, 0), jadeLyrics.lyricalLinesTimeReferences.length-1);

        JadeLyricsManager.LyricalLine lyricalLine = jadeLyrics.lyricalLinesTimeReferences[currentTimeIndex];

        if (lyricalLine.disapparenceThresholdTime < timePosition){
            lyricalLine = jadeLyrics.lyricalLines[Math.min(lyricalLine.index + 1, jadeLyrics.lyricalLines.length-1)];
        }

        JadeLyricsManager.LyricalLine newLineLine = null;
        while (lyricalLine.line.equals(" ") && lyricalLine.index > 0){
            newLineLine = lyricalLine;
            lyricalLine = jadeLyrics.lyricalLines[Math.max(0, lyricalLine.index-1)];
        }
        
        int currentIndex = lyricalLine.index;


        LyricalState lyricalState = new LyricalState();

        if (currentIndex < jadeLyrics.lyricalLines.length-1){
            int potentialFutureLineIndex = currentIndex + 1;
            while (jadeLyrics.lyricalLines[potentialFutureLineIndex ++].line.equals(" ") && potentialFutureLineIndex < jadeLyrics.lyricalLines.length);
            JadeLyricsManager.LyricalLine futureLine = jadeLyrics.lyricalLines[potentialFutureLineIndex - 1];
            if (futureLine.disapparenceThresholdTime == lyricalLine.disapparenceThresholdTime)
                lyricalState.futureLine = futureLine.line;
        }else{
            lyricalState.endOflyrics = true;
        }

        int currentLyricalInstruction = lyricalLine.lyricalInstructions.length - 1;
        double endTime = lyricalLine.endTime;
        for (int i = 1;i<lyricalLine.lyricalInstructions.length;i++){
            JadeLyricsManager.LyricalInstruction futureInstruction = lyricalLine.lyricalInstructions[i];
            JadeLyricsManager.LyricalInstruction instruction = lyricalLine.lyricalInstructions[i-1];
            if (futureInstruction.time < timePosition - LyricsPlayer.initialLyricalTiming){
                lyricalState.completedLine += instruction.newString;
            }else{
                currentLyricalInstruction = i - 1;
                endTime = futureInstruction.time;
                break;
            }
        }
        if (currentLyricalInstruction + 1 == lyricalLine.lyricalInstructions.length - 1)
            endTime -= LyricsPlayer.initialLyricalTiming/2;

        JadeLyricsManager.LyricalInstruction currentInstruction = lyricalLine.lyricalInstructions[currentLyricalInstruction];

        if (timePosition < lyricalLine.disapparenceThresholdTime){
            lyricalState.mainLine = lyricalLine.line;
            if (currentInstruction.time < timePosition - LyricsPlayer.initialLyricalTiming){
                if (lyricalState.endOflyrics && currentLyricalInstruction == lyricalLine.lyricalInstructions.length - 1){
                    endTime += 1;
                }

                if (currentLyricalInstruction == lyricalLine.lyricalInstructions.length - 1){
                    endTime -= .2;
                }
                double startTime = currentInstruction.time;
                endTime = Math.min(Math.max(startTime+.2, endTime), startTime+2);

                double timeFactor = Math.min(1, (timePosition - LyricsPlayer.initialLyricalTiming -startTime)/(endTime-startTime));

                lyricalState.currentWordStartTime = startTime;
                lyricalState.currentWordEndTime = endTime;

                double factor = //Math.pow(4*Math.pow(timeFactor - 0.5, 3) + 0.5, 1/3.0);
                    1 - Math.pow(1 - timeFactor, 2);


                int matches = 0;
                Matcher matcher = dashLinePattern.matcher(currentInstruction.newString);

                while (true){
                    if (matcher.find() == false){
                        break;
                    }
                    matches += 1;
                }

                if (matches >= 3){
                    factor = timeFactor;
                }
                // dashLinePattern.matcher(currentInstruction.newString);
                // if ((currentInstruction.newString.match(/[-.]/g) || []).length >= 3){
                //     factor = timeFactor
                // }

                lyricalState.lineProgresPercentage = (lyricalState.completedLine.length() - 1)/lyricalState.mainLine.length() + factor *(currentInstruction.newString.length())/lyricalState.mainLine.length();

                lyricalState.completedLine += currentInstruction.newString.substring(0, (int)(factor*(currentInstruction.newString.length()+0.5)));
            }
        }else{
            lyricalState.completedLine = "";
        }
        
        // int currentHistoricalLineIndex = currentIndex - 1;
        // while (currentHistoricalLineIndex >= 0 && lyricalState.historicalLines.length < 10){
        //     JadeLyricsManager.LyricalLine ls = jadeLyrics.lyricalLines[currentHistoricalLineIndex];

        //     if (ls.disapparenceThresholdTime > timePosition)
        //         if (ls.line != " ")
        //             lyricalState.historicalLines.push(ls.line);

        //     currentHistoricalLineIndex --;
        // }


        int currentBackLine = 1;

        for (int i = 0;i<10;i++){
            if (currentIndex - currentBackLine < 0)
                break;
            JadeLyricsManager.LyricalLine ls = jadeLyrics.lyricalLines[currentIndex - currentBackLine];

            while (ls.line.equals(" ")){
                currentBackLine += 1;
                if (currentIndex - currentBackLine < 0)
                    break;
                ls = jadeLyrics.lyricalLines[currentIndex - currentBackLine];
            };
    
            if (ls.disapparenceThresholdTime > timePosition)
                lyricalState.historicalLines[9 - i] = ls.line;

            
            currentBackLine += 1;

        }

        // for (int i = lyricalState.historicalLines.length;i<11;i++){
        //     lyricalState.historicalLines.push("");
        // }
        // lyricalState.historicalLines.reverse();

        lyricalState.lineStartTime = lyricalLine.startTime - LyricsPlayer.initialLyricalTiming;
        lyricalState.lineEndTime = lyricalLine.endTime - LyricsPlayer.initialLyricalTiming;
        lyricalState.wordEndTime = lyricalLine.lyricalInstructions[lyricalLine.lyricalInstructions.length-1].time - LyricsPlayer.initialLyricalTiming;
        lyricalState.wordStartTime = lyricalLine.lyricalInstructions[0].time - LyricsPlayer.initialLyricalTiming;
        if (newLineLine != null){
            lyricalState.lineEndTime = newLineLine.endTime - LyricsPlayer.initialLyricalTiming;
            if (timePosition > lyricalLine.disapparenceThresholdTime){
                lyricalState.wordStartTime = jadeLyrics.lyricalLines[Math.min(jadeLyrics.lyricalLines.length - 1, lyricalLine.index+1)].lyricalInstructions[0].time - LyricsPlayer.initialLyricalTiming;
            }
        }

        return lyricalState;
    }
    static LyricalState getLyricsStateLegacy(JadeLyricsManager.JadeLyrics jadeLyrics, double timePosition){
        timePosition += initialLyricalTiming;
        timePosition = Math.max(Math.min(timePosition, jadeLyrics.timeLength-2), 0);

        int currentTimeIndex = (int)((timePosition + 0.5) * detailFactor);
        JadeLyricsManager.LyricalLine nearestLyricalLine = jadeLyrics.lyricalLinesTimeReferences[currentTimeIndex];
        
        if (nearestLyricalLine.endTime < timePosition){
            currentTimeIndex ++;
        }else
            if (nearestLyricalLine.startTime > timePosition){
                currentTimeIndex --;
            }

        currentTimeIndex = Math.min(Math.max(currentTimeIndex, 0), jadeLyrics.lyricalLinesTimeReferences.length-1);

        JadeLyricsManager.LyricalLine lyricalLine = jadeLyrics.lyricalLinesTimeReferences[currentTimeIndex];


        if (lyricalLine.disapparenceThresholdTime < timePosition){
            lyricalLine = jadeLyrics.lyricalLines[Math.min(lyricalLine.index + 1, jadeLyrics.lyricalLines.length-1)];
        }

        int currentIndex = lyricalLine.index;

        LyricalState lyricalState = new LyricalState();
        if (currentIndex < jadeLyrics.lyricalLines.length-1){
            if (jadeLyrics.lyricalLines[currentIndex+1].disapparenceThresholdTime == lyricalLine.disapparenceThresholdTime)
                lyricalState.futureLine = jadeLyrics.lyricalLines[currentIndex+1].line;
        }else{
            lyricalState.endOflyrics = true;
        }

        int currentLyricalInstruction = lyricalLine.lyricalInstructions.length - 1;
        double endTime = lyricalLine.endTime;
        for (int i = 1;i<lyricalLine.lyricalInstructions.length;i++){
            JadeLyricsManager.LyricalInstruction futureInstruction = lyricalLine.lyricalInstructions[i];
            JadeLyricsManager.LyricalInstruction instruction = lyricalLine.lyricalInstructions[i-1];
            if (futureInstruction.time < timePosition - initialLyricalTiming){
                lyricalState.completedLine += instruction.newString;
            }else{
                currentLyricalInstruction = i - 1;
                endTime = futureInstruction.time;
                break;
            }
        }
        if (currentLyricalInstruction + 1 == lyricalLine.lyricalInstructions.length - 1)
            endTime -= initialLyricalTiming/2;

        JadeLyricsManager.LyricalInstruction currentInstruction = lyricalLine.lyricalInstructions[currentLyricalInstruction];

        if (timePosition < lyricalLine.disapparenceThresholdTime){
            lyricalState.mainLine = lyricalLine.line;
            if (currentInstruction.time < timePosition - initialLyricalTiming){
                if (lyricalState.endOflyrics && currentLyricalInstruction == lyricalLine.lyricalInstructions.length - 1){
                    endTime += 1;
                }
                double startTime = currentInstruction.time;
                endTime = (double)Math.min(startTime+2, endTime);

                double timeFactor = Math.min(1, (timePosition - initialLyricalTiming -startTime)/(endTime-startTime));

                lyricalState.currentWordStartTime = startTime;
                lyricalState.currentWordEndTime = endTime;

                double factor = //Math.pow(4*Math.pow(timeFactor - 0.5, 3) + 0.5, 1/3.0);
                    1 -Math.pow(1 - timeFactor, 5);

                factor = Math.min(Math.max(0, factor), 1);

                lyricalState.completedLine += currentInstruction.newString.substring(0, (int)(factor*(currentInstruction.newString.length()+0.5)));
            }
        }else{
            lyricalState.completedLine = "";
        }
            
        for (int i = Math.max(0, currentIndex - 10);i<currentIndex;i++){
            JadeLyricsManager.LyricalLine ls = jadeLyrics.lyricalLines[i];
            if (ls.disapparenceThresholdTime > timePosition)
                lyricalState.historicalLines[10-(currentIndex-i)] = ls.line;
        }

        lyricalState.lineStartTime = lyricalLine.startTime - initialLyricalTiming;
        lyricalState.lineEndTime = lyricalLine.endTime - initialLyricalTiming;
        lyricalState.wordEndTime = lyricalLine.lyricalInstructions[lyricalLine.lyricalInstructions.length-1].time - initialLyricalTiming;
        lyricalState.wordStartTime = lyricalLine.lyricalInstructions[0].time - initialLyricalTiming;

        return lyricalState;
    }
    static void main(String[] args) throws Exception{
        //JadeLyricsManager.parseAllLyrics();
        long timePosition = System.currentTimeMillis();
        JadeLyricsManager.JadeLyrics jl = JadeLyricsManager.hashedJadeLyrics.get("passionfruit");
        while (true){
            Thread.sleep(1);
            LyricalState state = getLyricsState(jl, (System.currentTimeMillis()-timePosition)/1000.0f);
            System.out.println(state.completedLine);
        }
    }
}