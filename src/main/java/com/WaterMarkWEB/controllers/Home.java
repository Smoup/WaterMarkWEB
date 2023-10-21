package com.WaterMarkWEB.controllers;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Controller
public class Home {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @PostMapping("/upload")
    public String uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "watermark", required = false) MultipartFile watermark,
            @RequestParam(value = "useDefaultWatermark", required = false) String useDefaultWatermark,
            Model model) {

        if (file.isEmpty()) {
            model.addAttribute("errorMessage", "Please attach an image");
            return "home";
        }

        if ((watermark == null || watermark.isEmpty()) &&
                (useDefaultWatermark == null || !useDefaultWatermark.equals("on"))) {
            model.addAttribute(
                    "errorMessage",
                    "Please upload a watermark or activate the checkbox");
            return "home";
        }

        try {
            var imageFile = File.createTempFile("image", ".jpg");
            file.transferTo(imageFile);

            File watermarkFile = null;
            if (watermark != null && !watermark.isEmpty()) {
                watermarkFile = File.createTempFile("watermark", ".png");
                watermark.transferTo(watermarkFile);
            }

            boolean isDefaultWatermarkSelected = useDefaultWatermark != null && useDefaultWatermark.equals("on");

            addWaterMark(model, watermarkFile, isDefaultWatermarkSelected, imageFile);

            return "home";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Error processing image");
            return "home";
        }
    }

    private static void addWaterMark(Model model, File watermarkFile, boolean isDefMark, File imageFile) {
        try {
            if (watermarkFile != null || isDefMark) {
                BufferedImage watermarkImage;

                if (isDefMark) {
                    var inputStream = Path.of(System.getProperty("user.dir") + "/default-watermark.png").toFile();
                    watermarkImage = ImageIO.read(inputStream);
                } else {
                    watermarkImage = ImageIO.read(watermarkFile);
                }

                Thumbnails.of(imageFile)
                        .watermark(Positions.CENTER, watermarkImage, 1.0f)
                        .outputQuality(1.0)
                        .scale(1.0)
                        .toFile(new File("temp-image-file.png"));

                model.addAttribute("infoMessage", "Your watermark file is ready to download");
            } else {
                model.addAttribute("errorMessage", "Error");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

