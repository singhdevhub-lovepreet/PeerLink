package p2p.controller;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.io.IOUtils;
import p2p.service.FileSharer;

public class FileController {

    private final FileSharer fileSharer;
    private final HttpServer server;
    private final String uploadDir;
    private final ExecutorService executorService;

    public FileController(int port) throws IOException {
        this.fileSharer = new FileSharer();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.uploadDir =
            System.getProperty("java.io.tmpdir") +
            File.separator +
            "peerlink-uploads";
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
        System.out.println(
            "API server started on port " + server.getAddress().getPort()
        );
    }

    public void stop() {
        server.stop(0);
        executorService.shutdown();
        System.out.println("API server stopped");
    }

    private class CORSHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.add(
                "Access-Control-Allow-Headers",
                "Content-Type,Authorization"
            );

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String response = "Not Found";
            exchange.sendResponseHeaders(404, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    private static class MultipartParser {

        private final byte[] data;
        private final String boundary;

        public MultipartParser(byte[] data, String boundary) {
            this.data = data;
            this.boundary = boundary;
        }

        public ParseResult parse() {
            try {
                byte[] headerSeparator = { 13, 10, 13, 10 }; // \r\n\r\n
                int headerEndIndex = findSequence(data, headerSeparator, 0);
                if (headerEndIndex == -1) {
                    return null;
                }
                int contentStartIndex = headerEndIndex + headerSeparator.length;

                String headers = new String(data, 0, headerEndIndex);

                byte[] boundarySeparator =
                    ("\r\n--" + this.boundary).getBytes();
                int contentEndIndex = findSequence(
                    data,
                    boundarySeparator,
                    contentStartIndex
                );

                if (contentEndIndex == -1) {
                    boundarySeparator = ("\r\n--" +
                        this.boundary +
                        "--").getBytes();
                    contentEndIndex = findSequence(
                        data,
                        boundarySeparator,
                        contentStartIndex
                    );
                }

                if (contentEndIndex == -1) {
                    return null;
                }

                byte[] fileContent = Arrays.copyOfRange(
                    data,
                    contentStartIndex,
                    contentEndIndex
                );

                String filename = "unnamed-file";
                String filenameMarker = "filename=\"";
                int filenameStart = headers.indexOf(filenameMarker);
                if (filenameStart != -1) {
                    filenameStart += filenameMarker.length();
                    int filenameEnd = headers.indexOf("\"", filenameStart);
                    if (filenameEnd != -1) {
                        filename = headers.substring(
                            filenameStart,
                            filenameEnd
                        );
                    }
                }

                String contentType = "application/octet-stream";
                String contentTypeMarker = "Content-Type: ";
                int contentTypeStart = headers.indexOf(contentTypeMarker);
                if (contentTypeStart != -1) {
                    contentTypeStart += contentTypeMarker.length();
                    int contentTypeEnd = headers.indexOf(
                        "\r\n",
                        contentTypeStart
                    );
                    if (contentTypeEnd != -1) {
                        contentType = headers.substring(
                            contentTypeStart,
                            contentTypeEnd
                        );
                    }
                }

                return new ParseResult(filename, contentType, fileContent);
            } catch (Exception e) {
                System.err.println(
                    "Error parsing multipart data: " + e.getMessage()
                );
                e.printStackTrace(); // Keep this for debugging
                return null;
            }
        }

        private int findSequence(byte[] data, byte[] sequence, int startPos) {
            outer: for (
                int i = startPos;
                i <= data.length - sequence.length;
                i++
            ) {
                for (int j = 0; j < sequence.length; j++) {
                    if (data[i + j] != sequence[j]) {
                        continue outer;
                    }
                }
                return i;
            }
            return -1;
        }

        public static class ParseResult {

            public final String filename;
            public final String contentType;
            public final byte[] fileContent;

            public ParseResult(
                String filename,
                String contentType,
                byte[] fileContent
            ) {
                this.filename = filename;
                this.contentType = contentType;
                this.fileContent = fileContent;
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

            if (
                contentType == null ||
                !contentType.startsWith("multipart/form-data")
            ) {
                String response =
                    "Bad Request: Content-Type must be multipart/form-data";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            try {
                String boundary = contentType.substring(
                    contentType.indexOf("boundary=") + 9
                );

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                IOUtils.copy(exchange.getRequestBody(), baos);
                byte[] requestData = baos.toByteArray();

                MultipartParser parser = new MultipartParser(
                    requestData,
                    boundary
                );
                MultipartParser.ParseResult result = parser.parse();

                if (result == null) {
                    String response =
                        "Bad Request: Could not parse file content";
                    exchange.sendResponseHeaders(
                        400,
                        response.getBytes().length
                    );
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                    return;
                }

                String filename = result.filename;
                if (filename == null || filename.trim().isEmpty()) {
                    filename = "unnamed-file";
                }

                String uniqueFilename =
                    UUID.randomUUID().toString() +
                    "_" +
                    new File(filename).getName();
                String filePath = uploadDir + File.separator + uniqueFilename;

                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    fos.write(result.fileContent);
                }

                int port = fileSharer.offerFile(filePath);

                new Thread(() -> fileSharer.startFileServer(port)).start();

                String jsonResponse = "{\"port\": " + port + "}";
                headers.add("Content-Type", "application/json");
                exchange.sendResponseHeaders(
                    200,
                    jsonResponse.getBytes().length
                );
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonResponse.getBytes());
                }
            } catch (Exception e) {
                System.err.println(
                    "Error processing file upload: " + e.getMessage()
                );
                String response = "Server error: " + e.getMessage();
                exchange.sendResponseHeaders(500, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
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

            String path = exchange.getRequestURI().getPath();
            String portStr = path.substring(path.lastIndexOf('/') + 1);

            try {
                int port = Integer.parseInt(portStr);

                try (
                    Socket socket = new Socket("localhost", port);
                    InputStream socketInput = socket.getInputStream()
                ) {
                    File tempFile = File.createTempFile("download-", ".tmp");
                    String filename = "downloaded-file"; // Default filename

                    try (
                        FileOutputStream fos = new FileOutputStream(tempFile)
                    ) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;

                        ByteArrayOutputStream headerBaos =
                            new ByteArrayOutputStream();
                        int b;
                        while ((b = socketInput.read()) != -1) {
                            if (b == '\n') break;
                            headerBaos.write(b);
                        }

                        String header = headerBaos.toString().trim();
                        if (header.startsWith("Filename: ")) {
                            filename = header.substring("Filename: ".length());
                        }

                        while ((bytesRead = socketInput.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }

                    headers.add(
                        "Content-Disposition",
                        "attachment; filename=\"" + filename + "\""
                    );
                    headers.add("Content-Type", "application/octet-stream");

                    exchange.sendResponseHeaders(200, tempFile.length());
                    try (
                        OutputStream os = exchange.getResponseBody();
                        FileInputStream fis = new FileInputStream(tempFile)
                    ) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }

                    tempFile.delete();
                } catch (IOException e) {
                    System.err.println(
                        "Error downloading file from peer: " + e.getMessage()
                    );
                    String response =
                        "Error downloading file: " + e.getMessage();
                    headers.add("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(
                        500,
                        response.getBytes().length
                    );
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
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
