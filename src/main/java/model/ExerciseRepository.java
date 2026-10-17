package model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import database.DatabaseConnection;
import database.DatabaseInitializer;
import enums.MuscleGroup;
import enums.SectionType;

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
    checks the data of exercises and muscle groups
    to see if foreign key IDs are matching up.
     */
    public void exerciseSanityCheck(List<Exercise> exerciseList, Connection connection) throws SQLException {

        String sql = """
                SELECT e.name AS exercise_name, mg.name AS muscle_group_name
                FROM Exercise AS e
                JOIN ExerciseMuscleGroup AS emg
                    ON e.exercise_id=emg.exercise_id
                JOIN MuscleGroup AS mg
                    ON emg.muscle_group_id=mg.muscle_group_id
                WHERE e.name=?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Exercise exercise: exerciseList) {
                statement.setString(1, exercise.getName());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        System.out.println(result.getString("exercise_name")
                                + " | " + result.getString("muscle_group_name"));
                    }
                }
            }
        }
    }

    /*
    method used to write Exercise rows into the database.
     */
    public int save(Exercise exercise, Connection connection) throws SQLException {
        String sql = """
                INSERT INTO Exercise(name, demo_url, instructions, difficulty,
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
                    return keys.getInt(1);

                }
            }
        }
        // if no generated key is returned, throw an exception.
        throw new SQLException("Exercise inserted; no generated key returned.");
    }

    /*
    writes junction table entries according to a specific
    exercise that has just been written and saved.
     */
    public void saveMuscleGroups(int exerciseId, Exercise exercise, Connection connection) throws SQLException {

        /*      some exercises (like Treadmill Walk) don't target any
                specific muscle(s), hence we will have no junction rows.
        */
        if (exercise.getMuscleGroups() == null) {
            return;
        }

        String sql = """
                INSERT INTO ExerciseMuscleGroup(exercise_id, muscle_group_id)
                VALUES(?,(SELECT muscle_group_id FROM MuscleGroup WHERE name=?))
                """;


        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            for (MuscleGroup muscleGroup : exercise.getMuscleGroups()) {
                statement.setInt(1, exerciseId);
                statement.setString(2, muscleGroup.name());
                statement.executeUpdate();
            }
        }
    }

    /*
    save junction entry for equipment
    according to an exercise.
     */
    public void saveEquipment(int exerciseId, Exercise exercise, Connection connection) throws SQLException {

        String sql = """
                INSERT INTO ExerciseEquipment(exercise_id, equipment_id)
                VALUES(?,(SELECT equipment_id FROM Equipment WHERE name=?))
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, exerciseId);
            statement.setString(2, exercise.getEquipment().name());
            statement.executeUpdate();
        }
    }

    /*
    write junction table entries for suitable sections
    according to a given exercise.
     */
    public void saveSuitableSections(int exerciseId, Exercise exercise, Connection connection) throws SQLException {

        String sql = """
                INSERT INTO SuitableSections(exercise_id, section_type_id)
                VALUES(?, (SELECT section_type_id FROM SectionType WHERE name=?))
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (SectionType sectionType : exercise.getSuitableSections()) {
                statement.setInt(1, exerciseId);
                statement.setString(2, sectionType.name());
                statement.executeUpdate();
            }
        }
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
                    int exerciseId = save(exercise, connection);
                    saveMuscleGroups(exerciseId, exercise, connection);
                    saveEquipment(exerciseId, exercise, connection);
                    saveSuitableSections(exerciseId, exercise, connection);
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
