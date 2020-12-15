import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class MyDBApp
{
	public static final String NAME = "MyDBAapp.jar";
	public static final String DESCRIPTION = "MyDBApp Server";
	public static final float VERSION = 0.99F;

	public static final int REBIND_WAIT = 3000;
    public static final int TIME_OUT = 3000;

	class WorkerThread extends Thread
	{
		public void run()
		{
			HashMap<String, SSLSocketFactory> sslSocketFactory = null;
			
            for (; ; )
            {
                try
                {
                	if (SSL)
                	{
                		File file = new File("cert");
                        File[] files = file.listFiles(
                    		new FilenameFilter() 
		                    	{
		                            @Override
		                            public boolean accept(File dir, String name)
		                            {
		                                if("p12".equals(name) || name.toLowerCase().endsWith(".p12"))
		                                    return true;
	                                    return false;
		                            }
		                        });

            			sslSocketFactory = new HashMap<String, SSLSocketFactory>();

                        for(File f: files)
                        {
                        	try
                        	{
                            	KeyStore keyStore = KeyStore.getInstance("PKCS12");
                            	FileInputStream fis = new FileInputStream(f);
        	            		keyStore.load(fis, "".toCharArray());
        	            		fis.close();
        	            		
        	            		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        	            		kmf.init(keyStore, "".toCharArray());

        	            		SSLContext sslContext = SSLContext.getInstance("TLS");
        	            		sslContext.init(kmf.getKeyManagers(), null, null);

        	            		sslSocketFactory.put(f.getName(), sslContext.getSocketFactory());

        	            		Log("HTTPS CERTIFICATE " + f.getName() + " OK");
                        	}
                        	catch (Exception e)
                        	{
                        		Log("HTTPS CERTIFICATE " + f.getName() + " FAILED");
                        	}
                        }
                	}
                	
                	server = new ServerSocket(Port, 1024);

                	Log((SSL ? "HTTPS" : "HTTP") + " LISTENING " + Port);

                	for (; ; )
        	        {
        		        Socket socket = server.accept();
        		        new ProcessRequestThread(socket, SSL, sslSocketFactory).start();
        	        }
                }
                catch (Exception e)
                {
                	Log((SSL ? "HTTPS" : "HTTP") + " FAILED " + Port);
                }
                
                try { Thread.sleep(REBIND_WAIT); }
                catch (InterruptedException e) { return; }
            }
		}		
	}
	
	class ProcessRequestThread extends Thread
	{
		Socket Socket;
		boolean SSL;
		
		HashMap<String, SSLSocketFactory> SSLSocketFactory;
		
		ProcessRequestThread(Socket socket, boolean ssl, HashMap<String, SSLSocketFactory> sslSocketFactory)
		{
			Socket = socket;
			SSL = ssl;
			SSLSocketFactory = sslSocketFactory;
		}
		
		public void run()
		{
	        try
	        {
	        	if (SSL)
	        	{
	        		InputStream ins = Socket.getInputStream();

	        		byte[] buffer = new byte[0xFF];
	        		int position = 0;
	        		SSLCapabilities capabilities = null;

	        		// Read the header of TLS record
	        		while (position < SSLExplorer.RECORD_HEADER_SIZE)
	        		{
		        		int count = SSLExplorer.RECORD_HEADER_SIZE - position;
		        		int n = ins.read(buffer, position, count);
		        		if (n < 0)
		        		{
			        		throw new Exception("unexpected end of stream!");
		        		}
		        		position += n;
	        		}

	        		// Get the required size to explore the SSL capabilities
	        		int recordLength = SSLExplorer.getRequiredSize(buffer, 0, position);
	        		if (buffer.length < recordLength)
	        		{
	        			buffer = Arrays.copyOf(buffer, recordLength);
	        		}

	        		while (position < recordLength) 
	        		{
	        			int count = recordLength - position;
	        			int n = ins.read(buffer, position, count);
	        			if (n < 0) 
	        			{
	        				return;
	        				//throw new Exception("unexpected end of stream!");
	        			}
	        			position += n;
	        		}

	        		// Explore
	        		capabilities = SSLExplorer.explore(buffer, 0, recordLength);
//	        		if (capabilities != null)
//	        		{
//	        			System.out.println("Record version: " + capabilities.getRecordVersion());
//	        			System.out.println("Hello version: " + capabilities.getHelloVersion());
//	        		}
	        		
	        		List<SNIServerName> serverNames = capabilities.getServerNames();
	        		
	        		String serverName = "";
	        		if (serverNames.size() > 0)
	        			serverName = new String(serverNames.get(0).getEncoded());

	        		SSLSocketFactory factory;
	        		while (true)
        			{
	        			factory = SSLSocketFactory.get("".equals(serverName) ? "p12" : (serverName + ".p12"));
	        			if (factory != null) break;
	        			if ("".equals(serverName)) break;
	        			String[] split = serverName.split("\\.", 2);
	        			if (split.length > 1)
	        				serverName = split[1];
	        			else
	        				serverName = "";
        			}

        			if (factory == null)
        				return;

	        		ByteArrayInputStream bais = new ByteArrayInputStream(buffer, 0, position);
	        		SSLSocket sslSocket = (SSLSocket)factory.createSocket(Socket, bais, true);
	        		
	        		Socket = sslSocket;
	        	}
	        	
	        	
	            Socket.setSoTimeout(TIME_OUT);

	            InputStream inputStream = Socket.getInputStream();
	            OutputStream outputStream = Socket.getOutputStream();
				Writer writer = new OutputStreamWriter(outputStream, "ISO-8859-1");

				String request = XHelper.readLine(inputStream);
				if (request == null) return;
				
	            String[] request_s = request.split(" ");

	            if (request_s.length != 3)
	            {
	                return;
	            }
	            
	            XTree global = new XTree();
	            XContext context = new XContext(global);
	            
	            context.Set("._REQUEST.REMOTE", Socket.getRemoteSocketAddress().toString());
	            context.Set("._REQUEST.SSL", SSL);
	            context.Set("._REQUEST.METHOD", request_s[0]);
	            context.Set("._REQUEST.URI", request_s[1]);
	            context.Set("._REQUEST.VERSION", request_s[2]);

	            XTree _HEADER = new XTree();
	            
	            String header = XHelper.readLine(inputStream);
	            while (!header.equals("") & header != null)
	            {
	            	String[] header_s = header.split(":", 2);

	                if (header_s.length == 2)
	                {
	                    String header_name = header_s[0].trim();
	                    String header_value = header_s[1].trim();

	                    _HEADER.map.put(header_name, header_value);
                    }

	                header = XHelper.readLine(inputStream);
	            }
	            
	            context.Set("._HEADER", _HEADER);
	            
	            Log((String)context.Get("._REQUEST.REMOTE") + " " + context.Get("._REQUEST.METHOD") + " " + ((boolean)context.Get("._REQUEST.SSL") ? "https://" : "http://") + context.Get("._HEADER.Host") + context.Get("._REQUEST.URI"));

	            Call("main", outputStream, writer, context);
	        
	            writer.close();
	        }
	        catch (Exception e)
	        {
	        	e.printStackTrace();
	        }
	        finally
	        {
	            try { Socket.close(); } catch (IOException e) { }
	        }
		}
	}
	
    public final int Port;
    public final boolean SSL;
    
    ServerSocket server;

    Thread tWorker;

    public MyDBApp(int port, boolean ssl)
    {
        Port = port;
        SSL = ssl;

        tWorker = new WorkerThread();
        tWorker.start();
    }

    public void Close()
    {
        tWorker.interrupt();

        try { server.close(); } catch (IOException e) { }
    }
    
    public static void Call(String function, OutputStream outputStream, Writer writer, XContext context)
    {
		try
		{
			XMachine xmachine = new XMachine(function, XHelper.ReadFile(function), writer, context);
			xmachine.Run();
		}
		catch (IOException e)
		{
			
		}
		catch (XExit e)
		{
			
		}
		catch (XException e)
		{
			try { writer.append("Exception at " + e.Function + ":" + e.Line + ": " + e.Message + "\n");	}
			catch (IOException e1) { }
		}
		finally
		{
			if (writer != null)
			{
				try { writer.flush(); } 
				catch (IOException e) { }
			}
		}
    }

    public static void Log(String message)
    {
    	System.out.println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) + " " + message);
    }
    
    public static void main(String[] args)
    {
    	String title = NAME + " v" + VERSION + " " + DESCRIPTION;
    	System.out.println(title);
    	System.out.println(new String(new char[title.length()]).replace("\0", "="));
    	System.out.println();
    	
		@SuppressWarnings("unused")
		MyDBApp mydbapp  = new MyDBApp(80, false);
		
		@SuppressWarnings("unused")
		MyDBApp mydbapps = new MyDBApp(443, true);

        while (true) {
	        try { Thread.sleep(Long.MAX_VALUE);	}
	        catch (InterruptedException e) { return; }
        }
    }
}
