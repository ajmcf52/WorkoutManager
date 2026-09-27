package com.workoutmanager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import model.ExerciseJsonLoader;
import model.Exercise;

public class WorkoutManagerApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException, URISyntaxException {
        FXMLLoader fxmlLoader = new FXMLLoader(WorkoutManagerApplication.class.getResource("exercise-library-view.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Workout Manager");
        stage.setScene(scene);
        stage.show();

    }
}
