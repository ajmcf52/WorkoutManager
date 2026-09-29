package com.workoutmanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import model.Exercise;
import model.WorkoutProgram;

public class ProgramBuilderController {

    @FXML
    private TableView<Exercise> availableExercisesView;

    @FXML
    private TableView<Exercise> programExercisesView;

    @FXML
    private TableColumn<Exercise, String> availableExercisesColumn;

    @FXML
    private TableColumn<Exercise, String> programExercisesColumn;

    @FXML
    private TextField programNameField;

    private ObservableList<Exercise> allExercises;

    private ObservableList<Exercise> programExercises;

    private WorkoutProgram workoutProgram;

    public WorkoutProgram getWorkoutProgram() {
        return workoutProgram;
    }

    public void setExerciseList(ObservableList<Exercise> exercises) {

        //making a shallow copy to avoid mutating original master list
        allExercises = FXCollections.observableArrayList(exercises);
        availableExercisesView.setItems(allExercises);
    }

    @FXML
    public void initialize() {
        availableExercisesColumn.setCellValueFactory(
                new PropertyValueFactory<>("name"));
        programExercisesColumn.setCellValueFactory(
                new PropertyValueFactory<>("name"));

    }

    @FXML
    private void addExerciseToProgram() {

    }

    @FXML
    private void removeExercise() {

    }

    @FXML
    private void saveProgram() {

    }

    @FXML
    private void cancelProgramCreation() {

    }
}