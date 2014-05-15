package com.oohlalog.log4j;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * The log4j appender for OohLaLog
 */
public class OohLaLogAppender extends AppenderSkeleton {
	private Queue<LoggingEvent> queue = new ArrayDeque<LoggingEvent>();
	private ExecutorService executorService = null;
	private long timeBuffer = 10000;
	private long lastFlush = System.currentTimeMillis();
	private Object lock = new Object();
	private final AtomicBoolean flushing = new AtomicBoolean( false );
	private final AtomicBoolean shutdown = new AtomicBoolean( false );

	// Config options
	private int maxBuffer = 150;//5;
	private int submissionThreadPool = 1;
	private String host = "api.oohlalog.com";
	private String path = "/api/logging/save.json";
	private String statsPath = "/api/timeSeries/save.json";
	private int port = 80;
	private String authToken = null;
	private boolean secure = false;
	private boolean debug = false;
	private String hostName = null;
	private boolean stats = false;
	private boolean memoryStats = true;
	private boolean fileSystemStats = true;
	private boolean cpuStats = true;
	private long statsInterval = 60000;

	Object previousCpuUsage;

	public OohLaLogAppender() {
		super();
		init();
		startFlushTimer();
		startStatsTimer();
	}
	public OohLaLogAppender(int submissionThreadPool, int maxBuffer) {
		super();
		this.submissionThreadPool = submissionThreadPool;
		this.maxBuffer = maxBuffer;
		init();
		startFlushTimer();
		startStatsTimer();
	}

	@Override
	protected void append( LoggingEvent event ) {
		if (getDebug()) System.out.println( ">>>>>>OohLaLogAppender.append" );
		if (getHostName() != null) event.setProperty("hostName", getHostName());
		queue.add( event );

		if ( queue.size() >= maxBuffer && !flushing.get() )
			flushQueue(queue, maxBuffer);
	}

	@Override
	public boolean requiresLayout() {
		return false;
		// TODO: implement
	}

	@Override
	public void close() {

		flushQueue(this.queue);
		this.shutdown.set( true );
		executorService.shutdown();
	}

	/**
	 * Flush <b>count</b> number of items from queue
	 * @param queue
	 */
	protected void flushQueue( final Queue<LoggingEvent> queue, final int count ) {
		if (getDebug()) System.out.println( ">>>>>>Flushing " + count + " items from queue");
		flushing.set( true );
		executorService.execute(new Runnable() {
			public void run() {
				while(queue.size() >= count) {
					List<LoggingEvent> logs = new ArrayList<LoggingEvent>(count);
					for (int i = 0; i < count; i++) {
						LoggingEvent log;
						if ((log = queue.poll()) == null)
							break;

						logs.add(log);
					}
					if(logs.size() > 0) {
						Payload pl = new Payload.Builder()
						.messages(logs)
						.authToken(getAuthToken())
						.host(getHost())
						.path(getPath())
						.port(getPort())
						.secure(getSecure())
						.debug(getDebug())
						.build();
						Payload.send( pl );
					}
				}



				lastFlush = System.currentTimeMillis();
				flushing.set( false );
			}
		});
	}

	/**
	 * flush queue completely
	 * @param queue
	 */
	protected void flushQueue( final Queue<LoggingEvent> queue ) {
		if (getDebug()) System.out.println( ">>>>>>Flushing Queue Completely" );
		executorService.execute(new Runnable() {
			public void run() {
				List<LoggingEvent> logs = new ArrayList<LoggingEvent>(queue.size());
				for (LoggingEvent le : queue) {
					logs.add(le);
				}
				if(logs.size() == 0) {
					return;
				}
				Payload pl = new Payload.Builder()
					.messages(logs)
					.authToken(getAuthToken())
					.host(getHost())
					.path(getPath())
					.port(getPort())
					.secure(getSecure())
					.debug(getDebug())
					.build();

				Payload.send( pl );

				lastFlush = System.currentTimeMillis();
			}
		});
	}

	protected void init() {
		if ( this.executorService != null ) {
			this.executorService.shutdown();
		}
		this.executorService = Executors.newFixedThreadPool(this.submissionThreadPool);
	}

	protected void startFlushTimer() {
		final OohLaLogAppender logger = this;
		Thread t = new Thread( new Runnable() {
			public void run() {
				// If appender closes, let thread die
				while ( !shutdown.get() ) {

					if (getDebug()) System.out.println( ">>Timer Cycle" );
					// If timeout, flush queue
					if ( (System.currentTimeMillis() - logger.lastFlush > logger.timeBuffer) && !logger.flushing.get() ) {
						if (getDebug()) System.out.println( "Flushing from timer expiration" );
						logger.flushQueue( logger.queue, logger.getMaxBuffer() );
					}

					// Sleep the thread
					try {
						Thread.sleep( logger.timeBuffer);
					}
					catch ( InterruptedException ie ) {
						// Ignore, and continue
					}
				}
			}
		});

		t.start();
	}


