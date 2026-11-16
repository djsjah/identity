package com.identity.core.image.validation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class ImageUploadValidator {
    private static final long MAX_PIXELS = 25_000_000L;

    private final Tika tika = new Tika();
    private final MultipartProperties multipartProperties;

    public ImageUploadData validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file must not be empty");
        }
        if (file.getSize() > multipartProperties.getMaxFileSize().toBytes()) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image file exceeds the allowed size");
        }

        byte[] content = readContent(file);
        String contentType = tika.detect(content);

        String extension = switch (contentType) {
            case MediaType.IMAGE_JPEG_VALUE -> ".jpg";
            case MediaType.IMAGE_PNG_VALUE -> ".png";
            default -> throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only JPEG and PNG images are supported"
            );
        };

        validateDimensions(content);
        return new ImageUploadData(contentType, extension, content);
    }

    private static byte[] readContent(MultipartFile file) {
        try {
            return file.getBytes();
        }
        catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read uploaded image", ex);
        }
    }

    private static void validateDimensions(byte[] content) {
        try (ImageInputStream imageInputStream = new MemoryCacheImageInputStream(new ByteArrayInputStream(content))) {
            Iterator<ImageReader> imageReaderIterator = ImageIO.getImageReaders(imageInputStream);

            if (!imageReaderIterator.hasNext()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image content");

            ImageReader imageReader = imageReaderIterator.next();
            try {
                imageReader.setInput(imageInputStream, true, true);
                int width = imageReader.getWidth(0);
                int height = imageReader.getHeight(0);

                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Image dimensions exceed the allowed limit"
                    );
                }
            }
            finally {
                imageReader.dispose();
            }

        }
        catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or corrupted image", ex);
        }
    }
}
