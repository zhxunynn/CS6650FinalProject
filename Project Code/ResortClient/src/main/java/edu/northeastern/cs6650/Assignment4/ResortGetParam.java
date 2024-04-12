package edu.northeastern.cs6650.Assignment4;

public class ResortGetParam {
    int resortID;
    int seasonID;
    int dayID;

    public ResortGetParam(int resortID, int seasonID, int dayID) {
        this.resortID = resortID;
        this.seasonID = seasonID;
        this.dayID = dayID;
    }

    public ResortGetParam() {

    }

    public int getResortID() {
        return resortID;
    }

    public void setResortID(int resortID) {
        this.resortID = resortID;
    }

    public int getSeasonID() {
        return seasonID;
    }

    public void setSeasonID(int seasonID) {
        this.seasonID = seasonID;
    }

    public int getDayID() {
        return dayID;
    }

    public void setDayID(int dayID) {
        this.dayID = dayID;
    }
}
