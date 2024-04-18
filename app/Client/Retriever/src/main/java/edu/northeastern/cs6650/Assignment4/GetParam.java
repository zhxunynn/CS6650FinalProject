package edu.northeastern.cs6650.Assignment4;

import java.util.ArrayList;
import java.util.List;

public class GetParam {
    private int resortID;
    private int seasonID;
    private int dayID;
    private int skierID; // Added for skier specific queries
    private List<String> resortList;
    private List<String> seasonList;

    // Constructor
    public GetParam(int resortID, int seasonID, int dayID, int skierID) {
        this.resortID = resortID;
        this.seasonID = seasonID;
        this.dayID = dayID;
        this.skierID = skierID;
    }

    public GetParam(int skierID, List<String> resortList, List<String> seasonList){
        this.skierID = skierID;
        this.resortList = resortList;
        this.seasonList = seasonList;
    }

    public GetParam() {

    }

    // Default constructor, getters and setters as before, plus:
    public int getSkierID() {
        return skierID;
    }

    public void setSkierID(int skierID) {
        this.skierID = skierID;
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

    public List<String> getResortList() {
        return resortList;
    }

    public void setResortList(List<String> resortList) {
        this.resortList = resortList;
    }

    public List<String> getSeasonList() {
        return seasonList;
    }

    public void setSeasonList(List<String> seasonList) {
        this.seasonList = seasonList;
    }
}

