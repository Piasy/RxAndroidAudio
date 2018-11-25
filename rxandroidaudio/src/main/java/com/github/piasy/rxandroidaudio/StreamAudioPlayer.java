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
import android.media.AudioManager;
import android.media.AudioTrack;
import androidx.annotation.WorkerThread;
import android.util.Log;

/**
 * Created by Piasy{github.com/Piasy} on 16/2/24.
 *
 * <em>NOTE: users should only have one instance active at the same time.</em>
 */
public final class StreamAudioPlayer {
    private static final String TAG = "StreamAudioPlayer";
    public static final int DEFAULT_SAMPLE_RATE = 44100;

    private AudioTrack mAudioTrack;

    private StreamAudioPlayer() {
        // singleton
    }

    private static final class StreamAudioPlayerHolder {
        private static final StreamAudioPlayer INSTANCE = new StreamAudioPlayer();
    }

    public static StreamAudioPlayer getInstance() {
        return StreamAudioPlayerHolder.INSTANCE;
    }

    public synchronized void init() {
        init(DEFAULT_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                StreamAudioRecorder.DEFAULT_BUFFER_SIZE);
    }

    /**
     * AudioFormat.CHANNEL_OUT_MONO
     * AudioFormat.ENCODING_PCM_16BIT
     *
     * @param bufferSize user may want to write data larger than minBufferSize, so they should able
     * to increase it
     */
    public synchronized void init(int sampleRate, int channelConfig, int audioFormat,
            int bufferSize) {
        if (mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
        }
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        mAudioTrack =
                new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig, audioFormat,
                        Math.max(minBufferSize, bufferSize), AudioTrack.MODE_STREAM);
        mAudioTrack.play();
    }

    @WorkerThread
    public synchronized boolean play(byte[] data, int size) {
        if (mAudioTrack != null) {
            try {
                int ret = mAudioTrack.write(data, 0, size);
                switch (ret) {
                    case AudioTrack.ERROR_INVALID_OPERATION:
                        Log.w(TAG, "play fail: ERROR_INVALID_OPERATION");
                        return false;
                    case AudioTrack.ERROR_BAD_VALUE:
                        Log.w(TAG, "play fail: ERROR_BAD_VALUE");
                        return false;
                    case AudioManager.ERROR_DEAD_OBJECT:
                        Log.w(TAG, "play fail: ERROR_DEAD_OBJECT");
                        return false;
                    default:
                        return true;
                }
            } catch (IllegalStateException e) {
                Log.w(TAG, "play fail: " + e.getMessage());
                return false;
            }
        }
        Log.w(TAG, "play fail: null mAudioTrack");
        return false;
    }

    public synchronized void release() {
        if (mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
        }
    }
}
