package database;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/*
initializer class for our database.
populates rows and columns by parsing
and executing statements from schema.sql
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {}

    public static void initialize() throws SQLException {
        String schemaString;
        BufferedReader reader;
        try {
            InputStream inputStream = DatabaseInitializer.class.getResourceAsStream("/com/workoutmanager/schema.sql");
            if (inputStream == null) {
                System.err.println("InputStream object from schema.sql is null.");
                throw new IllegalStateException();
            }
            reader = new BufferedReader(
                    new InputStreamReader(inputStream));
            schemaString = reader.readAllAsString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        boolean error = false;
        Connection connection = null;
        try {
            connection = DatabaseConnection.openConnection();
            System.out.println(connection.getMetaData().getURL());
            connection.setAutoCommit(false);

            for (String command : schemaString.split(";")) {
                if (!command.isBlank()) {
                    Statement statement = connection.createStatement();
                    statement.execute(command);
                }
            }
        }
        catch (SQLException e) {
            System.err.println("Error executing SQL schema");
            error = true;
            throw new RuntimeException(e);
        }
        finally {
            assert connection != null;
            if (error) {
                connection.rollback();
            }
            else {

                connection.commit();
            }
            connection.close();
        }


    }

}
