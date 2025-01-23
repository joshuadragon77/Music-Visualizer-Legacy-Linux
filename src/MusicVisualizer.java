import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.awt.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.util.LinkedList;
import java.awt.Taskbar;

import javax.sound.sampled.*;

import renderengine.engine.AnimationProvider;
import renderengine.engine.InputController;
import renderengine.engine.JWindow;
import renderengine.engine.RenderEngine;
import renderengine.engine.InputController.KeyboardDetails;
import renderengine.engine.InputController.KeyboardInputEvent;
import renderengine.engine.InputController.MouseDetails;
import renderengine.engine.InputController.MouseInputEvent;
import renderengine.engine.InputController.TextInputEvent;
import renderengine.engine.InputController.UIElementInputEvent;
import renderengine.engine.RenderEngine.InstructionEventHandler;
import renderengine.uielement.TextLabel;
import renderengine.uielement.Frame;
import renderengine.uielement.ImageLabel;
import renderengine.uielement.Structure;
import renderengine.uielement.TextBox;
import renderengine.uielement.TextButton;
import renderengine.uielement.UDim2;

public class MusicVisualizer {
    static final String fontFamily = "Source Code Pro for Powerline";
    

    static final String homeDirectory = System.getenv("HOME");

    static final String imageCacheDirectory = String.format("%s/.cache/jademusicvisualizercache/", homeDirectory);
    static final String lyricsDirectory = String.format("%s/Documents/Lyrics/", homeDirectory);

    static Image icon;

    private static AudioFormat af = new AudioFormat(192000.0f, 16, 1, true, true);

    private static Mixer.Info info = null;
    private static TargetDataLine dataLine = null;
    private static byte[] data = new byte[8192];

