package net.smoofyuniverse.epi.stats;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.mariuszgromada.math.mxparser.Function;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import net.smoofyuniverse.common.util.StringUtil;
import net.smoofyuniverse.epi.api.PlayerInfo;

public class RankingList {
	public static final int FORMAT_VERSION = 1;
	
	private static final JsonFactory factory = new JsonFactory();
	
	private Map<String, Ranking> rankings = new HashMap<>();
	private String[] players;
	private Instant date;
	
	public PlayerInfo[] infosCache;
	
	private Function[] functions;
	
	public RankingList(String[] players) {
		this(players, Instant.now());
	}
	
	public RankingList(String[] players, Instant date) {
		this.players = players;
		this.date = date;
	}
	
	private RankingList() {}
	
	public Ranking getOrCreate(String name) {
		Ranking r = this.rankings.get(name);
		if (r == null) {
			r = new Ranking(this, name, this.players.length);
			this.rankings.put(name, r);
			this.functions = null;
		}
		return r;
	}
	
	public Instant getDate() {
		return this.date;
	}
	
	public int getPlayerCount() {
		return this.players.length;
	}
	
	public String getPlayerName(int p) {
		return this.players[p];
	}
	
	public Collection<Ranking> getRankings() {
		return this.rankings.values();
	}
	
	public Function[] getFunctions() {
		if (this.functions == null) {
			this.functions = new Function[this.rankings.size() *2];
			int i = 0;
			for (Ranking r : this.rankings.values()) {
				this.functions[i++] = r.getFunction;
				this.functions[i++] = r.rankFunction;
			}
		}
		return this.functions;
	}
	
