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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import ch.usi.da.paxos.Util;
import ch.usi.da.paxos.api.PaxosRole;


/**
* DISCLAIMER. We assume that all learners have consecutive IDs, so the first node
* in the ring after the first learner (FNL) that is not a learner will have all
* learners behind it. This is used by the broadcasting learner BL: BL broadcasts
* the message to all other learners and FNL. Also, we assume that each learner in
* a ring is only a learner in that ring, having no other role (e.g., acceptor).
* Finally, we assume that the last acceptor is right before the first learner.<br>
*
* Name: FastNode<br>
* Description: <br>
* 
* Creation date: Feb 26, 2015<br>
* $Id$
* 
* @author Eduardo Bezerra eduardo.bezerra@usi.ch
*/
public class FastNode extends Node {
   
   private final Logger logger = Logger.getLogger(FastNode.class);

   public FastNode(String zoo_host, List<RingDescription> rings) {
      super(zoo_host, rings);
   }

   @Override
   public void start() throws IOException, KeeperException, InterruptedException{
      try {
         int pid = Integer.parseInt((new File("/proc/self")).getCanonicalFile().getName());
         logger.info("PID: " + pid);
      } catch (NumberFormatException | IOException e) {
      }
      // node address
      final InetAddress ip = Util.getHostAddress();
      boolean start_multi_learner = isMultiLearner(rings);
      for(RingDescription ring : rings){
         // ring socket port
         Random rand = new Random();
         int port = 2000 + rand.nextInt(1000); // assign port between 2000-3000
         InetSocketAddress addr = new InetSocketAddress(ip,port);
         // create ring manager
         ZooKeeper zoo = new ZooKeeper(zoo_host,3000,null);
         zoos.add(zoo);
         RingManager rm = new FastRingManager(ring.getRingID(),ring.getNodeID(),addr,zoo,"/ringpaxos");
         ring.setRingManager(rm);
         rm.init();
         // register and start roles
         for(PaxosRole role : ring.getRoles()){
            if(!role.equals(PaxosRole.Learner) || !start_multi_learner){
               if(role.equals(PaxosRole.Proposer)){
                  ProposerRole r = new ProposerRole(rm);
                  logger.debug("Node register role: " + role + " at node " + ring.getNodeID() + " in ring " + ring.getRingID());
                  rm.registerRole(role);
                  ringProposer.put(ring.getRingID(), r);
                  Thread t = new Thread(r);
                  t.setName(role.toString());
                  t.start();
               }else if(role.equals(PaxosRole.Acceptor)){
                  Role r = new AcceptorRole(rm);
                  logger.debug("Node register role: " + role + " at node " + ring.getNodeID() + " in ring " + ring.getRingID());
                  rm.registerRole(role);     
                  Thread t = new Thread(r);
                  t.setName(role.toString());
                  t.start();                 
               }else if(role.equals(PaxosRole.Learner)){
                  LearnerRole r = new LearnerRole(rm);
                  logger.debug("Node register role: " + role + " at node " + ring.getNodeID() + " in ring " + ring.getRingID());
                  rm.registerRole(role);
                  learner = r;
                  Thread t = new Thread(r);
                  t.setName(role.toString());
                  t.start();
               }
            }
         }        
      }
      if(start_multi_learner){ // start only one super learner
         logger.debug("starting a MultiRingLearner");
         MultiLearnerRole mr = new MultiLearnerRole(rings);
         learner = mr;
         Thread t = new Thread(mr);
         t.setName("MultiRingLearner");
         t.start();
      }
      running = true;
   }
   
}
