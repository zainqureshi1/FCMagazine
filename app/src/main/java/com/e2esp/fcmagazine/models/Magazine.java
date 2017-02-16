package com.e2esp.fcmagazine.models;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Zain on 2/10/2017.
 */

public class Magazine implements Parcelable {

    private String name;
    private String filePath;
    private int imageRes;
    private int pageCount;

    private Bitmap cover;

    public Magazine(String name, String filePath, int imageRes, int pageCount) {
        this.name = name;
        this.filePath = filePath;
        this.imageRes = imageRes;
        this.pageCount = pageCount;
    }

    public Magazine(Parcel in) {
        this.name = in.readString();
        this.filePath = in.readString();
        this.imageRes = in.readInt();
        this.pageCount = in.readInt();
        this.cover = in.readParcelable(Bitmap.class.getClassLoader());
    }

    public String getName() {
        return name;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getImageRes() {
        return imageRes;
    }

    public int getPageCount() {
        return pageCount;
    }

    public Bitmap getCover() {
        return cover;
    }

    public void setCover(Bitmap cover) {
        this.cover = cover;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(filePath);
        dest.writeInt(imageRes);
        dest.writeInt(pageCount);
        dest.writeParcelable(cover, flags);
    }

    public static final Parcelable.Creator<Magazine> CREATOR = new Parcelable.Creator<Magazine>() {
        public Magazine createFromParcel(Parcel p) {
            Magazine magazine = new Magazine(p);
            if (magazine == null) {
                throw new RuntimeException("Failed to unparcel Magazine");
            }
            return magazine;
        }
        public Magazine[] newArray(int size) {
            return new Magazine[size];
        }
    };

}
