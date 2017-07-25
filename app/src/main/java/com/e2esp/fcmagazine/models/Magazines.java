package com.e2esp.fcmagazine.models;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Created by Ali on 7/21/2017.
 */

public class Magazines  implements Parcelable {

    private String name;
    private Date date;
    private Bitmap cover;
    private boolean spaceFiller;

    public Magazines(boolean spaceFiller) {
        this.name = "";
        this.spaceFiller = spaceFiller;
    }

    public static final Creator<Magazines> CREATOR = new Creator<Magazines>() {
        @Override
        public Magazines createFromParcel(Parcel in) {
            //return new Magazines(in);
            Magazines magazine = new Magazines(in);

            if (magazine == null) {
                throw new RuntimeException("Failed to unparcel Magazine");
            }
            return magazine;
        }
               @Override
        public Magazines[] newArray(int size) {
            return new Magazines[size];
        }
    };

    public boolean isSpaceFiller() {
        return spaceFiller;
    }

    public Magazines(String name, Bitmap myBitmapRecent) {
        this.name = name;
        this.cover=myBitmapRecent;
    }

    public Magazines(String name, Bitmap myBitmapRecent, Date date) {
        this.name = name;
        this.cover=myBitmapRecent;
        this.date = date;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Bitmap getCover() {
        return cover;
    }

    public void setCover(Bitmap cover) {
        this.cover = cover;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeParcelable(cover, flags);
    }
    public Magazines(Parcel in) {
        this.name = in.readString();
        this.cover = in.readParcelable(Bitmap.class.getClassLoader());
    }

}
