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
import java.util.HashMap;

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

    private HashMap<Integer, Exercise> exerciseById;

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
        } catch (SQLException e) {
            System.err.println("Error loading exercises from JDBC. Exiting now.");
            throw new RuntimeException(e);
        }

        // step 1b -- build the exercise HashMap

        exerciseById = new HashMap<Integer, Exercise>();
        for (Exercise exercise : allExercises) {
            exerciseById.put(exercise.getExerciseId(), exercise);
        }

        programRepository = new ProgramRepository();
        try (Connection connection = DatabaseConnection.openConnection()) {
            allPrograms = FXCollections.
                    observableList(programRepository.loadAllWorkoutPrograms(connection, exerciseById));
        }
        catch (SQLException e) {
            System.err.println("Error loading WorkoutPrograms from JDBC. Exiting now.");
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }

        // step 2 -- inject data into appropriate controllers
        exerciseLibraryController.setExerciseList(allExercises);
        programLibraryController.setExerciseList(allExercises);
        programLibraryController.setWorkoutPrograms(allPrograms);

        // step 3 -- callback setup

        //new exercise creation CB (JDBC)
        exerciseLibraryController.setOnExerciseCreated(exercise -> {

            try (Connection connection = DatabaseConnection.openConnection()) {

                connection.setAutoCommit(false);
                try {
                    int exerciseId = exerciseRepository.save(exercise, connection);

                    exerciseRepository.saveSuitableSections(exerciseId, exercise, connection);
                    exerciseRepository.saveEquipment(exerciseId, exercise, connection);
                    exerciseRepository.saveMuscleGroups(exerciseId, exercise, connection);
                    connection.commit();

                    exercise.setExerciseId(exerciseId);
                    allExercises.add(exercise);


                } catch (SQLException e) {
                    connection.rollback();
                    System.err.println("Exercise failed to write (JDBC). Exiting now.");
                    throw new RuntimeException(e);
                }
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }


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

            try (Connection connection = DatabaseConnection.openConnection()) {
                connection.setAutoCommit(false);

                try {
                    int workoutProgramId = programRepository.saveWorkoutProgram(
                            connection, program);
                    connection.commit();
                    program.setWorkoutProgramId(workoutProgramId);
                    allPrograms.add(program);
                }
                catch (SQLException e) {
                    connection.rollback();
                    System.err.println("Failed to write WorkoutProgram to JDBC. Rolling back.");
                    throw new RuntimeException(e);
                }
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        // program deletion CB
        programLibraryController.setOnProgramDeleted(program -> {
            allPrograms.remove(program);
            programRepository.saveAll(allPrograms);
        });
    }
}
