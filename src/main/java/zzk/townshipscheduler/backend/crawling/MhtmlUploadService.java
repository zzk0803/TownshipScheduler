package zzk.townshipscheduler.backend.crawling;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Service for processing uploaded MHTML files using JavaMail API.
 * MHTML (MIME HTML) is a single file format that bundles HTML, CSS, images, and other resources.
 * 
 * This implementation uses the mature JavaMail library for reliable MIME parsing.
 */
@Service
public class MhtmlUploadService {

    private static final Logger logger = LoggerFactory.getLogger(MhtmlUploadService.class);

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    
    /**
     * Expected filename pattern for uploaded MHTML files.
     */
    public static final String EXPECTED_FILENAME = "Goods _ Township Wiki _ Fandom.mhtml";

    /**
     * Process uploaded MHTML file and extract HTML document using JavaMail API.
     *
     * @param mhtmlInputStream The MHTML file input stream
     * @return Parsed Jsoup Document
     * @throws IOException if processing fails
     */
    public Document processUploadedMhtml(InputStream mhtmlInputStream) throws IOException {
        logger.info("Processing uploaded MHTML file using JavaMail API");

        try {
            // Read all bytes first (for small files < 50MB)
            byte[] mhtmlBytes = mhtmlInputStream.readAllBytes();
            
            if (mhtmlBytes.length > MAX_FILE_SIZE) {
                throw new IOException("MHTML 文件过大，超过 50MB 限制");
            }

            // Create JavaMail Session (no server configuration needed)
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            
            // Parse MHTML as MimeMessage
            MimeMessage message = new MimeMessage(session, new ByteArrayInputStream(mhtmlBytes));
            
            // Get content
            Object content = message.getContent();
            
            if (content instanceof String) {
                // Simple HTML without multipart
                logger.debug("MHTML contains simple string content");
                return Jsoup.parse((String) content);
                
            } else if (content instanceof Multipart) {
                // Multipart MIME - extract HTML part
                logger.debug("MHTML contains multipart content");
                Multipart multipart = (Multipart) content;
                String htmlContent = extractHtmlFromMultipart(multipart);
                return Jsoup.parse(htmlContent);
                
            } else {
                throw new IOException("不支持的 MHTML 内容类型：" + 
                    (content != null ? content.getClass().getName() : "null"));
            }
            
        } catch (MessagingException e) {
            logger.error("解析 MHTML 失败", e);
            throw new IOException("MHTML 解析失败：" + e.getMessage(), e);
        }
    }

    /**
     * Extract HTML content from Multipart MIME structure.
     */
    private String extractHtmlFromMultipart(Multipart multipart) throws MessagingException, IOException {
        int count = multipart.getCount();
        logger.debug("Multipart contains {} parts", count);

        for (int i = 0; i < count; i++) {
            BodyPart part = multipart.getBodyPart(i);
            String contentType = part.getContentType().toLowerCase();
            
            logger.debug("Part {}: Content-Type={}", i, contentType);
            
            // Look for HTML content
            if (contentType.startsWith("text/html")) {
                logger.info("Found HTML part at index {}", i);
                
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
                logger.warn("Using fallback: found text part at index {}", i);
                Object partContent = part.getContent();
                if (partContent instanceof String) {
                    return (String) partContent;
                }
            }
        }

        throw new IOException("MHTML 中未找到 HTML 内容部分");
    }

    /**
     * Validate MHTML file header.
     */
    public void validateMhtmlHeader(InputStream inputStream) throws IOException {
        // Read first 1KB to check for MHTML signatures
        byte[] buffer = new byte[1024];
        int bytesRead = inputStream.read(buffer);
        
        if (bytesRead < 50) {
            throw new IOException("文件太小，不是有效的 MHTML 文件");
        }
        
        String header = new String(buffer, StandardCharsets.UTF_8);
        
        // Check for common MHTML signatures
        boolean isValidMhtml = header.contains("MIME-Version:") ||
                               header.contains("Content-Type: multipart/related") ||
                               header.contains("boundary=");
        
        if (!isValidMhtml) {
            throw new IOException(
                "不是有效的 MHTML 文件格式。请确保使用浏览器保存为\"MHTML 单个文件\"格式"
            );
        }
        
        logger.debug("MHTML header validation passed");
    }
}
