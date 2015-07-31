package ch.usi.da.paxos.ring;

import java.io.Serializable;

import ch.usi.da.paxos.api.LearnerDeliveryMetadata;

public class LearnerRoleDeliveryMetadata implements LearnerDeliveryMetadata, Serializable {
   private static final long serialVersionUID = 4134451730441335846L;
   
   public long instanceId;
   public long instanceValueCount;
   public long ringValueCount;
}
