package objects;

import java.io.File;
//import java.io.IOException;
//import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
//import javax.sound.sampled.AudioFileFormat;
//import javax.sound.sampled.AudioSystem;
//import javax.sound.sampled.UnsupportedAudioFileException;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;

// clase para la reproduccón de los archivos mp3
/*
    constructor recibe BasicPlayerListener listener
*/

public class MP3Player extends BasicPlayer 
{
    private BasicPlayer player = new BasicPlayer();
    private String currentFileName; // la canción actual
    private double currentVolumeValue;

   
    public MP3Player(BasicPlayerListener listener) 
    {
        player.addBasicPlayerListener(listener);
    }

    public void play(String fileName)
    {

        try {
            // si ponen la misma cancion pausada anteriormente
            if ((currentFileName != null) && (currentFileName.equals(fileName)) 
                    && (player.getStatus() == BasicPlayer.PAUSED)) 
            {
                player.resume();
                return;
            }

            File mp3File = new File(fileName);

            currentFileName = fileName;
            player.open(mp3File);
            player.play();
            player.setGain(currentVolumeValue);// instalamos el volumen del sonido

        } catch (BasicPlayerException ex) {
            Logger.getLogger(MP3Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void stop()
    {
        try {
            player.stop();
        } catch (BasicPlayerException ex) {
            Logger.getLogger(MP3Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void pause()
    {
        try {
            player.pause();
        } catch (BasicPlayerException ex) {
            Logger.getLogger(MP3Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // ajusta el volumen a la hora de reproducir la cancion
    public void setVolume(int currentValue, int maximumValue) 
    {
        try {
                this.currentVolumeValue = currentValue;

             if (currentValue == 0) 
             {
                 player.setGain(0);
             } 
                else 
             {
                 player.setGain(calcVolume(currentValue, maximumValue));
             }

        } catch (BasicPlayerException ex) {
            Logger.getLogger(MP3Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    //calcula el valor del volumen del sonido segun los valores 
    //del regulador Jslider
    private double calcVolume(int currentValue, int maximumValue) 
    {
        currentVolumeValue = (double)currentValue/(double)maximumValue;
        return currentVolumeValue;
    }

    public void jump(long bytes) 
    {
        try {
            player.seek(bytes);
            player.setGain(currentVolumeValue);// ajustamos el nivel del volumen
        } catch (BasicPlayerException ex) {
            Logger.getLogger(MP3Player.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
