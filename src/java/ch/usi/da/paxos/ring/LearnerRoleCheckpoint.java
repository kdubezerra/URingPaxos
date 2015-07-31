package ch.usi.da.paxos.ring;

import java.io.Serializable;

import ch.usi.da.paxos.api.LearnerCheckpoint;

public class LearnerRoleCheckpoint implements LearnerCheckpoint, Serializable {
   private static final long serialVersionUID = -3608411593761948683L;
   
   public long checkpointed_instanceId;
   public long checkpointed_instance_value_count;
   public long checkpointed_ring_value_count;
}
