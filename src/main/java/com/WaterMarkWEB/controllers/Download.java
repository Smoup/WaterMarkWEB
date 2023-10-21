package com.WaterMarkWEB.controllers;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Controller
public class Download {

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadImage() {
        final var projectRootPath = System.getProperty("user.dir");
        final var filePath = projectRootPath + "/temp-image-file.png";

        var imageFile = new File(filePath);

        if (!imageFile.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        try {
            var fis = new FileInputStream(imageFile);
            var bytes = IOUtils.toByteArray(fis);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentDispositionFormData("attachment", "watermarked-image.png");

            return ResponseEntity.ok().headers(headers).body(bytes);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
