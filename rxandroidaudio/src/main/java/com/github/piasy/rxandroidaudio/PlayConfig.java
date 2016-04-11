package com.github.piasy.rxandroidaudio;

import android.content.Context;
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

    @IntDef(value = { TYPE_FILE, TYPE_RES })
    public @interface Type {}

    @PlayConfig.Type
    final int mType;

    final Context mContext;

    @RawRes
    final int mAudioResource;

    final File mAudioFile;

    final boolean mLooping;

    @FloatRange(from = 0.0F, to = 1.0F)
    final float mLeftVolume;

    @FloatRange(from = 0.0F, to = 1.0F)
    final float mRightVolume;

    private PlayConfig(int type, Context context, int audioResource, File audioFile,
            boolean looping, float leftVolume, float rightVolume) {
        mType = type;
        mContext = context;
        mAudioResource = audioResource;
        mAudioFile = audioFile;
        mLooping = looping;
        mLeftVolume = leftVolume;
        mRightVolume = rightVolume;
    }


    public static Builder file(File file) {
        Builder builder = new Builder();
        builder.mAudioFile = file;
        builder.mType = TYPE_FILE;
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

        boolean mLooping;

        @FloatRange(from = 0.0F, to = 1.0F)
        float mLeftVolume = 1.0F;

        @FloatRange(from = 0.0F, to = 1.0F)
        float mRightVolume = 1.0F;

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
            return new PlayConfig(mType, mContext, mAudioResource, mAudioFile, mLooping,
                    mLeftVolume, mRightVolume);
        }
    }
}
