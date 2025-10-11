package zzk.townshipscheduler.utility;

import com.fasterxml.uuid.Generators;

import java.util.UUID;

public class UuidGenerator {

    private UuidGenerator() {

    }

    // 生成随机 UUID（v4）
    public static UUID random() {
        return Generators.randomBasedGenerator().generate();
    }

    // 生成时间排序 UUID（v6）
    public static UUID timeOrderedV6() {
        return Generators.timeBasedReorderedGenerator().generate();
    }

    // 生成时间排序 UUID（v7）
    public static UUID timeOrderedV7() {
        return Generators.timeBasedEpochGenerator().generate();
    }

}
