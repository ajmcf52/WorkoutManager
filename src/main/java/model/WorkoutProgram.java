package model;

import java.util.ArrayList;

/*
represents an exercise program
that users may build and follow at the gym.
 */
public class WorkoutProgram {

    private String programName;
    private ArrayList<Exercise> exercises;

    /*
    empty constructor; intended for use with Jackson
     */
    public WorkoutProgram() {}

    public WorkoutProgram(String name, ArrayList<Exercise> exerciseList) {
        this.exercises = exerciseList;
        this.programName = name;
    }

    public String getProgramName() {
        return this.programName;
    }

    public ArrayList<Exercise> getExercises() {
        return exercises;
    }

    public void setProgramName(String name) {
        this.programName = name;
    }

    public void setExercises(ArrayList<Exercise> exerciseList) {
        this.exercises = exerciseList;
    }

}
