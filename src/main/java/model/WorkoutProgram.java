package model;

import java.util.ArrayList;
import java.util.List;

/*
represents an exercise program
that users may build and follow at the gym.
 */
public class WorkoutProgram {

    private String programName;
    private List<Exercise> exercises;

    /*
    empty constructor; intended for use with Jackson
     */
    public WorkoutProgram() {}

    public WorkoutProgram(String name, List<Exercise> exerciseList) {
        this.exercises = exerciseList;
        this.programName = name;
    }

    public String getProgramName() {
        return this.programName;
    }

    public List<Exercise> getExercises() {
        return exercises;
    }

    public void setProgramName(String name) {
        this.programName = name;
    }

    public void setExercises(List<Exercise> exerciseList) {
        this.exercises = exerciseList;
    }

    @Override
    public String toString() {
        return getProgramName() + ": " + getExercises().size() + " exercises.";
    }

}
