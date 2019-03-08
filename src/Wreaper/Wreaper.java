/**
 * Determine a location to download the resources from 
 * 
 * We'll use interrupt threads to not have the system bogged by multiple while loops in long file downloads
 * 
 * @author Solomon Sonya
 */

package Wreaper;

import Driver.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.Timer;
import java.util.*;

import javax.net.ssl.HttpsURLConnection;

public class Wreaper extends Thread implements Runnable, ActionListener
{
	public static final String myClassName = "Wreaper";
	public static volatile Driver driver = new Driver();
	
	public String SITE_TO_WREAP = "";
	public boolean RECURSE_SUBDIRECTORIES = false;
	public String response_code = "", response_message = "";
	
	public volatile String log_file_top_folder = "";
	public volatile String top_folder_name = "";
	
	public volatile String CONTAINER_TOP_FOLDER = "";
	
	public volatile Socket skt = null;
	public volatile URL url = null;
	public volatile URLConnection url_connection = null;
	public volatile HttpURLConnection http_url_connection = null;
	public volatile HttpsURLConnection https_url_connection = null;

	public boolean read_website_via_interrupt_timer = true;
	public volatile boolean process_interrupt = true;
	public volatile Timer tmr = null;
	public volatile String line = "";
	
	public volatile LinkedList<String> list_to_use = new LinkedList<String>();
	public volatile LinkedList<String> list_links = new LinkedList<String>();
	public volatile LinkedList<String> list_directories = new LinkedList<String>();
	
	public volatile LinkedList<String> list_downloaded_resources = new LinkedList<String>();

	
	public volatile String line_to_add = "";
	
	public volatile Log log = null;
	public volatile boolean directory_discovered = false;
	
	public volatile int index = 0;
	
	public volatile BufferedReader brIn = null;
	
	/**to help us know if we need to extract the directory structure first, or if we're just downloading a direct link*/
	public boolean is_first_run = false;
	
	public volatile Wreaper parent = null;
	
	public long good_file_count = 0;
	public long bad_file_count = 0;
	
	public static volatile Log log_download_successful =  new Log(null, "_" + "download_success", true);
	public static volatile Log log_error  =  new Log(null, "_" + "download_error", true);

	public Wreaper(String site_to_wreap, boolean recurse_subdirectories, boolean implement_read_via_interrupt_thread, Wreaper par)
	{
		try
		{
			SITE_TO_WREAP = site_to_wreap;
			RECURSE_SUBDIRECTORIES = recurse_subdirectories;
			read_website_via_interrupt_timer = implement_read_via_interrupt_thread;
			parent = par;
			
			if(par != null)
			{
				this.list_directories = par.list_directories;
				this.list_links = par.list_links;
				CONTAINER_TOP_FOLDER = par.CONTAINER_TOP_FOLDER;
			}
			else//first run
				CONTAINER_TOP_FOLDER = driver.get_CONTAINER_top_folder_name(site_to_wreap);
			
			if(SITE_TO_WREAP == null || SITE_TO_WREAP.trim().equals(""))
			{
				driver.directive("PUNT! I am unable to contine with wreap process.  I received an empty location to analyze...");					
			}
			else
			{
				SITE_TO_WREAP = SITE_TO_WREAP.trim();
				
				if(SITE_TO_WREAP.startsWith("localhost"))
					SITE_TO_WREAP = "127.0.0.1" + SITE_TO_WREAP.substring(9);
				
				is_first_run = true;
				
				if(implement_read_via_interrupt_thread)
					this.start();
				else
					open_connection_first_run(SITE_TO_WREAP, read_website_via_interrupt_timer);
			}
			
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
			if(is_first_run)
				open_connection_first_run(SITE_TO_WREAP, read_website_via_interrupt_timer);
			
			
			
		}
		catch(Exception e)
		{
			//driver.eop(myClassName, "run", e);
			driver.directive("ERROR! I was unable to establish connection to [" + this.SITE_TO_WREAP + "] - message: " + e.getLocalizedMessage());
		}
	}
	
