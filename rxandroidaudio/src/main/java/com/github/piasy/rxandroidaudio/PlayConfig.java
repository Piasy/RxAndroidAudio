package com.github.piasy.rxandroidaudio;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.annotation.RawRes;
import android.text.TextUtils;
import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Piasy{github.com/Piasy} on 16/4/11.
 */
public class PlayConfig {
    static final int TYPE_FILE = 1;
    static final int TYPE_RES = 2;
    static final int TYPE_URL = 3;
    static final int TYPE_URI = 4;

    @PlayConfig.Type
    final int mType;

    final Context mContext;

    @RawRes
    final int mAudioResource;

    final Uri mUri;

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
        mUri = builder.mUri;
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

    public static Builder uri(Context context, Uri uri) {
        Builder builder = new Builder();
        builder.mContext = context;
        builder.mUri = uri;
        builder.mType = TYPE_URI;
        return builder;
    }

    boolean isArgumentValid() {
        switch (mType) {
            case TYPE_FILE:
                return mAudioFile != null && mAudioFile.exists();
            case TYPE_RES:
                return mAudioResource > 0 && mContext != null;
            case TYPE_URL:
                return !TextUtils.isEmpty(mUrl);
            case TYPE_URI:
                return mUri != null;
            default:
                return false;
        }
    }

    boolean isLocalSource() {
        switch (mType) {
            case PlayConfig.TYPE_FILE:
            case PlayConfig.TYPE_RES:
                return true;
            case PlayConfig.TYPE_URL:
            case PlayConfig.TYPE_URI:
            default:
                return false;
        }
    }

    boolean needPrepare() {
        switch (mType) {
            case PlayConfig.TYPE_FILE:
            case PlayConfig.TYPE_URL:
            case PlayConfig.TYPE_URI:
                return true;
            case PlayConfig.TYPE_RES:
            default:
                return false;
        }
    }

    @IntDef(value = { TYPE_FILE, TYPE_RES, TYPE_URL, TYPE_URI })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
    }

    public static class Builder {

        @PlayConfig.Type
        int mType;

        Context mContext;

        @RawRes
        int mAudioResource;

        Uri mUri;

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
