package korablique.recipecalculator.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserNameProvider {
    private static final String USER_NAME = "USER_NAME";
    private static final String USER_SURNAME = "USER_SURNAME";
    private Context context;
    @Inject
    public UserNameProvider(Context context) {
        this.context = context;
    }

    public void saveUserName(String name, String surname) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USER_NAME, name);
        editor.putString(USER_SURNAME, surname);
        editor.apply();
    }

    public String getUserName() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String userName = prefs.getString(USER_NAME, "");
        String userSurname = prefs.getString(USER_SURNAME, "");
        return userName + " " + userSurname;
    }
}
