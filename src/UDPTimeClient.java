import java.io.*;
import java.net.*;
import java.util.Date;


/**
 * Class implements a UDP time client.
 */
public class UDPTimeClient
{
	private static final int 	m_iPort 			= 9876;
	private static final String	m_strRequestTime 	= "What's the time?";

//-------------------------------------------------------------------------	
//-------------------------------------------------------------------------
	/**
	 * @param args - args[0] contains the IP of the server.
	 */
	public static void main(String[] args)
	{
		if(args.length == 0){
			System.out.println("OH NO!!! No given Hostname or IP!");
			System.out.println("Usage: UDPTimeClient [IP|Hostname]");
			return;
		}
		
		try
		{
			// UDP socket.
			DatagramSocket clientSocket = new DatagramSocket();
			
			// ===== sending the request ====
			
			// Convert string to InetAddress - a class that represents IP address.
			// getByName() resolves (performs DNS) if needed.
		    InetAddress ipAddr = InetAddress.getByName(args[0]);
		    
		    // to send data we need to convert it to byte[] - the way UDP "sees" the data.
		    byte[] bTimeRequest = m_strRequestTime.getBytes();
			
		    // create packet
		    // Creates a UDP packet that contains bTimeRequest as it's content (the application layer),
		    // The UDP packet destination address is "ipAddr" to port "m_iPort".
		    DatagramPacket sendPacket = new DatagramPacket(bTimeRequest, bTimeRequest.length, ipAddr, m_iPort); 
		    
		    // send packet through the socket!
		    clientSocket.send(sendPacket); 
		    System.out.println("Client: Request has been sent");
		  
			// ===== getting the response =====
		    
		    // first, we need to allocate enough room.
		    byte[] bTimeResponse = new byte[256];
		    
		    // prepare a UDP packet to be filled with the response.
		    DatagramPacket receivePacket = new DatagramPacket(bTimeResponse, bTimeResponse.length); 
		    
		    // receive function blocks execution until a packet has been received.
			// IF there is no port in defined, java will set the received pord to the DataPacket's port.
		    clientSocket.receive(receivePacket); 
		  
		    // get data from packet.
		    byte[] time = receivePacket.getData();
		  
		    // the time returns is a string holding a 64bit-int (long) that represents the time.
		    // the time is the milliseconds since 1.1.1970
		    String strTime = new String(time, 0, receivePacket.getLength());
		    Long lTime = Long.valueOf(strTime);
		    Date date = new Date(lTime);
		    		
		    System.out.println("Server's current time is: "+date.toString());
		    
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
