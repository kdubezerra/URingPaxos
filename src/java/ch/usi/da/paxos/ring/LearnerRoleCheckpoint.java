package ch.usi.da.paxos.ring;

import java.io.Serializable;

import ch.usi.da.paxos.api.LearnerCheckpoint;

public class LearnerRoleCheckpoint implements LearnerCheckpoint, Serializable {
   private static final long serialVersionUID = -3608411593761948683L;
   
   private LearnerRoleDeliveryMetadata deliveryMetadata;
   
   public LearnerRoleCheckpoint() {
   }
   
   public LearnerRoleCheckpoint(LearnerRoleDeliveryMetadata lrdm) {
      this.deliveryMetadata = lrdm;
   }
   
   public LearnerRoleDeliveryMetadata getDeliveryMetadata() {
      return deliveryMetadata;
   }
   
   public void setDeliveryMetadata(LearnerRoleDeliveryMetadata lrdm) {
      this.deliveryMetadata = lrdm;
   }
   
}
