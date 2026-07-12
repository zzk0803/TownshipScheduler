package zzk.townshipscheduler.backend.crawling;

import jakarta.mail.Multipart;
import org.jsoup.nodes.Document;

public record MHtmlParseResult(Document document, Multipart multipart) {

}