	public void save(Path file) throws IOException {
		String fn = file.getFileName().toString();
		
		if (fn.endsWith(".json")) {
			try (JsonGenerator json = factory.createGenerator(Files.newOutputStream(file))) {
				json.useDefaultPrettyPrinter();
				saveJSON(json);
			}
		} else if (fn.endsWith(".csv")) {
			try (BufferedWriter out = Files.newBufferedWriter(file)) {
				saveCSV(out);
			}
		} else {
			try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file))) {
				save(out);
			}
		}
	}
	
	public void save(DataOutputStream out) throws IOException {
		out.writeInt(FORMAT_VERSION);
		
		out.writeLong(this.date.toEpochMilli());
		
		int total = this.players.length;
		out.writeInt(total);
		for (String p : this.players)
			out.writeUTF(p);
		
		out.writeInt(this.rankings.size());
		for (Ranking r : this.rankings.values()) {
			out.writeUTF(r.name);
			boolean d = r.descendingMode;
			out.writeBoolean(d);
			
			r.descendingMode = false;
			Iterator<Integer> it = r.iterator();
			while (it.hasNext()) {
				int p = it.next();
				out.writeInt(p);
				out.writeDouble(r.getValue(p));
			}
			r.descendingMode = d;
		}
	}
	
	public void saveJSON(JsonGenerator json) throws IOException {
		json.writeStartObject();
		
		json.writeFieldName("format_version");
		json.writeNumber(FORMAT_VERSION);
		
		json.writeFieldName("date");
		json.writeString(StringUtil.DATETIME_FORMAT.format(this.date));
		
		json.writeFieldName("players");
		json.writeStartArray();
		for (String p : this.players)
			json.writeString(p);
		json.writeEndArray();
		
		json.writeFieldName("rankings");
		json.writeStartArray();
		for (Ranking r : this.rankings.values()) {
			json.writeStartObject();
			json.writeFieldName("name");
			json.writeString(r.name);
			
			boolean d = r.descendingMode;
			json.writeFieldName("descending");
			json.writeBoolean(d);
			
			json.writeFieldName("content");
			json.writeStartArray();
			r.descendingMode = false;
			Iterator<Integer> it = r.iterator();
			while (it.hasNext()) {
				int p = it.next();
				json.writeNumber(p);
				json.writeNumber(r.getValue(p));
			}
			r.descendingMode = d;
			
			json.writeEndArray();
			json.writeEndObject();
		}
		json.writeEndArray();
		
		json.writeEndObject();
	}
	
	public void saveCSV(BufferedWriter out) throws IOException {
		out.write("Date");
		out.write(',');
		StringUtil.DATETIME_FORMAT.formatTo(this.date, out);
		out.newLine();
		
		Ranking[] rankings = new Ranking[this.rankings.size()];
		Iterator<Integer>[] iterators = new Iterator[rankings.length];
		
		out.write("Classement");
		
		int i = 0;
		for (Ranking r : this.rankings.values()) {
			out.write(',');
			out.write("Joueur");
			out.write(',');
			out.write(r.name);
			
			rankings[i] = r;
			iterators[i++] = r.iterator();
		}
		
		for (int rank = 0; rank < this.players.length; rank++) {
			out.newLine();
			out.write(Integer.toString(rank +1));
			
			
			for (i = 0; i < rankings.length; i++) {
				Iterator<Integer> it = iterators[i];
				if (it.hasNext()) {
					int p = it.next();
					out.write(',');
					out.write(this.players[p]);
					out.write(',');
					out.write(Double.toString(rankings[i].getValue(p)));
				} else {
					out.write(",,");
				}
			}
		}
	}
	
	public static RankingList read(Path file) throws IOException {
		String fn = file.getFileName().toString();
		if (!Files.exists(file))
			throw new FileNotFoundException(fn);
		
		if (fn.endsWith(".json")) {
			try (JsonParser json = factory.createParser(Files.newInputStream(file))) {
				return readJSON(json);
			}
		} else {
			try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
				return read(in);
			}
		}
	}
	
	public static RankingList read(DataInputStream in) throws IOException {
		int version = in.readInt();
		if (version != FORMAT_VERSION)
			throw new IOException("Invalid format version: " + version);
		
		Instant date = Instant.ofEpochMilli(in.readLong());
		
		String[] players = new String[in.readInt()];
		for (int i = 0; i < players.length; i++)
			players[i] = in.readUTF();
		
		RankingList l = new RankingList(players, date);
		int rankings = in.readInt();
		for (int i = 0; i < rankings; i++) {
			Ranking r = new Ranking(l, in.readUTF(), players.length);
			boolean d = in.readBoolean();
			
			for (int p = 0; p < players.length; p++)
				r.put(in.readInt(), in.readDouble());
			
			r.descendingMode = d;
			l.rankings.put(r.name, r);
		}
		return l;
	}
	
	public static RankingList readJSON(JsonParser json) throws IOException {
		if (json.nextToken() != JsonToken.START_OBJECT)
			throw new JsonParseException(json, "Expected to start a new object");
		
		RankingList l = new RankingList();
		
		boolean versionCheck = true;
		while (json.nextToken() != JsonToken.END_OBJECT) {
			String field = json.getCurrentName();
			
			if (versionCheck) {
				if (!field.equals("format_version") || json.nextToken() != JsonToken.VALUE_NUMBER_INT)
					throw new IOException("Format version not provided");
				
				int version = json.getIntValue();
				if (version != FORMAT_VERSION)
					throw new IOException("Invalid format version: " + version);
				
				versionCheck = false;
				continue;
			}
			
			if (field.equals("date")) {
				if (json.nextToken() != JsonToken.VALUE_STRING)
					throw new JsonParseException(json, "Field 'date' was expected to be a string");
				
				l.date = Instant.from(StringUtil.DATETIME_FORMAT.parse(json.getValueAsString()));
				continue;
			}
			
			if (field.equals("players")) {
				if (json.nextToken() != JsonToken.START_ARRAY)
					throw new JsonParseException(json, "Field 'players' was expected to be an array");
				
				List<String> newPlayers = new ArrayList<>();
				
				while (json.nextToken() != JsonToken.END_ARRAY)
					newPlayers.add(json.getValueAsString());
				
				l.players = newPlayers.toArray(new String[newPlayers.size()]);
				continue;
			}
			
			if (field.equals("rankings")) {
				if (json.nextToken() != JsonToken.START_ARRAY)
					throw new JsonParseException(json, "Field 'rankings' was expected to be an array");
				
				while (json.nextToken() != JsonToken.END_ARRAY) {
					if (json.currentToken() != JsonToken.START_OBJECT)
						throw new JsonParseException(json, "Expected to start a new ranking object");
					
					String name = null;
					Boolean descending = null;
					List<Integer> players = null;
					List<Double> values = null;
					
					while (json.nextToken() != JsonToken.END_OBJECT) {
						String field2 = json.getCurrentName();
						
						try {
							if (field2.equals("name")) {
								if (json.nextToken() != JsonToken.VALUE_STRING)
									throw new JsonParseException(json, "Field 'name' of a ranking was expected to be a string");
								
								name = json.getValueAsString();
								continue;
							}
							
							if (field2.equals("descending")) {
								if (json.nextToken() != JsonToken.VALUE_FALSE && json.currentToken() != JsonToken.VALUE_TRUE)
									throw new JsonParseException(json, "Field 'descending' of a ranking was expected to be a boolean");
								
								descending = json.getValueAsBoolean();
								continue;
							}
							
							if (field2.equals("content")) {
								if (json.nextToken() != JsonToken.START_ARRAY)
									throw new JsonParseException(json, "Field 'content' of a ranking was expected to be an array");
								
								players = new ArrayList<>();
								values = new ArrayList<>();
								
								while (json.nextToken() != JsonToken.END_ARRAY) {
									players.add(json.getIntValue());
									json.nextToken();
									values.add(json.getDoubleValue());
								}
								continue;
							}
						} finally {
							if (name != null && descending != null && players != null && values != null) {
								Ranking r = new Ranking(l, name, players.size());
								
								for (int i = 0; i < players.size(); i++)
									r.put(players.get(i), values.get(i));
								
								r.descendingMode = descending;
								l.rankings.put(r.name, r);
							}
						}
						
						json.nextToken();
						json.skipChildren();
					}
				}
				continue;
			}
			
			json.nextToken();
			json.skipChildren();
		}
		
		if (l.date == null)
			throw new IllegalStateException("Date was not provided");
		if (l.players == null)
			throw new IllegalStateException("Players was not provided");
		
		return l;
	}
}
