package com.github.piasy.voiceinputmanager.state;

/**
 * Created by Piasy{github.com/Piasy} on 3/10/16.
 */
final class StatePreparing extends VoiceInputState {

    @Override
    public int state() {
        return STATE_PREPARING;
    }

    @Override
    public VoiceInputState pressed() {
        return this;
    }

    @Override
    public VoiceInputState released() {
        return new StateIdle();
    }

    @Override
    public VoiceInputState elapsed() {
        return new StateRecording();
    }

    @Override
    public String toString() {
        return "StatePreparing";
    }
}
