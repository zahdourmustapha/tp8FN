package com.example.tp8tdm;

import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.Toast;

import com.squareup.seismic.ShakeDetector;

public class MusicPlayerActivity extends AppCompatActivity  {

    TextView titleTv, currentTimeTv, totalTimeTv;
    SeekBar seekBar;


    public static boolean stopMusic = false;

    private MediaPlayer mediaPlayer;


    private ShakeDetector shakeDetector;


    private int selectedIndex;

    AudioModel selectedSong;


    ImageView pausePlay, nextBtn, previousBtn, musicIcon;
    ArrayList<AudioModel> songsList;
    AudioModel currentSong;
    AudioService audioService;
    boolean isBound = false;

    Button addToFavorite, viewFavorite;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioService.AudioServiceBinder binder = (AudioService.AudioServiceBinder) service;
            audioService = binder.getService();
            audioService.setMusicPlayerActivity(MusicPlayerActivity.this); // Use MusicPlayerActivity.this instead of this
            isBound = true;
            audioService.setOnStopMusicListener(() -> updatePlayPauseButton());

            if (audioService != null && selectedSong != null) {
                audioService.playMusic(songsList, songsList.indexOf(selectedSong));
            }
            initializeUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        shakeDetector = new ShakeDetector(this::onShakeDetected);
        shakeDetector.start(sensorManager);

        titleTv = findViewById(R.id.song_title);
        currentTimeTv = findViewById(R.id.current_time);
        totalTimeTv = findViewById(R.id.total_time);
        seekBar = findViewById(R.id.seek_bar);
        pausePlay = findViewById(R.id.pause_play);
        nextBtn = findViewById(R.id.next);
        previousBtn = findViewById(R.id.previous);
        musicIcon = findViewById(R.id.music_icon_big);

        addToFavorite = findViewById(R.id.add_to_favorite);
        viewFavorite = findViewById(R.id.view_favorite);


        songsList = (ArrayList<AudioModel>) getIntent().getSerializableExtra("LIST");
        selectedSong = (AudioModel) getIntent().getSerializableExtra("SELECTED_SONG");


        Intent intent = new Intent(this, AudioService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        addToFavorite.setOnClickListener(v -> addCurrentSongToFavorites());
        viewFavorite.setOnClickListener(v -> viewFavoriteSongs());
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (shakeDetector != null) {
            shakeDetector.stop();
        }
        unbindService(serviceConnection);
    }
    private void onShakeDetected() {
        if (isBound) {
            if (audioService.isPlaying()) {
                audioService.pausePlay();
                Toast.makeText(this, "Music paused", Toast.LENGTH_SHORT).show();
            } else {
                audioService.pausePlay();
                Toast.makeText(this, "Music resumed", Toast.LENGTH_SHORT).show();
            }
            updatePlayPauseButton();
            audioService.showNotification();
        }
    }



    private void initializeUI() {
        if (isBound) {
            songsList = audioService.fetchAudioFromMediaStore();
            audioService.playMusic(songsList, MyMediaPlayer.currentIndex);
            updateTitle();
            pausePlay.setOnClickListener(v -> pausePlay());
            nextBtn.setOnClickListener(v -> playNextSong());
            previousBtn.setOnClickListener(v -> playPreviousSong());

            // Set SeekBar max to the duration of the currently playing song
            seekBar.setMax(audioService.getDuration());

            // Set total time TextView
            totalTimeTv.setText(convertToMMSS(String.valueOf(audioService.getDuration())));

            // Start updating SeekBar progress
            updateSeekBar();

            // Set SeekBar listener
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        audioService.seekTo(progress);
                        currentTimeTv.setText(convertToMMSS(String.valueOf(progress)));
                    }
                }


                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Pause playback when the user starts dragging the SeekBar
                    audioService.pausePlay();
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Resume playback
                    audioService.pausePlay();
                }
            });
        }
    }


    private void updateSeekBar() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isBound) {
                    // Update SeekBar progress
                    seekBar.setProgress(audioService.getCurrentPosition());
                    // Update current time TextView
                    currentTimeTv.setText(convertToMMSS(String.valueOf(audioService.getCurrentPosition())));
                }
                // Repeat updating SeekBar progress every 100 milliseconds
                updateSeekBar();
            }
        }, 100);
    }

    private void pausePlay() {
        if (isBound) {
            audioService.pausePlay();
            updatePlayPauseButton();
        }
    }

    public void updatePlayPauseButton() {
        if (isBound) {
            if (audioService.isPlaying()) {
                pausePlay.setImageResource(R.drawable.ic_baseline_pause_circle_outline_24);
            } else {
                pausePlay.setImageResource(R.drawable.ic_baseline_play_circle_outline_24);
            }
        }
    }

    private void playNextSong() {
        if (isBound) {
            audioService.playNextSong();
            updateTitle();
            updatePlayPauseButton();
            setTotalTime();
        }
    }

    private void playPreviousSong() {
        if (isBound) {
            audioService.playPreviousSong();
            updateTitle();
            updatePlayPauseButton();
            setTotalTime();
        }
    }

    private void updateTitle() {
        if (isBound) {
            String currentSongTitle = audioService.getCurrentSongTitle();
            titleTv.setText(currentSongTitle);
        }
    }

    private String convertToMMSS(String duration) {
        Long millis = Long.parseLong(duration);
        return String.format("%02d:%02d",
                (millis / 1000) / 60,
                (millis / 1000) % 60);
    }

    private void setTotalTime() {
        if (isBound) {
            totalTimeTv.setText(convertToMMSS(audioService.getCurrentSongDuration()));
        }
    }

    public void updateTitle(String title) {
        // Update the title TextView with the new title
        titleTv.setText(title);
    }
    private void addCurrentSongToFavorites() {
        if (isBound) {
            AudioModel currentSong = audioService.getCurrentSong();
            if (currentSong != null) {
                FavoriteDatabaseHelper dbHelper = new FavoriteDatabaseHelper(this);
                dbHelper.addFavorite(currentSong);
                Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void viewFavoriteSongs() {
        Intent intent = new Intent(this, FavoriteActivity.class);
        startActivity(intent);
    }
    @Override
    protected void onResume() {
        super.onResume();

        // Check if stopMusic flag is true, stop the music if needed
        if (stopMusic) {
            // Stop the music playback
            stopMusic();
            // Reset the flag
            stopMusic = false;
        }
    }
    private void stopMusic() {
        if (isBound) {
            audioService.stopMusic();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopMusic();
        shakeDetector.stop();


    }



}
