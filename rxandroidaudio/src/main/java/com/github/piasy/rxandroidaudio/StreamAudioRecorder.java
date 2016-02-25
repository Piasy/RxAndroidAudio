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

package com.github.piasy.rxandroidaudio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Piasy{github.com/Piasy} on 16/2/24.
 *
 * <em>NOTE: users should only have one instance active at the same time.</em>
 */
public final class StreamAudioRecorder {
    private static final String TAG = "StreamAudioRecorder";
    public static final int DEFAULT_SAMPLE_RATE = 16000;
    public static final int DEFAULT_BUFFER_SIZE = 2048;

    private ExecutorService mExecutorService;
    final AtomicBoolean mIsRecording;

    private StreamAudioRecorder() {
        // singleton
        mIsRecording = new AtomicBoolean(false);
    }

    private static final class StreamAudioRecorderHolder {
        private static final StreamAudioRecorder INSTANCE = new StreamAudioRecorder();
    }

    public static StreamAudioRecorder getInstance() {
        return StreamAudioRecorderHolder.INSTANCE;
    }

    /**
     * Although Android frameworks jni implementation are the same for ENCODING_PCM_16BIT and
     * ENCODING_PCM_8BIT, the Java doc declared that the buffer type should be the corresponding
     * type, so we use different ways.
     */
    public interface AudioDataCallback {
        @WorkerThread
        void onAudioData(byte[] data, int size);

        void onError();
    }

    private class AudioRecordRunnable implements Runnable {

        private final AudioRecord mAudioRecord;
        private final AudioDataCallback mAudioDataCallback;

        private final byte[] mByteBuffer;
        private final short[] mShortBuffer;
        private final int mByteBufferSize;
        private final int mShortBufferSize;
        private final int mAudioFormat;

        AudioRecordRunnable(int sampleRate, int channelConfig, int audioFormat, int byteBufferSize,
                @NonNull AudioDataCallback audioDataCallback) {
            mAudioFormat = audioFormat;
            int minBufferSize =
                    AudioRecord.getMinBufferSize(sampleRate, channelConfig, mAudioFormat);
            mByteBufferSize = Math.max(byteBufferSize, minBufferSize);
            mShortBufferSize = mByteBufferSize / 2;
            mByteBuffer = new byte[mByteBufferSize];
            mShortBuffer = new short[mShortBufferSize];
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig,
                    audioFormat, mByteBufferSize);
            mAudioDataCallback = audioDataCallback;
        }

        @Override
        public void run() {
            if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                try {
                    mAudioRecord.startRecording();
                } catch (IllegalStateException e) {
                    Log.w(TAG, "startRecording fail: " + e.getMessage());
                    mAudioDataCallback.onError();
                    return;
                }
                while (mIsRecording.get()) {
                    int ret;
                    if (mAudioFormat == AudioFormat.ENCODING_PCM_16BIT) {
                        ret = mAudioRecord.read(mShortBuffer, 0, mShortBufferSize);
                        if (ret > 0) {
                            mAudioDataCallback.onAudioData(
                                    short2byte(mShortBuffer, ret, mByteBuffer), ret * 2);
                        } else {
                            onError(ret);
                            break;
                        }
                    } else {
                        ret = mAudioRecord.read(mByteBuffer, 0, mByteBufferSize);
                        if (ret > 0) {
                            mAudioDataCallback.onAudioData(mByteBuffer, ret);
                        } else {
                            onError(ret);
                            break;
                        }
                    }
                }
            }
            mAudioRecord.release();
        }

        private byte[] short2byte(short[] sData, int size, byte[] bData) {
            if (size >= sData.length || size * 2 >= bData.length) {
                Log.w(TAG, "short2byte: too long short data array");
            }
            for (int i = 0; i < size; i++) {
                bData[i * 2] = (byte) (sData[i] & 0x00FF);
                bData[(i * 2) + 1] = (byte) (sData[i] >> 8);
            }
            return bData;
        }

        private void onError(int errorCode) {
            if (errorCode == AudioRecord.ERROR_INVALID_OPERATION) {
                Log.w(TAG, "record fail: ERROR_INVALID_OPERATION");
                mAudioDataCallback.onError();
            } else if (errorCode == AudioRecord.ERROR_BAD_VALUE) {
                Log.w(TAG, "record fail: ERROR_BAD_VALUE");
                mAudioDataCallback.onError();
            }
        }
    }

    public synchronized boolean start(@NonNull AudioDataCallback audioDataCallback) {
        return start(DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, DEFAULT_BUFFER_SIZE, audioDataCallback);
    }

    /**
     * AudioFormat.CHANNEL_IN_MONO
     * AudioFormat.ENCODING_PCM_16BIT
     */
    public synchronized boolean start(int sampleRate, int channelConfig, int audioFormat,
            int bufferSize, @NonNull AudioDataCallback audioDataCallback) {
        stop();

        mExecutorService = Executors.newSingleThreadExecutor();
        if (mIsRecording.compareAndSet(false, true)) {
            mExecutorService.execute(
                    new AudioRecordRunnable(sampleRate, channelConfig, audioFormat, bufferSize,
                            audioDataCallback));
            return true;
        }
        return false;
    }

    public synchronized void stop() {
        mIsRecording.compareAndSet(true, false);

        if (mExecutorService != null) {
            mExecutorService.shutdown();
            mExecutorService = null;
        }
    }
}
