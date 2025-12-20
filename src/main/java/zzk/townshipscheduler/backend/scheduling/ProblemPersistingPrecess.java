package zzk.townshipscheduler.backend.scheduling;

import zzk.townshipscheduler.backend.persistence.TownshipProblemEntity;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

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
            throw new RuntimeException(e);
        }
        return serializedData;
    }


}
