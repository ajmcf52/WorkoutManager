package com.workoutmanager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Exercise;
import model.WorkoutProgram;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

/*
main controller for the Program Library view.
 */
public class ProgramLibraryController {

    @FXML
    private ListView<WorkoutProgram> programsListView;

    private ObservableList<WorkoutProgram> allWorkoutPrograms;

    private ObservableList<Exercise> allExercises;

    @FXML
    private Label selectedProgramLabel;

    @FXML
    private VBox detailsPane;

    @FXML
    private ListView<Exercise> programDetailsView;

    private Consumer<WorkoutProgram> onProgramCreated;

    private Consumer<WorkoutProgram> onProgramDeleted;

    // linking callbacks to MainController
    public void setOnProgramCreated(Consumer<WorkoutProgram> callback) {
        this.onProgramCreated = callback;
    }

    public void setOnProgramDeleted(Consumer<WorkoutProgram> callback) {
        this.onProgramDeleted = callback;
    }

    /*
    public setter for workout programs. Allows Main controller to inject program data.
     */
    public void setWorkoutPrograms(ObservableList<WorkoutProgram> allPrograms) {
        allWorkoutPrograms = allPrograms;
        programsListView.setItems(allWorkoutPrograms);
    }

    /*
    data injection for exercise list.
    sole purpose is to pass data to
    ProgramBuilderController when needed.
     */
    public void setExerciseList(ObservableList<Exercise> allExercises) {
        this.allExercises = allExercises;
    }

    @FXML
    private void showSelectedProgram(WorkoutProgram program) {
        programDetailsView
                .setItems(FXCollections.observableList(program.getExercises()));
        detailsPane.setVisible(true);
    }

    @FXML
    private void hideExerciseDetails() {
        detailsPane.setVisible(false);
    }

    /*
    opens a secondary modal for creating a program.
     */
    @FXML
    private void createNewProgramWindow() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/workoutmanager/program-builder-view.fxml"));
        Scene scene;
        try {
            scene = new Scene(loader.load());
        } catch (IOException e) {
            System.err.println("Program builder window failed to load.");
            throw new RuntimeException(e);
        }
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);

        ProgramBuilderController controller = loader.getController();
        controller.setExerciseList(allExercises);

        controller.setProgramNameAvailable(name -> {

            return allWorkoutPrograms.stream().noneMatch(program ->
                    program.getProgramName().trim().equalsIgnoreCase(name.trim()));
        });

        stage.showAndWait();

        WorkoutProgram program = controller.getWorkoutProgram();

        onProgramCreated.accept(program);
    }

    // delete button action listener
    public void deleteProgram() {

        //determine which program to delete
        WorkoutProgram selected = programsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        // confirm deletion to avoid user misclicks
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Program");
        alert.setHeaderText("Delete \"" + selected.getProgramName() + "\"?");

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            //trigger the callback
            onProgramDeleted.accept(selected);
        }


    }

    public void initialize() {

        // set cell factory for workout program list view
        programsListView.setCellFactory(listView -> new ListCell<>() {

            @Override
            protected void updateItem(WorkoutProgram program, boolean isEmpty) {
                super.updateItem(program, isEmpty);

                if (isEmpty || program == null) {
                    setText("");
                }
                else {
                    setText(program.getProgramName());
                }
            }
        });

        // set cell factory for program details pane
        programDetailsView.setCellFactory(listView -> new ListCell<>() {

            @Override
            protected void updateItem(Exercise exercise, boolean isEmpty) {
                super.updateItem(exercise, isEmpty);

                if (isEmpty || exercise == null) {
                    setText("");
                }
                else {
                    setText(exercise.getName());
                }
            }
        });

        //setup listener for program list cell action
        programsListView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue == null) {
                        hideExerciseDetails();
                        selectedProgramLabel.setText("");
                    }
                    else {
                        selectedProgramLabel.setText(newValue.getProgramName());
                        showSelectedProgram(newValue);
                    }
                });
    }
}
