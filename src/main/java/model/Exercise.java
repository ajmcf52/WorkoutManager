package model;

import enums.Difficulty;
import enums.Equipment;
import enums.MuscleGroup;

import java.util.EnumSet;

/*
Defines an exercise in the program.
 */

public class Exercise {
    private String name;
    private String demoLink; //YouTube URL; intended as a clickable link.
    private String instructions; // how to do the exercise.
    private EnumSet<MuscleGroup> muscleGroups;
    private Difficulty difficulty;
    private Equipment equipment;

    /*
    Jackson-friendly constructor.
    In place so JSON parser can properly make Exercise objects.
     */
    public Exercise() {}

    // go-to constructor
    public Exercise(String name,
                    String demoLink,
                    String instructions,
                    EnumSet<MuscleGroup> muscleGroups,
                    Difficulty difficulty,
                    Equipment equipment){
        this.name = name;
        this.demoLink = demoLink;
        this.instructions = instructions;
        this.muscleGroups = muscleGroups;
        this.difficulty = difficulty;
        this.equipment = equipment;
    }

    /*
    prints the exercise name and details in a semi-readable format.
     */
    public String toString() {
        return String.format("%s\nLink: %s\nHow to: %s\nMuscle Groups: %s\nDifficulty: %s",
                this.name,
                this.demoLink,
                this.instructions,
                this.muscleGroups,
                this.difficulty
                );
    }

    // getters

    public String getName() { return this.name; }
    public String getInstructions() { return this.instructions; }
    public String getDemoLink() { return this.demoLink; }
    public EnumSet<MuscleGroup> getMuscleGroups() { return this.muscleGroups; }
    public Difficulty getDifficulty() { return this.difficulty; }
    public Equipment getEquipment() { return this.equipment; }

    // setters

    public void setName(String name) { this.name = name; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public void setDemoLink(String demoLink) { this.demoLink = demoLink; }
    public void setMuscleGroups(EnumSet<MuscleGroup> muscleGroups) { this.muscleGroups = muscleGroups; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public void setEquipment(Equipment equipment) { this.equipment = equipment; }
}
