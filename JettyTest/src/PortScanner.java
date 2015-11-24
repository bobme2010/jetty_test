import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;


public class PortScanner {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		for(int i = 1; i< 65535; i++)
		{
		
			try{
					Socket socket= new Socket();
					InetSocketAddress address = new InetSocketAddress("plus.union400.com",i);
					socket.connect(address,500);
					
					System.out.println("");
					System.out.println("port["+i+"] is opened");
					socket.close();
			}
			catch(SocketTimeoutException ex1)
			{
				//System.out.println("port["+i+"] is deactived");
				System.out.print(".");
			}
			catch(Exception ex)
			{
				System.out.println(ex);
			}
		}
	}

}
