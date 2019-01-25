package korablique.recipecalculator.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import static korablique.recipecalculator.database.DbHelper.COLUMN_NAME_VERSION;
import static korablique.recipecalculator.database.DbHelper.TABLE_DATABASE_VERSION;

@Entity(tableName = TABLE_DATABASE_VERSION)
class DatabaseVersionEntity {
    @PrimaryKey
    private int id;

    @ColumnInfo(name = COLUMN_NAME_VERSION)
    private int version;

    public DatabaseVersionEntity(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
