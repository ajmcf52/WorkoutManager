package com.workoutmanager;

import enums.SectionType;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Exercise;
import model.ProgramExercise;
import model.WorkoutProgram;
import model.WorkoutSection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/*
ProgramBuilder works by splitting the WorkoutProgram into 3 SectionTypes.
Warmup, MainWorkout, and Cooldown.
Similarly, I will break the variable declarations into blocks.
 */
public class ProgramBuilderController {

    // Common class variable declarations
    @FXML
    private TextField programNameField;

    @FXML
    private Label errorMessageIndicator;

    private ObservableList<Exercise> availableExercises;

    private WorkoutProgram workoutProgram;

    Predicate<String> programNameAvailable;

    // *** Main Workout variables ***
    @FXML
    private ListView<Exercise> mainAvailableExercisesView;

    private FilteredList<Exercise> mainSearchedAvailable;

    @FXML
    private ListView<ProgramExercise> mainProgramExercisesView;

    @FXML
    private TextField mainSearchField;

    private ObservableList<ProgramExercise> mainProgramExercises;


    // *** Warmup variable declarations ***
    @FXML
    private ListView<Exercise> warmupAvailableExercisesView;

    private FilteredList<Exercise> warmupSearchedAvailable;

    @FXML
    private TextField warmupSearchField;

    @FXML
    private ListView<ProgramExercise> warmupProgramExercisesView;

    private ObservableList<ProgramExercise> warmupProgramExercises;


    // *** Cooldown variable declarations ***
    @FXML
    private TextField cooldownSearchField;

    @FXML
    private ListView<Exercise> cooldownAvailableExercisesView;

    @FXML
    private ListView<ProgramExercise> cooldownProgramExercisesView;

    private FilteredList<Exercise> cooldownSearchedAvailable;

    private ObservableList<ProgramExercise> cooldownProgramExercises;

    /*
    callback for ProgramLibraryController to validate
    program name availability.
     */
    public void setProgramNameAvailable(Predicate<String> callback) {
        this.programNameAvailable = callback;
    }

    /*
    allows parent controller to get access to WorkoutProgram after it has been built.
     */
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

        // setting selection possibilities for the three program section ListViews
        mainSearchedAvailable = new FilteredList<>(availableExercises);
        mainAvailableExercisesView.setItems(mainSearchedAvailable);

        warmupSearchedAvailable = new FilteredList<>(availableExercises);
        warmupAvailableExercisesView.setItems(warmupSearchedAvailable);

