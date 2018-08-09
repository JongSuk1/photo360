package com.example.kakao.myapplication;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by kakao on 2018. 6. 28..
 */

public class photoInfo implements Parcelable {
    private String photo_title;
    private byte[] photoInByte =null;

    public photoInfo(String title, byte[] Image) {
        this.photo_title = title;
        this.photoInByte = Image;
    }

    protected photoInfo(Parcel in) {
        photo_title = in.readString();
        photoInByte = in.createByteArray();
    }

    public static final Creator<photoInfo> CREATOR = new Creator<photoInfo>() {
        @Override
        public photoInfo createFromParcel(Parcel in) {
            return new photoInfo(in);
        }

        @Override
        public photoInfo[] newArray(int size) {
            return new photoInfo[size];
        }
    };

    public String getTitle(){
        return this.photo_title;
    }

    public byte[] getPhotoInByte(){
        return this.photoInByte;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(photo_title);
        parcel.writeByteArray(photoInByte);
    }
}