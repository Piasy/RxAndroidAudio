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

import android.content.Context;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;
import android.support.annotation.WorkerThread;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;
import rx.functions.Action1;

/**
 * Created by Piasy{github.com/Piasy} on 16/2/23.
 */
public final class RxAudioPlayer {

    static final String TAG = "RxAudioPlayer";

    MediaPlayer mPlayer;

    private RxAudioPlayer() {
        // singleton
    }

    private static class RxAudioPlayerHolder {
        private static final RxAudioPlayer INSTANCE = new RxAudioPlayer();
    }

    public static RxAudioPlayer getInstance() {
        return RxAudioPlayerHolder.INSTANCE;
    }

    /**
     * play audio from raw resource. should be scheduled in IO thread.
     */
    public Single<Boolean> play(final Context context, @RawRes final int audioRes) {
        return Single.create(new Single.OnSubscribe<Boolean>() {
            @Override
            public void call(final SingleSubscriber<? super Boolean> singleSubscriber) {
                stopPlay();

                Log.d(TAG, "MediaPlayer to start play: " + audioRes);
                mPlayer = MediaPlayer.create(context, audioRes);
                try {
                    mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            Log.d(TAG, "OnCompletionListener::onCompletion");

                            // could not call stopPlay immediately, otherwise the second sound
                            // could not play, thus no complete notification
                            // TODO discover why?
                            Observable.timer(50, TimeUnit.MILLISECONDS)
                                    .subscribe(new Action1<Long>() {
                                        @Override
                                        public void call(Long aLong) {
                                            stopPlay();
                                            singleSubscriber.onSuccess(true);
                                        }
                                    }, new Action1<Throwable>() {
                                        @Override
                                        public void call(Throwable throwable) {
                                            singleSubscriber.onError(throwable);
                                        }
                                    });
                        }
                    });
                    mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            Log.d(TAG, "OnErrorListener::onError" + what + ", " + extra);
                            singleSubscriber.onError(new Throwable("Player error: " + what + ", " +
                                    extra));
                            stopPlay();
                            return true;
                        }
                    });
                    mPlayer.setVolume(1.0F, 1.0F);
                    mPlayer.setLooping(false);
                    mPlayer.start();
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "startPlay fail, IllegalArgumentException: " + e.getMessage());
                    stopPlay();
                    singleSubscriber.onError(e);
                }
            }
        });
    }

    /**
     * play audio from local file. should be scheduled in IO thread.
     */
    public Single<Boolean> play(@NonNull final File audioFile) {
        return Single.create(new Single.OnSubscribe<Boolean>() {
            @Override
            public void call(final SingleSubscriber<? super Boolean> singleSubscriber) {
                stopPlay();

                Log.d(TAG, "MediaPlayer to start play: " + audioFile.getName());
                mPlayer = new MediaPlayer();
                try {
                    mPlayer.setDataSource(audioFile.getAbsolutePath());
                    mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            Log.d(TAG, "OnCompletionListener::onCompletion");

                            // could not call stopPlay immediately, otherwise the second sound
                            // could not play, thus no complete notification
                            // TODO discover why?
                            Observable.timer(50, TimeUnit.MILLISECONDS)
                                    .subscribe(new Action1<Long>() {
                                        @Override
                                        public void call(Long aLong) {
                                            stopPlay();
                                            singleSubscriber.onSuccess(true);
                                        }
                                    }, new Action1<Throwable>() {
                                        @Override
                                        public void call(Throwable throwable) {
                                            singleSubscriber.onError(throwable);
                                        }
                                    });
                        }
                    });
                    mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(MediaPlayer mp, int what, int extra) {
                            Log.d(TAG, "OnErrorListener::onError" + what + ", " + extra);
                            singleSubscriber.onError(new Throwable("Player error: " + what + ", " +
                                    extra));
                            stopPlay();
                            return true;
                        }
                    });
                    mPlayer.setVolume(1.0F, 1.0F);
                    mPlayer.setLooping(false);
                    mPlayer.prepare();
                    mPlayer.start();
                } catch (IllegalArgumentException | IOException e) {
                    Log.w(TAG, "startPlay fail, IllegalArgumentException: " + e.getMessage());
                    stopPlay();
                    singleSubscriber.onError(e);
                }
            }
        });
    }

    /**
     * Non reactive API.
     * */
    @WorkerThread
    public boolean playNonRxy(@NonNull final File audioFile,
            final MediaPlayer.OnCompletionListener onCompletionListener,
            final MediaPlayer.OnErrorListener onErrorListener) {
        stopPlay();

        Log.d(TAG, "MediaPlayer to start play: " + audioFile.getName());
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(audioFile.getAbsolutePath());
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(final MediaPlayer mp) {
                    Log.d(TAG, "OnCompletionListener::onCompletion");

                    // could not call stopPlay immediately, otherwise the second sound
                    // could not play, thus no complete notification
                    // TODO discover why?
                    Observable.timer(50, TimeUnit.MILLISECONDS)
                            .subscribe(new Action1<Long>() {
                                @Override
                                public void call(Long aLong) {
                                    stopPlay();
                                    onCompletionListener.onCompletion(mp);
                                }
                            }, new Action1<Throwable>() {
                                @Override
                                public void call(Throwable throwable) {
                                    Log.d(TAG, "OnCompletionListener::onError, " +
                                            throwable.getMessage());
                                }
                            });
                }
            });
            mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.d(TAG, "OnErrorListener::onError" + what + ", " + extra);
                    onErrorListener.onError(mp, what, extra);
                    stopPlay();
                    return true;
                }
            });
            mPlayer.setVolume(1.0F, 1.0F);
            mPlayer.setLooping(false);
            mPlayer.prepare();
            mPlayer.start();
            return true;
        } catch (IllegalArgumentException | IOException e) {
            Log.w(TAG, "startPlay fail, IllegalArgumentException: " + e.getMessage());
            stopPlay();
            return false;
        }
    }

    /**
     * Non reactive API.
     * */
    @WorkerThread
    public boolean playNonRxy(final Context context, @RawRes final int audioRes,
            final MediaPlayer.OnCompletionListener onCompletionListener,
            final MediaPlayer.OnErrorListener onErrorListener) {
        stopPlay();

        Log.d(TAG, "MediaPlayer to start play: " + audioRes);
        mPlayer = MediaPlayer.create(context, audioRes);
        try {
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(final MediaPlayer mp) {
                    Log.d(TAG, "OnCompletionListener::onCompletion");

                    // could not call stopPlay immediately, otherwise the second sound
                    // could not play, thus no complete notification
                    // TODO discover why?
                    Observable.timer(50, TimeUnit.MILLISECONDS)
                            .subscribe(new Action1<Long>() {
                                @Override
                                public void call(Long aLong) {
                                    stopPlay();
                                    onCompletionListener.onCompletion(mp);
                                }
                            }, new Action1<Throwable>() {
                                @Override
                                public void call(Throwable throwable) {
                                    Log.d(TAG, "OnCompletionListener::onError, " + throwable.getMessage());
                                }
                            });
                }
            });
            mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.d(TAG, "OnErrorListener::onError" + what + ", " + extra);
                    onErrorListener.onError(mp, what, extra);
                    stopPlay();
                    return true;
                }
            });
            mPlayer.setVolume(1.0F, 1.0F);
            mPlayer.setLooping(false);
            mPlayer.start();
            return true;
        } catch (IllegalStateException e) {
            Log.w(TAG, "startPlay fail, IllegalStateException: " + e.getMessage());
            stopPlay();
            return false;
        }
    }

    public synchronized boolean stopPlay() {
        if (mPlayer == null) {
            return false;
        }

        mPlayer.setOnCompletionListener(null);
        mPlayer.setOnErrorListener(null);
        try {
            mPlayer.stop();
            mPlayer.reset();
            mPlayer.release();
        } catch (IllegalStateException e) {
            Log.w(TAG, "stopPlay fail, IllegalStateException: " + e.getMessage());
        }
        mPlayer = null;
        return true;
    }
}
