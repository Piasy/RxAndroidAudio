/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.piasy.rxandroidaudio.example;

import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.github.piasy.audioprocessor.AudioProcessor;
import com.github.piasy.rxandroidaudio.StreamAudioPlayer;
import com.github.piasy.rxandroidaudio.StreamAudioRecorder;
import com.tbruyelle.rxpermissions2.RxPermissions;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class StreamActivity extends AppCompatActivity {

    static final int BUFFER_SIZE = 2048;

    @BindView(R.id.mBtnStart)
    Button mBtnStart;
    @BindView(R.id.mRatio)
    SeekBar mRatioBar;
    @BindView(R.id.mRatioValue)
    TextView mRatioValue;

    private RxPermissions mPermissions;
    private StreamAudioRecorder mStreamAudioRecorder;
    private StreamAudioPlayer mStreamAudioPlayer;
    private AudioProcessor mAudioProcessor;
    private FileOutputStream mFileOutputStream;
    private File mOutputFile;
    private byte[] mBuffer;
    private boolean mIsRecording = false;
    private float mRatio = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);
        ButterKnife.bind(this);

        mRatioBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mRatio = (float) progress / 100;
                mRatioValue.setText(String.valueOf(mRatio));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mStreamAudioRecorder = StreamAudioRecorder.getInstance();
        mStreamAudioPlayer = StreamAudioPlayer.getInstance();
        mAudioProcessor = new AudioProcessor(BUFFER_SIZE);
        mBuffer = new byte[BUFFER_SIZE];
    }

    @OnClick(R.id.mBtnStart)
    public void start() {
        if (mIsRecording) {
            stopRecord();
            mBtnStart.setText("Start");
            mIsRecording = false;
        } else {
            boolean isPermissionsGranted = getRxPermissions().isGranted(WRITE_EXTERNAL_STORAGE)
                                           && getRxPermissions().isGranted(RECORD_AUDIO);

            if (!isPermissionsGranted) {
                getRxPermissions()
                        .request(WRITE_EXTERNAL_STORAGE, RECORD_AUDIO)
                        .subscribe(granted -> {
                            // not record first time to request permission
                            if (granted) {
                                Toast.makeText(getApplicationContext(), "Permission granted",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(),
                                        "Permission not granted", Toast.LENGTH_SHORT).show();
                            }
                        }, Throwable::printStackTrace);
            } else {
                startRecord();
                mBtnStart.setText("Stop");
                mIsRecording = true;
            }
        }
    }

    private void startRecord() {
        try {
            mOutputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                                   File.separator + System.nanoTime() + ".stream.m4a");
            mOutputFile.createNewFile();
            mFileOutputStream = new FileOutputStream(mOutputFile);
            mStreamAudioRecorder.start(new StreamAudioRecorder.AudioDataCallback() {
                @Override
                public void onAudioData(byte[] data, int size) {
                    if (mFileOutputStream != null) {
                        try {
                            Log.d("AMP", "amp " + calcAmp(data, size));
                            mFileOutputStream.write(data, 0, size);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onError() {
                    mBtnStart.post(() -> {
                        Toast.makeText(getApplicationContext(), "Record fail",
                                Toast.LENGTH_SHORT).show();
                        mBtnStart.setText("Start");
                        mIsRecording = false;
                    });
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int calcAmp(byte[] data, int size) {
        int amplitude = 0;
        for (int i = 0; i + 1 < size; i += 2) {
            short value = (short) (((data[i + 1] & 0x000000FF) << 8) + (data[i + 1] & 0x000000FF));
            amplitude += Math.abs(value);
        }
        amplitude /= size / 2;
        return amplitude / 2048;
    }

    private void stopRecord() {
        mStreamAudioRecorder.stop();
        try {
            mFileOutputStream.close();
            mFileOutputStream = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.mBtnPlay)
    public void play() {
        Observable.just(mOutputFile)
                .subscribeOn(Schedulers.io())
                .subscribe(file -> {
                    try {
                        mStreamAudioPlayer.init();
                        FileInputStream inputStream = new FileInputStream(file);
                        int read;
                        while ((read = inputStream.read(mBuffer)) > 0) {
                            mStreamAudioPlayer.play(mBuffer, read);
                        }
                        inputStream.close();
                        mStreamAudioPlayer.release();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, Throwable::printStackTrace);
    }

    @OnClick(R.id.mBtnPlayChanged)
    public void playChanged() {
        Observable.just(mOutputFile)
                .subscribeOn(Schedulers.io())
                .subscribe(file -> {
                    try {
                        mStreamAudioPlayer.init();
                        FileInputStream inputStream = new FileInputStream(file);
                        int read;
                        while ((read = inputStream.read(mBuffer)) > 0) {
                            mStreamAudioPlayer.play(mRatio == 1
                                            ? mBuffer
                                            : mAudioProcessor.process(mRatio, mBuffer,
                                                    StreamAudioRecorder.DEFAULT_SAMPLE_RATE),
                                    read);
                        }
                        inputStream.close();
                        mStreamAudioPlayer.release();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }, Throwable::printStackTrace);
    }

    private RxPermissions getRxPermissions() {
        if (mPermissions == null) {
            mPermissions = new RxPermissions(this);
        }
        return mPermissions;
    }
}
