package zzk.townshipscheduler.backend.scheduling;

import zzk.townshipscheduler.backend.persistence.TownshipProblemEntity;
import zzk.townshipscheduler.backend.scheduling.model.TownshipSchedulingProblem;

import java.io.*;

class ProblemExternalizedProcess {

    private final byte[] bytes;

    public ProblemExternalizedProcess(byte[] bytes) {
        this.bytes = bytes;
    }

    public TownshipSchedulingProblem process() {
        return externalizedProblem();
    }

    private synchronized TownshipSchedulingProblem externalizedProblem() {
        TownshipSchedulingProblem townshipSchedulingProblem = null;
        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(this.bytes);
                ObjectInputStream ois = new ObjectInputStream(bais)
        ) {
            townshipSchedulingProblem = (TownshipSchedulingProblem) ois.readObject();
        }
        catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return townshipSchedulingProblem;
    }

}
