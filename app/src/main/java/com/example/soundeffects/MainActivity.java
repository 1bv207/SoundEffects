package com.example.soundeffects;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.lang.String.valueOf;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

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

    // マイク録音用
    private MediaRecorder rec;

    // 録音先のパス
    static final String filePath = Environment.getExternalStorageDirectory() + "/sample.wav";

    // 待ち時間計測用
    private long startTime = 0;
    private long endTime = 0;

    int recentOperator;     // 最近押されたキー
    boolean isRecordKeyPushed = false;   // プレイボタンが押されているか

    List<Integer> playList = new ArrayList<>();     // プレイリスト
    List<Long> waitTimeList = new ArrayList<>();    // 待ち時間リスト

    // 加速度センサー用
    protected final static  double RAD2DEG = 180 / Math.PI;

    SensorManager sensorManager;

    float[] rotationMatrix = new float[9];
    float[] gravity = new float[3];
    float[] geomagnetic = new float[3];
    float[] attitude = new float[3];

    int azimuth;
    int pitch;
    int roll;

    TextView azimuthText;
    TextView pitchText;
    TextView rollText;

    // 音量変更用
    private AudioManager mAudioManager;
    int vol;

    TextView volumeText;

    // ピッチ変更用
    float soundPitch;
    TextView soundPitchText;

    // デバックモード
    boolean debug_mode = false;

    private static final int PERMISSION_WRITE_EX_STR = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AudioAttributes attr = null;

        /// パーミッション許可を取る
        if (Build.VERSION.SDK_INT >= 23) {
            if(ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.READ_CONTACTS)
                    != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_CONTACTS
                        },
                        PERMISSION_WRITE_EX_STR);
            }
        }

        // SoundPoolインスタンスの生成
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        } else {
            attr = new AudioAttributes.Builder()
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

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        vol = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        // 加速度センサー
        findView();
        initSensor();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permission, int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permission, grantResults);
        super.onRequestPermissionsResult(requestCode, permission, grantResults);
        if (grantResults.length <= 0) {
            return;
        }
        switch (requestCode) {
            case PERMISSION_WRITE_EX_STR: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    /// 許可が取れた場合・・・
                    /// 必要な処理を書いておく
                } else {
                    /// 許可が取れなかった場合・・・
                    Toast.makeText(this,
                            "アプリを起動できません....", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            return;
        }
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
                recordText.append(valueOf(button.getText().charAt(0)));
            } else {                                                                        // 20個を超えようとした時のエラー
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("You can record up to 21 data.")
                        .setTitle("CAUTION")
                        .setIcon(R.drawable.caution)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

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
                isRecordKeyPushed = false;
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
                if (isRecordKeyPushed) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Please push STOP button.")
                            .setTitle("CAUTION")
                            .setIcon(R.drawable.caution)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                    builder.show();
                    break;
                }

                PlayListPlaying();

                isRecordKeyPushed = false;
                optionModeText.setText(optionButton.getText());
                break;
            case R.id.button_EXIT:                             // EXITボタンが押された時の動作
                System.exit(0);                                  // プログラムの終了
                break;
            case R.id.button_V_REC:
                optionModeText.setText(optionButton.getText());
                startMediaRecord();
                isRecordKeyPushed = true;
                break;
            case R.id.button_V_STOP:
                if (!isRecordKeyPushed) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Please push this button AFTER pushing Voice Recode button.")
                            .setTitle("CAUTION")
                            .setIcon(R.drawable.caution)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                    builder.show();
                    break;
                }

                optionModeText.setText(optionButton.getText());
                stopMediaRecord();
                isRecordKeyPushed = false;
                break;
            case R.id.button_V_PLAY:
                if (isRecordKeyPushed) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Please push VOICE_STOP button.")
                            .setTitle("CAUTION")
                            .setIcon(R.drawable.caution)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                    builder.show();
                    break;
                }

                optionModeText.setText(optionButton.getText());
                startMediaPlay();
                isRecordKeyPushed = false;
                break;
            default:
                break;
        }
    }

    // 音源再生用メソッド
    public void SEPlay(int id) {
        switch (id) {
            case R.id.buttonCymbal:
                soundPool.play(wavCymbal,1f , 1f, 0, 0, 1f);
                currentText.setText((String)"1. Cymbal");
                break;
            case R.id.buttonClap:
                soundPool.play(wavClap, 1f, 1f, 0, 0, 1f);
                currentText.setText((String)"2. Clap");
                break;
            case R.id.buttonBell:
                soundPool.play(wavBell,1f , 1f, 0, 0, 1f);
                currentText.setText((String)"3. Bell");
                break;
            case R.id.buttonDrum:
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
            builder.setMessage("Record data is empty.")
                    .setTitle("CAUTION")
                    .setIcon(R.drawable.caution)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
            builder.show();
            return;
        }

        // 待ち時間のリストが空のときのエラー(おそらくない)
        if (waitTimeList.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("WaitTime data is empty.")
                    .setTitle("CAUTION")
                    .setIcon(R.drawable.caution)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

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
                SEPlay(playList.get(i));
                i++;
                handler.postDelayed(this, waitTimeList.get(i));
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

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener((SensorEventListener) this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
    }

    // MICを使った録音開始
    private void startMediaRecord() {
        try {
            File wavFile = new File(filePath);
            if (wavFile.exists()) {
                wavFile.delete();
            }

            wavFile = null;
            rec = new MediaRecorder();
            rec.setAudioSource(MediaRecorder.AudioSource.MIC);
            rec.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            rec.setOutputFile(filePath);

            rec.prepare();
            Log.d("REC", "RECODING");
            rec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // MICを使った録音停止
    private void stopMediaRecord() {
        try {
            Log.d("REC", "STOP");
            rec.stop();
            rec.reset();
            rec.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // MICを使った録音の再生
    private void startMediaPlay() {
        try {
            // マイク再生用
            File file = new File(filePath);
            Log.d("REC", valueOf(file.length()));
            MediaPlayer mp = new MediaPlayer();
            mp.setDataSource(filePath);
            mp.prepare();
            mp.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_MAGNETIC_FIELD:
                geomagnetic = sensorEvent.values.clone();
                break;
            case Sensor.TYPE_ACCELEROMETER:
                gravity = sensorEvent.values.clone();
                break;
        }
        if (geomagnetic != null && gravity != null) {
            SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic);
            SensorManager.getOrientation(rotationMatrix, attitude);

            azimuth = (int)(attitude[0] * RAD2DEG);
            pitch = (int)(attitude[1] * RAD2DEG);
            roll = (int)(attitude[2] * RAD2DEG);

            if (debug_mode) {
                azimuthText.setText(format("Azm  %s", Integer.toString(azimuth)));
                pitchText.setText(format("Pit  %s", Integer.toString(pitch)));
                rollText.setText(format("Rol  %s", Integer.toString(roll)));
            } else {
                azimuthText.setText("");
                pitchText.setText("");
                rollText.setText("");
            }
        }

        try {
            if (roll < -90) roll = -90;
            else if (roll > 90) roll = 90;
            int changeVol = (int)(vol * ((float)(roll + 90) / 180));
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, changeVol, 0);

            if (debug_mode) {
                volumeText.setText(format("Vol  %s", Integer.toString(changeVol)));
            } else {
                volumeText.setText("");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            e.printStackTrace();
        }

        if (pitch > 50) pitch = 50;
        else if (pitch < -100) pitch = -100;

        soundPitch = 0.0F;

        soundPitch = ((float)(100 - pitch) / 100);

        BigDecimal tmp = new BigDecimal(soundPitch);
        tmp = tmp.setScale(1, BigDecimal.ROUND_HALF_UP);
        soundPitch = tmp.floatValue();
        if (debug_mode) {
            soundPitchText.setText(format("SP  %s", Float.toString(soundPitch)));
        } else {
            soundPitchText.setText("");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    protected void findView() {
        azimuthText = findViewById(R.id.azimuth);
        pitchText = findViewById(R.id.pitch);
        rollText = findViewById(R.id.roll);
        volumeText = findViewById(R.id.volume);
        soundPitchText = findViewById(R.id.soundPitch);
    }

    protected void initSensor() {
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
    }

    /* メニューバー */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.debug) {
            item.setChecked(!item.isChecked());
            debug_mode = item.isChecked();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}