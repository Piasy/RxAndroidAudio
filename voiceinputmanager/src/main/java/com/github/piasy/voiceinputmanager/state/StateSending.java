package com.github.piasy.voiceinputmanager.state;

/**
 * Created by Piasy{github.com/Piasy} on 3/10/16.
 */
final class StateSending extends VoiceInputState {

    StateSending(@State int pendingState) {
        super(pendingState);
    }

    @Override
    public int state() {
        return STATE_SENDING;
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
        if (mPendingState == STATE_PREPARING) {
            return new StatePreparing();
        } else {
            return new StateIdle();
        }
    }

    @Override
    public String toString() {
        return "StateSending";
    }
}
