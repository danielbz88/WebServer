import java.io.*;
import java.net.*;

/**
 * Class implements a UDP time client.
 */
public class UDPTimeServer
{
	private static final int 	m_iPort 			= 9876;
	private static final String	m_strRequestTime 	= "What's the time?";	
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		try
		{
			// UDP socket.
			
			// **************** CHECK THE PORT ***************
			DatagramSocket clientSocket = new DatagramSocket(m_iPort);
			
			while(true)
			{
				// ===== getting the request =====
			    
			    // first, we need to allocate enough room.
			    byte[] bTimeRequest = new byte[256];
			    
			    // prepare a UDP packet to be filled with the request.
			    DatagramPacket receivePacket = new DatagramPacket(bTimeRequest, bTimeRequest.length); 
			    
			    // receive function blocks execution until a packet has been received.
			    clientSocket.receive(receivePacket);
			    
			    System.out.println("Server: Caught a request");
			  
			    // get data from packet - check the request
			    String strRequest = new String(receivePacket.getData(), 0, receivePacket.getLength());
			    			    
			    // if unknown request - ignore
			    if(strRequest.compareTo(m_strRequestTime) != 0){
			    	continue;
			    }
			    
			    // request for current time - send it.
			    System.out.println("Sending current time to "+receivePacket.getAddress().toString());
			    
			    
				// ===== sending the request =====

			    // get the current time and covert it to string
			    Long lTime = System.currentTimeMillis();
			    String strCurTime = lTime.toString();
			    
			    // to send data we need to convert it to byte[] - the way UDP "sees" the data.
			    byte[] bTimeResponse = strCurTime.getBytes();
			    
			    // create packet
			    // Creates a UDP packet that contains bTimeRequest as it's content (the application layer),
			    // The UDP packet destination address is "ipAddr" to port "m_iPort".
			    DatagramPacket sendPacket = new DatagramPacket(bTimeResponse, bTimeResponse.length, receivePacket.getAddress(), receivePacket.getPort()); 
			    
			    // send packet through the socket!
			    System.out.println("Server: Sending Time");
			    clientSocket.send(sendPacket);
			    
			}
		  		    
		}
		catch(UnknownHostException e)
		{
			System.out.println("Given hostname or IP is unknown!");
		}
		catch(SocketException e)
		{
			System.out.println("Problem with socket: "+e.getMessage());
		}
		catch(IOException e)
		{
			System.out.println("Problem with send/receive: "+e.getMessage());
		}
		catch(Exception e)
		{
			System.out.println("An error has occured: "+e.getMessage());
		}
	}

}
