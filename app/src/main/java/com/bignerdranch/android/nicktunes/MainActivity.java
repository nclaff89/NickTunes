package com.bignerdranch.android.nicktunes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String PLAY_NOW = "PLAY NOW";
    public static final String SHOW_PLAY_BTN = "Show Play Button";
    public static final String SHOW_PAUSE_BTN = "Show Pause Button";

    private ImageView prevBtn;
    private ImageView nextBtn;
    private ImageView play_pause_btn;
    private ProgressBar progressBar;
    private TextView stationName;

    private final String BUTTON_STATE = "button_state";
    private final String CURRENT_BUTTON_POSTION = "current_button_position";
    private final String STARTED = "Started";
    private final String SHOW_PAUSE = "paused";

    private int currentStation = 0;  // current radio station
    private boolean isPlaying = false;
    private boolean isStarted = false;
    private boolean showPause = false;

    BroadcastReceiver broadcastReceiver = new MyReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        play_pause_btn = (ImageView) findViewById(R.id.play_ib);

        prevBtn = (ImageView) findViewById(R.id.previous_ib);
        nextBtn = (ImageView) findViewById(R.id.next_ib);
        stationName = (TextView)findViewById(R.id.station_tv);

        progressBar=(ProgressBar) findViewById(R.id.progressBar);

        play_pause_btn.setOnClickListener(this);
        prevBtn.setOnClickListener(this);
        nextBtn.setOnClickListener(this);

        disableButtons();

    }

    @Override
    public void onResume(){
        super.onResume();
        registerBroadcastReceiver();
    }

    @Override
    public void onSaveInstanceState(Bundle saveInstanceState) {
        saveInstanceState.putBoolean(BUTTON_STATE, isPlaying);
        saveInstanceState.putInt(CURRENT_BUTTON_POSTION, currentStation);
        saveInstanceState.putBoolean(STARTED, isStarted);
        saveInstanceState.putBoolean(SHOW_PAUSE, showPause);
        super.onSaveInstanceState(saveInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentStation = savedInstanceState.getInt(CURRENT_BUTTON_POSTION);
        isStarted = savedInstanceState.getBoolean(STARTED, isStarted);
        showPause = savedInstanceState.getBoolean(SHOW_PAUSE);
        isPlaying = savedInstanceState.getBoolean(BUTTON_STATE);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void playAudio(String media) {

        if (!isPlaying) {
            if (!isStarted) {
                progressBar.setVisibility(View.VISIBLE);
            }
            isPlaying = true;
            Intent playerIntent = new Intent(this, JamService.class);
            playerIntent.putExtra("media", media);
            playerIntent.setAction(JamService.START_STATE);
            startService(playerIntent);
            //bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            play_pause_btn.setImageResource(R.drawable.ic_action_pause);
        } else {
            isPlaying = false;
            progressBar.setVisibility(View.GONE);
            Intent playerIntent = new Intent(this, JamService.class);
            playerIntent.putExtra("media", media);
            playerIntent.setAction(JamService.PAUSE_STATE);
            startService(playerIntent);
            //bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            play_pause_btn.setImageResource(R.drawable.ic_action_play);
        }

    }

    private String selectStation(int curRadioStation) {
        String url = null;
        switch (curRadioStation) {
            case 0:
                url = getString(R.string.radio_station_1);
                stationName.setText("STATION 1");
                break;
            case 1:
                url = getString(R.string.radio_station_2);
                stationName.setText("STATION 2");
                break;
            case 2:
                url = getString(R.string.radio_station_3);
                stationName.setText("STATION 3");
                break;
            default:
                url = "";
        }

        return url;
    }

    @Override
    public void onClick(View view) {
        if (view == play_pause_btn) {
            Toast.makeText(MainActivity.this, "it is running!", Toast.LENGTH_SHORT).show();
            playAudio(selectStation(currentStation));
        }

        else if (view == prevBtn) {

            isStarted = false;
            isPlaying = true;
            currentStation--;
            Toast.makeText(MainActivity.this, "PREV", Toast.LENGTH_SHORT).show();

            Intent playerIntent = new Intent(this, JamService.class);
            stopService(playerIntent);
            playAudio(selectStation(currentStation));

            disableButtons();
            String myUrl = selectStation(currentStation);
            playAudio(myUrl);
        }

        else if (view == nextBtn) {
            isStarted = false;
            isPlaying = false;
            currentStation++;
            Toast.makeText(MainActivity.this, "NEXT", Toast.LENGTH_SHORT).show();


            Intent playerIntent = new Intent(this, JamService.class);
            stopService(playerIntent);

            disableButtons();
            String myUrl = selectStation(currentStation);
            playAudio(myUrl);

        }
    }

    private void disableButtons() {
        prevBtn.setVisibility(currentStation > 0 ? View.VISIBLE : View.GONE);
        nextBtn.setVisibility(currentStation < 2 ? View.VISIBLE : View.GONE);
    }

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(PLAY_NOW);
        this.registerReceiver(broadcastReceiver, filter);
    }

    public class MyReceiver extends BroadcastReceiver {
        private static final String TAG = "MyReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PLAY_NOW)) {
                progressBar.setVisibility(View.GONE);
                isStarted = true;
            }
        }
    }




}
