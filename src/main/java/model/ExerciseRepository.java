package model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/*
responsible for parsing the exercise JSON file, deserializing the data and giving
us something we can make sense of.
 */

public class ExerciseRepository {

    /*
    loads the repository of exercises from the JSON file.
     */
    public List<Exercise> loadAll(String exerciseFilepath) {

        URL resource = getClass().getResource(exerciseFilepath);
        if (resource == null) {
            System.err.println("Could not find exercises.json");
            return null;
        }

        File file = null;
        try {
            file = new File(resource.toURI());
        } catch (URISyntaxException e) {
            System.err.println("Exercise file creation failed.");
            return null;
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
    public void saveAll(List<Exercise> exerciseList, String exerciseFilepath) {
        ObjectMapper mapper = new ObjectMapper();

        URL resource = getClass().getResource(exerciseFilepath);
        if (resource == null) {
            System.err.println("ExerciseRepository in saveAll(): could not find exercises.json");
            return;
        }

        File file;
        try {
            file = new File(resource.toURI());
        } catch (URISyntaxException e) {
            System.err.println("ExerciseRepository in saveAll(): failed to create exercise file.");
            throw new RuntimeException(e);
        }

        try {
            mapper.writeValue(file, exerciseList);
        } catch (IOException e) {
            System.err.println("Mapper failed to update exercise list in JSON");
            throw new RuntimeException(e);
        }
    }
}
