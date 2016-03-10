package com.github.piasy.voiceinputmanager;

import com.github.piasy.rxandroidaudio.AudioRecorder;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Created by Piasy{github.com/Piasy} on 3/10/16.
 */
public class VoiceInputManagerTest {

    private AudioRecorder mAudioRecorder;
    private VoiceInputManager.EventListener mEventListener;
    private VoiceInputManager mVoiceInputManager;

    @Before
    public void setUp() {
        mAudioRecorder = mock(AudioRecorder.class);
        mEventListener = mock(VoiceInputManager.EventListener.class);
        mVoiceInputManager = new VoiceInputManager(mAudioRecorder, new File("."), mEventListener);
    }

    @After
    public void tearDown() {
        mVoiceInputManager.reset();
    }

    @Test
    public void testNormalRecord() {
        willReturn(true).given(mAudioRecorder)
                .prepareRecord(anyInt(), anyInt(), anyInt(), any(File.class));
        willReturn(true).given(mAudioRecorder).startRecord();
        willReturn(10).given(mAudioRecorder).stopRecord();

        mVoiceInputManager.toggleOn();
        sleep(500);
        mVoiceInputManager.toggleOff();
        sleep(500);

        then(mAudioRecorder).should(times(1))
                .prepareRecord(anyInt(), anyInt(), anyInt(), any(File.class));
        then(mAudioRecorder).should(times(1)).startRecord();
        then(mAudioRecorder).should(times(1)).stopRecord();

        then(mEventListener).should(times(1)).onPreparing();
        then(mEventListener).should(times(1)).onPrepared();
        then(mEventListener).should(times(1)).onStopped();
        then(mEventListener).should(atLeastOnce()).onAmplitudeChanged(anyInt());
        then(mEventListener).should(times(1)).onSend(any(File.class), anyInt());
        then(mEventListener).should(never()).onExpireCountdown(anyInt());
    }

    @Test
    public void testExpireCountdown() {
        willReturn(true).given(mAudioRecorder)
                .prepareRecord(anyInt(), anyInt(), anyInt(), any(File.class));
        willReturn(true).given(mAudioRecorder).startRecord();
        willReturn(14).given(mAudioRecorder).progress();
        willReturn(10).given(mAudioRecorder).stopRecord();

        mVoiceInputManager.toggleOn();
        sleep(500);
        mVoiceInputManager.toggleOff();
        sleep(500);

        then(mAudioRecorder).should(times(1))
                .prepareRecord(anyInt(), anyInt(), anyInt(), any(File.class));
        then(mAudioRecorder).should(times(1)).startRecord();
        then(mAudioRecorder).should(times(1)).stopRecord();

        then(mEventListener).should(times(1)).onPreparing();
        then(mEventListener).should(times(1)).onPrepared();
        then(mEventListener).should(times(1)).onStopped();
        then(mEventListener).should(atLeastOnce()).onAmplitudeChanged(anyInt());
        then(mEventListener).should(times(1)).onSend(any(File.class), anyInt());
        then(mEventListener).should(atLeastOnce()).onExpireCountdown(anyInt());
    }

    @Test
    public void testRecordTimeout() {
        willReturn(true).given(mAudioRecorder)
                .prepareRecord(anyInt(), anyInt(), anyInt(), any(File.class));
        willReturn(true).given(mAudioRecorder).startRecord();
        willReturn(15).given(mAudioRecorder).progress();
        willReturn(10).given(mAudioRecorder).stopRecord();

        mVoiceInputManager.toggleOn();
        sleep(500);

        then(mAudioRecorder).should(times(1))
                .prepareRecord(anyInt(), anyInt(), anyInt(), any(File.class));
        then(mAudioRecorder).should(times(1)).startRecord();
        then(mAudioRecorder).should(times(1)).stopRecord();

        then(mEventListener).should(times(1)).onPreparing();
        then(mEventListener).should(times(1)).onPrepared();
        then(mEventListener).should(times(1)).onStopped();
        then(mEventListener).should(atLeastOnce()).onAmplitudeChanged(anyInt());
        then(mEventListener).should(times(1)).onSend(any(File.class), anyInt());
        then(mEventListener).should(atLeastOnce()).onExpireCountdown(anyInt());
    }

