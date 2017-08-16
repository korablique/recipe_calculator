package korablique.recipecalculator;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class UserGoalActivity extends MyActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_goal);

        Spinner goalSpinner = (Spinner) findViewById(R.id.goal_spinner);
        ArrayAdapter<CharSequence> goalAdapter = ArrayAdapter.createFromResource(this,
                R.array.goals, android.R.layout.simple_spinner_item);
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        goalSpinner.setAdapter(goalAdapter);

        Spinner genderSpinner = (Spinner) findViewById(R.id.gender_spinner);
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(this,
                R.array.gender, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);

        Spinner physicalActivitySpinner = (Spinner) findViewById(R.id.physical_activity_spinner);
        ArrayAdapter<CharSequence> physicalActivityAdapter = ArrayAdapter.createFromResource(this,
                R.array.physical_activity, R.layout.long_spinner_item);
        physicalActivityAdapter.setDropDownViewResource(R.layout.long_spinner_item);
        physicalActivitySpinner.setAdapter(physicalActivityAdapter);
    }
}
