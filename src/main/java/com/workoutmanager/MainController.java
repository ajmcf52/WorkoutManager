package com.workoutmanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import model.Exercise;
import model.ExerciseRepository;
import model.ProgramRepository;
import model.WorkoutProgram;

import java.net.URL;

/*
main controller of the application.
responsibilities include:
- ownership of master data lists (allExercises, allPrograms)
- initial loading of master data lists
- passing data collections into ProgramLibrary
    and ExerciseLibrary (via controller setters)
 */
public class MainController {

    private ExerciseRepository exerciseRepository;
    private ProgramRepository programRepository;

    private ObservableList<Exercise> allExercises;
    private ObservableList<WorkoutProgram> allPrograms;

    @FXML
    private ExerciseLibraryController exerciseLibraryController;

    @FXML
    private ProgramLibraryController programLibraryController;

    /*
    this is where the bulk of the "main" work occurs:
    - loading exercise/program data
    - passing data collections into
        their appropriate controllers
    - setup callbacks for data updates
     */
    public void initialize() {

        // step 1 -- load data (exercises, programs)
        exerciseRepository = new ExerciseRepository();
        allExercises = FXCollections.
                observableList(exerciseRepository.loadAll());

        programRepository = new ProgramRepository();
        allPrograms = FXCollections.
                observableList(programRepository.loadAll());

        // step 2 -- inject data into appropriate controllers
        exerciseLibraryController.setExerciseList(allExercises);
        programLibraryController.setExerciseList(allExercises);
        programLibraryController.setWorkoutPrograms(allPrograms);

        // step 3 -- callback setup

        //new exercise creation CB
        exerciseLibraryController.setOnExerciseCreated(exercise -> {
            allExercises.add(exercise);
            exerciseRepository.saveAll(allExercises);
        });

        // new workout program CB
        programLibraryController.setOnProgramCreated(program -> {
            allPrograms.add(program);
            programRepository.saveAll(allPrograms);
        });

        // program deletion CB
        programLibraryController.setOnProgramDeleted(program -> {
            allPrograms.remove(program);
            programRepository.saveAll(allPrograms);
        });
    }
}