    @Test
    public void testQuickRelease() {
        willReturn(true).given(mAudioRecorder)
                .prepareRecord(anyInt(), anyInt(), anyInt(), any(File.class));
        willReturn(true).given(mAudioRecorder).startRecord();
        willReturn(1).given(mAudioRecorder).stopRecord();

        mVoiceInputManager.toggleOn();
        sleep(500);
        mVoiceInputManager.toggleOff();
        sleep(500);

        then(mAudioRecorder).should(times(1))
                .prepareRecord(anyInt(), anyInt(), anyInt(), any(File.class));
        then(mAudioRecorder).should(times(1)).startRecord();
        then(mAudioRecorder).should(times(1)).stopRecord();

        then(mEventListener).should(times(1)).onPreparing();
        then(mEventListener).should(times(1)).onPrepared();
        then(mEventListener).should(times(1)).onStopped();
        then(mEventListener).should(atLeastOnce()).onAmplitudeChanged(anyInt());
        then(mEventListener).should(never()).onSend(any(File.class), anyInt());
        then(mEventListener).should(never()).onExpireCountdown(anyInt());
    }

    @Test
    public void testPending() {
        willReturn(true).given(mAudioRecorder)
                .prepareRecord(anyInt(), anyInt(), anyInt(), any(File.class));
        willReturn(true).given(mAudioRecorder).startRecord();
        willReturn(5).given(mAudioRecorder).stopRecord();

        mVoiceInputManager.toggleOn();
        sleep(500);
        mVoiceInputManager.toggleOff();
        sleep(500);
        mVoiceInputManager.toggleOn();
        sleep(500);

        then(mAudioRecorder).should(times(2))
                .prepareRecord(anyInt(), anyInt(), anyInt(), any(File.class));
        then(mAudioRecorder).should(times(2)).startRecord();
        then(mAudioRecorder).should(times(1)).stopRecord();

        then(mEventListener).should(times(2)).onPreparing();
        then(mEventListener).should(times(2)).onPrepared();
        then(mEventListener).should(times(1)).onStopped();
        then(mEventListener).should(atLeastOnce()).onAmplitudeChanged(anyInt());
        then(mEventListener).should(times(1)).onSend(any(File.class), anyInt());
        then(mEventListener).should(never()).onExpireCountdown(anyInt());
    }

    @Test
    public void testPendingCanceled() {
        willReturn(true).given(mAudioRecorder)
                .prepareRecord(anyInt(), anyInt(), anyInt(), any(File.class));
        willReturn(true).given(mAudioRecorder).startRecord();
        willReturn(5).given(mAudioRecorder).stopRecord();
        final CountDownLatch latch = new CountDownLatch(1);
        mEventListener = new VoiceInputManager.EventListener() {
            @Override
            public void onPreparing() {

            }

            @Override
            public void onPrepared() {

            }

            @Override
            public void onStopped() {

            }

            @Override
            public void onSend(File audioFile, int duration) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onAmplitudeChanged(int level) {

            }

            @Override
            public void onExpireCountdown(int second) {

            }
        };
        mVoiceInputManager = new VoiceInputManager(mAudioRecorder, new File("."), mEventListener);

        mVoiceInputManager.toggleOn();
        sleep(500);
        mVoiceInputManager.toggleOff();
        sleep(500);
        mVoiceInputManager.toggleOn();
        sleep(100);
        mVoiceInputManager.toggleOff();
        latch.countDown();

        then(mAudioRecorder).should(times(1))
                .prepareRecord(anyInt(), anyInt(), anyInt(), any(File.class));
        then(mAudioRecorder).should(times(1)).startRecord();
        then(mAudioRecorder).should(times(1)).stopRecord();
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
