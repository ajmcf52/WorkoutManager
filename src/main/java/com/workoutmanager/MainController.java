package com.workoutmanager;

import database.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import model.Exercise;
import model.ExerciseRepository;
import model.ProgramRepository;
import model.WorkoutProgram;
import org.sqlite.core.DB;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
                    exercise.setExerciseId(exerciseId);

                    exerciseRepository.saveSuitableSections(exercise, connection);
                    exerciseRepository.saveEquipment(exercise, connection);
                    exerciseRepository.saveMuscleGroups(exercise, connection);

                    connection.commit();

                    allExercises.add(exercise);
                    exerciseById.put(exerciseId, exercise);

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

        // exercise UPDATE callback
        exerciseLibraryController.setOnExerciseModified((exercise, modified) -> {

            try (Connection connection = DatabaseConnection.openConnection()) {
                connection.setAutoCommit(false);

                try {
                    int index = allExercises.indexOf(exercise);
                    if (index < 0) {
                        throw new IllegalStateException(
                                "Exercise " + exercise.getExerciseId() +
                                " not found in master list.");
                    }
                    exerciseRepository.update(modified, connection);
                    connection.commit();

                    allExercises.set(index, modified);
                    exerciseById.put(modified.getExerciseId(), modified);
                }
                catch (SQLException e) {
                    connection.rollback();
                    System.err.println("UPDATE exercise " + exercise.getExerciseId() +
                            " failed; rolling back.");
                    throw new RuntimeException(e);
                }
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        // exercise library search filter change callback
        exerciseLibraryController.setOnFilterChanged(exerciseFilter -> {

            try (Connection connection = DatabaseConnection.openConnection()) {
                List<Integer> exerciseIds = exerciseRepository
                        .filterExercises(exerciseFilter, connection);

                List<Exercise> exerciseList = new ArrayList<>();
                for (Integer exerciseId : exerciseIds) {
                    exerciseList.add(exerciseById.get(exerciseId));
                }
                return exerciseList;
        }
            catch (SQLException e) {
                System.err.println("Error filtering exercise library" +
                        " search results (JDBC)");
                throw new RuntimeException(e);
            }
        });


        // exercise deletion callback (JDBC)
        exerciseLibraryController.setOnExerciseDeleted(exercise -> {

            try (Connection connection = DatabaseConnection.openConnection()) {
                    exerciseRepository.delete(exercise,connection);
            }
            catch (SQLException e) {
                /*
                error code 19 == SQLITE_CONSTRAINT error,
                which in the case of an Exercise deletion,
                should only ever pop for FK_CONSTRAINT error.
                 */
                if (e.getErrorCode() == 19) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Delete Failed");
                    alert.setHeaderText("Exercise used in a Workout program cannot be deleted.");
                    alert.showAndWait();
                }
                else {
                    System.err.println("Exercise deletion failed in JDBC. Exiting.");
                    throw new RuntimeException(e);
                }
            }

            allExercises.remove(exercise);
            exerciseById.remove(exercise.getExerciseId());
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

            try (Connection connection = DatabaseConnection.openConnection()) {
                programRepository.delete(connection, program.getWorkoutProgramId());
            }
            catch (SQLException e) {
                System.err.println("Program " + program.getProgramName() + " failed; exiting.");
                throw new RuntimeException(e);
            }

            allPrograms.remove(program);
        });
    }
}
