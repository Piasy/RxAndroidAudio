package com.github.piasy.rxandroidaudio;

import android.content.Context;
import android.media.AudioManager;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.RawRes;

import java.io.File;

/**
 * Created by Piasy{github.com/Piasy} on 16/4/11.
 */
public class PlayConfig {
    static final int TYPE_FILE = 1;
    static final int TYPE_RES = 2;
    static final int TYPE_URL = 3;

    @IntDef(value = {TYPE_FILE, TYPE_RES,TYPE_URL})
    public @interface Type {
    }

    @PlayConfig.Type
    final int mType;

    final Context mContext;

    @RawRes
    final int mAudioResource;

    final File mAudioFile;

    final String mUrl;

    final int mStreamType;

    final boolean mLooping;

    @FloatRange(from = 0.0F, to = 1.0F)
    final float mLeftVolume;

    @FloatRange(from = 0.0F, to = 1.0F)
    final float mRightVolume;

    private PlayConfig(Builder builder) {
        mType = builder.mType;
        mContext = builder.mContext;
        mAudioResource = builder.mAudioResource;
        mAudioFile = builder.mAudioFile;
        mStreamType = builder.mStreamType;
        mLooping = builder.mLooping;
        mLeftVolume = builder.mLeftVolume;
        mRightVolume = builder.mRightVolume;
        mUrl = builder.mUrl;
    }

    public static Builder file(File file) {
        Builder builder = new Builder();
        builder.mAudioFile = file;
        builder.mType = TYPE_FILE;
        return builder;
    }

    public static Builder url(String url) {
        Builder builder = new Builder();
        builder.mUrl = url;
        builder.mType = TYPE_URL;
        return builder;
    }

    public static Builder res(Context context, @RawRes int audioResource) {
        Builder builder = new Builder();
        builder.mContext = context;
        builder.mAudioResource = audioResource;
        builder.mType = TYPE_RES;
        return builder;
    }

    public static class Builder {

        @PlayConfig.Type
        int mType;

        Context mContext;

        @RawRes
        int mAudioResource;

        File mAudioFile;

        String mUrl;

        int mStreamType = AudioManager.STREAM_MUSIC;

        boolean mLooping;

        @FloatRange(from = 0.0F, to = 1.0F)
        float mLeftVolume = 1.0F;

        @FloatRange(from = 0.0F, to = 1.0F)
        float mRightVolume = 1.0F;

        /**
         * {@link AudioManager.STREAM_VOICE_CALL} etc.
         */
        public Builder streamType(int streamType) {
            mStreamType = streamType;
            return this;
        }

        public Builder looping(boolean looping) {
            mLooping = looping;
            return this;
        }

        public Builder leftVolume(@FloatRange(from = 0.0F, to = 1.0F) float leftVolume) {
            mLeftVolume = leftVolume;
            return this;
        }

        public Builder rightVolume(@FloatRange(from = 0.0F, to = 1.0F) float rightVolume) {
            mRightVolume = rightVolume;
            return this;
        }

        public PlayConfig build() {
            return new PlayConfig(this);
        }
    }
}