	public boolean open_connection_first_run(String site_to_wreap, boolean implement_read_file_via_interrupt_timer)
	{
		try
		{
			//first run, determine the file structure.  Second run, download each resource
			
			//
			//first run
			//
						
			if(driver.is_ipv4(site_to_wreap))
			{
				//bifurcate addr from port
				String [] addr_port = site_to_wreap.split(" ");
				
				if(addr_port == null || addr_port.length < 2)
					addr_port = site_to_wreap.split(":");
				
				else if(addr_port == null || addr_port.length < 2)
					addr_port = site_to_wreap.split(",");
				
				if(addr_port == null || addr_port.length < 1)
				{
					driver.directive("PUNT! I am unable to establish socket connection to [" + site_to_wreap + "]");
					return false;
				}
				
				String addr = addr_port[0];
				int port = 80;
				
				if(addr_port.length < 2)
				{
					driver.directive("NOTE: You did not provide a port for connection to [" + site_to_wreap + "]. I will attempt to connect to port 80...");					
				}
				else
				{
					try
					{
						addr = addr_port[0].replaceAll(",", "").trim();
						port = Integer.parseInt(addr_port[1].replaceAll(",", "").trim());
					}
					catch(Exception e)
					{
						driver.directive("ERROR! Address or port entry was invalid --> " + site_to_wreap);
						return false;
					}
					
				}			
				
				driver.sop("Attempting to establish connection to --> " + addr + " : " + port);
				
				skt = new Socket(addr, port);
				
				brIn = new BufferedReader(new InputStreamReader(skt.getInputStream()));
			}
			
//			else if(site_to_wreap.toLowerCase().trim().startsWith("https"))
//			{
//				driver.sop("Attempting to establish Secure HTTP connection to --> " + site_to_wreap);
//				
//				url = new URL(site_to_wreap);
//				https_url_connection = (HttpsURLConnection) url.openConnection();
//				
//				https_url_connection.setFollowRedirects(true);
//				https_url_connection.setConnectTimeout(1000*10);
//				https_url_connection.setInstanceFollowRedirects(true); 
//				https_url_connection.setRequestProperty("Connection",  "keep-alive");
//				https_url_connection.setRequestProperty("User-Agent",  "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome64.0.3282.186 Safari/537.36");
//				https_url_connection.setRequestProperty("Accept",  "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
//				https_url_connection.setRequestProperty("Accept-Encoding",  "gzip, deflate");
//				https_url_connection.setRequestProperty("Accept-Language",  "en-US,en;q=0.9");
//				
//				response_code 	 = String.valueOf(http_url_connection.getResponseCode());
//				response_message = String.valueOf(http_url_connection.getResponseMessage());
//				
//				
//				
//				brIn = new BufferedReader(new InputStreamReader(https_url_connection.getInputStream()));
//			}
//			
			else if(site_to_wreap.toLowerCase().trim().startsWith("http"))
			{
				driver.sop("Attempting to establish HTTP connection to --> " + site_to_wreap);
				
				url = new URL(site_to_wreap);
				http_url_connection = (HttpURLConnection) url.openConnection();
				
				http_url_connection.setFollowRedirects(true);
				http_url_connection.setConnectTimeout(1000*10);
				http_url_connection.setInstanceFollowRedirects(true); 
				http_url_connection.setRequestProperty("Connection",  "keep-alive");
				http_url_connection.setRequestProperty("User-Agent",  "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome64.0.3282.186 Safari/537.36");
				http_url_connection.setRequestProperty("Accept",  "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
				//http_url_connection.setRequestProperty("Accept-Encoding",  "gzip, deflate");
				http_url_connection.setRequestProperty("Accept-Language",  "en-US,en;q=0.9");
				
				response_code 	 = String.valueOf(http_url_connection.getResponseCode());
				response_message = String.valueOf(http_url_connection.getResponseMessage());
				
				
				
				brIn = new BufferedReader(new InputStreamReader(http_url_connection.getInputStream()));
			}
			else
			{
				driver.sop("Attempting to establish connection to --> " + site_to_wreap);
				
				url = new URL("http://" + site_to_wreap);
				url_connection = url.openConnection();
				
				url_connection.setConnectTimeout(1000*10);				
				url_connection.setRequestProperty("Connection",  "keep-alive");
				url_connection.setRequestProperty("User-Agent",  "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome64.0.3282.186 Safari/537.36");
				url_connection.setRequestProperty("Accept",  "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
				//url_connection.setRequestProperty("Accept-Encoding",  "gzip, deflate");
				url_connection.setRequestProperty("Accept-Language",  "en-US,en;q=0.9");
												
				brIn = new BufferedReader(new InputStreamReader(url_connection.getInputStream()));
			}
			
			
			
			//good made it here, we were able to open a connection
			
			if(response_code != null && !response_code.trim().equals("") && response_message != null && !response_message.trim().equals(""))				
				driver.sop("Connection established with [" + site_to_wreap + "] Response code: [" + response_code + "] Response message: [" + response_message + "]");
			else
				driver.sop("Connection established with [" + site_to_wreap + "]");
			
			top_folder_name = driver.get_top_folder_name(site_to_wreap);
			log_file_top_folder = driver.get_time_stamp("_");
			//this.log = new Log(top_folder_name + File.separator + log_file_top_folder, "_" + top_folder_name);
			this.log = new Log(CONTAINER_TOP_FOLDER, "_" + top_folder_name, false);
			
			
			//
			//FULL READ, OR READ USING INTERRUPT TIMER
			//
			if(implement_read_file_via_interrupt_timer)
			{
				//start the interrupt timer!
				this.tmr = new Timer(30, this);
				this.tmr.start();
			}
			
			//
			//FULL BLOCKING READ
			//
			else
			{
				line = "";
				
				while((line = brIn.readLine()) != null)
				{							
					read_line_first_run(line);
				}
				
				//
				//FINISHED
				//
				try	{	this.brIn.close(); } catch(Exception e){}
				
				if(log != null)
					log.close_and_open_log_file(false);
				
				//
				//
				//
				//only complete these actions if this is the main driver, and not the parent calling for additional recursive retrievals
				read_complete_first_run();
				
			}
			
			
			return true;
		}
		
		catch(ConnectException ce)
		{
			driver.directive("ERROR! I was unable to open connection to " + SITE_TO_WREAP);
		}
		
		catch(MalformedURLException mue)
		{
			driver.directive("ERROR! I was unable to open connection to " + SITE_TO_WREAP + " - this url appears to be MALFORMED...");
		}
		
//		catch(NullPointerException npe)
//		{
//			npe.printStackTrace(System.out);
//		}
		
		catch(Exception e)
		{
			driver.eop(myClassName, "open_connection_first_run", e);
		}
		
		return false;
	}
	
	public boolean read_complete_first_run()
	{
		try
		{
			driver.print_linked_list("Links: ", this.list_links);
			driver.print_linked_list("\nDirectories: ", this.list_directories);
			
			/*driver.directive("top_folder_name: " + top_folder_name);
			driver.directive("log_file_top_folder: " + log_file_top_folder);
			driver.directive("CONTAINER_TOP_FOLDER: " + CONTAINER_TOP_FOLDER);	*/		
			
			//save each file
			File fle = null;

			//			for(String link : list_links)
//			{
//				fle = driver.download(link, false, log_download_successful, log_error);
//				
//				if(fle == null || !fle.exists())
//				{
//					//list_error.add(link);
//					if(parent == null)
//						++bad_file_count;
//					else
//						++parent.bad_file_count;
//				}
//				else//good file
//				{
//					//list_already_existed_or_newly_downloaded_file.add(fle);
//					if(parent == null)
//						++good_file_count;
//					else
//						++parent.good_file_count;
//				}
//			}
			
			while(list_links != null && !list_links.isEmpty())
			{
				String link = list_links.removeFirst();
				
				if(!list_downloaded_resources.contains(link))
					list_downloaded_resources.add(link);
				
				fle = driver.download(link, false, log_download_successful, log_error);
				
				if(fle == null || !fle.exists())
				{
					//list_error.add(link);
					if(parent == null)
						++bad_file_count;
					else
						++parent.bad_file_count;
				}
				else//good file
				{
					//list_already_existed_or_newly_downloaded_file.add(fle);
					if(parent == null)
						++good_file_count;
					else
						++parent.good_file_count;
				}
			}
			
			
			driver.directive("\nCOMPLETE! Total files: [" + list_downloaded_resources.size() + "] Num newly download (or already existed) files: [" + good_file_count + "] Num error files: [" + bad_file_count + "]. Refer to download logs for additional info if required.");
			
			return true;
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "read_complete_first_run", e);
		}
		
		return false;
	}
	
	public boolean read_line_first_run(String line)
	{
		try
		{
			//driver.directive(line);
			
			log.log_directly(line);
			
			analyze_line_for_ahref(line, null);
			
			if(index++%25 == 0)
			{
				index = 0;
				driver.sp(".");
			}
									
			return true;
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "read_line_first_run", e);
		}
		
		return false;
	}
	
