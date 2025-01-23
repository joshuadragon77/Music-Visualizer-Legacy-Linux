import java.awt.image.BufferedImage;

import java.net.URL;
import java.sql.Date;
import java.text.DateFormat;
import java.io.File;

import java.io.FileReader;
import java.io.IOException;

import java.awt.Color;

import javax.imageio.ImageIO;

import java.util.Scanner;
import java.util.regex.*;

public class SpotifyCoverCache {
    
    final private static Pattern spotifyCoverURLPattern = Pattern.compile("https:\\/\\/i.scdn.co\\/image\\/(.*)");

    static String imageCacheLocation = "";

    static double measureGreyLevel(Color c){
        return (1 - Math.min(1, Math.abs(c.getRed() - c.getGreen())/127.0)) * (1 - Math.min(1, Math.abs(c.getRed() - c.getBlue())/127.0));
    }
    static double measureRelvance(Color c1, Color c2){
        return (1 - Math.min(1, Math.abs(c1.getRed() - c2.getRed())/255.0)) *
            (1 - Math.min(1, Math.abs(c1.getGreen() - c2.getGreen())/255.0)) *
            (1 - Math.min(1, Math.abs(c1.getBlue() - c2.getBlue())/255.0));
    }
    static double measureVibrance(Color c){
        return Math.max(Math.max(c.getBlue(), c.getGreen()), c.getRed())/255.0 * (1 - c.getBlue()/255.0 * c.getGreen()/255.0 * c.getRed()/255.0);
    }
    static double measureBrightness(Color c){
        return (c.getRed() + c.getBlue() + c.getGreen())/3.0/255;
    }

    /*class SavedParameters{
        static double relevancyLevel = 0.6800016886309095;
        static int sampleSize = 218;
        static double vibranceLevel = 9.416028986724598;
        static double ignoranceOfCommonality = 1.6653018955294254;
        static double smallSampleLevel = -14.44187212952454;
        static double greyScaleLevel = 4.103452884214695;
        static double uniqueLevel = -3.138287410172077;
    }*/


    static class Parameters{
        double relevancyLevel = 0.5;
        int sampleSize = 10;
        double vibranceLevel = 2;
        double ignoranceOfCommonality = 1/16.0;
        double smallSampleLevel = 2;
        double greyScaleLevel = 1;
        double uniqueLevel = 1;
        double brightnessLevel = 1;
        double darknessLevel = 1;
        Parameters(){};
        Parameters(double relevancyLevel, int sampleSize, double vibranceLevel, double ignoranceOfCommonality, double smallSampleLevel, double greyScaleLevel, double uniqueLevel, double brightnessLevel, double darknessLevel){
            this.relevancyLevel = relevancyLevel;
            this.sampleSize = sampleSize;
            this.vibranceLevel = vibranceLevel;
            this.ignoranceOfCommonality = ignoranceOfCommonality;
            this.smallSampleLevel = smallSampleLevel;
            this.greyScaleLevel = greyScaleLevel;
            this.uniqueLevel = uniqueLevel;
            this.brightnessLevel = brightnessLevel;
            this.darknessLevel = darknessLevel;
        }
    }

    final static Parameters genericParameter = new Parameters();
    final static Parameters whiteParameter = new Parameters(0.9571372710411626, 90, 1.21481894694214, 1.2391442528550245,-0.4885944351645626,0.5947544569963703,1.3767177474415435,-2.988488317253549,-9.328414275877135);
    final static Parameters foregroundParameters = new Parameters(
        0.810536892101989,
        35,
        15.074702546038452,
        3.6437789828167393,
        13.66534402820978,
        11.77614577819662,
        2.6740286539562677,
        -0.5126769043144241,
        -4.424771864999498
    );
    //new Parameters(0.8103179300866394,35,14.711692345218939,3.664591066706545,13.587841111550873,12.245966017780672,2.1288045560035487,0.34828840202415745,-3.9286954031525525);
    //new Parameters(0.8115446364736691, 10, 5.2529373082032205, 0.6455554205432104, 1.518713428766354, 2.911032535080988,-2.243117796346713,-2.127503171251606,-2.9847511601240457);
    //new Parameters(0.7047172357110041, 17, 15.206610090725839, 0.6441593510764148, 15.95653639108367, 11.795623537952572, 2.26765472939178, 1, 1);

