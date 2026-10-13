PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS WorkoutProgram (
    workout_id INTEGER PRIMARY KEY AUTOINCREMENT,
    program_name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS SectionType (
    section_type_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS Exercise (
    exercise_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    demo_url TEXT,
    instructions TEXT,
    difficulty TEXT NOT NULL
        CHECK (difficulty IN ('BEGINNER',
                             'NOVICE',
                             'INTERMEDIATE',
                             'ADVANCED',
                             'EXPERT')),
    movement_type TEXT NOT NULL
        CHECK (movement_type IN ('STRENGTH',
                                'CARDIO',
                                'DYNAMIC_FLOW',
                                'STATIC_STRETCH',
                                'MYOFASCIAL_RELEASE')),
    prescription_type TEXT NOT NULL
        CHECK (prescription_type IN ('DURATION',
                                    'REPS'))
);

CREATE TABLE IF NOT EXISTS MuscleGroup (
    muscle_group_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS Equipment (
    equipment_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS WorkoutSection (
    workout_section_id INTEGER PRIMARY KEY AUTOINCREMENT,
    workout_id INTEGER NOT NULL,
    section_type_id INTEGER NOT NULL,

    UNIQUE (workout_id, section_type_id),

    FOREIGN KEY (workout_id)
        REFERENCES WorkoutProgram(workout_id)
        ON DELETE CASCADE,

    FOREIGN KEY (section_type_id)
        REFERENCES SectionType(section_type_id)
        ON DELETE RESTRICT

);

CREATE TABLE IF NOT EXISTS SuitableSections (
    exercise_id INTEGER NOT NULL,
    section_type_id INTEGER NOT NULL,

    PRIMARY KEY (exercise_id, section_type_id),

    FOREIGN KEY (exercise_id) REFERENCES Exercise(exercise_id)
                              ON DELETE CASCADE,
    FOREIGN KEY (section_type_id) REFERENCES SectionType(section_type_id)
                              ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS ExerciseMuscleGroup (
    exercise_id INTEGER NOT NULL,
    muscle_group_id INTEGER NOT NULL,

    PRIMARY KEY (exercise_id, muscle_group_id),

    FOREIGN KEY (exercise_id) REFERENCES Exercise(exercise_id)
                                 ON DELETE CASCADE,
    FOREIGN KEY (muscle_group_id) REFERENCES MuscleGroup(muscle_group_id)
                                 ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS ExerciseEquipment (
    exercise_id INTEGER NOT NULL,
    equipment_id INTEGER NOT NULL,

    PRIMARY KEY (exercise_id, equipment_id),

    FOREIGN KEY (exercise_id) REFERENCES Exercise(exercise_id)
                               ON DELETE CASCADE,
    FOREIGN KEY (equipment_id) REFERENCES Equipment(equipment_id)
                               ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS ProgramExercise (
    program_exercise_id INTEGER PRIMARY KEY AUTOINCREMENT,
    workout_section_id INTEGER NOT NULL,
    exercise_id INTEGER NOT NULL,
    UNIQUE(workout_section_id, exercise_id),
    min_sets INTEGER CHECK (min_sets IS NULL OR min_sets > 0),
    max_sets INTEGER CHECK (max_sets IS NULL OR max_sets > 0),
    min_reps INTEGER CHECK (min_reps IS NULL OR min_reps > 0),
    max_reps INTEGER CHECK (max_reps IS NULL OR max_reps > 0),
    duration_value INTEGER CHECK (duration_value IS NULL OR duration_value > 0),
    duration_unit TEXT CHECK (duration_unit IN ('SEC', 'MIN')),
    intensity TEXT NOT NULL CHECK (intensity IN ('LIGHT',
                                                'MODERATE',
                                                'MODERATE_HEAVY',
                                                'HEAVY')),
    notes TEXT,

    FOREIGN KEY (workout_section_id) REFERENCES WorkoutSection(workout_section_id)
                             ON DELETE CASCADE,
    FOREIGN KEY (exercise_id) REFERENCES Exercise(exercise_id)
                             ON DELETE NO ACTION,
    CHECK ( (duration_value IS NOT NULL AND duration_unit IS NOT NULL)
                OR (duration_value IS NULL AND duration_unit IS NULL)),
    CHECK ( (min_sets IS NULL AND max_sets IS NULL)
                OR (min_sets IS NOT NULL AND max_sets IS NOT NULL AND min_sets <= max_sets) ),
    CHECK ( (min_reps IS NULL AND max_reps IS NULL)
                OR (min_reps IS NOT NULL AND max_reps IS NOT NULL AND min_reps <= max_reps))
);