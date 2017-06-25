/*******************************************************************************
 * Copyright (C) 2017 Hugo Dupanloup (Yeregorix)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;

import org.mariuszgromada.math.mxparser.Function;
import org.mariuszgromada.math.mxparser.FunctionExtension;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import net.smoofyuniverse.common.util.StringUtil;
import net.smoofyuniverse.epi.api.PlayerInfo;

public class RankingList {
	public static final int FORMAT_VERSION = 2;
	
	private static final JsonFactory factory = new JsonFactory();
	
	private Map<String, Ranking> rankings = new TreeMap<>();
	private PlayerInfo[] players;
	
	private Instant[] dateExtremums;
	
	private Set<String> totalExtensions = new HashSet<>();
	private Function[] functions;
	
	public RankingList(PlayerInfo[] players) {
		this.players = players;
	}
	
	private RankingList() {}
	
	public Ranking getOrCreate(String name) {
		Ranking r = this.rankings.get(name);
		if (r == null) {
			r = new Ranking(this, name, this.players.length);
			this.rankings.put(name, r);
			this.functions = null;
			
			int i = name.indexOf('_');
			if (i != -1)
				this.totalExtensions.add(name.substring(i +1));
		}
		return r;
	}
	
	public double total(Predicate<String> category, int player) {
		double total = 0;
		for (Ranking r : this.rankings.values()) {
			if (category.test(r.name))
				total += r.getValue(player);
		}
		return total;
	}
	
	public int getPlayerCount() {
		return this.players.length;
	}
	
	public PlayerInfo getPlayer(int p) {
		return this.players[p];
	}
	
	public Collection<Ranking> getRankings() {
		return this.rankings.values();
	}
	
	public Instant[] getDateExtremums() {
		if (this.dateExtremums == null) {
			Instant min = null, max = null;
			for (PlayerInfo p : this.players) {
				if (min == null || p.date.isBefore(min))
					min = p.date;
				if (max == null || p.date.isAfter(max))
					max = p.date;
			}
			this.dateExtremums = new Instant[] {min, max};
		}
		return this.dateExtremums;
	}
	
	public Function[] getFunctions() {
		if (this.functions == null) {
			this.functions = new Function[this.rankings.size() *2 + this.totalExtensions.size()];
			int i = 0;
			for (Ranking r : this.rankings.values()) {
				this.functions[i++] = r.getFunction;
				this.functions[i++] = r.rankFunction;
			}
			for (String s : this.totalExtensions) {
				this.functions[i++] = new Function("total_" + s, new TotalExtension(s));
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
		
		int total = this.players.length;
		out.writeInt(total);
		for (PlayerInfo p : this.players) {
			if (p.id == null) {
				out.writeLong(0);
				out.writeLong(0);
			} else {
				out.writeLong(p.id.getMostSignificantBits());
				out.writeLong(p.id.getLeastSignificantBits());
			}
			out.writeUTF(p.name);
			out.writeLong(p.date.toEpochMilli());
		}
		
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
		
		json.writeFieldName("players");
		json.writeStartArray();
		for (PlayerInfo p : this.players) {
			json.writeStartObject();
			
			json.writeFieldName("id");
			if (p.id == null)
				json.writeNull();
			else
				json.writeString(p.id.toString());
			
			json.writeFieldName("name");
			json.writeString(p.name);
			
			json.writeFieldName("date");
			json.writeString(StringUtil.DATETIME_FORMAT.format(p.date));
			
			json.writeEndObject();
		}
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
		out.write("Dates");
		out.write(',');
		Instant[] extremums = getDateExtremums();
		StringUtil.DATETIME_FORMAT.formatTo(extremums[0], out);
		out.write(',');
		StringUtil.DATETIME_FORMAT.formatTo(extremums[1], out);
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
					out.write(this.players[p].name);
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
		boolean old1 = version == 1; // Compatibility with older versions
		if (version != FORMAT_VERSION && !old1)
			throw new IOException("Invalid format version: " + version);
		
		PlayerInfo[] players;
		if (old1) {
			Instant date = Instant.ofEpochMilli(in.readLong());
			
			players = new PlayerInfo[in.readInt()];
			for (int i = 0; i < players.length; i++) {
				PlayerInfo p = new PlayerInfo();
				p.name = in.readUTF();
				p.date = date;
				players[i] = p;
			}
		} else {
			players = new PlayerInfo[in.readInt()];
			for (int i = 0; i < players.length; i++) {
				PlayerInfo p = new PlayerInfo();
				long most = in.readLong(), least = in.readLong();
				if (most != 0 || least != 0)
					p.id = new UUID(most, least);
				p.name = in.readUTF();
				p.date = Instant.ofEpochMilli(in.readLong());
				players[i] = p;
			}
		}
		
		RankingList l = new RankingList(players);
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
		
		boolean old1 = false; // Compatibility with older versions
		Instant date = null;
		
		boolean versionCheck = true;
		while (json.nextToken() != JsonToken.END_OBJECT) {
			String field = json.getCurrentName();
			
			if (versionCheck) {
				if (!field.equals("format_version") || json.nextToken() != JsonToken.VALUE_NUMBER_INT)
					throw new IOException("Format version not provided");
				
				int version = json.getIntValue();
				old1 = version == 1;
				if (version != FORMAT_VERSION && !old1)
					throw new IOException("Invalid format version: " + version);
				
				versionCheck = false;
				continue;
			}
			
			if (old1 && field.equals("date")) {
				if (json.nextToken() != JsonToken.VALUE_STRING)
					throw new JsonParseException(json, "Field 'date' was expected to be a string");
				
				date = Instant.from(StringUtil.DATETIME_FORMAT.parse(json.getValueAsString()));
				continue;
			}
			
			if (field.equals("players")) {
				if (json.nextToken() != JsonToken.START_ARRAY)
					throw new JsonParseException(json, "Field 'players' was expected to be an array");
				
				List<PlayerInfo> newPlayers = new ArrayList<>();
				
				if (old1) {
					if (date == null)
						throw new IllegalStateException("Date was not provided");
					
					while (json.nextToken() != JsonToken.END_ARRAY) {
						PlayerInfo p = new PlayerInfo();
						p.name = json.getValueAsString();
						p.date = date;
						newPlayers.add(p);
					}
				} else {
					while (json.nextToken() != JsonToken.END_ARRAY) {
						if (json.currentToken() != JsonToken.START_OBJECT)
							throw new JsonParseException(json, "Expected to start a new player object");
						
						PlayerInfo p = new PlayerInfo();
						
						while (json.nextToken() != JsonToken.END_OBJECT) {
							String field2 = json.getCurrentName();
							
							if (field2.equals("id")) {
								p.id = UUID.fromString(json.getValueAsString());
								continue;
							}
							
							if (field2.equals("name")) {
								p.name = json.getValueAsString();
								continue;
							}
							
							if (field2.equals("date")) {
								p.date = Instant.from(StringUtil.DATETIME_FORMAT.parse(json.getValueAsString()));
								continue;
							}
							
							json.nextToken();
							json.skipChildren();
						}
						
						newPlayers.add(p);
					}
					Instant.from(StringUtil.DATETIME_FORMAT.parse(json.getValueAsString()));
				}
				
				l.players = newPlayers.toArray(new PlayerInfo[newPlayers.size()]);
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
		
		if (l.players == null)
			throw new IllegalStateException("Players was not provided");
		return l;
	}
	
	public class TotalExtension implements FunctionExtension {
		public final String name;
		public int player;
		
		public TotalExtension(String name) {
			this.name = name;
		}

		@Override
		public double calculate(double... params) {
			return total((s) -> {
				int i = s.indexOf('_');
				return i != -1 && s.substring(i + 1).equals(this.name);
			}, this.player);
		}

		@Override
		public FunctionExtension clone() {
			return this;
		}

		@Override
		public int getParametersNumber() {
			return 1;
		}

		@Override
		public void setParameterValue(int index, double param) {
			if (index == 0)
				this.player = (int) param;
		}
	}
}