    /*class SavedParameters{
        //Detection of Background Colors
        static double relevancyLevel = 0.3555221842572829;
        static int sampleSize = 121;
        static double vibranceLevel = -1.6873517345505524;
        static double ignoranceOfCommonality = -0.49266346651647713;
        static double smallSampleLevel = -17.346806563623343;
        static double greyScaleLevel = -0.5216615389388846;
        static double uniqueLevel = 16.657278110032866;
    }*///76
    final static Parameters backgroundParameters = new Parameters(0.7900542435600248, 76, -12.277206152130615, 393.96405899603724,-305.14984595844174,-75.26089868800373, 90.53390395061945,-129.49539217246766,-91.45155112355047);
    //new Parameters(0.7917813538346227, 195, -12.213680192576234, 395.2280378703318, -304.2036145633889, -76.78132935819983, 89.71693434248095, -132.45981719045795, -90.70373252120746);

    static Color getPrimaryColors(BufferedImage im, Parameters parameters){
        class ColorArrange{
            Color color;
            int counts;
        }
        ColorArrange[] colorArrangement = new ColorArrange[100];

        int uniqueColors = 0;

        if (im == null)
            if (parameters.equals(foregroundParameters)){
                return new Color(255, 255, 255);
            }else
                return new Color(10, 10, 29);

        for (int x = 0;x<parameters.sampleSize;x++){
            for (int y = 0;y<parameters.sampleSize;y++){
                int color = im.getRGB(x*im.getWidth()/parameters.sampleSize, y*im.getHeight()/parameters.sampleSize);
                Color colorA = new Color(color);

                for (int i = 0;i<colorArrangement.length;i++){
                    ColorArrange ca = colorArrangement[i];
                    if (ca == null){
                        ca = new ColorArrange();
                        colorArrangement[i] = ca;
                        ca.color = colorA;
                        ca.counts ++;
                        uniqueColors ++;
                        break;
                    }else{
                        if (measureRelvance(ca.color, colorA) > parameters.relevancyLevel){
                            ca.counts ++;
                            break;
                        }
                    }
                }


            }
        }


        double fitness = 0;
        Color selectedColor = new Color(255, 255, 255);

        for (int i = 0;i<uniqueColors;i++){
            ColorArrange ca = colorArrangement[i];
            
            double currentFitness = Math.pow(1 - measureBrightness(ca.color), parameters.darknessLevel) * Math.pow(measureBrightness(ca.color), parameters.brightnessLevel) * Math.pow(1 - measureGreyLevel(ca.color), parameters.greyScaleLevel) * Math.pow(measureVibrance(ca.color) * Math.pow(1 - uniqueColors/100, parameters.uniqueLevel), parameters.vibranceLevel) 
                * Math.pow((double)ca.counts/parameters.sampleSize/parameters.sampleSize, parameters.ignoranceOfCommonality) * (Math.pow(1 - (double)ca.counts/parameters.sampleSize/parameters.sampleSize, parameters.smallSampleLevel));
            //System.out.println((char)27 + String.format("[48;2;%d;%d;%dm", ca.color.getRed(), ca.color.getGreen(), ca.color.getBlue()) + ca.color.toString() + ", " + currentFitness);

            if (currentFitness > fitness){
                selectedColor = ca.color;
                fitness = currentFitness;
            }
        }

        //System.out.println((char)27 + String.format("[48;2;%d;%d;%dm", selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue()) + selectedColor.toString());
    

        return selectedColor;
    };


    static BufferedImage fetchImage(String spotifyImageUrl) throws IOException{
        
        Matcher m = spotifyCoverURLPattern.matcher(spotifyImageUrl);
        if (m.find() == false)
            return null;
        
        String hashedUrl = m.group(1);
        //System.out.println(hashedUrl);
        File imageFile = new File(imageCacheLocation + File.separatorChar + hashedUrl);
        
        BufferedImage image;
        if (imageFile.exists() == false){
            image = ImageIO.read(new URL(spotifyImageUrl));

            File directory = new File(imageCacheLocation);

            if (directory.exists() == false){
                directory.mkdir();
            }
            
            ImageIO.write(image, "jpg", imageFile);
        }else{
            image = ImageIO.read(imageFile);
        }

        return image;
    }

    static BufferedImage fetchLocalImage(String albumName) throws IOException{
        
        File imageFile = new File(imageCacheLocation + File.separatorChar + albumName + ".jpg");

        if (imageFile.exists() == false)
            return null;

        return ImageIO.read(imageFile);
    }

