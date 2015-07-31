package ch.usi.da.paxos.ring;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import ch.usi.da.paxos.api.LearnerDeliveryMetadata;

public class MultiLearnerRoleDeliveryMetadata implements LearnerDeliveryMetadata, Serializable {
   private static final long serialVersionUID = -4990900306709619910L;
   
   long totalDeliveriesMade;
   Map<Integer, LearnerRoleDeliveryMetadata> individualRingsMetadata = new HashMap<Integer, LearnerRoleDeliveryMetadata>();;

   public long getTotalDeliveries() {
      return totalDeliveriesMade;
   }
   
   public void setTotalDeliveries(long val) {
      totalDeliveriesMade = val;
   }

   public LearnerRoleDeliveryMetadata getDelivery(int ring) {
      return individualRingsMetadata.get(ring);
   }
   
   public void setDelivery(int ring, LearnerRoleDeliveryMetadata ringmd) {
      individualRingsMetadata.put(ring, ringmd);
   }

   @Override
   public int compareTo(LearnerDeliveryMetadata o) {
      MultiLearnerRoleDeliveryMetadata other = (MultiLearnerRoleDeliveryMetadata) o;
      Long thisDels  = totalDeliveriesMade;
      Long otherDels = other.totalDeliveriesMade;
      return thisDels.compareTo(otherDels);
   }
   
   @Override
   public boolean equals(Object o) {
      MultiLearnerRoleDeliveryMetadata other = (MultiLearnerRoleDeliveryMetadata) o;
      return this.compareTo(other) == 0;
   }
   
   @Override
   public int hashCode() {
      int hash = 0;
      for (LearnerRoleDeliveryMetadata lrmd : individualRingsMetadata.values())
         hash ^= lrmd.hashCode();
      return hash;
   }

}
