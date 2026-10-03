package model;

import enums.DurationUnit;
import enums.MovementType;
import enums.PrescriptionType;
import enums.SectionType;

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
        String stringValue = "";
        // adding warmup component first.
        for (int i = 0; i < this.workoutSections.size(); i++) {

            stringValue += this.workoutSections.get(i).getSectionType().toString() + "\n";
            WorkoutSection workoutSection = this.workoutSections.get(i);

            for (int j = 0; j < workoutSection.getExercises().size(); j++) {

                ProgramExercise programExercise = workoutSection.getExercises().get(j);
                stringValue += programExercise.toString() + "\n";
            }
            stringValue += "\n";
        }
        return stringValue;
    }

}
