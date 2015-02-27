package ch.usi.da.paxos.ring;
/* 
 * Copyright (c) 2015 Università della Svizzera italiana (USI)
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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;


/**
* DISCLAIMER. We assume that all learners have consecutive IDs, so the first node
* in the ring after the first learner (FNL) that is not a learner will have all
* learners behind it. This is used by the broadcasting learner BL: BL broadcasts
* the message to all other learners and FNL. Also, we assume that each learner in
* a ring is only a learner in that ring, having no other role (e.g., acceptor).
* Finally, we assume that the last acceptor is right before the first learner.<br>
*
* Name: FastRingManager<br>
* Description: <br>
* 
* Creation date: Feb 26, 2015<br>
* $Id$
* 
* @author Eduardo Bezerra eduardo.bezerra@usi.ch
*/
public class FastRingManager extends RingManager {

   private final static Logger logger = Logger.getLogger(FastRingManager.class);
   
   List<Integer> previousLearners;
   private int successorOfAllLearners;
   
   public FastRingManager(int ringID, int nodeID, InetSocketAddress addr, ZooKeeper zoo) {
      super(ringID, nodeID, addr, zoo);
      previousLearners = new ArrayList<Integer>();
   }
   
   public FastRingManager(int ringID, int nodeID, InetSocketAddress addr, ZooKeeper zoo, String prefix) {
      super(ringID, nodeID, addr, zoo, prefix);
      previousLearners = new ArrayList<Integer>();
   }

   /**
    * Init the fast ring manger
    * 
    * (we need this init() because of the "this" references) 
    * 
    * @throws IOException
    * @throws KeeperException
    * @throws InterruptedException
    */
   @Override
   public void init() throws IOException, KeeperException, InterruptedException {
      network = new FastNetworkManager(this);
      zoo.register(this);
      registerNode();
      network.startServer();
   }
   
   @Override
   protected synchronized void notifyRingChanged(){
      // traditional ring-paxos successos
      InetSocketAddress successorAddr = getNodeAddress(getRingSuccessor(nodeID));
      logger.info("FastRingManager ring " + topologyID + " changed: " + nodes + " (successor: " + getRingSuccessor(nodeID) + " at " + successorAddr + ")");

      if(successorAddr != null && currentConnection == null || !currentConnection.equals(successorAddr)){
         /* give node time to start (zookeeper is fast!) */
         try { Thread.sleep(1000); } catch (InterruptedException e) { }
         network.disconnectClient();
         network.connectClient(successorAddr);
         currentConnection = successorAddr;
      }

   }
   
   @Override
   protected synchronized void notifyLearnersChanged() {
      System.out.println("Called FastRingManager.notifyLearnersChanged()");
      logger.info("FastRingManager ring " + topologyID + "'s new learners: " + learners);
      FastNetworkManager fnetwork = (FastNetworkManager) network;

      /* give learners time to start (zookeeper is fast!) */
      try { Thread.sleep(1000); } catch (InterruptedException e) { }

      // update connections to all learners
      Set<Integer> newLearnerSet = new HashSet<Integer>(learners);
      Set<Integer> learnersRemoved = new HashSet<Integer>(previousLearners);
      learnersRemoved.removeAll(newLearnerSet);
      previousLearners.clear();
      previousLearners.addAll(newLearnerSet);
      Collections.sort(previousLearners);

      for (int removedLearnerId : learnersRemoved)
         if (removedLearnerId != nodeID)
            fnetwork.removeLearnerConnection(removedLearnerId);

      for (int learnerId : learners)
         if (learnerId != nodeID)
            fnetwork.ensureLearnerConnection(learnerId, getNodeAddress(learnerId));

      // create special connection to the node that succeeds all learners
      // (making sure that the local node is a learner and that there is at
      // least one non-learner)
      if (localNodeIsLearner()) {
         successorOfAllLearners = getRingSuccessor(getLastLearner());
         System.out.println("Local node " + nodeID + " is a learner. Successor of all learners is node " + successorOfAllLearners);
         logger.info("Local node " + nodeID + " is a learner. Successor of all learners is node " + successorOfAllLearners);
         if (learners.contains(successorOfAllLearners) == false) {
            logger.info("Local learner " + nodeID + " connecting to learnersSuccessor " + successorOfAllLearners);
            fnetwork.ensureLearnersSuccessorConnection(successorOfAllLearners, getNodeAddress(successorOfAllLearners));
         } else {
            logger.info("Local node " + nodeID + " thinks successor " + successorOfAllLearners + " is a learner too.");
         }
      }
   }
   
   public boolean localNodeIsAcceptor() {
      return acceptors.contains(nodeID);
   }
   
   public boolean localNodeIsLastAcceptor() {
      return nodeID == last_acceptor;
   }
   
   public boolean localNodeIsLearner() {
      return learners.contains(nodeID);
   }
   
   public boolean hasLearners() {
      return !learners.isEmpty();
   }
   
   public int getLastLearner() {
      return learners.get(learners.size() - 1);
   }
   
   public int getBroadcasterLearnerId(long instanceId) {
      int bcasterIndex = (int) (instanceId % (long) learners.size());
      int bcasterId    = learners.get(bcasterIndex);
      return bcasterId;
   }
   
}
