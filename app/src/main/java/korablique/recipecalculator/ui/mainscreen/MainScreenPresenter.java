package korablique.recipecalculator.ui.mainscreen;


import android.content.Intent;
import android.os.Bundle;

public interface MainScreenPresenter {
    void onActivityCreate();
    void onActivitySaveState(Bundle outState);
    void onActivityRestoreState(Bundle savedInstanceState);
    void onActivityResume();
    void onActivityResult(int requestCode, int resultCode, Intent data);
}
