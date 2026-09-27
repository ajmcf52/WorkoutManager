package model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.Exercise;

import java.io.File;
import java.io.IOException;
import java.util.List;

/*
responsible for parsing the exercise JSON file, deserializing the data and giving
us something we can make sense of.
 */

public class ExerciseJsonLoader {
    public List<Exercise> loadExercises(String exerciseFile) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(new File(exerciseFile), new TypeReference<List<Exercise>>() {}
            );
        } catch (IOException e) {
            System.err.println("Failed to read exercise file.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unknown file reading error: refer to stack trace.");
            e.printStackTrace();
        }
        return null; // if we get here, mapper.readValue failed.
    }
}
