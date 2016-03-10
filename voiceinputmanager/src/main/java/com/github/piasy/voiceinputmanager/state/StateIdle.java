package com.github.piasy.voiceinputmanager.state;

/**
 * Created by Piasy{github.com/Piasy} on 3/10/16.
 */
final class StateIdle extends VoiceInputState {

    @Override
    public int state() {
        return STATE_IDLE;
    }

    @Override
    public VoiceInputState pressed() {
        return new StatePreparing();
    }

    @Override
    public VoiceInputState released() {
        return this;
    }

    @Override
    public VoiceInputState elapsed() {
        return this;
    }

    @Override
    public String toString() {
        return "StateIdle";
    }
}
