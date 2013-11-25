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

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;

import org.apache.log4j.Logger;

import ch.usi.da.paxos.message.Message;

/**
 * Name: TCPSender<br>
 * Description: <br>
 * 
 * Creation date: Apr 3, 2013<br>
 * $Id$
 * 
 * @author Samuel Benz <benz@geoid.ch>
 */
public class TCPSender implements Runnable {

	private final static Logger logger = Logger.getLogger(TCPSender.class);
	
	private final NetworkManager manager;
	
	private final SocketChannel client;
	
	private final TransferQueue<Message> send_queue;
	
	private final ByteBuffer buffer = ByteBuffer.allocate(65536);
	
	/**
	 * @param manager
	 * @throws IOException 
	 */
	public TCPSender(NetworkManager manager,SocketChannel socket,TransferQueue<Message> queue) throws IOException{
		this.manager = manager;
		this.client = socket;
		this.send_queue = queue;
	}
	
	@Override
	public void run() {
		Message m = null;
		while(client.isConnected()){
			try {
				m = send_queue.poll(1,TimeUnit.SECONDS);
				if(m != null){
					int length = Message.length(m);
					int necessary_length = 4 + length; // size + payload
					if (manager.crc_32) necessary_length += 8; // long crc32
					if(buffer.remaining() >= necessary_length){
//						buffer.putInt(NetworkManager.MAGIC_NUMBER);
						
					   
					   buffer.putInt(length);
						
						Message.toBuffer(buffer, m);
						
//                  System.out.println(String.format("Message.length(m) = %d, buffer.position() = %d", length, buffer.position()));						
						
						if(manager.crc_32){
							buffer.putLong(Message.getCRC32(m));
//							System.out.println("put the crc 32 in the message buffer");
						}
						buffer.flip();
						while(buffer.remaining() > 0) {
						   int sentbytes = client.write(buffer); // client runs in blocking mode !
//						   System.out.println(String.format("sent %d bytes", sentbytes));
						}
						buffer.clear();
						manager.send_count++;
						manager.send_bytes = manager.send_bytes + length;
					}else{
						logger.error("TCPSender buffer too small!");
						send_queue.add(m);
					}
				}
			} catch (SocketException | ClosedChannelException | CancelledKeyException e ) {
				if(m != null){ // put back if already closed
					send_queue.add(m);
				}
			} catch (IOException e) {
				logger.error("TCPSender send error",e);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
	}
}
