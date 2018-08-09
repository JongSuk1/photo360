package com.example.kakao.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Gallery360 extends AppCompatActivity {

    private ArrayList<photoInfo> photoInfoArrayList = new ArrayList<photoInfo>();
    private ListView imageInfo_ListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery360);

        imageInfo_ListView= (ListView) findViewById(R.id.imageInfo_listview);

        int permissionInfo = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);

        if(permissionInfo == PackageManager.PERMISSION_GRANTED){
            Log.d("I have","Permission");
        }

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            }, 1);
        }

        Bitmap bmp = null;
        final String rootSD = Environment.getExternalStorageDirectory().getAbsolutePath();
        File file = new File(rootSD+"/StitchImage");

        try {
            File[] imageFileList = file.listFiles();
            Arrays.sort(imageFileList);
            Log.d("image number", String.valueOf(imageFileList.length));

            for (int i = 0 ; i < imageFileList.length ; i++) {
                String imageFileName;
                byte[] imageInBytes;

                imageFileName = imageFileList[i].getName();
                int imageLen = (int) imageFileList[i].length();
                imageInBytes = new byte[imageLen];
                BufferedInputStream buf = new BufferedInputStream(new FileInputStream(imageFileList[i]));
                buf.read(imageInBytes, 0, imageInBytes.length);
                buf.close();

                photoInfo NewItem = new photoInfo(imageFileName, imageInBytes);
                photoInfoArrayList.add(NewItem);
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Log.d("arraylist number", String.valueOf(photoInfoArrayList.size()));
        Collections.reverse(photoInfoArrayList);
        photoInfoAdapter mPhotoInfoAdapter = new photoInfoAdapter(this, photoInfoArrayList);
        imageInfo_ListView.setAdapter(mPhotoInfoAdapter);

        imageInfo_ListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                byte[] myImg = photoInfoArrayList.get(position).getPhotoInByte();
                Bitmap bmp = BitmapFactory.decodeByteArray(myImg, 0, myImg.length);

                Intent intent = new Intent(Gallery360.this,ViewImage.class);

                String filename = "image";
                FileOutputStream stream = null;
                String filepath =  rootSD + "/StitchImage/" + photoInfoArrayList.get(position).getTitle();
                try {
                    stream = Gallery360.this.openFileOutput(filename, Context.MODE_PRIVATE);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    stream.close();
                    intent.putExtra("image", filename);
                    intent.putExtra("filepath",filepath);
                    startActivity(intent);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }
}
