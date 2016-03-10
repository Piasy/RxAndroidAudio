package com.github.piasy.voiceinputmanager;

import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;
import com.github.piasy.rxandroidaudio.AudioRecorder;
import com.github.piasy.rxandroidaudio.RxAmplitude;
import com.github.piasy.voiceinputmanager.state.VoiceInputState;
import java.io.File;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by Piasy{github.com/Piasy} on 16/2/29.
 */
public final class VoiceInputManager {
    private static final String TAG = "RecorderStateManager";

    private static final int DEFAULT_MIN_AUDIO_LENGTH_SECONDS = 2;
    private static final int DEFAULT_MAX_AUDIO_LENGTH_SECONDS = 15;

    private static Action1<Throwable> sOnErrorLogger = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            throwable.printStackTrace();
        }
    };

    private AudioRecorder mAudioRecorder;
    private final File mAudioFilesDir;
    private EventListener mEventListener;

    private final int mMinAudioLengthSeconds;
    private final int mMaxAudioLengthSeconds;

    private File mAudioFile;

    private VoiceInputState mVoiceInputState;

    private VoiceInputManager(AudioRecorder audioRecorder, File audioFilesDir,
            EventListener eventListener, int minAudioLengthSeconds, int maxAudioLengthSeconds) {
        mAudioRecorder = audioRecorder;
        mAudioFilesDir = audioFilesDir;
        mEventListener = eventListener;
        mMinAudioLengthSeconds = minAudioLengthSeconds;
        mMaxAudioLengthSeconds = maxAudioLengthSeconds;
        mVoiceInputState = VoiceInputState.init();
    }

    private static volatile VoiceInputManager sRecorderStateManager;

    public static VoiceInputManager manage(@NonNull AudioRecorder audioRecorder,
            @NonNull File audioFilesDir, @NonNull EventListener eventListener) {
        return manage(audioRecorder, audioFilesDir, eventListener, DEFAULT_MIN_AUDIO_LENGTH_SECONDS,
                DEFAULT_MAX_AUDIO_LENGTH_SECONDS);
    }

    public static VoiceInputManager manage(@NonNull AudioRecorder audioRecorder,
            @NonNull File audioFilesDir, @NonNull EventListener eventListener,
            int minAudioLengthSeconds, int maxAudioLengthSeconds) {
        if (sRecorderStateManager == null) {
            synchronized (VoiceInputManager.class) {
                if (sRecorderStateManager == null) {
                    sRecorderStateManager =
                            new VoiceInputManager(audioRecorder, audioFilesDir, eventListener,
                                    minAudioLengthSeconds, maxAudioLengthSeconds);
                }
            }
        }
        sRecorderStateManager.mAudioRecorder = audioRecorder;
        sRecorderStateManager.mEventListener = eventListener;
        return sRecorderStateManager;
    }

    public interface EventListener {
        @WorkerThread
        void onPreparing();

        @WorkerThread
        void onPrepared();

        @WorkerThread
        void onStopped();

        /**
         * This method is called in worker thread, and should send audio file sync.
         */
        @WorkerThread
        void onSend(File audioFile, int duration);

        @WorkerThread
        void onAmplitudeChanged(int level);

        @WorkerThread
        void onExpireCountdown(int second);
    }

    public void toggleOn() {
        Log.d(TAG, "toggleOn @ " + System.currentTimeMillis());
        addSubscribe(Observable.just(true)
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        pressed();
                        if (state() == VoiceInputState.STATE_PREPARING) {
                            startRecord();
                        }
                    }
                }, sOnErrorLogger));
    }

    private synchronized void pressed() {
        Log.d(TAG, "before pressed " + mVoiceInputState + " @ " + System.currentTimeMillis());
        mVoiceInputState = mVoiceInputState.pressed();
        Log.d(TAG, "after pressed " + mVoiceInputState + " @ " + System.currentTimeMillis());
    }

    private synchronized void elapsed() {
        Log.d(TAG, "before elapsed " + mVoiceInputState + " @ " + System.currentTimeMillis());
        mVoiceInputState = mVoiceInputState.elapsed();
        Log.d(TAG, "after elapsed " + mVoiceInputState + " @ " + System.currentTimeMillis());
    }

    private synchronized void released() {
        Log.d(TAG, "before released " + mVoiceInputState + " @ " + System.currentTimeMillis());
        mVoiceInputState = mVoiceInputState.released();
        Log.d(TAG, "after released " + mVoiceInputState + " @ " + System.currentTimeMillis());
    }

    private synchronized void quickReleased() {
        Log.d(TAG, "before quickReleased " + mVoiceInputState + " @ " + System.currentTimeMillis());
        mVoiceInputState = mVoiceInputState.quickReleased();
        Log.d(TAG, "after quickReleased " + mVoiceInputState + " @ " + System.currentTimeMillis());
    }

    private synchronized void timeout() {
        Log.d(TAG, "before timeout " + mVoiceInputState + " @ " + System.currentTimeMillis());
        mVoiceInputState = mVoiceInputState.timeout();
        Log.d(TAG, "after timeout " + mVoiceInputState + " @ " + System.currentTimeMillis());
    }

    private synchronized void resetState() {
        Log.d(TAG, "before resetState " + mVoiceInputState + " @ " + System.currentTimeMillis());
        mVoiceInputState = VoiceInputState.init();
        Log.d(TAG, "after resetState " + mVoiceInputState + " @ " + System.currentTimeMillis());
    }

    @VoiceInputState.State
    private synchronized int state() {
        return mVoiceInputState.state();
    }

    private void startRecord() {
        mEventListener.onPreparing();
        mAudioFile = new File(mAudioFilesDir.getAbsolutePath() +
                File.separator + System.currentTimeMillis() + ".file.m4a");
        boolean prepared = mAudioRecorder.prepareRecord(MediaRecorder.AudioSource.MIC,
                MediaRecorder.OutputFormat.MPEG_4, MediaRecorder.AudioEncoder.AAC, mAudioFile);
        if (!prepared) {
            reset();
            return;
        }

        mEventListener.onPrepared();
        boolean started = mAudioRecorder.startRecord();
        if (!started) {
            reset();
            return;
        }

        elapsed();
        if (state() == VoiceInputState.STATE_RECORDING) {
            addSubscribe(RxAmplitude.from(mAudioRecorder)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Action1<Integer>() {
                        @Override
                        public void call(Integer level) {
                            int progress = mAudioRecorder.progress();
                            mEventListener.onAmplitudeChanged(level);

                            if (progress >= mMaxAudioLengthSeconds - 3) {
                                mEventListener.onExpireCountdown(mMaxAudioLengthSeconds - progress);
                                if (progress == mMaxAudioLengthSeconds) {
                                    timeout();
                                    if (state() == VoiceInputState.STATE_STOPPING) {
                                        stopRecord();
                                    }
                                }
                            }
                        }
                    }, sOnErrorLogger));
        }
    }

    public void reset() {
        unSubscribeAll();
        resetState();
    }

    public void toggleOff() {
        Log.d(TAG, "toggleOff @ " + System.currentTimeMillis());
        released();
        int state = state();
        if (state == VoiceInputState.STATE_STOPPING) {
            stopRecord();
        } else if (state == VoiceInputState.STATE_IDLE) {
            addSubscribe(Observable.just(true)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean aBoolean) {
                            mEventListener.onStopped();
                        }
                    }, sOnErrorLogger));
        }
    }

    private void stopRecord() {
        addSubscribe(Observable.just(true)
                .subscribeOn(Schedulers.io())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        int seconds = mAudioRecorder.stopRecord();
                        mEventListener.onStopped();
                        if (seconds >= mMinAudioLengthSeconds) {
                            elapsed();
                            if (state() == VoiceInputState.STATE_SENDING) {
                                mEventListener.onSend(mAudioFile, seconds);
                                elapsed();
                                if (state() == VoiceInputState.STATE_PREPARING) {
                                    startRecord();
                                }
                            }
                        } else {
                            quickReleased();
                        }
                    }
                }, sOnErrorLogger));
    }

    private CompositeSubscription mCompositeSubscription;

    private void addSubscribe(Subscription subscription) {
        if (mCompositeSubscription == null || mCompositeSubscription.isUnsubscribed()) {
            mCompositeSubscription = new CompositeSubscription();
        }
        mCompositeSubscription.add(subscription);
    }

    private void unSubscribeAll() {
        if (mCompositeSubscription != null && !mCompositeSubscription.isUnsubscribed()) {
            mCompositeSubscription.unsubscribe();
        }
        mCompositeSubscription = null;
    }
}
