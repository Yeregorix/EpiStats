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

import net.smoofyuniverse.epi.api.PlayerInfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class DataCollection {
	public static final int FORMAT_VERSION = 2;

	private Map<UUID, PlayerInfo> idsToPlayers = new HashMap<>();
	private PlayerInfo[] players;

	private Instant minStartDate, maxStartDate, minEndDate, maxEndDate;
	private boolean containsIntervals;

	public DataCollection(PlayerInfo[] players) {
		if (players.length == 0)
			throw new IllegalArgumentException("Empty array");

		this.containsIntervals = players[0].startDate != null;

		for (PlayerInfo p : players) {
			if (this.containsIntervals == (p.startDate == null))
				throw new IllegalArgumentException("Array must be homogeneous");

			if (p.startDate != null) {
				if (this.minStartDate == null || p.startDate.isBefore(this.minStartDate))
					this.minStartDate = p.startDate;
				if (this.maxStartDate == null || p.startDate.isAfter(this.maxStartDate))
					this.maxStartDate = p.startDate;
			}

			if (p.endDate != null) {
				if (this.minEndDate == null || p.endDate.isBefore(this.minEndDate))
					this.minEndDate = p.endDate;
				if (this.maxEndDate == null || p.endDate.isAfter(this.maxEndDate))
					this.maxEndDate = p.endDate;
			}

			this.idsToPlayers.put(p.id, p);
		}

		this.players = players;
	}

	public boolean containsIntervals() {
		return this.containsIntervals;
	}

	public Instant getMinStartDate() {
		if (this.minStartDate == null)
			throw new NoSuchElementException();
		return this.minStartDate;
	}

	public Instant getMaxStartDate() {
		if (this.maxStartDate == null)
			throw new NoSuchElementException();
		return this.maxStartDate;
	}

	public Instant getMinEndDate() {
		if (this.minEndDate == null)
			throw new NoSuchElementException();
		return this.minEndDate;
	}

	public Instant getMaxEndDate() {
		if (this.maxEndDate == null)
			throw new NoSuchElementException();
		return this.maxEndDate;
	}

	public int getPlayerCount() {
		return this.players.length;
	}

	public PlayerInfo getPlayer(int p) {
		return this.players[p];
	}

	public Optional<PlayerInfo> getPlayer(UUID id) {
		return Optional.ofNullable(this.idsToPlayers.get(id));
	}

	public void save(Path file) throws IOException {
		try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file))) {
			save(out);
		}
	}

	public void save(DataOutputStream out) throws IOException {
		out.writeInt(FORMAT_VERSION);

		GZIPOutputStream zip = new GZIPOutputStream(out);
		out = new DataOutputStream(zip);

		out.writeInt(this.players.length);
		for (PlayerInfo p : this.players) {
			if (p.startDate != null || p.startStats != null)
				throw new IllegalArgumentException("Intervals are not saved");

			out.writeLong(p.id.getMostSignificantBits());
			out.writeLong(p.id.getLeastSignificantBits());
			out.writeUTF(p.name);
			out.writeUTF(p.guild == null ? "" : p.guild);
			out.writeLong(p.endDate.toEpochMilli());

			out.writeInt(p.endStats.size());
			for (Map.Entry<String, Map<String, Double>> e : p.endStats.entrySet()) {
				out.writeUTF(e.getKey());
				Map<String, Double> map = e.getValue();
				out.writeInt(map.size());
				for (Map.Entry<String, Double> stat : map.entrySet()) {
					out.writeUTF(stat.getKey());
					out.writeDouble(stat.getValue());
				}
			}
		}

		zip.finish();
	}

	public static DataCollection read(Path file) throws IOException {
		String fn = file.getFileName().toString();
		if (!Files.exists(file))
			throw new FileNotFoundException(fn);

		try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
			return read(in);
		}
	}

	public static DataCollection read(DataInputStream in) throws IOException {
		int version = in.readInt();
		boolean old1 = version == 1;
		if (version != FORMAT_VERSION && !old1)
			throw new IOException("Invalid format version: " + version);

		if (!old1)
			in = new DataInputStream(new GZIPInputStream(in));

		PlayerInfo[] players = new PlayerInfo[in.readInt()];
		for (int i = 0; i < players.length; i++) {
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

			players[i] = new PlayerInfo(Collections.unmodifiableMap(stats), name, guild, id, date);
		}

		return new DataCollection(players);
	}
}
