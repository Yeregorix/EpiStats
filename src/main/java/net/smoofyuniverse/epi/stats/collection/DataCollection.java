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

package net.smoofyuniverse.epi.stats.collection;

import net.smoofyuniverse.epi.api.PlayerInfo;
import net.smoofyuniverse.epi.util.DoubleIterator;
import net.smoofyuniverse.epi.util.DoubleList;
import net.smoofyuniverse.epi.util.ImmutableDoubleList;
import net.smoofyuniverse.epi.util.ImmutableList;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class DataCollection {
	public static final int CURRENT_VERSION = 3, MINIMUM_VERSION = 1;

	public final Map<UUID, Integer> players;

	public final Map<String, Map<String, ImmutableDoubleList>> stats;
	public final ImmutableList<UUID> ids;
	public final ImmutableList<String> names, guilds;
	public final ImmutableList<Instant> startDates, endDates;

	public final Instant minStartDate, maxStartDate, minEndDate, maxEndDate;
	public final boolean containsIntervals;
	public final int size;

	private DataCollection(Map<UUID, Integer> players, Map<String, Map<String, ImmutableDoubleList>> stats, ImmutableList<UUID> ids, ImmutableList<String> names,
						   ImmutableList<String> guilds, ImmutableList<Instant> startDates, ImmutableList<Instant> endDates,
						   Instant minStartDate, Instant maxStartDate, Instant minEndDate, Instant maxEndDate, int size) {
		this.players = players;

		this.stats = stats;
		this.names = names;
		this.guilds = guilds;
		this.ids = ids;
		this.startDates = startDates;
		this.endDates = endDates;

		this.minStartDate = minStartDate;
		this.maxStartDate = maxStartDate;
		this.minEndDate = minEndDate;
		this.maxEndDate = maxEndDate;

		this.containsIntervals = this.startDates != null;
		this.size = size;
	}

	public void save(Path file) throws IOException {
		try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file))) {
			save(out);
		}
	}

	public void save(DataOutputStream out) throws IOException {
		out.writeInt(CURRENT_VERSION);

		GZIPOutputStream zip = new GZIPOutputStream(out);
		out = new DataOutputStream(zip);

		serialize(out, true, true);

		zip.finish();
	}

	public void serialize(DataOutputStream out, boolean writeGuilds, boolean writeStats) throws IOException {
		out.writeInt(this.size);

		for (UUID id : this.ids) {
			out.writeLong(id.getMostSignificantBits());
			out.writeLong(id.getLeastSignificantBits());
		}

		for (String name : this.names)
			out.writeUTF(name);

		if (writeGuilds) {
			for (String guild : this.guilds)
				out.writeUTF(guild == null ? "" : guild);
		}

		out.writeBoolean(this.containsIntervals);
		if (this.containsIntervals) {
			for (Instant date : this.startDates)
				out.writeLong(date.toEpochMilli());
		}

		for (Instant date : this.endDates)
			out.writeLong(date.toEpochMilli());

		if (writeStats) {
			out.writeInt(this.stats.size());
			for (Entry<String, Map<String, ImmutableDoubleList>> e : this.stats.entrySet()) {
				out.writeUTF(e.getKey());
				out.writeInt(e.getValue().size());
				for (Entry<String, ImmutableDoubleList> list : e.getValue().entrySet()) {
					out.writeUTF(list.getKey());
					DoubleIterator it = list.getValue().iterator();
					while (it.hasNext())
						out.writeDouble(it.next());
				}
			}
		}
	}

	public static DataCollection read(Path file) throws IOException {
		if (!Files.exists(file))
			throw new FileNotFoundException(file.getFileName().toString());

		try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
			return read(in);
		}
	}

	public static DataCollection read(DataInputStream in) throws IOException {
		int version = in.readInt();
		if (version > CURRENT_VERSION || version < MINIMUM_VERSION)
			throw new IOException("Invalid format version: " + version);

		if (version >= 2)
			in = new DataInputStream(new GZIPInputStream(in));

		return deserialize(in, version, true, true);
	}

	public static DataCollection deserialize(DataInputStream in, int version, boolean readGuilds, boolean readStats) throws IOException {
		int size = in.readInt();
		if (version <= 2) {
			Builder b = new Builder(size, false);
			for (int i = 0; i < size; i++) {
				UUID id = new UUID(in.readLong(), in.readLong());
				String name = in.readUTF();
				String guild = in.readUTF();
				if (guild.isEmpty())
					guild = null;
				Instant date = Instant.ofEpochMilli(in.readLong());

				Map<String, Map<String, Double>> stats = new HashMap<>();
				int maps = in.readInt();
				for (int j = 0; j < maps; j++) {
					Map<String, Double> map = new HashMap<>();
					stats.put(in.readUTF(), map);
					int count = in.readInt();
					for (int y = 0; y < count; y++)
						map.put(in.readUTF(), in.readDouble());
				}

				b.add(Collections.unmodifiableMap(stats), id, name, guild, date);
			}
			return b.build();
		}

		Map<UUID, Integer> players = new HashMap<>();
		UUID[] ids = new UUID[size];
		for (int i = 0; i < size; i++) {
			UUID id = new UUID(in.readLong(), in.readLong());
			ids[i] = id;

			players.put(id, i);
		}

		String[] names = new String[size];
		for (int i = 0; i < size; i++)
			names[i] = in.readUTF();


		String[] guilds = new String[size];
		if (readGuilds) {
			for (int i = 0; i < size; i++) {
				String g = in.readUTF();
				guilds[i] = g.isEmpty() ? null : g;
			}
		}

		Instant minStartDate = null, maxStartDate = null;
		Instant[] startDates = null;
		if (in.readBoolean()) {
			startDates = new Instant[size];
			for (int i = 0; i < size; i++) {
				Instant t = Instant.ofEpochMilli(in.readLong());
				startDates[i] = t;

				if (minStartDate == null || t.isBefore(minStartDate))
					minStartDate = t;
				if (maxStartDate == null || t.isAfter(maxStartDate))
					maxStartDate = t;
			}
		}

		Instant minEndDate = null, maxEndDate = null;
		Instant[] endDates = new Instant[size];
		for (int i = 0; i < size; i++) {
			Instant t = Instant.ofEpochMilli(in.readLong());
			endDates[i] = t;

			if (minEndDate == null || t.isBefore(minEndDate))
				minEndDate = t;
			if (maxEndDate == null || t.isAfter(maxEndDate))
				maxEndDate = t;
		}

		Map<String, Map<String, ImmutableDoubleList>> stats = new HashMap<>();
		if (readStats) {
			int maps = in.readInt();
			for (int j = 0; j < maps; j++) {
				String key = in.readUTF();
				int count = in.readInt();
				Map<String, ImmutableDoubleList> map = new HashMap<>();
				for (int y = 0; y < count; y++) {
					String stat = in.readUTF();
					double[] array = new double[size];
					for (int i = 0; i < size; i++)
						array[i] = in.readDouble();
					map.put(stat, ImmutableDoubleList.of(array));
				}
				stats.put(key, Collections.unmodifiableMap(map));
			}
		}

		return new DataCollection(Collections.unmodifiableMap(players), Collections.unmodifiableMap(stats), ImmutableList.of(ids), ImmutableList.of(names),
				ImmutableList.of(guilds), startDates == null ? null : ImmutableList.of(startDates), ImmutableList.of(endDates), minStartDate, maxStartDate, minEndDate, maxEndDate, size);
	}

	public static DataMergeResult merge(Collection<UUID> col, DataCollection start, DataCollection end) {
		boolean useIntervals = start != null;

		if (end.containsIntervals || (useIntervals && start.containsIntervals))
			throw new IllegalArgumentException("Intervals");
		if (useIntervals && start.maxEndDate.isAfter(end.minEndDate))
			throw new IllegalArgumentException("Collision");

		int size = col.size();
		if (end.size < size)
			size = end.size;
		if (useIntervals && start.size < size)
			size = start.size;

		int[] endIndexes = new int[size];
		int[] startIndexes = useIntervals ? new int[size] : null;
		int startMissing = 0, endMissing = 0;

		int current = 0;
		for (UUID id : col) {
			Integer index1 = end.players.get(id);
			if (index1 == null) {
				endMissing++;
				continue;
			}

			if (useIntervals) {
				Integer index2 = start.players.get(id);
				if (index2 == null) {
					startMissing++;
					continue;
				}

				startIndexes[current] = index2;
			}

			endIndexes[current] = index1;
			current++;
		}

		Map<UUID, Integer> players = new HashMap<>();
		UUID[] ids = new UUID[size];
		for (int i = 0; i < size; i++) {
			UUID id = end.ids.get(endIndexes[i]);
			ids[i] = id;

			players.put(id, i);
		}

		String[] names = new String[size];
		for (int i = 0; i < size; i++)
			names[i] = end.names.get(endIndexes[i]);

		String[] guilds = new String[size];
		for (int i = 0; i < size; i++)
			guilds[i] = end.guilds.get(endIndexes[i]);

		Instant minStartDate = null, maxStartDate = null;
		Instant[] startDates = null;
		if (useIntervals) {
			startDates = new Instant[size];
			for (int i = 0; i < size; i++) {
				Instant t = start.endDates.get(startIndexes[i]);
				startDates[i] = t;

				if (minStartDate == null || t.isBefore(minStartDate))
					minStartDate = t;
				if (maxStartDate == null || t.isAfter(maxStartDate))
					maxStartDate = t;
			}
		}

		Instant minEndDate = null, maxEndDate = null;
		Instant[] endDates = new Instant[size];
		for (int i = 0; i < size; i++) {
			Instant t = end.endDates.get(endIndexes[i]);
			endDates[i] = t;

			if (minEndDate == null || t.isBefore(minEndDate))
				minEndDate = t;
			if (maxEndDate == null || t.isAfter(maxEndDate))
				maxEndDate = t;
		}

		Map<String, Map<String, ImmutableDoubleList>> stats = new HashMap<>();
		for (Entry<String, Map<String, ImmutableDoubleList>> e : end.stats.entrySet()) {
			Map<String, ImmutableDoubleList> e2 = null;
			if (useIntervals) {
				e2 = start.stats.get(e.getKey());
				if (e2 == null)
					continue;
			}

			Map<String, ImmutableDoubleList> section = new HashMap<>();

			for (Entry<String, ImmutableDoubleList> stat : e.getValue().entrySet()) {
				ImmutableDoubleList list2 = null;
				if (useIntervals) {
					list2 = e2.get(stat.getKey());
					if (list2 == null)
						continue;
				}

				ImmutableDoubleList list1 = stat.getValue();

				double[] values = new double[size];
				if (useIntervals) {
					for (int i = 0; i < size; i++)
						values[i] = list1.get(endIndexes[i]) - list2.get(startIndexes[i]);
				} else {
					for (int i = 0; i < size; i++)
						values[i] = list1.get(endIndexes[i]);
				}

				section.put(stat.getKey(), ImmutableDoubleList.of(values));
			}

			stats.put(e.getKey(), Collections.unmodifiableMap(section));
		}

		return new DataMergeResult(new DataCollection(Collections.unmodifiableMap(players), Collections.unmodifiableMap(stats), ImmutableList.of(ids), ImmutableList.of(names),
				ImmutableList.of(guilds), startDates == null ? null : ImmutableList.of(startDates), ImmutableList.of(endDates), minStartDate, maxStartDate, minEndDate, maxEndDate, size),
				startMissing, endMissing);
	}

	public static Builder builder(int capacity, boolean useIntervals) {
		return new Builder(capacity, useIntervals);
	}

	public static class Builder {
		private Map<UUID, Integer> players = new HashMap<>();

		private Map<String, Map<String, DoubleList>> stats;
		private List<UUID> ids;
		private List<String> names, guilds;
		private List<Instant> startDates, endDates;

		private Instant minStartDate, maxStartDate, minEndDate, maxEndDate;
		private int capacity, size;

		private Builder(int capacity, boolean useIntervals) {
			this.capacity = capacity;

			this.ids = new ArrayList<>(capacity);
			this.names = new ArrayList<>(capacity);
			this.guilds = new ArrayList<>(capacity);
			if (useIntervals)
				this.startDates = new ArrayList<>(capacity);
			this.endDates = new ArrayList<>(capacity);

			this.stats = new HashMap<>();
		}

		public void add(PlayerInfo info) {
			add(info.stats, info.id, info.name, info.guild, info.date);
		}

		public synchronized void add(Map<String, Map<String, Double>> stats, UUID id, String name, String guild, Instant date) {
			if (this.size >= this.capacity)
				throw new IndexOutOfBoundsException("Capacity: " + this.capacity);
			if (this.startDates != null)
				throw new UnsupportedOperationException();
			if (id == null || name == null || date == null)
				throw new IllegalArgumentException();
			if (this.players.containsKey(id))
				throw new IllegalArgumentException("Already added");

			this.players.put(id, this.size);

			if (stats != null) {
				for (Entry<String, Map<String, Double>> e : stats.entrySet()) {
					Map<String, DoubleList> section = this.stats.get(e.getKey());
					if (section == null) {
						section = new HashMap<>();
						this.stats.put(e.getKey(), section);
					}

					for (Entry<String, Double> stat : e.getValue().entrySet()) {
						DoubleList list = section.get(stat.getKey());
						if (list == null) {
							list = new DoubleList(this.capacity);
							section.put(stat.getKey(), list);
						}

						list.set(this.size, stat.getValue());
					}
				}
			}

			this.ids.add(id);
			this.names.add(name);
			this.guilds.add(guild);
			this.endDates.add(date);

			if (this.minEndDate == null || date.isBefore(this.minEndDate))
				this.minEndDate = date;
			if (this.maxEndDate == null || date.isAfter(this.maxEndDate))
				this.maxEndDate = date;

			this.size++;
		}

		public void add(PlayerInfo start, PlayerInfo end) {
			if (!start.id.equals(end.id))
				throw new IllegalArgumentException("UUID mismatch");
			add(start.stats, end.stats, end.id, end.name, end.guild, start.date, end.date);
		}

		public synchronized void add(Map<String, Map<String, Double>> startStats, Map<String, Map<String, Double>> endStats, UUID id, String name, String guild, Instant startDate, Instant endDate) {
			if (this.size >= this.capacity)
				throw new IndexOutOfBoundsException("Capacity: " + this.capacity);
			if (this.startDates == null)
				throw new UnsupportedOperationException();
			if (id == null || name == null || startDate == null || endDate == null)
				throw new IllegalArgumentException();
			if (this.players.containsKey(id))
				throw new IllegalArgumentException("Already added");

			this.players.put(id, this.size);

			if (startStats != null && endStats != null) {
				for (Entry<String, Map<String, Double>> e : endStats.entrySet()) {
					Map<String, Double> e2 = startStats.get(e.getKey());
					if (e2 == null)
						continue;

					Map<String, DoubleList> section = this.stats.get(e.getKey());
					if (section == null) {
						section = new HashMap<>();
						this.stats.put(e.getKey(), section);
					}

					for (Entry<String, Double> stat : e.getValue().entrySet()) {
						Double stat2 = e2.get(stat.getKey());
						if (stat2 == null)
							continue;

						DoubleList list = section.get(stat.getKey());
						if (list == null) {
							list = new DoubleList(this.capacity);
							section.put(stat.getKey(), list);
						}

						list.set(this.size, stat.getValue() - stat2);
					}
				}
			}

			this.ids.add(id);
			this.names.add(name);
			this.guilds.add(guild);
			this.startDates.add(startDate);
			this.endDates.add(endDate);

			if (this.minStartDate == null || startDate.isBefore(this.minStartDate))
				this.minStartDate = startDate;
			if (this.maxStartDate == null || startDate.isAfter(this.maxStartDate))
				this.maxStartDate = startDate;

			if (this.minEndDate == null || endDate.isBefore(this.minEndDate))
				this.minEndDate = endDate;
			if (this.maxEndDate == null || endDate.isAfter(this.maxEndDate))
				this.maxEndDate = endDate;

			this.size++;
		}

		public int size() {
			return this.size;
		}

		public DataCollection build() {
			Map<String, Map<String, ImmutableDoubleList>> newStats = new HashMap<>();
			for (Entry<String, Map<String, DoubleList>> e : this.stats.entrySet()) {
				Map<String, ImmutableDoubleList> section = new HashMap<>();
				for (Entry<String, DoubleList> list : e.getValue().entrySet())
					section.put(list.getKey(), list.getValue().toImmutable());
				newStats.put(e.getKey(), Collections.unmodifiableMap(section));
			}

			return new DataCollection(Collections.unmodifiableMap(this.players), Collections.unmodifiableMap(newStats), ImmutableList.copyOf(this.ids),
					ImmutableList.copyOf(this.names), ImmutableList.copyOf(this.guilds), this.startDates == null ? null : ImmutableList.copyOf(this.startDates),
					ImmutableList.copyOf(this.endDates), this.minStartDate, this.maxStartDate, this.minEndDate, this.maxEndDate, this.size);
		}
	}
}