    public static void main(String[] args) throws Exception {
        //thank you to the team behind Project Lanai for their work!
        //not to be inconsiderate. i needed desparetly a way to make my app run faster and efficiently. 
        // System.setProperty("sun.java2d.metal", "true");

        class SpotifyVisualizerHandler{
            static Color primaryColor = new Color(29, 29, 29);
            static Color secondaryColor = new Color(29, 29, 29);
            static Color genericColor = new Color(29, 29, 29);
            static TextLabel primaryText;
            static TextLabel secondaryText;
            static ImageLabel backgroundImage;
            static TextLabel futureText;
            static TextLabel[] historicalLinesLabel;
            static Frame menu;
        }
    	
        SpotifyCoverCache.imageCacheLocation = imageCacheDirectory;

    	icon = ImageIO.read(MusicVisualizer.class.getResource("icon.png"));
    	
    	// Taskbar taskbar = Taskbar.getTaskbar();
    	// taskbar.setIconImage(icon);

        Color backgroundColor = new Color(10, 10, 29);
        Color primaryColor = new Color(0, 255, 255);

        Frame backgroundFrame = new Frame();
        backgroundFrame.name = "Main Background";
        backgroundFrame.setZIndex(-5);
        backgroundFrame.setBackgroundColor(new Color(10, 10, 29));
        backgroundFrame.setBackgroundTransparency(0f);
        backgroundFrame.setIndiciateOnHighlight(false);
        backgroundFrame.setSize(new UDim2(1.0, 1.0));
        Frame[] bars = new Frame[100];
        double[] barValues = new double[bars.length];
        int segments = bars.length;
        for (int i = 0; i < bars.length; i++) {
            Frame newFrame = new Frame();
            newFrame.name = "Visualizer Bar";
            newFrame.setVisibility(true);
            newFrame.setSize(new UDim2(1.0 / (segments), 0, 1, 0));
            newFrame.setPosition(new UDim2(1.0 / segments * i, 0, 0, 0));
            newFrame.setBackgroundColor(primaryColor);
            backgroundFrame.appendChild(newFrame);
            bars[i] = newFrame;
        }

        abstract class BarVisualizerTheme{
            String visualizerName;
            abstract void instructGraphics(long elapsed);
            abstract void initialize();
            abstract void close();
            static BarVisualizerTheme currentVisualizerTheme;
            BarVisualizerTheme(String visualizerName){
                this.visualizerName = visualizerName;
            }
        }

        BarVisualizerTheme[] visualizerThemes = new BarVisualizerTheme[]{
            new BarVisualizerTheme("Jadenarium Basic") {
                void initialize(){
                    backgroundFrame.setBackgroundColor(backgroundColor);
                    for (int i = 0;i<bars.length;i++){
                        bars[i].setBackgroundColor(primaryColor);
                    }
                }
                void instructGraphics(long elapsed){
                    for (int i = 0;i<bars.length;i++){
                        bars[i].setBackgroundColor(primaryColor);
                        bars[i].setSize(new UDim2(bars[i].getSize().xScale, 5,
                                bars[i].getSize().yScale + (barValues[i] - bars[i].getSize().yScale) / 8, 0));
                        bars[i].setPosition(new UDim2(bars[i].getPosition().xScale, 0, 1 - bars[i].getSize().yScale, 0));
                    }
                }
                void close(){};
            },
            new BarVisualizerTheme("Jadenarium") {
                Color color1 = new Color(0, 170, 255);
                Color color2 = new Color(0, 255, 255);

                Color tweenColor(Color colorFrom, Color colorTo, double lerp){
                    return new Color(
                        (int)(colorFrom.getRed() + (colorTo.getRed() - colorFrom.getRed()) * lerp),
                        (int)(colorFrom.getGreen() + (colorTo.getGreen() - colorFrom.getGreen()) * lerp),
                        (int)(colorFrom.getBlue() + (colorTo.getBlue() - colorFrom.getBlue()) * lerp)
                    );
                }

                void initialize(){
                    backgroundFrame.setBackgroundColor(backgroundColor);
                    for (int i = 0;i<bars.length;i++){
                        bars[i].setBackgroundColor(primaryColor);
                    }
                }
                void instructGraphics(long elapsed){
                    for (int i = 0;i<bars.length;i++){
                        bars[i].setBackgroundColor(primaryColor);
                        bars[i].setSize(new UDim2(bars[i].getSize().xScale, 5,
                                bars[i].getSize().yScale + (barValues[i] - bars[i].getSize().yScale) / 8, 0));
                        bars[i].setPosition(new UDim2(bars[i].getPosition().xScale, 0, 1 - bars[i].getSize().yScale, 0));
                        
                        double loudness = (double)bars[i].getSize().yScale;

                        bars[i].setBackgroundColor(tweenColor(backgroundColor, tweenColor(color1, color2, (double)Math.max(loudness-0.25, 0)/0.75f), (double)Math.min(loudness, 0.25)/0.25f));
                    }
                }
                void close(){};
            },
            new BarVisualizerTheme("Raw") {
                Color color1 = new Color(0, 170, 255);
                Color color2 = new Color(0, 255, 255);

                Color tweenColor(Color colorFrom, Color colorTo, double lerp){
                    return new Color(
                        (int)(colorFrom.getRed() + (colorTo.getRed() - colorFrom.getRed()) * lerp),
                        (int)(colorFrom.getGreen() + (colorTo.getGreen() - colorFrom.getGreen()) * lerp),
                        (int)(colorFrom.getBlue() + (colorTo.getBlue() - colorFrom.getBlue()) * lerp)
                    );
                }

                void initialize(){
                    backgroundFrame.setBackgroundColor(backgroundColor);
                    for (int i = 0;i<bars.length;i++){
                        bars[i].setBackgroundColor(primaryColor);
                    }
                }
                void instructGraphics(long elapsed){
                    for (int i = 0;i<bars.length;i++){
                        bars[i].setBackgroundColor(primaryColor);
                        bars[i].setSize(new UDim2(bars[i].getSize().xScale, 5,barValues[i] , 0));
                        bars[i].setPosition(new UDim2(bars[i].getPosition().xScale, 0, 1 - bars[i].getSize().yScale, 0));
                        
                        double loudness = (double)bars[i].getSize().yScale;

                        bars[i].setBackgroundColor(tweenColor(backgroundColor, tweenColor(color1, color2, (double)Math.max(loudness-0.25, 0)/0.75f), (double)Math.min(loudness, 0.25)/0.25f));
                    }
                }
                void close(){};
            },
            new BarVisualizerTheme("Skybox Visualizer Algorithm (Jadenarium)") {
                Color color1 = new Color(0, 170, 255);
                double mathClamp(double value, double min, double max){
                    return Math.min(Math.max(value, min), max);
                }

                double[] barsSensitity = new double[bars.length];

                void initialize(){
                    backgroundFrame.setBackgroundColor(backgroundColor);
                    for (int i = 0;i<bars.length;i++){
                        bars[i].setBackgroundColor(color1);
                    }
                    for (int i = 0;i<barsSensitity.length;i++)
                        barsSensitity[i] = (double)Math.random();
                }
                void instructGraphics(long elapsed){
                    double overallPercentage = 0;
                    for (int i = 0;i<bars.length;i+=10){
                        overallPercentage += barValues[i];
                    }
                    overallPercentage /= bars.length/10;

                    for (int i = 0;i<bars.length;i++){
                        double sensitivity = barsSensitity[i];
                        double loudness = mathClamp((1-(mathClamp(Math.abs(overallPercentage-sensitivity), 0, .2f)/.2f))*mathClamp(overallPercentage*2, 0, 1), 0, 1);//Math.sqrt((1-Math.min((Math.abs(sensitivity-overallPercentage)/0.25), 1))) * Math.sqrt(overallPercentage);

                        bars[i].setSize(new UDim2(bars[i].getSize().xScale, 5,
                                bars[i].getSize().yScale + (loudness - bars[i].getSize().yScale) / 4, 0));
                        bars[i].setPosition(new UDim2(bars[i].getPosition().xScale, 0, 1 - bars[i].getSize().yScale, 0));
                    }
                }
                void close(){};
            },
            new BarVisualizerTheme("Rainbow") {

                long startTime = System.currentTimeMillis();

                Color tweenColor(Color colorFrom, Color colorTo, double lerp){
                    return new Color(
                        (int)(colorFrom.getRed() + (colorTo.getRed() - colorFrom.getRed()) * lerp),
                        (int)(colorFrom.getGreen() + (colorTo.getGreen() - colorFrom.getGreen()) * lerp),
                        (int)(colorFrom.getBlue() + (colorTo.getBlue() - colorFrom.getBlue()) * lerp)
                    );
                }

                void initialize(){
                    backgroundFrame.setBackgroundColor(backgroundColor);
                    startTime = System.currentTimeMillis();
                }
                void instructGraphics(long elapsed){
                    long timeElapsed = System.currentTimeMillis()-startTime;

                    double timeFactor = timeElapsed/5000.0f;

                    int r = Math.max(0, (int)(255*Math.pow(Math.sin(2*Math.PI*(timeFactor+0.08333333333)), 1/2.0)));
                    int g = Math.max(0, (int)(255*Math.pow(Math.sin(2*Math.PI*(timeFactor-0.25)), 1/2.0)));
                    int b = Math.max(0, (int)(255*Math.pow(Math.sin(2*Math.PI*(timeFactor-0.5833333333)), 1/2.0)));

                    Color currentColor = new Color(r, g, b);

                    double overallPercentage = 0;

                    for (int i = 0;i<bars.length;i++){
                        bars[i].setSize(new UDim2(bars[i].getSize().xScale, 5,
                                bars[i].getSize().yScale + (barValues[i] - bars[i].getSize().yScale) / 8, 0));
                        bars[i].setPosition(new UDim2(bars[i].getPosition().xScale, 0, 1 - bars[i].getSize().yScale, 0));
                        bars[i].setBackgroundColor(tweenColor(backgroundColor, currentColor, (double)Math.pow(bars[i].getSize().yScale, 0.75f)));
                        overallPercentage += bars[i].getSize().yScale;
                    }
                    
                    overallPercentage /= bars.length;
                    backgroundFrame.setBackgroundColor(tweenColor(backgroundColor, currentColor, overallPercentage + (0 - overallPercentage) * 0.75f));
                }
                void close(){};
            },
            new BarVisualizerTheme("Burny") {

                Color color1 = new Color(255, 0, 0);
                Color color2 = new Color(255, 137, 27);

                Color tweenColor(Color colorFrom, Color colorTo, double lerp){
                    return new Color(
                        (int)(colorFrom.getRed() + (colorTo.getRed() - colorFrom.getRed()) * lerp),
                        (int)(colorFrom.getGreen() + (colorTo.getGreen() - colorFrom.getGreen()) * lerp),
                        (int)(colorFrom.getBlue() + (colorTo.getBlue() - colorFrom.getBlue()) * lerp)
                    );
                }

                void initialize(){
                    backgroundFrame.setBackgroundColor(backgroundColor);
                }
                void instructGraphics(long elapsed){
                    for (int i = 0;i<bars.length;i++){
                        bars[i].setSize(new UDim2(bars[i].getSize().xScale, 5,
                                bars[i].getSize().yScale + (barValues[i] - bars[i].getSize().yScale) / 8, 0));
                        bars[i].setPosition(new UDim2(bars[i].getPosition().xScale, 0, 1 - bars[i].getSize().yScale, 0));

                        double loudness = (double)bars[i].getSize().yScale;

                        bars[i].setBackgroundColor(tweenColor(backgroundColor, tweenColor(color1, color2, Math.max(loudness-0.25f, 0)/0.75f), Math.min(loudness, 0.25f)/0.25f));
                    }
                }
                void close(){};
            },
            new BarVisualizerTheme("White & Black") {
                Color color1 = new Color(29, 29, 29);
                Color color2 = new Color(255, 255, 255);

                void initialize(){
                    backgroundFrame.setBackgroundColor(color2);
                    for (int i = 0;i<bars.length;i++){
                        bars[i].setBackgroundColor(color1);
                    }
                }
                void instructGraphics(long elapsed){
                    for (int i = 0;i<bars.length;i++){
                        bars[i].setSize(new UDim2(bars[i].getSize().xScale, 5,
                                bars[i].getSize().yScale + (barValues[i] - bars[i].getSize().yScale) / 8, 0));
                        bars[i].setPosition(new UDim2(bars[i].getPosition().xScale, 0, 1 - bars[i].getSize().yScale, 0));
                    }
                }
                void close(){};
            },
            new BarVisualizerTheme("Basic Spotify") {


                void initialize(){
                    SpotifyVisualizerHandler.menu.setBackgroundTransparency(0.75f);
                }
                void instructGraphics(long elapsed){

                    Color secondaryColor = SpotifyVisualizerHandler.secondaryColor;
                    Color primaryColor = SpotifyVisualizerHandler.primaryColor;

                    if (SpotifyCoverCache.measureBrightness(primaryColor) < 0.1){
                        primaryColor = primaryColor.brighter();
                    }
                    if (SpotifyCoverCache.measureBrightness(secondaryColor) < 0.1){
                        secondaryColor = lerp(secondaryColor, primaryColor, 0.25);
                    }
                    if (SpotifyCoverCache.measureBrightness(secondaryColor) > 0.75){
                        secondaryColor = secondaryColor.darker();
                    }
                    if (SpotifyCoverCache.measureRelvance(secondaryColor, primaryColor) > 0.1){
                        //primaryColor = primaryColor.brighter().brighter();
                        secondaryColor = secondaryColor.darker().darker();
                    }

                    backgroundFrame.setBackgroundColor(secondaryColor.darker().darker().darker());
                    for (int i = 0;i<bars.length;i++){
                        bars[i].setBackgroundColor(secondaryColor);
                    }

                    SpotifyVisualizerHandler.menu.setBackgroundColor(secondaryColor.darker().darker().darker().darker());
                    
                    primaryColor = lerp(primaryColor, primaryColor.brighter().brighter(), (Math.sin(System.currentTimeMillis()/250.0)/2 + 0.5)*.5);

                    for (TextLabel a : SpotifyVisualizerHandler.historicalLinesLabel){
                        a.setMaterialColor(primaryColor);
                    }
                    SpotifyVisualizerHandler.primaryText.setMaterialColor(primaryColor);
                    SpotifyVisualizerHandler.futureText.setMaterialColor(primaryColor.darker().darker().darker().darker());
                    SpotifyVisualizerHandler.secondaryText.setMaterialColor(SpotifyVisualizerHandler.futureText.getMaterialColor());

                    for (int i = 0;i<bars.length;i++){
                        bars[i].setSize(new UDim2(bars[i].getSize().xScale, 5,
                                bars[i].getSize().yScale + (barValues[i] - bars[i].getSize().yScale) / 8, 0));
                        bars[i].setPosition(new UDim2(bars[i].getPosition().xScale, 0, 1 - bars[i].getSize().yScale, 0));
                    }
                }
                void close(){
                    for (TextLabel a : SpotifyVisualizerHandler.historicalLinesLabel){
                        a.setMaterialColor(new Color(255, 255, 255));
                    }
                    SpotifyVisualizerHandler.primaryText.setMaterialColor(new Color(255, 255, 255));
                    SpotifyVisualizerHandler.futureText.setMaterialColor(new Color(75, 75, 75));
                    SpotifyVisualizerHandler.secondaryText.setMaterialColor(new Color(75, 75, 75));
                    SpotifyVisualizerHandler.menu.setBackgroundColor(new Color(0, 0, 0));
                    SpotifyVisualizerHandler.menu.setBackgroundTransparency(0.25f);
                };
            },
            new BarVisualizerTheme("Spotify Lyrics Like") {

                double previousLoudness = 0;

                void initialize(){
                    SpotifyVisualizerHandler.menu.setBackgroundTransparency(0.5f);
                    for (int i = 0;i<bars.length;i++){
                        bars[i].setBackgroundTransparency(1f);
                    }
                }
                void instructGraphics(long elapsed){

                    Color genericColor = SpotifyVisualizerHandler.primaryColor;
                    if (SpotifyCoverCache.measureBrightness(genericColor) > 0.5){
                        genericColor = genericColor.darker().darker();
                    }

                    double loudness = 0;
                    for (int i = 0;i<bars.length;i++){
                        loudness += barValues[i];
                    }
                    loudness /= bars.length;

                    previousLoudness = previousLoudness + (loudness - previousLoudness)*(Math.pow(loudness, 4)+.25)/1.25;

                    genericColor = lerp(genericColor, genericColor.darker().darker().darker().darker(), 1-previousLoudness);

                    backgroundFrame.setBackgroundColor(genericColor.darker().darker());

                    SpotifyVisualizerHandler.menu.setBackgroundColor(genericColor.darker().darker().darker());
                    

                    SpotifyVisualizerHandler.futureText.setMaterialColor(genericColor.brighter());
                    SpotifyVisualizerHandler.secondaryText.setMaterialColor(genericColor.brighter().brighter());



                }
                void close(){
                    for (int i = 0;i<bars.length;i++){
                        bars[i].setBackgroundTransparency(0f);
                    }
                    SpotifyVisualizerHandler.secondaryText.setMaterialColor(new Color(75, 75, 75));
                    SpotifyVisualizerHandler.menu.setBackgroundColor(new Color(0, 0, 0));
                    SpotifyVisualizerHandler.menu.setBackgroundTransparency(0.25f);
                    SpotifyVisualizerHandler.futureText.setMaterialColor(new Color(75, 75, 75));
                };
            },
            new BarVisualizerTheme("Spotify Lyrics + Visualizer") {

                double previousLoudness = 0;

                void initialize(){
                    SpotifyVisualizerHandler.menu.setBackgroundTransparency(0.5f);
                }
                void instructGraphics(long elapsed){

                    Color genericColor = SpotifyVisualizerHandler.primaryColor;
                    Color primaryColor = SpotifyVisualizerHandler.primaryColor;

                    if (SpotifyVisualizerHandler.primaryText.getTransparency() < 0.9){
                        if (SpotifyCoverCache.measureBrightness(genericColor) > 0.5){
                            genericColor = genericColor.darker().darker();
                        }

                        double loudness = 0;
                        for (int i = 0;i<bars.length;i++){
                            loudness += barValues[i];
                        }
                        loudness /= bars.length;

                        previousLoudness = previousLoudness + (loudness - previousLoudness)*(Math.pow(loudness, 4)+.25)/1.25;

                        genericColor = lerp(genericColor, genericColor.darker().darker().darker().darker(), 1-previousLoudness);


                        SpotifyVisualizerHandler.menu.setBackgroundColor(genericColor.darker().darker().darker());
                        

                        SpotifyVisualizerHandler.futureText.setMaterialColor(genericColor.brighter());
                        SpotifyVisualizerHandler.secondaryText.setMaterialColor(genericColor.brighter().brighter());
                    }
                    SpotifyVisualizerHandler.backgroundImage.setTransparency(0.8f + 0.2f * SpotifyVisualizerHandler.primaryText.getTransparency());
                    backgroundFrame.setBackgroundColor(lerp(genericColor.darker().darker(), primaryColor.darker().darker(), SpotifyVisualizerHandler.primaryText.getTransparency()));
                    for (int i = 0;i<bars.length;i++){
                        bars[i].setBackgroundTransparency((1 - SpotifyVisualizerHandler.primaryText.getTransparency()));
                        bars[i].setBackgroundColor(primaryColor);
                    }
                    if (SpotifyVisualizerHandler.primaryText.getTransparency() > 0){
    
                        for (int i = 0;i<bars.length;i++){
                            bars[i].setSize(new UDim2(bars[i].getSize().xScale, 1,
                                    bars[i].getSize().yScale + (barValues[i] - bars[i].getSize().yScale) / 8, 0));
                            bars[i].setPosition(new UDim2(bars[i].getPosition().xScale, 0, 1 - bars[i].getSize().yScale, 0));
                        }
                    }


                }
                void close(){
                    for (int i = 0;i<bars.length;i++){
                        bars[i].setBackgroundTransparency(0f);
                    }
                    SpotifyVisualizerHandler.backgroundImage.setTransparency(0.9f);
                    SpotifyVisualizerHandler.secondaryText.setMaterialColor(new Color(75, 75, 75));
                    SpotifyVisualizerHandler.menu.setBackgroundColor(new Color(0, 0, 0));
                    SpotifyVisualizerHandler.menu.setBackgroundTransparency(0.25f);
                    SpotifyVisualizerHandler.futureText.setMaterialColor(new Color(75, 75, 75));
                    SpotifyVisualizerHandler.menu.setBackgroundColor(new Color(0, 0, 0));
                    SpotifyVisualizerHandler.menu.setBackgroundTransparency(0.25f);
                    SpotifyVisualizerHandler.backgroundImage.setBackgroundTransparency(0.9f);
                };
            },
            new BarVisualizerTheme("Spotify Visualizer") {

                void initialize(){
                    SpotifyVisualizerHandler.menu.setBackgroundTransparency(0.5f);
                    for (int i = 0;i<SpotifyVisualizerHandler.historicalLinesLabel.length;i++){
                        SpotifyVisualizerHandler.historicalLinesLabel[i].setVisibility(false);
                    }
                    SpotifyVisualizerHandler.futureText.setVisibility(false);
                    SpotifyVisualizerHandler.primaryText.setVisibility(false);
                    SpotifyVisualizerHandler.secondaryText.setVisibility(false);
                }
                void instructGraphics(long elapsed){

                    Color primaryColor = SpotifyVisualizerHandler.primaryColor;

                    backgroundFrame.setBackgroundColor(primaryColor.darker().darker().darker());
                    for (int i = 0;i<bars.length;i++){
                        bars[i].setBackgroundColor(primaryColor);
                    }

                    SpotifyVisualizerHandler.menu.setBackgroundColor(primaryColor.darker().darker().darker().darker());

                    for (int i = 0;i<bars.length;i++){
                        bars[i].setSize(new UDim2(bars[i].getSize().xScale, 5,
                                bars[i].getSize().yScale + (barValues[i] - bars[i].getSize().yScale) / 8, 0));
                        bars[i].setPosition(new UDim2(bars[i].getPosition().xScale, 0, 1 - bars[i].getSize().yScale, 0));
                    }

                }
                void close(){
                    for (int i = 0;i<SpotifyVisualizerHandler.historicalLinesLabel.length;i++){
                        SpotifyVisualizerHandler.historicalLinesLabel[i].setVisibility(true);
                    }
                    SpotifyVisualizerHandler.futureText.setVisibility(true);
                    SpotifyVisualizerHandler.primaryText.setVisibility(true);
                    SpotifyVisualizerHandler.secondaryText.setVisibility(true);
                    SpotifyVisualizerHandler.menu.setBackgroundColor(new Color(0, 0, 0));
                    SpotifyVisualizerHandler.menu.setBackgroundTransparency(0.25f);
                };
            },
        };

        BarVisualizerTheme.currentVisualizerTheme = visualizerThemes[0];
        
        JWindow window = new JWindow("Music Visualizer") {
            /**
             *
             */
            private static final long serialVersionUID = 3525644686069851288L;

            public void instructGraphics(long elapsed) {
                BarVisualizerTheme.currentVisualizerTheme.instructGraphics(elapsed);
            }
        };
        window.setIconImage(icon);

        InputController ic = window.getInputController();
        RenderEngine re = window.getRenderingEngine();
        AnimationProvider ap = re.getAnimationProvider();
        re.setStructure(backgroundFrame);
        re.setFPS(240);

        re.setEventClockSpeed(8);
        re.setAutomaticFPSAdjust(true);
        re.enableOptimizationAlgorithm(false);
        window.setDebugDisplayMode(JWindow.debugMenu_disabled);
        
        Frame menu = new Frame();
        menu.name = "Menu";
        menu.setSize(new UDim2(1.0, 1.0));
        menu.setBackgroundColor(new Color(0, 0, 0));
        menu.setBackgroundTransparency(0.25f);

        SpotifyVisualizerHandler.menu = menu;

        Frame generalSettings = new Frame();
        generalSettings.name = "General Settings";
        generalSettings.setBackgroundTransparency(1f);
        generalSettings.setSize(new UDim2(1.0, 0, 1.0, -100));
        generalSettings.setPosition(new UDim2(0, 100));
        generalSettings.setParent(menu);

        Frame lyricsSetting = new Frame();
        lyricsSetting.name = "Lyrics Setting";
        lyricsSetting.setBackgroundTransparency(1f);
        lyricsSetting.setSize(new UDim2(1.0, 0, 1.0, -100));
        lyricsSetting.setPosition(new UDim2(1.0, 0, 0, 100));
        lyricsSetting.setParent(menu);

        Frame vibe = new Frame();
        vibe.name = "Vibe";
        vibe.setBackgroundTransparency(1f);
        vibe.setSize(new UDim2(1.0, 0, 1.0, -100));
        vibe.setPosition(new UDim2(2.0, 0, 0, 100));
        vibe.setParent(menu);

        Frame lyricsMaker = new Frame();
        lyricsMaker.name = "Lyrics Maker";
        lyricsMaker.setBackgroundTransparency(1f);
        lyricsMaker.setSize(new UDim2(1.0, 0, 1.0, -100));
        lyricsMaker.setPosition(new UDim2(2.0, 0, 0, 100));
        lyricsMaker.setParent(menu);

        TextBox clockSpeed;
        TextBox frameRate;
        TextBox sampleSize;
        TextButton spotifyPlaybackFix;


        class Input1 implements TextInputEvent, UIElementInputEvent{
            static int sampleSizeInt = 1024;
            static boolean fixSpotifyPlayback = true;

			@Override
			public void onFocus() {
				
				
			}

			@Override
			public void focusLost(boolean onEnter){}

            @Override
            public void onMouseButtonEvent(MouseDetails arg0) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void onMouseMovedEvent(MouseDetails arg0) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void mouseEnter(MouseDetails arg0) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void mouseLeave(MouseDetails arg0) {
                // TODO Auto-generated method stub
                
            }
        	
        }
        class Input2 implements UIElementInputEvent{
            static boolean openedMenu = true;

            @Override
            public void onMouseButtonEvent(MouseDetails arg0) {
                
                
            }

            @Override
            public void onMouseMovedEvent(MouseDetails arg0) {
                
                
            }

            @Override
            public void mouseEnter(MouseDetails arg0) {
                
                
            }

            @Override
            public void mouseLeave(MouseDetails arg0) {
                
                
            }
        	
        }

        
        TextButton menuButton = new TextButton();
        menuButton.name = "Visualizer General Menu Toggle";
        menuButton.setText("🅧");
        menuButton.setSize(new UDim2(0, 75, 0, 35));
        menuButton.setPosition(new UDim2(0, 0, 0, 0));
        menuButton.setParent(backgroundFrame);
        menuButton.setTextHorizontalAlignment(TextLabel.alignTextCenter);
        menuButton.setTextVerticalAlignment(TextLabel.alignTextCenter);
        menuButton.setZIndex(25);
        menuButton.setBackgroundTransparency(1f);
        menuButton.setBackgroundColor(new Color(29, 29, 29));

        ic.appendUIElementListener(menuButton, new Input2(){
            public void onMouseButtonEvent(MouseDetails arg0){
                if (arg0.isButtonDown() == false)
                    return;
                Input2.openedMenu = Input2.openedMenu != true;
                if (Input2.openedMenu){
                    menuButton.setText("🅧");
                    ap.tweenElementPosition(menu, new UDim2(0.0, 0.0), 0.25f, AnimationProvider.transition1_idk);
                }else{
                    menuButton.setText("● ● ●");
                    ap.tweenElementPosition(menu, new UDim2(0.0, 1.0), 0.25f, AnimationProvider.transition1_idk);
                }
            };
            public void mouseEnter(MouseDetails arg0) {
                menuButton.setBackgroundTransparency(0.5f);
            }
            public void mouseLeave(MouseDetails arg0) {
                menuButton.setBackgroundTransparency(1f);
            }
        });

        {
            TextLabel tl = new TextLabel();
            tl.name = "Title";
            tl.setText("TheJades's Music Visualizer");
            tl.setSize(new UDim2(0, 1000, 0, 100));
            tl.setPosition(new UDim2(0.5, -500, 0, 0));
            tl.setBackgroundTransparency(1);
            tl.setFont(new Font(fontFamily, Font.BOLD, 40));
            tl.setTextHorizontalAlignment(TextLabel.alignTextCenter);
            tl.setTextVerticalAlignment(TextLabel.alignTextCenter);
            tl.setZIndex(2);
            tl.setParent(menu);

            tl = tl.clone();
            tl.setText("Select Audio Input");
            tl.setFont(new Font(fontFamily, Font.BOLD, 30));
            tl.setPosition(new UDim2(0, 245));
            tl.setSize(new UDim2(0.5, 0, 0, 35));
            tl.setParent(generalSettings);

            tl = tl.clone();
            tl.setText("Select Visualizer Theme");
            tl.setPosition(new UDim2(0.5, 0, 0, 245));
            tl.setParent(generalSettings);

            tl = tl.clone();
            tl.setText("Select Lyrics");
            tl.setPosition(new UDim2(0.25, 0, 0, 55));
            tl.setParent(lyricsSetting);

        }
        {

            Frame selectionFrame = new Frame();
            selectionFrame.setPosition(new UDim2(0.1, 0, 0, 145));
            selectionFrame.setSize(new UDim2(0.2, 0, 0, 5));
            selectionFrame.setBackgroundColor(new Color(255, 255, 255));
            selectionFrame.setZIndex(2);

            TextButton tb = new TextButton();
            tb.name = "Menu Switch Button";
            tb.setText("General Settings");
            tb.setSize(new UDim2(0.2, 0, 0, 50));
            tb.setPosition(new UDim2(0.1, 0, 0, 100));
            tb.setBackgroundColor(new Color(29, 29, 29));
            tb.setTextHorizontalAlignment(TextLabel.alignTextCenter);
            tb.setTextVerticalAlignment(TextLabel.alignTextCenter);
            tb.setZIndex(2);
            tb.setParent(menu);

            ic.appendUIElementListener(tb, new Input2(){
                public void onMouseButtonEvent(MouseDetails args0){
                    if (args0.isButtonDown()){
                        ap.tweenElementPosition(selectionFrame, new UDim2(0.1, 0, 0, 145), 0.25f, AnimationProvider.transition1_idk);
                        ap.tweenElementPosition(generalSettings, new UDim2(0.0, 0, 0, 100), 0.25f, AnimationProvider.transition1_idk);
                        ap.tweenElementPosition(lyricsSetting, new UDim2(1.0, 0, 0, 100), 0.25f, AnimationProvider.transition1_idk);
                        ap.tweenElementPosition(vibe, new UDim2(2.0, 0, 0, 100), 0.25f, AnimationProvider.transition1_idk);
                        ap.tweenElementPosition(lyricsMaker, new UDim2(3.0, 0, 0, 100), 0.25f, AnimationProvider.transition1_idk);
                    }
                };
            });

            tb = tb.clone();
            tb.setText("Lyrics Settings");
            tb.setPosition(new UDim2(0.3, 0, 0, 100));
            tb.setParent(menu);

            selectionFrame.setParent(menu);

            ic.appendUIElementListener(tb, new Input2(){
                public void onMouseButtonEvent(MouseDetails args0){
                    if (args0.isButtonDown()){
                        ap.tweenElementPosition(selectionFrame, new UDim2(0.3, 0, 0, 145), 0.25f, AnimationProvider.transition1_idk);
                        ap.tweenElementPosition(generalSettings, new UDim2(-1.0, 0, 0, 100), 0.25f, AnimationProvider.transition1_idk);
                        ap.tweenElementPosition(lyricsSetting, new UDim2(0.0, 0, 0, 100), 0.25f, AnimationProvider.transition1_idk);
                        ap.tweenElementPosition(vibe, new UDim2(0.0, 0, 0, 100), 0.25f, AnimationProvider.transition1_idk);
                        ap.tweenElementPosition(lyricsMaker, new UDim2(2.0, 0, 0, 100), 0.25f, AnimationProvider.transition1_idk);
                    }
                };
            });


            tb = tb.clone();
            tb.setText("Vibe");
            tb.setPosition(new UDim2(0.5, 0, 0, 100));
            tb.setParent(menu);

            selectionFrame.setParent(menu);

            ic.appendUIElementListener(tb, new Input2(){
                public void onMouseButtonEvent(MouseDetails args0){
                    if (args0.isButtonDown()){
                        ap.tweenElementPosition(selectionFrame, new UDim2(0.5, 0, 0, 145), 0.25f, AnimationProvider.transition1_idk);
                        ap.tweenElementPosition(generalSettings, new UDim2(-2.0, 0, 0, 100), 0.25f, AnimationProvider.transition1_idk);
                        ap.tweenElementPosition(lyricsSetting, new UDim2(-1.0, 0, 0, 100), 0.25f, AnimationProvider.transition1_idk);
                        ap.tweenElementPosition(vibe, new UDim2(0.0, 0, 0, 100), 0.25f, AnimationProvider.transition1_idk);
                        ap.tweenElementPosition(lyricsMaker, new UDim2(1.0, 0, 0, 100), 0.25f, AnimationProvider.transition1_idk);
                    }
                };
            });


            tb = tb.clone();
            tb.setText("Lyrics Maker");
            tb.setPosition(new UDim2(0.7, 0, 0, 100));
            tb.setParent(menu);

            selectionFrame.setParent(menu);

            ic.appendUIElementListener(tb, new Input2(){
                public void onMouseButtonEvent(MouseDetails args0){
                    if (args0.isButtonDown()){
                        ap.tweenElementPosition(selectionFrame, new UDim2(0.7, 0, 0, 145), 0.25f, AnimationProvider.transition1_idk);
                        ap.tweenElementPosition(generalSettings, new UDim2(-3.0, 0, 0, 100), 0.25f, AnimationProvider.transition1_idk);
                        ap.tweenElementPosition(lyricsSetting, new UDim2(-2.0, 0, 0, 100), 0.25f, AnimationProvider.transition1_idk);
                        ap.tweenElementPosition(vibe, new UDim2(0.0, 0, 0, 100), 0.25f, AnimationProvider.transition1_idk);
                        ap.tweenElementPosition(lyricsMaker, new UDim2(0.0, 0, 0, 100), 0.25f, AnimationProvider.transition1_idk);
                    }
                };
            });

        }
        {
            TextLabel tl = new TextLabel();
            tl.name = "Setting Name";
            tl.setParent(generalSettings);
            tl.setText("Animation Clock Speed (ms):");
            tl.setSize(new UDim2(0, 600, 0, 35));
            tl.setPosition(new UDim2(0.5, -450, 0, 100));
            tl.setFont(new Font(fontFamily, Font.PLAIN, 25));
            tl.setZIndex(2);
            tl.setBackgroundColor(new Color(10, 10, 29));

            clockSpeed = new TextBox();
            clockSpeed.name = "Clock Speed TextBox";
            clockSpeed.setParent(generalSettings);
            clockSpeed.setSize(new UDim2(0, 300, 0, 35));
            clockSpeed.setBackgroundColor(new Color(29, 29, 29));
            clockSpeed.setPosition(new UDim2(0.5, 150, 0, 100));
            clockSpeed.setFont(new Font(fontFamily, Font.PLAIN, 25));
            clockSpeed.setText("8");
            clockSpeed.setZIndex(2);
        }
        {
            TextLabel tl = new TextLabel();
            tl.name = "Setting Name";
            tl.setParent(generalSettings);
            tl.setText("Frame Rate (fps): ");
            tl.setSize(new UDim2(0, 600, 0, 35));
            tl.setPosition(new UDim2(0.5, -450, 0, 135));
            tl.setBackgroundColor(new Color(10, 10, 29));
            tl.setFont(new Font(fontFamily, Font.PLAIN, 25));
            tl.setZIndex(2);

            frameRate = new TextBox();
            frameRate.name = "Framerate TextBox";
            frameRate.setParent(generalSettings);
            frameRate.setSize(new UDim2(0, 300, 0, 35));
            frameRate.setBackgroundColor(new Color(29, 29, 29));
            frameRate.setPosition(new UDim2(0.5, 150, 0, 135));
            frameRate.setFont(new Font(fontFamily, Font.PLAIN, 25));
            frameRate.setText("60");
            frameRate.setZIndex(2);
        }
        {
            TextLabel tl = new TextLabel();
            tl.name = "Setting Name";
            tl.setParent(generalSettings);
            tl.setText("Sample Size (bitrate): ");
            tl.setSize(new UDim2(0, 600, 0, 35));
            tl.setPosition(new UDim2(0.5, -450, 0, 170));
            tl.setBackgroundColor(new Color(10, 10, 29));
            tl.setFont(new Font(fontFamily, Font.PLAIN, 25));
            tl.setZIndex(2);

            sampleSize = new TextBox();
            sampleSize.name = "SampleSize TextBox";
            sampleSize.setParent(generalSettings);
            sampleSize.setSize(new UDim2(0, 300, 0, 35));
            sampleSize.setBackgroundColor(new Color(29, 29, 29));
            sampleSize.setPosition(new UDim2(0.5, 150, 0, 170));
            sampleSize.setFont(new Font(fontFamily, Font.PLAIN, 25));
            sampleSize.setText("1024");
            sampleSize.setZIndex(2);
        }
        {
            TextLabel tl = new TextLabel();
            tl.name = "Setting Name";
            tl.setParent(generalSettings);
            tl.setText("Spotify Auto Playback Fix:");
            tl.setSize(new UDim2(0, 600, 0, 35));
            tl.setPosition(new UDim2(0.5, -450, 0, 205));
            tl.setBackgroundColor(new Color(10, 10, 29));
            tl.setFont(new Font(fontFamily, Font.PLAIN, 25));
            tl.setZIndex(2);

            spotifyPlaybackFix = new TextButton();
            spotifyPlaybackFix.name = "spotifyPlaybackFix TextBox";
            spotifyPlaybackFix.setParent(generalSettings);
            spotifyPlaybackFix.setSize(new UDim2(0, 300, 0, 35));
            spotifyPlaybackFix.setBackgroundColor(new Color(29, 29, 29));
            spotifyPlaybackFix.setPosition(new UDim2(0.5, 150, 0, 205));
            spotifyPlaybackFix.setFont(new Font(fontFamily, Font.PLAIN, 25));
            spotifyPlaybackFix.setText("Refresh Playback");
            spotifyPlaybackFix.setZIndex(2);
        }
        
        
        ic.appendTextBoxListener(frameRate, new Input1(){
            public void focusLost(boolean onEnter) {
				
				if (onEnter) {
					int time = Integer.parseInt(frameRate.getText());
					re.setFPS(time);
				}else {
					frameRate.setText(re.getFPS() + "");
				}
			}
        });
        
        ic.appendTextBoxListener(clockSpeed, new Input1(){
            public void focusLost(boolean onEnter) {
				
				if (onEnter) {
					int time = Integer.parseInt(clockSpeed.getText());
					re.setEventClockSpeed(time);
				}
			}
        });

        ic.appendTextBoxListener(sampleSize, new Input1(){
            public void focusLost(boolean onEnter) {
				
				if (onEnter) {
					int time = Integer.parseInt(sampleSize.getText());
					Input1.sampleSizeInt = time;
				}
			}
        });

        ic.appendUIElementListener(spotifyPlaybackFix, new Input1(){
            public void onMouseButtonEvent(MouseDetails event) {
				if (event.isButtonDown() == false)
                    return;

                fixSpotifyPlayback = fixSpotifyPlayback != true;

                if (fixSpotifyPlayback){
                    spotifyPlaybackFix.setText("Refresh Playback");
                }else{
                    spotifyPlaybackFix.setText("Guess Playback");
                }
			}
        });

        class VisualizerThemeInput implements UIElementInputEvent{

            BarVisualizerTheme visualizer;
            TextButton textButton;
            static TextButton previousSelectedTextButton = null;

            @Override
            public void onMouseButtonEvent(MouseDetails arg0) {
                
                if (arg0.isButtonDown() == false)
                    return;
                if (previousSelectedTextButton != null){
                    previousSelectedTextButton.setBackgroundColor(new Color(29, 29, 29));
                    previousSelectedTextButton.setMaterialColor(new Color(255, 255, 255));
                }
                textButton.setBackgroundColor(new Color(255, 255, 255));
                textButton.setMaterialColor(new Color(29, 29, 29));
                previousSelectedTextButton = textButton;

                if (BarVisualizerTheme.currentVisualizerTheme != null)
                    BarVisualizerTheme.currentVisualizerTheme.close();
                visualizer.initialize();
                BarVisualizerTheme.currentVisualizerTheme = visualizer;
            }

            @Override
            public void onMouseMovedEvent(MouseDetails arg0) {
                
                
            }

            @Override
            public void mouseEnter(MouseDetails arg0) {
                
                
            }

            @Override
            public void mouseLeave(MouseDetails arg0) {
                
                
            }
            
        }

        {
            for (int i = 0;i<visualizerThemes.length;i++){
                BarVisualizerTheme theme = visualizerThemes[i];

                TextButton tb = new TextButton();
                tb.name = "Visualizer Theme Selection Button";
                tb.setText(theme.visualizerName);
                tb.setPosition(new UDim2(0.75 - 0.4/2, 0, 0, 25*i + 285));
                tb.setBackgroundColor(new Color(29, 29, 29));
                tb.setSize(new UDim2(0.4, 0, 0, 25));
                tb.setFont(new Font(fontFamily, Font.PLAIN, 20));
                tb.setZIndex(2);

                if (i == 0){
                    tb.setBackgroundColor(new Color(255, 255, 255));
                    tb.setMaterialColor(new Color(29, 29, 29));
                    VisualizerThemeInput.previousSelectedTextButton = tb;
                }

                VisualizerThemeInput input = new VisualizerThemeInput();
                input.visualizer = theme;
                input.textButton = tb;
                ic.appendUIElementListener(tb, input);

                tb.setParent(generalSettings);
            }
        }
        {
            class TextButtonInput implements UIElementInputEvent{
                Mixer.Info selectedInfo = null;
                TextButton textButton = null;
                static TextButton previousSelectedTextButton = null;
                @Override
                public void onMouseButtonEvent(MouseDetails eventInfo) {
                    
                    if (eventInfo.isButtonDown()){
                        info = selectedInfo;
    
                        try{
                            dataLine = AudioSystem.getTargetDataLine(af, info);
                            dataLine.open();
                            dataLine.start();
                            data = new byte[8192];
    
                            if (previousSelectedTextButton != null){
                                previousSelectedTextButton.setBackgroundColor(new Color(29, 29, 29));
                                previousSelectedTextButton.setMaterialColor(new Color(255, 255, 255));
                            }
                            textButton.setBackgroundColor(new Color(255, 255, 255));
                            textButton.setMaterialColor(new Color(29, 29, 29));
                            previousSelectedTextButton = textButton;
                        }
                        catch(Exception err){}
                    }
                }
    
                @Override
                public void onMouseMovedEvent(MouseDetails eventInfo){}
    
                @Override
                public void mouseEnter(MouseDetails eventInfo){}
    
                @Override
                public void mouseLeave(MouseDetails eventInfo){}
                
            }

            int i = 0;
            for (Mixer.Info v : AudioSystem.getMixerInfo()) {
                TextButton tb = new TextButton();
                tb.name = "Audio System Selection Button";
                tb.setText(v.getName() + ":" + v.getDescription());
                tb.setPosition(new UDim2(0.25 - 0.4/2, 0, 0, 25*i++ + 285));
                tb.setBackgroundColor(new Color(29, 29, 29));
                tb.setSize(new UDim2(0.4, 0, 0, 25));
                tb.setFont(new Font(fontFamily, Font.PLAIN, 20));
                tb.setZIndex(2);

                TextButtonInput input = new TextButtonInput();
                input.selectedInfo = v;
                input.textButton = tb;
                ic.appendUIElementListener(tb, input);

                tb.setParent(generalSettings);
                
            }
        }

        Frame timePositionBackground = new Frame();
        timePositionBackground.name = "Time Position Background";
        timePositionBackground.setSize(new UDim2(0.75, 0, 0, 25));
        timePositionBackground.setBackgroundColor(new Color(29, 29, 29));
        timePositionBackground.setPosition(new UDim2(0.125, 0, 1, -75));
        timePositionBackground.setParent(vibe);
        timePositionBackground.setZIndex(30);

        TextButton timePositionBackgroundHover = new TextButton();
        timePositionBackgroundHover.name = "Time Progress Bar All";
        timePositionBackgroundHover.setSize(new UDim2(1.0, 1.0));
        timePositionBackgroundHover.setBackgroundTransparency(1f);
        timePositionBackgroundHover.setZIndex(35);
        timePositionBackgroundHover.setParent(timePositionBackground);

        Frame timePositionBar = new Frame();
        timePositionBar.name = "Time Position Bar";
        timePositionBar.setBackgroundColor(new Color(127, 127, 127));
        timePositionBar.setSize(new UDim2(0, 1.0));
        timePositionBar.setParent(timePositionBackground);
        timePositionBar.setZIndex(30);
        
        Frame templateTick = new Frame();
        templateTick.name = "Jade Lyrics Tick Element";
        templateTick.setBackgroundColor(new Color(255, 255, 255));
        templateTick.setBackgroundTransparency(0.5f);
        templateTick.setSize(new UDim2(0, 1, 0.75, 0));
        templateTick.setPosition(new UDim2(0.125, 0));
        templateTick.setZIndex(30);

        ImageLabel artwork = new ImageLabel();
        artwork.name = "Artwork";
        artwork.setPosition(new UDim2(0.875, -640/5, 1, -135));
        artwork.setSize(new UDim2(640/5, 640/5));
        artwork.setBorderColor(new Color(29, 29, 29));
        artwork.setBorderThickness(10);
        artwork.setZIndex(30);
        artwork.setParent(vibe);

        TextButton artworkButton = new TextButton();
        artworkButton.name = "Spotify Song Button";
        artworkButton.setBackgroundColor(new Color(29, 29, 29));
        artworkButton.setBackgroundTransparency(1f);
        artworkButton.setSize(new UDim2(1.0, 1.0));
        artworkButton.setZIndex(30);
        artworkButton.setIndiciateOnHighlight(false);
        artworkButton.setParent(artwork);

        TextLabel status = new TextLabel();
        status.name = "Status";
        status.setText("0:00 / 0:00 - No Lyrics Playing");
        status.setPosition(new UDim2(0.125, 325, 1, -135));
        status.setBackgroundTransparency(1f);
        status.setSize(new UDim2(0.75, -325, 0, 50));
        status.setFont(new Font(fontFamily, Font.PLAIN, 15));
        status.setTextHorizontalAlignment(TextLabel.alignTextRight);
        status.setTextWrap(true);
        status.setZIndex(30);
        status.setParent(vibe);

        TextLabel secondaryText = new TextLabel();
        secondaryText.name = "Current Complete Lyrical Element";
        secondaryText.setBackgroundTransparency(1f);
        secondaryText.setSize(new UDim2(0.8, 0, 0, 100));
        secondaryText.setText("");
        secondaryText.setMaterialColor(new Color(75, 75, 75));
        secondaryText.setPosition(new UDim2(0.1, 0, 1, -300));
        secondaryText.setFont(new Font(fontFamily, Font.BOLD, 35));
        secondaryText.setTextWrap(true);

        TextLabel primaryText = new TextLabel();
        primaryText.name = "Current Lyrical Element";
        primaryText.setBackgroundTransparency(1f);
        primaryText.setSize(new UDim2(0.8, 0, 0, 100));
        primaryText.setText("");
        primaryText.setPosition(new UDim2(0.1, 0, 1, -300));
        primaryText.setFont(new Font(fontFamily, Font.BOLD, 35));
        primaryText.setTextWrap(true);

        TextLabel futureText = new TextLabel();
        futureText.name = "Future Lyrical Element";
        futureText.setBackgroundTransparency(1f);
        futureText.setSize(new UDim2(0.8, 0, 0, 50));
        futureText.setText("");
        futureText.setPosition(new UDim2(0.1, 0, 1, -200));
        futureText.setMaterialColor(new Color(75, 75, 75));
        futureText.setFont(new Font(fontFamily, Font.BOLD, 25));

        Frame scrollLyrics = new Frame();
        scrollLyrics.name = "Past Lyrical Frame";
        scrollLyrics.setSize(new UDim2(0.8, 0, 0, 600));
        scrollLyrics.setPosition(new UDim2(0.1, 0, 1, -900));
        scrollLyrics.setBackgroundTransparency(1f);

        TextLabel historicalLyrics = new TextLabel();
        historicalLyrics.name = "Past Lyrical Element";
        historicalLyrics.setBackgroundTransparency(1);
        historicalLyrics.setSize(new UDim2(1, 0, 0, 80));
        historicalLyrics.setText("");
        historicalLyrics.setBackgroundTransparency(1f);
        historicalLyrics.setPosition(new UDim2(0, 0, 1, -25));
        historicalLyrics.setFont(new Font(fontFamily, Font.PLAIN, 20));

        TextButton restartButton = new TextButton();
        restartButton.name = "Restart Button";
        restartButton.setText("◀◀");
        restartButton.setPosition(new UDim2(0.125, 0, 1, -135));
        restartButton.setBackgroundColor(new Color(29, 29, 29));
        restartButton.setSize(new UDim2(0, 25, 0, 25));
        restartButton.setFont(new Font(fontFamily, Font.PLAIN, 20));
        restartButton.setTextHorizontalAlignment(TextLabel.alignTextCenter);
        restartButton.setTextVerticalAlignment(TextLabel.alignTextCenter);
        restartButton.setParent(vibe);
        restartButton.setZIndex(2);

        TextButton pauseButton = new TextButton();
        pauseButton.name = "Pause Button";
        pauseButton.setText("▶");
        pauseButton.setPosition(new UDim2(0.125, 25, 1, -135));
        pauseButton.setBackgroundColor(new Color(29, 29, 29));
        pauseButton.setSize(new UDim2(0, 25, 0, 25));
        pauseButton.setTextHorizontalAlignment(TextLabel.alignTextCenter);
        pauseButton.setTextVerticalAlignment(TextLabel.alignTextCenter);
        pauseButton.setFont(new Font(fontFamily, Font.PLAIN, 20));
        pauseButton.setParent(vibe);
        pauseButton.setZIndex(30);

        TextButton nextTrack = new TextButton();
        nextTrack.name = "Next Track";
        nextTrack.setText("▶▶");
        nextTrack.setPosition(new UDim2(0.125, 50, 1, -135));
        nextTrack.setBackgroundColor(new Color(29, 29, 29));
        nextTrack.setSize(new UDim2(0, 25, 0, 25));
        nextTrack.setTextHorizontalAlignment(TextLabel.alignTextCenter);
        nextTrack.setTextVerticalAlignment(TextLabel.alignTextCenter);
        nextTrack.setFont(new Font(fontFamily, Font.PLAIN, 20));
        nextTrack.setParent(vibe);
        nextTrack.setZIndex(30);

        TextButton spotfiyIntegrate = new TextButton();
        spotfiyIntegrate.name = "Spotify Integrate Button";
        spotfiyIntegrate.setText("🔇 Spotify Disconnected");
        spotfiyIntegrate.setPosition(new UDim2(0.125, 75, 1, -135));
        spotfiyIntegrate.setBackgroundColor(new Color(29, 29, 29));
        spotfiyIntegrate.setSize(new UDim2(0, 250, 0, 25));
        spotfiyIntegrate.setTextHorizontalAlignment(TextLabel.alignTextCenter);
        spotfiyIntegrate.setTextVerticalAlignment(TextLabel.alignTextCenter);
        spotfiyIntegrate.setFont(new Font(fontFamily, Font.PLAIN, 15));
        spotfiyIntegrate.setParent(vibe);
        spotfiyIntegrate.setZIndex(30);


        TextLabel songPlayingNotification = new TextLabel();
        songPlayingNotification.name = "Song Notification";
        songPlayingNotification.setBackgroundTransparency(1f);
        songPlayingNotification.setText("");
        songPlayingNotification.setTextVerticalAlignment(TextLabel.alignTextUp);
        songPlayingNotification.setTextHorizontalAlignment(TextLabel.alignTextRight);
        songPlayingNotification.setSize(new UDim2(0, 400, 0, 300));
        songPlayingNotification.setFont(new Font(fontFamily, Font.PLAIN, 40));
        songPlayingNotification.setPosition(new UDim2(1, -450, 0, 25));
        songPlayingNotification.setTextWrap(true);
        songPlayingNotification.setParent(backgroundFrame);

        TextLabel songInfoNotification = songPlayingNotification.clone();
        songInfoNotification.name = "Song Notification Information";
        songInfoNotification.setFont(new Font(fontFamily, Font.PLAIN, 20));
        songInfoNotification.setBackgroundTransparency(1f);
        songInfoNotification.setText("");
        songInfoNotification.setSize(new UDim2(0, 500, 0, 300));
        songInfoNotification.setPosition(new UDim2(1, -550, 0, 70));
        songInfoNotification.setParent(backgroundFrame);

        TextLabel songTypeNotification = songPlayingNotification.clone();
        songTypeNotification.setFont(new Font(fontFamily, Font.PLAIN, 20));
        songTypeNotification.setBackgroundTransparency(1f);
        songTypeNotification.setText("");
        songTypeNotification.setSize(new UDim2(0, 500, 0, 300));
        songTypeNotification.setMaterialColor(new Color(255, 0, 85));
        songTypeNotification.setPosition(new UDim2(1, -550, 0, 125));
        songTypeNotification.setParent(backgroundFrame);
        songTypeNotification.name = "Jade Lyrics Supported Text";

        ImageLabel backgroundImage = new ImageLabel();
        backgroundImage.setSize(new UDim2(2.0, 2.0));
        backgroundImage.setTransparency(0.9f);
        backgroundImage.setBackgroundTransparency(1f);
        backgroundImage.setImageRenderMode(ImageLabel.image_Fill_Mode);
        backgroundImage.setZIndex(0);
        backgroundImage.setParent(backgroundFrame);
        backgroundImage.name = "Background Image";

        Frame spotifyHistory = new Frame();
        spotifyHistory.setPosition(new UDim2(0.125, -138, 1, -535));
        spotifyHistory.setSize(new UDim2(300, 800));
        spotifyHistory.setBackgroundTransparency(1f);
        spotifyHistory.name = "Spotify History Frame";
        spotifyHistory.setZIndex(0);
        spotifyHistory.setParent(vibe);

        TextLabel lyricsHint = new TextLabel();
        lyricsHint.name = "Lyrics Hint";
        lyricsHint.setZIndex(30);
        lyricsHint.setSize(new UDim2(1.0, 1.0));
        lyricsHint.setPosition(new UDim2(0, 1.5));
        lyricsHint.setFont(new Font(fontFamily, Font.PLAIN, 15));
        lyricsHint.setTextHorizontalAlignment(TextLabel.alignTextLeft);
        lyricsHint.setText("[0:00]: some lyrics....");
        //lyricsHint.setParent(timePositionBackground);

        class SpotifyHistoryFrame extends Frame{

            boolean hovered = false;

            long timeSinceHovered = System.currentTimeMillis();
            long timeSinceUnhoverered = System.currentTimeMillis();

            public SpotifyHistoryFrame clone(){
                SpotifyHistoryFrame a = new SpotifyHistoryFrame();
                for (Structure b :this.getChildren())
                    b.clone().setParent(a);
                this.cloneFields(a);
                return a;
            }
        }

        SpotifyHistoryFrame spotifyHistoryEntry = new SpotifyHistoryFrame();
        spotifyHistoryEntry.name = "Spotify History Entry";
        spotifyHistoryEntry.setSize(new UDim2(1.0, 0, 0, 40));
        spotifyHistoryEntry.setBackgroundColor(new Color(94, 45, 149));
        spotifyHistoryEntry.setZIndex(25);
        spotifyHistoryEntry.setVisibility(false);

        ImageLabel spotifyHistoryEntryImage = new ImageLabel();
        spotifyHistoryEntryImage.setSize(new UDim2(30, 30));
        spotifyHistoryEntryImage.setPosition(new UDim2(5, 5));
        spotifyHistoryEntryImage.setZIndex(25);
        // spotifyHistoryEntryImage.setImage(SpotifyCoverCache.fetchImage("https://i.scdn.co/image/ab67616d0000b273e47c67684f4de40964258fe5"));
        spotifyHistoryEntryImage.setBackgroundTransparency(1f);
        spotifyHistoryEntryImage.name = "Spotify History Entry Image";
        spotifyHistoryEntryImage.setParent(spotifyHistoryEntry);

        TextLabel spotifyHistoryEntryText = new TextLabel();
        spotifyHistoryEntryText.setText("Drawing by Ivory Rasmus");
        spotifyHistoryEntryText.setTextWrap(true);
        spotifyHistoryEntryText.setZIndex(25);
        spotifyHistoryEntryText.setFont(new Font(fontFamily, Font.PLAIN, 15));
        spotifyHistoryEntryText.setTextVerticalAlignment(TextLabel.alignTextCenter);
        spotifyHistoryEntryText.setSize(new UDim2(1.0, -40, 1.0, 0));
        spotifyHistoryEntryText.setPosition(new UDim2(40, 0));
        spotifyHistoryEntryText.setBackgroundTransparency(1f);
        spotifyHistoryEntryText.setParent(spotifyHistoryEntry);
        spotifyHistoryEntryText.name = "Spotify History Entry Name";

        TextButton spotifyHistoryEntryButton = new TextButton();
        spotifyHistoryEntryButton.setBackgroundTransparency(1f);
        spotifyHistoryEntryButton.setSize(new UDim2(1.0, 1.0));
        spotifyHistoryEntryButton.setTransparency(1f);
        spotifyHistoryEntryButton.setZIndex(26);
        spotifyHistoryEntryButton.setParent(spotifyHistoryEntry);
        spotifyHistoryEntryText.name = "Spotify History Entry Button";
        

        class SpotifyHistoryHandler implements UIElementInputEvent{
            static SpotifyHistoryFrame[] spotifyHistoryElements = new SpotifyHistoryFrame[20];

            static boolean selected = false;


            static long timeSinceHovered = System.currentTimeMillis();
            static long timeSinceUnhoverered = System.currentTimeMillis();

            static class SpotifyTrackEntry{
                Color primaryColor;
                Color secondaryColor;
    
                String id;
                BufferedImage image;
                String trackName;
                String artistName;
            }

            int indexLocation = 0;
            static SpotifyTrackEntry[] spotifyTrackHistory = new SpotifyTrackEntry[20];
            
            SpotifyHistoryHandler(Frame historyElement, int indexLocation){
                this.indexLocation = indexLocation;
            }

            static Color getContrastingColour(Color color){
                if (SpotifyCoverCache.measureBrightness(color) > 0.5){
                    return new Color(29, 29, 29);
                }else
                    return new Color(255, 255, 255);
            }

            static void refreshTrack(){

                try{
                    for (int i = 0;i<spotifyTrackHistory.length;i++){
                        Frame historyElement = spotifyHistoryElements[i];
                        SpotifyTrackEntry trackEntry = spotifyTrackHistory[i];
                        if (trackEntry == null){
                            historyElement.setVisibility(false);
                            continue;
                        }
                        historyElement.setVisibility(true);
                        ImageLabel imageLabel = (ImageLabel)historyElement.getChildren()[0];
                        TextLabel titleName = (TextLabel)historyElement.getChildren()[1];
                        imageLabel.setImage(trackEntry.image);
                        titleName.setText(trackEntry.trackName + " by " + trackEntry.artistName);
                        historyElement.setBackgroundColor(trackEntry.secondaryColor);
                        titleName.setMaterialColor(getContrastingColour(trackEntry.secondaryColor));
                    }
                }
                catch(Exception er){
                    er.printStackTrace();
                }
            }

            static void removeLastTrack(){
                for (int i = spotifyTrackHistory.length-1;i>0;i--){
                    spotifyTrackHistory[i] = spotifyTrackHistory[i - 1];
                }
                spotifyTrackHistory[0] = null;
                refreshTrack();
            }

            static void appendNewTrack(SpotifyTrackEntry track){
                for (int i = 0;i<spotifyTrackHistory.length-1;i++){
                    spotifyTrackHistory[i] = spotifyTrackHistory[i + 1];
                }
                spotifyTrackHistory[spotifyTrackHistory.length-1] = track;
                refreshTrack();
            }

            @Override
            public void onMouseButtonEvent(MouseDetails arg0) {
                // TODO Auto-generated method stub
                if (arg0.isButtonDown() == false)
                    return;

                try {
                    SpotifyState.playSpotifyTrack(spotifyTrackHistory[this.indexLocation].id);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            }

            @Override
            public void onMouseMovedEvent(MouseDetails arg0) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void mouseEnter(MouseDetails arg0) {
                // TODO Auto-generated method stub
                spotifyHistoryElements[this.indexLocation].hovered = true;
                spotifyHistoryElements[this.indexLocation].timeSinceHovered = System.currentTimeMillis();
                if (selected)
                    return;
                selected = true;
                if (System.currentTimeMillis() - timeSinceUnhoverered < 50){
                    timeSinceHovered = System.currentTimeMillis() - 1000;
                }else
                    timeSinceHovered = System.currentTimeMillis();
            }

            @Override
            public void mouseLeave(MouseDetails arg0) {
                // TODO Auto-generated method stub
                spotifyHistoryElements[this.indexLocation].hovered = false;
                spotifyHistoryElements[this.indexLocation].timeSinceUnhoverered = System.currentTimeMillis();
                if (selected == false)
                    return;
                timeSinceUnhoverered = System.currentTimeMillis();
                selected = false;
                
            }
        }

        for (int i = 0;i<SpotifyHistoryHandler.spotifyHistoryElements.length;i++){
            SpotifyHistoryFrame element = spotifyHistoryEntry.clone();
            element.setPosition(new UDim2(0, 40*i));
            ic.appendUIElementListener(element.getChildren()[2], new SpotifyHistoryHandler(element, i));
            SpotifyHistoryHandler.spotifyHistoryElements[i] = element;
            element.setParent(spotifyHistory);
        }

        TextLabel[] historicalLinesLabel = new TextLabel[10];
        for (int i = 0;i<historicalLinesLabel.length;i++){
            historicalLinesLabel[i] = historicalLyrics.clone();
            historicalLinesLabel[i].setParent(scrollLyrics);
        }

        SpotifyVisualizerHandler.futureText = futureText;
        SpotifyVisualizerHandler.primaryText = primaryText;
        SpotifyVisualizerHandler.secondaryText = secondaryText;
        SpotifyVisualizerHandler.futureText = futureText;
        SpotifyVisualizerHandler.historicalLinesLabel = historicalLinesLabel;
        SpotifyVisualizerHandler.backgroundImage = backgroundImage;

        class LyricalHandler implements UIElementInputEvent, InstructionEventHandler{
            TextButton textButton = null;
            JadeLyricsManager.JadeLyrics lyrics;

            static boolean mouseButtonDown = false;
            static boolean hoveringTimePositionBar = false;
            static long timeSinceTimePositionUpdate = System.currentTimeMillis();
            static double leftOffTimePosition = 0;

            static LyricalHandler self;
            static Frame[] lyricalTicks = new Frame[0];
            static TextButton previousSelectedTextButton = null;
            static double currentTimePosition = 0;
            static boolean playing = false;
            static JadeLyricsManager.JadeLyrics selectedLyrics;
            static JadeLyricsManager.JadeLyrics previousLyrics;

            

            static long timeSinceSpotifyTrack = 0;
            static String currentSpotifyID = "";
            static Color primaryColorArtwork = new Color(29, 29, 29);
            static Color secondaryColorArtwork = new Color(29, 29, 29);
            static Color genericColorArtwork = new Color(29, 29, 29);
            static BufferedImage spotifyArtwork;
            static double spotifyTimeLength = 0;
            static boolean spotifyPlayback = false;
            static double spotifyTimePosition = 0;
            static String spotifyTrackInfo = "";
            static boolean spotifyReadable = false;
            static long spotifyTimeUpdate = System.currentTimeMillis();

            static double parsedTimeLength = 0;

            static boolean spotifyIntegrationEnabled = false;
            static boolean spotifyServiceEnabled = false;

            static boolean adjustForSpotifyBug = false;
            static boolean spotifyExperiencingBugs = false;
            static boolean leftBeginning = false;

            static long timeInitialized;

            static LinkedList<String> historicalLyricalLines = new LinkedList<String>();

            static TextButton[] lyricalButtons = new TextButton[0];

            static final Color goodColorState = new Color(0, 255, 85);
            static final Color badColorState = new Color(255, 0, 85);

            void resetPlayback(){
                playing = false;
                timeInitialized = System.currentTimeMillis();
                pauseButton.setText("▶");
                historicalLyricalLines.clear();
            }

            double getTimeFactorLyrics(double currentTimePosition, double firstWordSpoken, double lastWordSpoken, double newLineBeginning){
                double y1Value = 2 * (currentTimePosition - firstWordSpoken) + 1.5f;
                int eNable = (int)Math.max(0, Math.signum(currentTimePosition - (lastWordSpoken + newLineBeginning -0.5)/2));
                double y2Value = - currentTimePosition + lastWordSpoken + 1 + (2*currentTimePosition - lastWordSpoken - newLineBeginning + 0.5) * eNable;

                return 1 - Math.max(Math.min(y1Value, 1), 0) * Math.max(Math.min(y2Value, 1), 0);
            }

            @Override
            public void onNewInstructions(long arg0) {


                if (spotifyIntegrationEnabled && spotifyReadable){
                    parsedTimeLength = spotifyTimeLength;
                    timeInitialized = spotifyTimeUpdate - (long)(1000*spotifyTimePosition);

                    if (spotifyPlayback == false){
                        currentTimePosition = spotifyTimePosition;
                    }else
                        currentTimePosition = (System.currentTimeMillis()-timeInitialized)/1000.0f;

                    if (hoveringTimePositionBar && mouseButtonDown){
                        double mousePercentage = (double)(ic.getMouseLocation().x - timePositionBackgroundHover.getAbsolutePosition().x)/timePositionBackgroundHover.getAbsoluteSize().width;
                        currentTimePosition = Math.max(Math.min(mousePercentage * spotifyTimeLength, parsedTimeLength), 0);
                        leftOffTimePosition = currentTimePosition;
                        timeSinceTimePositionUpdate = System.currentTimeMillis();
                    }
                    if (System.currentTimeMillis() - timeSinceTimePositionUpdate < 1000)
                        currentTimePosition = leftOffTimePosition;

                    status.setText(String.format("%s / %s - %s", parseTime((int)currentTimePosition), parseTime((int)parsedTimeLength), spotifyTrackInfo));

                    if (spotifyPlayback){
                        pauseButton.setText("∣∣");
                    }else{
                        pauseButton.setText("▶");
                    }

                    double timeStartFactor = Math.min(Math.max((currentTimePosition-0.75f)*2, 0), 1) * Math.min(Math.max(5-currentTimePosition, 0), 1);                    
                    double animatingFactor = (double)Math.pow(timeStartFactor - 1, 3) + 1;

                    songInfoNotification.setText(spotifyTrackInfo.substring(0, (int)(spotifyTrackInfo.length()*animatingFactor)));
                    songPlayingNotification.setText("Now Playing...".substring(0, (int)(14*animatingFactor)));
                    if (selectedLyrics != null){
                        songTypeNotification.setMaterialColor(goodColorState);
                        songTypeNotification.setText("Jade Lyrics Supported!".substring(0, (int)(22*animatingFactor)));
                    }else{
                        songTypeNotification.setMaterialColor(badColorState);
                        songTypeNotification.setText("Jade Lyrics not Supported".substring(0, (int)(25*animatingFactor)));
                    }

                    currentTimePosition += 0.0;//0.15;
                    if (spotifyExperiencingBugs && Input1.fixSpotifyPlayback == false){
                        currentTimePosition -= 0.95;
                    }
                    artwork.setImage(spotifyArtwork);
                    backgroundImage.setImage(spotifyArtwork);
                    if (spotifyArtwork != null){
                        timePositionBackground.setSize(new UDim2(0.75, -640/5-25, 0, 25));
                        status.setSize(new UDim2(0.75, -325-640/5-25, 0, 50));
                        backgroundImage.setVisibility(
                            BarVisualizerTheme.currentVisualizerTheme.visualizerName.equals("Spotify Lyrics Like") || 
                            BarVisualizerTheme.currentVisualizerTheme.visualizerName.equals("Spotify Lyrics + Visualizer"));
                        artwork.setVisibility(true);
                    }else{
                        artwork.setVisibility(false);
                        timePositionBackground.setSize(new UDim2(0.75, 0, 0, 25));
                        status.setSize(new UDim2(0.75, -325, 0, 50));
                    }
                }else{

                    if (playing){
                        currentTimePosition = (System.currentTimeMillis()-timeInitialized)/1000.0f;
                    }

                    timePositionBackground.setSize(new UDim2(0.75, 0, 0, 25));
                    status.setSize(new UDim2(0.75, -325, 0, 50));
                    artwork.setVisibility(false);
                    backgroundImage.setVisibility(false);
                    artwork.setImage(null);

                    if (selectedLyrics == null){
                        status.setText("0:00 / 0:00 - No Lyrics Playing");
                        return;
                    }
    
                    parsedTimeLength = selectedLyrics.timeLength;
                    status.setText(String.format("%s / %s - %s", parseTime((int)currentTimePosition), parseTime((int)parsedTimeLength), selectedLyrics.songName));

                    songInfoNotification.setText("");
                    songPlayingNotification.setText("");
                    songTypeNotification.setText("");
                }

                {
                    double timeFactor = Math.min((System.currentTimeMillis() - timeSinceSpotifyTrack)/1000.0, 1);
                    double animationFactor = Math.pow(timeFactor, 2);


                    SpotifyVisualizerHandler.primaryColor = lerp(SpotifyVisualizerHandler.primaryColor, primaryColorArtwork, animationFactor);
                    SpotifyVisualizerHandler.secondaryColor = lerp(SpotifyVisualizerHandler.secondaryColor, secondaryColorArtwork, animationFactor);;
                    SpotifyVisualizerHandler.genericColor = lerp(SpotifyVisualizerHandler.genericColor, genericColorArtwork, animationFactor);
                    artwork.setTransparency(1 - (float)timeFactor);
                    artworkButton.setBackgroundColor(SpotifyVisualizerHandler.primaryColor);
                    artwork.setBorderColor(SpotifyVisualizerHandler.secondaryColor);
                    status.setMaterialColor(SpotifyVisualizerHandler.primaryColor);
                    artwork.setBackgroundColor(SpotifyVisualizerHandler.secondaryColor);
                    lyricsHint.setBackgroundColor(SpotifyVisualizerHandler.primaryColor);
                    lyricsHint.setMaterialColor(lerp(lyricsHint.getMaterialColor(), SpotifyHistoryHandler.getContrastingColour(primaryColorArtwork), animationFactor));
                    if (SpotifyCoverCache.measureBrightness(SpotifyVisualizerHandler.secondaryColor) < 0.5){
                        timePositionBackground.setBackgroundColor(lerp(timePositionBackground.getBackgroundColor(), new Color(100, 100, 100), animationFactor));
                    }else{
                        timePositionBackground.setBackgroundColor(lerp(timePositionBackground.getBackgroundColor(), new Color(10, 10, 29), animationFactor));
                    }
                }

                if (selectedLyrics != previousLyrics){
                    previousLyrics = selectedLyrics;
                    if (previousLyrics != null){
                        {
                            for (Frame frame : lyricalTicks){
                                timePositionBackground.removeChild(frame);
                            }
                        }
                        lyricalTicks = new Frame[selectedLyrics.lyricalLines.length];
    
                        {
                            int i = 0;
                            for (JadeLyricsManager.LyricalLine line : selectedLyrics.lyricalLines){
                                
                                Frame lyricalTick = templateTick.clone();
                                double startTime = line.lyricalInstructions[Math.min(0, line.lyricalInstructions.length-1)].time;
                                double endTime = line.lyricalInstructions[Math.max(0, line.lyricalInstructions.length-1)].time;
                                lyricalTick.setPosition(new UDim2(startTime/parsedTimeLength, 0.25));
                                lyricalTick.setSize(new UDim2((endTime-startTime)/parsedTimeLength, 1, 0.5, 0));
                                lyricalTicks[i++] = lyricalTick;
                                lyricalTick.setParent(timePositionBackground);
                            }
                        }
                    }else{
                        for (int i = 0;i<10;i++){
                            TextLabel tl = historicalLinesLabel[i];
                            tl.setText("");
                        };
                        for (Frame frame : lyricalTicks){
                            frame.setParent(null);
                        }
                        lyricalTicks = new Frame[0];
                        primaryText.setText("");
                        secondaryText.setText("");
                        futureText.setText("");
                        return;
                    }
                }else
                    if (selectedLyrics != null){
                        double maximumAnimationFactor = 0;
                        int i = 0;

                        for (JadeLyricsManager.LyricalLine line : selectedLyrics.lyricalLines){
                            Frame lyricalTick = lyricalTicks[i++];
                            if (lyricalTick == null)
                                continue;
                            double startTime = line.lyricalInstructions[Math.min(0, line.lyricalInstructions.length-1)].time;
                            double endTime = line.lyricalInstructions[Math.max(0, line.lyricalInstructions.length-1)].time;
                            double futureStartTime = endTime;
                            if (i < selectedLyrics.lyricalLines.length){
                                JadeLyricsManager.LyricalLine nextLine = selectedLyrics.lyricalLines[i];
                                futureStartTime = nextLine.startTime;
                            }else{
                                futureStartTime += 1;
                            }

                            endTime = Math.min(endTime + 4, futureStartTime);

                            double timeFactor = 
                                Math.max(Math.min(2*(currentTimePosition - startTime + 0.125), 1), 0) * 
                                Math.max(Math.min(-2*(currentTimePosition - endTime - 0.125), 1), 0);

                            double animationFactor = Math.pow((timeFactor-1), 3) + 1;
                            maximumAnimationFactor = Math.max(maximumAnimationFactor, animationFactor);

                            /*if (element.selected)
                                lastSelectedElement = element;

                            double animationFactor2 = 0;

                            if (element.equals(lastSelectedElement)){
                                
                            }else{

                            }*/

                            lyricalTick.setPosition(new UDim2(startTime/parsedTimeLength, (int)(-2 * animationFactor), 0.25 - animationFactor * 0.25, 0));
                            lyricalTick.setSize(new UDim2((endTime-startTime)/parsedTimeLength, 1+(int)(4 * animationFactor), 0.5 + animationFactor * 0.5, 0));
                            lyricalTick.setBackgroundColor(lerp(SpotifyVisualizerHandler.secondaryColor.darker().darker(), SpotifyVisualizerHandler.primaryColor, animationFactor));
                            
                        };

                        /*if (LyricalTickElement.globalSelected != lastSelectedElement.selected){
                            LyricalTickElement.globalSelected = lastSelectedElement.selected;
                            if (LyricalTickElement.globalSelected){
                                LyricalTickElement.timeSinceSelected = System.currentTimeMillis() - (1000 - Math.min(1000, System.currentTimeMillis() - LyricalTickElement.timeSinceUnSelected));
                            }else{
                                LyricalTickElement.timeSinceUnSelected = System.currentTimeMillis() - (1000 - Math.min(1000, System.currentTimeMillis() - LyricalTickElement.timeSinceUnSelected));
                            }
                        }
                        if (lastSelectedElement.selected){
                            double timeFactorA = Math.min((System.currentTimeMillis() - LyricalTickElement.timeSinceSelected)/500.0, 1);
                            lyricsHint.setText(String.format("[%s]: %s", parseTime((int)(lastSelectedElement.startTime + 0.5)), lastSelectedElement.line));
                            lyricsHint.setPosition(new UDim2(0.0, 1 + 0.5 * timeFactorA));
                            lyricsHint.setTransparency(1 -(float)timeFactorA);
                            lyricsHint.setBackgroundTransparency(1 - (float)timeFactorA);
                        }else{
                            double timeFactorA = Math.min((System.currentTimeMillis() - LyricalTickElement.timeSinceUnSelected)/500.0, 1);
                            lyricsHint.setPosition(new UDim2(0.0, 1.5 - 0.5 * timeFactorA));
                            lyricsHint.setTransparency((float)timeFactorA);
                            lyricsHint.setBackgroundTransparency((float)timeFactorA);
                        }*/
                        timePositionBar.setBackgroundColor(lerp(SpotifyVisualizerHandler.secondaryColor, SpotifyVisualizerHandler.primaryColor, maximumAnimationFactor));
                        timePositionBar.setSize(new UDim2(currentTimePosition/parsedTimeLength, 0.75 + 0.25 * maximumAnimationFactor));
                    }else{
                        timePositionBar.setSize(new UDim2(currentTimePosition/parsedTimeLength, 1.0));
                        timePositionBar.setBackgroundColor(SpotifyVisualizerHandler.secondaryColor);
                    }

                timePositionBar.setPosition(new UDim2(0, 0, (1 - timePositionBar.getSize().yScale)/2, 0));

                double xAnimationOffset = Math.sin(System.currentTimeMillis()/2000.0)*25;
                double yAnimationOffset = Math.sin(System.currentTimeMillis()/3000.0)*10;
                
                if (backgroundImage.getParent() != null){
                    backgroundImage.setPosition(new UDim2(-0.5, (int)(10*xAnimationOffset), -0.5, (int)(10*yAnimationOffset)));
                }

                {
                    double positionFactor = 0;
                    if (SpotifyHistoryHandler.selected && LyricalHandler.spotifyIntegrationEnabled){

                        double timeFactor = Math.min(1, (System.currentTimeMillis() - SpotifyHistoryHandler.timeSinceHovered)/500.0);
                        
                        positionFactor = Math.pow(timeFactor, 0.25);
                    }else{
                        double timeFactor = Math.min(1, (System.currentTimeMillis() - SpotifyHistoryHandler.timeSinceUnhoverered)/500.0);
                        positionFactor = Math.pow(1 - timeFactor, 0.25);
                    }
                    spotifyHistory.setPosition(new UDim2(0.125, -138, 1, -spotifyHistory.getSize().yOffset-100-(int)(35*Math.pow(positionFactor, 0.25))));
                    for (int i = 0;i<SpotifyHistoryHandler.spotifyHistoryElements.length;i++){
                        SpotifyHistoryFrame element = SpotifyHistoryHandler.spotifyHistoryElements[i];
                        SpotifyHistoryHandler.SpotifyTrackEntry trackEntry = SpotifyHistoryHandler.spotifyTrackHistory[i];
                        if (trackEntry != null){
                            element.setBackgroundTransparency((float)(1 - positionFactor));
                            ((TextLabel)element.getChildren()[1]).setTransparency((float)(1 - positionFactor));
                            ((ImageLabel)element.getChildren()[0]).setTransparency((float)(1 - positionFactor));
                            element.setVisibility(positionFactor != 0);
                            if (element.hovered){
                                double timeFactor = Math.min(1, (System.currentTimeMillis() - element.timeSinceHovered)/250.0);
                                element.setBackgroundColor(lerp(trackEntry.secondaryColor, trackEntry.primaryColor, timeFactor));
                                ((TextLabel)element.getChildren()[1]).setMaterialColor(lerp(SpotifyHistoryHandler.getContrastingColour(trackEntry.secondaryColor), 
                                    SpotifyHistoryHandler.getContrastingColour(trackEntry.primaryColor), timeFactor));
                            }else{
                                double timeFactor = Math.min(1, (System.currentTimeMillis() - element.timeSinceUnhoverered)/250.0);
                                element.setBackgroundColor(lerp(trackEntry.primaryColor, trackEntry.secondaryColor, timeFactor));
                                ((TextLabel)element.getChildren()[1]).setMaterialColor(lerp(SpotifyHistoryHandler.getContrastingColour(trackEntry.primaryColor), 
                                SpotifyHistoryHandler.getContrastingColour(trackEntry.secondaryColor), timeFactor));
                            }
                        }
                        
                        
                    }
                }


                if (previousLyrics == null){
                    primaryText.setTransparency(1);
                    return;
                }
                LyricsPlayer.LyricalState ls = LyricsPlayer.getLyricsState(selectedLyrics, currentTimePosition - 0.1);


                double timeFactor = Math.min(1, Math.max(0, (currentTimePosition-ls.lineStartTime)/0.5f));
                //System.out.println(String.format("%s, %s", ls.mainLine, timeFactor));
                double animationFactor = //(Math.sin(Math.PI * timeFactor - Math.PI/2) + 1)/2;
                //(double)Math.pow(timeFactor-1, 3) + 1;
                1.1*(Math.pow(timeFactor-1, 3) + 1) - .1 * (Math.sin(Math.PI * Math.max(0, Math.min(1, 4*(timeFactor - 0.75))) - Math.PI/2) + 1)/2;
                double actualEndTime = ls.lineEndTime;
                if (ls.endOflyrics){
                    actualEndTime = parsedTimeLength;
                }
                double fadeAwayTimeFactor = getTimeFactorLyrics(currentTimePosition, ls.wordStartTime, ls.wordEndTime+4, actualEndTime);
                double animationFactor2 = (double)Math.pow(fadeAwayTimeFactor-1, 5) + 1;

                double timeFactor3 = Math.min(1, (currentTimePosition - ls.currentWordStartTime)/(ls.currentWordEndTime - ls.currentWordStartTime));
                double animationFactor3 = Math.pow(1 - timeFactor3, 3) + 1;


                //System.out.println(String.format("%s, %s, %s, %s, %s",fadeAwayTimeFactor, currentTimePosition, ls.wordStartTime, ls.wordEndTime+4, actualEndTime));

                for (int i = 0;i<10;i++){
                    TextLabel tl = historicalLinesLabel[i];
                    tl.setText(ls.historicalLines[9-i]);

                    if (i == 0){
                        tl.setFont(new Font(fontFamily, Font.PLAIN, (int)(45-25*Math.min(1, animationFactor))));
                        tl.setPosition(new UDim2(0, 
                            (int)(xAnimationOffset * 2 + xAnimationOffset * (1 - Math.min(1, animationFactor)) * 2), 1, 
                            -140+(int)(90*(1-animationFactor)) + (int)(yAnimationOffset + yAnimationOffset * (1 - animationFactor) * 2)));
                    }else
                        tl.setPosition(new UDim2(0, (int)(xAnimationOffset * 2), 1, -140-40*i+(int)(40*(1-animationFactor)) + (int)yAnimationOffset));
                    tl.setTransparency(Math.min(1, (float)Math.max(tl.getPosition().yOffset/-500.0, animationFactor2)));
                };
                
                if (spotifyIntegrationEnabled == false && selectedLyrics.timeLength < currentTimePosition){
                    self.resetPlayback();
                    return;
                }
                primaryText.setText(ls.completedLine);

                secondaryText.setText(ls.mainLine);

                double absoluteWidth = backgroundFrame.getAbsoluteSize().getWidth()*0.8;
                int pixelWidth = 0;
                String[] lyricalLineWordContent = ls.mainLine.split(" ");
                for (String word : lyricalLineWordContent){
                    if ((word.length()) * 27 + pixelWidth > absoluteWidth)
                        break;
                    pixelWidth += (word.length() + 1) * 27;
                }
                primaryText.setSize(new UDim2(0, pixelWidth, 0, 150));
                secondaryText.setSize(new UDim2(0, pixelWidth, 0, 150));
                
                secondaryText.setPosition(new UDim2(0.1, 
                    (int)(xAnimationOffset * 2 + xAnimationOffset * animationFactor), 1, 
                    (int)(-200-150*animationFactor + yAnimationOffset + yAnimationOffset * animationFactor * 2 + animationFactor3 * 10)));
                secondaryText.setFont(new Font(fontFamily, Font.BOLD, (int)(25+20*Math.min(1, animationFactor))));

                primaryText.setPosition(new UDim2(0.1, 
                    (int)(xAnimationOffset * 2 + xAnimationOffset * animationFactor), 1, 
                    (int)(-200-150*animationFactor + yAnimationOffset + yAnimationOffset * animationFactor * 2+ animationFactor3 * 10)));
                primaryText.setFont(new Font(fontFamily, Font.BOLD, (int)(25+20*Math.min(1, animationFactor))));
                futureText.setPosition(new UDim2(0.1, (int)(xAnimationOffset * 2), 1, (int)yAnimationOffset - 200));
                futureText.setText(ls.futureLine);
                

                primaryText.setTransparency((float)animationFactor2);
                secondaryText.setTransparency((float)animationFactor2);
                futureText.setTransparency((float)Math.sqrt(Math.max((1-animationFactor), animationFactor2)));
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

            @Override
            public void onMouseButtonEvent(MouseDetails eventInfo) {
                
                if (eventInfo.isButtonDown()){

                    selectedLyrics = lyrics;
                    try{
                        if (previousSelectedTextButton != null){
                            previousSelectedTextButton.setBackgroundColor(new Color(29, 29, 29));
                            previousSelectedTextButton.setMaterialColor(new Color(255, 255, 255));
                        }
                        textButton.setBackgroundColor(new Color(255, 255, 255));
                        textButton.setMaterialColor(new Color(29, 29, 29));
                        previousSelectedTextButton = textButton;
                    }
                    catch(Exception err){}
                }
            }

            @Override
            public void onMouseMovedEvent(MouseDetails eventInfo){}

            @Override
            public void mouseEnter(MouseDetails eventInfo){}

            @Override
            public void mouseLeave(MouseDetails eventInfo){}
            

            public void runSpotifyService(){
                class SpotifyServiceRender implements InstructionEventHandler{
                    public Color color1 = new Color(28, 215, 96);
                    public Color color2 = new Color(0, 125, 0);

                    @Override
                    public void onNewInstructions(long arg0) {
                        spotfiyIntegrate.setBackgroundColor(lerp(color1, color2, (double)(0.5f*Math.sin(System.currentTimeMillis()/500.0)+0.5f)));
                    }

                }
                if (spotifyServiceEnabled)
                    return;

                SpotifyServiceRender renderer = new SpotifyServiceRender();
                re.attachInstructionalEventListener(renderer);
                spotfiyIntegrate.setMaterialColor(new Color(10, 10, 29));
                spotifyServiceEnabled = true;


                try{
                    while (LyricalHandler.spotifyIntegrationEnabled){
                        SpotifyState currentSpotifyState = SpotifyState.getSpotifyStatus();
                        if (currentSpotifyState == null){
                            spotifyReadable = false;
                        }else{
                            spotifyReadable = true;
                            spotifyTrackInfo = currentSpotifyState.trackName + " by " + currentSpotifyState.artist;
                            spotifyTimeLength = currentSpotifyState.timeLength;
                            spotifyTimePosition = currentSpotifyState.timePosition;
                            spotifyPlayback = currentSpotifyState.playing;
                            spotifyTimeUpdate = currentSpotifyState.updatedTime;


                            if (currentSpotifyID.equals(currentSpotifyState.id) == false){
                                spotifyExperiencingBugs = false;
                                SpotifyHistoryHandler.SpotifyTrackEntry spotifyTrackEntry = new SpotifyHistoryHandler.SpotifyTrackEntry();
                                spotifyTrackEntry.trackName = currentSpotifyState.trackName;
                                spotifyTrackEntry.artistName = currentSpotifyState.artist;
                                spotifyTrackEntry.id = currentSpotifyState.id;

                                currentSpotifyID = currentSpotifyState.id;
                                timeSinceSpotifyTrack = System.currentTimeMillis();
                                BufferedImage artwork;
                                if (currentSpotifyState.localTrack){
                                    artwork = SpotifyCoverCache.fetchLocalImage(currentSpotifyState.albumName);
                                }else
                                    artwork = SpotifyCoverCache.fetchImage(currentSpotifyState.artworkURL);
                                spotifyTrackEntry.image = artwork;
                                spotifyArtwork = artwork;
                                primaryColorArtwork = SpotifyCoverCache.getPrimaryColors(spotifyArtwork, SpotifyCoverCache.foregroundParameters);
                                secondaryColorArtwork = SpotifyCoverCache.getPrimaryColors(spotifyArtwork, SpotifyCoverCache.backgroundParameters);

                                spotifyTrackEntry.primaryColor = primaryColorArtwork;
                                if (SpotifyCoverCache.measureRelvance(primaryColorArtwork, secondaryColorArtwork) > 0.25){
                                    if (SpotifyCoverCache.measureBrightness(primaryColorArtwork) > 0.5){
                                        spotifyTrackEntry.secondaryColor = secondaryColorArtwork.darker();
                                    }else
                                        spotifyTrackEntry.secondaryColor = secondaryColorArtwork.brighter();
                                }else
                                    spotifyTrackEntry.secondaryColor = secondaryColorArtwork;

                                if (SpotifyHistoryHandler.spotifyTrackHistory[8] != null && spotifyTrackEntry.id.equals(SpotifyHistoryHandler.spotifyTrackHistory[8].id)){
                                    SpotifyHistoryHandler.removeLastTrack();
                                }else
                                    SpotifyHistoryHandler.appendNewTrack(spotifyTrackEntry);
                            }


                            JadeLyricsManager.JadeLyrics jl = 
                                JadeLyricsManager.hashedJadeLyrics.get(currentSpotifyState.id.toLowerCase());
            
                            if (jl == null){
                                jl = JadeLyricsManager.hashedJadeLyrics.get(String.format("%s of %s by %s", currentSpotifyState.trackName, currentSpotifyState.albumName, currentSpotifyState.artist).toLowerCase());
                            }
                            if (jl == null){
                                jl = JadeLyricsManager.hashedJadeLyrics.get(String.format("%s by %s", currentSpotifyState.trackName, currentSpotifyState.artist).toLowerCase());
                            }
                            if (jl == null){
                                jl = JadeLyricsManager.hashedJadeLyrics.get(currentSpotifyState.trackName.toLowerCase());
                            }
                            selectedLyrics = jl;

                            if (spotifyTimePosition > spotifyTimeLength - 2){
                                adjustForSpotifyBug = true;
                            }
                            if (Input1.fixSpotifyPlayback){
                                if (adjustForSpotifyBug && spotifyTimePosition < 2){
                                    adjustForSpotifyBug = false;
                                    //Spotify is gey and is broken
                                    Thread.sleep(1000);
                                    SpotifyState.pausePlaySpotify();
                                    Thread.sleep(5);
                                    SpotifyState.pausePlaySpotify();
                                    Thread.sleep(300);
                                    SpotifyState.pausePlaySpotify();
                                    Thread.sleep(5);
                                    SpotifyState.pausePlaySpotify();
                                    Thread.sleep(300);
                                    SpotifyState.pausePlaySpotify();
                                    Thread.sleep(5);
                                    SpotifyState.pausePlaySpotify();
                                    Thread.sleep(300);
                                    SpotifyState.pausePlaySpotify();
                                    Thread.sleep(5);
                                    SpotifyState.pausePlaySpotify();
                                }
                            }else{
                                if (currentSpotifyState.playing == false)
                                    spotifyExperiencingBugs = false;

                                if (adjustForSpotifyBug && spotifyTimePosition < 2){
                                    adjustForSpotifyBug = false;
                                    spotifyExperiencingBugs = true;
                                    leftBeginning = false;
                                }
                                if (spotifyTimePosition < 2 && leftBeginning){
                                    leftBeginning = false;
                                    spotifyExperiencingBugs = false; 
                                }
                                if (spotifyTimePosition > 2 && spotifyExperiencingBugs)
                                    leftBeginning = true;
                            }
                        }
                    
                        if (previousSelectedTextButton != null){
                            previousSelectedTextButton.setBackgroundColor(new Color(29, 29, 29));
                            previousSelectedTextButton.setMaterialColor(new Color(255, 255, 255));
                            previousSelectedTextButton = null;
                        }
                        Thread.sleep(500);
                    }
                }
                catch(Exception er){
                    er.printStackTrace();
                };
                currentSpotifyID = "";
                timeSinceSpotifyTrack = System.currentTimeMillis();
                spotifyArtwork = null;
                primaryColorArtwork = new Color(255, 255, 255);
                secondaryColorArtwork = new Color(29, 29, 29);
                re.disconnectInstructionalEventListener(renderer);
                spotfiyIntegrate.setBackgroundColor(new Color(29, 29, 29));
                spotfiyIntegrate.setMaterialColor(new Color(255, 255, 255));
                spotifyServiceEnabled = false;
                spotifyIntegrationEnabled = false;
            }

            public static int page = 0;

            public void displayLyrics(){

                Object[] parsedLyrics = JadeLyricsManager.parsedJadeLyrics.toArray();
                for (int i = 0;i<lyricalButtons.length;i++)
                    if (lyricalButtons[i] != null)
                        lyricalButtons[i].setParent(null);

                lyricalButtons = new TextButton[parsedLyrics.length];

                page = Math.max(Math.min(page, parsedLyrics.length/44), 0);

                for (int i = 0;i<parsedLyrics.length;i++){

                    if (!(i/2 >= 22 * page && i/2 < 22 * (page + 1)))
                        continue;

                    JadeLyricsManager.JadeLyrics jl = (JadeLyricsManager.JadeLyrics)parsedLyrics[i];
                    TextButton tb = new TextButton();
                    tb.name = "Jade Lyrics Selection Button";
                    tb.setText(jl.songName);
                    tb.setPosition(new UDim2(0.125 + 0.375*(i%2), 0, 0, i/2%22*25 + 100));
                    tb.setBackgroundColor(new Color(29, 29, 29));
                    tb.setSize(new UDim2(0.375, 0, 0, 25));
                    tb.setFont(new Font(fontFamily, Font.PLAIN, 18));
                    tb.setZIndex(2);

                    LyricalHandler input = new LyricalHandler();
                    input.textButton = tb;
                    input.lyrics = jl;
                    ic.appendUIElementListener(tb, input);
                    lyricalButtons[i] = tb;

                    tb.setParent(lyricsSetting);
                }
            }

            public void loadLyrics() throws IOException{
                JadeLyricsManager.parseAllLyrics(lyricsDirectory);
                displayLyrics();
            }

        }

        LyricalHandler lie = new LyricalHandler();
        LyricalHandler.self = lie;

        ic.appendUIElementListener(artworkButton, new LyricalHandler(){
            public void mouseEnter(MouseDetails arg0){
                artworkButton.setBackgroundTransparency(0.5f);
            }
            public void mouseLeave(MouseDetails arg0){
                artworkButton.setBackgroundTransparency(1f);
            }
            public void onMouseButtonEvent(MouseDetails args0){
                if (args0.isButtonDown() == false)
                    return;
                
                ProcessBuilder pb = new ProcessBuilder("open", currentSpotifyID);
                try{
                    pb.start();
                }
                catch(Exception er){};
            }
        });

        re.attachInstructionalEventListener(lie);

        class A implements MouseInputEvent{

            @Override
            public void onMouseButtonEvent(MouseDetails arg0) {
                // TODO Auto-generated method stub
                
                if (arg0.isButtonDown() == false){
                    LyricalHandler.mouseButtonDown = false;
                    if (LyricalHandler.hoveringTimePositionBar){
                        LyricalHandler.hoveringTimePositionBar = false;
                        LyricalHandler.timeInitialized = System.currentTimeMillis() - (int)(LyricalHandler.currentTimePosition * 1000);
                        LyricalHandler.timeSinceTimePositionUpdate = System.currentTimeMillis();
                        if (LyricalHandler.spotifyIntegrationEnabled)
                            try{
                                LyricalHandler.spotifyExperiencingBugs = false;
                                SpotifyState.seekSpotify((int)(LyricalHandler.currentTimePosition));
                                // SpotifyState((int)(LyricalHandler.currentTimePosition));
                            }
                            catch(Exception er){};
                    }
                }
            }

            @Override
            public void onMouseMovedEvent(MouseDetails arg0) {
                // TODO Auto-generated method stub
                
            }

        }

        ic.appendMouseListener(new A());

        ic.appendUIElementListener(timePositionBackgroundHover, new LyricalHandler(){
            public void onMouseButtonEvent(MouseDetails args0){
                mouseButtonDown = args0.isButtonDown();
                if (mouseButtonDown){
                    hoveringTimePositionBar = true;
                }
                /*
                if (mouseButtonDown == false && hoveringTimePositionBar){
                    timeInitialized = System.currentTimeMillis() - (int)(currentTimePosition * 1000);
                    if (spotifyIntegrationEnabled)
                        try{
                            SpotifyState.seekSpotify((int)(currentTimePosition));
                        }
                        catch(Exception er){};
                    hoveringTimePositionBar = false;
                }*/
            }
        });
        ic.appendUIElementListener(spotfiyIntegrate, new LyricalHandler(){
            public void onMouseButtonEvent(MouseDetails args0){
                if (args0.isButtonDown() == false)
                    return;
                LyricalHandler.spotifyIntegrationEnabled = LyricalHandler.spotifyIntegrationEnabled != true;
                if (LyricalHandler.spotifyIntegrationEnabled){
                    spotfiyIntegrate.setText("🔊 Spotify Connected");
                    lie.runSpotifyService();
                }else{
                    spotfiyIntegrate.setText("🔇 Spotify Disconnected");
                }
            }
        });
        ic.appendUIElementListener(restartButton, new LyricalHandler(){
            public void onMouseButtonEvent(MouseDetails args0){
                if (args0.isButtonDown() == false)
                    return;
                if (spotifyIntegrationEnabled){
                    try{
                        SpotifyState.previousTrack();
                    }
                    catch(Exception er){};
                    return;
                }
                self.resetPlayback();
                lie.onNewInstructions(0);
            }
            public void mouseEnter(MouseDetails args0){
                if (SpotifyHistoryHandler.selected)
                    return;
                if (LyricalHandler.spotifyIntegrationEnabled){
                    SpotifyHistoryHandler.selected = true;
                    SpotifyHistoryHandler.timeSinceHovered = System.currentTimeMillis();
                }
            }
            public void mouseLeave(MouseDetails args0){
                if (SpotifyHistoryHandler.selected == false)
                    return;
                SpotifyHistoryHandler.selected = false;
                SpotifyHistoryHandler.timeSinceUnhoverered = System.currentTimeMillis();
            }
        });
        ic.appendUIElementListener(nextTrack, new LyricalHandler(){
            public void onMouseButtonEvent(MouseDetails args0){
                if (args0.isButtonDown() == false)
                    return;
                if (spotifyIntegrationEnabled){
                    try{
                        SpotifyState.skipTrack();
                    }
                    catch(Exception er){};
                    return;
                }
                if (LyricalHandler.selectedLyrics == null)
                    return;
                LyricalHandler.playing = LyricalHandler.playing != true;
                if (LyricalHandler.playing){
                    LyricalHandler.timeInitialized = System.currentTimeMillis()-(long)(LyricalHandler.currentTimePosition*1000);
                    pauseButton.setText("∣∣");
                }else{
                    pauseButton.setText("▶");
                }
                lie.onNewInstructions(0);
            }
        });

        TextButton previousPage = new TextButton();
        previousPage.setText("Previous Page");
        previousPage.setPosition(new UDim2(0.125, 0, 0, 675));
        previousPage.setBackgroundColor(new Color(29, 29, 29));
        previousPage.setSize(new UDim2(0.25, 0, 0, 25));
        previousPage.setFont(new Font(fontFamily, Font.PLAIN, 20));
        previousPage.setZIndex(2);
        previousPage.setParent(lyricsSetting);

        TextButton nextPage = previousPage.clone();
        nextPage.setText("Next Page");
        nextPage.setPosition(new UDim2(0.625, 0, 0, 675));
        nextPage.setParent(lyricsSetting);

        TextLabel currentPage = new TextLabel();
        currentPage.setText("Current Page: 1");
        currentPage.setPosition(new UDim2(0.375, 0, 0, 675));
        currentPage.setBackgroundColor(new Color(29, 29, 29));
        currentPage.setSize(new UDim2(0.25, 0, 0, 25));
        currentPage.setFont(new Font(fontFamily, Font.PLAIN, 20));
        currentPage.setParent(lyricsSetting);


        ic.appendUIElementListener(previousPage, new LyricalHandler(){
            public void onMouseButtonEvent(MouseDetails args0){
                if (args0.isButtonDown() == false)
                    return;
                LyricalHandler.page -= 1;
                self.displayLyrics();
                currentPage.setText("Current Page: " + (LyricalHandler.page + 1));
            };
        }); 
        ic.appendUIElementListener(nextPage, new LyricalHandler(){
            public void onMouseButtonEvent(MouseDetails args0){
                if (args0.isButtonDown() == false)
                    return;
                LyricalHandler.page += 1;
                self.displayLyrics();
                currentPage.setText("Current Page: " + (LyricalHandler.page + 1));
            };
        });

        ic.appendUIElementListener(pauseButton, new LyricalHandler(){
            public void onMouseButtonEvent(MouseDetails args0){
                if (args0.isButtonDown() == false)
                    return;
                if (spotifyIntegrationEnabled){
                    try{
                        SpotifyState.pausePlaySpotify();
                    }
                    catch(Exception er){};
                    return;
                }
                if (LyricalHandler.selectedLyrics == null)
                    return;
                LyricalHandler.playing = LyricalHandler.playing != true;
                if (LyricalHandler.playing){
                    LyricalHandler.timeInitialized = System.currentTimeMillis()-(long)(LyricalHandler.currentTimePosition*1000);
                    pauseButton.setText("∣∣");
                }else{
                    pauseButton.setText("▶");
                }
                lie.onNewInstructions(0);
            }
        });

        lie.loadLyrics();        
        {
            TextBox songName = new TextBox();
            songName.name = "Song Name TextBox";
            songName.setSize(new UDim2(0.75, 0, 0, 50));
            songName.setBackgroundColor(new Color(29, 29, 29));
            songName.setPlaceHolderText("Enter Song Name Here (or Auto Generated from Spotify Connect)");
            songName.setPosition(new UDim2(0.125, 0, 0, 75));
            songName.setParent(lyricsMaker);
            songName.setFont(new Font(fontFamily, Font.BOLD, 25));
            songName.setTextVerticalAlignment(TextLabel.alignTextCenter);
            songName.setZIndex(2);

            TextBox lyricsBox = new TextBox();
            lyricsBox.name = "Lyrics TextBox";
            lyricsBox.setSize(new UDim2(0.75, 0, 1, -350));
            lyricsBox.setPosition(new UDim2(0.125, 0, 0, 150));
            lyricsBox.setBackgroundColor(new Color(29, 29, 29));
            lyricsBox.setPlaceHolderText("Enter Song Lyrics Here");
            lyricsBox.setEnterOnSubmit(false);
            lyricsBox.setFont(new Font(fontFamily, Font.PLAIN, 10));
            lyricsBox.setParent(lyricsMaker);
            lyricsBox.setZIndex(2);
            lyricsBox.setText("");

            TextButton record = new TextButton();
            record.name = "Record Button";
            record.setText("Record Lyrics");
            record.setSize(new UDim2(0.75, 0, 0, 35));
            record.setPosition(new UDim2(0.125, 0, 1, -175));
            record.setBackgroundColor(new Color(0, 255, 85));
            record.setMaterialColor(new Color(29, 29, 29));
            record.setParent(lyricsMaker);
            record.setZIndex(2);

            TextLabel recordingFrame = new TextLabel();
            recordingFrame.name = "Recording Frame";
            recordingFrame.setSize(new UDim2(0.75, 0, 1, -350));
            recordingFrame.setPosition(new UDim2(0.125, 0, 0, 150));
            recordingFrame.setBackgroundColor(new Color(10, 10, 29));
            recordingFrame.setParent(lyricsMaker);
            recordingFrame.setMaterialColor(new Color(127, 127, 127));
            recordingFrame.setFont(new Font(fontFamily, Font.PLAIN, 30));
            recordingFrame.setText("");
            recordingFrame.setVisibility(false);
            recordingFrame.setTextWrap(true);
            recordingFrame.setZIndex(2);

            TextLabel currentRecording = new TextLabel();
            currentRecording.name = "Current Recording TextLabel";
            currentRecording.setSize(new UDim2(0.75, 0, 0, 120));
            currentRecording.setPosition(new UDim2(0.125, 0, 0, 150));
            currentRecording.setBackgroundTransparency(1f);
            currentRecording.setParent(lyricsMaker);
            currentRecording.setFont(new Font(fontFamily, Font.PLAIN, 30));
            currentRecording.setText("");
            currentRecording.setVisibility(false);
            currentRecording.setTextWrap(true);
            currentRecording.setZIndex(2);

            class LyricsMakerHandler implements UIElementInputEvent, KeyboardInputEvent{

                String bashScript = "";
                String selectedSongName;

                boolean recordingSate = false;
                boolean debounce = false;

                final char eC = (char)0x1B;

                static String parseTime(int time){
                    int seconds = time % 60;
                    int minutes = time / 60;
    
                    if (seconds < 10){
                        return minutes + ":0" + seconds;
                    }else{
                        return minutes + ":" + seconds;
                    }
                }

                public void refreshSongTitle(double currentTime){
                    bashScript += "\necho -n \"\n" + eC + "[40:107mPlaying " + selectedSongName + "... [" + parseTime((int)currentTime) + "]" + eC + "[0m\";";
                }

                public void startRecording(){

                    if (debounce)
                        return;
                    if (recordingSate){
                        recordingSate = false;
                        synchronized(this){
                            this.notify();
                        }
                        return;
                    }
                    recordingSate = true;
                    recordingFrame.setVisibility(true);
                    currentRecording.setVisibility(true);

                    String lyrics = lyricsBox.getText();

                    lyrics = lyrics.replaceAll("\\[.+\\]\\n", "");
                    lyrics = lyrics.replaceAll("(?<!\\(.+), ", "\n");
                    lyrics = lyrics.replaceAll("(?<=\\w)(-)(?=\\w) ", " - ");
                    lyrics = lyrics.replaceAll("\\(", "\n(");
                    lyrics = lyrics.replaceAll("\\! ", "!\n");
                    lyrics = lyrics.replaceAll("\\? ", "?\n");
                    lyrics = lyrics.replaceAll("\\. ", ".\n");
                    
                    String[] lyricalLines = lyrics.split("\n");

                    String futureLines = "";

                    for (int i = 0;i<Math.min(15, lyricalLines.length);i++){
                        futureLines += lyricalLines[i] + "\n\n";
                    }
                    recordingFrame.setText(futureLines);

                    debounce = true;
                    try {
                        if (LyricalHandler.spotifyIntegrationEnabled){
                            if (LyricalHandler.spotifyPlayback)
                                SpotifyState.pausePlaySpotify();

                            SpotifyState.seekSpotify(0);
                        }
                    } catch (Exception e) {
                    }
                    for (int i = 0;i<4;i++){
                        record.setText("Recording in... " + (3-i) + " (press space to submit said word)");
                        try {
                            synchronized(this){
                                Thread.sleep(1000);
                            };
                        } catch (InterruptedException e) {
                            
                            e.printStackTrace();
                        }
                    }
                    debounce = false;

                    selectedSongName = songName.getText();

                    if (selectedSongName.equals("")){ 
                        try{
                            SpotifyState spotifyState = SpotifyState.getSpotifyStatus();
                            selectedSongName = spotifyState.trackName + " of " + spotifyState.albumName + " by " + spotifyState.artist;
                        }
                        catch(Exception er){};
                    }
                    songName.setText(selectedSongName);

                    bashScript = """
#!/bin/bash
# Jade's Lyrics Generator for the song""" + " " + selectedSongName + """
\ngetcurrenttime(){{
    echo $(gdate +%s.%N)
}}
initialtime=$(getcurrenttime);
waittill(){{
    sleep $((initialtime+$1-$(getcurrenttime)));
}};
""";

                    try{
                        if (LyricalHandler.spotifyIntegrationEnabled){
                            SpotifyState.seekSpotify(0);
                            if (LyricalHandler.spotifyPlayback == false)
                                SpotifyState.pausePlaySpotify();
                        }
                    }
                    catch(Exception er){};
                    record.setText("Stop Recording (press enter to submit said word)");
                    record.setBackgroundColor(new Color(255, 0, 85));

                    long startTime = System.currentTimeMillis() + 100;

                    ic.appendKeyboardListener(this);

                    int currentIndex = 0;
                    for (String line : lyricalLines){
                        String completedLine = "";

                        futureLines = "";
                        
                        bashScript += "echo -n \"" + eC + "[s\"";
                        refreshSongTitle((System.currentTimeMillis()-startTime)/1000.0f);
                        bashScript += "echo -n \"" + eC + "[1A" + eC + "[u" + eC + "[10000D\"";

                        if (LyricalHandler.spotifyIntegrationEnabled && LyricalHandler.spotifyReadable)
                            startTime = LyricalHandler.spotifyTimeUpdate - (long)(1000*LyricalHandler.spotifyTimePosition) + 100;

                        for (int i = currentIndex;i<Math.min(currentIndex+15, lyricalLines.length);i++){
                            futureLines += lyricalLines[i] + "\n\n";
                        }
                        recordingFrame.setText(futureLines);

                        String[] words = line.split(" ");
                        for (String word : words){
                            try {
                                synchronized(this){
                                    this.wait();
                                };
                            } catch (InterruptedException e) {
                                
                                e.printStackTrace();
                            }
                            if (recordingSate == false)
                                break;
                            if (LyricalHandler.spotifyIntegrationEnabled && LyricalHandler.spotifyReadable)
                                startTime = LyricalHandler.spotifyTimeUpdate - (long)(1000*LyricalHandler.spotifyTimePosition) + 100;
                            completedLine += word + " ";
                            currentRecording.setText(completedLine);
                            bashScript += "\nwaittill " + (System.currentTimeMillis()-startTime)/1000.0f + ";";
                            String formattedWord = word.replaceAll("\"", "\\\"");
                            bashScript += "\necho -n \"" + formattedWord + " \";";
                            bashScript += "echo -n \"" + eC + "[s\";";
                            refreshSongTitle((System.currentTimeMillis()-startTime)/1000.0f);
                            bashScript += "echo -n \"" + eC + "[1A" + eC + "[u\";";
                        }
                        if (recordingSate == false)
                            break;
                        bashScript += "echo -n \"\n" + eC + "[2K\";";
                        refreshSongTitle((System.currentTimeMillis()-startTime)/1000.0f);
                        bashScript += "echo -n \"" + eC + "[A" + eC + "[1000D\";";
                        currentRecording.setText("");
                        currentIndex ++;
                    }
                    ic.disconnectKeyboardListener(this);

                    if (recordingSate)
                        try {
                            File file = new File(lyricsDirectory + selectedSongName.replace("\\/", " "));
                            FileWriter fileWriter = new FileWriter(file);
                            fileWriter.write(bashScript);
                            fileWriter.close();
                            lie.loadLyrics();
                        }
                        catch(Exception er){};
                    recordingFrame.setVisibility(false);
                    currentRecording.setVisibility(false);
                    songName.setText("");
                    record.setText("Record Lyrics");
                    record.setBackgroundColor(new Color(0, 255, 85));
                }

                @Override
                public void onMouseButtonEvent(MouseDetails arg0) {
                    
                    
                }

                @Override
                public void onMouseMovedEvent(MouseDetails arg0) {
                    
                    
                }

                @Override
                public void mouseEnter(MouseDetails arg0) {
                    
                    
                }

                @Override
                public void mouseLeave(MouseDetails arg0) {
                    
                    
                }

                @Override
                public void onKeyClick(KeyboardDetails arg0) {
                    
                    
                }

                @Override
                public void onKeyDown(KeyboardDetails arg0) {
                    
                    if (arg0.isKeyPressed() && arg0.getKeyId() == 10){
                        synchronized(this){
                            this.notify();
                        }
                    }
                }

                @Override
                public void onKeyUp(KeyboardDetails arg0) {
                    
                    
                }

            }
            LyricsMakerHandler lmh = new LyricsMakerHandler();

            ic.appendUIElementListener(record, new LyricsMakerHandler(){
                public void onMouseButtonEvent(MouseDetails args0){
                    if (args0.isButtonDown() == false)
                        return;
                    lmh.startRecording();
                }
            });

        }
        menu.setParent(backgroundFrame);
        secondaryText.setParent(backgroundFrame);
        primaryText.setParent(backgroundFrame);
        futureText.setParent(backgroundFrame);
        scrollLyrics.setParent(backgroundFrame);

        JadeFourierTransform.generateLookupTables(100, 10000);

        int streamSize = 10000;
        int streamInsertIndex = 0;
        int streamSequences = 20;
        int streamSequenceSize = streamSize / streamSequences;
        
        byte[] stream = new byte[streamSize];

        long timeSinceCheck = System.nanoTime();

        {
            while (true){
                if (dataLine == null){
                    Thread.sleep(500);
                    continue;
                }else{
                    Thread.sleep(1);
                }

                while (dataLine.available() > 36000){
                    dataLine.read(stream, streamInsertIndex * streamSequenceSize, streamSequenceSize);
                    streamInsertIndex = (streamInsertIndex + 1) % streamSequences;
                }

                if ((System.nanoTime() - timeSinceCheck) / 1000.0 > 28){

                    timeSinceCheck = System.nanoTime();

                    double[] processedAudioData = new double[streamSize];
                    for (int r = 0;r<processedAudioData.length / 2;r++){
                        if (r > processedAudioData.length-1)
                            break; 
                        processedAudioData[r] = (stream[r * 2]);//20.0);
                    }
                    double[] frequency = JadeFourierTransform.getDescreteFrequency(processedAudioData);
        
                    // displayFrequencyGraph(frequency);
                    for (int r = 0;r<100;r++){
                        barValues[r] = frequency[r];
                    }

                }
                
                Thread.sleep(13);
            }
        }
        
        
    }
    static Color lerp(Color colorFrom, Color colorTo, double lerpFactor){
        return new Color(
            (int)(colorFrom.getRed() + (colorTo.getRed()-colorFrom.getRed())*lerpFactor),
            (int)(colorFrom.getGreen() + (colorTo.getGreen()-colorFrom.getGreen())*lerpFactor),
            (int)(colorFrom.getBlue() + (colorTo.getBlue()-colorFrom.getBlue())*lerpFactor));
    }
}
