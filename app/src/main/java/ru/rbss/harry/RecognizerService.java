package ru.rbss.harry;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

/**
 * Created by jandor on 04.03.15.
 */
public class RecognizerService extends Service {
    private static final String MENU_SEARCH = "menu";
    private static final String KAPUT_DRACONIS = "kaput";
    private static final String AKCIO_UCHEBNIK = "akcio";
    private static final String UCHEBNIK = "uchebnik";
    private static final String LUMOS = "lumos";
    private static final String LEMON = "lemon";
    private static final String TAG = "RecognizerService";


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private class WorkerThread extends HandlerThread   implements RecognitionListener{
        private SpeechRecognizer recognizer;


        public WorkerThread(String name) {
            super(name);
        }

        @Override
        protected void onLooperPrepared() {
            showMessage("initializing");
            Assets assets = null;
            try {
                assets = new Assets(RecognizerService.this);

                File assetsDir = assets.syncAssets();
                // start speech recognizer
                File modelsDir = new File(assetsDir, "models");
                recognizer = defaultSetup()
                        //.setAcousticModel(new File(modelsDir, "hmm/en-us-semi"))
                        .setAcousticModel(new File(modelsDir, "hmm/msu_ru_nsh.cd_cont_1000_8gau_16000"))
                                //.setDictionary(new File(modelsDir, "dict/cmu07a.dic"))
                        .setDictionary(new File(modelsDir, "dict/ru.dic"))
                        .setRawLogDir(assetsDir)
                        .setKeywordThreshold(1e-5f)
                        .getRecognizer();
                recognizer.addListener(this);

                // Create keyword-activation search.
                //recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
                // Create grammar-based searches.

                File menuGrammar = new File(modelsDir, "grammar/menu.gram");
                //recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);
                //File digitsGrammar = new File(modelsDir, "grammar/digits.gram");
                //recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);
                // Create language model search.
                //File languageModel = new File(modelsDir, "lm/weather.dmp");
                //recognizer.addNgramSearch(FORECAST_SEARCH, languageModel);
                File keywordsFile = new File(modelsDir, "grammar/keywords.gram");
                printKeywords(keywordsFile);
                recognizer.addKeywordSearch(MENU_SEARCH, keywordsFile);
                recognizer.startListening(MENU_SEARCH);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //showMessage("done initializing");
        }

        private void printKeywords(File file) throws IOException {
            InputStream is = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(reader);
            String line = null;
            while ((line = br.readLine()) != null)
                Log.d(TAG, "keywords : "+line);
        }
        @Override
        public void run() {
            super.run();

            if (recognizer != null) {
                recognizer.stop();
                recognizer = null;
            }
        }

        private void switchSearch(String searchName) {
            recognizer.stop();
            recognizer.startListening(searchName);
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "speech started");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "speech ended");
            switchSearch(MENU_SEARCH);
        }

        @Override
        public void onPartialResult(Hypothesis hypothesis) {
            final String text = hypothesis.getHypstr();
            final int score = hypothesis.getBestScore();

            Log.d(TAG, "partial result: " + text);


            if (text.startsWith(KAPUT_DRACONIS)||
                    text.startsWith(AKCIO_UCHEBNIK)||
                    text.contains(UCHEBNIK) ||
                    text.startsWith(LUMOS) ||
                    text.startsWith(LEMON))

            {
                Log.d(TAG, "partial result recog: "+text);
                showMessage(score + " " + text);
                Intent intent = new Intent(RecognizerService.this, BluetoothService.class);
                startService(intent);
                switchSearch(MENU_SEARCH);
            }
        }

        @Override
        public void onResult(Hypothesis hypothesis) {
            //showMessage("akcio");

            //switchSearch(MENU_SEARCH);
            /*
            final String text = hypothesis.getHypstr();
            final int score = hypothesis.getBestScore();

            Log.d(TAG, text);
            if (text.equals(KAPUT_DRACONIS)||
                    text.equals(AKCIO_UCHEBNIK)||
                    text.equals(LUMOS))
            {
                showMessage(score + " " + text);

                switchSearch(MENU_SEARCH);
            }
            */
        }



    }
    HandlerThread mThread;
    Handler mHandler = new Handler();

    private void tryStartThread(){
        if (mThread == null) {
            mThread = new WorkerThread("Recognizer");
            mThread.start();
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result =  super.onStartCommand(intent, flags, startId);
        tryStartThread();
        Toast.makeText(this, "started", Toast.LENGTH_SHORT).show();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.icon)
                        .setContentText("Voice recog active")
                        .setContentTitle("hp voice recognition")
                        .setAutoCancel(false)
                .setContentIntent(pendingIntent).build();

        startForeground(1, notification);

        return result;
    }



    @Override
    public void onDestroy() {
        if (mThread != null)
            mThread.getLooper().quit();

        Toast.makeText(this, "stopped", Toast.LENGTH_SHORT).show();
        stopForeground(true);
        super.onDestroy();
    }

    private void showMessage(final String message){
        mHandler.post(new Runnable(){

            @Override
            public void run() {
                Toast.makeText(RecognizerService.this, message, Toast.LENGTH_SHORT).show();
            }
        });

    }
}
