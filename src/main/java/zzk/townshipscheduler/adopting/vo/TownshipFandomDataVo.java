package zzk.townshipscheduler.adopting.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import zzk.townshipscheduler.backend.crawling.RawDataCrawledCell;

import java.io.Serializable;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TownshipFandomDataVo
        implements Serializable {

    public static final String NA = "N/A";

    private String category = NA;

    private String name = NA;

    private String image = NA;

    private Optional<byte[]> imageBytes;

    private String materials = NA;

    private String level = NA;

    private String cost = NA;

    private String sellPrice = NA;

    private String xp = NA;

    private String time = NA;

    private String dealerAvailable = NA;

    private String helpGrade = NA;

    public static TownshipFandomVoBuilder builder() {
        return new TownshipFandomVoBuilder();
    }

    public static class TownshipFandomVoBuilder {

        private String category;

        private String name;

        private String image;

        private byte[] imageBytes;

        private transient Function<String, byte[]> imgBytsFunc;

        private String materials;

        private String level;

        private String cost;

        private String sellPrice;

        private String xp;

        private String time;

        private String dealerAvailable;

        private String helpGrade;

        TownshipFandomVoBuilder() {
        }

        public TownshipFandomVoBuilder ofField(String fieldName, RawDataCrawledCell value) {
            final String text = value.getText();
            switch (fieldName.toLowerCase(Locale.ENGLISH)) {
                case "goods" -> {
                    return name(value.reasonableText());
                }
                case "goods[colspan:1]",
                     "Goods[colspan:1]" -> {//include symbol '[' ']',so str.tolowercase() doesn't work??
                    Optional<String> imgOptional = value.getImageString();
                    imgOptional.ifPresent(this::image);
                    return this;
                }
                case "image" -> {
                    return image(value.getImgList()
                                         .getFirst()
                                         .getSrc());
                }
                case "materials" -> {
                    return materials(text);
                }
                case "level" -> {
                    return level(text);
                }
                case "cost" -> {
                    return cost(text);
                }
                case "sell price", "price" -> {
                    return sellPrice(text);
                }
                case "xp" -> {
                    return xp(text);
                }
                case "time 0%-0", "Time 0%-0[colspan:1]", "time 0%-0[colspan:1]" -> {
                    return time(text);
                }
                case "dealer available icon", "Dealer Available Icon[colspan:1]",
                     "dealer available icon[colspan:1]" -> {
                    return dealerAvailable(text);
                }
                case "help icon", "Help Icon[colspan:1]", "help icon[colspan:1]" -> {
                    return helpGrade(text);
                }
                default -> {
                    throw new IllegalArgumentException(fieldName + "=" + value);
                }
            }
        }

        public TownshipFandomVoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public TownshipFandomVoBuilder image(String image) {
            this.image = image;
            this.imageBytes = this.imgBytsFunc.apply(this.image);
            return this;
        }

        public TownshipFandomVoBuilder materials(String materials) {
            this.materials = materials;
            return this;
        }

        public TownshipFandomVoBuilder level(String level) {
            this.level = level;
            return this;
        }

        public TownshipFandomVoBuilder cost(String cost) {
            this.cost = cost;
            return this;
        }

        public TownshipFandomVoBuilder sellPrice(String sellPrice) {
            this.sellPrice = sellPrice;
            return this;
        }

        public TownshipFandomVoBuilder xp(String xp) {
            this.xp = xp;
            return this;
        }

        public TownshipFandomVoBuilder time(String time) {
            this.time = time;
            return this;
        }

        public TownshipFandomVoBuilder dealerAvailable(String dealerAvailable) {
            this.dealerAvailable = dealerAvailable;
            return this;
        }

        public TownshipFandomVoBuilder helpGrade(String helpGrade) {
            this.helpGrade = helpGrade;
            return this;
        }

        public TownshipFandomVoBuilder category(String category) {
            this.category = category;
            return this;
        }

        public TownshipFandomVoBuilder funcFromHtmlToBytes(Function<String, byte[]> function) {
            this.imgBytsFunc = function;
            return this;
        }

        public TownshipFandomDataVo build() {
            return new TownshipFandomDataVo(
                    this.category,
                    this.name,
                    this.image,
                    Optional.ofNullable(this.imageBytes),
                    this.materials,
                    this.level,
                    this.cost,
                    this.sellPrice,
                    this.xp,
                    this.time,
                    this.dealerAvailable,
                    this.helpGrade
            );
        }

    }

}
