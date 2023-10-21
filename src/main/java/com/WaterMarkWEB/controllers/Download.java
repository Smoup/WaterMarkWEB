package com.WaterMarkWEB.controllers;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Controller
public class Download {

    @GetMapping("/download")
    public String downloadImage(HttpServletResponse response, Model model) {
        try (var os = response.getOutputStream()) {
            final var projectRootPath = System.getProperty("user.dir");
            final var filePath = projectRootPath + "/temp-image-file.png";

            var imageFile = new File(filePath);

            if (!imageFile.exists()) {
                model.addAttribute("errorMessage", "Image not found");
                return "home";
            }

            response.setContentType("image/png");
            response.setHeader("Content-Disposition", "attachment; filename=watermarked-image.png");

            sendFile(imageFile, os);
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Error downloading image");
            return "home";
        }
        return null;
    }

    private static void sendFile(File imageFile, ServletOutputStream os) {
        try (var fis = new FileInputStream(imageFile)) {
            var buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
