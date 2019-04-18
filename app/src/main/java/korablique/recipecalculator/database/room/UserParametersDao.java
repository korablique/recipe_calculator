package korablique.recipecalculator.database.room;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import static korablique.recipecalculator.database.UserParametersContract.COLUMN_NAME_MEASUREMENTS_TIMESTAMP;
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

    @Query("SELECT * FROM " + USER_PARAMETERS_TABLE_NAME +
            " ORDER BY " + ID + " ASC LIMIT 1")
    UserParametersEntity loadFirstUserParameters();

    @Query("SELECT * FROM " + USER_PARAMETERS_TABLE_NAME +
            " WHERE " + COLUMN_NAME_MEASUREMENTS_TIMESTAMP + " >= :from" +
            " AND " + COLUMN_NAME_MEASUREMENTS_TIMESTAMP + " <= :to" +
            " ORDER BY " + COLUMN_NAME_MEASUREMENTS_TIMESTAMP + " ASC")
    List<UserParametersEntity> loadUserParameters(long from, long to);
}
