package com.workoutmanager;

import enums.Difficulty;
import enums.Equipment;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.Exercise;
import model.ExerciseJsonLoader;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class ExerciseLibraryController {
    @FXML
    private TableView<Exercise> exerciseTable;

    private ObservableList<Exercise> allExercises;

    private FilteredList<Exercise> filteredExercises;

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
    private Hyperlink demoLink;

    /*
    enum format helper function.
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
        } catch (Exception e) {
            e.printStackTrace();
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

        filteredExercises.setPredicate(exercise -> {
            boolean searchMatch, equipmentMatch, difficultyMatch;

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
            return searchMatch && equipmentMatch && difficultyMatch;
        });

        exerciseTable.setItems(filteredExercises);
    }

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(
                new PropertyValueFactory<>("name")
        );

        difficultyColumn.setCellValueFactory(
                new PropertyValueFactory<>("difficulty")
        );

        equipmentColumn.setCellValueFactory(
                new PropertyValueFactory<>("equipment")
        );


        // working out the path to our JSON file

        URL resource = getClass().getResource("/com/workoutmanager/exercises.json");
        if (resource == null) {
            System.out.println("Could not find exercises.json");
            return;
        }

        File file = null;
        try {
            file = new File(resource.toURI());
        } catch (URISyntaxException e) {
            System.err.println("Exercise file creation failed.");
            e.printStackTrace();
            return;
        }

        // loading exercises, rendering observable
        ExerciseJsonLoader loader = new ExerciseJsonLoader();
        List<Exercise> exercises = loader.loadExercises(file.getPath());

        allExercises = FXCollections.observableArrayList(exercises);
        filteredExercises = new FilteredList<Exercise>(allExercises);

        //nothing has been selected yet when app is booted.
        hideExerciseDetails();

        exerciseTable.setItems(filteredExercises);
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

        // adding search listener
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            updatePredicate();
        });

    }
}
