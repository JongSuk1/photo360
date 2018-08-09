package com.example.kakao.myapplication;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.SensorEvent;
import android.media.ExifInterface;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ViewImage extends AppCompatActivity implements View.OnTouchListener {

    private GyroObserver gyroObserver;
    private final double STORED_IMAGE = -1.0;
    private PanoImageView panoImageView;
    private float touchX;
    private float tmpX;
    private float mv=0;


    private float inertia_move;
    private float move;

    private Handler viewHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_image);


        double MaxRotateRadian = getIntent().getDoubleExtra("MaxRotateRadian",STORED_IMAGE);

        if (MaxRotateRadian == STORED_IMAGE){
            String filepath = getIntent().getStringExtra("filepath");
            try {
                ExifInterface exif = new ExifInterface(filepath);
                String tmp_fov = exif.getAttribute(ExifInterface.TAG_COPYRIGHT);
                if(tmp_fov == null){
                    MaxRotateRadian = Math.PI/9;
                    Log.d("it doesn't have","horizontal value");
                }
                else{
                    MaxRotateRadian = Double.parseDouble(exif.getAttribute(ExifInterface.TAG_COPYRIGHT));
                    Log.d("it has","horizontal value");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        MaxRotateRadian = Math.min(MaxRotateRadian, Math.PI);
        boolean WrapAroundView = false;

        if(MaxRotateRadian == Math.PI){
            // change viewer mode to wrap-around
            WrapAroundView = true;
        }
        gyroObserver = new GyroObserver();
        gyroObserver.setMaxRotateRadian(MaxRotateRadian);
        Log.d("horizontal fov : ",String.valueOf(MaxRotateRadian));

        panoImageView = (PanoImageView) findViewById(R.id.panorama_image_view);
        panoImageView.setgyroObserver(gyroObserver);
        panoImageView.setWrapAroundView(WrapAroundView);
        panoImageView.setOnTouchListener(ViewImage.this);
        panoImageView.setClickable(true);

        FileInputStream is = null;
        try {
            is = this.openFileInput("image");
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if(WrapAroundView){// 360뷰어를 만들기 위해 기존 이미지를 3장 복제해서 올린다
                Bitmap bmp360 = Bitmap.createBitmap(bmp.getWidth()*3, bmp.getHeight(), bmp.getConfig());
                Canvas canvas = new Canvas(bmp360);
                for(int i = 0 ; i < 3; i ++){
                    canvas.drawBitmap(bmp, bmp.getWidth() * i, 0, null);
                }
                bmp = bmp360;
            }
            panoImageView.setImageBitmap(bmp);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        gyroObserver.register(this);
        viewHandler = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {
                super.handleMessage(msg);
                float motion = Float.parseFloat((String)msg.obj);
                panoImageView.updateProgress(motion);
            }
        };

    }

    @Override
    protected void onPause() {
        super.onPause();
        gyroObserver.unregister();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            Log.d("TAG", "OnTouch : ACTION_DOWN");
            touchX = event.getX();
            inertia_move = 0f;
        } else if (action == MotionEvent.ACTION_UP) {
            Log.d("TAG", "OnTouch : ACTION_UP");
            MotionThread motionThread = new MotionThread(inertia_move);
            motionThread.start();

        } else if (action == MotionEvent.ACTION_MOVE) {
            Log.d("TAG", "OnTouch : ACTION_MOVE");
            tmpX = event.getX();
            move = (tmpX-touchX)*0.0005f;

            if(move > 0){
                inertia_move = inertia_move > move? inertia_move : move;
            }
            else{
                inertia_move = inertia_move < move? inertia_move : move;
            }
            panoImageView.updateProgress(move);
            touchX = tmpX;

        }

        return true;
    }

    public class MotionThread extends Thread{
        private float motion = 0f;

        public MotionThread(float mv){
            this.motion = mv;
        }

        @Override
        public void run() {
            int count = 0;
            // move during 2.5s
            while(count < 5000){
                // 메시지 얻어오기
                android.os.Message msg = viewHandler.obtainMessage();
                // 메시지 ID, arg1, arg2 설정은 의미 없습니다 메시지의 obj 성분만 mv로 사용합니다
                String str_motion = String.valueOf(this.motion);
                msg.obj = str_motion;
                viewHandler.sendMessage(msg);
                // update mv with drag motion
                this.motion *= 0.8;
                try {
                    Thread.sleep(50);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