	public boolean analyze_line_for_ahref(String line, String parent_link)
	{
		//determine if line has <a href="..."> - bcs if so, this is a line we wish to process
		try
		{
			if(line == null || line.trim().equals(""))
				return false;
			
			if(parent_link != null && !parent_link.trim().endsWith("/"))
				parent_link = parent_link.trim() + "/";
			
			line = line.trim();
			
			if(line.contains("alt=\"[DIR]\"") || line.toLowerCase().contains("alt=\"[dir]\""))
			{
				directory_discovered = true;
				this.list_to_use = this.list_directories;								
			}
			else
			{
				directory_discovered = false;
				this.list_to_use = this.list_links;
			}
			
			if(line.toLowerCase().contains("a href=\""))
			{
				//head
				line = line.substring(line.indexOf("a href=\"")+8).trim();  
				
				//tail
				line = line.substring(0, line.indexOf("\"")).trim();
				
				if(line != null && !line.trim().equals("") && !this.list_links.contains(line))
				{
					//keep line
					
					//
					//directory
					//
					if(directory_discovered)
					{
						this.list_to_use = this.list_directories;	
						
						//determine if we have to add full directory
						//if(line.indexOf("/") >= 0 && line.indexOf("/") < line.length()-2)
						if(line.toLowerCase().startsWith("http") || line.toLowerCase().startsWith("www"))
						{
							//we might have the full link, add it
							line_to_add = line;
						}
						else if(parent_link != null)
							line_to_add = parent_link + line;
						else if(this.SITE_TO_WREAP.endsWith("/"))
							line_to_add = SITE_TO_WREAP + line;
						else
							line_to_add = SITE_TO_WREAP + "/" + line;
							
						//
						//recurse directory
						//String site_to_wreap, boolean recurse_subdirectories, boolean implement_read_via_interrupt_thread, Wreaper par)
						if(this.RECURSE_SUBDIRECTORIES)
						{
							Wreaper wreaper_directory = new Wreaper(line_to_add, RECURSE_SUBDIRECTORIES, this.read_website_via_interrupt_timer, this);
						}
						
					}
					
					//
					//file
					//
					else
					{
						this.list_to_use = this.list_links;
						
						if(line.toLowerCase().startsWith("http") || line.toLowerCase().startsWith("www")) //e.g. <a href="http://mirror1.malwaredomains.com/files/20160802.txt">20160802.txt</a>                                       02-Aug-2016 21:53               14741
							line_to_add = line;
						
						else if(line.startsWith("//"))
							line_to_add = line.substring(2);
						
						else if(line.startsWith("/"))
						{
							if(parent_link == null)
							{
								if(SITE_TO_WREAP.endsWith("/"))
									line_to_add = SITE_TO_WREAP + line.substring(1);
								else
									line_to_add = SITE_TO_WREAP + line;
							}
							else
								line_to_add = parent_link + line.substring(1);
						}
						
						else if(!line.contains("/")) //e.g. <a href="20160803.txt">20160803.txt</a>                                       03-Aug-2016 21:56               14849
						{
							//need to add full link here...
							
							if(line.toLowerCase().startsWith("http") || line.toLowerCase().startsWith("www")) //e.g. <a href="http://mirror1.malwaredomains.com/files/20160802.txt">20160802.txt</a>                                       02-Aug-2016 21:53               14741
								line_to_add = line;
							
							
							
							else if(parent_link == null)
							{
								if(SITE_TO_WREAP.endsWith("/"))
									line_to_add = SITE_TO_WREAP + line;
								else
									line_to_add = SITE_TO_WREAP + "/" + line;
							}
							else
								line_to_add = parent_link + line;
						}
						 
						
						else
							line_to_add = line;
					}
					
					//
					//add link					
					//
					if(!list_to_use.contains(line_to_add))
						list_to_use.add(line_to_add);
					
					/*if(this.SITE_TO_WREAP.endsWith("/"))
						list_to_use.add(SITE_TO_WREAP + line);
					else
						list_to_use.add(SITE_TO_WREAP + "/" + line);*/
				}
			}
			
			
			//line = line.substring(0, line.indexOf("\""));
			
			//driver.directive("-->" + line);
			
			return true;
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "analyze_line_for_ahref", e);
		}
		
		return false;
	}
	
	public void actionPerformed(ActionEvent ae)
	{
		try
		{
			if(ae.getSource() == this.tmr)
				process_interrupt();
			
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "ae", e);
		}
	}
	
	public boolean process_interrupt()
	{
		try
		{
			if(!this.process_interrupt)
				return false;
			
			this.process_interrupt = false;
			
			line = brIn.readLine();
			
			//
			//BASE CASE
			//
			if(line == null)
			{
				//close!
				try	{	this.brIn.close(); } catch(Exception e){}
				try	{	this.tmr.stop(); } catch(Exception e){}
				
				if(log != null)
					log.close_and_open_log_file(false);

				//do not release semaphore
				return read_complete_first_run();
			}
			
			//
			//otw, process line
			//
			//line = line.trim();
			
			//if(!line.equals(""))		
				this.read_line_first_run(line);
			
			
			this.process_interrupt = true;
			return true;
		}
		catch(Exception e)
		{
			driver.eop(myClassName, "process_interrupt", e);
		}
		
		
		this.process_interrupt = true;
		return false;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
//test set: https://dl.fedoraproject.org/pub/alt/, view-source:http://mirror1.malwaredomains.com/files/