        cooldownSearchedAvailable = new FilteredList<>(availableExercises);
        cooldownAvailableExercisesView.setItems(cooldownSearchedAvailable);

    }

    @FXML
    public void initialize() {

        mainProgramExercises = FXCollections.observableArrayList();
        mainProgramExercisesView.setItems(mainProgramExercises);

        warmupProgramExercises = FXCollections.observableArrayList();
        warmupProgramExercisesView.setItems(warmupProgramExercises);

        cooldownProgramExercises = FXCollections.observableArrayList();
        cooldownProgramExercisesView.setItems(cooldownProgramExercises);

        /*
        allows ListView to solely display the Exercise name
        instead of a full block of text displaying all
        exercise info.
         */
        mainAvailableExercisesView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Exercise exercise, boolean isEmpty) {
                super.updateItem(exercise, isEmpty);

                if (isEmpty || exercise == null) {
                    setText(null);
                }
                else {
                    setText(exercise.getName());
                }
            }
        });

        warmupAvailableExercisesView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Exercise exercise, boolean isEmpty) {
                super.updateItem(exercise, isEmpty);

                if (isEmpty || exercise == null) {
                    setText(null);
                }
                else {
                    setText(exercise.getName());
                }
            }
        });

        cooldownAvailableExercisesView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Exercise exercise, boolean isEmpty) {
                super.updateItem(exercise, isEmpty);

                if (isEmpty || exercise == null) {
                    setText(null);
                }
                else {
                    setText(exercise.getName());
                }
            }
        });

        // clears possible "Program empty" error message
        // when user takes further action to modify program name/exercises.
        mainAvailableExercisesView.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<Exercise>) change -> {
            clearErrorMessage();
        });
        programNameField.textProperty().addListener((observable -> {
            clearErrorMessage();
        }));
        warmupAvailableExercisesView.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<Exercise>) change -> {
                    clearErrorMessage();
                });
        cooldownAvailableExercisesView.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<Exercise>) change -> {
                    clearErrorMessage();
                });
        mainSearchField.textProperty().addListener(observable -> {
            clearErrorMessage();
        });
        warmupSearchField.textProperty().addListener(observable -> {
            clearErrorMessage();
        });
        cooldownSearchField.textProperty().addListener(observable -> {
            clearErrorMessage();
        });
        
        // search predicate listener
        mainSearchField.textProperty().addListener(observable -> {
            updatePredicate(mainSearchedAvailable,
                    mainSearchField, SectionType.MAIN_WORKOUT);
        });
        warmupSearchField.textProperty().addListener(observable -> {
            updatePredicate(warmupSearchedAvailable,
                    warmupSearchField, SectionType.WARM_UP);
        });
        cooldownSearchField.textProperty().addListener(observable -> {
            updatePredicate(cooldownSearchedAvailable,
                    cooldownSearchField, SectionType.COOL_DOWN);
        });

    }

    @FXML
    private void addWarmupExercise() {
        addExerciseToProgram(SectionType.WARM_UP);
    }
    
    @FXML
    private void addMainExercise() {
        addExerciseToProgram(SectionType.MAIN_WORKOUT);
    }
    
    @FXML
    private void addCooldownExercise() {
        addExerciseToProgram(SectionType.COOL_DOWN);
    }
    
    //NOTE one small thing to consider.
    // if we add then remove a program,
    // should it go back in the same spot
    // of the original list, and not at the bottom of the list?

    /*
    helper method; adds a selected exercise to the program section currently being built.
     */
    @FXML
    private void addExerciseToProgram(SectionType sectionType) {

        Exercise exerciseToAdd = getExerciseToAdd(sectionType);

        // check for no exercise selected.
        if (exerciseToAdd == null) {
            return;
        }
        /*
        create the Program Exercise modal to
        retrieve user-inputted metadata
        (sets, reps, duration, intensity, etc.)
         */
        FXMLLoader loader = new FXMLLoader(getClass().
                getResource("/com/workoutmanager/exercise-prescription-view.fxml"));
        Scene scene;
        try {
            scene = new Scene(loader.load());
        } catch (IOException e) {
            System.err.println("Exercise prescription window failed to load.");
            throw new RuntimeException(e);
        }
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);

        ProgramExerciseController controller = loader.getController();
        controller.setExercise(exerciseToAdd);

        stage.showAndWait();

        ProgramExercise programExercise = controller.getProgramExercise();

        addExerciseToSection(sectionType, programExercise);
        availableExercises.remove(exerciseToAdd);
    }

    /*
    helper function. gets the selected exercise to add
    according to the passed section type.
     */
    private Exercise getExerciseToAdd(SectionType sectionType) {
        Exercise exerciseToAdd = null;

        if (sectionType == SectionType.COOL_DOWN) {
            exerciseToAdd = cooldownAvailableExercisesView.getSelectionModel()
                    .getSelectedItem();
        }
        else if (sectionType == SectionType.MAIN_WORKOUT) {
            exerciseToAdd = mainAvailableExercisesView.getSelectionModel()
                    .getSelectedItem();
        }
        else {
            exerciseToAdd = warmupAvailableExercisesView.getSelectionModel()
                    .getSelectedItem();
        }
        return exerciseToAdd;
    }

    /*
    adds the given exercise to the correct section,
    according to SectionType.

    no need to check for null value, as this is already done.
     */
    private void addExerciseToSection(SectionType sectionType, ProgramExercise programExercise) {
        if (sectionType == SectionType.COOL_DOWN) {
            cooldownProgramExercises.add(programExercise);
        }
        else if (sectionType == SectionType.WARM_UP) {
            warmupProgramExercises.add(programExercise);
        }
        else {
            mainProgramExercises.add(programExercise);
        }
    }

    @FXML
    private void removeExerciseFromWarmup() {
        removeExercise(SectionType.WARM_UP);
    }
    @FXML
    private void removeExerciseFromCooldown() {
        removeExercise(SectionType.COOL_DOWN);
    }
    @FXML
    private void removeExerciseFromMain() {
        removeExercise(SectionType.MAIN_WORKOUT);
    }

    /*
    determines which exercise we are removing, based on
    SectionType.
     */
    private ProgramExercise getExerciseToRemove(SectionType sectionType) {
        ProgramExercise exerciseToRemove;
        if (sectionType == SectionType.COOL_DOWN) {
            exerciseToRemove = cooldownProgramExercisesView.getSelectionModel()
                    .getSelectedItem();
        }
        else if (sectionType == SectionType.MAIN_WORKOUT) {
            exerciseToRemove = mainProgramExercisesView.getSelectionModel()
                    .getSelectedItem();
        }
        else {
            exerciseToRemove = warmupProgramExercisesView.getSelectionModel()
                    .getSelectedItem();
        }
        return exerciseToRemove;
    }

    /*
    removes the exercise from a given program section,
    adding it back to available exercises.
     */
    private void removeExerciseFromSection(SectionType sectionType, ProgramExercise programExercise) {
        if (sectionType == SectionType.COOL_DOWN) {
            cooldownProgramExercises.remove(programExercise);
        }
        else if (sectionType == SectionType.WARM_UP) {
            warmupProgramExercises.remove(programExercise);
        }
        else {
            mainProgramExercises.remove(programExercise);
        }
        availableExercises.add(programExercise.getExercise());
    }

    /*
    removes a selected exercise from the program section currently being built;
    adds it back to "available exercises".
     */
    @FXML
    private void removeExercise(SectionType sectionType) {

        ProgramExercise exerciseToRemove = getExerciseToRemove(sectionType);

        // if no program exercise is selected
        // to remove, simply return.
        if (exerciseToRemove == null) {
            return;
        }
        removeExerciseFromSection(sectionType, exerciseToRemove);
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
        // check program name availability with ProgramLibrary callback
        if (!programNameAvailable.test(programName)) {
            triggerErrorMessage("Program name already exists!");
            return;
        }

        /*
        validate the main and warmup sections to ensure non-empty
        (cooldown is optional)
         */

        List<ProgramExercise> warmupExerciseList = warmupProgramExercisesView.getItems();
        if (warmupExerciseList.isEmpty()) {
            triggerErrorMessage("Warmup must contain at least one exercise.");
            return;
        }
        List<ProgramExercise> mainExerciseList = mainProgramExercisesView.getItems();
        if (mainExerciseList.isEmpty()) {
            triggerErrorMessage("Main workout must contain at least one exercise.");
            return;
        }
        List<WorkoutSection> workoutSections = getWorkoutSections(warmupExerciseList, mainExerciseList);
        workoutProgram = new WorkoutProgram(programName,workoutSections);
        closeWindow();
    }

    /*
    builds a list of workout sections given the lists of program exercises.
     */
    private List<WorkoutSection> getWorkoutSections(List<ProgramExercise> warmupExerciseList, List<ProgramExercise> mainExerciseList) {
        List<ProgramExercise> cooldownExerciseList = cooldownProgramExercisesView.getItems();

        //building workout sections
        WorkoutSection warmupSection = new WorkoutSection(SectionType.WARM_UP, warmupExerciseList);
        WorkoutSection mainSection = new WorkoutSection(SectionType.MAIN_WORKOUT, mainExerciseList);
        WorkoutSection cooldownSection = new WorkoutSection(SectionType.COOL_DOWN, cooldownExerciseList);
        List<WorkoutSection> workoutSections = new ArrayList<WorkoutSection>();
        workoutSections.add(warmupSection);
        workoutSections.add(mainSection);
        workoutSections.add(cooldownSection);
        return workoutSections;
    }

    /*
    updates the search predicate on
    list of available exercises for specific program section views.
     */
    private void updatePredicate(FilteredList<Exercise> filteredList,
                                 TextField searchField,
                                 SectionType sectionType) {
        String searchText = searchField.getText().trim().toLowerCase();

        mainSearchedAvailable.setPredicate(exercise -> {
            return exercise.getSuitableSections().contains(sectionType)
                    && exercise.getName().toLowerCase().contains(searchText)
                    && !filteredList.contains(exercise);
        });
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