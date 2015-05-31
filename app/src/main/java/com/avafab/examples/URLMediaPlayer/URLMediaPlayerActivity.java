package com.avafab.examples.URLMediaPlayer;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.IOException;

public class URLMediaPlayerActivity extends Activity {

    private static final String LOG_TAG = URLMediaPlayerActivity.class.getSimpleName();

    private static final String PLAYER_ACTION = "com.avafab.player.player_action";

    private static final String ACTION_EXTRA = "action_extra";

    private static final String ACTION_PLAY = "play";
    private static final String ACTION_PAUSE = "pause";
    private static final String ACTION_STOP = "stop";

    private static final int ID = 9842;

    private MediaPlayer mMediaPlayer;
    private SeekBar mSeekBar;
    private String mAudioFileName;

    private PlayerActionReceiver mPlayerActionReceiver;

    private Thread mNotificationUpdater;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // remove title and go full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // get data from main activity intent
        Intent intent = getIntent();

        if (intent.hasExtra(MainActivity.AUDIO_URL)) {
            final String audioFile = intent.getStringExtra(MainActivity.AUDIO_URL);
            final String coverImage = intent.getStringExtra(MainActivity.IMG_URL);
            mAudioFileName = audioFile.substring(audioFile.lastIndexOf('/') + 1);

            mPlayerActionReceiver = new PlayerActionReceiver();
            // create a media player
            mMediaPlayer = new MediaPlayer();
            // try to load data and play
            try {
                // give data to mMediaPlayer
                mMediaPlayer.setDataSource(audioFile);
                // media player asynchronous preparation
                mMediaPlayer.prepareAsync();

                // create a progress dialog (waiting media player preparation)
                final ProgressDialog dialog = new ProgressDialog(URLMediaPlayerActivity.this);

                // set message of the dialog
                dialog.setMessage(getString(R.string.loading));

                // prevent dialog to be canceled by back button press
                dialog.setCancelable(false);

                // show dialog at the bottom
                dialog.getWindow().setGravity(Gravity.CENTER);

                // show dialog
                dialog.show();

                // inflate layout
                setContentView(R.layout.activity_media_player);

                // display title
                ((TextView) findViewById(R.id.now_playing_text)).setText(audioFile);

                /// Load cover image (we use Picasso Library)
                // Get image view
                ImageView imageView = (ImageView) findViewById(R.id.coverImage);

                // Image url
                String imageUrl = coverImage;

                Picasso.with(getApplicationContext()).load(imageUrl).into(imageView);

                // execute this code at the end of asynchronous media player preparation
                mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
                    public void onPrepared(final MediaPlayer mp) {
                        //start media player
                        mp.start();

                        mNotificationUpdater = new Thread(new UpdateNotificationTask());
                        mNotificationUpdater.start();

                        // link seekbar to bar view
                        mSeekBar = (SeekBar) findViewById(R.id.seekBar);

                        //update seekbar
                        mHandler.post(mRunnable);

                        //dismiss dialog
                        dialog.dismiss();
                    }
                });


            } catch (IOException e) {
                finish();
                Toast.makeText(this, getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
            }
            registerReceiver(mPlayerActionReceiver, new IntentFilter(PLAYER_ACTION));
        }
    }

    private Handler mHandler = new Handler();
    private Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            if (mMediaPlayer != null) {
                //set max value
                int duration = mMediaPlayer.getDuration();
                mSeekBar.setMax(duration);

                //update total time text view
                TextView totalTime = (TextView) findViewById(R.id.totalTime);
                totalTime.setText(getTimeString(duration));

                //set progress to current position
                int mCurrentPosition = mMediaPlayer.getCurrentPosition();
                mSeekBar.setProgress(mCurrentPosition);

                //update current time text view
                TextView currentTime = (TextView) findViewById(R.id.currentTime);
                currentTime.setText(getTimeString(mCurrentPosition));

                //handle drag on seekbar
                mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (mMediaPlayer != null && fromUser) {
                            mMediaPlayer.seekTo(progress);
                        }
                    }
                });
            }

            //repeat above code every second
            mHandler.postDelayed(this, 10);
        }
    };

    private class UpdateNotificationTask implements Runnable {

        private NotificationManager mManager;

        public UpdateNotificationTask() {
            mManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    mManager.notify(ID, createNotificationControlPanel());
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Thread was interrupted");
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void playerAction(View view) {
        switch (view.getId()) {
            case R.id.play:
                play();
                break;
            case R.id.pause:
                pause();
                break;
            case R.id.backward:
                seekBackward();
                break;
            case R.id.forward:
                seekForward();
                break;

        }
    }

    public void play() {
        mMediaPlayer.start();
        if (!mNotificationUpdater.isAlive()) {
            if(!mNotificationUpdater.isInterrupted())
                mNotificationUpdater.interrupt();
            mNotificationUpdater = new Thread(new UpdateNotificationTask());
            mNotificationUpdater.start();
        }
    }

    public void pause() {
        mMediaPlayer.pause();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify(ID, createChoiceNotification());
        mNotificationUpdater.interrupt();
    }

    public void stop() {
        mMediaPlayer.stop();
        mHandler.removeMessages(0);
        mMediaPlayer.release();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(ID);
        finish();
    }

    private Notification createNotificationControlPanel() {
        Intent contentIntent = new Intent(this, URLMediaPlayerActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, ID+10, contentIntent, 0);

        Intent pauseIntent = new Intent(PLAYER_ACTION);
        pauseIntent.putExtra(ACTION_EXTRA, ACTION_PAUSE);
        PendingIntent pauseAction = PendingIntent.getBroadcast(this, ID + 1000, pauseIntent, 0);

        return new NotificationCompat.Builder(this)
                .setContentTitle(mAudioFileName)
                .setContentText(getString(R.string.playing) + getTimeString(mMediaPlayer.getCurrentPosition()))
                .setSmallIcon(R.drawable.notif_icon)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_action_pause, getString(R.string.pause), pauseAction)
                .setAutoCancel(false)
                .build();
    }

    private Notification createChoiceNotification() {
        Intent contentIntent = new Intent(this, URLMediaPlayerActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, ID+10, contentIntent, 0);

        Intent playIntent = new Intent(PLAYER_ACTION);
        playIntent.putExtra(ACTION_EXTRA, ACTION_PLAY);
        PendingIntent playAction = PendingIntent.getBroadcast(this, ID + 100, playIntent, 0);

        Intent stopIntent = new Intent(PLAYER_ACTION);
        stopIntent.putExtra(ACTION_EXTRA, ACTION_STOP);
        PendingIntent stopAction = PendingIntent.getBroadcast(this, ID + 150, stopIntent, 0);

        return new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.player_option))
                .setSmallIcon(R.drawable.notif_icon)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_action_play, getString(R.string.play), playAction)
                .addAction(R.drawable.ic_action_pause, getString(R.string.stop), stopAction)
                .build();
    }

    private class PlayerActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PLAYER_ACTION)) {
                String action = intent.getStringExtra(ACTION_EXTRA);
                Log.d(LOG_TAG, action);
                if (action.equals(ACTION_PLAY)) {
                    play();
                } else if (action.equals(ACTION_STOP)) {
                    stop();
                } else if (action.equals(ACTION_PAUSE)) {
                    pause();
                }
            }
        }
    }


    public void seekForward() {
        //set seek time
        int seekForwardTime = 5000;
        // get current song position
        int currentPosition = mMediaPlayer.getCurrentPosition();
        // check if seekForward time is lesser than song duration
        if (currentPosition + seekForwardTime <= mMediaPlayer.getDuration()) {
            // forward song
            mMediaPlayer.seekTo(currentPosition + seekForwardTime);
        } else {
            // forward to end position
            mMediaPlayer.seekTo(mMediaPlayer.getDuration());
        }
    }

    public void seekBackward() {

        //set seek time
        int seekBackwardTime = 5000;

        // get current song position
        int currentPosition = mMediaPlayer.getCurrentPosition();
        // check if seekBackward time is greater than 0 sec
        if (currentPosition - seekBackwardTime >= 0) {
            // forward song
            mMediaPlayer.seekTo(currentPosition - seekBackwardTime);
        } else {
            // backward to starting position
            mMediaPlayer.seekTo(0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mPlayerActionReceiver);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mMediaPlayer != null) {
            stop();
        } else finish();
    }

    private String getTimeString(long millis) {
        StringBuffer buf = new StringBuffer();

        long hours = millis / (1000 * 60 * 60);
        long minutes = (millis % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = ((millis % (1000 * 60 * 60)) % (1000 * 60)) / 1000;

        buf
                .append(String.format("%02d", hours))
                .append(":")
                .append(String.format("%02d", minutes))
                .append(":")
                .append(String.format("%02d", seconds));

        return buf.toString();
    }
}