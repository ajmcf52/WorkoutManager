package model;

import enums.DurationUnit;
import enums.Intensity;
import enums.MovementType;
import enums.PrescriptionType;

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

    private Integer duration;

    private DurationUnit durationUnit;

    private Intensity intensity;

    private String notes;

    public ProgramExercise() {}

    public ProgramExercise(Exercise exercise, Integer minSets, Integer maxSets,
                           Integer minReps, Integer maxReps, Integer duration,
                           DurationUnit durationUnit, Intensity intensity, String notes) {
        this.exercise = exercise;
        this.minSets = minSets;
        this.maxSets = maxSets;
        this.minReps = minReps;
        this.maxReps = maxReps;
        this.duration = duration;
        this.durationUnit = durationUnit;
        this.intensity = intensity;
        this.notes = notes;
    }

    @Override
    public String toString() {
        String stringValue = exercise.getName() + " -- ";

        if (exercise.getPrescriptionType() == PrescriptionType.DURATION) {

            if (exercise.getMovementType() != MovementType.CARDIO) {
                stringValue += setsToString() + " x ";
            }
            stringValue += durationToString() + " -- ";
        }
        // handles everything rep-based.
        else {
            stringValue += setsToString() + " x " + repsToString() + " -- ";
        }
        stringValue += intensity.toString();
        return stringValue;
    }

    /*
   set format helper.
    */
    private String setsToString() {
        String stringValue = "";
        if (minSets == null || maxSets == null) {
            return stringValue;
        }
        stringValue += String.valueOf(minSets);
        if (minSets < maxSets) {
            stringValue += "-" + String.valueOf(maxSets);
        }
        return stringValue;
    }

    /*
    rep format helper.
     */
    private String repsToString() {
        String stringValue = "";
        if (minReps == null || maxReps == null) {
            return stringValue;
        }

        stringValue += String.valueOf(minReps);
        if (minReps < maxReps) {
            stringValue += "-" + String.valueOf(maxReps);
        }
        return stringValue;
    }

    /*
    duration format helper.
     */
    private String durationToString() {
        String stringValue = "";

        if (durationUnit == DurationUnit.MIN) {
            stringValue += String.valueOf(duration) + " min";
        }
        else {
            stringValue += String.valueOf(duration) + " sec";
        }
        return stringValue;
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

    public Integer getDuration() {
        return duration;
    }

    public DurationUnit getDurationUnit() { return durationUnit; }

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

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public void setDurationUnit(DurationUnit durationUnit) {
        this.durationUnit = durationUnit;
    }

    public void setIntensity(Intensity intensity) {
        this.intensity = intensity;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
