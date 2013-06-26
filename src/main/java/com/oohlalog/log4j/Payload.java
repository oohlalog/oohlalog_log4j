package com.oohlalog.log4j;

import com.google.gson.Gson;
import org.apache.log4j.spi.LoggingEvent;

import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representation of a payload sent to OohLaLog
 */
public class Payload {
	// payload constants
	static final String PAYLOAD_LOGS = "logs";
	static final String PAYLOAD_COUNTERS = "counters";

	// Config
	private String authToken = null;
	private String host = null;
	private String path = null;
	private int port = 80;
	private List<LoggingEvent> messages = null;
	private Map<String, Object> counters = null;

	/**
	 * lock constructor to require usage of the builder
	 */
	private Payload() {
		super();
	}

	/**
	 * Serialize payload into a transferrable dataformat (json)
	 * @param pl
	 * @return
	 */
	public static String serialize( Payload pl ) {
		Map<String,Object> payload = new HashMap<String,Object>();

		// Add logs
		payload.put( PAYLOAD_LOGS, new ArrayList<Map<String,Object>>( pl.getMessages().size() ));
		for( LoggingEvent le : pl.getMessages() ) {
			((List<Map<String,Object>>)payload.get( PAYLOAD_LOGS )).add(
				transform( le )
			);
		}

		// Add counters
		if ( pl.getCounters() != null )
			payload.put( PAYLOAD_COUNTERS, pl.getCounters() );

		// Add api key
		payload.put( "apiKey", pl.getAuthToken() );

		return new Gson().toJson( payload );
	}

	/**
	 * Serialize a single logging event into transferrable dataformat (json)
	 * @param le
	 * @return
	 */
	public static String serialize( LoggingEvent le ) {
		return new Gson().toJson( transform( le ) );
	}

	/**
	 * Transform a logging event into a map for serialization
	 * @param le
	 * @return
	 */
	private static Map<String,Object> transform( LoggingEvent le ) {
		Map<String,Object> map = new HashMap<String,Object>();

		map.put( "level", le.getLevel().toString() );
		map.put( "message", le.getRenderedMessage() );
		map.put( "category", le.getLoggerName() );
		map.put( "timestamp", le.getTimeStamp() );

		if ( le.getThrowableInformation() != null ) {
			StringBuffer strBuf = new StringBuffer();
			String[] details = le.getThrowableStrRep();
			for ( int i = 0; i < details.length; i++ ) {
				strBuf.append( details[i] + ( i == details.length -1 ? "" : "\n" ) );
			}
			map.put(
				"details",
				strBuf.toString()
			);
		}

		return map;
	}

	/**
	 * Write this payload to remote service
	 * @param pl
	 * @throws RuntimeException
	 */
	public static void send( Payload pl ) throws RuntimeException {
		OutputStream os = null;
    BufferedReader rd  = null;
    StringBuilder sb = null;
    String line = null;
    HttpURLConnection con = null;
		try {
			System.out.println("Serializing: " + pl.toString());
			// Serialize payload into json
			String json = serialize( pl );

			// System.out.println( ">>>>>>>>>>>Payload: " + pl.toString() );

			// Create connection to oohlalog server
			URL url = new URL( "http", pl.getHost(), pl.getPort(), pl.getPath()+"?apiKey="+pl.getAuthToken() );

			// System.out.println( ">>>>>>>>>>>Submitting to: " + url.toString() );
			// System.out.println( ">>>>>>>>>>>JSON: " + json.toString() );
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
			//System.out.println( ">>>>>>>>>>>Received: " + sb.toString() );

		}
		catch ( Throwable t ) {
			t.printStackTrace();
		}
		finally {
			if ( os != null ) {
				try {
				  con.disconnect();
					os.flush();
					os.close();
					con = null;
				}
				catch ( Throwable t ) {
					// swallow
				}
			}
		}
	}

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public List<LoggingEvent> getMessages() {
		return messages;
	}

	public void setMessages(List<LoggingEvent> messages) {
		this.messages = messages;
	}

	public Map<String, Object> getCounters() {
		return counters;
	}

	public void setCounters(Map<String, Object> counters) {
		this.counters = counters;
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

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer();
		sb.append("Payload");
		sb.append("{authToken='").append(authToken).append('\'');
		sb.append(", host='").append(host).append('\'');
		sb.append(", path='").append(path).append('\'');
		sb.append(", port=").append(port);
		sb.append('}');
		return sb.toString();
	}

	/**
	 * Builder pattern helper
	 */
	public static class Builder {
		private String authToken = null;
		private String host = null;
		private String path = null;
		private int port = 80;
		private List<LoggingEvent> messages = null;
		private Map<String, Object> counters = null;

		public Builder() {}
		public Payload build() {
			Payload pl = new Payload();
			pl.authToken = this.authToken;
			pl.host = this.host;
			pl.messages = this.messages;
			pl.counters = this.counters;
			pl.port = this.port;
			pl.path = this.path;
			return pl;
		}

		public Builder authToken( String token ) {
			this.authToken = token;
			return this;
		}

		public Builder host( String host ) {
			this.host = host;
			return this;
		}

		public Builder path( String path ) {
			this.path = path;
			return this;
		}

		public Builder messages( List<LoggingEvent> msgs ) {
			this.messages = msgs;
			return this;
		}

		public Builder counters( Map<String,Object> counters ) {
			this.counters = counters;
			return this;
		}

		public Builder port( int port ) {
			this.port = port;
			return this;
		}
	}
}
