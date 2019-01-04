package korablique.recipecalculator.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserNameProvider {
    private static final String USER_FIRST_NAME = "USER_FIRST_NAME";
    private static final String USER_LAST_SURNAME = "USER_LAST_SURNAME";
    private Context context;
    @Inject
    public UserNameProvider(Context context) {
        this.context = context;
    }

    public void saveUserName(FullName fullName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USER_FIRST_NAME, fullName.getFirstName());
        editor.putString(USER_LAST_SURNAME, fullName.getLastName());
        editor.apply();
    }

    public FullName getUserName() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String userFirstName = prefs.getString(USER_FIRST_NAME, "");
        String userLastName = prefs.getString(USER_LAST_SURNAME, "");
        return new FullName(userFirstName, userLastName);
    }
}
