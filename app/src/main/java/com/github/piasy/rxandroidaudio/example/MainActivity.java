package com.github.piasy.rxandroidaudio.example;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.mBtnFileMode)
    public void fileMode() {
        startActivity(new Intent(this, FileActivity.class));
    }

    @OnClick(R.id.mBtnStreamMode)
    public void streamMode() {
        startActivity(new Intent(this, StreamActivity.class));
    }
}
