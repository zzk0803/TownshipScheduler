package zzk.townshipscheduler.adopting.vo;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.Optional;

@Value
@Builder
public class GoodsVo
        implements Serializable {

    private String name;

    private String category;

    private String bomString;

    private String durationStrings;

    private String level;

    private Optional<byte[]> imageBytes;

}
