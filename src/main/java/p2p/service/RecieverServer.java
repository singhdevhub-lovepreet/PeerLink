package p2p.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class RecieverServer {

    public void receiveFile(String serverIpAddress, int serverPort, String saveFilePath) throws IOException {
        try (Socket socket = new Socket(serverIpAddress, serverPort);
             InputStream inputStream = socket.getInputStream();
             FileOutputStream fileOutputStream = new FileOutputStream(saveFilePath)) {

            System.out.println("Connected to server: " + serverIpAddress + ":" + serverPort);
            System.out.println("Receiving file and saving to: " + saveFilePath);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }

            System.out.println("File received successfully and saved.");

        } catch (IOException e) {
            System.err.println("Error receiving file: " + e.getMessage());
            throw e;
        }
    }

}