package com.example.yjaiswal.translatordemo;
/*
    @author: Yuvraj Jaiswal

 */


import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private TextView txtSpeechInput;
    private TextView wordLetter;
    private ImageButton btnSpeak;
    private ImageButton btnRepeat;
    private ImageButton btnWrite;
    private VideoView videoView;
    private EditText editText;
    private String currentSentence;
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
        btnRepeat = (ImageButton) findViewById(R.id.btnReplay);
        btnWrite = (ImageButton) findViewById(R.id.btnWrite);
        wordLetter = (TextView) findViewById(R.id.wordLetter);
        editText = (EditText) findViewById(R.id.sentence);
        videoView = (VideoView) findViewById(R.id.video_view);

        btnSpeak.setOnClickListener(this);

        btnRepeat.setOnClickListener(this);

        btnWrite.setOnClickListener(this);


    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnSpeak :{
                promptSpeechInput();
                break;
            }
            case R.id.btnReplay: {
                repeatSign();
                break;
            }
            case R.id.btnWrite:{
                takeTextInput();
            }

        }
    }


    private void promptSpeechInput() {

        // clear previous
        txtSpeechInput.setText("");
        wordLetter.setText("");
        videoView.stopPlayback();
        editText.setText("",TextView.BufferType.EDITABLE);


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


    private void repeatSign() {

        try {
            String words[] = currentSentence.split(" ");

            play(words);

        } catch(Exception e) {
            Log.e(TAG,e.toString());
            Toast.makeText(getApplicationContext(),
                    "Something went wrong, please try again",
                    Toast.LENGTH_SHORT).show();
        }

    }


    private void takeTextInput() {

        try {

            InputMethodManager inputManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);

            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);


            Log.e(TAG, String.valueOf(editText.getText()));
            String sentence = String.valueOf(editText.getText()).trim();

            ParseTask parseTask = new ParseTask();

            parseTask.execute(sentence);

        } catch(Exception e) {
            Log.e(TAG,e.toString());
            Toast.makeText(getApplicationContext(),
                    "Something went wrong, please try again",
                    Toast.LENGTH_SHORT).show();
        }

    }


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
        try {

            editText.setText(text,TextView.BufferType.EDITABLE);

            ParseTask parseTask = new ParseTask();

            parseTask.execute(text);

        } catch(Exception e) {
            Log.e(TAG,e.toString());
            Toast.makeText(getApplicationContext(),
                    "Something went wrong, please try again",
                    Toast.LENGTH_SHORT).show();
        }

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

                currentSentence = response;

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
//                Log.e(TAG,word);

                if(word.equals("I") || word.toLowerCase().equals("my")){
                    word = "me";
                }
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

        if(queue.isEmpty()) {
            return;
        }


        Resources res = context.getResources();
        int wordId = res.getIdentifier(queue.peek().toLowerCase(), "raw", context.getPackageName());


        if(wordId == 0) {
            String word = queue.peek();

            wordLetter.setText(word);
            Queue<String> letterQueue = new LinkedList<>();
            for(int i=0;i<word.length();i++) {
                letterQueue.add(String.valueOf(word.charAt(i)));

            }

            playLetters(letterQueue,queue);
            queue.remove();
        } else {
            try {
                String uriPath = "android.resource://" + getPackageName() + "/raw/" + wordId;
                Uri myUri = Uri.parse(uriPath); // initialize Uri here

                txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
                txtSpeechInput.setText(queue.peek());
                videoView.setVideoURI(myUri);
                videoView.start();
                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {

                        PlaybackParams myPlayBackParams = new PlaybackParams();
                        myPlayBackParams.setSpeed(1.5f); //here set speed eg. 0.5 for slow 2 for fast mode
                        mp.setPlaybackParams(myPlayBackParams);

                    }
                });

                videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mp) {

//                        Log.e(TAG,"this is in completion " + queue.peek());
                        queue.remove();

                        if (!queue.isEmpty()) {
//                            Log.e(TAG,"This is here " + queue.peek());
                            playVideo(queue);
                        } else {
                            txtSpeechInput.setText("");
                            wordLetter.setText("");
                            return;
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "lalal" + e.toString());
                Toast.makeText(getApplicationContext(),
                        "Something went wrong, please try again",
                        Toast.LENGTH_SHORT).show();

            }
        }
    }

    // You need to have a separate function (which essentially does the same thing
    // because if in same function you do a recursive call, the second call starts a new thread
    // and takes over the first thread which was displaying letters

    // Hence, this function is severely needed or hell breaks loose

    // I am God

    public void playLetters(final Queue<String> queue,final Queue<String> queueMain ) {

        if(queue.isEmpty()) {
            playVideo(queueMain);
            return;
        }

        Resources res = context.getResources();
        int wordId = res.getIdentifier(queue.peek().toLowerCase(), "raw", context.getPackageName());

        try {
            String uriPath = "android.resource://" + getPackageName() + "/raw/" + wordId;
            Log.e(TAG,"Peek here " +queue.peek());
            Uri myUri = Uri.parse(uriPath); // initialize Uri here

            txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
            txtSpeechInput.setText(queue.peek());
            videoView.setVideoURI(myUri);
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    //works only from api 23
                    Log.e(TAG,"Comes here for " + queue.peek());
                    PlaybackParams myPlayBackParams = new PlaybackParams();
                    myPlayBackParams.setSpeed(2f); //here set speed eg. 0.5 for slow 2 for fast mode
                    mp.setPlaybackParams(myPlayBackParams);
                    videoView.start();
                }
            });

            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {

                    Log.e(TAG,"this is in completion " + queue.peek());
                    queue.remove();

                    if (!queue.isEmpty()) {
                        Log.e(TAG,"This is here " + queue.peek());
                        playLetters(queue,queueMain);
                    } else {
                        txtSpeechInput.setText("");
                        wordLetter.setText("");
                        playVideo(queueMain);
                        return;
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "lalal" + e.toString());
            Toast.makeText(getApplicationContext(),
                    "Something went wrong, please try again",
                    Toast.LENGTH_SHORT).show();

        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
}
