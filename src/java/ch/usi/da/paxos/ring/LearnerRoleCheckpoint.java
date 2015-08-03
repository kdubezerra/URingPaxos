package ch.usi.da.paxos.ring;

import java.io.Serializable;

import ch.usi.da.paxos.api.LearnerCheckpoint;
import ch.usi.da.paxos.api.LearnerDeliveryMetadata;

public class LearnerRoleCheckpoint implements LearnerCheckpoint, Serializable {
   private static final long serialVersionUID = -3608411593761948683L;
   
   LearnerRoleDeliveryMetadata deliveryMetadata;
   
   public LearnerRoleCheckpoint(LearnerRoleDeliveryMetadata lrdm) {
      this.deliveryMetadata = lrdm;
   }
   
   @Override
   public LearnerDeliveryMetadata getLearnerDeliveryMetadata() {
      return deliveryMetadata;
   }
   
}
