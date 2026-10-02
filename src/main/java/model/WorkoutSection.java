package model;

import enums.SectionType;

import java.util.List;

/*
section of a workout.
will typically be one of three things:
- warm up
- cool down
- main workout
 */
public class WorkoutSection {
    private SectionType sectionType;
    private List<ProgramExercise> exercises;

    public WorkoutSection() {}

    public WorkoutSection(SectionType sectionType, List<ProgramExercise> exercises) {
        this.sectionType = sectionType;
        this.exercises = exercises;
    }

    public SectionType getSectionType() {
        return sectionType;
    }

    public List<ProgramExercise> getExercises() {
        return exercises;
    }

    public void setSectionType(SectionType sectionType) {
        this.sectionType = sectionType;
    }

    public void setExercises(List<ProgramExercise> exercises) {
        this.exercises = exercises;
    }

}
