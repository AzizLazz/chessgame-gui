package com.example.chessgame.chessgamegui;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Game extends Application {
    private static final int BOARD_SIZE = 8; // 8x8 chessboard

    @Override
    public void start(Stage primaryStage) {
        // Create a GridPane for the chessboard
        GridPane board = new GridPane();

        // Create the chessboard squares
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Button square = new Button();
                square.setPrefSize(60, 60); // Set the preferred size for each square

                // Alternate colors between black and white squares
                if ((row + col) % 2 == 0) {
                    square.setStyle("-fx-background-color: grey;"); // White square
                } else {
                    square.setStyle("-fx-background-color: grey;"); // Grey square
                }

                // Set border properties
                square.setStyle(square.getStyle() + "-fx-border-color: black; -fx-border-width: 2px;");

                // Store row and column in final variables for the lambda
                final int finalRow = row; // Declare as final
                final int finalCol = col; // Declare as final

                // Add the square to the board at the specified grid position
                if (!((row == 0 && col == 0) || (row == 0 && col == 7) || (row == 7 && col == 0) || (row == 7 && col == 7))) {
                    board.add(square, col, row);
                }

                // Add an event handler for each square
                square.setOnAction(event -> {
                    System.out.println("Square clicked at: (" + finalRow + ", " + finalCol + ")");
                    // Add your action here
                });
            }
        }

        // Create buttons for additional functionality (if needed)
        Button btn1 = new Button("Top Left Button");
        Button btn2 = new Button("Right Button");

        // Add event handlers for additional buttons (optional)
        btn1.setOnAction(event -> {
            System.out.println("Top Left Button Clicked!");
        });
        btn2.setOnAction(event -> {
            System.out.println("Right Button Clicked!");
        });

        // Create an HBox for the additional buttons
        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(btn1, btn2);
        buttonBox.setAlignment(Pos.CENTER_LEFT);

        // Create a VBox for overall layout
        VBox root = new VBox(); // Create VBox as the main layout
        root.getChildren().addAll(board, buttonBox); // Add the chessboard and button box to the VBox

        // Create a scene with the layout
        Scene scene = new Scene(root, 480, 480); // Set appropriate size
        primaryStage.setTitle("JavaFX Chess Game");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
