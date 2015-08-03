package ch.usi.da.paxos.ring;
/* 
 * Copyright (c) 2013 Università della Svizzera italiana (USI)
 * 
 * This file is part of URingPaxos.
 *
 * URingPaxos is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * URingPaxos is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with URingPaxos.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.log4j.Logger;

import ch.usi.da.paxos.api.ConfigKey;
import ch.usi.da.paxos.api.Learner;
import ch.usi.da.paxos.api.LearnerCheckpoint;
import ch.usi.da.paxos.api.LearnerDeliveryMetadata;
import ch.usi.da.paxos.api.PaxosRole;
import ch.usi.da.paxos.message.Message;
import ch.usi.da.paxos.message.MessageType;
import ch.usi.da.paxos.message.Value;
import ch.usi.da.paxos.storage.Decision;

/**
 * Name: MultiLearnerRole<br>
 * Description: <br>
 * 
 * Creation date: Mar 04, 2013<br>
 * $Id$
 * 
 * @author Samuel Benz benz@geoid.ch
 */
public class MultiLearnerRole extends Role implements Learner {

   private final static Logger logger = Logger.getLogger(MultiLearnerRole.class);
   
   private final Map<Integer,RingDescription> ringmap = new HashMap<Integer,RingDescription>();
   
   private final List<Integer> ring = new ArrayList<Integer>();
   
   private final int maxRing = 20;
   
   private final BlockingQueue<Decision> values = new LinkedBlockingQueue<Decision>(); 
   
   private final LearnerRole[] learner = new LearnerRole[maxRing];
   
   private int M = 1;
      
   private int deliverRing;
   
   private int referenceRing = 0;

   private final long[] skip_count = new long[maxRing];
   
   private final long[] latency = new long[maxRing];
   
   private boolean deliver_skip_messages = false;
   
   private long multiring_delivered_values = 0;
   private boolean waitingForCheckpoint = true;
   private Semaphore sem_waitingForCheckpoint = new Semaphore(0);
   private Semaphore sem_learnersReady = new Semaphore(0);

   /**
    * @param rings a list of rings
    */
   public MultiLearnerRole(List<RingDescription> rings) {
      int minRing = maxRing+1;
      for(RingDescription ring : rings){
         if(ring.getRingID() < minRing){
            minRing = ring.getRingID();
         }
         this.ring.add(ring.getRingID());
         this.ringmap.put(ring.getRingID(),ring);
      }
      Collections.sort(ring);
      RingManager firstRing = rings.get(0).getRingManager();
      deliverRing = minRing;
      logger.debug("MultiRingLearner initial deliverRing=" + deliverRing);
      if(firstRing.getConfiguration().containsKey(ConfigKey.multi_ring_m)){
         M = Integer.parseInt(firstRing.getConfiguration().get(ConfigKey.multi_ring_m));
         logger.info("MultiRingLearner M=" + M);
      }
      if(firstRing.getConfiguration().containsKey(ConfigKey.deliver_skip_messages)){
         if(firstRing.getConfiguration().get(ConfigKey.deliver_skip_messages).contains("1")){
            deliver_skip_messages = true;
         }
         logger.info("MultiRingLearner deliver_skip_messages: " + (deliver_skip_messages ? "enabled" : "disabled"));
      }
      if(firstRing.getConfiguration().containsKey(ConfigKey.reference_ring)){
         referenceRing = Integer.parseInt(firstRing.getConfiguration().get(ConfigKey.reference_ring));
         logger.info("MultiRingLearner reference ring=" + referenceRing);
      }
   }

   @Override
   public void run() {
      CountDownLatch latch = new CountDownLatch(ringmap.size());
      for(Entry<Integer,RingDescription> e : ringmap.entrySet()){
         // create learners
         RingManager ring = e.getValue().getRingManager();
         Role r = new LearnerRole(ring, latch);
         learner[e.getKey()] = (LearnerRole) r;
         logger.debug("MultiRingLeaner register role: " + PaxosRole.Learner + " at node " + ring.getNodeID() + " in ring " + ring.getRingID());
         ring.registerRole(PaxosRole.Learner);     
         Thread t = new Thread(r);
         t.setName(PaxosRole.Learner + "-" + e.getKey());
         t.start();
         skip_count[e.getKey()] = 0;
      }
      try {
         latch.await(); // wait until all learner are ready
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }
      
      signalAllLearnersReady();
      
      waitForCheckpoint();
      
      while(true){
         
         long next_multiring_value_count = ++multiring_delivered_values;
         deliverRing = getRingForDelivery(next_multiring_value_count);
         Decision d = learner[deliverRing].getNextDecision();
         if (d.isSkip()) {
            continue;
         }
         else {
            d.setDeliveryMetadata(getLastDeliveryMetadata());
            values.add(d);
         }
         
      }
   }
	
