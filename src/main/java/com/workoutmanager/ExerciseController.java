package com.workoutmanager;

import enums.*;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.Exercise;

import java.net.URI;
import java.util.*;

/*
form used for creating a new exercise and adding it to
the program's persistent memory in JSON.
 */
public class ExerciseController {

    private Exercise modified;

    private Exercise exercise;

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
    private HBox muscleGroupContainer;

    @FXML
    private TextField exerciseDemoUrl;

    @FXML
    private TextArea exerciseInstructions;

    @FXML
    private Label errorMessageIndicator;

    @FXML
    private Button cancelButton;

    public Exercise getModified() { return modified; }

    public Exercise getExercise() {
        return exercise;
    }

    /*
    only called by Update CRUD operation
     */
    public void setExercise(Exercise exercise) {
        this.exercise = exercise;

        //pre-populate fields according to this exercise.

        exerciseName.setText(exercise.getName());

        difficultySelection.getSelectionModel().select(exercise.getDifficulty());

        equipmentSelection.getSelectionModel().select(exercise.getEquipment());

        prescriptionTypeSelection.getSelectionModel()
                .select(exercise.getPrescriptionType());

        movementTypeSelection.getSelectionModel().select(exercise.getMovementType());

        for (SectionType sectionType : exercise.getSuitableSections()) {
            suitableSectionSelection.getSelectionModel().select(sectionType);
        }

        for (MuscleGroup muscleGroup : exercise.getMuscleGroups()) {
            muscleGroupSelection.getSelectionModel().select(muscleGroup);
        }

        exerciseDemoUrl.setText(exercise.getDemoLink());

        exerciseInstructions.setText(exercise.getInstructions());
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

        prescriptionTypeSelection.getItems().addAll(PrescriptionType.values());
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

    @FXML
    private void onMovementTypeSelection() {
        MovementType movementType = movementTypeSelection.getValue();
        if (movementType == null) {
            return;
        }
        if (movementType == MovementType.CARDIO) {
            muscleGroupSelection.getSelectionModel().clearSelection();
            muscleGroupContainer.setVisible(false);
            muscleGroupContainer.setManaged(false);
        }
        else {
            muscleGroupContainer.setVisible(true);
            muscleGroupContainer.setManaged(true);
        }
    }

    /*
    validation helper; used by both creation
    and modification workflows.
     */
    @FXML
    private boolean validateInputs() {

        if (exerciseName.getText().isBlank()) {
            triggerErrorMessage("Please provide an exercise name.");
            return false;
        }

        if (difficultySelection.getValue() == null) {
            triggerErrorMessage("Please select an exercise difficulty level.");
            return false;
        }

        if (prescriptionTypeSelection.getValue() == null) {
            triggerErrorMessage("Please select a prescription type.");
            return false;
        }

        if (movementTypeSelection.getValue() == null) {
            triggerErrorMessage("Please select a movement type.");
            return false;
        }

        if (suitableSectionSelection.getSelectionModel().getSelectedItems().isEmpty()) {
            triggerErrorMessage("Please select one or more suitable workout sections.");
            return false;
        }

        if (equipmentSelection.getValue() == null) {
            triggerErrorMessage("Please select exercise equipment.");
            return false;
        }

        // if MovementType is Cardio, muscle group selection is nullified.
        if (muscleGroupSelection.getSelectionModel().getSelectedItems().isEmpty()
                && movementTypeSelection.getValue() != MovementType.CARDIO) {
            triggerErrorMessage("Please set one or more muscle groups.");
            return false;
        }
        // demo link optional; if input supplied, we validate the URL.
        if (!exerciseDemoUrl.getText().isEmpty()) {
            boolean isLinkValid = validateURL(exerciseDemoUrl.getText());

            if (!isLinkValid) {
                triggerErrorMessage("Please provide a valid demo URL, or no URL at all.");
                return false;
            }
        }

        return true;
    }

    /*
    saves exercise modifications, passing them
    back to the LibraryController.
     */
    @FXML
    private void saveExerciseModification() {

        modified = new Exercise();
        modified.setExerciseId(exercise.getExerciseId());
        /*
        for now, we will not bother to track
        Exercise state, and will assume that
        the user has changed something.
         */
        modified.setName(exerciseName.getText());

        modified.setDifficulty(difficultySelection.getValue());

        modified.setEquipment(equipmentSelection.getValue());

        modified.setPrescriptionType(prescriptionTypeSelection.getValue());

        modified.setMovementType(movementTypeSelection.getValue());

        EnumSet<SectionType> sectionTypes = EnumSet.copyOf(suitableSectionSelection
                .getSelectionModel().getSelectedItems());
        modified.setSuitableSections(sectionTypes);

        EnumSet<MuscleGroup> muscleGroups = EnumSet.copyOf(muscleGroupSelection
                .getSelectionModel().getSelectedItems());
        modified.setMuscleGroups(muscleGroups);

        modified.setDemoLink(exerciseDemoUrl.getText());

        modified.setInstructions(exerciseInstructions.getText());

        closeWindow();
    }

    /*
    take values from the form, validate,
    create new Exercise object, and pass it to ExerciseLibraryController.
     */
    @FXML
    private void saveExerciseCreation() {

        if (!validateInputs()) {
            return;
        }

        /*
        if exercise is not null, it's already
        been passed in and is being modified.
         */
        if (exercise != null) {
            saveExerciseModification();
        }

        else {

            //acquiring form values
            String name = exerciseName.getText();

            Difficulty difficulty = difficultySelection.getValue();

            PrescriptionType prescriptionType = prescriptionTypeSelection.getValue();

            MovementType movementType = movementTypeSelection.getValue();

            ObservableList<SectionType> selectedSectionTypes = suitableSectionSelection.selectionModelProperty()
                    .getValue().getSelectedItems();
            EnumSet<SectionType> sectionTypes = EnumSet.copyOf(selectedSectionTypes);

            Equipment equipment = equipmentSelection.getValue();

            ObservableList<MuscleGroup> selectedMuscleGroups = muscleGroupSelection.
                    selectionModelProperty().getValue().getSelectedItems();
            EnumSet<MuscleGroup> muscleGroups = movementType == MovementType.CARDIO ?
                    null : EnumSet.copyOf(selectedMuscleGroups);

            String demoLink = exerciseDemoUrl.getText();

            String instructions = exerciseInstructions.getText();

            exercise = new Exercise(name, demoLink, instructions,
                    muscleGroups, difficulty, equipment, prescriptionType,
                    movementType, sectionTypes);

            closeWindow();
        }
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
