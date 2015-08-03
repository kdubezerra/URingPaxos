package ch.usi.da.paxos.ring;

import java.io.Serializable;

import ch.usi.da.paxos.api.LearnerCheckpoint;
import ch.usi.da.paxos.api.LearnerDeliveryMetadata;

public class MultiLearnerRoleCheckpoint implements LearnerCheckpoint, Serializable {
   private static final long serialVersionUID = 8481644400096733652L;

   MultiLearnerRoleDeliveryMetadata multiLearnerRoleDeliveryMetadata;

   public MultiLearnerRoleCheckpoint() {
   }
   
   public MultiLearnerRoleCheckpoint(MultiLearnerRoleDeliveryMetadata mlrdm) {
      this.multiLearnerRoleDeliveryMetadata = mlrdm;
   }
   
   public long getTotalDeliveries() {
      return multiLearnerRoleDeliveryMetadata.totalDeliveriesMade;
   }

   @Override
   public LearnerDeliveryMetadata getLearnerDeliveryMetadata() {
      return multiLearnerRoleDeliveryMetadata;
   }
   
}
