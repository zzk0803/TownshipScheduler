package zzk.townshipscheduler.backend.crawling;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public final class CrawledDataCell implements Cloneable {

    @Builder.Default
    private String html = "";

    @Builder.Default
    private String text = "";

    private List<Anchor> anchorList;

    private List<Img> imgList;

    private Type type = Type.CELL;

    private CellSpan span;

    public boolean boolContentLooksLikeCategory() {
        return !HeadUtility.boolContentNotLikeCategory(getType(), getText(), getAnchorList());
    }

    public String getTextAsHead() {
        if (!text.isBlank()) {
            return text;
        }
        if (!anchorList.isEmpty()) {
            return "%s".formatted(anchorList.getFirst()
                    .getTitle());
        }
        if (!imgList.isEmpty()) {
            return "%s".formatted(imgList.getFirst()
                    .getAlt());
        }
        return html;
    }

    public String reasonableText() {
        if (!text.isBlank()) {
            return text;
        }
        if (!imgList.isEmpty()) {
            return "%s".formatted(imgList.getFirst()
                    .getAlt());
        }
        if (!anchorList.isEmpty()) {
            return "%s".formatted(anchorList.getFirst()
                    .getTitle());
        }
        return html;
    }

    public Optional<String> getImageString() {
        if (imgList.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(imgList.stream()
                .map(Img::getSrc)
                .toList()
                .getFirst());
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(getHtml());
        result = 31 * result + Objects.hashCode(getText());
        result = 31 * result + Objects.hashCode(getAnchorList());
        result = 31 * result + Objects.hashCode(getImgList());
        result = 31 * result + Objects.hashCode(getType());
        result = 31 * result + Objects.hashCode(getSpan());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CrawledDataCell that)) return false;

        return Objects.equals(getHtml(), that.getHtml()) && Objects.equals(getText(), that.getText()) && Objects.equals(
                getAnchorList(),
                that.getAnchorList()
        ) && Objects.equals(getImgList(), that.getImgList()) && getType() == that.getType() && Objects.equals(
                getSpan(),
                that.getSpan()
        );
    }

    @Override
    public CrawledDataCell clone() {
        try {
            CrawledDataCell cloned = (CrawledDataCell) super.clone();
            cloned.setImgList(this.imgList);
            cloned.setAnchorList(this.anchorList);
            cloned.setType(this.type);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public enum Type {
        HEAD, CELL
    }

    @Data
    public static class Anchor {

        private String href;

        private String title;

        private String text;

    }

    @Data
    public static class Img {

        private String alt;

        private String src;

        private byte[] bytes;

        @Override
        public int hashCode() {
            return Objects.hashCode(getSrc());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Img img)) return false;
            return Objects.equals(getSrc(), img.getSrc());
        }

    }

    @Data
    @AllArgsConstructor
    public static class CellSpan {

        public static final int REGULAR = 0;

        public static final int NA_DEFINE = 0;

        public static final int NA_EFFECT = 1;

        private int rowSpan = NA_DEFINE;

        private int columnSpan = NA_DEFINE;

    }

    private static class HeadUtility {

        private static boolean boolContentNotLikeCategory(Type type, String text, List<Anchor> anchorList) {
            if (type == Type.CELL) {
                return true;
            } else {
                return textEmptyOrBlank(text) || textHasComma(text) || textSplitSpaceMoreThan3(text) || !justOneAnchor(
                        anchorList);
            }
        }

        private static boolean textEmptyOrBlank(String text) {
            return text.isEmpty() || text.isBlank();
        }

        private static boolean textHasComma(String text) {
            return Arrays.stream(text.split(","))
                           .count() > 1;
        }

        private static boolean textSplitSpaceMoreThan3(String text) {
            return text.split("\\s").length > 3;
        }

        private static boolean justOneAnchor(List<Anchor> anchorList) {
            return anchorList.size() == 1;
        }

    }

}
