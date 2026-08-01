package zzk.townshipscheduler.backend.crawling;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Service for processing uploaded MHTML files using JavaMail API.
 * MHTML (MIME HTML) is a single file format that bundles HTML, CSS, images, and other resources.
 * <p>
 * This implementation uses the mature JavaMail library for reliable MIME parsing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MhtmlProcessComponent {

    /**
     * Expected filename pattern for uploaded MHTML files.
     */
    public static final String EXPECTED_FILENAME = "Goods _ Township Wiki _ Fandom.mhtml";

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    public Result processMhtmlFile(File mhtmlFile) {
        if (mhtmlFile == null) {
            throw new IllegalArgumentException();
        }

        if (mhtmlFile.isDirectory()) {
            throw new IllegalArgumentException();
        }

        if (!mhtmlFile.exists()) {
            throw new IllegalArgumentException();
        }

        try {
            byte[] bytes = Files.readAllBytes(mhtmlFile.toPath());
            return processMhtmlBytes(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public Result processMhtmlBytes(byte[] data) {
        try (var inputStream = new ByteArrayInputStream(data)) {
            this.validateMhtmlHeader(inputStream);
            inputStream.reset();

            return this.parseMhtmlInputStream(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Validate MHTML file header.
     */
    public void validateMhtmlHeader(InputStream inputStream)
            throws IOException {
        // Read first 1KB to check for MHTML signatures
        byte[] buffer = new byte[1024];
        int bytesRead = inputStream.read(buffer);

        if (bytesRead < 50) {
            throw new IOException("文件太小，不是有效的 MHTML 文件");
        }

        String header = new String(buffer, StandardCharsets.UTF_8);

        // Check for common MHTML signatures
        boolean isValidMhtml = header.contains("MIME-Version:") || header.contains("Content-Type: multipart/related") || header.contains("boundary=");

        if (!isValidMhtml) {
            throw new IOException("不是有效的 MHTML 文件格式。请确保使用浏览器保存为\"MHTML 单个文件\"格式");
        }

        log.debug("MHTML header validation passed");
    }

    /**
     * Process uploaded MHTML file and extract HTML document using JavaMail API.
     *
     * @param mhtmlInputStream The MHTML file input stream
     * @return Parsed Jsoup Document
     * @throws IOException if processing fails
     */
    public Result parseMhtmlInputStream(InputStream mhtmlInputStream)
            throws IOException {
        log.info("Processing uploaded MHTML file using JavaMail API");
        try {
            // Read all bytes first (for small files < 50MB)
            byte[] mhtmlBytes = mhtmlInputStream.readAllBytes();

            if (mhtmlBytes.length > MAX_FILE_SIZE) {
                throw new IOException("MHTML 文件过大，超过 50MB 限制");
            }

            // Create JavaMail Session (no server configuration needed)
            Properties props = new Properties();

            // Parse MHTML as MimeMessage
            MimeMessage message = new MimeMessage(
                    Session.getDefaultInstance(props),
                    new ByteArrayInputStream(mhtmlBytes)
            );

            // Get content
            Object content = message.getContent();

            if (content instanceof String) {
                // Simple HTML without multipart
                log.debug("MHTML contains simple string content");
                return new Result(null, Jsoup.parse((String) content));

            } else if (content instanceof MimeMultipart multipart) {
                // Multipart MIME - extract HTML part
                log.debug("MHTML contains multipart content");
                String htmlContent = extractHtmlFromMultipart(multipart);
                return new Result(multipart, Jsoup.parse(htmlContent));

            } else {
                throw new IOException("不支持的 MHTML 内容类型：" + (content != null
                        ? content.getClass().getName()
                        : "null"));
            }

        } catch (MessagingException e) {
            log.error("解析 MHTML 失败", e);
            throw new IOException("MHTML 解析失败：" + e.getMessage(), e);
        }
    }

    /**
     * Extract HTML content from Multipart MIME structure.
     */
    private String extractHtmlFromMultipart(Multipart multipart)
            throws MessagingException, IOException {
        int count = multipart.getCount();
        log.debug("Multipart contains {} parts", count);

        for (int i = 0; i < count; i++) {
            BodyPart part = multipart.getBodyPart(i);
            String contentType = part.getContentType().toLowerCase();

            log.debug("Part {}: Content-Type={}", i, contentType);

            // Look for HTML content
            if (contentType.startsWith("text/html")) {
                log.info("Found HTML part at index {}", i);

                // Get content
                Object partContent = part.getContent();
                if (partContent instanceof String) {
                    return (String) partContent;
                } else if (partContent instanceof InputStream) {
                    return new String(((InputStream) partContent).readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }

        // Fallback: try to get first text part
        for (int i = 0; i < count; i++) {
            BodyPart part = multipart.getBodyPart(i);
            String contentType = part.getContentType().toLowerCase();

            if (contentType.startsWith("text/plain") || contentType.startsWith("text/")) {
                log.warn("Using fallback: found text part at index {}", i);
                Object partContent = part.getContent();
                if (partContent instanceof String) {
                    return (String) partContent;
                }
            }
        }

        throw new IOException("MHTML 中未找到 HTML 内容部分");
    }

    public record Result(
            MimeMultipart multipart,
            Document document
    ) {

    }

}
