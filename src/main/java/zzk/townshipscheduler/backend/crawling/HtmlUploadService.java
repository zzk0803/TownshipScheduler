package zzk.townshipscheduler.backend.crawling;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service for processing uploaded ZIP files containing saved web pages.
 * Extracts and parses HTML content from "Save Page As" archives.
 */
@Service
public class HtmlUploadService {

    private static final Logger logger = LoggerFactory.getLogger(HtmlUploadService.class);

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final String[] POSSIBLE_HTML_NAMES = {"Goods.html", "goods.html","Goods.htm", "goods.htm","Goods _ Township Wiki _ Fandom.htm"};

    /**
     * Process uploaded ZIP file and extract HTML document.
     *
     * @param zipInputStream The ZIP file input stream
     * @return Parsed Jsoup Document
     * @throws IOException if processing fails
     */
    public Document processUploadedZip(InputStream zipInputStream) throws IOException {
        logger.info("Processing uploaded ZIP file");

        Path tempDir = Files.createTempDirectory(".township-scheduler");
        try {
            // Unzip to temporary directory
            unzipToTempDirectory(zipInputStream, tempDir);
            logger.debug("Unzipped to: {}", tempDir);

            // Find main HTML file
            Path htmlPath = findMainHtmlFile(tempDir)
                    .orElseThrow(() -> new IOException(
                            "未找到主 HTML 文件。请确保 ZIP 包含类似 'Goods.html' 的文件"
                    ));

            logger.info("Found HTML file: {}", htmlPath);

            // Parse and return
            return parseHtmlFile(htmlPath);

        } finally {
            // Cleanup temporary files
            cleanupTempDirectory(tempDir);
        }
    }

    /**
     * Unzip the input stream to a temporary directory.
     *
     * @param zipInputStream The ZIP input stream
     * @param tempDir        The temporary directory
     * @throws IOException if extraction fails
     */
    private void unzipToTempDirectory(InputStream zipInputStream, Path tempDir) throws IOException {
        long totalSize = 0;
        int entryCount = 0;

        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry entry;

            logger.info("Starting ZIP extraction to: {}", tempDir);

            while ((entry = zis.getNextEntry()) != null) {
                // Log all entries for debugging
                logger.debug("Found ZIP entry: '{}' (size: {}, dir: {})", 
                    entry.getName(), entry.getSize(), entry.isDirectory());

                // Validate entry name to prevent path traversal attacks
                String entryName = entry.getName();
                if (!isValidZipEntryName(entryName)) {
                    logger.warn("Skipping invalid entry name: '{}'", entryName);
                    zis.closeEntry();
                    continue;
                }

                // Check total size to prevent DoS
                totalSize += entry.getCompressedSize();
                if (totalSize > MAX_FILE_SIZE) {
                    throw new IOException("ZIP 文件过大，超过 50MB 限制");
                }

                Path entryPath = tempDir.resolve(entryName).normalize();

                // Security check: ensure the resolved path is within tempDir
                if (!entryPath.startsWith(tempDir)) {
                    logger.warn("Skipping entry with path traversal attempt: {}", entryName);
                    zis.closeEntry();
                    continue;
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                    logger.debug("Created directory: {}", entryName);
                } else {
                    // Create parent directories if needed
                    Path parent = entryPath.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }

                    // Extract file
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    logger.debug("Extracted: {} ({} bytes)", entryName, entry.getSize());
                    entryCount++;
                }

                zis.closeEntry();
            }

            logger.info("Successfully extracted {} entries (total size: {} bytes)", entryCount, totalSize);

