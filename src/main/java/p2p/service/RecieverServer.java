package service;

public class RecieverServer {

    public void recieveFile(String ipAddress, Integer port){
        
        try (ServerSocket server = new ServerSocket(port)) {
            Socket clientSocket = server.accept();

        }
    }

}