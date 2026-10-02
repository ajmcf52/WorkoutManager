package com.workoutmanager;

import com.dlsc.formsfx.model.structure.Section;
import enums.*;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Exercise;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/*
form used for creating a new exercise and adding it to
the program's persistent memory in JSON.
 */
public class NewExerciseController {

    private Exercise savedExercise;

    @FXML
    private TextField exerciseName;

    @FXML
    private ComboBox<Difficulty> difficultySelection;

    @FXML
    private ComboBox<Equipment> equipmentSelection;

    @FXML
    private ComboBox<PrescriptionType> prescriptionTypeSelection;

    @FXML
    private ComboBox<MovementType> movementTypeSelection;

    @FXML
    private ListView<SectionType> suitableSectionSelection;

    @FXML
    private ListView<MuscleGroup> muscleGroupSelection;

    @FXML
    private TextField exerciseDemoUrl;

    @FXML
    private TextArea exerciseInstructions;

    @FXML
    private Label errorMessageIndicator;

    @FXML
    private Button saveButton;

    @FXML
    private Button cancelButton;

    public Exercise getExercise() {
        return savedExercise;
    }

    @FXML
    public void initialize() {

        //populate selections for equipment, difficulty, and muscle groups
        //removing first item in each case, as that is the NULL("All") option used in search filtering.
        ArrayList<Difficulty> exerciseDifficulties = new ArrayList<Difficulty>(Arrays.stream(Difficulty.values()).toList());
        exerciseDifficulties.removeFirst();
        difficultySelection.getItems().addAll(exerciseDifficulties);

        ArrayList<Equipment> exerciseEquipments = new ArrayList<Equipment>
                (Arrays.stream(Equipment.values()).toList());
        exerciseEquipments.removeFirst();
        equipmentSelection.getItems().addAll(exerciseEquipments);

        ArrayList<PrescriptionType> prescriptionTypes = new ArrayList<>
                (Arrays.stream(PrescriptionType.values()).toList());
        prescriptionTypeSelection.getItems().addAll(prescriptionTypes);

        //TODO refactor above statements to more simply get all enum values.

        movementTypeSelection.getItems().addAll(MovementType.values());

        suitableSectionSelection.getItems().addAll(SectionType.values());
        suitableSectionSelection.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        ArrayList<MuscleGroup> muscleGroups = new ArrayList<MuscleGroup>
                (Arrays.stream(MuscleGroup.values()).toList());
        muscleGroups.removeFirst();
        muscleGroupSelection.getItems().addAll(muscleGroups);

        //multiple muscle group selection enabled.
        muscleGroupSelection.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        /*
        if an error message has appeared from bad input,
        it should be cleared once the user re-selects
        one of the given fields.
         */
        exerciseName.textProperty().addListener((observable, oldValue, newValue) -> {
            clearErrorMessage();
        });
        difficultySelection.valueProperty().addListener((observable, oldValue, newValue) -> {
            clearErrorMessage();
        });
        equipmentSelection.valueProperty().addListener((observable, oldValue, newValue) -> {
            clearErrorMessage();
        });
        prescriptionTypeSelection.valueProperty().addListener(observable -> {
            clearErrorMessage();
        });
        muscleGroupSelection.getSelectionModel().getSelectedItems().addListener((ListChangeListener<MuscleGroup>) change -> {
            clearErrorMessage();
        });
        exerciseDemoUrl.textProperty().addListener((observable, oldValue, newValue) -> {
            clearErrorMessage();
        });
        exerciseInstructions.textProperty().addListener((observable, oldValue, newValue) -> {
            clearErrorMessage();
        });

    }

    /*
    take values from the form, validate,
    create new Exercise object, and pass it to ExerciseLibraryController.
     */
    @FXML
    private void saveExerciseCreation() {
        /*
        values we're collecting:
        -exercise name (no special characters or numbers--letters only)
        -difficulty
        -equipment
        -muscle groups
        prescription type
        -demo link URL
        -instructions
         */

        String name = exerciseName.getText();

        //acquiring form values and validating
        if (name.isBlank()) {
            triggerErrorMessage("Please provide an exercise name.");
            return;
        }

        Difficulty difficulty = difficultySelection.getValue();
        if (difficulty == null) {
            triggerErrorMessage("Please select an exercise difficulty level.");
            return;
        }

        PrescriptionType prescriptionType = prescriptionTypeSelection.getValue();
        if (prescriptionType == null) {
            triggerErrorMessage("Please select a prescription type.");
            return;
        }

        MovementType movementType = movementTypeSelection.getValue();
        if (movementType == null) {
            triggerErrorMessage("Please select a movement type.");
            return;
        }

        ObservableList<SectionType> selectedSectionTypes = suitableSectionSelection.selectionModelProperty()
                .getValue().getSelectedItems();

        if (selectedSectionTypes.isEmpty()) {
            triggerErrorMessage("Please select one or more suitable workout sections.");
            return;
        }
        EnumSet<SectionType> sectionTypes = EnumSet.copyOf(selectedSectionTypes);

        Equipment equipment = equipmentSelection.getValue();
        if (equipment == null) {
            triggerErrorMessage("Please select exercise equipment.");
            return;
        }
        ObservableList<MuscleGroup> selectedMuscleGroups = muscleGroupSelection.
                selectionModelProperty().getValue().getSelectedItems();

        if (selectedMuscleGroups.isEmpty()) {
            triggerErrorMessage("Please set one or more muscle groups.");
            return;
        }
        EnumSet<MuscleGroup> muscleGroups = EnumSet.copyOf(selectedMuscleGroups);

        String demoLink = exerciseDemoUrl.getText();
        boolean isLinkValid = validateURL(demoLink);

        if (!isLinkValid) {
            triggerErrorMessage("Please provide a valid demo URL.");
            return;
        }

        String instructions = exerciseInstructions.getText();

        // instructions
        if (instructions.isBlank()) {
            triggerErrorMessage("Please provide exercise instructions");
            return;
        }

        savedExercise = new Exercise(name,demoLink,instructions,
                muscleGroups,difficulty,equipment,prescriptionType,
                movementType, sectionTypes);

        closeWindow();
    }

    /*
    returns true if the demo URL is considered valid; false otherwise.
     */
    private boolean validateURL(String demoURL) {
        URI demoURI;

        try {
            demoURI = URI.create(demoURL);
        } catch (Exception e) {
            return false;
        }

        String uriScheme = demoURI.getScheme();
        if (uriScheme == null) {
           return false;
        }

        uriScheme = uriScheme.toLowerCase();
        if (!uriScheme.equals("http") && !uriScheme.equals("https")) {
            return false;
        }

        return !demoURI.getHost().isBlank();
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    /*
    calls method to close the exercise creation window.
     */
    @FXML
    private void cancelExerciseCreation() {
        closeWindow();
    }

    /*
    communicates a problem with the form's user inputted values.
     */
    @FXML
    private void triggerErrorMessage(String errorMessage) {
        errorMessageIndicator.setText(errorMessage);
        errorMessageIndicator.setVisible(true);
    }

    // clears the error.
    @FXML
    private void clearErrorMessage() {
        errorMessageIndicator.setText("");
        errorMessageIndicator.setVisible(false);
    }

}
