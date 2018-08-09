package com.example.kakao.myapplication;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class StitchImage extends AppCompatActivity {

    private Mat matInput;
    private Mat matOutput;
    private Mat result;
    private Bitmap result_bm;
    private boolean save_check;
    private double MaxRotateRadian;


    public native void NativeStitch(Mat[] imgs, double maxRotateRadian, long resultMatAddr, boolean wavecorrect, boolean multiBand);
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java3");
    }

    private Mat readImageFromResources(){
        Mat img = null;
        try {
            img = Utils.loadResource(this , R.drawable.image1);
            Imgproc.cvtColor(img,img,Imgproc.COLOR_RGB2BGR);
            return img;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return img;
    }

    /*
     *  화면의 Save 버튼을 누르면 실행되는 함수
     */
    public void SaveImage(View view) throws IOException {
        if (save_check){
            Toast.makeText(this, "이미지가 이미 저장되어 있습니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String ex_storage = Environment.getExternalStorageDirectory().getAbsolutePath();
        String folder_name = "/StitchImage/";

        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdfNow = new SimpleDateFormat("yyyyMMddHHmmss");

        String file_name = sdfNow.format(date) + ".jpg";
        String string_path = ex_storage + folder_name;

        File file_path;
        file_path = new File(string_path);
        if (!file_path.isDirectory()) {
            file_path.mkdirs();
        }
        FileOutputStream out = new FileOutputStream(string_path + file_name);
        result_bm.compress(Bitmap.CompressFormat.JPEG, 100, out);
        save_check = true;
        out.close();

        ExifInterface exif = new ExifInterface(string_path+file_name);
        exif.setAttribute(ExifInterface.TAG_COPYRIGHT,String.valueOf(MaxRotateRadian));
        exif.saveAttributes();
        Toast.makeText(this, "이미지가 저장되었습니다", Toast.LENGTH_SHORT).show();
    }

    /*
     *  화면의 View 버튼을 누르면 실행되는 함수
     */
    public void ViewImage(View view) throws IOException{
        Intent intent = new Intent(this,ViewImage.class);

        if(MaxRotateRadian >= Math.PI){
            MaxRotateRadian = Math.PI;
        }

        String filename = "image";
        FileOutputStream stream = this.openFileOutput(filename, Context.MODE_PRIVATE);
        result_bm.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        stream.close();
        intent.putExtra("image", filename);
        intent.putExtra("MaxRotateRadian", MaxRotateRadian);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stitch_image);

        final ImageView imageView = (ImageView) findViewById(R.id.imageView);
        save_check = false;

        int image_num = getIntent().getIntExtra("imagenum",3);
        MaxRotateRadian = getIntent().getDoubleExtra("MaxRotateRadian",Math.PI/9);
        Mat imgArr[] = new Mat[image_num];


        for (int i =0 ; i<image_num ; i++) {
            Bitmap bmp = null;
            String filename =getIntent().getStringExtra("image"+i);
            try {
                FileInputStream is =this.openFileInput(filename);
                bmp = BitmapFactory.decodeStream(is);
                imgArr[i] = readImageFromResources();

                Utils.bitmapToMat(bmp,imgArr[i]);
                Imgproc.cvtColor(imgArr[i],imgArr[i],Imgproc.COLOR_RGB2BGR);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        Log.d("imgArr"," loading complete");

        result = new Mat();
        boolean wavecorrect = true;
        boolean multiBand = true;


        NativeStitch(imgArr, MaxRotateRadian, result.getNativeObjAddr(),wavecorrect,multiBand);

        if (result.empty()){
            Log.e("stitching","is failed");
            Toast.makeText(this,"이미지 스티칭에 실패하였습니다",Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("stitching","is finished");
        Imgproc.cvtColor(result,result,Imgproc.COLOR_BGR2RGB);
        Bitmap bmp2 = Bitmap.createBitmap(result.cols(),result.rows(),Bitmap.Config.RGB_565);
        Utils.matToBitmap(result,bmp2);


        int sW = bmp2.getWidth();
        int sH = bmp2.getHeight();
        int black=0;

        List<Integer> PixList = new ArrayList();
        int[] pixels = new int [sW*sH];
        bmp2.getPixels(pixels,0,sW,0,0,sW,sH);

        for(int y = 0; y < sH; y++){
            black = 0;
            for(int x = 0; x < sW; x++){
                if(pixels[y*sW+x]==Color.BLACK){
                    black++;
                }
            }
            if(black<Math.round(sW/4)){
                for(int i = 0; i < sW;i++){
                    PixList.add(pixels[y*sW+i]);
                }
            }
            else{
                Log.d(String.valueOf(y),"line is deleted");
            }
        }

        int[] ret = new int[PixList.size()];
        for (int i=0; i < ret.length; i++)
        {
            ret[i] = PixList.get(i).intValue();
        }

        result_bm = Bitmap.createBitmap(ret,sW,Math.round(ret.length/sW), Bitmap.Config.RGB_565);

        imageView.setImageBitmap(result_bm);

    }
}
