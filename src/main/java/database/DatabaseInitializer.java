package database;

import java.io.*;
import java.sql.*;
import java.util.List;

/*
initializer class for our database.
populates rows and columns by parsing
and executing statements from schema.sql
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {}

    private static void seedReferenceTables(Connection connection) throws SQLException {
        List<String> sectionTypeNames = List.of("WARM_UP",
                "MAIN_WORKOUT",
                "COOL_DOWN");
        List<String> muscleGroupNames = List.of("CORE",
                "QUADS",
                "ADDUCTORS",
                "HAMSTRINGS",
                "CALVES",
                "GLUTES",
                "HIP_FLEXORS",
                "BICEPS",
                "TRICEPS",
                "CHEST",
                "BACK",
                "SHOULDERS");
        List<String> equipmentNames = List.of("BARBELL",
                "KETTLEBELL",
                "BODYWEIGHT",
                "CABLE",
                "MACHINE",
                "DUMBBELL",
                "BOX",
                "TRX");

        String sql = """
                INSERT OR IGNORE INTO SectionType(name) VALUES (?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String sectionType : sectionTypeNames) {
                statement.setString(1, sectionType);
                statement.executeUpdate();
            }
        }
        sql = """
                INSERT OR IGNORE INTO Equipment(name) VALUES (?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String equipmentName : equipmentNames) {
                statement.setString(1, equipmentName);
                statement.executeUpdate();
            }
        }
        sql = """
                INSERT OR IGNORE INTO MuscleGroup(name) VALUES (?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String muscleGroupName : muscleGroupNames) {
                statement.setString(1, muscleGroupName);
                statement.executeUpdate();
            }
        }
    }

    /*
    initialize tables in the database
    if they don't already exist.
     */
    private static void initializeSchema(Connection connection) throws SQLException, IOException {

        InputStream inputStream = DatabaseInitializer.class.getResourceAsStream("/com/workoutmanager/schema.sql");
        if (inputStream == null) {
            throw new IllegalStateException("schema.sql not found.");
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String schemaString = reader.readAllAsString();
            for (String command : schemaString.split(";")) {
                if (!command.isBlank()) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(command);
                    }
                }
            }
        }
    }

    public static void initialize() throws SQLException, IOException {
        try (Connection connection = DatabaseConnection.openConnection()) {

            connection.setAutoCommit(false);

            try {
                // creates tables if they don't already exist
                initializeSchema(connection);

                // insert common static values into reference tables
                seedReferenceTables(connection);

                connection.commit();

            } catch (RuntimeException | IOException e) {
                connection.rollback();
                throw e;
            }
        }


    }

}
