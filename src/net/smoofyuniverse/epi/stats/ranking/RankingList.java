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

package net.smoofyuniverse.epi.stats.ranking;

import net.smoofyuniverse.common.util.StringUtil;
import net.smoofyuniverse.epi.api.PlayerInfo;
import net.smoofyuniverse.epi.stats.collection.DataCollection;
import net.smoofyuniverse.epi.stats.operation.PlayerDependantArgument;
import org.mariuszgromada.math.mxparser.Argument;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class RankingList {
	public static final int CURRENT_VERSION = 6, MINIMUM_VERSION = 1;
	
	private Map<String, Ranking> rankings = new TreeMap<>();
	private Set<String> extensions = new HashSet<>();
	public final DataCollection collection;

	public RankingList(DataCollection col) {
		this.collection = col;
	}

	public Optional<Ranking> get(String name) {
		return Optional.ofNullable(this.rankings.get(name));
	}

	public Ranking getOrCreate(String name) {
		Ranking r = this.rankings.get(name);
		if (r == null) {
			r = new Ranking(this, name);
			this.rankings.put(name, r);

			int i = name.indexOf('_');
			if (i != -1)
				this.extensions.add(name.substring(i + 1));
		} else
			r.descending = false;
		return r;
	}

	public void copy(String name, String newName) {
		Ranking r = this.rankings.get(name);
		if (r != null)
			this.rankings.put(newName, r.copy(newName));
	}

	public void move(String name, String newName) {
		Ranking r = this.rankings.remove(name);
		if (r != null)
			this.rankings.put(newName, r.copy(newName));
	}

	public Collection<Ranking> getRankings() {
		return this.rankings.values();
	}

	public List<Ranking> list(Predicate<String> predicate) {
		List<Ranking> l = new ArrayList<>();
		for (Ranking r : this.rankings.values()) {
			if (predicate.test(r.name))
				l.add(r);
		}
		return l;
	}

	public int remove(Predicate<String> predicate) {
		int count = 0;
		Iterator<Ranking> it = this.rankings.values().iterator();
		while (it.hasNext()) {
			if (predicate.test(it.next().name)) {
				it.remove();
				count++;
			}
		}
		return count;
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

	public double total(Predicate<String> predicate, int player) {
		double total = 0;
		for (Ranking r : this.rankings.values()) {
			if (predicate.test(r.name))
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

		if (fn.endsWith(".csv")) {
			try (BufferedWriter out = Files.newBufferedWriter(file)) {
				saveCSV(out);
			}
		} else {
			try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file))) {
				save(out);
			}
		}
	}
	
	public void saveCSV(BufferedWriter out) throws IOException {
		out.write("Dates");
		out.newLine();

		if (this.collection.containsIntervals) {
			out.write("Début");
			StringUtil.DATETIME_FORMAT.formatTo(this.collection.minStartDate, out);
			out.write(',');
			StringUtil.DATETIME_FORMAT.formatTo(this.collection.maxStartDate, out);
			out.newLine();
		}

		out.write("Fin");
		out.write(',');
		StringUtil.DATETIME_FORMAT.formatTo(this.collection.minEndDate, out);
		out.write(',');
		StringUtil.DATETIME_FORMAT.formatTo(this.collection.maxEndDate, out);
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
			iterators[i++] = r.list().iterator();
		}

		for (int rank = 0; rank < this.collection.size; rank++) {
			out.newLine();
			out.write(Integer.toString(rank +1));

			for (i = 0; i < rankings.length; i++) {
				Iterator<Integer> it = iterators[i];
				if (it.hasNext()) {
					int p = it.next();
					out.write(',');
					out.write(this.collection.names.get(p));
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

		GZIPOutputStream zip = new GZIPOutputStream(out);
		out = new DataOutputStream(zip);

		serialize(out);

		zip.finish();
	}

	public void serialize(DataOutputStream out) throws IOException {
		this.collection.serialize(out, false, false);

		out.writeInt(this.rankings.size());
		for (Ranking r : this.rankings.values()) {
			out.writeUTF(r.name);
			out.writeBoolean(r.descending);

			out.writeInt(r.size());
			for (int p : r.list().originalOrder()) {
				out.writeInt(p);
				out.writeDouble(r.getValue(p));
			}
		}
	}

	public static RankingList read(Path file) throws IOException {
		if (!Files.exists(file))
			throw new FileNotFoundException(file.getFileName().toString());

		try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
			return read(in);
		}
	}

	public static RankingList read(DataInputStream in) throws IOException {
		int version = in.readInt();
		if (version > CURRENT_VERSION || version < MINIMUM_VERSION)
			throw new IOException("Invalid format version: " + version);

		if (version >= 5)
			in = new DataInputStream(new GZIPInputStream(in));

		return deserialize(in, version);
	}

	public static RankingList deserialize(DataInputStream in, int version) throws IOException {
		DataCollection col;
		if (version == 1) {
			Instant date = Instant.ofEpochMilli(in.readLong());

			int size = in.readInt();
			DataCollection.Builder builder = DataCollection.builder(size, false);
			for (int i = 0; i < size; i++)
				builder.add(null, PlayerInfo.EMPTY_UUID, in.readUTF(), null, date);

			col = builder.build();
		} else if (version <= 5) {
			boolean useIntervals = version >= 4 && in.readBoolean();

			int size = in.readInt();
			DataCollection.Builder builder = DataCollection.builder(size, useIntervals);
			for (int i = 0; i < size; i++) {
				UUID id = new UUID(in.readLong(), in.readLong());
				String name = in.readUTF();
				if (useIntervals)
					builder.add(null, null, id, name, null, Instant.ofEpochMilli(in.readLong()), Instant.ofEpochMilli(in.readLong()));
				else
					builder.add(null, id, name, null, Instant.ofEpochMilli(in.readLong()));
			}

			col = builder.build();
		} else {
			col = DataCollection.deserialize(in, 3, false, false);
		}

		RankingList l = new RankingList(col);
		int rankings = in.readInt();
		for (int i = 0; i < rankings; i++) {
			Ranking r = new Ranking(l, in.readUTF());
			boolean d = in.readBoolean();

			int size = version == 2 ? col.size : in.readInt();
			for (int p = 0; p < size; p++)
				r.put(in.readInt(), in.readDouble());

			r.descending = d;
			l.rankings.put(r.name, r);
		}

		return l;
	}
}
