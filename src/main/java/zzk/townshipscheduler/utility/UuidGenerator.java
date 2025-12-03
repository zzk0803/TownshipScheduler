package zzk.townshipscheduler.utility;

import com.fasterxml.uuid.Generators;

import java.util.UUID;

public class UuidGenerator {

    private UuidGenerator() {

    }

    public static UUID random() {
        return Generators.randomBasedGenerator().generate();
    }

    public static UUID timeOrderedV6() {
        return Generators.timeBasedReorderedGenerator().generate();
    }

    public static UUID timeOrderedV7() {
        return Generators.timeBasedEpochGenerator().generate();
    }

}
