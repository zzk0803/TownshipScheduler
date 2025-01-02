package zzk.townshipscheduler.backend.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

import java.io.Serializable;

/**
 * DTO for {@link WikiCrawledEntity}
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikiCrawledEntityDtoForImage implements Serializable {

    private static final long serialVersionUID = 2172076167210788081L;

    private final byte[] imageBytes;

}
