# RxAndroidAudio
Android Audio encapsulation library, with part Rx support.

## Usage
Add to gradle dependency of your module build.gradle:

```gradle
repositories {
    jcenter()
}

dependencies {
    compile 'com.github.piasy:rxandroidaudio:1.0.0'
}
```

Use in code:

Record to file:

```java
mAudioRecorder = AudioRecorder.getInstance();
mAudioFile = new File(
        Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + System.nanoTime() + ".file.m4a");
mAudioRecorder.prepareRecord(MediaRecorder.AudioSource.MIC,
        MediaRecorder.OutputFormat.MPEG_4, MediaRecorder.AudioEncoder.AAC,
        mAudioFile);
// ...
mAudioRecorder.stopRecord();
```

Play a file:

```java
mRxAudioPlayer.play(audioFile)
        .subscribeOn(Schedulers.io())
        .subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean aBoolean) {
                // play finished
            }
        });
```

Record a stream:

```java
mOutputFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
        File.separator + System.nanoTime() + ".stream.m4a");
mOutputFile.createNewFile();
mFileOutputStream = new FileOutputStream(mOutputFile);
mStreamAudioRecorder.start(new StreamAudioRecorder.AudioDataCallback() {
    @Override
    public void onAudioData(byte[] data, int size) {
        if (mFileOutputStream != null) {
            try {
                mFileOutputStream.write(data, 0, size);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onError() {
        mBtnStart.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Record fail",
                        Toast.LENGTH_SHORT).show();
                mBtnStart.setText("Start");
                mIsRecording = false;
            }
        });
    }
});
```

Play a stream:

```java
Observable.just(mOutputFile).subscribeOn(Schedulers.io()).subscribe(new Action1<File>() {
    @Override
    public void call(File file) {
        try {
            mStreamAudioPlayer.init();
            FileInputStream inputStream = new FileInputStream(file);
            int read;
            while ((read = inputStream.read(mBuffer)) > 0) {
                mStreamAudioPlayer.play(mBuffer, read);
            }
            inputStream.close();
            mStreamAudioPlayer.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
});
```

See [full example](https://github.com/Piasy/RxAndroidAudio/tree/master/app) for more details.

[Download demo apk](https://www.pgyer.com/rsyU).

## Dev tips
+  You need create an empty file named `bintray.properties` under root project dir, which is used for uploading artifact to bintray.
