package com.example.kakao.myapplication;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by kakao on 2018. 6. 28..
 */

public class photoInfoAdapter extends BaseAdapter {

    Context context;
    ArrayList<photoInfo> photoInfoArrayList;

    ImageView capturedImageView;
    TextView title;

    public photoInfoAdapter(Context _context, ArrayList<photoInfo> _ArrayList) {
        this.context = _context;
        this.photoInfoArrayList = _ArrayList;
    }

    @Override
    public int getCount() {
        return this.photoInfoArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return photoInfoArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = LayoutInflater.from(context).inflate(R.layout.photoinfo, null);
        capturedImageView = (ImageView) convertView.findViewById(R.id.image360_imageview);
        title = (TextView) convertView.findViewById(R.id.imageTitle_textview);
        title.setText(photoInfoArrayList.get(position).getTitle());
        byte[] bytes = photoInfoArrayList.get(position).getPhotoInByte();
        capturedImageView.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
        return convertView;
    }
}

