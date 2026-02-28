package zzk.townshipscheduler.backend.scheduling;

import lombok.extern.slf4j.Slf4j;
import zzk.townshipscheduler.backend.persistence.TownshipProblemEntity;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

@Slf4j
class ProblemPersistingPrecess {

    private final TownshipSchedulingProblem townshipSchedulingProblem;

    public ProblemPersistingPrecess(
            TownshipSchedulingProblem townshipSchedulingProblem
    ) {
        this.townshipSchedulingProblem = townshipSchedulingProblem;
    }

    public synchronized TownshipProblemEntity process() {

        TownshipProblemEntity townshipProblemEntity = new TownshipProblemEntity();
        townshipProblemEntity.setUuid(this.townshipSchedulingProblem.getUuid());
        townshipProblemEntity.setProblemSerialized(serializeProblem());

        return townshipProblemEntity;

    }

    private byte[] serializeProblem() {
        byte[] serializedData = null;
        try (
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)
        ) {

            oos.writeObject(this.townshipSchedulingProblem);
            oos.flush();
            serializedData = bos.toByteArray();
        }
        catch (IOException e) {
            log.error("can't persist problem {}",this.townshipSchedulingProblem.getUuid());
        }
        return serializedData;
    }


}
