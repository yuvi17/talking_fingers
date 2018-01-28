package com.example.yjaiswal.translatordemo;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONObject;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {

    private TextView txtSpeechInput;
    private ImageButton btnSpeak;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private static final String TAG = MainActivity.class.getSimpleName();
    private String url = "http://35.154.90.117/parse?sentence=";
    private Context context = this;
    HttpHandler httpHandler = new HttpHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);

        // hide the action bar
        //getActionBar().hide();

        btnSpeak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });
    }
    private void promptSpeechInput() {


        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String text = "";
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    text = result.get(0);
                }
                break;
            }

        }

        ParseTask parseTask = new ParseTask();

        parseTask.execute(text);





    }



    private class ParseTask extends AsyncTask<String,Integer,String> {

        protected String doInBackground(String... text) {
            String query = "";
            try {
                query = URLEncoder.encode(text[0]);
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(),
                        "Something went wrong, please try again",
                        Toast.LENGTH_SHORT).show();
            }
            String jsonStr = httpHandler.makeServiceCall(url+query);
            return jsonStr;
        }

        @Override
        protected void onPostExecute(String jsonStr) {


            try {
                JSONObject jsonObj = new JSONObject(jsonStr);
                String response = jsonObj.getString("response");
                response = response.trim();

                String words[] = response.split(" ");

                Log.e(TAG, "Response from url: " + response);

                play(words);


            } catch(Exception e) {
                Log.e(TAG,"lalala" + e.toString());
                Toast.makeText(getApplicationContext(),
                        "Something went wrong, please try again",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }



    public void play(String[] words) {

        try {

            Queue<String> queue = new LinkedList<>();
            for(String word: words ) {
                Log.e(TAG,word);


                queue.add(word);
            }

            playVideo(queue);

        } catch (Exception e) {
            Log.e(TAG,"lalal" + e.toString());
            Toast.makeText(getApplicationContext(),
                    "Something went wrong, please try again",
                    Toast.LENGTH_SHORT).show();

        }

    }


    public void playVideo(final Queue<String> queue) {
        Resources res = context.getResources();
        int soundId = res.getIdentifier(queue.peek().toLowerCase(), "raw", context.getPackageName());
        String uriPath = "android.resource://" + getPackageName() + "/raw/" + soundId;
        Uri myUri = Uri.parse(uriPath); // initialize Uri here
        VideoView videoView = (VideoView)findViewById(R.id.video_view);
        txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
        txtSpeechInput.setText(queue.peek());
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {

                queue.remove();

                if(!queue.isEmpty()) {
                    playVideo(queue);
                } else{
                    return;
                }
            }
        });

        videoView.setVideoURI(myUri);
        videoView.start();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
