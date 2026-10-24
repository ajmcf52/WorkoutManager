package model;

import enums.Difficulty;
import enums.Equipment;
import enums.MuscleGroup;

/*
Filter class to package DB filter inputs
for the exercise library search function.
 */
public class ExerciseFilter {

    private String searchText;
    private Difficulty difficulty;
    private Equipment equipment;
    private MuscleGroup muscleGroup;

    public ExerciseFilter() {}

    public ExerciseFilter(String searchText, Difficulty difficulty,
                          Equipment equipment, MuscleGroup muscleGroup) {
        this.searchText = searchText;
        this.difficulty = difficulty;
        this.equipment = equipment;
        this.muscleGroup = muscleGroup;
    }

    public String getSearchText() {
        return searchText;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public MuscleGroup getMuscleGroup() {
        return muscleGroup;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public void setMuscleGroup(MuscleGroup muscleGroup) {
        this.muscleGroup = muscleGroup;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }
}
