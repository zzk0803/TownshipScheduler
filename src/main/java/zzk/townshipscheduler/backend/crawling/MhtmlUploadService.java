package zzk.townshipscheduler.backend.crawling;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for processing uploaded MHTML files containing saved web pages with all resources.
 * MHTML (MIME HTML) is a single file format that bundles HTML, CSS, images, and other resources.
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
     * Process uploaded MHTML file and extract HTML document.
     *
     * @param mhtmlInputStream The MHTML file input stream
     * @return Parsed Jsoup Document
     * @throws IOException if processing fails
     */
    public Document processUploadedMhtml(InputStream mhtmlInputStream) throws IOException {
        logger.info("Processing uploaded MHTML file");

        // Read the entire MHTML content
        String mhtmlContent = new String(mhtmlInputStream.readAllBytes(), StandardCharsets.UTF_8);
        
        // Validate MHTML format
        if (!isValidMhtml(mhtmlContent)) {
            throw new IOException("无效的 MHTML 文件格式");
        }

        // Extract HTML part from MHTML
        String htmlContent = extractHtmlFromMhtml(mhtmlContent);
        
        // Parse and return
        return Jsoup.parse(htmlContent);
    }

    /**
     * Validate if the content is a valid MHTML file.
     */
    private boolean isValidMhtml(String content) {
        // MHTML files typically start with MIME headers
        return content.contains("MIME-Version:") || 
               content.contains("Content-Type:") ||
               content.contains("multipart/related");
    }

    /**
     * Extract the main HTML content from MHTML.
     * 
     * MHTML format:
     * ------=_NextPart_000_0000_01234567.89ABCDEF
     * Content-Type: text/html
     * Content-Location: file:///C:/path/to/file.html
     * 
     * [HTML content here]
     * 
     * ------=_NextPart_000_0000_01234567.89ABCDEF
     * Content-Type: image/png
     * Content-Transfer-Encoding: base64
     * 
     * [Base64 encoded image]
     */
    private String extractHtmlFromMhtml(String mhtmlContent) throws IOException {
        logger.debug("Extracting HTML from MHTML content");

        // Pattern to match MIME boundaries
        Pattern boundaryPattern = Pattern.compile(
            "------=_NextPart_[0-9A-F_]+",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher boundaryMatcher = boundaryPattern.matcher(mhtmlContent);
        if (!boundaryMatcher.find()) {
            throw new IOException("未找到 MHTML 边界标识符");
        }
        
        String boundary = boundaryMatcher.group();
        logger.debug("Found MHTML boundary: {}", boundary);

        // Split by boundary
        String[] parts = mhtmlContent.split(Pattern.quote(boundary));
        
        logger.debug("Found {} parts in MHTML", parts.length);

        // Find the HTML part
        for (String part : parts) {
            if (part.contains("Content-Type: text/html") || 
                part.contains("Content-Type: application/x-html")) {
                
                logger.debug("Found HTML part");
                
                // Extract content after headers
                String htmlContent = extractPartContent(part);
                
                // Decode if necessary
                if (part.contains("Content-Transfer-Encoding: quoted-printable")) {
                    htmlContent = decodeQuotedPrintable(htmlContent);
                }
                
                return htmlContent;
            }
        }

        // If no HTML part found, try to find the first text part
        for (String part : parts) {
            if (part.trim().startsWith("Content-Type: text/plain") ||
                part.trim().startsWith("Content-Type: text/html")) {
                
                logger.debug("Using fallback: found text part");
                return extractPartContent(part);
            }
        }

        throw new IOException("MHTML 中未找到 HTML 内容部分");
    }

    /**
     * Extract content from a MIME part (after headers).
     */
    private String extractPartContent(String mimePart) {
        // Find the end of headers (empty line)
        int headerEnd = mimePart.indexOf("\r\n\r\n");
        if (headerEnd == -1) {
            headerEnd = mimePart.indexOf("\n\n");
        }
        
        if (headerEnd == -1) {
            return ""; // No content
        }
        
        // Extract content after headers
        String content = mimePart.substring(headerEnd + 2).trim();
        
        // Remove trailing boundary or dashes
        if (content.startsWith("--")) {
            int boundaryStart = content.indexOf("\r\n--");
            if (boundaryStart > 0) {
                content = content.substring(0, boundaryStart);
            }
        }
        
        return content;
    }

    /**
     * Decode Quoted-Printable encoding.
     */
    private String decodeQuotedPrintable(String input) {
        try {
            // Replace =XX hex sequences with actual characters
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (c == '=' && i + 2 < input.length()) {
                    String hex = input.substring(i + 1, i + 3);
                    try {
                        int charCode = Integer.parseInt(hex, 16);
                        result.append((char) charCode);
                        i += 2; // Skip the two hex digits
                    } catch (NumberFormatException e) {
                        result.append(c);
                    }
                } else if (c == '=' && (i + 1 == input.length() || 
                          input.charAt(i + 1) == '\r' || input.charAt(i + 1) == '\n')) {
                    // Soft line break - skip it
                    i++;
                    if (i < input.length() && input.charAt(i) == '\r') i++;
                    if (i < input.length() && input.charAt(i) == '\n') i++;
                } else {
                    result.append(c);
                }
            }
            return result.toString();
        } catch (Exception e) {
            logger.warn("解码 Quoted-Printable 失败，返回原始内容", e);
            return input;
        }
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
                               header.contains("------=_NextPart_");
        
        if (!isValidMhtml) {
            throw new IOException(
                "不是有效的 MHTML 文件格式。请确保使用浏览器保存为\"MHTML 单个文件\"格式"
            );
        }
        
        logger.debug("MHTML header validation passed");
    }
}
