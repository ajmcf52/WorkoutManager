package model;

import java.util.ArrayList;
import java.util.List;

/*
represents an exercise program
that users may build and follow at the gym.
 */
public class WorkoutProgram {

    private String programName;
    private List<WorkoutSection> workoutSections;

    /*
    empty constructor; intended for use with Jackson
     */
    public WorkoutProgram() {}

    public WorkoutProgram(String name, List<WorkoutSection> sections) {
        this.workoutSections = sections;
        this.programName = name;
    }

    public String getProgramName() {
        return this.programName;
    }

    public List<WorkoutSection> getWorkoutSections() {
        return this.workoutSections;
    }

    public void setProgramName(String name) {
        this.programName = name;
    }

    public void setWorkoutSections(List<WorkoutSection> sections) {
        this.workoutSections = sections;
    }

    @Override
    public String toString() {
        return getProgramName() + ": " + getWorkoutSections().size() + " sections.";
    }

}
