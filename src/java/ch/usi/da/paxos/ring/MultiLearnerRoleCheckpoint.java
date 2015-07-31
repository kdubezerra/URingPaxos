package ch.usi.da.paxos.ring;

import java.io.Serializable;
import java.util.Map;

import ch.usi.da.paxos.api.LearnerCheckpoint;

public class MultiLearnerRoleCheckpoint implements LearnerCheckpoint, Serializable {
   private static final long serialVersionUID = 8481644400096733652L;

   long totalValuesDelivered;   
   Map<Integer, LearnerRoleCheckpoint> ringCheckpoints;
   
   public long getTotalDeliveries() {
      return totalValuesDelivered;
   }
   
   public void setTotalDeliveries(long td) {
      totalValuesDelivered = td;
   }
   
   public LearnerRoleCheckpoint getLearnerCheckpoint(int ringId) {
      return ringCheckpoints.get(ringId);
   }
   
   public void setLearnerCheckpoint(int ringId, LearnerRoleCheckpoint cp) {
      ringCheckpoints.put(ringId, cp);
   }
}
