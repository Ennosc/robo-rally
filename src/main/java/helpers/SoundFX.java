package helpers;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SoundFX {
    private static final Logger logger = Logger.getLogger(SoundFX.class.getName());
    private static Clip backgroundClip;

    /**
     * Plays background music and stops any currently playing background music.
     *
     * @param fileName The name of the sound file
     */
    public static void playBackgroundMusic(String fileName) {
        stopBackgroundMusic();
        try {
            URL soundURL = SoundFX.class.getResource("/sounds/" + fileName);
            if (soundURL == null) {
                throw new IllegalArgumentException("Sound file not found: " + fileName);
            }
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundURL);
            backgroundClip = AudioSystem.getClip();
            backgroundClip.open(audioInputStream);

            if (backgroundClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) backgroundClip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(-15.0f);
            }

            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundClip.start();

        } catch (UnsupportedAudioFileException e) {
            logger.log(Level.SEVERE, "Unsupported audio file: " + fileName, e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "I/O error while trying to read the sound file: " + fileName, e);
        } catch (LineUnavailableException e) {
            logger.log(Level.SEVERE, "No available line to play the sound file: " + fileName, e);
        }
    }

    /**
     * Stops the background music.
     */
    public static void stopBackgroundMusic() {
        if (backgroundClip != null && backgroundClip.isRunning()) {
            backgroundClip.stop();
            backgroundClip.close();
        }
    }

    /**
     * Plays a sound effect over the background music.
     *
     * @param fileName The name of the sound file.
     */
    public static void playSoundEffect(String fileName) {
        try {
            File soundFile = new File("src/main/resources/sounds/" + fileName);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
            Clip soundEffectClip = AudioSystem.getClip();
            soundEffectClip.open(audioInputStream);
            soundEffectClip.start();

            soundEffectClip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    soundEffectClip.close();
                }
            });
        } catch (UnsupportedAudioFileException e) {
            logger.log(Level.SEVERE, "Unsupported audio file: " + fileName, e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "I/O error while trying to read the sound file: " + fileName, e);
        } catch (LineUnavailableException e) {
            logger.log(Level.SEVERE, "No available line for the sound file: " + fileName, e);
        }
    }
}