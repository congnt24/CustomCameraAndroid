package com.altplus.democustomcamera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class CaptureResultActivity extends AppCompatActivity {

    ImageView capturedImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_result);
        capturedImage = (ImageView) findViewById(R.id.captured_image);
        if(getIntent().hasExtra("bitmap")) {
            Bitmap b = BitmapFactory.decodeByteArray(
                    getIntent().getByteArrayExtra("bitmap"),0,getIntent().getByteArrayExtra("bitmap").length);
            capturedImage.setImageBitmap(b);
        }
    }
}
