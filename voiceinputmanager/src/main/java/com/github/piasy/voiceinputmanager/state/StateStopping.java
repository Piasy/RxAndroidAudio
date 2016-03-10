package com.github.piasy.voiceinputmanager.state;

/**
 * Created by Piasy{github.com/Piasy} on 3/10/16.
 */
final class StateStopping extends VoiceInputState {

    @Override
    public int state() {
        return STATE_STOPPING;
    }

    @Override
    public VoiceInputState pressed() {
        this.mPendingState = STATE_PREPARING;
        return this;
    }

    @Override
    public VoiceInputState released() {
        this.mPendingState = STATE_IDLE;
        return this;
    }

    @Override
    public VoiceInputState elapsed() {
        return new StateSending(this.mPendingState);
    }

    @Override
    public VoiceInputState quickReleased() {
        return new StateIdle();
    }

    @Override
    public String toString() {
        return "StateStopping";
    }
}
