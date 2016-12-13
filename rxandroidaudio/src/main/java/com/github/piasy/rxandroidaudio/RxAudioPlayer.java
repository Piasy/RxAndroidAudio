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

import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;
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

    private static final String TAG = "RxAudioPlayer";

    private MediaPlayer mPlayer;

    private RxAudioPlayer() {
        // singleton
    }

    public static RxAudioPlayer getInstance() {
        return RxAudioPlayerHolder.INSTANCE;
    }

    /**
     * play audio from local file. should be scheduled in IO thread.
     */
    public Single<Boolean> play(@NonNull final PlayConfig config) {
        if (!config.isArgumentValid()) {
            return Single.error(new IllegalArgumentException(""));
        }
        switch (config.mType) {
            case PlayConfig.TYPE_FILE:
                return Single.create(new Single.OnSubscribe<Boolean>() {
                    @Override
                    public void call(final SingleSubscriber<? super Boolean> singleSubscriber) {
                        stopPlay();

                        Log.d(TAG, "MediaPlayer to start play: " + config.mAudioFile.getName());
                        mPlayer = new MediaPlayer();
                        try {
                            mPlayer.setDataSource(config.mAudioFile.getAbsolutePath());
                            setMediaPlayerListener(singleSubscriber);
                            mPlayer.setVolume(config.mLeftVolume, config.mRightVolume);
                            mPlayer.setAudioStreamType(config.mStreamType);
                            mPlayer.setLooping(config.mLooping);
                            mPlayer.prepare();
                            mPlayer.start();
                        } catch (IllegalArgumentException | IOException e) {
                            Log.w(TAG, "startPlay fail, IllegalArgumentException: "
                                       + e.getMessage());
                            stopPlay();
                            singleSubscriber.onError(e);
                        }
                    }
                });
            case PlayConfig.TYPE_RES:
                return Single.create(new Single.OnSubscribe<Boolean>() {
                    @Override
                    public void call(final SingleSubscriber<? super Boolean> singleSubscriber) {
                        stopPlay();

                        Log.d(TAG, "MediaPlayer to start play: " + config.mAudioResource);
                        mPlayer = MediaPlayer.create(config.mContext, config.mAudioResource);
                        try {
                            setMediaPlayerListener(singleSubscriber);
                            mPlayer.setVolume(config.mLeftVolume, config.mRightVolume);
                            mPlayer.setLooping(config.mLooping);
                            mPlayer.start();
                        } catch (IllegalArgumentException e) {
                            Log.w(TAG, "startPlay fail, IllegalArgumentException: "
                                       + e.getMessage());
                            stopPlay();
                            singleSubscriber.onError(e);
                        }
                    }
                });
            case PlayConfig.TYPE_URL:
                return Single.create(new Single.OnSubscribe<Boolean>() {
                    @Override
                    public void call(final SingleSubscriber<? super Boolean> singleSubscriber) {
                        stopPlay();

                        Log.d(TAG, "MediaPlayer to start play: " + config.mUrl);
                        mPlayer = new MediaPlayer();
                        try {
                            mPlayer.setDataSource(config.mUrl);
                            setMediaPlayerListener(singleSubscriber);
                            mPlayer.setVolume(config.mLeftVolume, config.mRightVolume);
                            mPlayer.setAudioStreamType(config.mStreamType);
                            mPlayer.setLooping(config.mLooping);
                            mPlayer.prepare();
                            mPlayer.start();
                        } catch (IllegalArgumentException | IOException e) {
                            Log.w(TAG, "startPlay fail, IllegalArgumentException: "
                                       + e.getMessage());
                            stopPlay();
                            singleSubscriber.onError(e);
                        }
                    }
                });
            default:
                // can't happen, just fix checkstyle
                return Single.error(new IllegalArgumentException(""));
        }
    }

    /**
     * Non reactive API.
     */
    @WorkerThread
    public boolean playNonRxy(@NonNull final PlayConfig config,
            final MediaPlayer.OnCompletionListener onCompletionListener,
            final MediaPlayer.OnErrorListener onErrorListener) {
        stopPlay();

        if (!config.isArgumentValid()) {
            return false;
        }
        switch (config.mType) {
            case PlayConfig.TYPE_FILE:
                Log.d(TAG, "MediaPlayer to start play: " + config.mAudioFile.getName());
                mPlayer = new MediaPlayer();
                try {
                    mPlayer.setDataSource(config.mAudioFile.getAbsolutePath());
                    setMediaPlayerListener(onCompletionListener, onErrorListener);
                    mPlayer.setVolume(config.mLeftVolume, config.mRightVolume);
                    mPlayer.setAudioStreamType(config.mStreamType);
                    mPlayer.setLooping(config.mLooping);
                    mPlayer.prepare();
                    mPlayer.start();
                    return true;
                } catch (IllegalArgumentException | IOException e) {
                    Log.w(TAG, "startPlay fail, IllegalArgumentException: " + e.getMessage());
                    stopPlay();
                    return false;
                }
            case PlayConfig.TYPE_RES:
                Log.d(TAG, "MediaPlayer to start play: " + config.mAudioResource);
                mPlayer = MediaPlayer.create(config.mContext, config.mAudioResource);
                try {
                    setMediaPlayerListener(onCompletionListener, onErrorListener);
                    mPlayer.setVolume(config.mLeftVolume, config.mRightVolume);
                    mPlayer.setLooping(config.mLooping);
                    mPlayer.start();
                    return true;
                } catch (IllegalStateException e) {
                    Log.w(TAG, "startPlay fail, IllegalStateException: " + e.getMessage());
                    stopPlay();
                    return false;
                }
            case PlayConfig.TYPE_URL:
                Log.d(TAG, "MediaPlayer to start play: " + config.mUrl);
                mPlayer = new MediaPlayer();
                try {
                    mPlayer.setDataSource(config.mUrl);
                    setMediaPlayerListener(onCompletionListener, onErrorListener);
                    mPlayer.setVolume(config.mLeftVolume, config.mRightVolume);
                    mPlayer.setAudioStreamType(config.mStreamType);
                    mPlayer.setLooping(config.mLooping);
                    mPlayer.prepare();
                    mPlayer.start();
                    return true;
                } catch (IllegalArgumentException | IOException e) {
                    Log.w(TAG, "startPlay fail, IllegalArgumentException: " + e.getMessage());
                    stopPlay();
                    return false;
                }
            default:
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

    public int progress() {
        if (mPlayer != null) {
            return mPlayer.getCurrentPosition() / 1000;
        }
        return 0;
    }

    /**
     * allow further customized manipulation.
     */
    public MediaPlayer getMediaPlayer() {
        return mPlayer;
    }

    private void setMediaPlayerListener(final SingleSubscriber<? super Boolean> singleSubscriber) {
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d(TAG, "OnCompletionListener::onCompletion");

                // could not call stopPlay immediately, otherwise the second sound
                // could not play, thus no complete notification
                // TODO discover why?
                Observable.timer(50, TimeUnit.MILLISECONDS).subscribe(new Action1<Long>() {
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
    }

    private void setMediaPlayerListener(final MediaPlayer.OnCompletionListener onCompletionListener,
            final MediaPlayer.OnErrorListener onErrorListener) {
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(final MediaPlayer mp) {
                Log.d(TAG, "OnCompletionListener::onCompletion");

                // could not call stopPlay immediately, otherwise the second sound
                // could not play, thus no complete notification
                // TODO discover why?
                Observable.timer(50, TimeUnit.MILLISECONDS).subscribe(new Action1<Long>() {
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
    }

    private static class RxAudioPlayerHolder {
        private static final RxAudioPlayer INSTANCE = new RxAudioPlayer();
    }
}
