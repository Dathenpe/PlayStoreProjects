package tetris;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

public class HighScore implements Comparable<HighScore>, Parcelable {
    private final int score;
    private final int level;
    private final String date;

    public HighScore(int score, int level, String date) {
        this.score = score;
        this.level = level;
        this.date = date;
    }

    protected HighScore(Parcel in) {
        score = in.readInt();
        level = in.readInt();
        date = in.readString();
    }

    public static final Creator<HighScore> CREATOR = new Creator<HighScore>() {
        @Override
        public HighScore createFromParcel(Parcel in) {
            return new HighScore(in);
        }

        @Override
        public HighScore[] newArray(int size) {
            return new HighScore[size];
        }
    };

    public int getScore() { return score; }
    public int getLevel() { return level; }
    public String getDate() { return date; }

    @Override
    public int compareTo(@NonNull HighScore other) {
        return Integer.compare(other.score, this.score);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(score);
        dest.writeInt(level);
        dest.writeString(date);
    }
}