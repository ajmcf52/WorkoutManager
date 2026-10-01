package model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/*
responsible for parsing the exercise JSON file, deserializing the data and giving
us something we can make sense of.
 */

public class ExerciseRepository {

    /*
    loads the repository of exercises from the JSON file.
     */
    public List<Exercise> loadAll() {

        Path resource = Paths.get("appdata", "exercises.json");

        File file = null;
        try {
            file = new File(resource.toUri());
        } catch (RuntimeException e) {
            System.err.println("Exercise file creation failed.");
            throw e;
        }

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
