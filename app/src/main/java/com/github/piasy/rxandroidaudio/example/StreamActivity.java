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

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
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
import com.tbruyelle.rxpermissions.RxPermissions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class StreamActivity extends AppCompatActivity {

    static final int BUFFER_SIZE = 2048;

    @BindView(R.id.mBtnStart)
    Button mBtnStart;
    @BindView(R.id.mRatio)
    SeekBar mRatioBar;
    @BindView(R.id.mRatioValue)
    TextView mRatioValue;

    private byte[] mBuffer;
    private StreamAudioRecorder mStreamAudioRecorder;
    private StreamAudioPlayer mStreamAudioPlayer;
    private AudioProcessor mAudioProcessor;
    private FileOutputStream mFileOutputStream;
    private File mOutputFile;
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
            boolean isPermissionsGranted = RxPermissions.getInstance(getApplicationContext())
                                                   .isGranted(
                                                           Manifest.permission
                                                                   .WRITE_EXTERNAL_STORAGE)
                                           && RxPermissions.getInstance(getApplicationContext())
                                                   .isGranted(Manifest.permission.RECORD_AUDIO);
            if (!isPermissionsGranted) {
                RxPermissions.getInstance(getApplicationContext())
                        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.RECORD_AUDIO)
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean granted) {
                                // not record first time to request permission
                                if (granted) {
                                    Toast.makeText(getApplicationContext(), "Permission granted",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getApplicationContext(),
                                            "Permission not granted", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                throwable.printStackTrace();
                            }
                        });
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
                            mFileOutputStream.write(data, 0, size);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onError() {
                    mBtnStart.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Record fail",
                                    Toast.LENGTH_SHORT).show();
                            mBtnStart.setText("Start");
                            mIsRecording = false;
                        }
                    });
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        Observable.just(mOutputFile).subscribeOn(Schedulers.io()).subscribe(new Action1<File>() {
            @Override
            public void call(File file) {
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
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }

    @OnClick(R.id.mBtnPlayChanged)
    public void playChanged() {
        Observable.just(mOutputFile).subscribeOn(Schedulers.io()).subscribe(new Action1<File>() {
            @Override
            public void call(File file) {
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
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }
}
