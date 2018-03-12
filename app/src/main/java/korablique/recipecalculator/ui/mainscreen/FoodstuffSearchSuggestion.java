package korablique.recipecalculator.ui.mainscreen;

import android.os.Parcel;
import android.os.Parcelable;

import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

import korablique.recipecalculator.model.Foodstuff;


public class FoodstuffSearchSuggestion implements SearchSuggestion {
    private Foodstuff suggestion;

    public static final Parcelable.Creator<FoodstuffSearchSuggestion> CREATOR
            = new Parcelable.Creator<FoodstuffSearchSuggestion>() {
        public FoodstuffSearchSuggestion createFromParcel(Parcel in) {
            return new FoodstuffSearchSuggestion(
                    in.readParcelable(Foodstuff.class.getClassLoader()));
        }

        public FoodstuffSearchSuggestion[] newArray(int size) {
            return new FoodstuffSearchSuggestion[size];
        }
    };

    public FoodstuffSearchSuggestion(Foodstuff suggestion) {
        this.suggestion = suggestion;
    }

    @Override
    public String getBody() {
        return suggestion.getName();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(suggestion, flags);
    }
}
