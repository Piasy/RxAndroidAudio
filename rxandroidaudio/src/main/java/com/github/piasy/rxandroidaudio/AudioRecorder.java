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

import android.media.MediaRecorder;
import android.support.annotation.IntDef;
import android.support.annotation.WorkerThread;
import android.util.Log;
import java.io.File;
import java.io.IOException;

/**
 * Encapsulate {@link MediaRecorder},
 *
 * <em>NOTE: To avoid multi-thread call on native method which causing silent crash like:
 *
 * A/libc: Fatal signal 11 (SIGSEGV) at 0x00000010 (code=1), thread 9302 (RxComputationTh)
 *
 * this class simply use singleton and synchronized to keep thread safety.
 * refer to the comment under StackOverflow question:
 * http://stackoverflow.com/questions/14023291/fatal-signal-11-sigsegv-at-0x00000000-code-1-phonegap
 * </em>
 */
public final class AudioRecorder {
    private static final String TAG = "AudioRecorder";

    private static final int STOP_AUDIO_RECORD_DELAY_MILLIS = 300;
    public static final int DEFAULT_SAMPLE_RATE = 8000;
    public static final int DEFAULT_BIT_RATE = 16000;

    private AudioRecorder() {
        // singleton
    }

    private static class RxAndroidAudioHolder {
        private static final AudioRecorder INSTANCE = new AudioRecorder();
    }

    public static AudioRecorder getInstance() {
        return RxAndroidAudioHolder.INSTANCE;
    }

    private static final int STATE_IDLE = 0;
    private static final int STATE_PREPARED = 1;
    private static final int STATE_RECORDING = 2;

    private int mState = STATE_IDLE;

    public static final int ERROR_SDCARD_ACCESS = 1;
    public static final int ERROR_INTERNAL = 2;
    public static final int ERROR_NOT_PREPARED = 3;

    @IntDef(value = { ERROR_SDCARD_ACCESS, ERROR_INTERNAL, ERROR_NOT_PREPARED })
    public @interface Error {

    }

    public interface OnErrorListener {
        void onError(@Error int error);
    }

    private OnErrorListener mOnErrorListener;

    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public synchronized int getMaxAmplitude() {
        if (mState != STATE_RECORDING) {
            return 0;
        }
        return mRecorder.getMaxAmplitude();
    }

    private long mSampleStart = 0;       // time at which latest record or play operation started

    private MediaRecorder mRecorder;

    public int progress() {
        if (mState == STATE_RECORDING) {
            return (int) ((System.currentTimeMillis() - mSampleStart) / 1000);
        }
        return 0;
    }

    /**
     * Directly start record, including prepare and start.
     *
     * MediaRecorder.AudioSource.MIC
     * MediaRecorder.OutputFormat.MPEG_4
     * MediaRecorder.AudioEncoder.AAC
     */
    @WorkerThread
    public synchronized boolean startRecord(int audioSource, int outputFormat, int audioEncoder,
            File outputFile) {
        stopRecord();

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(audioSource);
        mRecorder.setOutputFormat(outputFormat);
        mRecorder.setAudioEncoder(audioEncoder);
        mRecorder.setOutputFile(outputFile.getAbsolutePath());

        // Handle IOException
        try {
            mRecorder.prepare();
        } catch (IOException exception) {
            Log.w(TAG, "startRecord fail, prepare fail: " + exception.getMessage());
            setError(ERROR_INTERNAL);
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            return false;
        }
        // Handle RuntimeException if the recording couldn't start
        try {
            mRecorder.start();
        } catch (RuntimeException exception) {
            Log.w(TAG, "startRecord fail, start fail: " + exception.getMessage());
            setError(ERROR_INTERNAL);
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            return false;
        }
        mSampleStart = System.currentTimeMillis();
        mState = STATE_RECORDING;
        return true;
    }

    /**
     * prepare for a new audio record, with default sample rate and bit rate.
     */
    @WorkerThread
    public synchronized boolean prepareRecord(int audioSource, int outputFormat, int audioEncoder,
            File outputFile) {
        return prepareRecord(audioSource, outputFormat, audioEncoder, DEFAULT_SAMPLE_RATE,
                DEFAULT_BIT_RATE, outputFile);
    }

    /**
     * prepare for a new audio record.
     */
    @WorkerThread
    public synchronized boolean prepareRecord(int audioSource, int outputFormat, int audioEncoder,
            int sampleRate, int bitRate, File outputFile) {
        stopRecord();

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(audioSource);
        mRecorder.setOutputFormat(outputFormat);
        mRecorder.setAudioSamplingRate(sampleRate);
        mRecorder.setAudioEncodingBitRate(bitRate);
        mRecorder.setAudioEncoder(audioEncoder);
        mRecorder.setOutputFile(outputFile.getAbsolutePath());

        // Handle IOException
        try {
            mRecorder.prepare();
        } catch (IOException exception) {
            Log.w(TAG, "startRecord fail, prepare fail: " + exception.getMessage());
            setError(ERROR_INTERNAL);
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            return false;
        }
        mState = STATE_PREPARED;
        return true;
    }

    /**
     * After prepared, start record now.
     */
    @WorkerThread
    public synchronized boolean startRecord() {
        if (mRecorder == null || mState != STATE_PREPARED) {
            setError(ERROR_NOT_PREPARED);
            return false;
        }
        // Handle RuntimeException if the recording couldn't start
        try {
            mRecorder.start();
        } catch (RuntimeException exception) {
            Log.w(TAG, "startRecord fail, start fail: " + exception.getMessage());
            setError(ERROR_INTERNAL);
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            return false;
        }
        mSampleStart = System.currentTimeMillis();
        mState = STATE_RECORDING;
        return true;
    }

    /**
     * stop record, and save audio file.
     *
     * @return record audio length in seconds, -1 if not a successful record.
     */
    @WorkerThread
    public synchronized int stopRecord() {
        if (mRecorder == null) {
            mState = STATE_IDLE;
            return -1;
        }

        int length = -1;
        switch (mState) {
            case STATE_RECORDING:
                try {
                    // seems to be a bug in Android's AAC based audio encoders
                    // ref: http://stackoverflow.com/a/24092524/3077508
                    Thread.sleep(STOP_AUDIO_RECORD_DELAY_MILLIS);
                    mRecorder.stop();
                    length = (int) ((System.currentTimeMillis() - mSampleStart) / 1000);
                } catch (RuntimeException e) {
                    Log.w(TAG, "stopRecord fail, stop fail(no audio data recorded): " +
                            e.getMessage());
                } catch (InterruptedException e) {
                    Log.w(TAG,
                            "stopRecord fail, stop fail(InterruptedException): " + e.getMessage());
                }
                // fall down
            case STATE_PREPARED:
                // fall down
            case STATE_IDLE:
                // fall down
            default:
                try {
                    mRecorder.reset();
                } catch (RuntimeException e) {
                    Log.w(TAG, "stopRecord fail, reset fail " + e.getMessage());
                }
                mRecorder.release();
                mRecorder = null;
                mState = STATE_IDLE;
                break;
        }

        return length;
    }

    private void setError(int error) {
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(error);
        }
    }
}
