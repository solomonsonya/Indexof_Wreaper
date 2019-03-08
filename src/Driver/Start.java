/**
 * @author Solomon Sonya
 */

package Driver;


import java.io.*;

import ServerSocket_Client.*;
import Worker.ThdWorker;

public class Start extends Thread implements Runnable
{
	public static final String myClassName = "Start";
	public static volatile Driver driver = new Driver();
	
	public static volatile StandardInListener std_in = null;
	public static volatile PrintWriter pwOut = null;
	public static volatile BufferedReader brIn = null;
	
	public static volatile ServerSocket_Client svr_skt_client = null;
	public static volatile ThdWorker thd_worker = null;
	
	public static String [] args = null;
	public Start(String [] argv)
	{
		try
		{
			args = argv;
			
			this.start();
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "Constructor - 1", e);
		}
	}
	
	public void run()
	{
		try
		{
			initialize_program();
			analyze_args(args);	
			
			
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "run", e);
		}
		
	}
	
	public boolean initialize_program()
	{
		try
		{
			//place initialiation steps here if necessary
			brIn = new BufferedReader(new InputStreamReader(System.in));
			pwOut = new PrintWriter(new OutputStreamWriter (System.out));
			
			//
			//NOTIFY USER
			//
			driver.directive("///////////////////////////////////////////////////////////////////////////////////");
			driver.directive("// Welcome to " + Driver.FULL_NAME + " by Solomon Sonya @Carpenter1010\t//");
			driver.directive("/////////////////////////////////////////////////////////////////////////////////\n");
			
			
			//
			//handle procuring additional input from user here...
			//
			
			
			
			//
			//Start the StandardInListener thread
			//
			std_in = new StandardInListener(brIn, pwOut);
			
			//
			//Server Socket
			//
			svr_skt_client = new ServerSocket_Client(ServerSocket_Client.DEFAULT_CLIENT_PORT);
			
			//
			//ThdWorker
			//
			thd_worker = new ThdWorker();
			
			return true;
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "initialize_program", e);
		}
		
		return false;
	}
	
	public boolean analyze_args(String [] arg)
	{
		try
		{
			if(arg == null || arg.length < 1)
			{
				driver.directive("NULL VALUES!");
			}
			
			return true;
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "analyze_args", e);
		}
		
		return false;
	}
	
	

}
