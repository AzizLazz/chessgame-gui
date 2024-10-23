package com.example.chessgame.chessgamegui;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Game extends Application {

    public static long redSingles;   // Bitboard for red single pieces
    public static long blueSingles;  // Bitboard for blue single pieces
    public static long redDoubles;   // Bitboard for red double pieces (top knights)
    public static long blueDoubles;  // Bitboard for blue double pieces (top knights)

    public static long red_on_blue;   // Bitboard for red double pieces (top knights)
    public static long blue_on_red;

    public static final long NOT_A_FILE = 0xfefefefefefefefeL; // 11111110...
    public static final long NOT_H_FILE = 0x7f7f7f7f7f7f7f7fL; // 01111111...
    public static final long NOT_AB_FILE = 0xFCFCFCFCFCFCFCFCL; // Not columns A and B
    public static final long NOT_GH_FILE = 0x3F3F3F3F3F3F3F3FL; // Not columns G and H

    private static final int RED_DIRECTION = 8;   // Red moves down
    private static final int BLUE_DIRECTION = -8;   // Blue moves up
    public static final long CORNER_MASK = ~(1L | (1L << 7) | (1L << 56) | (1L << 63));

    private static final int BOARD_SIZE = 8; // 8x8 chessboard
    private final Button[][] squares = new Button[BOARD_SIZE][BOARD_SIZE]; // Store all squares in a 2D array
    private int lastSelectedRow = -1; // Track the last selected square's row
    private int lastSelectedCol = -1; // Track the last selected square's column
    private final List<Button> highlightedSquares = new ArrayList<>(); // Store newly highlighted squares
    private final Map<Button, Circle> originalGraphics = new HashMap<>(); // Store the original graphics (pieces)

    private final GridPane board = new GridPane();
    int[][] piecePositions = new int[BOARD_SIZE][BOARD_SIZE];
    boolean  isRed = true;
    boolean  turn = true;
    public static final long topRowMask = 0xFFL;  // Mask for the top row (bits 0-7)
    public static final long bottomRowMask = 0xFFL << 56;  // Mask for the bottom row (bits 56-63)
    private Label statusLabel;


    @Override
    public void start(Stage primaryStage) {


        piecePositions = readFEN("b0b0b0b0b0b0/1b0b0b0b0b0b01/8/8/8/8/1r0r0r0r0r0r01/r0r0r0r0r0r0");
        //6/8/8/8/8/1r06/2r0r0r0r0r01/r0r0r0r0r0r0
        //2bb3/5b02/1bb1bb2b0b0/2br3r01/2b0r04/5r0rr1/2rr2r02/3r02
        //b0b0b0b0b0b0/1b0b0b0b0b0b01/8/8/8/8/1r0r0r0r0r0r01/r0r0r0r0r0r0

        placePiecesOnBoard();

        statusLabel = new Label("Game in progress...");
        statusLabel.setFont(new Font("Arial", 43));
        statusLabel.setTextFill(Color.BLACK);
        statusLabel.setAlignment(Pos.CENTER_RIGHT);

        Button btn1 = new Button("New Game");
        Button btn2 = new Button("Get Best Move from AI");
        Button btn3 = new Button("complete Bot Game");

        btn1.setOnAction(event -> {piecePositions = readFEN("b0b0b0b0b0b0/1b0b0b0b0b0b01/8/8/8/8/1r0r0r0r0r0r01/r0r0r0r0r0r0");
        board.getChildren().clear();
        placePiecesOnBoard();}
        );
        btn2.setOnAction(event -> {

            String fen = toFEN();
            String bestMove = AIBotClient.getBestMoveFromServer(fen); // Call the AI server

            System.out.println("Best move received from AI: " + bestMove);

             Alert alert = new Alert(Alert.AlertType.INFORMATION, "Best move: " + bestMove);
             alert.showAndWait();
        });

        //missing implementation
        btn3.setOnAction(event -> System.out.println("TODO"));

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(btn1, btn2, btn3);
        buttonBox.setAlignment(Pos.CENTER);
        Label msg = new Label("  A      B      C      D      E      F      G      H");
        msg.setFont(new Font("Arial", 43));
        msg.setTextFill(Color.BLACK);

        VBox root = new VBox();
        root.getChildren().addAll(msg,board, buttonBox,statusLabel);

        Scene scene = new Scene(root, 800, 900);
        primaryStage.setTitle("Aziz GUI");
        primaryStage.setScene(scene);
        primaryStage.show();
    }



    private static void doMoveAndReturnModifiedBitBoards(byte from, byte to, boolean isRedTurn) {
        if (isRedTurn) {
            if ((redDoubles != 0 && (redDoubles & (1L << from)) != 0) || (red_on_blue != 0 && (red_on_blue & (1L << from)) != 0)) {
                moveDoublePieceOnBitBoards(from, to, true);
            } else {
                moveSinglePieceOnBitBoards(from, to, true);
            }
        } else {
            if ((blueDoubles != 0 && (blueDoubles & (1L << from)) != 0) || (blue_on_red != 0 && (blue_on_red & (1L << from)) != 0)) {
                moveDoublePieceOnBitBoards(from, to, false);
            } else {
                moveSinglePieceOnBitBoards(from, to, false);
            }
        }
    }



    private static void moveSinglePieceOnBitBoards(byte fromIndex, byte toIndex, boolean isRed) {
        // Determine which bitboards to use based on the player's color
        long ownSingles = isRed ? redSingles : blueSingles;
        long enemySingles = isRed ? blueSingles : redSingles;
        long ownDoubles = isRed ? redDoubles : blueDoubles;
        long enemyDoubles = isRed ? blueDoubles : redDoubles;
        long ownOnEnemy = isRed ? red_on_blue : blue_on_red;
        long enemyOnOwn = isRed ? blue_on_red : red_on_blue;

        // Clear the bit at the original position
        ownSingles &= ~(1L << fromIndex);

        // Handling different scenarios on the destination
        if ((enemySingles & (1L << toIndex)) != 0) {
            // Capture enemy single, transform to own single
            ownSingles |= (1L << toIndex);
            enemySingles &= ~(1L << toIndex);
        } else if ((enemyDoubles & (1L << toIndex)) != 0) {
            // Lands on enemy double, transforms to ownOnEnemy
            ownOnEnemy |= (1L << toIndex);
            enemyDoubles &= ~(1L << toIndex);
        } else if ((ownSingles & (1L << toIndex)) != 0) {
            // Lands on own single, transforms to own double
            ownDoubles |= (1L << toIndex);
            ownSingles &= ~(1L << toIndex);
        } else if ((enemyOnOwn & (1L << toIndex)) != 0) {
            // Lands on enemyOnOwn, transforms to own double
            ownDoubles |= (1L << toIndex);
            enemyOnOwn &= ~(1L << toIndex);
        } else {
            // Regular move to an empty square, becomes a single
            ownSingles |= (1L << toIndex);
        }

        // Update the bitboards directly
        if (isRed) {
            redSingles = ownSingles;
            blueSingles = enemySingles;
            redDoubles = ownDoubles;
            blueDoubles = enemyDoubles;
            red_on_blue = ownOnEnemy;
            blue_on_red = enemyOnOwn;
        } else {
            blueSingles = ownSingles;
            redSingles = enemySingles;
            blueDoubles = ownDoubles;
            redDoubles = enemyDoubles;
            blue_on_red = ownOnEnemy;
            red_on_blue = enemyOnOwn;
        }
    }



    private static void moveDoublePieceOnBitBoards(byte fromIndex, byte toIndex, boolean isRed) {
        // Determine which bitboards to use based on the player's color
        long ownDoubles = isRed ? redDoubles : blueDoubles;
        long ownSingles = isRed ? redSingles : blueSingles;
        long enemySingles = isRed ? blueSingles : redSingles;
        long enemyDoubles = isRed ? blueDoubles : redDoubles;
        long enemyOnOwn = isRed ? blue_on_red : red_on_blue;
        long ownOnEnemy = isRed ? red_on_blue : blue_on_red;

        // Determine the bottom type of the double
        boolean bottomIsEnemy = (ownOnEnemy & (1L << fromIndex)) != 0;

        // Remove the double piece from the original position
        ownDoubles &= ~(1L << fromIndex);
        ownOnEnemy &= ~(1L << fromIndex);

        // Handle the landing cases
        if ((ownSingles & (1L << toIndex)) != 0) {
            // Landing on own single, turn it into own double
            ownDoubles |= (1L << toIndex);
            ownSingles &= ~(1L << toIndex);
        } else if ((enemyOnOwn & (1L << toIndex)) != 0) {
            // Landing on enemy_on_own, turn it into own double
            ownDoubles |= (1L << toIndex);
            enemyOnOwn &= ~(1L << toIndex);
        } else if ((enemyDoubles & (1L << toIndex)) != 0) {
            // Landing on enemy double, turn it into ownOnEnemy
            ownOnEnemy |= (1L << toIndex);
            enemyDoubles &= ~(1L << toIndex);
        } else if ((enemySingles & (1L << toIndex)) != 0) {
            // Regular move to empty space, place the top of the double as a single
            ownSingles |= (1L << toIndex);
            enemySingles &= ~(1L << toIndex);
        } else {
            // Move to an empty square, becomes a single
            ownSingles |= (1L << toIndex);
        }

        // Always turn the former bottom of the double into a single at the original position
        if (bottomIsEnemy) {
            enemySingles |= (1L << fromIndex);
        } else {
            ownSingles |= (1L << fromIndex);
        }

        // Update the state directly
        if (isRed) {
            redSingles = ownSingles;
            blueSingles = enemySingles;
            redDoubles = ownDoubles;
            blueDoubles = enemyDoubles;
            red_on_blue = ownOnEnemy;
            blue_on_red = enemyOnOwn;
        } else {
            blueSingles = ownSingles;
            redSingles = enemySingles;
            blueDoubles = ownDoubles;
            redDoubles = enemyDoubles;
            blue_on_red = ownOnEnemy;
            red_on_blue = enemyOnOwn;
        }
    }



    private Circle createPiece(int pieceType) {
        Circle piece = null;

        switch (pieceType) {
            case 2: // Red piece
                piece = new Circle(25);
                piece.setFill(Color.rgb(200, 20, 20));
                piece.setStroke(Color.rgb(0, 0, 0));
                piece.setStrokeWidth(2);
                break;
            case 3: // Blue piece
                piece = new Circle(25);
                piece.setFill(Color.rgb(30, 80, 200));
                piece.setStroke(Color.rgb(0, 0, 0));
                piece.setStrokeWidth(2);
                break;
            case 4: // Red on Red
                piece = new Circle(30);
                piece.setFill(Color.rgb(200, 20, 20));
                piece.setStroke(Color.rgb(110, 14, 14));
                piece.setStrokeWidth(10);
                break;
            case 5: // Blue on Blue
                piece = new Circle(30);
                piece.setFill(Color.rgb(30, 80, 200));
                piece.setStroke(Color.rgb(25, 41, 96)); // Blue piece with blue outline
                piece.setStrokeWidth(10);
                break;
            case 6: // Blue on Red
                piece = new Circle(30);
                piece.setFill(Color.rgb(30, 80, 200));
                piece.setStroke(Color.rgb(200, 20, 20)); // Blue piece with red outline
                piece.setStrokeWidth(10);
                break;
            case 7: // Red on Blue
                piece = new Circle(30);
                piece.setFill(Color.rgb(200, 20, 20));
                piece.setStroke(Color.rgb(30, 80, 200)); // Red piece with blue outline
                piece.setStrokeWidth(10);
                break;
            default:
                break;
        }

        return piece;
    }

    public static byte convertToBitBoardIndex(int row, int col) {
        return (byte) (row * 8 + col);
    }

    public String currentWinningState() {
        // Define masks for the top and bottom rows

        long bluePieces = blueSingles | blueDoubles | blue_on_red;
        long redPieces = redSingles | redDoubles | red_on_blue;

        // Check if any blue piece is in the top row or no red pieces
        if ((bluePieces & topRowMask) != 0 || redPieces == 0) {
            return "Winner: Blue";
        }
        // Check if any red piece is in the bottom row or no blue pieces
        else if ((redPieces & bottomRowMask) != 0 || bluePieces == 0) {
            return "Winner: Red";
        }
        // Check if there are any possible moves for blue
        else if (getPossibleMovesForTeam(false) == 0) {
            // Check if there are any possible moves for red, if no -> draw
            if (getPossibleMovesForTeam(true) == 0) {
                return "Draw";
            } else {
                return "Winner: Red";
            }
        }
        // Check if there are any possible moves for red
        else if (getPossibleMovesForTeam(true) == 0) {
            // Check if there are any possible moves for blue, if no -> draw
            if (getPossibleMovesForTeam(false) == 0) {
                return "Draw";
            } else {
                return "Winner: Blue";
            }
        }
        return "Ongoing";
    }

    public long getPossibleMovesForTeam(boolean red) {
        if (red) {
            return getPossibleMovesSingles(redSingles, true) | getPossibleMovesDoubles(redDoubles | red_on_blue, true);
        } else {
            return getPossibleMovesSingles(blueSingles, false) | getPossibleMovesDoubles(blueDoubles | blue_on_red, false);
        }
    }



    public void makeMove2(int fromRow, int fromCol, int toRow, int toCol, boolean isRed) {
        // Convert the 2D grid indices to the corresponding bitboard indices
        byte fromIndex = convertToBitBoardIndex(fromRow, fromCol);
        byte toIndex = convertToBitBoardIndex(toRow, toCol);

        // It's a single piece
        doMoveAndReturnModifiedBitBoards(fromIndex, toIndex, isRed);

        // Rebuild the pieces on the board based on the new FEN
        placePiecesOnBoard();
        System.out.println(currentWinningState());

        String winningState = currentWinningState();
        if (winningState == "Winner: Red") {
            statusLabel.setText("Red Wins! Game over");
            statusLabel.setTextFill(Color.RED); // Change label color to red
        } else if (winningState == "Winner: Blue") {
            statusLabel.setText("Blue Wins! Game over");
            statusLabel.setTextFill(Color.BLUE); // Change label color to blue
        } else if (winningState == "Draw") {
            statusLabel.setText("It's a Draw!");
            statusLabel.setTextFill(Color.GRAY); // Change label color to gray
        }
    }


    public void placePiecesOnBoard() {

        String newFen = toFEN();
        piecePositions = readFEN(newFen);
        board.getChildren().clear(); // Clear the board to place the new pieces after the move

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                Button square = new Button();
                square.setPrefSize(100, 100);

                // background color and border
                square.setStyle("-fx-background-color: rgb(163,163,186); -fx-border-color: black; -fx-border-width: 2px;");

                int pieceType = piecePositions[row][col];

                Circle piece = createPiece(pieceType);

                if (piece != null) {
                    square.setGraphic(piece);
                    originalGraphics.put(square, piece); // Store original pieces
                }

                if (!((row == 0 && col == 0) || (row == 0 && col == 7) || (row == 7 && col == 0) || (row == 7 && col == 7))) {
                    board.add(square, col, row);
                }

                // Add the square to the 2D array for reference
                squares[row][col] = square;

                final int finalRow = row;
                final int finalCol = col;


                squares[row][col].setOnAction(event -> {
                    System.out.println("Square clicked at: (" + finalRow + ", " + finalCol + ")"+ getPieceType(finalRow,finalCol));


                    if (currentWinningState() == "Ongoing") {
                        // Case 1: Check if the same square is clicked again or if an empty piece is clicked
                        if (((finalRow == lastSelectedRow && finalCol == lastSelectedCol) || pieceType == 0 ) && !isMoveHighlighted(finalRow, finalCol)) {
                            clearHighlightedSquares(); //  clear the  highlights
                            lastSelectedCol = -1;

                            // Case 2: A piece has been selected and a valid move square is clicked
                        } else if (lastSelectedRow != -1 && lastSelectedCol != -1 && isMoveHighlighted(finalRow, finalCol)) {

                            makeMove2(lastSelectedRow, lastSelectedCol, finalRow, finalCol, isRed);// Make the move
                            turn = !turn;
                            lastSelectedRow = -1;
                            lastSelectedCol = -1;

                            // Case 3: First click on a new piece to highlight possible moves
                        } else if (turn && (pieceType == 2 || pieceType == 4 || pieceType == 7)) {
                            isRed = (pieceType == 2 || pieceType == 4 || pieceType == 7);
                            // Highlight possible moves for this piece
                            showPossibleMoves(finalRow, finalCol, isRed);
                            lastSelectedRow = finalRow;
                            lastSelectedCol = finalCol;
                        } else if (!turn && !(pieceType == 2 || pieceType == 4 || pieceType == 7)) {
                            isRed = (pieceType == 2 || pieceType == 4 || pieceType == 7);
                            // Highlight possible moves for this piece
                            showPossibleMoves(finalRow, finalCol, isRed);
                            lastSelectedRow = finalRow;
                            lastSelectedCol = finalCol;
                        }
                    }}
                );


            }
        }


    }


    private int getPieceType(int row, int col) {
        Circle piece = (Circle) squares[row][col].getGraphic();
        if (piece == null) return 0;

        // Check the properties of the Circle to determine its type
        Color fill = (Color) piece.getFill();
        Color stroke = (Color) piece.getStroke();

        if (fill.equals(Color.rgb(200, 20, 20)) && stroke.equals(Color.rgb(0, 0, 0))) {
            return 2; // Red single
        } else if (fill.equals(Color.rgb(30, 80, 200)) && stroke.equals(Color.rgb(0, 0, 0))) {
            return 3; // Blue single
        } else if (fill.equals(Color.rgb(200, 20, 20)) && stroke.equals(Color.rgb(110, 14, 14))) {
            return 4; // Red double
        } else if (fill.equals(Color.rgb(30, 80, 200)) && stroke.equals(Color.rgb(25, 41, 96))) {
            return 5; // Blue double
        } else if (fill.equals(Color.rgb(30, 80, 200)) && stroke.equals(Color.rgb(200, 20, 20))) {
            return 6; // Blue on red
        } else if (fill.equals(Color.rgb(200, 20, 20)) && stroke.equals(Color.rgb(30, 80, 200))) {
            return 7; // Red on blue
        }

        return 0;
    }


    private boolean isMoveHighlighted(int targetRow, int targetCol) {
        Button targetButton = squares[targetRow][targetCol];
        return highlightedSquares.contains(targetButton);
    }


    private void showPossibleMoves(int row, int col, boolean isRed) {
        // Clear previous highlights
        clearHighlightedSquares();

        int index = row * BOARD_SIZE + col; // index to use with the Bitboards

        // Get the possible moves bitboard for the piece at the current index
        long possibleMoves = getPossibleMovesForIndividualPiece((byte) index, isRed);

        for (int i = 0; i < BOARD_SIZE * BOARD_SIZE; i++) {
            if ((possibleMoves & (1L << i)) != 0) {

                int targetRow = i / BOARD_SIZE;
                int targetCol = i % BOARD_SIZE;

                // Highlight the target square
                Button targetSquare = squares[targetRow][targetCol];
                storeOriginalState(targetSquare); // Store the original state before adding the highlight
                Circle highlightPiece = new Circle(18);
                highlightPiece.setFill(Color.rgb(77, 10, 121));
                targetSquare.setGraphic(highlightPiece); // Highlight the square as a possible move
                highlightedSquares.add(targetSquare);
            }
        }
    }

    // store the original graphic state of a square
    private void storeOriginalState(Button targetSquare) {
        if (!originalGraphics.containsKey(targetSquare)) {
            originalGraphics.put(targetSquare, (Circle) targetSquare.getGraphic());
        }
    }

    private void clearHighlightedSquares() {
        for (Button targetSquare : highlightedSquares) {
            // Restore the original graphic (either a piece or null)
            targetSquare.setGraphic(originalGraphics.get(targetSquare));
        }
        highlightedSquares.clear(); // Clear the list of highlighted squares
    }

    public int[][] readFEN(String fen) {


        final String[][] TEMP_MAPPINGS = {
                {"r0", "X"}, // red
                {"b0", "Y"}, // blue
                {"rr", "A"}, // red double
                {"rb", "D"}, // Blue on red
                {"bb", "C"}, // blue double
                {"br", "B"}  // Red on blue
        };

        for (String[] mapping : TEMP_MAPPINGS) { // For easier indexes
            fen = fen.replace(mapping[0], mapping[1]);
        }

        String[] rows = fen.split("/");

        int[][] types = new int[BOARD_SIZE][BOARD_SIZE];

        // Loop through the rows in reverse order
        for (int row = 0; row < rows.length; row++) {
            int col = (row == 0 || row == 7) ? 1 : 0; //  for non-corner rows
            int currentRow = BOARD_SIZE - 1 - row; // reverses row order

            for (int i = 0; i < rows[row].length(); i++) {
                char c = rows[row].charAt(i);
                if (Character.isDigit(c)) {
                    // Empty squares
                    col += c - '0';
                } else {
                    byte index = (byte) ((7 - row) * 8 + col);
                    switch (c) {
                        case 'X' -> {
                            types[currentRow][col] = 2;
                            redSingles |= 1L << index;// Red piece
                        }
                        case 'Y' -> {
                            types[currentRow][col] = 3;
                            blueSingles |= 1L << index;// Blue piece
                        }
                        case 'A' -> {
                            types[currentRow][col] = 4;
                            redDoubles |= 1L << index;// Red on red
                        }
                        case 'C' -> {
                            types[currentRow][col] = 5;
                            blueDoubles |= 1L << index;// Blue on blue
                        }
                        case 'D' -> {
                            types[currentRow][col] = 6;
                            blue_on_red |= 1L << index;// Red on blue
                        }
                        case 'B' -> {
                            types[currentRow][col] = 7;
                            red_on_blue |= 1L << index;// Blue on red
                        }
                        default -> types[currentRow][col] = 0;
                    }
                    col++;
                }
            }
        }

        return types;
    }

    public long getPossibleMovesForIndividualPiece(byte index, boolean isRed) {
        long singlePieceMask = 1L << index;

        // single pieces
        if (((isRed ? redSingles : blueSingles) & singlePieceMask) != 0) {
            return getPossibleMovesSingles(singlePieceMask, isRed);
        }

        // double pieces
        if (((isRed ? (redDoubles | red_on_blue) : (blueDoubles | blue_on_red)) & singlePieceMask) != 0) {
            return getPossibleMovesDoubles(singlePieceMask, isRed);
        }
        throw new IllegalStateException("index"+index +" doesnt fit any figure type");
    }

    public long getPossibleMovesSingles(long singles, boolean isRed) {
        int direction = isRed ? RED_DIRECTION : BLUE_DIRECTION;
        long emptySpaces = ~(redSingles | blueSingles | redDoubles | blueDoubles | red_on_blue | blue_on_red) & CORNER_MASK; // All empty spaces
        long enemyPieces = isRed ? (blue_on_red | blueDoubles | blueSingles) : (redSingles | redDoubles | red_on_blue); // Enemy single figures

        long jumpable = (emptySpaces | (isRed ? redSingles : blueSingles));
        // Forward moves (no capture)
        long forwardMoves = shift(singles, direction) & jumpable;//Removes all occupied spaces,
        //commentedBits("Fwd:",forwardMoves);
        // Side moves (left and right)
        long leftMoves = shift(singles & NOT_A_FILE, -1) & jumpable;
        long rightMoves = shift(singles & NOT_H_FILE, 1) & jumpable;

        // Capture moves (diagonal)
        long leftCapture = shift(singles & NOT_A_FILE, direction - 1) & enemyPieces; //+-7 9 so diagonal
        long rightCapture = shift(singles & NOT_H_FILE, direction + 1) & enemyPieces;
        //System.out.println("Possible moves:");
        return forwardMoves | leftMoves | rightMoves | leftCapture | rightCapture;
    }

    public long getPossibleMovesDoubles(long doubles, boolean isRed) {//FIXED
        long occupiedSpaces = redSingles | blueSingles | redDoubles | blueDoubles | red_on_blue | blue_on_red;
        long emptySpaces = ~occupiedSpaces & CORNER_MASK; // All empty spaces, excluding corners

        //long emptyOrSingleDoubleable = (emptySpaces | (isRed ? redSingles : blueSingles) | (isRed? redDoubles : blueDoubles));
        long jumpable = (emptySpaces | (redSingles | blueSingles) | (isRed ? blueDoubles : redDoubles) | (isRed ? blue_on_red : red_on_blue));

        long twoForwardOneLeft = shift(doubles & (isRed ? NOT_A_FILE : NOT_H_FILE), isRed ? 15 : -15);
        long oneForwardTwoLeft = shift(doubles & (isRed ? NOT_AB_FILE : NOT_GH_FILE), isRed ? 6 : -6);

        long twoForwardOneRight = shift(doubles & (isRed ? NOT_H_FILE : NOT_A_FILE), isRed ? 17 : -17);
        long oneForwardTwoRight = shift(doubles & (isRed ? NOT_GH_FILE : NOT_AB_FILE), isRed ? 10 : -10);

        return jumpable & (twoForwardOneLeft | oneForwardTwoLeft | twoForwardOneRight | oneForwardTwoRight);
        //All possible moves for doubles. We can capture on all 4 fields, though do we need extra capture?

    }

    public static long shift(long bitboard, int offset) {
        if (bitboard == 0) return 0;
        return offset > 0 ? (bitboard << offset & CORNER_MASK) : (bitboard >>> -offset & CORNER_MASK);
    }

    public static String toFEN() {
        StringBuilder fen = new StringBuilder();

        for (int row = 0; row < 8; row++) {
            int emptyCount = 0;

            for (int col = (row == 0 || row == 7) ? 1 : 0; col < ((row == 0 || row == 7) ? 7 : 8); col++) {//Start end have corners,6 length
                int index = (7 - row) * 8 + col;
                if (index == 0 || index == 7 || index == 56) {//63 never happens
                    continue; // Skip corners
                }
                String piece; // Set depending on bitboards
                if ((redSingles & (1L << index)) != 0) {
                    piece = "r0";
                } else if ((blueSingles & (1L << index)) != 0) {
                    piece = "b0";
                } else if ((redDoubles & (1L << index)) != 0) {
                    piece = "rr";
                } else if ((blueDoubles & (1L << index)) != 0) {
                    piece = "bb";
                } else if ((blue_on_red & (1L << index)) != 0) {
                    piece = "rb";
                } else if ((red_on_blue & (1L << index)) != 0) {
                    piece = "br";
                } else {
                    emptyCount++;
                    piece = "";
                }

                if (!piece.isEmpty()) { // Count if empty for number
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(piece);
                } else if (col == 7 || col == 6 && (row == 0 || row == 7)) {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                }
            }
            if (row < 7) {
                fen.append('/');
            }
        }
        return fen.toString();
    }

    public static void main(String[] args) {
        launch();
    }
}