	void updateLatencies(MutableInt heartbeat) {
      if(learner[referenceRing] != null && deliverRing != referenceRing){
         int ref_lat = learner[referenceRing].latency_to_coordinator;
         int lat = learner[deliverRing].latency_to_coordinator;
         int diff = ref_lat - lat;
         heartbeat.increment();
         if(Math.abs(lat - latency[deliverRing]) > 20 || heartbeat.compareTo(500) > 0){
            heartbeat.setValue(0);
            RingManager rm = ringmap.get(deliverRing).getRingManager();
            Message m = new Message(0,rm.getNodeID(),PaxosRole.Leader,MessageType.Latency,0,0,new Value("LAT",Integer.toString(diff).getBytes()));
            if(rm.isNodeCoordinator()){
               rm.getNetwork().getLeader().deliver(rm, m);
            }else{
               rm.getNetwork().send(m);
            }
            logger.info("MultiRingLearner latency between coordinator/coordinator in ring " + referenceRing + "/" + deliverRing + " is " + diff + " ms");
         }
      }
      latency[deliverRing] = learner[deliverRing].latency_to_coordinator;
	}

	public int getRingSuccessor(int id){
		int pos = ring.indexOf(new Integer(id));
		if(pos+1 >= ring.size()){
			return ring.get(0);
		}else{
			return ring.get(pos+1);
		}
	}
	
   int getRingForDelivery(long deliveryIndex) {
      int ringIndexInList = (int) (((deliveryIndex - 1) % (M * ring.size())) / M);
      return ring.get(ringIndexInList);
   }

	@Override
	public BlockingQueue<Decision> getDecisions() {
		return values;
	}

	@Override
	public void setSafeInstance(Integer ring, Long instance) {
		learner[ring].setSafeInstance(ring,instance);
	}
	
	@Override
	public void setSafeDelivery(LearnerDeliveryMetadata metadata) {
	   MultiLearnerRoleDeliveryMetadata md = (MultiLearnerRoleDeliveryMetadata) metadata;
      for(int ringId : ringmap.keySet()){
         learner[ringId].setSafeDelivery(md.getDelivery(ringId));
      }
	}

   @Override
   public void provideLearnerCheckpoint(LearnerCheckpoint cp) {
      
      System.out.println("Waiting for all single-ring LearnerRole objects to be ready to apply checkpoint...");
      waitForAllLearnersReady();
      System.out.println("All single-ring LearnerRole objects ready. Applying checkpoint...");
      
      // 1: install the checkpoint
      if (cp == null) {
         for(int ringId : ringmap.keySet()){
            learner[ringId].provideLearnerCheckpoint(null);
         }
      }
      else {
         MultiLearnerRoleCheckpoint checkpoint = (MultiLearnerRoleCheckpoint) cp;
         multiring_delivered_values = checkpoint.getTotalDeliveries();
         for(int ringId : ringmap.keySet()) {
            learner[ringId].provideLearnerCheckpoint(checkpoint.getLearnerCheckpoint(ringId));
         }
      }
      
      // 2: signal that the checkpoint was provided (and assume that the checkpoint was recent enough)
      signalCheckpointReceived();
   }
   
   private void waitForCheckpoint() {
      System.out.println("MultiLearnerRole :: waiting for checkpoint...");
      while (waitingForCheckpoint) {
         sem_waitingForCheckpoint.acquireUninterruptibly();
      }
      System.out.println("MultiLearnerRole :: got checkpoint!");
   }
   
   private void signalCheckpointReceived() {
      waitingForCheckpoint = false;
      sem_waitingForCheckpoint.release();
   }
   
   private void waitForAllLearnersReady() {
      sem_learnersReady.acquireUninterruptibly();
   }
   
   private void signalAllLearnersReady() {
      sem_learnersReady.release();
   }

   @Override
   public Decision getNextDecision() {
      Decision d = null;
      try {
         d = values.take();
      } catch (InterruptedException e) {
         e.printStackTrace();
         System.exit(1);
      }
      return d;
   }

   @Override
   public LearnerCheckpoint createCheckpointObject(LearnerDeliveryMetadata metadata) {
      MultiLearnerRoleDeliveryMetadata md = (MultiLearnerRoleDeliveryMetadata) metadata;
      
      MultiLearnerRoleCheckpoint cp = new MultiLearnerRoleCheckpoint();
      cp.setTotalDeliveries(md.getTotalDeliveries());
      for(int ringId : ringmap.keySet())
         cp.setLearnerCheckpoint(ringId, (LearnerRoleCheckpoint) learner[ringId].createCheckpointObject(md.getDelivery(ringId)));
      return cp;
   }

   @Override
   public LearnerDeliveryMetadata getLastDeliveryMetadata() {
      MultiLearnerRoleDeliveryMetadata dm = new MultiLearnerRoleDeliveryMetadata();
      dm.setTotalDeliveries(multiring_delivered_values);
      for(int ringId : ringmap.keySet())
         dm.setDelivery(ringId, (LearnerRoleDeliveryMetadata) learner[ringId].getLastDeliveryMetadata());
      return dm;
   }

}
