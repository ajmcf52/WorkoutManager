# Workout Manager

## Overview

Workout Manager is a Java desktop application for building + managing
exercise-based workout and mobility programs. 

Currently, the application provides
a searchable exercise library with filtering, detailed exercise information,
exercise creation, input validation, and persistent JSON storage. The end-goal is
to expand the application into a full program-building tool capable of organizing
exercises into workouts, featuring supersets as well as circuit-based programming.

Notably, this project is being developed as a portfolio project to demonstrate
practical Java development, object-oriented design, GUI programming, data persistence,
filtering, validation, and eventually more advanced topics such as database integration,
algorithms, and concurrency.

## Current Features

- Searchable exercise library
- Filtering by:
  - Equipment
  - Difficulty
  - Muscle group
- Combined multi-filter support
- Exercise detail panel
- External exercise demonstration links
- New exercise creation via modal form
  - Multi-select muscle group assignment
  - Input validation
  - URI validation for demo links
- Persistent exercise storage with JSON
- Automatic UI updates through JavaFX observable collections

## Tech Stack

Java (OpenJDK 27)

JavaFX

FXML

Maven

Jackson + JSON

Git + GitHub

## Architecture

Workout Manager follows a simple separation-of-concerns (SoC) approach.
FXML defines the interface, JavaFX controllers manage user interaction
and UI state, domain classes represent application data, and Jackson 
handles serialization and persistence.

Key architectural components include:

- `Exercise` as the primary domain model
- Enum classes for structured values such as equipment, difficulty, and muscle groups
- JavaFX controllers for exercise-library and exercise-creation workflows
- FXML files for UI layout
- A master `ObservableList<Exercise>` containing the application's exercise data
- A `FilteredList<Exercise>` used as a non-destructive view of the master exercise collection
- JSON loading and writing via Jackson

## Technical Highlights

- JavaFX `TableView` for displaying structured exercise data
- Live text-based searching
- Predicate-based filtering across multiple criteria
- `ObservableList`/`FilteredList` data flow
- Lambda expressions for UI listeners and filtering logic
- Enum-based domain modeling
- Modal secondary-window workflow using JavaFX `Stage`
- Controller-to-controller data handoff
- Jackson JSON serialization and deserialization
- Persistent exercise creation across application restarts
- Input validation and user-facing error handling
- URI validation for external exercise demo links
- Java module configuration for JavaFX and Jackson

## Notable Challenges

### Non-destructive filtering

An early filtering implementation recreated filtered collections during each
update, which caused the dataset to progressively shrink. The final design
maintains a master `ObservableList<Exercise>` and exposes a persistent
`FilteredList<Exercise>` to the `TableView`. Search terms and filters only
update the predicate, allowing filters to be freely applied and removed
without modifying the underlying dataset.

### Java module configuration

Because the project uses the Java module system, JavaFX and Jackson required
explicit module access. Jackson also required reflective access to model and
enum packages in order to deserialize exercise data correctly.

### Resource loading

FXML and JSON resources needed to be loaded through the application classpath
rather than relying on machine-specific absolute file paths.

### Secondary-window communication

The New Exercise form is opened as a modal JavaFX stage. The secondary controller
validates user input and returns a completed `Exercise` object to the Exercise
Library controller after the window closes.

### Persistent data storage

The application originally only loaded exercise data from JSON. Persistence
was later introduced, so newly created exercises can be serialized back to
the JSON file and remain available after the application restarts.

## Design Decisions

### JSON before SQLite

JSON was selected as the initial persistence layer because it keeps storage
simple while the application's domain model and feature set are still evolving.
A migration to SQLite is planned once program creation and additional CRUD
functionality are more mature.

### Filtering as a view

Filtering does not modify the application's underlying exercise collection.
Instead, a `FilteredList` acts as a dynamic view over the master dataset.

This keeps filtering reversible and simplifies the process of combining search
text, equipment, difficulty, and muscle group criteria.

### Separate exercise-creation window

Exercise creation is handled in a dedicated modal form rather than directly
inside the Exercise Library view. This keeps the library focused on browsing
and selection while isolating creation and validation logic.

## Roadmap

### Next

- Program Builder
- Workout sections
- Supersets and circuits
- Program saving/loading

### Later

- SQLite persistence
- Edit/delete exercises
- Program analysis
- Background processing & concurrency
- Unit testing
- Exercise progression/regression relationships
- Application packaging