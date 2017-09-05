package com.e2esp.fcmagazine.models;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Created by Ali on 7/21/2017.
 */

public class Magazine implements Parcelable {

    private String name;
    private Date date;
    private Bitmap cover;

    private int downloadedMagazinePages;
    private int totalMagazinePages;

    private boolean downloaded;
    private boolean downloading;
    private boolean spaceFiller;

    public Magazine(String name, Bitmap cover, Date date, boolean downloaded, int downloadedMagazinePages, int totalMagazinePages) {
        this.name = name;
        this.cover = cover;
        this.date = date;
        this.downloaded = downloaded;
        this.downloadedMagazinePages = downloadedMagazinePages;
        this.totalMagazinePages = totalMagazinePages;
    }

    public Magazine(boolean spaceFiller) {
        this.name = "";
        this.spaceFiller = spaceFiller;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public int getDownloadedMagazinePages() {

        return downloadedMagazinePages;
    }

    public void setDownloadedMagazinePages(int downloadedMagazinePages) {
        this.downloadedMagazinePages = downloadedMagazinePages;
    }

    public int getTotalMagazinePages() {
        return totalMagazinePages;
    }

    public void setTotalMagazinePages(int totalMagazinePages) {
        this.totalMagazinePages = totalMagazinePages;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public boolean isDownloading() {
        return downloading;
    }

    public void setDownloading(boolean downloading) {
        this.downloading = downloading;
    }

    public boolean isSpaceFiller() {
        return spaceFiller;
    }

    public Magazine(Parcel in) {
        this.name = in.readString();
        this.cover = in.readParcelable(Bitmap.class.getClassLoader());
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

    public static final Creator<Magazine> CREATOR = new Creator<Magazine>() {
        @Override
        public Magazine createFromParcel(Parcel in) {
            //return new Magazine(in);
            Magazine magazine = new Magazine(in);

            if (magazine == null) {
                throw new RuntimeException("Failed to unparcel Magazine");
            }
            return magazine;
        }

        @Override
        public Magazine[] newArray(int size) {
            return new Magazine[size];
        }
    };

}
