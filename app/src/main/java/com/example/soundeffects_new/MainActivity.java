package com.example.soundeffects_new;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;

import android.content.DialogInterface;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // プレイ中音源用TextView
    TextView currentText;

    // レコード用TextView
    TextView recordText;

    // モード用TextView
    TextView optionModeText;

    // 音源再生用
    SoundPool soundPool;
    int wavCymbal;
    int wavClap;
    int wavBell;
    int wavDrum;

    // 待ち時間計測用
    private long startTime = 0;
    private long endTime = 0;

    int recentOperator;     // 最近押されたキー
    boolean isRecordKeyPushed = false;   // レコードボタンが押されているか

    List<Integer> playList = new ArrayList<>();     // プレイリスト
    List<Long> waitTimeList = new ArrayList<>();    // 待ち時間リスト

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // SoundPoolインスタンスの生成
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        } else {
            AudioAttributes attr = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setAudioAttributes(attr)
                    .setMaxStreams(5)
                    .build();
        }

        // 音源をロード
        wavCymbal = soundPool.load(this, R.raw.cymbal, 1);
        wavClap = soundPool.load(this, R.raw.clap, 1);
        wavBell = soundPool.load(this, R.raw.bell, 1);
        wavDrum = soundPool.load(this, R.raw.drum, 1);

        // 画面のアイテムのインスタンスを取得
        currentText = (TextView)findViewById(R.id.currentPlay);
        recordText = (TextView)findViewById(R.id.Record);
        optionModeText = (TextView)findViewById(R.id.optionmode);
    }

    // 4つの音源ボタン用メソッド
    public void buttonSEClick(View view) {
        Button button = (Button) view;
        recentOperator = button.getId();

        if (isRecordKeyPushed) {
            if (playList.size() < 21) {                     // プレイリストのサイズは20個まで
                // 待ち時間計測
                endTime = System.currentTimeMillis();
                long time = endTime - startTime;
                waitTimeList.add(time);
                startTime = System.currentTimeMillis();

                // 再生
                SEPlay(recentOperator);
                playList.add(recentOperator);
                recordText.append(String.valueOf(button.getText().charAt(0)));
            } else {                                                                        // 20個を超えようとした時のエラー
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("You can record up to 20 data.")
                        .setTitle("CAUTION")
//                        .setIcon(R.drawable.caution)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Log.d("Dialog_message", "PlayList cannot save more than 20 data.");
                            }
                        });
                builder.show();
            }
        }
        else {                                              // プレイリストには保存せず再生
            SEPlay(recentOperator);
        }
    }

    // 5つの機能ボタン用メソッド
    @SuppressLint("NonConstantResourceId")
    public void buttonOptionClick(View view) {
        Button optionButton = (Button) view;
        recentOperator = optionButton.getId();

        switch (recentOperator) {
            case R.id.button_REC:                               // RECORDボタンが押された時の動作
                isRecordKeyPushed = true;
                optionModeText.setText(optionButton.getText());
                startTime = System.currentTimeMillis();         // 計測開始
                break;
            case R.id.button_STOP:                             // STOPボタンが押された時の動作
                isRecordKeyPushed = false;                      // Recordを止める
                optionModeText.setText(optionButton.getText());
                endTime = System.currentTimeMillis();
                long time = endTime - startTime;
                waitTimeList.add(time);
                break;
            case R.id.button_DEL:                              // DELETEボタンが押された時の動作
                playList.clear();
                if (playList.isEmpty())
                    recordText.setText("");
                optionModeText.setText(optionButton.getText());
                isRecordKeyPushed = false;
                break;
            case R.id.button_PLAY:                             // PLAYボタンが押された時の動作
                if (isRecordKeyPushed) {                        // RECORD中にPLAYが押されたら、STOPを押すように指示するポップアップを表示
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Please push STOP button.")
                            .setTitle("CAUTION")
//                            .setIcon(R.drawable.caution)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Log.d("Dialog_message", "Pushed PLAY button during recoding.");
                                }
                            });
                    builder.show();
                    break;
                }

                PlayListPlaying();                              // PlayListを再生する

                isRecordKeyPushed = false;
                optionModeText.setText(optionButton.getText());
                break;
            case R.id.button_EXIT:                             // EXITボタンが押された時の動作
                System.exit(0);                                  // プログラムの終了
                break;
            default:
                break;
        }
    }

    // 音源再生用メソッド
    @SuppressLint("NonConstantResourceId")
    public void SEPlay(int id) {
        switch (id) {
            case R.id.buttonCymbal:     // 1. シンバル
                soundPool.play(wavCymbal,1f , 1f, 0, 0, 1f);
                currentText.setText((String)"1. Cymbal");
                break;
            case R.id.buttonClap:       // 2. 拍手
                soundPool.play(wavClap, 1f, 1f, 0, 0, 1f);
                currentText.setText((String)"2. Clap");
                break;
            case R.id.buttonBell:       // 3. ベル
                soundPool.play(wavBell,1f , 1f, 0, 0, 1f);
                currentText.setText((String)"3. Bell");
                break;
            case R.id.buttonDrum:       // 4. ドラム
                soundPool.play(wavDrum,1f , 1f, 0, 0, 1f);
                currentText.setText((String)"4. Drum");
                break;
            default:
                break;
        }
    }

    // PlayListの再生用メソッド
    public void PlayListPlaying() {

        // プレイリストが空のときのエラー
        if (playList.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Record data is empty.")     // メッセージ
                    .setTitle("CAUTION")                    // タイトル
//                    .setIcon(R.drawable.caution)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {    // クリックの処理
                            Log.d("Dialog_message", "PlayList is Null.");   // ログに出力
                        }
                    });
            builder.show();
            optionModeText.setText("");
            return;
        }

        // 待ち時間のリストが空のときのエラー(おそらくない)
        if (waitTimeList.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("WaitTime data is empty.")       // メッセージ
                    .setTitle("CAUTION")                        // タイトル
//                    .setIcon(R.drawable.caution)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {        // クリックの処理
                            Log.d("Dialog_message", "WaitTimeList is Null.");   // ログに出力
                        }
                    });
            builder.show();
            return;
        }

        // ここからリストの再生部分
        final Handler handler = new Handler();
        final Runnable r = new Runnable() {
            int i = 0;
            @Override
            public void run() {
                if (i >= playList.size()) {
                    optionModeText.setText("");
                    currentText.setText("");
                    return;
                }
                if (recentOperator == R.id.button_STOP) {           // STOPボタンが押されたら停止
                    currentText.setText("");
                    return;
                }
                SEPlay(playList.get(i));    // i番目の音源を再生
                i++;
                handler.postDelayed(this, waitTimeList.get(i));     // i番目とi+1番目の間の待ち時間だけDelayさせる
            }
        };
        handler.post(r);
    }

    // soundPoolのメモリ解放メソッド
    @Override
    protected void onPause() {
        super.onPause();
        soundPool.release();
        soundPool = null;
    }
}

