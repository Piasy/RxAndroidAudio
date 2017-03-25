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
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

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

    private MediaPlayer create(final PlayConfig config) throws IOException {
        stopPlay();

        MediaPlayer player;
        switch (config.mType) {
            case PlayConfig.TYPE_URI:
                Log.d(TAG, "MediaPlayer to start play uri: " + config.mUri);
                player = new MediaPlayer();
                player.setDataSource(config.mContext, config.mUri);
                return player;
            case PlayConfig.TYPE_FILE:
                Log.d(TAG, "MediaPlayer to start play file: " + config.mAudioFile.getName());
                player = new MediaPlayer();
                player.setDataSource(config.mAudioFile.getAbsolutePath());
                return player;
            case PlayConfig.TYPE_RES:
                Log.d(TAG, "MediaPlayer to start play: " + config.mAudioResource);
                player = MediaPlayer.create(config.mContext, config.mAudioResource);
                return player;
            case PlayConfig.TYPE_URL:
                Log.d(TAG, "MediaPlayer to start play: " + config.mUrl);
                player = new MediaPlayer();
                player.setDataSource(config.mUrl);
                return player;
            default:
                // can't happen, just fix checkstyle
                throw new IllegalArgumentException("Unknown type: " + config.mType);
        }
    }

    /**
     * play audio from local file. should be scheduled in IO thread.
     */
    public Observable<Boolean> play(@NonNull final PlayConfig config) {
        if (!config.isArgumentValid()) {
            return Observable.error(new IllegalArgumentException(""));
        }

        return Observable.<Boolean>create(emitter -> {
            MediaPlayer player = create(config);
            setMediaPlayerListener(player, emitter);
            player.setVolume(config.mLeftVolume, config.mRightVolume);
            player.setAudioStreamType(config.mStreamType);
            player.setLooping(config.mLooping);
            if (config.needPrepare()) {
                player.prepare();
            }
            emitter.onNext(true);

            player.start();
            mPlayer = player;
        }).doOnError(e -> stopPlay());
    }

    /**
     * prepare audio from local file. should be scheduled in IO thread.
     */
    public Observable<Boolean> prepare(@NonNull final PlayConfig config) {
        if (!config.isArgumentValid() || !config.isLocalSource()) {
            return Observable.error(new IllegalArgumentException(""));
        }

        return Observable.<Boolean>create(emitter -> {
            MediaPlayer player = create(config);
            setMediaPlayerListener(player, emitter);
            player.setVolume(config.mLeftVolume, config.mRightVolume);
            player.setAudioStreamType(config.mStreamType);
            player.setLooping(config.mLooping);
            if (config.needPrepare()) {
                player.prepare();
            }
            emitter.onNext(true);

            mPlayer = player;
        }).doOnError(e -> stopPlay());
    }

    public void pause() {
        mPlayer.pause();
    }

    public void resume() {
        mPlayer.start();
    }

    /**
     * Non reactive API.
     */
    @WorkerThread
    public boolean playNonRxy(@NonNull final PlayConfig config,
            final MediaPlayer.OnCompletionListener onCompletionListener,
            final MediaPlayer.OnErrorListener onErrorListener) {
        if (!config.isArgumentValid()) {
            return false;
        }

        try {
            MediaPlayer player = create(config);
            setMediaPlayerListener(player, onCompletionListener, onErrorListener);
            player.setVolume(config.mLeftVolume, config.mRightVolume);
            player.setAudioStreamType(config.mStreamType);
            player.setLooping(config.mLooping);
            if (config.needPrepare()) {
                player.prepare();
            }
            player.start();

            mPlayer = player;
            return true;
        } catch (RuntimeException | IOException e) {
            Log.w(TAG, "startPlay fail, IllegalArgumentException: " + e.getMessage());
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

    private void setMediaPlayerListener(final MediaPlayer player,
            final ObservableEmitter<Boolean> emitter) {
        player.setOnCompletionListener(mp -> {
            Log.d(TAG, "OnCompletionListener::onCompletion");

            // could not call stopPlay immediately, otherwise the second sound
            // could not play, thus no complete notification
            // TODO discover why?
            Observable.timer(50, TimeUnit.MILLISECONDS).subscribe(aLong -> {
                stopPlay();
                emitter.onComplete();
            }, emitter::onError);
        });
        player.setOnErrorListener((mp, what, extra) -> {
            Log.d(TAG, "OnErrorListener::onError" + what + ", " + extra);
            emitter.onError(new Throwable("Player error: " + what + ", " + extra));
            stopPlay();
            return true;
        });
    }

    private void setMediaPlayerListener(final MediaPlayer player,
            final MediaPlayer.OnCompletionListener onCompletionListener,
            final MediaPlayer.OnErrorListener onErrorListener) {
        player.setOnCompletionListener(mp -> {
            Log.d(TAG, "OnCompletionListener::onCompletion");

            // could not call stopPlay immediately, otherwise the second sound
            // could not play, thus no complete notification
            // TODO discover why?
            Observable.timer(50, TimeUnit.MILLISECONDS).subscribe(aLong -> {
                stopPlay();
                onCompletionListener.onCompletion(mp);
            }, throwable -> Log.d(TAG, "OnCompletionListener::onError, " + throwable.getMessage()));
        });
        player.setOnErrorListener((mp, what, extra) -> {
            Log.d(TAG, "OnErrorListener::onError" + what + ", " + extra);
            onErrorListener.onError(mp, what, extra);
            stopPlay();
            return true;
        });
    }

    private static class RxAudioPlayerHolder {
        private static final RxAudioPlayer INSTANCE = new RxAudioPlayer();
    }
}
