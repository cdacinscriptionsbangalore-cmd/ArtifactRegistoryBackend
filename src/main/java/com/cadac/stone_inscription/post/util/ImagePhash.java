package com.cadac.stone_inscription.post.util;

import dev.brachtendorf.jimagehash.hash.Hash;
import dev.brachtendorf.jimagehash.hashAlgorithms.PerceptiveHash;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.web.multipart.MultipartFile;

public class ImagePhash {

    public static Hash genratePhash(MultipartFile image) throws IOException {

        return new PerceptiveHash(64).hash(ImageIO.read(image.getInputStream()));
    }

    public static Double imagePHashComparing(Hash image1Phash, Hash image2Phash) {

        double normalizedHamming = image1Phash.normalizedHammingDistance(image2Phash);

        double similarityPercent = (1 - normalizedHamming) * 100;

        return similarityPercent;

    }

}
