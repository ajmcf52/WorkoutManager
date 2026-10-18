package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/*
non-instantiable DB utility class.
responsible for opening connections.
 */
public final class DatabaseConnection {
    private static final String URL = "jdbc:sqlite:./appdata/workoutmanager.db";

    private DatabaseConnection() {}

    public static String getDatabaseURL() {
        return URL;
    }

    public static Connection openConnection() throws SQLException {
        //open the connection
        Connection connection = DriverManager.getConnection(URL);

        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = on");
        }

        return connection;
        // return Connection object
    }
}
