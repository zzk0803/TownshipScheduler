package zzk.townshipscheduler.backend.crawling;

import jakarta.activation.DataHandler;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
class MhtmlImageExtractor {

    public List<ImageData> processMultipart(MimeMultipart multipart)
            throws Exception {
        List<ImageData> images = new ArrayList<>();

        log.info("multipart.getCount()=={}", multipart.getCount());
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            DataHandler dataHandler = bodyPart.getDataHandler();
            String bodyPartEncoding = MimeUtility.getEncoding(dataHandler);
            String contentType = bodyPart.getContentType().toLowerCase();
            log.info("contentType:{},bodyPartEncoding:{}", contentType, bodyPartEncoding);

            if (contentType.startsWith("image")) {
                try {
                    images.add(extractImage(bodyPart, i, bodyPartEncoding));
                } catch (Exception e) {
                    log.error(e.toString());
                }
            } else if (bodyPart.getContent() instanceof Multipart) {
                images.addAll(processMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }

        return images;
    }

    private ImageData extractImage(BodyPart bodyPart, int i, String bodyPartEncoding)
            throws Exception {
        String contentType = bodyPart.getContentType();
        String mimeType = contentType.split(";")[0].trim();
        String extension = getExtensionFromMimeType(mimeType);
        int bodyPartSize = bodyPart.getSize();

        String contentId = getHeaderValue(bodyPart, "Content-ID");
        String contentLocation = getHeaderValue(bodyPart, "Content-Location");
        String filename = generateFilename(contentId, contentLocation, i, extension);

        long size;
        byte[] byteArray;
        try (InputStream is = bodyPart.getInputStream(); ByteArrayOutputStream fos = new ByteArrayOutputStream(bodyPartSize)) {
            size = is.transferTo(fos);
            byteArray = fos.toByteArray();
        }

        ImageData info = new ImageData();
        info.setFilename(filename);
        info.setContentType(mimeType);
        info.setContentId(contentId);
        info.setContentLocation(contentLocation);
        info.setData(byteArray);
        info.setSize(size);

        return info;
    }

    private String getHeaderValue(BodyPart part, String name)
            throws Exception {
        String[] values = part.getHeader(name);
        if (values != null && values.length > 0) {
            return values[0].replaceAll("[<>]", "");
        }
        return null;
    }

    private String getExtensionFromMimeType(String mimeType) {
        return switch (mimeType) {
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/bmp" -> "bmp";
            case "image/webp" -> "webp";
            case "image/svg+xml" -> "svg";
            default -> "bin";
        };
    }

    private String generateFilename(String contentId, String contentLocation, int index, String extension) {
        String baseName;

        if (contentLocation != null && !contentLocation.isEmpty()) {
            baseName = contentLocation;
            int lastSlash = baseName.lastIndexOf('/');
            if (lastSlash >= 0) {
                baseName = baseName.substring(lastSlash + 1);
            }
            int queryIdx = baseName.indexOf('?');
            if (queryIdx > 0) {
                baseName = baseName.substring(0, queryIdx);
            }
            baseName = baseName.replaceAll("\\.[^.]+$", "");
        } else if (contentId != null && !contentId.isEmpty()) {
            baseName = "cid_" + contentId.replaceAll("[^a-zA-Z0-9]", "_");
        } else {
            baseName = "image_" + index;
        }

        baseName = baseName.replaceAll("[\\\\/:*?\"<>|]", "_");

        if (baseName.length() > 50) {
            baseName = baseName.substring(0, 50);
        }

        return baseName + "." + extension;
    }

    @Data
    public static class ImageData {

        private String filename;

        private String filepath;

        private String contentType;

        private String contentId;

        private String contentLocation;

        private byte[] data;

        private long size;

    }

}
