package model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import database.DatabaseConnection;
import database.DatabaseInitializer;
import enums.*;

import javax.xml.transform.Result;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

/*
responsible for parsing the exercise JSON file, deserializing the data and giving
us something we can make sense of.
 */

public class ExerciseRepository {

    /*
    QUERIES USED FOR TESTING
     */
    private static final String MG_INSPECTION = """
                SELECT e.name AS exercise_name, mg.name AS muscle_group_name
                FROM Exercise e
                JOIN ExerciseMuscleGroup emg
                ON e.exercise_id=emg.exercise_id
                JOIN MuscleGroup mg
                ON emg.muscle_group_id=mg.muscle_group_id
                WHERE e.exercise_id=?
                """;

    private static final String SS_INSPECTION = """
                SELECT e.name AS exercise_name, st.name AS section_type_name
                FROM Exercise e
                JOIN SuitableSections ss
                ON e.exercise_id=ss.exercise_id
                JOIN SectionType st
                ON ss.section_type_id=st.section_type_id
                WHERE e.exercise_id=?
                """;

    private static final String EQ_INSPECTION = """
                SELECT e.name AS exercise_name, eq.name AS equipment_name
                FROM Exercise e
                JOIN ExerciseEquipment ee
                ON e.exercise_id=ee.exercise_id
                JOIN Equipment eq
                ON eq.equipment_id=ee.equipment_id
                WHERE e.exercise_id=?
                """;

