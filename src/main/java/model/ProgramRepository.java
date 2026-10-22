package model;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import database.DatabaseConnection;
import enums.DurationUnit;
import enums.Intensity;
import enums.SectionType;
import javafx.collections.FXCollections;

import javax.xml.transform.Result;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

/*
responsible for saving and loading workout programs.
utilizes the Jackson API to read/write in JSON.
 */
public class ProgramRepository {

    /*
    migrate old difficulty values to new ones.
     */
    public void migrateDifficultyValues(File file) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = null;
        try {
            root = mapper.readTree(file);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }

        for (JsonNode node : root) {
            ObjectNode workoutProgramNode = (ObjectNode) node;
            JsonNode sections = workoutProgramNode.get("workoutSections");
            if (!sections.isArray()) {
                System.out.println("migration failed.");
                return;
            }
            ArrayNode sectionArray = (ArrayNode) sections;
            for (JsonNode workoutSection : sectionArray) {
                JsonNode sectionExercises = workoutSection.get("exercises");
                ArrayNode exercises = (ArrayNode) sectionExercises;
                for (JsonNode exercise : exercises) {
                    ObjectNode exerciseNode = (ObjectNode) exercise.get("exercise");
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
            }
        }
        try {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file, root);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void saveProgramExercise(Connection connection,
                                    ProgramExercise programExercise,
                                    int workoutSectionId) throws  SQLException {
        String sql = """
                INSERT INTO ProgramExercise(workout_section_id, exercise_id,
                min_sets, max_sets, min_reps, max_reps, duration_value,
                duration_unit, intensity, notes)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, workoutSectionId);

            int exerciseId = programExercise.getExercise().getExerciseId();
            statement.setInt(2, exerciseId);

            Integer minSets = programExercise.getMinSets();
            Integer maxSets = programExercise.getMaxSets();
            if (minSets == null) {
                statement.setNull(3, Types.INTEGER);
                statement.setNull(4, Types.INTEGER);
            }
            else {
                statement.setInt(3, minSets);
                statement.setInt(4, maxSets);
            }

            Integer minReps = programExercise.getMinReps();
            Integer maxReps = programExercise.getMaxReps();
            if (minReps == null) {
                statement.setNull(5, Types.INTEGER);
                statement.setNull(6, Types.INTEGER);
            }
            else {
                statement.setInt(5, minReps);
                statement.setInt(6, maxReps);
            }

            Integer durationValue = programExercise.getDuration();
            if (durationValue == null) {
                statement.setNull(7, Types.INTEGER);
                statement.setNull(8, Types.VARCHAR);
            }
            else {
                String durationUnit = programExercise.getDurationUnit().name();
                statement.setInt(7, durationValue);
                statement.setString(8, durationUnit);
            }

            statement.setString(9,
                    programExercise.getIntensity().name());

            String notes = programExercise.getNotes();
            if (notes == null || notes.isBlank()) {
                statement.setNull(10, Types.VARCHAR);
            }
            else {
                statement.setString(10, notes);
            }

            // fields set; time to insert.
            statement.executeUpdate();
        }

    }

    public void saveWorkoutSection(Connection connection, WorkoutSection section, int programId) throws SQLException {
        int workoutSectionId = 0;
        String sql = """
                INSERT INTO WorkoutSection(workout_id, section_type_id)
                VALUES(?, (
                SELECT st.section_type_id
                FROM SectionType st
                WHERE st.name=?))
                """;
        try (PreparedStatement statement = connection.prepareStatement
                (sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, programId);
            statement.setString(2, section.getSectionType().name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No generated key for WorkoutSection. Program failure.");
                }
                workoutSectionId = keys.getInt(1);
            }
        }

        for (ProgramExercise prExercise : section.getExercises()) {
            saveProgramExercise(connection, prExercise, workoutSectionId);
        }
    }

    /**
     * CRUD operation used to save a WorkoutProgram object to JDBC
     * @param connection DB connection
     * @param program the workout program
     * @throws SQLException in case of bad DB operation(s)
     */
    public int saveWorkoutProgram(Connection connection, WorkoutProgram program) throws SQLException {
        int programId = 0;
        String sql = """
                INSERT INTO WorkoutProgram(program_name)
                VALUES(?)
                """;
        try (PreparedStatement statement = connection.prepareStatement
                (sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, program.getProgramName());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No generated key for WorkoutProgram. Exiting.");
                }
                programId = keys.getInt(1);
            }
        }

        // WorkoutProgram inserted; now onto WorkoutSections.
        for (WorkoutSection section: program.getWorkoutSections()) {
            saveWorkoutSection(connection, section, programId);
        }


        return programId;
    }

    /*
    one-time function used for mapping
    JSON to JDBC WorkoutProgram entities.
     */
    public void migrateWorkoutPrograms() throws SQLException {

        List<WorkoutProgram> workoutPrograms = loadAll();

        try (Connection connection = DatabaseConnection.openConnection()) {
        /*
        we will write all entries + sub-entries
        as one transaction; if anything fails,
        we roll back.
         */
            connection.setAutoCommit(false);



            String sql = """
                    INSERT INTO WorkoutProgram(program_name) VALUES(?)
                    """;
            for (WorkoutProgram workoutProgram : workoutPrograms) {
                Integer workoutId = null;
                // saving WorkoutProgram entity--top layer
                try (PreparedStatement statement = connection.prepareStatement(sql,
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, workoutProgram.getProgramName());
                    statement.executeUpdate();


                    try (ResultSet result = statement.getGeneratedKeys()) {
                        if (result.next()) {
                            workoutId = result.getInt(1);
                        }
                    }
                } catch (RuntimeException | SQLException e) {
                    connection.rollback();
                    throw new RuntimeException(e);
                }

                //saving WorkoutSections--middle layer

                String workoutSectionSql = """
                         INSERT INTO WorkoutSection(workout_id, section_type_id)
                         VALUES(?,
                         (SELECT section_type_id
                         FROM SectionType
                         WHERE name=?))
                        """;

                if (workoutId == null) {
                    throw new IllegalStateException();
                }

                try (PreparedStatement
                             statement =
                             connection.prepareStatement(

                                     workoutSectionSql, Statement.RETURN_GENERATED_KEYS)) {

                    for (WorkoutSection workoutSection : workoutProgram.getWorkoutSections()) {
                        String workoutSectionName =
                                workoutSection.getSectionType().name();

                        statement.setInt(1, workoutId);
                        statement.setString(2, workoutSectionName);
                        statement.executeUpdate();

                        Integer workoutSectionId = null;
                        try (ResultSet result = statement.getGeneratedKeys()) {
                            if (result.next()) {
                                workoutSectionId = result.getInt(1);
                            }
                        }

                        if (workoutSectionId == null) {
                            throw new IllegalStateException("workoutSectionId cannot be null. Exiting.");
                        }

                        String programExerciseSql = """
                                INSERT INTO ProgramExercise(workout_section_id, exercise_id,
                                min_sets, max_sets, min_reps, max_reps, duration_value,
                                duration_unit, intensity, notes) VALUES(?,(
                                    SELECT exercise_id
                                    FROM Exercise
                                    WHERE name=?
                                ),?,?,?,?,?,?,?,?)
                                """;
                        try (PreparedStatement prExStatement
                                     = connection.prepareStatement(programExerciseSql)) {
                            for (ProgramExercise programExercise : workoutSection.getExercises()) {

                                prExStatement.setInt(1, workoutSectionId);

                                prExStatement.setString(2,
                                        programExercise.getExercise().getName());

                                // if minSets == null, maxSets should also be null.
                                if (programExercise.getMinSets() == null) {
                                    prExStatement.setNull(3, Types.INTEGER);
                                    prExStatement.setNull(4, Types.INTEGER);
                                }
                                else {
                                    prExStatement.setInt(3,
                                            programExercise.getMinSets());
                                    prExStatement.setInt(4,
                                            programExercise.getMaxSets());
                                }

                                // setting variables for min/max reps (nullable)
                                if (programExercise.getMinReps() == null) {
                                    prExStatement.setNull(5, Types.INTEGER);
                                    prExStatement.setNull(6, Types.INTEGER);
                                }
                                else {
                                    prExStatement.setInt(5,
                                            programExercise.getMinReps());
                                    prExStatement.setInt(6,
                                            programExercise.getMaxReps());
                                }

                                //setting vars for duration unit/value (nullable)
                                if (programExercise.getDuration() == null) {
                                    prExStatement.setNull(7, Types.INTEGER);
                                    prExStatement.setNull(8, Types.VARCHAR);
                                }
                                else {
                                    prExStatement.setInt(7,
                                            programExercise.getDuration());
                                    prExStatement.setString(8,
                                            programExercise.getDurationUnit().name());
                                }

                                //setting intensity
                                prExStatement.setString(9,
                                        programExercise.getIntensity().name());

                                //setting notes
                                if (programExercise.getNotes() == null) {
                                    prExStatement.setNull(10, Types.VARCHAR);
                                }
                                else {
                                    prExStatement.setString(10,
                                            programExercise.getNotes());
                                }

                                prExStatement.executeUpdate();
                            }
                        }
                    }
                } catch (RuntimeException | SQLException e) {
                    connection.rollback();
                    throw new RuntimeException(e);
                }
            }
            // if we get here, our transaction is successful.
            connection.commit();
        }
    }

    /**
     * CRUD operation -- used to delete a workout
     * @param connection DB connection
     * @param workoutProgramId IDs a workout program
     * @throws SQLException in case of JDBC error
     */
    public void delete(Connection connection, int workoutProgramId) throws SQLException {

        String sql = """
                DELETE FROM WorkoutProgram
                WHERE workout_id=?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, workoutProgramId);
            statement.executeUpdate();
        }
    }

    /**
     *
     * @param connection Db connection
     * @param workoutSectionId IDs a unique workout section
     * @param sectionType (ie, WARM_UP)
     * @return new WorkoutSection with ProgramExercises attached.
     * @throws SQLException caught by higher order fn calls
     */
    public WorkoutSection loadWorkoutSection(Connection connection,
                                             int workoutSectionId,
                                             SectionType sectionType,
                                             HashMap<Integer, Exercise> exerciseMap) throws SQLException {
        WorkoutSection workoutSection = new WorkoutSection();

        workoutSection.setSectionType(sectionType);

        String programExercisesSql = """
                SELECT *
                FROM ProgramExercise
                WHERE workout_section_id=?
                """;

        List<ProgramExercise> programExercises = new ArrayList<ProgramExercise>();

        try (PreparedStatement programExQuery = connection.prepareStatement
                (programExercisesSql)) {
            programExQuery.setInt(1, workoutSectionId);
            try (ResultSet prExResults = programExQuery.executeQuery()) {

                // each result represents a ProgramExercise.
                while (prExResults.next()) {

                    ProgramExercise programExercise = new ProgramExercise();

                    programExercise.setProgramExerciseId(
                            prExResults.getInt("program_exercise_id")
                    );

                    Integer exerciseId = prExResults.getInt("exercise_id");
                    Exercise exercise = exerciseMap.get(exerciseId);
                    if (exercise == null) {
                        throw new IllegalStateException("exercise ID ot a "
                                + exerciseId + " found to be null. Exiting now.");
                    }
                    programExercise.setExercise(exercise);

                    int minSets = prExResults.getInt("min_sets");
                    programExercise.setMinSets(prExResults.wasNull() ? null : minSets);

                    int maxSets = prExResults.getInt("max_sets");
                    programExercise.setMaxSets(prExResults.wasNull() ? null : maxSets);

                    int minReps = prExResults.getInt("min_reps");
                    programExercise.setMinReps(prExResults.wasNull() ? null : minReps);

                    int maxReps = prExResults.getInt("max_reps");
                    programExercise.setMaxReps(prExResults.wasNull() ? null : maxReps);

                    int durationValue = prExResults.getInt("duration_value");
                    programExercise.setDuration(prExResults.wasNull() ? null : durationValue);

                    // small null check for duration type Enum
                    String durationUnitString = prExResults.getString("duration_unit");
                    programExercise.setDurationUnit(
                    durationUnitString == null ? null : DurationUnit.valueOf(durationUnitString));

                    programExercise.setIntensity(
                            Intensity.valueOf(
                                    prExResults.getString("intensity")
                            )
                    );

                    programExercise.setNotes(
                            prExResults.getString("notes")
                    );

                    // add newly created ProgramExercise
                    programExercises.add(programExercise);
                }
            }
        }
        workoutSection.setExercises(programExercises);

        return workoutSection;
    }

    public List<WorkoutProgram> loadAllWorkoutPrograms(
            Connection connection, HashMap<Integer, Exercise> exerciseById) throws SQLException {

        List<WorkoutProgram> workoutPrograms = new ArrayList<>();

        /*
        to reconstruct each WorkoutProgram,
        need its WorkoutSections and ProgramExercises.
         */
        String workoutProgramSql = """
                SELECT *
                FROM WorkoutProgram
                ORDER BY workout_id
                """;
        try (Statement statement = connection.createStatement()) {
            try (ResultSet results = statement.executeQuery(workoutProgramSql)) {
                while (results.next()) {

                    WorkoutProgram workoutProgram = new WorkoutProgram();

                    int workoutId = results.getInt("workout_id");
                    workoutProgram.setWorkoutProgramId(workoutId);

                    workoutProgram.setProgramName(
                            results.getString("program_name"));

                    /*
                    getting workout section IDs and
                    section type names in one go.
                     */
                    String sectionSql = """
                            SELECT ws.*, st.name as section_type_name
                            FROM WorkoutSection ws
                            JOIN SectionType st
                            ON ws.section_type_id=st.section_type_id
                            WHERE workout_id=?
                            ORDER BY section_type_id
                            """;

                    List<WorkoutSection> workoutSections = new ArrayList<WorkoutSection>();

                    try (PreparedStatement sectionQuery =
                                 connection.prepareStatement(sectionSql)) {
                        sectionQuery.setInt(1, workoutId);
                        try (ResultSet sectionResults = sectionQuery.executeQuery()) {
                            while (sectionResults.next()) {
                                /*
                                now for each section,
                                we can get the ProgramExercises
                                and reconstruct.
                                 */
                                int workoutSectionId = sectionResults.getInt(
                                        "workout_section_id"
                                );
                                SectionType sectionType = SectionType.valueOf(
                                        sectionResults.getString("section_type_name")
                                );

                                WorkoutSection workoutSection = loadWorkoutSection(
                                        connection,
                                        workoutSectionId,
                                        sectionType,
                                        exerciseById
                                );

                                workoutSections.add(workoutSection);

                            }
                        }
                    }
                    workoutProgram.setWorkoutSections(workoutSections);
                    workoutPrograms.add(workoutProgram);
                }
            }
        }

        return workoutPrograms;
    }

    /*
     * SOON TO BE DEPRECATED.
     * method for loading
     * all WorkoutPrograms
     * out of JSON.
     * Moving to JDBC.
     */
    public List<WorkoutProgram> loadAll() {

        Path resource = Paths.get("appdata", "programs.json");

        File file = null;
        try {
            file = new File(resource.toUri());
        } catch (RuntimeException e) {
            System.err.println("ProgramRepository: error creating I/O file.");
            throw e;
        }

        ObjectMapper mapper = new ObjectMapper();
        List<WorkoutProgram> loadedPrograms;
        try {
            loadedPrograms = mapper.readValue(file,
                    new TypeReference<List<WorkoutProgram>>() {});
        }
        //this catch clause is for the edge case of an empty JSON file.
        catch (MismatchedInputException e) {
            System.err.println(e.getMessage());
            return new ArrayList<WorkoutProgram>();
        }
        catch (IOException e) {
            System.err.println("Failed to read programs.json");
            throw new RuntimeException(e);
        }

        /*
        returns the loaded programs; if none are found in the JSON file,
        simply returns an empty list.
         */
        return loadedPrograms;
    }

    /*
    Soon to be Deprecated.
    Method used to save all
    WorkoutProgram objects
    to JSON.
    Moving to JDBC.
     */
    public void saveAll(List<WorkoutProgram> programList) {

        Path resource = Paths.get("appdata", "programs.json");

        File file = null;
        try {
            file = new File(resource.toUri());
        } catch (RuntimeException e) {
            System.err.println("Error creating I/O file from programs file path.");
            throw e;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(file, programList);
        } catch (IOException e) {
            System.err.println("Failed to write to programs.json");
            throw new RuntimeException(e);
        }
    }

}
