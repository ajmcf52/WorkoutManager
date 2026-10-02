package com.workoutmanager;

import enums.Intensity;
import enums.PrescriptionType;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import model.Exercise;
import model.ProgramExercise;

public class ProgramExerciseController {

    // exercise we are prescribing sets/reps for
    private Exercise exercise;

    private ProgramExercise programExercise;

    @FXML
    private Label exerciseLabel;

    @FXML
    private TextField minSetsField;

    @FXML
    private TextField maxSetsField;

    @FXML
    private ComboBox<Intensity> intensitySelections;

    @FXML
    private TextField exerciseNotesField;

    // if the exercise is rep-based,
    // ask user to fill in reps.
    @FXML
    private HBox repRangeContainer;

    @FXML
    private TextField minRepsField;

    @FXML
    private TextField maxRepsField;

    // if the exercise is time/duration-based,
    // ask user to fill in time duration (sec)
    @FXML
    private HBox durationContainer;

    @FXML
    private TextField durationField;

    @FXML
    private Label errorMessageDisplay;

    /*
    sets the exercise to be prescribed.
    makes appropriate fields based off
    prescription type.
     */
    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
        if (this.exercise.getPrescriptionType()
                == PrescriptionType.DURATION) {
            durationContainer.setVisible(true);
        }
        else {
            repRangeContainer.setVisible(true);
        }
    }

    // allows parental access of ProgramExercise.
    public ProgramExercise getProgramExercise() {
        return programExercise;
    }

    @FXML
    public void initialize() {
        intensitySelections.getItems().addAll(Intensity.values());

        minRepsField.textProperty().addListener(observable -> {
            clearErrorMessage();
        });
        maxRepsField.textProperty().addListener(observable -> {
            clearErrorMessage();
        });
        minSetsField.textProperty().addListener(observable -> {
            clearErrorMessage();
        });
        maxSetsField.textProperty().addListener(observable -> {
            clearErrorMessage();
        });
        durationField.textProperty().addListener(observable -> {
            clearErrorMessage();
        });
        intensitySelections.valueProperty().addListener(observable -> {
            clearErrorMessage();
        });
        exerciseNotesField.textProperty().addListener(observable -> {
            clearErrorMessage();
        });
    }

    public void clearErrorMessage() {
        errorMessageDisplay.setText("");
        errorMessageDisplay.setVisible(false);
    }

    public void triggerErrorMessage(String msg) {
        errorMessageDisplay.setText(msg);
        errorMessageDisplay.setVisible(true);
    }

    /*
    saves a prescribed exercise to the program its being added to.
     */
    public void savePrescribedExercise() {

        // PERFORM VALIDATIONS
        // first, validate inputs common to both exercise types.
        // sets + intensity
        String minSetsText = minSetsField.getText();
        if (minSetsText.isEmpty()) {
            triggerErrorMessage(
                    "Please supply a minimum number of sets."
            );
            return;
        }
        int minSets = 0;
        try {
            minSets = Integer.parseInt(minSetsText);
        } catch (NumberFormatException e) {
            triggerErrorMessage(
                    "Please supply a positive integer for \"Minimum Sets\"."
            );
            return;
        }
        if (minSets <= 0) {
            triggerErrorMessage(
                    "\"Minimum Sets\" must be a positive integer."
            );
            return;
        }
        String maxSetsText = maxSetsField.getText();
        if (maxSetsText.isEmpty()) {
            triggerErrorMessage(
                    "Please supply a maximum number of sets."
            );
            return;
        }
        int maxSets = 0;
        try {
            maxSets = Integer.parseInt(maxSetsText);
        } catch (NumberFormatException e) {
            triggerErrorMessage("Please supply a positive integer for \"Maximum Sets\".");
            return;
        }
        if (maxSets <= 0) {
            triggerErrorMessage(
                    "\"Maximum Sets\" must be a positive integer."
            );
            return;
        }
        if (minSets > maxSets) {
            triggerErrorMessage(
                    "MinSets must be less than MaxSets."
            );
            return;
        }
        // sets fully validated.

        Intensity intensity = intensitySelections.getValue();
        if (intensity == null) {
            triggerErrorMessage(
                    "Please select an exercise intensity."
            );
            return;
        }
        // intensity validated.

        // now, we go into duration and rep-specific validations.
        // in each case, if we reach the end, a ProgramExercise will be created.

        String notes = exerciseNotesField.getText();

        if (exercise.getPrescriptionType() ==
        PrescriptionType.DURATION) {
            String durationText = durationField.getText();
            if (durationText.isEmpty()) {
                triggerErrorMessage(
                        "Please supply an exercise duration (sec)."
                );
                return;
            }
            int duration = 0;
            try {
                duration = Integer.parseInt(durationText);
            } catch (NumberFormatException e) {
                triggerErrorMessage(
                        "Please supply a positive integer for duration."
                );
                return;
            }
            if (duration <= 0) {
                triggerErrorMessage("Please supply a positive time duration.");
                return;
            }
            programExercise = new ProgramExercise(exercise, minSets, maxSets, null, null,
                    duration, intensity, notes);
        }
        else {
            String minRepsText = minRepsField.getText();
            if (minRepsText.isEmpty()) {
                triggerErrorMessage(
                        "Please supply a value for minimum reps."
                );
                return;
            }
            int minReps = 0;
            try {
                minReps = Integer.parseInt(minRepsText);
            } catch (NumberFormatException e) {
                triggerErrorMessage(
                        "Please supply a positive integer for \"Min Reps\"."
                );
                return;
            }
            if (minReps <= 0) {
                triggerErrorMessage(
                        "\"Minimum Reps\" must be a positive integer."
                );
                return;
            }
            String maxRepsText = maxRepsField.getText();
            if (maxRepsText.isEmpty()) {
                triggerErrorMessage(
                        "Please supply a value for maximum reps."
                );
                return;
            }
            int maxReps = 0;
            try {
                maxReps = Integer.parseInt(maxRepsText);
            } catch (NumberFormatException e) {
                triggerErrorMessage(
                        "Please supply a positive integer for \"Max Reps\"."
                );
                return;
            }
            if (maxReps <= 0) {
                triggerErrorMessage(
                        "\"Max Reps\" must be a positive integer."
                );
                return;
            }
            if (minReps > maxReps) {
                triggerErrorMessage(
                        "\"Min Reps\" cannot exceed the value of \"Max Reps\"."
                );
                return;
            }
            //all validated up to this point.
            programExercise = new ProgramExercise(exercise, minSets, maxSets, minReps,
                    maxReps, null, intensity, notes);
        }
        //finally, we have our newly created ProgramExercise for both cases.
        // value is set within if-else, so we can simply close the window.
        closeWindow();
    }

    // window close helper
    public void closeWindow() {
        Stage stage = (Stage) exerciseLabel.getScene().getWindow();
        stage.close();
    }

    public void cancelPrescribedExercise() {
        closeWindow();
    }

}
