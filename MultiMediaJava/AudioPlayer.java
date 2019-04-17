import java.io.InputStream;
import java.io.BufferedInputStream;
import java.sql.Time;
import java.util.Timer;

import javax.sound.sampled.*;

public class AudioPlayer {
	private AudioInputStream audioInputStream;
	private Clip dataClip;
	private int pausePos;

    public AudioPlayer(InputStream waveStream, int pos) {
    	try {
			dataClip = AudioSystem.getClip();
		} catch (Exception e){
    		e.printStackTrace();
		}
		setAudio(waveStream, pos);
    }

    public void setAudio(InputStream waveStream, int pos){
    	pausePos = pos;
    	if(dataClip.isOpen())
    		dataClip.close();
		try {
			InputStream bufferedIn = new BufferedInputStream(waveStream); // new
			audioInputStream = AudioSystem.getAudioInputStream(bufferedIn);
			dataClip.open(audioInputStream);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setPos(int pos){
    	this.pausePos = pos;
	}

    public void play(){
		try{
			dataClip.setFramePosition(pausePos);
			dataClip.start();
		} catch (Exception e){
			e.printStackTrace();
		}
    }

    public void pause(){
    	pausePos = dataClip.getFramePosition();
    	dataClip.stop();
	}
	public void stop(){
    	pausePos = 0;
		dataClip.stop();
	}
}
