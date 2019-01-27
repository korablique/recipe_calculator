package korablique.recipecalculator.database.room;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import static korablique.recipecalculator.database.UserParametersContract.ID;
import static korablique.recipecalculator.database.UserParametersContract.USER_PARAMETERS_TABLE_NAME;

@Dao
public interface UserParametersDao {
    /**
     * @return inserted user parameters' id or -1 if failed
     */
    @Insert
    long insertUserParameters(UserParametersEntity userParameters);

    @Query("SELECT * FROM " + USER_PARAMETERS_TABLE_NAME +
            " ORDER BY " + ID + " DESC LIMIT 1")
    UserParametersEntity loadCurrentUserParameters();
}
