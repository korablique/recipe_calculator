package korablique.recipecalculator.model;

import android.os.Parcel;
import android.os.Parcelable;

public class DateOfBirth implements Parcelable {
    private int day;
    private int month;
    private int year;

    public DateOfBirth(int day, int month, int year) {
        this.day = day;
        this.month = month;
        this.year = year;
    }

    /**
     * @param dateOfBirthString string of format dd.MM.yyyy
     */
    public DateOfBirth(String dateOfBirthString) {
        String dayStr = dateOfBirthString.substring(0, dateOfBirthString.indexOf("."));
        String monthStr = dateOfBirthString.substring(
                dateOfBirthString.indexOf(".") + 1, dateOfBirthString.lastIndexOf("."));
        String yearStr = dateOfBirthString.substring(dateOfBirthString.lastIndexOf(".") + 1);
        day = Integer.parseInt(dayStr);
        month = Integer.parseInt(monthStr);
        year = Integer.parseInt(yearStr);
    }

    public int getDay() {
        return day;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    protected DateOfBirth(Parcel in) {
        day = in.readInt();
        month = in.readInt();
        year = in.readInt();
    }

    public static final Creator<DateOfBirth> CREATOR = new Creator<DateOfBirth>() {
        @Override
        public DateOfBirth createFromParcel(Parcel in) {
            return new DateOfBirth(in);
        }

        @Override
        public DateOfBirth[] newArray(int size) {
            return new DateOfBirth[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(day);
        dest.writeInt(month);
        dest.writeInt(year);
    }

    @Override
    public String toString() {
        return day + "." + month + "." + year;
    }
}
