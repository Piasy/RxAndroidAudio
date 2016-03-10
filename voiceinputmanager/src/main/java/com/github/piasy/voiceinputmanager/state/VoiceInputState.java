package com.github.piasy.voiceinputmanager.state;

import android.support.annotation.IntDef;

/**
 * Created by Piasy{github.com/Piasy} on 3/10/16.
 */
public abstract class VoiceInputState {

    public static final int STATE_IDLE = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_RECORDING = 2;
    public static final int STATE_STOPPING = 3;
    public static final int STATE_SENDING = 4;

    protected VoiceInputState() {
        this(STATE_IDLE);
    }

    protected VoiceInputState(@State int pendingState) {
        mPendingState = pendingState;
    }

    public static VoiceInputState init() {
        return new StateIdle();
    }

    @IntDef(value = {
            STATE_IDLE, STATE_PREPARING, STATE_RECORDING, STATE_STOPPING,
            STATE_SENDING
    })
    public @interface State {}

    @State
    protected int mPendingState;

    @State
    public abstract int state();

    public abstract VoiceInputState pressed();

    public abstract VoiceInputState released();

    public abstract VoiceInputState elapsed();

    public VoiceInputState quickReleased() {
        return this;
    }

    public VoiceInputState timeout() {
        return this;
    }
}
