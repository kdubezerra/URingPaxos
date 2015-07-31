package ch.usi.da.paxos.ring;

import java.io.Serializable;

import ch.usi.da.paxos.api.LearnerDeliveryMetadata;

public class LearnerRoleDeliveryMetadata implements LearnerDeliveryMetadata, Serializable {
   private static final long serialVersionUID = 4134451730441335846L;
   
   public long instanceId;
   public long instanceValueCount;
   public long ringValueCount;
   
   @Override
   public int compareTo(LearnerDeliveryMetadata o) {
      LearnerRoleDeliveryMetadata other = (LearnerRoleDeliveryMetadata) o;
      Long thisRingVals  = ringValueCount;
      Long otherRingVals = other.ringValueCount;
      return thisRingVals.compareTo(otherRingVals);
   }
   
   @Override
   public boolean equals(Object o) {
      LearnerRoleDeliveryMetadata other = (LearnerRoleDeliveryMetadata) o;
      return this.compareTo(other) == 0;
   }
   
   @Override
   public int hashCode() {
      return (int) (instanceId ^ instanceValueCount ^ ringValueCount);
   }
}
