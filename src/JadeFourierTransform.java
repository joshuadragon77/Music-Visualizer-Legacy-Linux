import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;


public class JadeFourierTransform {

    static double[][] sinLookupTable = null;
    static double[][] cosLookupTable = null;

    static void generateLookupTables(int frequencyBins, int sampleSize){
        sinLookupTable = new double[frequencyBins][];
        cosLookupTable = new double[frequencyBins][];
        
        for (int currentFrequencyBin = 0;currentFrequencyBin < frequencyBins ;currentFrequencyBin += 1){


            double actualFrequencyBin = Math.pow(1.026901, currentFrequencyBin * 2 + 75) - 1;
            // double b = Math.pow(((1479 - actualFrequencyBin)/1479.0 + 2) / 4, 2);
            int observedLength = sampleSize;//(int)(function.length * b);

            cosLookupTable[currentFrequencyBin] = new double[observedLength];
            sinLookupTable[currentFrequencyBin] = new double[observedLength];
            
            for (int r = 0;r<observedLength;r++){
                cosLookupTable[currentFrequencyBin][r] = Math.cos((float)(- r * (actualFrequencyBin) / 64.0 / frequencyBins));
                sinLookupTable[currentFrequencyBin][r] = Math.sin((float)(- r * (actualFrequencyBin) / 64.0 / frequencyBins));
            }

        }
    }

    static double[] getDescreteFrequency(double[] function){

        int frequencyBins = 100;
        double[] frequencies = new double[frequencyBins];

        for (int currentFrequencyBin = 0;currentFrequencyBin < frequencyBins ;currentFrequencyBin += 1){
            
            int observedLength = function.length;//(int)(function.length * b);

            double X = 0;
            double Y = 0;
            for (int r = 0;r<observedLength;r++){
                double x = function[r] * cosLookupTable[currentFrequencyBin][r];
                double y = function[r] * sinLookupTable[currentFrequencyBin][r];
                X += x;
                Y += y;
            }

            X *= (1 + 10 * Math.pow((double)currentFrequencyBin/frequencyBins, 2)) / (2 * observedLength);
            Y *= (1 + 10 * Math.pow((double)currentFrequencyBin/frequencyBins, 2)) / (2 * observedLength);

            frequencies[currentFrequencyBin] = Math.clamp(Math.sqrt(Math.pow(X, 2) + Math.pow(Y, 2)), 0, 1);
        }

        return frequencies;
    }
    public static void main(String[] args) throws Exception{

        generateLookupTables(100, 20000);

        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        Mixer.Info selectedMixer = null;

        for (Mixer.Info mixer : mixers){
            if (mixer.getDescription().equals("Direct Audio Device: Background Music")){
                selectedMixer = mixer;
                break;
            }
        }
        if (selectedMixer == null){
            throw new Exception("Failed to obtain Mixer Device. Cannot initialize audio listening feedback.");
        }
        AudioFormat af = new AudioFormat(192000.0f, 16, 1, true, true);
        
        TargetDataLine dataLine = AudioSystem.getTargetDataLine(af, selectedMixer);
        dataLine.open();
        dataLine.start();
        
        int streamSize = 20000;
        int streamInsertIndex = 0;
        int streamSequences = 20;
        int streamSequenceSize = streamSize / streamSequences;
        
        byte[] stream = new byte[streamSize];
        dataLine.read(stream, streamInsertIndex , streamSize);

        long timeSinceCheck = System.nanoTime();

        double previousVolumeFactor = 1;

        while (true){

            while (dataLine.available() > 36000){
                dataLine.read(stream, streamInsertIndex * streamSequenceSize, streamSequenceSize);
                streamInsertIndex = (streamInsertIndex + 1) % streamSequences;
            }

            if ((System.nanoTime() - timeSinceCheck) / 1000.0 > 28){

                timeSinceCheck = System.nanoTime();

                double[] processedAudioData = new double[streamSize / 2];
                for (int r = 0;r<processedAudioData.length;r++){
                    processedAudioData[r] = (stream[r * 2]);//20.0);
                }
                double[] frequency = getDescreteFrequency(processedAudioData);

                double peakFrequency = 0;

                // displayFrequencyGraph(frequency);
                byte[] buffer2 = new byte[100];
                for (int r = 0;r<100;r++){
                    peakFrequency = Math.max(peakFrequency, frequency[r]);
                    buffer2[r] = (byte)(Math.min((int)(frequency[r]*255 * previousVolumeFactor), 255));
                }
                System.out.write(buffer2);
                System.out.flush();
                
                double volumeFactor = Math.min(25, Math.max(.025, 1/peakFrequency)) * 0.9;
            
                if (peakFrequency == 0){
                    volumeFactor = 1;   
                }

                if (volumeFactor / 0.8 + .1 < previousVolumeFactor / 0.8 ){

                    previousVolumeFactor = previousVolumeFactor + (volumeFactor - previousVolumeFactor) / 2;
                }

                previousVolumeFactor = previousVolumeFactor + (volumeFactor - previousVolumeFactor) / 100;
            }
            
            Thread.sleep(13);

        }
    }
}
