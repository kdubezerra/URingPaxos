package ch.usi.da.paxos;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ch.usi.da.paxos.message.Message;
import ch.usi.da.paxos.message.PaxosRole;
import ch.usi.da.paxos.message.MessageType;
import ch.usi.da.paxos.message.Value;

/**
 * Name: LearnerListener<br>
 * Description: <br>
 * 
 * Creation date: Apr 9, 2012<br>
 * $Id$
 * 
 * @author benz@geoid.ch
 */
public class LearnerListener implements Runnable {

	private final Learner learner;
	
	private final DatagramChannel channel;
	
	private ByteBuffer buffer = ByteBuffer.allocate(8192);
	
	private final List<DatagramPacket> out = new ArrayList<DatagramPacket>();
	
	private Selector selector;
	
	/**
	 * Public constructor
	 * 
	 * @param learner
	 * @throws IOException 
	 */
	public LearnerListener(Learner learner) throws IOException{
		this.learner = learner;
		this.channel = learner.getChannel();
		selector = Selector.open();
	}
	
	@Override
	public void run() {
		try{
			channel.register(selector, SelectionKey.OP_READ);
			while (selector.isOpen()){
				selector.select(2000);
				Integer request = learner.getRequests().poll();
				if(request != null){
					Message m = new Message(request,learner.getID(),PaxosRole.Acceptor,MessageType.Accept,new Integer(9999),new Value(System.currentTimeMillis()+ "" + learner.getID(),new byte[0]));
					byte[] b = Message.toWire(m);
					DatagramPacket packet = new DatagramPacket(b,b.length,Configuration.getGroup(m.getReceiver()));
					out.add(packet);
					channel.register(selector,SelectionKey.OP_READ|SelectionKey.OP_WRITE);
				}
				Set<SelectionKey> keys = selector.selectedKeys();
				synchronized (keys){
					Iterator<SelectionKey> it = keys.iterator();
					while (it.hasNext()){
						SelectionKey key = (SelectionKey)it.next();
						it.remove();
						if (!key.isValid())
							continue;
						if (key.isReadable()){
							read(key);
						}
						if (key.isWritable()){
							write(key);
						}
					}
				}
			}
			selector.close();
			channel.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	private void read(SelectionKey key){
		DatagramChannel channel = (DatagramChannel)key.channel();
		try{
			buffer.clear();
			SocketAddress address = channel.receive(buffer);
			if (address == null)
				return;
			buffer.flip();
			int	count = buffer.remaining();
			if (count > 0){
				byte[] bytes = new byte[count];
				buffer.get(bytes);
				DatagramPacket in = new DatagramPacket(bytes, count, address);
				Message m = Message.fromWire(in.getData());
				if(m != null){
					// learn a value
					Majority maj;
					synchronized(learner.getInstanceList()){
						if(!learner.getInstanceList().containsKey(m.getInstance())){
							maj = new Majority();
							learner.getInstanceList().put(m.getInstance(),maj);
						}else{
							maj = learner.getInstanceList().get(m.getInstance());
						}
					}
					if(maj != null){
						synchronized (maj){
							maj.addMessage(m);
							if(maj.isQuorum()){
								learner.getDecisions().add(maj.getMajorityDecision());
								learner.getInstanceList().remove(m.getInstance());
							}
						}
					}
				}
			}
			selector.wakeup();
		}catch (IOException e){
			e.printStackTrace();
		}
	}
	
	private void write(SelectionKey key){
		DatagramChannel channel = (DatagramChannel)key.channel();
		try {
			while (!out.isEmpty()){
				DatagramPacket	packet = (DatagramPacket)out.get(0);
				buffer.clear();
				if(packet != null){
					buffer.put(packet.getData());
					buffer.flip();
					channel.send(buffer, packet.getSocketAddress());
					if (buffer.hasRemaining())
						return;
				}
				out.remove(0);
			}
			key.interestOps(SelectionKey.OP_READ);
			selector.wakeup();
		}catch (IOException e){
			e.printStackTrace();
		}
	}

}
