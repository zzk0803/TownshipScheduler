package zzk.townshipscheduler.backend.crawling;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
class MhtmlImageExtractor {

    public List<ImageData> processMultipart(Multipart multipart) throws Exception {
        List<ImageData> images = new ArrayList<>();

        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            String contentType = bodyPart.getContentType()
                    .toLowerCase();

            // 只处理图片类型
            if (contentType.startsWith("image/")) {
                ImageData info = extractImage(bodyPart, i);
                if (info != null) {
                    images.add(info);
                }
            }
            // 递归处理嵌套的multipart
            else if (bodyPart.getContent() instanceof Multipart) {
                images.addAll(processMultipart((Multipart) bodyPart.getContent()));
            }
        }

        return images;
    }

    private ImageData extractImage(BodyPart bodyPart, int i) throws Exception {
        // 获取图片类型
        String contentType = bodyPart.getContentType();
        String mimeType = contentType.split(";")[0].trim();
        String extension = getExtensionFromMimeType(mimeType);
        int bodyPartSize = bodyPart.getSize();

        // 获取Content-ID和Content-Location（用于关联HTML中的引用）
        String contentId = getHeaderValue(bodyPart, "Content-ID");
        String contentLocation = getHeaderValue(bodyPart, "Content-Location");

        // 生成文件名
        String filename = generateFilename(contentId, contentLocation, i, extension);


        // 保存图片数据
        long size;
        try (
                InputStream is = bodyPart.getInputStream();
                ByteArrayOutputStream fos = new ByteArrayOutputStream(bodyPartSize)
        ) {
            size = is.transferTo(fos);
        }

        // 返回图片信息
        ImageData info = new ImageData();
        info.setFilename(filename);
        info.setContentType(mimeType);
        info.setContentId(contentId);
        info.setContentLocation(contentLocation);
        info.setSize(size);

        System.out.printf(
                "提取图片: %s (类型: %s, 大小: %d bytes)%n",
                filename, mimeType, size
        );

        return info;
    }

    private String getHeaderValue(BodyPart part, String name) throws Exception {
        String[] values = part.getHeader(name);
        if (values != null && values.length > 0) {
            // 去除可能的尖括号 <xxx>
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

    private String generateFilename(
            String contentId, String contentLocation,
            int index, String extension
    ) {
        String baseName;

        if (contentLocation != null && !contentLocation.isEmpty()) {
            // 从URL路径提取文件名
            baseName = contentLocation;
            int lastSlash = baseName.lastIndexOf('/');
            if (lastSlash >= 0) {
                baseName = baseName.substring(lastSlash + 1);
            }
            // 去除查询参数
            int queryIdx = baseName.indexOf('?');
            if (queryIdx > 0) {
                baseName = baseName.substring(0, queryIdx);
            }
            // 去除扩展名（后面统一加）
            baseName = baseName.replaceAll("\\.[^.]+$", "");
        } else if (contentId != null && !contentId.isEmpty()) {
            baseName = "cid_" + contentId.replaceAll("[^a-zA-Z0-9]", "_");
        } else {
            baseName = "image_" + index;
        }

        // 清理非法字符
        baseName = baseName.replaceAll("[\\\\/:*?\"<>|]", "_");

        // 限制长度
        if (baseName.length() > 50) {
            baseName = baseName.substring(0, 50);
        }

        return baseName + "." + extension;
    }

    // 图片信息类
    public static class ImageData {

        private String filename;

        private String filepath;

        private String contentType;

        private String contentId;

        private String contentLocation;

        private byte[] data;

        private long size;

        // Getters and Setters
        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getFilepath() {
            return filepath;
        }

        public void setFilepath(String filepath) {
            this.filepath = filepath;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String getContentId() {
            return contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        public String getContentLocation() {
            return contentLocation;
        }

        public void setContentLocation(String contentLocation) {
            this.contentLocation = contentLocation;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return String.format(
                    "ImageData{filename='%s', type='%s', size=%d bytes}",
                    filename, contentType, size
            );
        }

    }

}
