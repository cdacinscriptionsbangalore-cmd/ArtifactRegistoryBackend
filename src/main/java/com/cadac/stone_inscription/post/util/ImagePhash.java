package com.cadac.stone_inscription.post.util;

import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import com.cadac.stone_inscription.exception.StoneInscriptionException;

public class ImagePhash {

    public static Hash genratePhash(MultipartFile image) throws IOException {

        BufferedImage bufferedImage = ImageIO.read(image.getInputStream());

        if (bufferedImage == null) {
            System.out.println("Cannot decode image: " + image.getOriginalFilename() + " contentType="
                    + image.getContentType() + " size=" + image.getSize() + "ImageIO ka issue hai ");
            throw new StoneInscriptionException("Unsupported or corrupted image",HttpStatus.BAD_REQUEST);
        } else {
            return new PerceptiveHash(64).hash(bufferedImage);
        }

    }

    public static Double imagePHashComparing(Hash image1Phash, Hash image2Phash) {

        double normalizedHamming = image1Phash.normalizedHammingDistance(image2Phash);

        double similarityPercent = (1 - normalizedHamming) * 100;

        return similarityPercent;

    }

}
