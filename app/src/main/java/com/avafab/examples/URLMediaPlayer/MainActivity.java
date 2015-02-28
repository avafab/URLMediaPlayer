package com.avafab.examples.URLMediaPlayer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class MainActivity extends ActionBarActivity {

    public final static String AUDIO_URL = "audio_url";
    public final static String IMG_URL = "image_url";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /** open player  */
    public void openPlayer(View view) {
        Intent intent = new Intent(this, URLMediaPlayerActivity.class);
        intent.putExtra(AUDIO_URL, "https://dl.dropboxusercontent.com/u/2763264/RSS%20MP3%20Player/prova1.mp3");
        intent.putExtra(IMG_URL, "https://dl.dropboxusercontent.com/u/2763264/RSS%20MP3%20Player/img3.jpg");
        startActivity(intent);
    }


}
