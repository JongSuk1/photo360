package com.example.kakao.myapplication;

import android.Manifest;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import Jama.*;

public class CaptureImage extends AppCompatActivity {


    private TextureView textureView;

    //Check state orientation of output image
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static{
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }

    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CameraCaptureSession.CaptureCallback mSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }
    };
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private ImageView guideImage_init = null;
    private ImageView guideImageL = null;
    private ImageView guideImageR = null;
    private final int guideImage_width = 150;
    private final int guideImage_height = 150;


    private ImageButton mButton_capture;
    private ImageView FrameImageView;


    private int capture_width = 640;
    private int capture_height = 480;
    private float camera_sensor_width;
    private float camera_sensor_height;
    private float focal_length;
    private float disp_width;
    private float disp_height;

    //Save to FILE
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private static Handler CaptureButtonHandler;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private SensorEventListener deviceAngleEventListener;

    private final float[] mRotationMatrix = new float[16];
    private float[] mQuaternion = new float[5];
    private float[] interpolatedQuaternion = new float[5];
    private float[] prevQuaternion = new float[5];
    private float[] guideImageL_pos_realCoord = new float[4];
    private float[] guideImageR_pos_realCoord = new float[4];

    private double[][] guideImageL_pos_phoneCoord_2 = new double[4][1];
    private double[][] guideImageR_pos_phoneCoord_2 = new double[4][1];
    private double[][] initial_principalAxis = new double[4][1];
    private double[][] mRotationMatrix_2 ;
    private double[][] guideImageL_pos_realCoord_2 ;
    private double[][] guideImageR_pos_realCoord_2 ;
    private double[][] Sky_phoneCoord_2 = new double[4][1];
    private final double[][] Sky_realCoord_2 = new double[4][1];
    private double[][] Xaxis_phoneCoord_2 = new double[4][1];

    private float[] fov=new float[2];
    private double imageL_yaw_displacement;
    private double imageL_yaw_displacement_interval = Math.PI / 10;

    private double imageR_yaw_displacement;
    private double imageR_yaw_displacement_interval = 2*Math.PI - Math.PI / 10;
    private ArrayList<Bitmap> imageArrayList;

    private final float DIST_IN_RANGE = 30 * 30;
    int image_in_frame_count = 0;
    int imageL_in_frame_count = 0;
    int imageR_in_frame_count = 0;
    private long take_picture_tic=0;
    private boolean isTaken = true;
    private boolean isTilted = false;
    private boolean isTiltToastMsgShown = false;
    private static final int IMAGE_ARRAY_FULL = 100;

    private static final int TAKE_INIT = -1;
    private static final int TAKE_LEFT = 0;
    private static final int TAKE_RIGHT = 1;
    private int TAKE_MODE;


    class takePictureThread extends Thread{
        @Override
        public void run(){
            try {
                takePicture();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };




    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            cameraDevice=null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_image);
        CaptureButtonHandler = new Handler(){
            @Override
            public void handleMessage(android.os.Message msg){
                super.handleMessage(msg);
                mButton_capture.setScaleType(ImageView.ScaleType.FIT_XY);
                switch(msg.what) {
                    case 1:
                        mButton_capture.setImageDrawable(getResources().getDrawable(R.drawable.capture_10));
                        break;
                    case 2:
                        mButton_capture.setImageDrawable(getResources().getDrawable(R.drawable.capture_20));
                        break;
                    case 3:
                        mButton_capture.setImageDrawable(getResources().getDrawable(R.drawable.capture_30));
                        break;
                    case 4:
                        mButton_capture.setImageDrawable(getResources().getDrawable(R.drawable.capture_40));
                        break;
                    case 5:
                        mButton_capture.setImageDrawable(getResources().getDrawable(R.drawable.capture_50));
                        break;
                    case 6:
                        mButton_capture.setImageDrawable(getResources().getDrawable(R.drawable.capture_60));
                        break;
                    case 7:
                        mButton_capture.setImageDrawable(getResources().getDrawable(R.drawable.capture_70));
                        break;
                    case 8:
                        mButton_capture.setImageDrawable(getResources().getDrawable(R.drawable.capture_80));
                        break;
                    case 9:
                        mButton_capture.setImageDrawable(getResources().getDrawable(R.drawable.capture_90));
                        break;
                    case 10:
                        mButton_capture.setImageDrawable(getResources().getDrawable(R.drawable.capture_100));
                        break;
                    case IMAGE_ARRAY_FULL:
                        try{
                            View view = getLayoutInflater().from(CaptureImage.this).inflate(R.layout.activity_capture_image,null);
                            CaptureFinish(view);
                        }
                        catch (IOException e){
                            e.printStackTrace();
                        }
                    default:
                        mButton_capture.setImageDrawable(getResources().getDrawable(R.drawable.capture_100));
                        break;
                }
            }

        };


        calcCameraParam((CameraManager) getSystemService(Context.CAMERA_SERVICE));
        FrameImageView = (ImageView) findViewById(R.id.imageView);

        mButton_capture = (ImageButton) findViewById(R.id.btnCapture);
        mButton_capture.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (imageArrayList.isEmpty()) {

                } else {
                    View view = getLayoutInflater().from(CaptureImage.this).inflate(R.layout.activity_capture_image,null);
                    try {
                        CaptureFinish(view);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });




        textureView = (TextureView)findViewById(R.id.textureView);
        //From Java 1.4 , you can use keyword 'assert' to check expression true or false
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        disp_width = metrics.widthPixels;
        disp_height = metrics.heightPixels;
        Log.d("ApplicationTagName", "Display width in px is " + String.valueOf(metrics.widthPixels));
        Log.d("ApplicationTagName", "Display height in px is " + String.valueOf(metrics.heightPixels));

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if(mSensor == null){
            Toast.makeText(this, "This device cannot support rotationvectorsensor!", Toast.LENGTH_SHORT).show();
            finish();
        }

        imageArrayList = new ArrayList<Bitmap>();
        init_matrices();


        deviceAngleEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (mSensor != null) {
                    mQuaternion = sensorEvent.values;
                    SensorManager.getRotationMatrixFromVector(
                            mRotationMatrix, mQuaternion);
                    mRotationMatrix_2 = reShape(mRotationMatrix, 4, 4);
                    for(int i = 0 ; i < 4 ; i ++){
                        Log.d("L " + String.valueOf(i) + "->", String.valueOf(guideImageL_pos_realCoord[i]));
                        Log.d("R " + String.valueOf(i) + "->", String.valueOf(guideImageR_pos_realCoord[i]));
                    }
                    guideImageL_pos_realCoord_2 = reShape(guideImageL_pos_realCoord, 4, 1);
                    guideImageR_pos_realCoord_2 = reShape(guideImageR_pos_realCoord, 4, 1);
                    tiltAlarmNotify(-15, 15);

                    if(imageArrayList.size() > 0){

                        if(guideImage_init != null){
                            RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.myLayout);
                            relativeLayout.removeView(guideImage_init);
                            guideImage_init = null;
                        }
                        /*
                        if(guideImageL ==null)
                        {
                            RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.myLayout);
                            guideImageL = new ImageView(getApplicationContext());
                            guideImageL.setImageDrawable(getResources().getDrawable(R.drawable.mycon));
                            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                            relativeLayout.addView(guideImageL, layoutParams);
                            guideImageL.getLayoutParams().width = guideImage_width;
                            guideImageL.getLayoutParams().height = guideImage_height;
                            guideImageL.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

                            guideImageL.requestLayout();

                        }
                        */

                        if(guideImageR ==null)
                        {
                            RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.myLayout);
                            guideImageR = new ImageView(getApplicationContext());
                            guideImageR.setImageDrawable(getResources().getDrawable(R.drawable.mycon));
                            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                            relativeLayout.addView(guideImageR, layoutParams);
                            guideImageR.getLayoutParams().width = guideImage_width;
                            guideImageR.getLayoutParams().height = guideImage_height;
                            guideImageR.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                            guideImageR.requestLayout();
                        }


                        //guideImageL_pos_phoneCoord_2 = ((new Matrix(mRotationMatrix_2).transpose())).times(new Matrix(guideImageL_pos_realCoord_2)).getArray();
                        guideImageR_pos_phoneCoord_2 = ((new Matrix(mRotationMatrix_2).transpose())).times(new Matrix(guideImageR_pos_realCoord_2)).getArray();
                        //int x_cam_L = (int) (focal_length * guideImageL_pos_phoneCoord_2[0][0] / guideImageL_pos_phoneCoord_2[2][0] * disp_width / camera_sensor_width);
                        //int y_cam_L = (int) (focal_length * guideImageL_pos_phoneCoord_2[1][0] / guideImageL_pos_phoneCoord_2[2][0] * disp_height / camera_sensor_height);
                        int x_cam_R = (int) (focal_length * guideImageR_pos_phoneCoord_2[0][0] / guideImageR_pos_phoneCoord_2[2][0] * disp_width / camera_sensor_width);
                        int y_cam_R = (int) (focal_length * guideImageR_pos_phoneCoord_2[1][0] / guideImageR_pos_phoneCoord_2[2][0] * disp_height / camera_sensor_height);
                        //setImage(guideImageL, x_cam_L, y_cam_L);
                        setImage(guideImageR, x_cam_R, y_cam_R);

                        //double distL = (x_cam_L) * (x_cam_L)+ (y_cam_L) * (y_cam_L);
                        double distR = (x_cam_R) * (x_cam_R)+ (y_cam_R) * (y_cam_R);
                        /*if(distL < DIST_IN_RANGE){
                            if(imageL_in_frame_count!=-1)
                                imageL_in_frame_count++;
                        }
                        else{
                            imageL_in_frame_count = 0;
                        }*/

                        if(distR < DIST_IN_RANGE){
                            if(imageR_in_frame_count!=-1)
                                imageR_in_frame_count++;
                        }
                        else{
                            imageR_in_frame_count = 0;
                        }
/*
                        if(imageL_in_frame_count==0){
                            guideImageL.setImageDrawable(getResources().getDrawable(R.drawable.mycon));
                        }
                        else if(imageL_in_frame_count==1){
                            guideImageL.setImageDrawable(getResources().getDrawable(R.drawable.loading_image_1));
                        }
                        else if(imageL_in_frame_count==2){
                            guideImageL.setImageDrawable(getResources().getDrawable(R.drawable.loading_image_2));
                        }
                        else if(imageL_in_frame_count==3){
                            guideImageL.setImageDrawable(getResources().getDrawable(R.drawable.loading_image_3));
                        }
                        else if(imageL_in_frame_count==4){
                            imageL_in_frame_count = -1;
                            if(!isTilted){
                                TAKE_MODE = TAKE_LEFT;
                                takePictureThread takePictureThread = new takePictureThread();
                                takePictureThread.start();
                                guideImageL.setImageDrawable(getResources().getDrawable(R.drawable.loading_image_4));
                            }
                            else{
                                imageL_in_frame_count = 0;
                                guideImageL.setImageDrawable(getResources().getDrawable(R.drawable.mycon));
                            }
                        }
                        else{
                            //do nothing
                        }
*/
                        if(imageR_in_frame_count==0){
                            guideImageR.setImageDrawable(getResources().getDrawable(R.drawable.mycon));
                        }
                        else if(imageR_in_frame_count==1){
                            guideImageR.setImageDrawable(getResources().getDrawable(R.drawable.loading_image_1));
                        }
                        else if(imageR_in_frame_count==2){
                            guideImageR.setImageDrawable(getResources().getDrawable(R.drawable.loading_image_2));
                        }
                        else if(imageR_in_frame_count==3){
                            guideImageR.setImageDrawable(getResources().getDrawable(R.drawable.loading_image_3));
                        }
                        else if(imageR_in_frame_count==4){
                            imageR_in_frame_count = -1;
                            if(!isTilted ){
                                TAKE_MODE = TAKE_RIGHT;
                                takePictureThread takePictureThread = new takePictureThread();
                                takePictureThread.start();
                                guideImageR.setImageDrawable(getResources().getDrawable(R.drawable.loading_image_4));
                            }
                            else{
                                imageR_in_frame_count = 0;
                                guideImageR.setImageDrawable(getResources().getDrawable(R.drawable.mycon));
                            }
                        }
                        else{
                            //do nothing
                        }
                    }
                    // imageSize == 0
                    else{
                        if (guideImage_init == null) {
                            RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.myLayout);
                            guideImage_init = new ImageView(getApplicationContext());
                            guideImage_init.setImageDrawable(getResources().getDrawable(R.drawable.mycon));
                            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                            relativeLayout.addView(guideImage_init, layoutParams);
                            guideImage_init.getLayoutParams().width = guideImage_width;
                            guideImage_init.getLayoutParams().height = guideImage_height;
                            guideImage_init.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                            guideImage_init.requestLayout();
                        }

                        double[][] initCaptureGuide = new double[4][1];
                        double[][] initCaptureGuide_real = new double[4][1];
                        double[][] initCaptureGuide_phone = new double[4][1];

                        initCaptureGuide[0][0] = 0;
                        initCaptureGuide[1][0] = 0;
                        initCaptureGuide[2][0] = 1;
                        initCaptureGuide[3][0] = 0;

                        initCaptureGuide_real = ((new Matrix(mRotationMatrix_2))).times(new Matrix(initCaptureGuide)).getArray();
                        initCaptureGuide_real[2][0] = 0;

                        initCaptureGuide_phone = ((new Matrix(mRotationMatrix_2).transpose())).times(new Matrix(initCaptureGuide_real)).getArray();

                        int x_cam_init = (int) (focal_length * initCaptureGuide_phone[0][0] / initCaptureGuide_phone[2][0] * disp_width / camera_sensor_width);
                        int y_cam_init = (int) (focal_length * initCaptureGuide_phone[1][0] / initCaptureGuide_phone[2][0] * disp_height / camera_sensor_height);

                        setImage(guideImage_init, x_cam_init, y_cam_init);
                        Log.d("x", String.valueOf(x_cam_init));
                        Log.d("y", String.valueOf(y_cam_init));

                        double dist = (x_cam_init) * (x_cam_init)+ (y_cam_init) * (y_cam_init);
                        if(dist < DIST_IN_RANGE){
                            if(image_in_frame_count!=-1)
                                image_in_frame_count++;
                        }
                        else{
                            image_in_frame_count = 0;
                        }

                        if(image_in_frame_count==0){
                            guideImage_init.setImageDrawable(getResources().getDrawable(R.drawable.mycon));
                        }
                        else if(image_in_frame_count==1){
                            guideImage_init.setImageDrawable(getResources().getDrawable(R.drawable.loading_image_1));
                        }
                        else if(image_in_frame_count==2){
                            guideImage_init.setImageDrawable(getResources().getDrawable(R.drawable.loading_image_2));
                        }
                        else if(image_in_frame_count==3){
                            guideImage_init.setImageDrawable(getResources().getDrawable(R.drawable.loading_image_3));
                        }
                        else if(image_in_frame_count==4){
                            image_in_frame_count = -1;
                            if(!isTilted){
                                TAKE_MODE = TAKE_INIT;
                                takePictureThread takePictureThread = new takePictureThread();
                                takePictureThread.start();
                                guideImage_init.setImageDrawable(getResources().getDrawable(R.drawable.loading_image_4));
                            }
                            else{
                                image_in_frame_count = 0;
                                guideImage_init.setImageDrawable(getResources().getDrawable(R.drawable.mycon));
                            }
                        }
                        else{
                            //do nothing
                        }

                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) { }
        };


    }


    /**
     * @param maxAngle 화면이 기울어지는 최대 각도
     * @param minAngle 화면이 기울어지는 최소 각도
     *  화면이 기울어지면 ((min, max) 벗어나면)Toast msg 로 알려줌
     */
    private void tiltAlarmNotify(int minAngle, int maxAngle) {
        Sky_phoneCoord_2 = ((new Matrix(mRotationMatrix_2)).transpose()).times(new Matrix(Sky_realCoord_2)).getArray();
        double tiltedAngle = radToDeg(Math.atan(Sky_phoneCoord_2[0][0] / Sky_phoneCoord_2[1][0]));
        Toast tiltNotifyToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        tiltNotifyToast.setText("화면이 옆으로 기울어져 있음");
        if (tiltedAngle > maxAngle || tiltedAngle < minAngle) {
            isTilted = true;
            if(! isTiltToastMsgShown){
                tiltNotifyToast.show();
                isTiltToastMsgShown = true;
            }
        }
        else {
            isTilted = false;
            if(isTiltToastMsgShown){
                tiltNotifyToast.cancel();
                isTiltToastMsgShown = false;
            }
        }
    }


    private double radToDeg(double v) {
        return v*180.0/Math.PI;
    }


    public void CaptureFinish(View view) throws IOException{

        textureView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);


        FrameImageView.setImageDrawable(getResources().getDrawable(R.drawable.stitching_pageloading));
        android.view.ViewGroup.LayoutParams layoutParams = FrameImageView.getLayoutParams();
        layoutParams.width = 900;
        layoutParams.height = 600;
        FrameImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameImageView.setLayoutParams(layoutParams);

        Intent intent = new Intent(this, StitchImage.class);
        for(int i =0 ; i<imageArrayList.size() ; i++){
            String filename = "image"+i+".jpg";
            FileOutputStream stream = this.openFileOutput(filename, Context.MODE_PRIVATE);
            imageArrayList.get(i).compress(Bitmap.CompressFormat.JPEG,100,stream);
            stream.close();
            intent.putExtra("image"+i , filename);
        }
        intent.putExtra("imagenum",imageArrayList.size());

        double MaxRotateRadian = (fov[0]+imageL_yaw_displacement_interval*(imageArrayList.size()-1))/2;
        intent.putExtra("MaxRotateRadian",MaxRotateRadian);
        startActivity(intent);
    }

    /**
     *  matrix 초기화하는 메소드
     */

    private void init_matrices(){
        mRotationMatrix[ 0] = 1;
        mRotationMatrix[ 4] = 1;
        mRotationMatrix[ 8] = 1;
        mRotationMatrix[12] = 1;

        Sky_realCoord_2[0][0] = 0;
        Sky_realCoord_2[1][0] = 0;
        Sky_realCoord_2[2][0] = 1;
        Sky_realCoord_2[3][0] = 0;

        Xaxis_phoneCoord_2[0][0] = 0;
        Xaxis_phoneCoord_2[1][0] = 0;
        Xaxis_phoneCoord_2[2][0] = 1;
        Xaxis_phoneCoord_2[3][0] = 0;
    }

    private void update_guideImage_position(float[] guideImage_pos, double theta){
            guideImage_pos[0] = (float)(Math.cos(theta) * initial_principalAxis[0][0] - Math.sin(theta) * initial_principalAxis[1][0]) ;
            guideImage_pos[1] = (float)(Math.sin(theta) * initial_principalAxis[0][0] + Math.cos(theta) * initial_principalAxis[1][0]) ;
            guideImage_pos[2] = (float)initial_principalAxis[2][0];
            guideImage_pos[3] = (float)initial_principalAxis[3][0];
    }


    /**
     * 화면 가운데를 중심으로 (x, y) 픽셀 위치에 이미지를 위치시키는 메소드
     * @param x_pixel 가로픽셀
     * @param y_pixel 세로픽셀
     */
    private void setImage(ImageView imageView, int x_pixel, int y_pixel){
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) imageView.getLayoutParams();
        layoutParams.leftMargin = -x_pixel + (int)disp_width/2 - guideImage_width/2; // 휴대폰 x 축 방향이 왼쪽 방향
        layoutParams.topMargin = +y_pixel + (int)disp_height/2 - guideImage_height/2;
        imageView.setLayoutParams(layoutParams);
    }

    /**
     * 픽셀을 DP 로 변환하는 메소드.
     * @param px 픽셀
     * @return 픽셀에서 dp 로 변환된 값.
     */
    private int pxToDp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }

    /**
     * DP 를 픽셀로 변환하는 메소드.
     * @param dp dp
     * @return dp 에서 변환된 픽셀 값.
     */
    private int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    private double[][] reShape(float[] mRotationMatrix, int row, int column) {
        double[][] reShapedMat = new double[row][column];
        for(int i=0; i<row; i++){
            for(int j=0; j<column; j++){
                reShapedMat[i][j] = mRotationMatrix[i*column+j];
            }
        }
        return reShapedMat;
    }

    public void takePicture() throws IOException{
        if(System.currentTimeMillis() - take_picture_tic < 1000 || !isTaken){
            return;
        }
        isTaken = false;
        take_picture_tic = System.currentTimeMillis();
        Log.d("take picture", String.valueOf(imageArrayList.size()));
        if(cameraDevice == null)
            return;
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if(characteristics != null)
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);

            //Capture image with custom size
            if(jpegSizes != null && jpegSizes.length > 0)
            {
                capture_width = jpegSizes[0].getWidth();
                capture_height = jpegSizes[0].getHeight();
            }
            final ImageReader reader = ImageReader.newInstance(capture_width, capture_height,ImageFormat.JPEG,1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(new Surface(textureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = null;
                    try{
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        {
                            if(image != null)
                                image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes,0, bytes.length);
                    bmp = Bitmap.createScaledBitmap(bmp, 1024, 768, false);

                    android.graphics.Matrix matrix = new android.graphics.Matrix();
                    matrix.postRotate(90);
                    Bitmap rotatedbmp = Bitmap.createBitmap(bmp,0,0,bmp.getWidth(),bmp.getHeight(),matrix, false);
                    Log.d("TAKE_MODE", String.valueOf(TAKE_MODE));
                    switch(TAKE_MODE){
                        case TAKE_INIT:
                            imageArrayList.add(rotatedbmp);
                            break;
                        case TAKE_LEFT:
                            // insert element to the leftmost part of arraylist
                            imageArrayList.add(0, rotatedbmp);
                            break;
                        case TAKE_RIGHT:
                            imageArrayList.add(rotatedbmp);
                            break;
                        default:
                            Log.e("TAKEMODE error","");
                            break;
                    }

                    if(imageArrayList.size()==21){
                        CaptureButtonHandler.sendEmptyMessage(IMAGE_ARRAY_FULL);
                    }
                }
            };

            reader.setOnImageAvailableListener(readerListener,mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    createCameraPreview();
                    isTaken = true;

                    int imageNum = imageArrayList.size();
                    /*
                    if(imageNum == 1){
                        initial_principalAxis = (new Matrix(mRotationMatrix_2)).times(new Matrix(Xaxis_phoneCoord_2)).getArray();
                    }*/

                    switch(TAKE_MODE){
                        case TAKE_INIT:
                            initial_principalAxis = (new Matrix(mRotationMatrix_2)).times(new Matrix(Xaxis_phoneCoord_2)).getArray();
                            imageL_yaw_displacement += imageL_yaw_displacement_interval;
                            update_guideImage_position(guideImageL_pos_realCoord, imageL_yaw_displacement);
                            imageR_yaw_displacement -= imageL_yaw_displacement_interval;
                            update_guideImage_position(guideImageR_pos_realCoord, imageR_yaw_displacement);

                            break;
                        case TAKE_LEFT:
                            imageL_yaw_displacement += imageL_yaw_displacement_interval;
                            update_guideImage_position(guideImageL_pos_realCoord, imageL_yaw_displacement);
                            break;
                        case TAKE_RIGHT:
                            imageR_yaw_displacement -= imageL_yaw_displacement_interval;
                            update_guideImage_position(guideImageR_pos_realCoord, imageR_yaw_displacement);
                            break;
                        default:
                            Log.e("TAKEMODE error", "");
                            break;
                    }
                    CaptureButtonHandler.sendEmptyMessage(imageNum);
                }
            };


            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try{
                        cameraCaptureSession.capture(captureBuilder.build(),captureListener,mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        Log.e("captureSesssion", "failed!");
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e("onConfigure", "failed!");
                }
            },mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void createCameraPreview() {
        try{
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert  texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(),imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if(cameraDevice == null)
                        return;
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(CaptureImage.this, "Changed", Toast.LENGTH_SHORT).show();
                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null)
            Toast.makeText(this, "No Camera Device Error", Toast.LENGTH_SHORT).show();
        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 10);

        try{
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(),mSessionCaptureCallback , mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try{
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            //Check realtime permission if run higher API 23
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this,new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId,stateCallback,mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CAMERA_PERMISSION)
        {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "You can't use camera without permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();

        FrameImageView.setImageDrawable(getResources().getDrawable(R.drawable.cam_frame));
        android.view.ViewGroup.LayoutParams layoutParams = FrameImageView.getLayoutParams();
        layoutParams.width = 400;
        layoutParams.height = 400;
        FrameImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        FrameImageView.setLayoutParams(layoutParams);

        TAKE_MODE = TAKE_INIT;

        mButton_capture.setImageDrawable(getResources().getDrawable(R.drawable.capture_0));
        mButton_capture.setScaleType(ImageView.ScaleType.FIT_XY);
        imageArrayList.clear();
        if(guideImage_init != null){
            RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.myLayout);
            relativeLayout.removeView(guideImage_init);
            guideImage_init = null;
        }
        /*if(guideImageL != null){
            RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.myLayout);
            relativeLayout.removeView(guideImageL);
            guideImageL = null;
        }*/
        if(guideImageR != null){
            RelativeLayout relativeLayout = (RelativeLayout) findViewById(R.id.myLayout);
            relativeLayout.removeView(guideImageR);
            guideImageR = null;
        }

        //imageL_yaw_displacement = 0.0;
        imageR_yaw_displacement = 0.0;

        if(textureView.isAvailable())
            openCamera();
        else
            textureView.setSurfaceTextureListener(textureListener);
        mSensorManager.registerListener(deviceAngleEventListener, mSensor, 100000);
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
        mSensorManager.unregisterListener(deviceAngleEventListener);
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try{
            mBackgroundThread.join();
            mBackgroundThread= null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

    }

    private void calcCameraParam(CameraManager cManager) {
        // this function calculates camera parameters as shown :
        // fov, camera sensor size, focal_length

        float horizonalAngle;
        float verticalAngle;
        try {
            for (final String cameraId : cManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cManager.getCameraCharacteristics(cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation == CameraCharacteristics.LENS_FACING_BACK) {
                    float[] maxFocus = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                    focal_length = maxFocus[0];
                    Log.d("focal length", String.valueOf(focal_length));
                    SizeF size = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                    camera_sensor_width = size.getWidth();
                    camera_sensor_height = size.getHeight();
                    horizonalAngle = (float) (2*Math.atan(camera_sensor_width/(focal_length*2)));
                    verticalAngle = (float) (2*Math.atan(camera_sensor_height/(focal_length*2)));
                    fov[0] = horizonalAngle;
                    fov[1] = verticalAngle;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}