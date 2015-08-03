package ch.usi.da.paxos.api;

public interface LearnerDeliveryMetadata extends Comparable<LearnerDeliveryMetadata> {
   boolean precedes(LearnerDeliveryMetadata other);
}