    /*
    script used to eliminate orphan rows from foreign key
    enforcement not being in place within DatabaseConnection.openConnection()
     */
    public void cleanUpOrphanedRows(Connection connection) throws SQLException {

        String cleanEe = """
                DELETE FROM ExerciseEquipment
                WHERE exercise_id NOT IN (
                    SELECT exercise_id
                    FROM Exercise)
                """;

        String cleanEmg = """
                DELETE FROM ExerciseMuscleGroup
                WHERE exercise_id NOT IN (
                    SELECT exercise_id
                    FROM Exercise)
                """;

        String cleanSs = """
                DELETE FROM SuitableSections
                WHERE exercise_id NOT IN (
                    SELECT exercise_id
                    FROM Exercise)
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(cleanEe);
            statement.execute(cleanEmg);
            statement.execute(cleanSs);
        }


    }

    /*
    inspecting table rows following
    deletion--queries should return
    zero.
     */
    public void deletionSanityCheck(int exerciseId, Connection connection) throws SQLException {
        String ssRowCount = """
                SELECT COUNT(*) AS row_count
                FROM SuitableSections ss
                WHERE ss.exercise_id=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(ssRowCount)) {
            statement.setInt(1, exerciseId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    System.out.println("SuitableSection Delete test passed: " +
                            (result.getInt("row_count") == 0));
                }
            }
        }

        String mgRowCount = """
                SELECT COUNT(*) AS row_count
                FROM ExerciseMuscleGroup emg
                WHERE emg.exercise_id=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(mgRowCount)) {
            statement.setInt(1, exerciseId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    System.out.println("Exercise Muscle Group Delete test passed: " +
                            (result.getInt("row_count") == 0));
                }
            }
        }

        String eqRowCount = """
                SELECT COUNT(*) AS row_count
                FROM ExerciseEquipment ee
                WHERE ee.exercise_id=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(eqRowCount)) {
            statement.setInt(1, exerciseId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    System.out.println("Exercise Equipment Delete test passed: " +
                            (result.getInt("row_count") == 0));
                }
            }
        }

        String exRowCount = """
                SELECT COUNT(*) AS row_count
                FROM Exercise e
                WHERE e.exercise_id=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(exRowCount)) {
            statement.setInt(1, exerciseId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    System.out.println("Exercise delete test passed: " +
                            (result.getInt("row_count") == 0));
                }
            }
        }
    }

    /*
    inspecting junction table rows
    following exercise creation to
    ensure FK relations have been
    established properly.
     */
    public void creationSanityCheck(int exerciseId, Connection connection) throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(MG_INSPECTION)) {
            statement.setInt(1, exerciseId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    System.out.println(results.getString("exercise_name") + " | "
                    + results.getString("muscle_group_name"));
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(SS_INSPECTION)) {
            statement.setInt(1, exerciseId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    System.out.println(results.getString("exercise_name") + " | "
                    + results.getString("section_type_name"));
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(EQ_INSPECTION)) {
            statement.setInt(1, exerciseId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    System.out.println(results.getString("exercise_name") + " | "
                    + results.getString("equipment_name"));
                }
            }
        }
    }

    /*
    Dynamic query builder for multi-predicate JDBC search filtering.

    We add SQL fragments in order, adding param values in the same order.

    eg:
    ["e.name LIKE ?", "e.difficulty = ?"]
    ["%barbell%", "INTERMEDIATE"]

    this allows us to construct the full query properly, starting with
    WHERE and joined by AND between query fragments.

    predicates beginning with NULL/"All" are simply omitted.

    LEFT JOINs are used to preserve Exercise rows that have no active
    muscle groups, such as cardio exercises (eg, Treadmill Walk).

     */
    public List<Integer> filterExercises(ExerciseFilter filter, Connection connection)
            throws SQLException {
        List<Integer> exerciseIds = new ArrayList<>();

        String sql = """
                SELECT DISTINCT e.exercise_id
                FROM Exercise e
                JOIN ExerciseEquipment ee
                ON e.exercise_id = ee.exercise_id
                LEFT JOIN ExerciseMuscleGroup emg
                ON e.exercise_id = emg.exercise_id
                JOIN Equipment eq
                ON ee.equipment_id=eq.equipment_id
                LEFT JOIN MuscleGroup mg
                ON emg.muscle_group_id=mg.muscle_group_id
                
                """;

        String searchText = filter.getSearchText();
        Difficulty difficulty = filter.getDifficulty();
        Equipment equipment = filter.getEquipment();
        MuscleGroup muscleGroup = filter.getMuscleGroup();

        List<String> queryConditions = new ArrayList<>();
        List<String> paramValues = new ArrayList<>();

        if (searchText != null && !searchText.isBlank()) {
            queryConditions.add("e.name LIKE ?");
            paramValues.add("%" + searchText + "%");
        }
        if (difficulty != null && difficulty != Difficulty.NULL) {
            queryConditions.add("e.difficulty = ?");
            paramValues.add(difficulty.name());
        }
        if (equipment != null && equipment != Equipment.NULL) {
            queryConditions.add("eq.name = ?");
            paramValues.add(equipment.name());
        }
        if (muscleGroup != null && muscleGroup != MuscleGroup.NULL) {
            queryConditions.add("mg.name = ?");
            paramValues.add(muscleGroup.name());
        }
        if (!queryConditions.isEmpty()) {
            sql += "WHERE " + queryConditions.getFirst();
        }
        for (int i = 1; i < queryConditions.size(); i++) {
            sql += "\nAND " + queryConditions.get(i);
        }


        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int i = 0; i < paramValues.size(); i++) {
                statement.setString(i + 1, paramValues.get(i));
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    exerciseIds.add(resultSet.getInt("exercise_id"));
                }
            }
        }
        return exerciseIds;
    }

    /*
    deletes a given Exercise from JDBC
     */
    public void delete(Exercise exercise, Connection connection) throws SQLException {
        String sql = """
                DELETE FROM Exercise
                WHERE exercise_id=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, exercise.getExerciseId());
            statement.executeUpdate();
        }

        deletionSanityCheck(exercise.getExerciseId(), connection);

    }

    /*
    CRUD function--updates the exercise by ID
    with the given fields.
     */
    public void update(Exercise exercise, Connection connection) throws SQLException {
        String sql = """
                UPDATE Exercise
                SET name=?, demo_url=?, instructions=?, difficulty=?,
                movement_type=?, prescription_type=?
                WHERE exercise_id=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, exercise.getName());

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

            statement.setInt(7, exercise.getExerciseId());

            int rowsAffected = statement.executeUpdate();
            if (rowsAffected != 1) {
                throw new SQLException("Exercise object not found in JDBC.");
            }
        }

        /*
        update junction table rows;
        delete first then reinsert.
         */
        deleteJunctionRows(exercise, connection);
        saveEquipment(exercise, connection);
        saveMuscleGroups(exercise,connection);
        saveSuitableSections(exercise, connection);
    }

    /*
    helper for UPDATE.
     */
    private void deleteJunctionRows(Exercise exercise, Connection connection) throws SQLException {
        String sqlEq = """
                DELETE FROM ExerciseEquipment
                WHERE exercise_id=?
                """;
        String sqlMg = """
                DELETE FROM ExerciseMuscleGroup
                WHERE exercise_id=?
                """;
        String sqlSs = """
                DELETE FROM SuitableSections
                WHERE exercise_id=?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sqlEq)) {
            statement.setInt(1, exercise.getExerciseId());
            statement.executeUpdate();
        }

        try (PreparedStatement statement = connection.prepareStatement(sqlMg)) {
            statement.setInt(1, exercise.getExerciseId());
            statement.executeUpdate();
        }

        try (PreparedStatement statement = connection.prepareStatement(sqlSs)) {
            statement.setInt(1, exercise.getExerciseId());
            statement.executeUpdate();
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
                    int exerciseId = keys.getInt(1);
                    creationSanityCheck(exerciseId, connection);
                    return exerciseId;
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
    public void saveMuscleGroups(Exercise exercise, Connection connection) throws SQLException {

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
                statement.setInt(1, exercise.getExerciseId());
                statement.setString(2, muscleGroup.name());
                statement.executeUpdate();
            }
        }
    }

    /*
    save junction entry for equipment
    according to an exercise.
     */
    public void saveEquipment(Exercise exercise, Connection connection) throws SQLException {

        String sql = """
                INSERT INTO ExerciseEquipment(exercise_id, equipment_id)
                VALUES(?,(SELECT equipment_id FROM Equipment WHERE name=?))
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, exercise.getExerciseId());
            statement.setString(2, exercise.getEquipment().name());
            statement.executeUpdate();
        }
    }

    /*
    write junction table entries for suitable sections
    according to a given exercise.
     */
    public void saveSuitableSections(Exercise exercise, Connection connection) throws SQLException {

        String sql = """
                INSERT INTO SuitableSections(exercise_id, section_type_id)
                VALUES(?, (SELECT section_type_id FROM SectionType WHERE name=?))
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (SectionType sectionType : exercise.getSuitableSections()) {
                statement.setInt(1, exercise.getExerciseId());
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
                    saveMuscleGroups(exercise, connection);
                    saveEquipment(exercise, connection);
                    saveSuitableSections(exercise, connection);
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
    helper for loading an exercise's equipment from JDBC
     */
    public Equipment loadEquipment(Connection connection, int exerciseId) throws SQLException {
        String sql = """
                SELECT e.name AS equipment_name
                FROM Equipment e
                JOIN ExerciseEquipment ee
                ON e.equipment_id = ee.equipment_id
                WHERE ee.exercise_id=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, exerciseId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Equipment.valueOf(result.getString("equipment_name"));
                }
            }
        }

        throw new SQLException("Error: Exercise with ID " + exerciseId + " was found to have no equipment.");
    }

    /*
    helper for loading an exercise's suitable sections from JDBC
     */
    public Set<SectionType> loadSuitableSections(Connection connection, int exerciseId) throws SQLException {
        Set<SectionType> suitableSections = new HashSet<>();

        String sql = """
                SELECT st.name AS section_name
                FROM SectionType st
                JOIN SuitableSections ss
                ON ss.section_type_id=st.section_type_id
                WHERE ss.exercise_id=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, exerciseId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    SectionType sectionType = SectionType.valueOf(
                            results.getString("section_name"));
                    suitableSections.add(sectionType);
                }
            }
        }

        if (suitableSections.isEmpty()) {
            throw new SQLException("Error: Exercise ID " + exerciseId + " found to have no suitable sections.");
        }

        return suitableSections;
    }

    /*
    helper method for loading an exercise's muscle groups from JDBC
     */
    public EnumSet<MuscleGroup> loadMuscleGroups(Connection connection, int exerciseId) throws SQLException {
        EnumSet<MuscleGroup> muscleGroups = EnumSet.noneOf(MuscleGroup.class);

        String sql = """
                SELECT mg.name AS muscle_group_name
                FROM MuscleGroup mg
                JOIN ExerciseMuscleGroup emg
                ON mg.muscle_group_id=emg.muscle_group_id
                WHERE emg.exercise_id=?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, exerciseId);
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    MuscleGroup muscleGroup = MuscleGroup.valueOf(
                            results.getString("muscle_group_name"));
                    muscleGroups.add(muscleGroup);
                }
            }
        }
        // EnumSet can be empty in the case of cardio.
        return muscleGroups;
    }

    /*
    loads all exercises in the library from JDBC
     */
    public List<Exercise> loadAllFromDatabase(Connection connection) throws SQLException {
        List<Exercise> exerciseList = new ArrayList<>();

        String sql = """
                SELECT *
                FROM Exercise
                ORDER BY exercise_id
                """;
        try (Statement statement = connection.createStatement()) {
            ResultSet results = statement.executeQuery(sql);

            while (results.next()) {

                /*
                reconstructing exercise objects from JDBC
                 */
                Exercise exercise = new Exercise();
                exercise.setExerciseId(results.getInt("exercise_id"));
                exercise.setName(results.getString("name"));

                // null value can pass through here...
                exercise.setDemoLink(results.getString("demo_url"));

                //...as well as here.
                exercise.setInstructions(results.getString("instructions"));

                Difficulty difficulty = Difficulty.valueOf(results.getString("difficulty"));
                exercise.setDifficulty(difficulty);

                MovementType movementType = MovementType.valueOf(results.getString("movement_type"));
                exercise.setMovementType(movementType);

                PrescriptionType prescriptionType = PrescriptionType.valueOf
                        (results.getString("prescription_type"));
                exercise.setPrescriptionType(prescriptionType);

                int exerciseId = results.getInt("exercise_id");

                /*
                loading junction table results
                 */
                EnumSet<MuscleGroup> muscleGroups = loadMuscleGroups(connection, exerciseId);
                exercise.setMuscleGroups(muscleGroups);

                Set<SectionType> suitableSections = loadSuitableSections(connection, exerciseId);
                exercise.setSuitableSections(suitableSections);

                Equipment equipment = loadEquipment(connection, exerciseId);
                exercise.setEquipment(equipment);


                exerciseList.add(exercise);
            }
        }
        return exerciseList;
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
    DEPRECATED.

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
