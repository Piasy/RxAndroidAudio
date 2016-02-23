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

import android.support.annotation.NonNull;
import android.util.Log;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.functions.Func1;

/**
 * Created by Piasy{github.com/Piasy} on 16/2/22.
 *
 * Get amplitude level in Rx way, max level is 8.
 */
public final class RxAmplitude {

    static final String TAG = "RxAmplitude";

    private static final int DEFAULT_AMPLITUDE_INTERVAL = 200;

    // on i9300, max value is 16385
    static final int AMPLITUDE_MAX_VALUE = 16385;
    static final int AMPLITUDE_MAX_LEVEL = 8;

    final Random mRandom;

    private RxAmplitude() {
        mRandom = new Random(System.nanoTime());
    }

    public static Observable<Integer> from(@NonNull AudioRecorder audioRecorder) {
        return from(audioRecorder, DEFAULT_AMPLITUDE_INTERVAL);
    }

    public static Observable<Integer> from(@NonNull final AudioRecorder audioRecorder, long interval) {
        return new RxAmplitude().start(audioRecorder, interval);
    }

    private Observable<Integer> start(@NonNull final AudioRecorder audioRecorder, long interval) {
        return Observable.interval(interval, TimeUnit.MILLISECONDS)
                .map(new Func1<Long, Integer>() {
                    @Override
                    public Integer call(Long aLong) {
                        int amplitude;
                        try {
                            amplitude = audioRecorder.getMaxAmplitude();
                        } catch (RuntimeException e) {
                            Log.i(TAG, "getMaxAmplitude fail: " + e.getMessage());
                            amplitude = mRandom.nextInt(AMPLITUDE_MAX_VALUE);
                        }
                        amplitude = amplitude / (AMPLITUDE_MAX_VALUE / AMPLITUDE_MAX_LEVEL);
                        return amplitude;
                    }
                });
    }
}
