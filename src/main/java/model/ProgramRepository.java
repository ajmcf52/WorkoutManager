package model;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import javafx.collections.FXCollections;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/*
responsible for saving and loading workout programs.
utilizes the Jackson API to read/write in JSON.
 */
public class ProgramRepository {

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
            return Collections.emptyList();
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
