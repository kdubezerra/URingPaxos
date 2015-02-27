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
import java.net.StandardSocketOptions;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

import org.apache.log4j.Logger;

import ch.usi.da.paxos.message.Message;


/**
* DISCLAIMER. We assume that all learners have consecutive IDs, so the first node
* in the ring after the first learner (FNL) that is not a learner will have all
* learners behind it. This is used by the broadcasting learner BL: BL broadcasts
* the message to all other learners and FNL. Also, we assume that each learner in
* a ring is only a learner in that ring, having no other role (e.g., acceptor).
* Finally, we assume that the last acceptor is right before the first learner.<br>
*
* Name: FastNetworkManager<br>
* Description: <br>
* 
* Creation date: Feb 26, 2015<br>
* $Id$
* 
* @author Eduardo Bezerra eduardo.bezerra@usi.ch
*/
public class FastNetworkManager extends NetworkManager {
   
   public static class ConnectionInfo {
      public SocketChannel channel;
      public TransferQueue<Message> send_queue;
      public ConnectionInfo(SocketChannel ch, TransferQueue<Message> q) {
         channel    = ch;
         send_queue = q;
      }
   }
   
   protected final static Logger logger = Logger.getLogger(FastNetworkManager.class);

   private Map<Integer, ConnectionInfo> learnersOutwardConnections;
   ConnectionInfo learnersSuccessorConnection;
   
   public FastNetworkManager(RingManager ring) throws IOException {
      super(ring);
      learnersOutwardConnections = new ConcurrentHashMap<Integer, ConnectionInfo>();
      learnersSuccessorConnection = null;
   }
   
   /**
    * @param m the message to send
    */
   @Override
   public void send(Message m){
      try {

         FastRingManager fring = (FastRingManager) ring;
         final int  nodeId     = fring.getNodeID();
         final long instanceId = m.getInstance();
         
         // * if you are the last acceptor (and there are learners), rotate the send among all learners
         // * if you are a learner and you're the broadcasting learner for that instanceId, then send
         //   the message to all other learners AND the first non-learner after all learners (i.e., 
         //   all learners' successor)
         // * otherwise, just follow the standard ring-paxos protocol

         if (fring.localNodeIsLastAcceptor() && fring.hasLearners()) {
            // get the broadcasting learner, rotating the instance id among the learners
            int bcasterLearnerId = fring.getBroadcasterLearnerId(instanceId);
            ConnectionInfo bcasterLearnerConnection = learnersOutwardConnections.get(bcasterLearnerId);
            logger.info(String.format("FastNetworkManager last acceptor sending to broadcast-learner %d: %s", bcasterLearnerId, m));
            bcasterLearnerConnection.send_queue.transfer(m);
         }
         else if (fring.localNodeIsLearner()) {
            // if this is the bcasting learner, bcast to learners (except itself) and learnersSuccessor
            // otherwise, do nothing
            if (nodeId == fring.getBroadcasterLearnerId(instanceId)) {
               for (int learnerId : fring.getLearners()) {
                  if (learnerId != nodeId) {
                     logger.info(String.format("FastNetworkManager learner %d broadcasting to learner %d: %s", nodeId, learnerId, m));
                     ConnectionInfo learnerConnection = learnersOutwardConnections.get(learnerId);
                     learnerConnection.send_queue.transfer(m);
                  }
               }
               if (learnersSuccessorConnection != null)
                     learnersSuccessorConnection.send_queue.transfer(m);
            }
         }
         else {
            // this not a learner, nor the last acceptor, so follow standard protocol
            send_queue.transfer(m); // (blocking call)
         }
      } catch (InterruptedException e) {
      }
   }
   
   /**
    * connect to the ring successor
    * 
    * @param addr
    */
   @Override
   public void connectClient(InetSocketAddress addr){
      client = createConnection(addr, send_queue).channel;
   }
   
   public void removeLearnerConnection(int learnerId) {
      try {
         ConnectionInfo learnerConnection = learnersOutwardConnections.remove(learnerId);
         if (learnerConnection != null) {
            learnerConnection.channel.close();
            learnerConnection.send_queue.clear();
            logger.info("FastNetworkManager closed connection to learner " + learnerId);
         } else {
            logger.info("FastNetworkManager couldn't close connection to learner " + learnerId + " (not found in connections map).");
         }
      } catch (IOException e) {
         e.printStackTrace();
         System.err.println("FastNetworkManager couldn't close connection to learner " + learnerId);
      }
   }
   
   public void ensureLearnerConnection(int learnerId, InetSocketAddress learnerAddress) {
      try {
         ConnectionInfo oldConnection = learnersOutwardConnections.get(learnerId);
         if (oldConnection != null && oldConnection.channel.getRemoteAddress() != learnerAddress) {
            learnersOutwardConnections.remove(learnerId);
            oldConnection.channel.close();
            oldConnection = null;
         }
         if (oldConnection == null) {
            ConnectionInfo newConnection = createConnection(learnerAddress);
            learnersOutwardConnections.put(learnerId, newConnection);
         }
      } catch (IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
   }
   
   public void ensureLearnersSuccessorConnection(int lsId, InetSocketAddress lsAddress) {
      try {
         if (learnersSuccessorConnection != null && learnersSuccessorConnection.channel.getRemoteAddress() != lsAddress) {
            learnersSuccessorConnection.channel.close();
            learnersSuccessorConnection.send_queue.clear();
            learnersSuccessorConnection = null;
         }
         if (learnersSuccessorConnection == null) {
            learnersSuccessorConnection = createConnection(lsAddress);
         }
      } catch (IOException e) {
         e.printStackTrace();
         System.exit(1);
      }
   }
   
   public ConnectionInfo createConnection(InetSocketAddress addr) {
      return createConnection(addr, new LinkedTransferQueue<Message>());
   }
   
   public ConnectionInfo createConnection(InetSocketAddress addr, TransferQueue<Message> sendQueue) {
      SocketChannel newChannel = null;
      try {
         newChannel = SocketChannel.open();
         newChannel.setOption(StandardSocketOptions.SO_SNDBUF,buf_size);
         newChannel.setOption(StandardSocketOptions.SO_RCVBUF,buf_size);       
         newChannel.socket().setSendBufferSize(buf_size);
         newChannel.configureBlocking(true); // Client runs in Blocking Mode !!!
         newChannel.connect(addr);
         newChannel.setOption(StandardSocketOptions.TCP_NODELAY,tcp_nodelay);
         Thread t = new Thread(new TCPSender(this,newChannel,sendQueue));
         t.setName("TCPSender");
         t.start();
         logger.info("FastNetworkManager created connection " + addr + " (" + newChannel.getLocalAddress() + ")");
      } catch (IOException e) {
         logger.error("FastNetworkManager client connect error",e);
      }
      return new ConnectionInfo(newChannel, sendQueue);
   }
   
}
