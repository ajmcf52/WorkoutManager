package model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import database.DatabaseConnection;
import database.DatabaseInitializer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.List;

/*
responsible for parsing the exercise JSON file, deserializing the data and giving
us something we can make sense of.
 */

public class ExerciseRepository {

    /*
    method used to write Exercise rows into the database.
     */
    public int save(Exercise exercise, Connection connection) throws SQLException {
        String sql = """
                INSERT OR IGNORE INTO Exercise(name, demo_url, instructions, difficulty,
                movement_type, prescription_type)
                 VALUES(?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, exercise.getName());

            // optional fields (demoURL, exercise instructions)
            if (exercise.getDemoLink() == null || exercise.getDemoLink().isBlank()) {
                statement.setNull(2, Types.VARCHAR);
            }
            else {
                statement.setString(2, exercise.getDemoLink());
            }
            if (exercise.getInstructions() == null || exercise.getInstructions().isBlank()) {
                statement.setNull(3, Types.VARCHAR);
            }
            else {
                statement.setString(3, exercise.getInstructions());
            }

            statement.setString(4, exercise.getDifficulty().name());
            statement.setString(5, exercise.getMovementType().name());
            statement.setString(6, exercise.getPrescriptionType().name());

            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int exerciseId = keys.getInt(1);
                    System.out.println("id: " + exerciseId);
                    return exerciseId;
                }
            }
        }
        // if no generated key is returned, throw an exception.
        throw new SQLException("Exercise inserted; no generated key returned.");
    }

    /*
    mapping exercise list from JSON to JDBC.
     */
    public void migrateExercisesFromJson() throws SQLException {
        List<Exercise> exerciseList = loadAll();

        if (exerciseList == null) {
            throw new IllegalStateException("exercise list is null: IllegalStateException");
        }

        try (Connection connection = DatabaseConnection.openConnection()) {

            // if all exercises don't migrate successfully, we roll back.
            connection.setAutoCommit(false);

            try {
                for (Exercise exercise : exerciseList) {
                    save(exercise, connection);
                }
                connection.commit();
            }
            catch (RuntimeException | SQLException e) {
                System.err.println(e.getMessage());
                connection.rollback();
                throw new RuntimeException(e);
            }
        }
    }

    /*
    migrates old difficulty values to new ones.
     */
    public void migrateDifficultyValues(File file) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        try {
            root = mapper.readTree(file);
        } catch (IOException e) {
            System.err.println("Migration script unable to read exercises.json");
            throw new RuntimeException(e);
        }
        for (JsonNode node : root) {
            ObjectNode exerciseNode = (ObjectNode) node;
            String oldValue = exerciseNode.get("difficulty").asText();
            String newValue = switch (oldValue) {
                case "ONE", "ONE_AND_HALF" -> "BEGINNER";
                case "TWO", "TWO_AND_HALF" -> "NOVICE";
                case "THREE", "THREE_AND_HALF" -> "INTERMEDIATE";
                case "FOUR" -> "ADVANCED";
                case "FIVE", "FOUR_AND_HALF" -> "EXPERT";
                default -> oldValue;
            };
            exerciseNode.put("difficulty", newValue);
        }
        try {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file, root);
        } catch (IOException e) {
            System.err.println("Data migrator failed to write: exercises.json");
            throw new RuntimeException(e);
        }
    }

    /*
    loads the repository of exercises from the JSON file.
     */
    public List<Exercise> loadAll() {

        try {
            DatabaseInitializer.initialize();
        } catch (SQLException | IOException e) {
            System.err.println("Error initializing DB (DatabaseInitializer)");
            throw new RuntimeException(e);
        }

        Path resource = Paths.get("appdata", "exercises.json");

        File file = null;
        try {
            file = new File(resource.toUri());
        } catch (RuntimeException e) {
            System.err.println("Exercise file creation failed.");
            throw e;
        }

        ObjectMapper mapper = new ObjectMapper();
        List<Exercise> exerciseList = null;
        try {
            exerciseList = mapper.readValue(file, new TypeReference<List<Exercise>>() {}
            );
        } catch (IOException e) {
            System.err.println("Failed to read exercise file.");
        } catch (Exception e) {
            System.err.println("Unknown file reading error.");
            throw e;
        }

        if (exerciseList == null) {
            throw new IllegalStateException("exercise list is null: IllegalStateException");
        }

        return exerciseList;
    }

    /*
    writes the list of exercises to the JSON file.
    to append a newly created exercise, this is the
    cleanest way to avoid clumsy and brittle code.
     */
    public void saveAll(List<Exercise> exerciseList) {
        ObjectMapper mapper = new ObjectMapper();

        Path resource = Paths.get("appdata", "exercises.json");

        File file;
        try {
            file = new File(resource.toUri());
        } catch (RuntimeException e) {
            System.err.println("ExerciseRepository in saveAll(): failed to create exercise file.");
            throw e;
        }

        try {
            mapper.writeValue(file, exerciseList);
        } catch (IOException e) {
            System.err.println("Mapper failed to update exercise list in JSON");
            throw new RuntimeException(e);
        }
    }
}
