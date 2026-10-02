package model;

import enums.Intensity;

/*
exercise within a program. different from a generic exercise.
contains several additional fields relevant
to a prescribed exercise (sets, reps, duration, etc.)
 */
public class ProgramExercise {
    private Exercise exercise;

    private Integer minSets;
    private Integer maxSets;

    private Integer minReps;
    private Integer maxReps;

    private Integer durationSeconds;

    private Intensity intensity;

    private String notes;

    public ProgramExercise() {}

    public ProgramExercise(Exercise exercise, Integer minSets, Integer maxSets,
                           Integer minReps, Integer maxReps, Integer durationSecs,
                           Intensity intensity, String notes) {
        this.exercise = exercise;
        this.minSets = minSets;
        this.maxSets = maxSets;
        this.minReps = minReps;
        this.maxReps = maxReps;
        this.durationSeconds = durationSecs;
        this.intensity = intensity;
        this.notes = notes;
    }

    // getters and setters

    public Exercise getExercise() {
        return exercise;
    }

    public Integer getMinSets() {
        return minSets;
    }

    public Integer getMaxSets() {
        return maxSets;
    }

    public Integer getMinReps() {
        return minReps;
    }

    public Integer getMaxReps() {
        return maxReps;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public Intensity getIntensity() {
        return intensity;
    }

    public String getNotes() {
        return notes;
    }

    public void setMinSets(Integer minSets) {
        this.minSets = minSets;
    }

    public void setMaxSets(Integer maxSets) {
        this.maxSets = maxSets;
    }

    public void setMinReps(Integer minReps) {
        this.minReps = minReps;
    }

    public void setMaxReps(Integer maxReps) {
        this.maxReps = maxReps;
    }

    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public void setIntensity(Intensity intensity) {
        this.intensity = intensity;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
