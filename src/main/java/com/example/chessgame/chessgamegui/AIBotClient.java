package com.example.chessgame.chessgamegui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class AIBotClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5095;

    // Method to connect to the AI server and get the best move for the given FEN
    public static String getBestMoveFromServer(String fen) {
        String bestMove = "";

        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Send the FEN to the server
            System.out.println("Sending FEN to server: " + fen);
            out.println(fen);

            // Receive the best move from the server
            bestMove = in.readLine();
            System.out.println("Received best move from server: " + bestMove);

        } catch (Exception e) {
            System.out.println("Error connecting to AI server: " + e.getMessage());
            e.printStackTrace();
        }

        return bestMove;
    }
}
