package korablique.recipecalculator.util;

import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Набор утилитарных функций для работы с файловой системой.
 */
public class FileSystemUtils {
    private FileSystemUtils() {}

    public static void copyFileFromAssets(
            AssetManager assetManager,
            String sourceFileName,
            File target) throws IOException {
        if (target.exists()) {
            throw new IllegalArgumentException("Target file already exists: " + target.getAbsolutePath());
        }

        // Открываем файл в ассетах как входящий поток
        InputStream myInput = assetManager.open(sourceFileName);

        // Открываем целевой файл как исходящий поток
        OutputStream myOutput = new FileOutputStream(target);

        // перемещаем байты из входящего файла в целевой
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }

        // закрываем потоки
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }
}
