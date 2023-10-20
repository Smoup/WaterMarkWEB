package org.smoup;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class WebServer {
    private final static int BUFFER_SIZE = 256;
    private AsynchronousServerSocketChannel server;

    private final static String HEADERS =
            "HTTP/1.1 200 OK\n" +
                    "Server: naive\n" +
                    "Content-Type: text/html\n" +
                    "Content-Length: %s\n" +
                    "Connection: close\n\n";

    private final static String UPLOAD_FORM =
            "<form action=\"/upload\" method=\"post\" enctype=\"multipart/form-data\">" +
                    "<input type=\"file\" name=\"file\" accept=\"image/*\" required>" +
                    "<input type=\"submit\" value=\"Загрузить\">" +
            "</form>";

    public void bootstrap() {
        try {
            server = AsynchronousServerSocketChannel.open();
            server.bind(new InetSocketAddress("127.0.0.1", 8080));

            while (true) {
                Future<AsynchronousSocketChannel> future = server.accept();
                handleClient(future);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Future<AsynchronousSocketChannel> future)
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        System.out.println("New client thread");

        AsynchronousSocketChannel clientChannel = future.get(30, TimeUnit.SECONDS);

        while (clientChannel != null && clientChannel.isOpen()) {
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            StringBuilder builder = new StringBuilder();
            boolean keepReading = true;

            while (keepReading) {
                clientChannel.read(buffer).get();

                int position = buffer.position();
                keepReading = position == BUFFER_SIZE;

                byte[] array = keepReading
                        ? buffer.array()
                        : Arrays.copyOfRange(buffer.array(), 0, position);

                builder.append(new String(array));
                buffer.clear();
            }

            String request = builder.toString();

            if (request.contains("GET /download")) {
                // Здесь обрабатываем запрос на скачивание обработанного изображения
                handleImageDownload(clientChannel);
            } else if (request.contains("POST /upload")) {
                // Здесь обрабатываем загрузку и обработку изображения
                handleImageUpload(clientChannel, request);
            } else {
                // Отправляем HTML форму для загрузки изображения
                String body = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <title>Загрузка и обработка изображения</title>
                        </head>
                        <body>
                            <h1>Загрузка и обработка изображения</h1>
                            %s
                            <br>
                            <a href="/download">Скачать обработанное изображение</a>
                        </body>
                        </html>
                        """.formatted(UPLOAD_FORM);
                String page = String.format(HEADERS, body.length()) + body;
                ByteBuffer resp = ByteBuffer.wrap(page.getBytes());
                clientChannel.write(resp);
            }

            clientChannel.close();
        }
    }

    private void handleImageDownload(AsynchronousSocketChannel clientChannel) throws IOException {
        // Здесь вы можете добавить код для обработки и отправки обработанного изображения
        // Загрузите обработанное изображение и отправьте его клиенту

        Path imageFilePath = Path.of("path/to/processed/image.jpg");

        if (Files.exists(imageFilePath)) {
            byte[] imageBytes = Files.readAllBytes(imageFilePath);
            String headers = String.format(HEADERS, imageBytes.length);
            ByteBuffer responseBuffer = ByteBuffer.wrap(headers.getBytes());
            clientChannel.write(responseBuffer);
            responseBuffer = ByteBuffer.wrap(imageBytes);
            clientChannel.write(responseBuffer);
        } else {
            String notFoundResponse = "HTTP/1.1 404 Not Found\nContent-Length: 0\nConnection: close\n\n";
            ByteBuffer notFoundBuffer = ByteBuffer.wrap(notFoundResponse.getBytes());
            clientChannel.write(notFoundBuffer);
        }
    }

    private void handleImageUpload(AsynchronousSocketChannel clientChannel, String request) throws IOException {
        // Разбор заголовков запроса и извлечение данных файла

        String boundary = request.substring(request.indexOf("boundary=") + "boundary=".length());
        boundary = "--" + boundary;

        byte[] boundaryBytes = boundary.getBytes();

        int boundaryIndex = 0;

        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        ByteArrayOutputStream fileData = new ByteArrayOutputStream();
        String fileName = null;

        int readBytes;

        while (true) {
            try {
                if (!((readBytes = clientChannel.read(buffer).get()) != -1)) break;
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            buffer.flip();

            for (int i = 0; i < readBytes; i++) {
                byte b = buffer.get(i);

                if (b == boundaryBytes[boundaryIndex]) {
                    boundaryIndex++;
                    if (boundaryIndex == boundaryBytes.length) {
                        // Мы достигли конца границы
                        if (fileName != null) {
                            // Записать данные в файл
                            Files.write(Path.of(fileName), fileData.toByteArray(), StandardOpenOption.CREATE);
                            // Применить водяной знак к файлу
                            applyWatermark(Path.of(fileName), "path/to/watermark.png");
                            fileData.reset();
                        }
                        // Прочитать заголовки для следующего файла
                        String headers = new String(fileData.toByteArray());
                        String[] headerLines = headers.split("\r\n");
                        for (String header : headerLines) {
                            if (header.startsWith("Content-Disposition:")) {
                                int startIndex = header.indexOf("filename=\"") + "filename=\"".length();
                                int endIndex = header.indexOf("\"", startIndex);
                                fileName = header.substring(startIndex, endIndex);
                            }
                        }
                        fileData.reset();
                        boundaryIndex = 0;
                    }
                } else {
                    boundaryIndex = 0;
                }

                fileData.write(b);
            }

            buffer.clear();
        }

        // Отправить ответ об успешной загрузке
        String response = "HTTP/1.1 200 OK\nContent-Length: 0\nConnection: close\n\n";
        clientChannel.write(ByteBuffer.wrap(response.getBytes()));
    }

    private void applyWatermark(Path imagePath, String watermarkPath) throws IOException {
        BufferedImage originalImage = ImageIO.read(imagePath.toFile());
        BufferedImage watermarkImage = ImageIO.read(new File(watermarkPath));

        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        Graphics2D graphics = originalImage.createGraphics();
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        graphics.drawImage(watermarkImage, 0, 0, null);
        graphics.dispose();

        ImageIO.write(originalImage, "jpg", imagePath.toFile());
    }
}
