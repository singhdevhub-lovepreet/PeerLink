package p2p.controller;

import p2p.service.FileSharer;

import java.io.*;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class FileController {
    private final FileSharer fileSharer;
    private final HttpServer server;
    private final String uploadDir;
    private final ExecutorService executorService;

    public FileController(int port) throws IOException {
        this.fileSharer = new FileSharer();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.uploadDir = System.getProperty("java.io.tmpdir") + File.separator + "peerlink-uploads";
        this.executorService = Executors.newFixedThreadPool(10);
        
        File uploadDirFile = new File(uploadDir);
        if (!uploadDirFile.exists()) {
            uploadDirFile.mkdirs();
        }
        
        server.createContext("/upload", new UploadHandler());
        server.createContext("/download", new DownloadHandler());
        server.createContext("/", new CORSHandler());
        
        server.setExecutor(executorService);
    }
    
    public void start() {
        server.start();
        System.out.println("API server started on port " + server.getAddress().getPort());
    }
    
    public void stop() {
        server.stop(0);
        executorService.shutdown();
        System.out.println("API server stopped");
    }
    
    private class CORSHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Handle CORS preflight requests
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "Content-Type,Authorization");
            
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            
            // For any other request, return 404
            String response = "Not Found";
            exchange.sendResponseHeaders(404, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    private class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String response = "Method Not Allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
            
            Headers requestHeaders = exchange.getRequestHeaders();
            String contentType = requestHeaders.getFirst("Content-Type");
            
            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                String response = "Bad Request: Content-Type must be multipart/form-data";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
            
            String boundary = contentType.substring(contentType.indexOf("boundary=") + 9);

            InputStream requestBody = exchange.getRequestBody();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = requestBody.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            
            byte[] requestData = baos.toByteArray();
            String requestString = new String(requestData);
            
            String filenameHeader = "filename=\"";
            int filenameStart = requestString.indexOf(filenameHeader);
            if (filenameStart == -1) {
                String response = "Bad Request: No filename found";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
            
            filenameStart += filenameHeader.length();
            int filenameEnd = requestString.indexOf("\"", filenameStart);
            String filename = requestString.substring(filenameStart, filenameEnd);
            
            String fileContentHeader = "\r\n\r\n";
            int fileContentStart = requestString.indexOf(fileContentHeader, filenameEnd) + fileContentHeader.length();
            
            String boundaryEnd = "--" + boundary + "--";
            int fileContentEnd = requestString.indexOf(boundaryEnd, fileContentStart) - 2; // -2 for \r\n
            
            if (fileContentStart == -1 || fileContentEnd == -1 || fileContentEnd <= fileContentStart) {
                String response = "Bad Request: Could not parse file content";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
            
            byte[] fileContent = new byte[fileContentEnd - fileContentStart];
            System.arraycopy(requestData, fileContentStart, fileContent, 0, fileContent.length);
            
            String uniqueFilename = UUID.randomUUID().toString() + "_" + filename;
            String filePath = uploadDir + File.separator + uniqueFilename;
            
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(fileContent);
            }
            
            int port = fileSharer.offerFile(filePath);
            
            new Thread(() -> fileSharer.startFileServer(port)).start();
            
            String jsonResponse = "{\"port\": " + port + "}";
            headers.add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonResponse.getBytes());
            }
        }
    }
    
    private class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                String response = "Method Not Allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }
            
            // Extract port from path
            String path = exchange.getRequestURI().getPath();
            String portStr = path.substring(path.lastIndexOf('/') + 1);
            
            try {
                int port = Integer.parseInt(portStr);
                
                // For the UI demo, we'll just return a success message
                // In a real implementation, we would connect to the peer and download the file
                String response = "File download initiated on port " + port;
                headers.add("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } catch (NumberFormatException e) {
                String response = "Bad Request: Invalid port number";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }
}