    public static void main(String[] args) throws Exception{
        imageCacheLocation = args[0];
        //if (args[0].equals("mac")){
        //    imageCacheLocation = "/Users/joshuaounalom/Pictures/MusicVisualizerCache";
        //}else
        //    if (args[0].equals("andriod")){
        //        imageCacheLocation = "/Users/joshuaounalom/Pictures/MusicVisualizerCache";
        //    }else
        //        imageCacheLocation = "/home/thejades/Documents/TheJadesHomeNetwork/HomeDrive/Joshua's Space/Documents/SpotifyArtworkAlgorithm/Cache";
        //        imageCacheLocation = "/home/thejades/Documents/TheJadesHomeNetwork/HomeDrive/Joshua's Space/Documents/SpotifyArtworkAlgorithm/Cache";
        
        Scanner a = new Scanner(System.in);
        System.out.print("Number of Threads Allocated: ");
        int threads = Integer.parseInt(a.nextLine());

        train(threads);

        //System.out.println(getPrimaryColors(fetchImage("https://i.scdn.co/image/ab67616d0000b27318b8088fe0c3dbf78398b55a")));
    }
    public static void train(int threads) throws IOException, InterruptedException{
        Pattern trainingPattern = Pattern.compile("(\\d*);(\\d*);(\\d*)m;(.*);(\\d*);(\\d*)");
        File file = new File("background.txt");
        
        class TrainingData{
            String imageUrl;
            BufferedImage im;
            Color c;
        }
        TrainingData[] data = new TrainingData[1000];

        FileReader fr = new FileReader(file);

        char[] newReadStream = new char[256];
        int readCharacter;
        
        String readStream = String.valueOf(newReadStream);

        while (true){
            newReadStream = new char[1024];
            readCharacter = fr.read(newReadStream);
            
            readStream += String.valueOf(newReadStream);

            if (readCharacter == -1)
                break;
        }
        fr.close();
        
        Matcher m = trainingPattern.matcher(readStream);

        int max = 0;
        {
            int i = 0;
            while (m.find()){
                data[i] = new TrainingData();
                data[i].c = new Color(
                    Integer.parseInt(m.group(1)),
                    Integer.parseInt(m.group(2)),
                    Integer.parseInt(m.group(3))
                );
                data[i].imageUrl = m.group(4);
                File imageFile = new File(imageCacheLocation + File.separatorChar + data[i].imageUrl);
                data[i].im = ImageIO.read(imageFile);
                i ++;
            }
            System.out.println("done loading training data of " + i);
            max = i;
        }

        System.out.println("training...");


        Parameters currentParameter = new Parameters();   
        /*currentParameter.sampleSize = 10; 
        currentParameter.brightnessLevel = 1;
        currentParameter.darknessLevel = 1;
        currentParameter.greyScaleLevel = 1;
        currentParameter.ignoranceOfCommonality = 1;
        currentParameter.relevancyLevel = 0.5;
        currentParameter.uniqueLevel = 1;
        currentParameter.vibranceLevel = 1;
        currentParameter.smallSampleLevel = 1;*/
        Parameters savedParameter = new Parameters();    
        savedParameter.sampleSize = 200; 

        /*Parameters.relevancyLevel = SavedParameters.relevancyLevel;
        Parameters.sampleSize = SavedParameters.sampleSize;
        Parameters.vibranceLevel = SavedParameters.vibranceLevel;
        Parameters.ignoranceOfCommonality = SavedParameters.ignoranceOfCommonality;
        Parameters.smallSampleLevel = SavedParameters.smallSampleLevel;
        Parameters.greyScaleLevel = SavedParameters.greyScaleLevel;
        Parameters.uniqueLevel = SavedParameters.uniqueLevel;*/
        

        class Result{
            String reason;
            boolean matched = false;
        }
        int maxIterations = max;
        int numberOfThreads = threads;
        MultiThreader mt = new MultiThreader(numberOfThreads){
            @Override
            public Object run(int threadId, Object[] args) {
                Result[] results = new Result[maxIterations/numberOfThreads];
                // TODO Auto-generated method stub

                for (int i = 0;i<maxIterations/numberOfThreads;i++){
                    
                    int r = i + maxIterations*threadId/numberOfThreads;
                    Result result = new Result();
                    results[i] = result;

                    Color c = getPrimaryColors(data[r].im, currentParameter);

                    double relevancy = measureRelvance(c, data[r].c);
                    if (relevancy > 0.75){
                        result.matched = true;
                    }else{
                        result.reason = "Expected Color:" + (char)27 + String.format("[48;2;%d;%d;%dm%s", 
                            data[i].c.getRed(),data[i].c.getGreen(),data[i].c.getBlue(),
                            data[i].imageUrl) + (char)27 + "[0mSampled Color:" + (char)27 + String.format("[48;2;%d;%d;%dm%s", 
                            c.getRed(),c.getGreen(),c.getBlue(),
                            data[i].imageUrl) + (char)27 + "[0mRelevancy: " + relevancy * 100 + "%";
                    }
                }

                return results;
            }
        };

        class A{
            static int time;
            static long startTime = System.currentTimeMillis();
            static int operations = 0;
            static int currentOperations = 0;
            static int timeSpentActive = 0;
            static int timeSpentInactive = 0;
            static double currentFitnessLevel = 0;
            static double fitnessLevel = 0;
            static String[] unsuccessfulG = new String[0];
            static double threadUsage = 0;
            static long timeSinceProgress = 0;
        }

        Thread a = new Thread(){
            public void run(){
                try{
                    while (true){
                        String a = "";
                        a += (char)27 + "[2J" + "\n";
                        a += String.format("---------------------------\nOperations Per Second: %s\t[%s]\nThreads (MultiThreaded): %s\nThread Usage: ", (int)((double)A.currentOperations/A.time*1000), (char)27 + "[107m" + " ".repeat((int)((double)A.currentOperations/A.time*1000)) + (char)27 + "[0m", numberOfThreads) + "\n";
                        
                        Color c = MusicVisualizer.lerp(new Color(0, 255, 85), new Color(255, 0, 85), (A.threadUsage));
                        a += String.format((char)27 + "[38;2;%d;%d;%dmMain Thread:%s%%" + (char)27 + "[0m,\n", c.getRed(), c.getGreen(), c.getBlue(), Math.round(A.threadUsage * 100));
                        for (int i = 0;i<numberOfThreads;i++){
                            double threadUsage = mt.getThreadUsage(i);
                            c = MusicVisualizer.lerp(new Color(0, 255, 85), new Color(255, 0, 85), threadUsage);
                            a += (String.format((char)27 + "[38;2;%d;%d;%dmThread%s:%s%%" + (char)27 + "[0m,\t", c.getRed(), c.getGreen(), c.getBlue(), i, Math.round(threadUsage * 100)));
                        }
                        String b = "No Progress Has Been Made";
                        if (A.timeSinceProgress != 0){
                            b = DateFormat.getDateTimeInstance().format(new Date(A.timeSinceProgress)) + "; " + (System.currentTimeMillis() - A.timeSinceProgress)/1000 + " seconds ago";
                        }
                        a += (String.format("\nMax Fitness Level: %s%% (%s)\nParameters:", A.currentFitnessLevel*100, b)) + "\n";
                        a += (String.format("relevancyLevel: %s\nsampleSize: %s\nvibranceLevel: %s\nignoranceOfCommonality: %s\nsmallSampleLevel: %s\ngreyScaleLevel: %s\nuniqueLevel: %s\nbrightnessLevel: %s\ndarknessLevel: %s", 
                        savedParameter.relevancyLevel, savedParameter.sampleSize, savedParameter.vibranceLevel, savedParameter.ignoranceOfCommonality, savedParameter.smallSampleLevel, savedParameter.greyScaleLevel, savedParameter.uniqueLevel, savedParameter.brightnessLevel, savedParameter.darknessLevel)) + "\n"; 
                        a += (String.format("\nCurrent Fitness Level: %s%%\nParameters:", A.fitnessLevel*100)) + "\n";
                        a += (String.format("relevancyLevel: %s\nsampleSize: %s\nvibranceLevel: %s\nignoranceOfCommonality: %s\nsmallSampleLevel: %s\ngreyScaleLevel: %s\nuniqueLevel: %s\nbrightnessLevel: %s\ndarknessLevel: %s", 
                        currentParameter.relevancyLevel, currentParameter.sampleSize, currentParameter.vibranceLevel, currentParameter.ignoranceOfCommonality, currentParameter.smallSampleLevel, currentParameter.greyScaleLevel, currentParameter.uniqueLevel, currentParameter.brightnessLevel, currentParameter.darknessLevel)) + "\n";
            
                        a += ("Non-matchings:\n") + "\n";
                        for (String s: A.unsuccessfulG){
                            if (s != null)
                                a += (s)  + "\n";
                        }
                        Thread.sleep(100);
                        System.out.println(a);
                    }
                }
                catch(Exception er){};
            }
        };
        a.start();

        boolean starting = true;

        while (true){
            int matching = 0;

            long startTimeA = System.currentTimeMillis();
            int unsuccesses = 0;
            String[] unsuccessful = new String[15];
            Object[] multiThreadResults = mt.start(null);


            for (Object object: multiThreadResults){
                Result[] results = (Result[])object;
                for (int i = 0;i<results.length;i++){
                    if (results[i].matched){
                        matching ++;
                    }else{
                        if (unsuccesses < 15)
                            unsuccessful[unsuccesses++] = results[i].reason;
                    }
                }
            }
            A.timeSpentActive += System.currentTimeMillis() - startTimeA;

            if (System.currentTimeMillis() - A.startTime > 25){
                A.time = (int)(System.currentTimeMillis() - A.startTime);
                A.startTime = System.currentTimeMillis();
                A.currentOperations = A.operations;
                A.operations = 0;
                A.threadUsage = (double)A.timeSpentActive/(A.timeSpentInactive + A.timeSpentActive);
                A.timeSpentActive = 0;
                A.timeSpentInactive = 0;
            }

            startTimeA = System.currentTimeMillis();

            A.operations ++;
            A.fitnessLevel = (double)matching/max;
            

            if (A.fitnessLevel > A.currentFitnessLevel){
                A.unsuccessfulG = unsuccessful;
                A.currentFitnessLevel = A.fitnessLevel;
                savedParameter.relevancyLevel = currentParameter.relevancyLevel;
                savedParameter.sampleSize = currentParameter.sampleSize;
                savedParameter.vibranceLevel = currentParameter.vibranceLevel;
                savedParameter.ignoranceOfCommonality = currentParameter.ignoranceOfCommonality;
                savedParameter.smallSampleLevel = currentParameter.smallSampleLevel;
                savedParameter.greyScaleLevel = currentParameter.greyScaleLevel;
                savedParameter.uniqueLevel = currentParameter.uniqueLevel;
                savedParameter.brightnessLevel = currentParameter.brightnessLevel;
                savedParameter.darknessLevel = currentParameter.darknessLevel;
                if (starting == false)
                    A.timeSinceProgress = System.currentTimeMillis();
                starting = false;
            }else{
                currentParameter.relevancyLevel = savedParameter.relevancyLevel;
                currentParameter.sampleSize = savedParameter.sampleSize;
                currentParameter.vibranceLevel = savedParameter.vibranceLevel;
                currentParameter.ignoranceOfCommonality = savedParameter.ignoranceOfCommonality;
                currentParameter.smallSampleLevel = savedParameter.smallSampleLevel;
                currentParameter.greyScaleLevel = savedParameter.greyScaleLevel;
                currentParameter.uniqueLevel = savedParameter.uniqueLevel;
                currentParameter.brightnessLevel = savedParameter.brightnessLevel;
                currentParameter.darknessLevel = savedParameter.darknessLevel;
            }
            currentParameter.relevancyLevel = Math.random();
            if (Math.random() > 0.25){
                currentParameter.sampleSize += (Math.random()*50-25);
                currentParameter.sampleSize = Math.max(50, Math.min(currentParameter.sampleSize, 600));
            }
            currentParameter.vibranceLevel += (Math.random() - 0.5) * 2;
            currentParameter.ignoranceOfCommonality += (Math.random() - 0.5) * 2;
            currentParameter.smallSampleLevel += (Math.random() - 0.5) * 2;
            currentParameter.greyScaleLevel += (Math.random() - 0.5) * 2;
            currentParameter.uniqueLevel += (Math.random() - 0.5) * 2;
            currentParameter.brightnessLevel += (Math.random() - 0.5) * 2;
            currentParameter.darknessLevel += (Math.random() - 0.5) * 2;
            A.timeSpentInactive += System.currentTimeMillis() - startTimeA;
        }

    }
}
