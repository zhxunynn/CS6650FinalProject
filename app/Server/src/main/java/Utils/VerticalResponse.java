package Utils;

import com.google.gson.annotations.SerializedName;

public class VerticalResponse {
    @SerializedName("vertical")
    private int vertical;

    public int getVertical() {
        return vertical;
    }

    public void setVertical(int vertical) {
        this.vertical = vertical;
    }
}
