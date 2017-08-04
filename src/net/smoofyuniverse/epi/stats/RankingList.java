/*
 * Copyright (c) 2017 Hugo Dupanloup (Yeregorix)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.smoofyuniverse.epi.stats;

import com.fasterxml.jackson.core.*;
import net.smoofyuniverse.common.util.StringUtil;
import net.smoofyuniverse.epi.api.PlayerInfo;
import org.mariuszgromada.math.mxparser.Argument;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class RankingList {
	public static final int CURRENT_VERSION = 4, MINIMUM_VERSION = 1;
	
	private static final JsonFactory factory = new JsonFactory();
	
	private Map<String, Ranking> rankings = new TreeMap<>();
	private Set<String> extensions = new HashSet<>();
	private DataCollection collection;

	public RankingList(DataCollection col) {
		this.collection = col;
	}
	
	private RankingList() {}

	public DataCollection getCollection() {
		return this.collection;
	}

	public Ranking getOrCreate(String name) {
		Ranking r = this.rankings.get(name);
		if (r == null) {
			r = new Ranking(this, name);
			this.rankings.put(name, r);

			int i = name.indexOf('_');
			if (i != -1)
				this.extensions.add(name.substring(i + 1));
		}
		return r;
	}

	public void copy(String name, String newName) {
		Ranking r = this.rankings.get(name);
		if (r != null)
			this.rankings.put(newName, r.copy(newName));
	}

	public void rename(String name, String newName) {
		Ranking r = this.rankings.remove(name);
		if (r != null)
			this.rankings.put(newName, r.rename(newName));
	}

	public Collection<Ranking> getRankings() {
		return this.rankings.values();
	}

	public Argument[] getAllArguments(AtomicInteger player) {
		Argument[] args = new Argument[this.rankings.size() * 2 + this.extensions.size()];
		int i = 0;
		for (Ranking r : this.rankings.values()) {
			args[i++] = new PlayerDependantArgument(r.name, player, r::getValue);
			args[i++] = new PlayerDependantArgument("rank_" + r.name, player, p -> {
				int rank = r.getRank(p);
				return rank == -1 ? Double.NaN : rank + 1;
			});
		}
		for (String s : this.extensions)
			args[i++] = new PlayerDependantArgument("total_" + s, player, p -> total(s, p));
		return args;
	}

	public double total(String extension, int player) {
		return total((s) -> {
			int i = s.indexOf('_');
			return i != -1 && s.substring(i + 1).equals(extension);
		}, player);
	}

	public double total(Predicate<String> category, int player) {
		double total = 0;
		for (Ranking r : this.rankings.values()) {
			if (category.test(r.name))
				total += r.getValue(player);
		}
		return total;
	}
	
	public Argument[] getArguments(String expression, AtomicInteger player) {
		List<Argument> args = new ArrayList<>();
		boolean rank_ = expression.contains("rank_"), total_ = expression.contains("total_");

		for (Ranking r : this.rankings.values()) {
			if (!expression.contains(r.name))
				continue;
			args.add(new PlayerDependantArgument(r.name, player, r::getValue));

			if (rank_ && expression.contains("rank_" + r.name)) {
				args.add(new PlayerDependantArgument("rank_" + r.name, player, p -> {
					int rank = r.getRank(p);
					return rank == -1 ? Double.NaN : rank + 1;
				}));
			}
		}

		if (total_) {
			for (String s : this.extensions) {
				if (expression.contains("total_" + s))
					args.add(new PlayerDependantArgument("total_" + s, player, p -> total(s, p)));
			}
		}

		return args.toArray(new Argument[args.size()]);
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

	public void saveJSON(JsonGenerator json) throws IOException {
		json.writeStartObject();

		json.writeFieldName("format_version");
		json.writeNumber(CURRENT_VERSION);

		boolean useIntervals = this.collection.containsIntervals();

		json.writeFieldName("players");
		json.writeStartArray();
		int total = getPlayerCount();
		for (int i = 0; i < total; i++) {
			PlayerInfo p = getPlayer(i);
			json.writeStartObject();

			json.writeFieldName("id");
			if (p.id == null)
				json.writeNull();
			else
				json.writeString(p.id.toString());

			json.writeFieldName("name");
			json.writeString(p.name);

			if (useIntervals) {
				json.writeFieldName("dates");
				json.writeStartArray();
				json.writeString(StringUtil.DATETIME_FORMAT.format(p.startDate));
				json.writeString(StringUtil.DATETIME_FORMAT.format(p.endDate));
				json.writeEndArray();
			} else {
				json.writeFieldName("date");
				json.writeString(StringUtil.DATETIME_FORMAT.format(p.endDate));
			}

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
		out.newLine();

		if (this.collection.containsIntervals()) {
			out.write("Début");
			StringUtil.DATETIME_FORMAT.formatTo(this.collection.getMinStartDate(), out);
			out.write(',');
			StringUtil.DATETIME_FORMAT.formatTo(this.collection.getMaxStartDate(), out);
			out.newLine();
		}

		out.write("Fin");
		out.write(',');
		StringUtil.DATETIME_FORMAT.formatTo(this.collection.getMinEndDate(), out);
		out.write(',');
		StringUtil.DATETIME_FORMAT.formatTo(this.collection.getMaxEndDate(), out);
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

		int total = getPlayerCount();
		for (int rank = 0; rank < total; rank++) {
			out.newLine();
			out.write(Integer.toString(rank +1));

			for (i = 0; i < rankings.length; i++) {
				Iterator<Integer> it = iterators[i];
				if (it.hasNext()) {
					int p = it.next();
					out.write(',');
					out.write(getPlayer(p).name);
					out.write(',');
					out.write(Double.toString(rankings[i].getValue(p)));
				} else {
					out.write(",,");
				}
			}
		}
	}

	public void save(DataOutputStream out) throws IOException {
		out.writeInt(CURRENT_VERSION);

		boolean useIntervals = this.collection.containsIntervals();
		out.writeBoolean(useIntervals);

		int total = getPlayerCount();
		out.writeInt(total);
		for (int i = 0; i < total; i++) {
			PlayerInfo p = getPlayer(i);
			if (p.id == null) {
				out.writeLong(0);
				out.writeLong(0);
			} else {
				out.writeLong(p.id.getMostSignificantBits());
				out.writeLong(p.id.getLeastSignificantBits());
			}
			out.writeUTF(p.name);
			if (useIntervals)
				out.writeLong(p.startDate.toEpochMilli());
			out.writeLong(p.endDate.toEpochMilli());
		}

		out.writeInt(this.rankings.size());
		for (Ranking r : this.rankings.values()) {
			out.writeUTF(r.name);
			boolean d = r.descendingMode;
			out.writeBoolean(d);

			r.descendingMode = false;
			out.writeInt(r.size());
			Iterator<Integer> it = r.iterator();
			while (it.hasNext()) {
				int p = it.next();
				out.writeInt(p);
				out.writeDouble(r.getValue(p));
			}
			r.descendingMode = d;
		}
	}

	public int getPlayerCount() {
		return this.collection.getPlayerCount();
	}

	public PlayerInfo getPlayer(int p) {
		return this.collection.getPlayer(p);
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

	public static RankingList readJSON(JsonParser json) throws IOException {
		if (json.nextToken() != JsonToken.START_OBJECT)
			throw new JsonParseException(json, "Expected to start a new object");

		RankingList l = new RankingList();

		int version = -1;
		Instant date = null;

		while (json.nextToken() != JsonToken.END_OBJECT) {
			String field = json.getCurrentName();

			if (version == -1) {
				if (!field.equals("format_version") || json.nextToken() != JsonToken.VALUE_NUMBER_INT)
					throw new IOException("Format version not provided");

				version = json.getIntValue();
				if (version > CURRENT_VERSION || version < MINIMUM_VERSION)
					throw new IOException("Invalid format version: " + version);

				continue;
			}

			if (version == 1 && field.equals("date")) {
				if (json.nextToken() != JsonToken.VALUE_STRING)
					throw new JsonParseException(json, "Field 'date' was expected to be a string");

				date = Instant.from(StringUtil.DATETIME_FORMAT.parse(json.getValueAsString()));
				continue;
			}

			if (field.equals("players")) {
				if (json.nextToken() != JsonToken.START_ARRAY)
					throw new JsonParseException(json, "Field 'players' was expected to be an array");

				List<PlayerInfo> players = new ArrayList<>();

				if (version == 1) {
					if (date == null)
						throw new IllegalStateException("Date was not provided");

					while (json.nextToken() != JsonToken.END_ARRAY) {
						if (json.currentToken() != JsonToken.VALUE_STRING)
							throw new JsonParseException(json, "Field 'players' was expected to contains strings");

						players.add(new PlayerInfo(json.getValueAsString(), null, null, date));
					}
				} else {
					while (json.nextToken() != JsonToken.END_ARRAY) {
						if (json.currentToken() != JsonToken.START_OBJECT)
							throw new JsonParseException(json, "Expected to start a new player object");

						UUID id = null;
						String name = null;
						Instant startDate = null, endDate = null;

						while (json.nextToken() != JsonToken.END_OBJECT) {
							String field2 = json.getCurrentName();

							if (field2.equals("id")) {
								if (json.nextToken() != JsonToken.VALUE_STRING)
									throw new JsonParseException(json, "Field 'id' of a player was expected to be a string");

								id = UUID.fromString(json.getValueAsString());
								continue;
							}

							if (field2.equals("name")) {
								if (json.nextToken() != JsonToken.VALUE_STRING)
									throw new JsonParseException(json, "Field 'name' of a player was expected to be a string");

								name = json.getValueAsString();
								continue;
							}

							if (field2.equals("dates")) {
								if (json.nextToken() != JsonToken.START_ARRAY)
									throw new JsonParseException(json, "Field 'dates' of a player was expected to be an array");

								List<Instant> dates = new ArrayList<>();
								while (json.nextToken() != JsonToken.END_ARRAY) {
									if (json.currentToken() != JsonToken.VALUE_STRING)
										throw new JsonParseException(json, "Field 'dates' of a player was expected to contains strings");

									dates.add(Instant.from(StringUtil.DATETIME_FORMAT.parse(json.getValueAsString())));
								}

								startDate = dates.get(0);
								endDate = dates.get(1);
								continue;
							}

							if (field2.equals("date")) {
								if (json.nextToken() != JsonToken.VALUE_STRING)
									throw new JsonParseException(json, "Field 'date' of a player was expected to be a string");

								endDate = Instant.from(StringUtil.DATETIME_FORMAT.parse(json.getValueAsString()));
								continue;
							}

							json.nextToken();
							json.skipChildren();
						}

						if (name == null)
							throw new IllegalArgumentException("Field 'name' is missing");
						if (id == null)
							throw new IllegalArgumentException("Field 'id' is missing");
						if (endDate == null)
							throw new IllegalArgumentException("Field 'dates' is missing");

						players.add(new PlayerInfo(name, id, startDate, endDate));
					}
				}

				l.collection = new DataCollection(players.toArray(new PlayerInfo[players.size()]));
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
								Ranking r = new Ranking(l, name);

								for (int i = 0; i < players.size(); i++)
									r.put(players.get(i), values.get(i));

								r.descendingMode = descending;
								l.rankings.put(r.name, r);

								name = null;
								descending = null;
								players = null;
								values = null;
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

		if (l.collection == null)
			throw new IllegalStateException("Players was not provided");
		return l;
	}

	public static RankingList read(DataInputStream in) throws IOException {
		int version = in.readInt();
		if (version > CURRENT_VERSION || version < MINIMUM_VERSION)
			throw new IOException("Invalid format version: " + version);

		PlayerInfo[] players;
		if (version == 1) {
			Instant date = Instant.ofEpochMilli(in.readLong());

			players = new PlayerInfo[in.readInt()];
			for (int i = 0; i < players.length; i++) {
				players[i] = new PlayerInfo(in.readUTF(), null, null, date);
			}
		} else {
			boolean useIntervals = version >= 4 && in.readBoolean();

			players = new PlayerInfo[in.readInt()];
			for (int i = 0; i < players.length; i++) {
				long most = in.readLong(), least = in.readLong();
				UUID id = (most != 0 || least != 0) ? new UUID(most, least) : null;
				String name = in.readUTF();
				Instant startDate = useIntervals ? Instant.ofEpochMilli(in.readLong()) : null;
				Instant endDate = Instant.ofEpochMilli(in.readLong());
				players[i] = new PlayerInfo(name, id, startDate, endDate);
			}
		}

		RankingList l = new RankingList(new DataCollection(players));
		int rankings = in.readInt();
		for (int i = 0; i < rankings; i++) {
			Ranking r = new Ranking(l, in.readUTF());
			boolean d = in.readBoolean();

			int size = version == 2 ? players.length : in.readInt();
			for (int p = 0; p < size; p++)
				r.put(in.readInt(), in.readDouble());

			r.descendingMode = d;
			l.rankings.put(r.name, r);
		}

		return l;
	}
}