	protected void startStatsTimer() {
		final OohLaLogAppender logger = this;
		Thread t = new Thread( new Runnable() {
			public void run() {
				// If appender closes, let thread die
				while (!shutdown.get() ) {
					if (logger.getStats()) {
						if (logger.getDebug()) System.out.println( ">>Stats Timer" );
						// If timeout, flush queue
						OutputStream os = null;
					    BufferedReader rd  = null;
					    StringBuilder sb = null;
					    String line = null;
					    HttpURLConnection con = null;

						try {
							Map<String,Object> payload = new HashMap<String, Object>();
							payload.put("metrics", StatsUtils.getStats(logger));
							String h = logger.getHostName();
							if (h == null ){
								 try { h = java.net.InetAddress.getLocalHost().getHostName(); }
								 catch (java.net.UnknownHostException uh) {}
							}
							payload.put("host", h);
							String json = new Gson().toJson( payload );

							URL url = new URL( (logger.getSecure() ? "https" : "http"), logger.getHost(), logger.getPort(), logger.getStatsPath()+"?apiKey="+logger.getAuthToken() );

							if (logger.getDebug()) System.out.println( ">>>>>>>>>>>Submitting to: " + url.toString() );
							if (logger.getDebug()) System.out.println( ">>>>>>>>>>>JSON: " + json.toString() );
							con = (HttpURLConnection) url.openConnection();
							con.setDoOutput(true);
							con.setDoInput(true);
							con.setInstanceFollowRedirects(false);
							con.setRequestMethod("POST");
							con.setRequestProperty("Content-Type", "application/json");
							con.setRequestProperty("Content-Length", "" + json.getBytes().length);
							con.setUseCaches(false);

							// Get output stream and write json
							os = con.getOutputStream();
							os.write( json.getBytes() );

							rd  = new BufferedReader(new InputStreamReader(con.getInputStream()));
							sb = new StringBuilder();

							while ((line = rd.readLine()) != null){
							  sb.append(line + '\n');
							}
							if (logger.getDebug()) System.out.println( ">>>>>>>>>>>Received: " + sb.toString() );

						} catch (Exception e) {
							if (debug) e.printStackTrace();
							System.out.println("Unable to send stats: "+e.getMessage());
						} finally {
							if ( os != null ) {
								try {
								  con.disconnect();
									os.flush();
									os.close();
									con = null;
								}
								catch ( Throwable t ) {
								}
							}
						}
					}

					// Sleep the thread
					try {
						Thread.sleep( logger.statsInterval);
					}
					catch ( InterruptedException ie ) {
						// Ignore, and continue
					}
				}
			}
		});

		t.start();
	}

	protected Map<String,Double> getRuntimeStats() {
		Map<String, Double> map = new HashMap<String, Double>();
		Runtime runtime = Runtime.getRuntime();
		map.put("maxMemory", new Double(runtime.maxMemory()));
		map.put("freeMemory", new Double(runtime.freeMemory()));
		map.put("totalMemory", new Double(runtime.totalMemory()));
		map.put("usedMemory", new Double(runtime.totalMemory() - runtime.freeMemory()));
		return map;
	}

	public int getMaxBuffer() {
		return maxBuffer;
	}

	public void setMaxBuffer(int maxBuffer) {
		this.maxBuffer = maxBuffer;
	}

	public int getSubmissionThreadPool() {
		return submissionThreadPool;
	}

	public void setSubmissionThreadPool(int submissionThreadPool) {
		this.submissionThreadPool = submissionThreadPool;
		synchronized ( lock ) {
			init();
		}
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getStatsPath() {
		return statsPath;
	}

	public void setStatsPath(String statsPath) {
		this.statsPath = statsPath;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	public long getTimeBuffer() {
		return timeBuffer;
	}

	public void setTimeBuffer(long timeBuffer) {
		this.timeBuffer = timeBuffer;
	}

	public boolean getSecure() {
		return secure;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public boolean getDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean getStats() {
		return stats;
	}

	public void setStats(boolean stats) {
		this.stats = stats;
	}

	public long getStatsInterval() {
		return statsInterval;
	}

	public void setStatsInterval(long statsInterval) {
		this.statsInterval = statsInterval;
	}

	public boolean getMemoryStats() {
		return memoryStats;
	}

	public void setMemoryStats(boolean memoryStats) {
		this.memoryStats = memoryStats;
	}

	public boolean getCpuStats() {
		return cpuStats;
	}

	public void setCpuStats(boolean cpuStats) {
		this.cpuStats = cpuStats;
	}

	public boolean getFileSystemStats() {
		return fileSystemStats;
	}

	public void setFileSystemStats(boolean fileSystemStats) {
		this.fileSystemStats = fileSystemStats;
	}

}
