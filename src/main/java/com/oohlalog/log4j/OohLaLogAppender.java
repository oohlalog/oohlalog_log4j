package com.oohlalog.log4j;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The log4j appender for OohLaLog
 */
public class OohLaLogAppender extends AppenderSkeleton {
	private Queue<LoggingEvent> queue = new ArrayDeque<LoggingEvent>();
	private ExecutorService executorService = null;
	private long timeBuffer = 60 * 1000;
	private long lastFlush = System.currentTimeMillis();
	private Object lock = new Object();
	private final AtomicBoolean flushing = new AtomicBoolean( false );
	private final AtomicBoolean shutdown = new AtomicBoolean( false );

	// Config options
	private int maxBuffer = 150;//5;
	private int submissionThreadPool = 1;
	private String host = "api.oohlalog.com";
	private String path = "/api/logging/save.json";
	private int port = 80;
	private String authToken = null;
	private boolean secure = false;
	private boolean debug = false;

	public OohLaLogAppender() {
		super();
		init();
		startFlushTimer();
	}
	public OohLaLogAppender(int submissionThreadPool, int maxBuffer) {
		super();
		this.submissionThreadPool = submissionThreadPool;
		this.maxBuffer = maxBuffer;
		init();
		startFlushTimer();
	}

	@Override
	protected void append( LoggingEvent event ) {
		if (getDebug()) System.out.println( ">>>>>>OohLaLogAppender.append" );
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

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
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

}
