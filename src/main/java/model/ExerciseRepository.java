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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/*
responsible for parsing the exercise JSON file, deserializing the data and giving
us something we can make sense of.
 */

public class ExerciseRepository {

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
        } catch (SQLException e) {
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
        //migrateDifficultyValues(file);

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(file, new TypeReference<List<Exercise>>() {}
            );
        } catch (IOException e) {
            System.err.println("Failed to read exercise file.");
        } catch (Exception e) {
            System.err.println("Unknown file reading error.");
            throw e;
        }

        return null; // if we get here, mapper.readValue failed.
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
