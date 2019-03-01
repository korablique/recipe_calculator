package korablique.recipecalculator.database.room;

import android.content.Context;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.annotation.AnyThread;
import androidx.room.Room;
import korablique.recipecalculator.FileSystemUtils;
import korablique.recipecalculator.TestEnvironmentDetector;
import korablique.recipecalculator.database.DatabaseThreadExecutor;

import static korablique.recipecalculator.database.room.Migrations.MIGRATION_1_2;
import static korablique.recipecalculator.database.room.Migrations.MIGRATION_2_3;
import static korablique.recipecalculator.database.room.Migrations.MIGRATION_3_4;
import static korablique.recipecalculator.database.room.Migrations.MIGRATION_4_5;
import static korablique.recipecalculator.database.room.Migrations.MIGRATION_5_6;

/**
 * Владелец объекта БД. Отвечает за его хранение и инициализацию.
 * Потокобезопасен.
 */
@AnyThread
@Singleton
public class DatabaseHolder {
    private static final String DATABASE_NAME = "Main.db";
    private final Context context;
    private final DatabaseThreadExecutor databaseThreadExecutor;
    private AppDatabase db;

    @Inject
    public DatabaseHolder(Context context, DatabaseThreadExecutor databaseThreadExecutor) {
        this.context = context;
        this.databaseThreadExecutor = databaseThreadExecutor;
    }

    public synchronized AppDatabase getDatabase() {
        if (db == null) {
            db = createDB();
        }
        return db;
    }

    /**
     * Закрывает соединение с БД.
     * Метод не стоит использовать вне тестов - нет смысла, и Room не особо
     * даёт гарантии о результатах закрытия.
     */
    synchronized void closeDatabaseConnection() {
        if (db == null) {
            return;
        }
        db.close();
        db = null;
    }

    private AppDatabase createDB() {
        File dbFile = getDBFile();
        if (!dbFile.exists()) {
            createDBFile(dbFile);
        }

        AppDatabase.Builder<AppDatabase> builder =
                Room.databaseBuilder(context, AppDatabase.class, dbFile.getAbsolutePath());

        // Все фоновые операции Room должен выполнять на специальном фоновом БД-потоке.
        builder.setQueryExecutor(databaseThreadExecutor::execute);
        // Сообщим о миграциях.
        builder.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6);
        // Позволим работу на главном потоке в тестах.
        if (TestEnvironmentDetector.isInTests()) {
            builder.allowMainThreadQueries();
        }
        return builder.build();
    }

    File getDBFile() {
        return new File(context.getFilesDir(), DATABASE_NAME);
    }

    private void createDBFile(File dbFile) {
        try {
            FileSystemUtils.copyFileFromAssets(context.getAssets(), DATABASE_NAME, dbFile);
        } catch (IOException e) {
            // Если не получается скопировать БД из ассетов -
            // функционирование приложение невозможно, немедленно падаем.
            throw new Error("Couldn't copy DB from assets", e);
        }

        if (!dbFile.exists()) {
            // Если не получается скопировать БД из ассетов -
            // функционирование приложение невозможно, немедленно падаем.
            throw new Error("Couldn't copy DB from assets for unknown reasons");
        }
    }
}
