package com.workoutmanager;

import enums.Difficulty;
import enums.Equipment;
import enums.MuscleGroup;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Exercise;
import model.ExerciseRepository;
import model.ProgramRepository;
import model.WorkoutProgram;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ExerciseLibraryController {
    @FXML
    private TableView<Exercise> exerciseTable;

    private FilteredList<Exercise> filteredExercises;

    private ObservableList<WorkoutProgram> allPrograms;

    @FXML
    private TableColumn<Exercise, String> nameColumn;

    @FXML
    private TableColumn<Exercise, Difficulty> difficultyColumn;

    @FXML
    private TableColumn<Exercise, Equipment> equipmentColumn;

    @FXML
    private VBox detailsPane;

    @FXML
    private Label nameLabel;

    @FXML
    private Label difficultyLabel;

    @FXML
    private Label equipmentLabel;

    @FXML
    private Label muscleGroupsLabel;

    @FXML
    private TextArea instructionsArea;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<Equipment> equipmentFilter;

    @FXML
    private ComboBox<Difficulty> difficultyFilter;

    @FXML
    private ComboBox<MuscleGroup> muscleGroupFilter;

    @FXML
    private Hyperlink demoLink;

    private Consumer<Exercise> onExerciseCreated;

    /*
    public setter. allows Main controller to inject all exercise data.
     */
    public void setExerciseList(ObservableList<Exercise> exercises) {
        filteredExercises = new FilteredList<>(exercises);
        exerciseTable.setItems(filteredExercises);
    }

    /*
    callback function--allows newly created exercises to be passed
    up the chain the MainController while avoiding tight coupling.
     */
    public void setOnExerciseCreated(Consumer<Exercise> callback) {
        this.onExerciseCreated = callback;
    }

    /*
    enum format helper function.
    used for clean display of exercise table data.
     */
    private String formatEnum(Enum<?> value) {
        String text = value.name()
                .toLowerCase()
                .replace('_', ' ');

        return Character.toUpperCase(text.charAt(0))
                + text.substring(1);
    }

    /*
    opens URL in a web browser when
    a Demo Link is pressed in the details panel.
     */
    private void openLink(String url) {
        try {
            java.awt.Desktop.getDesktop()
                    .browse(java.net.URI.create(url));
        } catch (IOException e) {
            System.err.println("IO Error during demo link opening.");
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.err.println("Exception opening demo link.");
            throw e;
        }
    }

    /*
    helper method.
    whenever an exercise is selected from the main table,
    this function updates the details panel accordingly.
     */
    private void showExerciseDetails(Exercise exercise) {

        detailsPane.setVisible(true);
        detailsPane.setManaged(true);

        nameLabel.setText(exercise.getName());

        difficultyLabel.setText(
                "Difficulty: " + exercise.getDifficulty()
        );

        equipmentLabel.setText(
                "Equipment: " + exercise.getEquipment()
        );

        muscleGroupsLabel.setText(
                "Muscle Groups: " +
                        exercise.getMuscleGroups()
                                .stream()
                                .map(this::formatEnum)
                                .collect(Collectors.joining(", "))
        );

        instructionsArea.setText(
                exercise.getInstructions()
        );

        demoLink.setText("Open Demo");
        demoLink.setOnAction(event -> {
            String url = exercise.getDemoLink();

            if (url != null && !url.isBlank()) {
                openLink(url);
            }
        });
    }

    /*
    secondary window creation that occurs when
    a user wishes to create a new exercise.
     */
    @FXML
    private void createNewExerciseWindow() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/workoutmanager/new-exercise-view.fxml"));
        Scene scene;
        try {
            scene = new Scene(loader.load());
        } catch (IOException e) {
            System.err.println("Secondary exercise window failed to load.");
            throw new RuntimeException(e);
        }
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.initModality(Modality.APPLICATION_MODAL);

        //wait for secondary window to do its work
        stage.showAndWait();

        NewExerciseController controller = loader.getController();
        Exercise newExercise = controller.getExercise();
        if (newExercise == null) {
            return;
        }

        //make the callback to MainController
        onExerciseCreated.accept(newExercise);
    }

    //hides details when nothing is selected.
    private void hideExerciseDetails() {
        detailsPane.setVisible(false);
    }

    /*
    filters search results based on all 3 parameters: search text,
    equipment filter and difficulty filter.
     */
    private void updatePredicate() {
        String searchText = searchField.getText().toLowerCase();
        Equipment equipmentValue = equipmentFilter.getValue();
        Difficulty difficultyValue = difficultyFilter.getValue();
        MuscleGroup muscleGroupValue = muscleGroupFilter.getValue();

        filteredExercises.setPredicate(exercise -> {
            boolean searchMatch, equipmentMatch, difficultyMatch, muscleGroupMatch;

            //search text predicate
            if (searchText.isBlank()) {
                searchMatch = true;
            }
            else {
                searchMatch = exercise.getName().toLowerCase().contains(searchText);
            }

            //equipment predicate
            if (equipmentValue == null || equipmentValue.toString().equals("All")) {
                equipmentMatch = true;
            }
            else {
                equipmentMatch = (equipmentValue == exercise.getEquipment());
            }

            //difficulty predicate
            if (difficultyValue == null || difficultyValue.toString().equals("All")) {
                difficultyMatch = true;
            }
            else {
                difficultyMatch = (difficultyValue == exercise.getDifficulty());
            }

            //muscle group predicate
            if (muscleGroupValue == null || muscleGroupValue.toString().equals("All")) {
                muscleGroupMatch = true;
            }
            else {
                muscleGroupMatch = exercise.getMuscleGroups().contains(muscleGroupValue);
            }

            return searchMatch && equipmentMatch && difficultyMatch && muscleGroupMatch;
        });

        exerciseTable.setItems(filteredExercises);
    }

    @FXML
    public void initialize() {

        // table cell formatting
        nameColumn.setCellValueFactory(
                new PropertyValueFactory<>("name")
        );

        difficultyColumn.setCellValueFactory(
                new PropertyValueFactory<>("difficulty")
        );

        equipmentColumn.setCellValueFactory(
                new PropertyValueFactory<>("equipment")
        );

        //nothing has been selected yet when app is booted.
        hideExerciseDetails();

        exerciseTable.setMinSize(450,350);
        exerciseTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        /*
        whenever an empty cell is selected, the details panel is cleared
        using this.
         */
        exerciseTable.setRowFactory(table -> {
            TableRow<Exercise> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (row.isEmpty()) {
                    exerciseTable.getSelectionModel().clearSelection();
                    hideExerciseDetails();
                }
            });
            return row;
        });

        exerciseTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (newValue == null) {
                        hideExerciseDetails();
                    } else {
                        showExerciseDetails(newValue);
                    }
                });

        //populating combo boxes; alignment; action listeners
        equipmentFilter.getItems().addAll(enums.Equipment.values());
        equipmentFilter.setPrefWidth(Region.USE_COMPUTED_SIZE);
        equipmentFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            updatePredicate();
        });

        difficultyFilter.getItems().addAll(enums.Difficulty.values());
        difficultyFilter.setPrefWidth(Region.USE_COMPUTED_SIZE);
        difficultyFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            updatePredicate();
        });

        muscleGroupFilter.getItems().addAll(MuscleGroup.values());
        muscleGroupFilter.setPrefWidth(Region.USE_COMPUTED_SIZE);
        muscleGroupFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            updatePredicate();
        });

        // adding search listener
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            updatePredicate();
        });

    }
}
