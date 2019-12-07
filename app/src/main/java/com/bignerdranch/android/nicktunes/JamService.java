package com.bignerdranch.android.nicktunes;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.IOException;

public class JamService extends Service implements MediaPlayer.OnCompletionListener,
MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener{

    private MediaPlayer mediaPlayer;
    private String url;
    private AudioManager audioManager;
    private WifiManager.WifiLock wifiLock;

    public static final String START_STATE= "START_STATE";
    public static final String PAUSE_STATE="PAUSE_STATE";
    public static final String STOP_STATE="STOP_STATE";

    public static final String TAG = "MediaPlayer Error";

    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager phoneManager;


    @Override
    public void onCreate(){
        super.onCreate();

        callStateListener();


    }

    /**
     * Initialize media player and handle start, pause, and stop states
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        String actionState = intent.getAction();
        if(actionState != null){
            switch (actionState){
                case START_STATE:
                    Toast.makeText(this, "start",Toast.LENGTH_SHORT).show();
                    if(mediaPlayer == null){
                        try{
                            //pass url to the stream service
                            url = intent.getExtras().getString("media");
                        }catch (NullPointerException e){
                            stopSelf();
                        }
                        if(!requestAudioFocus()){
                            stopSelf();
                        }

                        if(url != null && url != ""){
                            initializePlayer();
                        }

                    }    else{
                        mediaPlayer.start();
                    }
                    break;
                case PAUSE_STATE:
                    Toast.makeText(this, "Paused",Toast.LENGTH_SHORT).show();
                    if(mediaPlayer != null && mediaPlayer.isPlaying()){
                        mediaPlayer.pause();
                    }
                    break;
                case STOP_STATE:
                    Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
                    if(mediaPlayer!= null && mediaPlayer.isPlaying()){
                        mediaPlayer.stop();
                    }
                    break;

            }
        }
        else{
            Toast.makeText(this,"select media", Toast.LENGTH_SHORT).show();
        }
        return super.onStartCommand(intent, flags, startId);

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        stopStream();

        stopSelf();

    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    /**
     * Handle errors for debugging
     * @param mediaPlayer
     * @param i
     * @param i1
     * @return
     */
    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        switch (i){
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d(TAG, "Media Error, Not valid for progessive Playback");
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d(TAG, "Media Error, Server died.");
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d(TAG, "Media Error, Unknown");
                break;
        }
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {

    }

    /**
     * onPrepared method plays media stream after PreapreAsync calls and media
     * player enters prepared state
     * @param mediaPlayer
     */
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        playStream();

    }

    /**
     * handle audio focus changes
     * @param i
     */

    @Override
    public void onAudioFocusChange(int i) {
        switch(i){
            case AudioManager.AUDIOFOCUS_GAIN:
                if(mediaPlayer == null){
                    initializePlayer();
                }
                else if(!mediaPlayer.isPlaying()){
                    mediaPlayer.start();
                    Intent intent = new Intent();
                    intent.setAction(MainActivity.SHOW_PAUSE_BTN);
                    sendBroadcast(intent);
                }
                mediaPlayer.setVolume(1.0f,1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if(mediaPlayer.isPlaying()){
                    mediaPlayer.stop();
                    Intent intent = new Intent();
                    intent.setAction(MainActivity.SHOW_PLAY_BTN);
                    sendBroadcast(intent);
                }
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if(mediaPlayer.isPlaying()){
                    Intent intent = new Intent();
                    intent.setAction(MainActivity.SHOW_PAUSE_BTN);
                    sendBroadcast(intent);
                    mediaPlayer.pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mediaPlayer != null){
            stopStream();
            mediaPlayer.release();
            wifiLock.release();
        }
        removeAudioFocus();
    }

    /**
     * Initialize the media player and call PrepareAsync.
     * set wake lock and wifilock
     */
    private void initializePlayer(){
        mediaPlayer = new MediaPlayer();


        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        // Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the url
            mediaPlayer.setDataSource(url);
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }

        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        wifiLock = ((WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "MyServiceLook");
        wifiLock.acquire();

        mediaPlayer.prepareAsync();

    }

    /**
     * function to play stream
     */

    private void playStream(){
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            Intent intentProgressBar = new Intent();
            intentProgressBar.setAction(MainActivity.PLAY_NOW);
            sendBroadcast(intentProgressBar);
        }

    }

    /**
     * function to pause stream
     */
    private void pauseStream(){
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }

    }

    /**
     * function to stop stream
     */
    private void stopStream(){
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

    }

    /**
     * Resume stream
     */

    private void resumeStream(){
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }

    }

    /**
     * Telephone manager. This will pause the player if there is
     * an incoming or outgoing call being made. Then resume playing after
     * the call has ended
     */

    private void callStateListener(){
        // Get the telephony manager
        phoneManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseStream();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumeStream();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        phoneManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);

    }

    private boolean requestAudioFocus(){
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return true;
        }
        return false;
    }

    private boolean removeAudioFocus(){
        return  AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }


}
