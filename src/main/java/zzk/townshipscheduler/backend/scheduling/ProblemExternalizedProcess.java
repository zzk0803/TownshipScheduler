package zzk.townshipscheduler.backend.scheduling;

import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.persistence.TownshipProblemEntity;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

@Slf4j
class ProblemExternalizedProcess {

    private final byte[] bytes;

    private String uuid;

    public ProblemExternalizedProcess(TownshipProblemEntity townshipProblemEntity) {
        this(townshipProblemEntity.getProblemSerialized());
        this.uuid = townshipProblemEntity.getUuid();
    }

    public ProblemExternalizedProcess(byte[] bytes) {
        this.bytes = bytes;
    }

    public synchronized TownshipSchedulingProblem process() {
        TownshipSchedulingProblem townshipSchedulingProblem = null;
        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(this.bytes);
                ObjectInputStream ois = new ObjectInputStream(bais)
        ) {
            townshipSchedulingProblem = (TownshipSchedulingProblem) ois.readObject();
        }
        catch (IOException | ClassNotFoundException e) {
            log.error("can't externalized a problem,so it has been passed");
            if (uuid != null) {
                log.error("problem id is {}", this.uuid);
            }
        }
        return townshipSchedulingProblem;
    }

}
