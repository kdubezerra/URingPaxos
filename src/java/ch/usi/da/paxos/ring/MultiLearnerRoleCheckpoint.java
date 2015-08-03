package ch.usi.da.paxos.ring;

import java.io.Serializable;

import ch.usi.da.paxos.api.LearnerCheckpoint;

public class MultiLearnerRoleCheckpoint implements LearnerCheckpoint, Serializable {
   private static final long serialVersionUID = 8481644400096733652L;

   private MultiLearnerRoleDeliveryMetadata deliveryMetadata;

   public MultiLearnerRoleCheckpoint() {
   }
   
   public MultiLearnerRoleCheckpoint(MultiLearnerRoleDeliveryMetadata deliveryMetadata) {
      this.deliveryMetadata = deliveryMetadata;
   }
   
   public MultiLearnerRoleDeliveryMetadata getDeliveryMetadata() {
      return deliveryMetadata;
   }

   public void setDeliveryMetadata(MultiLearnerRoleDeliveryMetadata deliveryMetadata) {
      this.deliveryMetadata = deliveryMetadata;
   }
   
}
