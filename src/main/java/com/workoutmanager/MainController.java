package com.workoutmanager;

import database.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import model.Exercise;
import model.ExerciseRepository;
import model.ProgramRepository;
import model.WorkoutProgram;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;

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

        try (Connection connection = DatabaseConnection.openConnection()) {
            allExercises = FXCollections.
                    observableList(exerciseRepository.loadAllFromDatabase(connection));

            //exerciseRepository.cleanUpOrphanedRows(connection);

        } catch (SQLException e) {
            System.err.println("Error loading exercises from JDBC. Exiting now.");
            throw new RuntimeException(e);
        }

        programRepository = new ProgramRepository();
        allPrograms = FXCollections.
                observableList(programRepository.loadAll());

        // step 2 -- inject data into appropriate controllers
        exerciseLibraryController.setExerciseList(allExercises);
        programLibraryController.setExerciseList(allExercises);
        programLibraryController.setWorkoutPrograms(allPrograms);

        // step 3 -- callback setup

        //new exercise creation CB (JDBC)
        exerciseLibraryController.setOnExerciseCreated(exercise -> {

            try (Connection connection = DatabaseConnection.openConnection()) {
                int exerciseId = exerciseRepository.save(exercise, connection);

                exerciseRepository.saveSuitableSections(exerciseId, exercise, connection);
                exerciseRepository.saveEquipment(exerciseId, exercise, connection);
                exerciseRepository.saveMuscleGroups(exerciseId, exercise, connection);

                exercise.setExerciseId(exerciseId);
            }
            catch (SQLException e) {
                System.err.println("Exercise failed to write (JDBC). Exiting now.");
                throw new RuntimeException(e);
            }

            allExercises.add(exercise);
        });

        // exercise deletion callback (JDBC)
        exerciseLibraryController.setOnExerciseDeleted(exercise -> {

            allExercises.remove(exercise);

            try (Connection connection = DatabaseConnection.openConnection()) {
                    exerciseRepository.delete(exercise,connection);
            }
            catch (SQLException e) {
                System.err.println("Exercise deletion failed in JDBC. Exiting.");
                throw new RuntimeException(e);
            }
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
