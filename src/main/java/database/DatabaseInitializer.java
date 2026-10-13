package database;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/*
initializer class for our database.
populates rows and columns by parsing
and executing statements from schema.sql
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {}

    public static void initialize() {
        BufferedReader reader;
        try {
            InputStream inputStream = DatabaseInitializer.class.getResourceAsStream("/com/workoutmanager/schema.sql");
            if (inputStream == null) {
                System.err.println("InputStream object from schema.sql is null.");
                throw new NullPointerException();
            }
            reader = new BufferedReader(
                    new InputStreamReader(inputStream));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
