package model;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import database.DatabaseConnection;
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

    public List<WorkoutProgram> loadAllWorkoutPrograms(Connection connection) throws SQLException {
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
                    workoutProgram.setProgramName(
                            results.getString("program_name"));
                    int workoutId = results.getInt("workout_id");

                    /*
                    with a workout ID, we can query workout
                    sections and rebuild them with their
                    programmed exercises, one section at a time.
                     */
                    String sectionSql = """
                            SELECT *
                            FROM WorkoutSection
                            WHERE workout_id=?
                            """;
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

                                WorkoutSection section = new WorkoutSection();
                                int workoutSectionId = sectionResults.getInt(
                                        "workout_section_id"
                                );

                                /*
                                identifying the section type.
                                 */
                                int sectionTypeId = sectionResults.getInt(
                                        "section_type_id"
                                );
                                String sectionTypeSql = """
                                        SELECT name
                                        FROM SectionType
                                        WHERE section_type_id=?
                                        """;
                                try (PreparedStatement sectionTypeQuery =
                                             connection.prepareStatement(sectionTypeSql)) {
                                    sectionTypeQuery.setInt(1,sectionTypeId);

                                    try (ResultSet sectionTypeResult = sectionTypeQuery.executeQuery()) {
                                        if (sectionTypeResult.next()) {
                                            section.setSectionType(SectionType.valueOf(
                                                    sectionTypeResult.getString("name")
                                            ));
                                        }
                                    }
                                }

                                /*
                                identifying the program exercises that
                                go along with this workout section.
                                 */
                                String programExercisesSql = """
                                        SELECT *
                                        FROM ProgramExercise
                                        WHERE workout_section_id=?
                                        """;

                                try (PreparedStatement programExQuery = connection.prepareStatement
                                        (programExercisesSql)) {
                                    programExQuery.setInt(1,workoutSectionId);
                                    try (ResultSet prExResults = programExQuery.executeQuery()) {

                                        /*
                                        now for each result, rebuild
                                        a ProgramExercise object.
                                         */
                                        while (prExResults.next()) {

                                        }
                                    }
                                }

                            }
                        }
                    }
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