            // List all files in temp directory for debugging
            try (var files = Files.list(tempDir)) {
                var fileList = files.toList();
                logger.info("Files in temp directory ({}):", fileList.size());
                fileList.forEach(f -> logger.info("  - {}", f.getFileName()));
            }
        }
    }

    /**
     * Validate ZIP entry name to prevent path traversal attacks and invalid names.
     *
     * @param name the entry name
     * @return true if the name is valid
     */
    private boolean isValidZipEntryName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        // Check for path traversal attempts
        if (name.contains("..") || name.startsWith("/") || name.startsWith("\\")) {
            return false;
        }

        // Check for invalid characters that might cause issues
        // Some ZIP files from browsers may contain problematic characters
        if (name.contains("\0")) {
            return false;
        }

        return true;
    }

    /**
     * Find the main HTML file in the extracted directory.
     * Looks for common naming patterns from browser "Save Page As".
     *
     * @param extractedDir The extracted directory
     * @return Optional path to the main HTML file
     */
    private Optional<Path> findMainHtmlFile(Path extractedDir) {
        logger.info("Searching for HTML file in: {}", extractedDir);
        
        // List all files for debugging
        try (var files = Files.list(extractedDir)) {
            var fileList = files.toList();
            logger.info("Found {} files/directories in extracted dir", fileList.size());
            fileList.forEach(f -> {
                try {
                    logger.info("  - {} (dir: {}, size: {} bytes)", 
                        f.getFileName(), Files.isDirectory(f), 
                        Files.isRegularFile(f) ? Files.size(f) : 0);
                } catch (IOException e) {
                    logger.debug("Failed to get size for: {}", f.getFileName());
                }
            });
        } catch (IOException e) {
            logger.warn("Failed to list directory contents", e);
        }
        
        // Try common HTML filenames (exact match)
        logger.debug("Trying exact filename matches...");
        for (String possibleName : POSSIBLE_HTML_NAMES) {
            Path htmlPath = extractedDir.resolve(possibleName);
            logger.debug("Checking: {} -> exists: {}", possibleName, Files.exists(htmlPath));
            if (Files.exists(htmlPath) && Files.isRegularFile(htmlPath)) {
                logger.info("Found exact match: {}", possibleName);
                return Optional.of(htmlPath);
            }
        }
        
        // Fallback 1: Case-insensitive search
        logger.debug("Trying case-insensitive search...");
        try (var stream = Files.list(extractedDir)) {
            Optional<Path> found = stream
                .filter(path -> Files.isRegularFile(path))
                .filter(path -> {
                    String name = path.getFileName().toString().toLowerCase();
                    return name.endsWith(".htm") || name.endsWith(".html");
                })
                .filter(path -> !path.toString().contains("_files") && 
                               !path.toString().contains(".files"))
                .findFirst();
            
            if (found.isPresent()) {
                logger.info("Found HTML file (case-insensitive): {}", found.get().getFileName());
                return found;
            }
        } catch (IOException e) {
            logger.warn("Error during case-insensitive search", e);
        }
        
        // Fallback 2: Fuzzy match - look for files containing "Goods" or "Township"
        logger.debug("Trying fuzzy match...");
        try (var stream = Files.list(extractedDir)) {
            Optional<Path> found = stream
                .filter(path -> Files.isRegularFile(path))
                .filter(path -> {
                    String name = path.getFileName().toString().toLowerCase();
                    return (name.contains("goods") || name.contains("township")) &&
                           (name.endsWith(".htm") || name.endsWith(".html"));
                })
                .filter(path -> !path.toString().contains("_files") && 
                               !path.toString().contains(".files"))
                .findFirst();
            
            if (found.isPresent()) {
                logger.info("Found HTML file (fuzzy match): {}", found.get().getFileName());
                return found;
            }
        } catch (IOException e) {
            logger.warn("Error during fuzzy search", e);
        }
        
        logger.error("No HTML file found in: {}", extractedDir);
        return Optional.empty();
    }

    /**
     * Parse an HTML file into a Jsoup Document.
     *
     * @param htmlPath Path to the HTML file
     * @return Parsed Jsoup Document
     * @throws IOException if parsing fails
     */
    private Document parseHtmlFile(Path htmlPath) throws IOException {
        logger.info("Parsing HTML file: {}", htmlPath.getFileName());

        // Parse with UTF-8 encoding
        Document document = Jsoup.parse(
                htmlPath.toFile(),
                StandardCharsets.UTF_8.name()
        );

        // Validate that it contains expected content
        validateHtmlContent(document);

        return document;
    }

    /**
     * Validate that the HTML contains expected wiki content.
     *
     * @param document The parsed document
     * @throws IOException if validation fails
     */
    private void validateHtmlContent(Document document) throws IOException {
        // Check for article-table class (Township wiki specific)
        if (document.getElementsByClass("article-table").isEmpty()) {
            throw new IOException(
                    "HTML 中未找到商品表格 (class='article-table')。" +
                    "请确认是正确的 Township Wiki 页面保存的文件。"
            );
        }

        logger.info("HTML validation passed - found article tables");
    }

    /**
     * Clean up temporary directory and all its contents.
     *
     * @param tempDir The temporary directory to clean
     */
    private void cleanupTempDirectory(Path tempDir) {
        try {
            if (Files.exists(tempDir)) {
                try (var stream = Files.walk(tempDir)) {
                    stream.sorted((a, b) -> b.compareTo(a)) // Reverse order for deep deletion
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException e) {
                                    logger.warn("Failed to delete temp file: {}", path, e);
                                }
                            });
                }
            }
            logger.debug("Cleaned up temporary directory: {}", tempDir);
        } catch (IOException e) {
            logger.error("Error cleaning up temporary directory", e);
        }
    }

    /**
     * Validate ZIP file header to ensure it's a valid ZIP archive.
     *
     * @param inputStream The input stream to validate
     * @throws IOException if not a valid ZIP file
     */
    public void validateZipHeader(InputStream inputStream) throws IOException {
        byte[] header = new byte[4];
        int bytesRead = inputStream.read(header);

        if (bytesRead < 4) {
            throw new IOException("文件太小，不是有效的 ZIP 文件");
        }

        // ZIP files start with PK\003\004 (0x504B0304)
        boolean isValidZip = (header[0] == (byte) 0x50 &&
                             header[1] == (byte) 0x4b &&
                             header[2] == (byte) 0x03 &&
                             header[3] == (byte) 0x04) ||
                            // Also allow empty ZIP or other signatures
                            (header[0] == (byte) 0x50 &&
                             header[1] == (byte) 0x4b &&
                             header[2] == (byte) 0x05 &&
                             header[3] == (byte) 0x06);

        if (!isValidZip) {
            throw new IOException(
                    "不是有效的 ZIP 文件格式。文件头：" +
                    String.format("%02X %02X %02X %02X", header[0], header[1], header[2], header[3])
            );
        }

        logger.debug("ZIP header validation passed");
    }

    public void validateZipHeader(byte[] data) throws IOException {
        if (data.length < 4) {
            throw new IOException("文件太小，不是有效的 ZIP 文件");
        }

        // ZIP files start with PK\003\004 (0x504B0304)
        // Read first 4 bytes directly from the data array
        boolean isValidZip = (data[0] == (byte) 0x50 &&
                data[1] == (byte) 0x4b &&
                data[2] == (byte) 0x03 &&
                data[3] == (byte) 0x04) ||
                // Also allow empty ZIP or other signatures
                (data[0] == (byte) 0x50 &&
                        data[1] == (byte) 0x4b &&
                        data[2] == (byte) 0x05 &&
                        data[3] == (byte) 0x06);

        if (!isValidZip) {
            throw new IOException(
                    "不是有效的 ZIP 文件格式。文件头：" +
                            String.format("%02X %02X %02X %02X", data[0], data[1], data[2], data[3])
            );
        }

        logger.debug("ZIP header validation passed");
    }

}
