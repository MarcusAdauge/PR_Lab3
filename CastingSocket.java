import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

public class CastingSocket extends MulticastSocket{
	
	private int port;
	private InetAddress groupIP;
	
	public CastingSocket(final InetAddress IP,final int PORT) throws IOException{
		super(PORT);
		super.joinGroup(IP);
		this.groupIP = IP;
		this.port = PORT;
		//super.setBroadcast(true);
		//super.connect(InetAddress.getByName("255.255.255.255"), PORT);
	}
	
	public void cast(int msgType, Object obj){
	   try{   
		    ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objOutputStream = new ObjectOutputStream(byteOutputStream);
            objOutputStream.writeObject(obj);
            
            byte[] dataBuffer = byteOutputStream.toByteArray();
            byte[] sendBuffer = new byte[dataBuffer.length + 1];

            //prepend the message type to data to be sent
            sendBuffer[0] = (byte)msgType;
            for(int i = 1; i < sendBuffer.length; i++)
            	sendBuffer[i] = dataBuffer[i-1];
            
            super.send(new DatagramPacket(sendBuffer, sendBuffer.length, groupIP, port));
		} catch (IOException e) { e.printStackTrace(); }
	}
	
	
	// type = variable to store the message type
	public Object receive(int[] type) throws ClassNotFoundException, IOException{
		byte[] recvBuffer = new byte[5000];
		ObjectInputStream objInputStream = null;
	    
	    try{
	    	DatagramPacket packet = new DatagramPacket(recvBuffer, recvBuffer.length);
	    	
	    	super.setSoTimeout(2000); 
		    super.receive(packet);		  
		    
		    type[0] = -1;
		    if(packet.getData() == null) return null;
		    type[0] = recvBuffer[0];
		    byte[] dataBuffer = new byte[packet.getLength() - 1];
		    for(int i = 0; i < dataBuffer.length; i++)
		    	dataBuffer[i] = recvBuffer[i+1];
		    	
		    ByteArrayInputStream byteInputStream = new ByteArrayInputStream(dataBuffer);
		    objInputStream = new ObjectInputStream(byteInputStream);
		    
	    }catch(IOException e){ System.out.println(e.getMessage()); return null; }
	    
	    return objInputStream.readObject();
	}
	
}
