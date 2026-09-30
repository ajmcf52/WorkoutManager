package com.workoutmanager;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import model.Exercise;
import model.WorkoutProgram;

import java.util.ArrayList;
import java.util.List;

public class ProgramBuilderController {

    @FXML
    private ListView<Exercise> availableExercisesView;

    private FilteredList<Exercise> searchedAvailable;

    @FXML
    private ListView<Exercise> programExercisesView;

    @FXML
    private TextField programNameField;

    @FXML
    private TextField searchField;

    @FXML
    private Label errorMessageIndicator;

    private ObservableList<Exercise> availableExercises;

    private ObservableList<Exercise> programExercises;

    private WorkoutProgram workoutProgram;

    public WorkoutProgram getWorkoutProgram() {
        return workoutProgram;
    }

    /*
    setter for available exercise list. setter is called
    before showAndWait() from main modal controller.
     */
    public void setExerciseList(ObservableList<Exercise> exercises) {

        //making a shallow copy to avoid mutating original master list
        availableExercises = FXCollections.observableArrayList(exercises);
        searchedAvailable = new FilteredList<>(availableExercises);
        availableExercisesView.setItems(searchedAvailable);
    }

    @FXML
    public void initialize() {

        programExercises = FXCollections.observableArrayList();
        programExercisesView.setItems(programExercises);

        /*
        allows ListView to solely display the Exercise name
        instead of a full block of text displaying all
        exercise info.
         */
        availableExercisesView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Exercise exercise, boolean empty) {
                super.updateItem(exercise, empty);

                if (empty || exercise == null) {
                    setText(null);
                }
                else {
                    setText(exercise.getName());
                }
            }
        });

        // clears possible "Program empty" error message
        // when user takes further action to modify program name/exercises.
        availableExercisesView.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<Exercise>) change -> {
            clearErrorMessage();
        });
        programNameField.textProperty().addListener((observable -> {
            clearErrorMessage();
        }));

        // search predicate listener
        searchField.textProperty().addListener(observable -> {
            updatePredicate();
        });

    }

    //NOTE one small thing to consider.
    // if we add then remove a program,
    // should it go back in the same spot
    // of the original list, and not at the bottom of the list?

    /*
    helper method; adds a selected exercise to the program currently being built.
     */
    @FXML
    private void addExerciseToProgram() {

        Exercise exerciseToAdd = availableExercisesView.getSelectionModel()
                .getSelectedItem();

        // check for no exercise selected.
        if (exerciseToAdd == null) {
            return;
        }

        /*
        room for improvement here; if we use a Hashed data structure,
        this removal becomes O(1) in time.
         */
        availableExercises.remove(exerciseToAdd);
        programExercises.add(exerciseToAdd);

    }

    /*
    removes a selected exercise from the program currently being built;
    adds it back to "available exercises".
     */
    @FXML
    private void removeExercise() {

        Exercise exerciseToRemove = programExercisesView.getSelectionModel()
                .getSelectedItem();

        // if no program exercise is selected
        // to remove, simply return.
        if (exerciseToRemove == null) {
            return;
        }

        programExercises.remove(exerciseToRemove);
        availableExercises.add(exerciseToRemove);
    }

    /*
    saves the program being created by the user.
     */
    @FXML
    private void saveProgram() {

        //validate user input.
        String programName = programNameField.getText();
        if (programName.isEmpty()) {
            triggerErrorMessage("Please provide a name for your program.");
            return;
        }

        List<Exercise> programExerciseList = programExercisesView.getItems();
        if (programExerciseList.isEmpty()) {
            triggerErrorMessage("Program empty. Please select one or more exercises to add.");
            return;
        }

        workoutProgram = new WorkoutProgram(programName, programExerciseList);
        closeWindow();
    }

    /*
    updates the search predicate on
    list of available exercises.
     */
    private void updatePredicate() {
        String searchText = searchField.getText().toLowerCase();

        searchedAvailable.setPredicate(exercise -> {

            if (searchText.isEmpty()) {
                return true;
            }
            else {
                return exercise.getName().toLowerCase()
                        .contains(searchText);
            }
        });

        availableExercisesView.setItems(searchedAvailable);
    }

    /*
    user-facing error handling.
     */
    @FXML private void triggerErrorMessage(String errorMessage) {
        errorMessageIndicator.setText(errorMessage);
        errorMessageIndicator.setVisible(true);
    }

    @FXML
    private void clearErrorMessage() {
        errorMessageIndicator.setText("");
        errorMessageIndicator.setVisible(false);
    }

    /*
    cancels program creation; closes secondary modal window.
     */
    @FXML
    private void cancelProgramCreation() {
        workoutProgram = null;
        closeWindow();
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) programNameField.getScene().getWindow();
        stage.close();
    }
}