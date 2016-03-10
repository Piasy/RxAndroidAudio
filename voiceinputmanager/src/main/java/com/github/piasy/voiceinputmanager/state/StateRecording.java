package com.github.piasy.voiceinputmanager.state;

/**
 * Created by Piasy{github.com/Piasy} on 3/10/16.
 */
final class StateRecording extends VoiceInputState {

    @Override
    public int state() {
        return STATE_RECORDING;
    }

    @Override
    public VoiceInputState pressed() {
        return this;
    }

    @Override
    public VoiceInputState released() {
        return new StateStopping();
    }

    @Override
    public VoiceInputState elapsed() {
        return this;
    }

    @Override
    public VoiceInputState timeout() {
        return new StateStopping();
    }

    @Override
    public String toString() {
        return "StateRecording";
    }
}
