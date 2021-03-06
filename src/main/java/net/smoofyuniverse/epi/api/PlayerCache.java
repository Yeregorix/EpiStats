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

package net.smoofyuniverse.epi.api;

import net.smoofyuniverse.common.app.App;
import net.smoofyuniverse.logger.core.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class PlayerCache {
	public static final int CURRENT_VERSION = 2, MINIMUM_VERSION = 1;

	private static final Logger logger = App.getLogger("PlayerCache");
	
	public final Path directory;
	
	public PlayerCache(Path dir) {
		this.directory = dir;
		
		try {
			Files.createDirectories(dir);
		} catch (IOException ignored) {}
	}
	
	public List<UUID> list() {
		List<UUID> list = new ArrayList<>();

		try (DirectoryStream<Path> st = Files.newDirectoryStream(this.directory)) {
			for (Path file : st) {
				String fn = file.getFileName().toString();
				if (fn.endsWith(".pdat")) {
					try {
						list.add(UUID.fromString(fn.substring(0, fn.length() -5)));
					} catch (IllegalArgumentException ignored) {}
				}
			}
		} catch (Exception e) {
			logger.warn("Failed to list files", e);
		}
		
		return list;
	}
	
	public boolean contains(UUID id) {
		return Files.exists(this.directory.resolve(id + ".pdat"));
	}
	
	public Optional<PlayerInfo> read(UUID id) {
		Path file = this.directory.resolve(id + ".pdat");
		if (!Files.exists(file))
			return Optional.empty();
		
		try {
			return Optional.of(read(file));
		} catch (IOException e) {
			logger.warn("Failed to read file " + file.getFileName(), e);
			return Optional.empty();
		}
	}
	
	public PlayerInfo read(Path file) throws IOException {
		try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
			return read(in);
		}
	}
	
	public PlayerInfo read(DataInputStream in) throws IOException {
		int version = in.readInt();
		if (version > CURRENT_VERSION || version < MINIMUM_VERSION)
			throw new IOException("Invalid format version: " + version);

		if (version >= 2)
			in = new DataInputStream(new GZIPInputStream(in));

		UUID id = new UUID(in.readLong(), in.readLong());
		String name = in.readUTF();
		String guild = in.readUTF();
		if (guild.isEmpty())
			guild = null;
		Instant date = Instant.ofEpochMilli(in.readLong());

		Map<String, Map<String, Double>> stats = new HashMap<>();
		int maps = in.readInt();
		for (int i = 0; i < maps; i++) {
			Map<String, Double> map = new HashMap<>();
			stats.put(in.readUTF(), map);
			int count = in.readInt();
			for (int y = 0; y < count; y++)
				map.put(in.readUTF(), in.readDouble());
		}

		return new PlayerInfo(Collections.unmodifiableMap(stats), id, name, guild, date);
	}
	
	public void save(PlayerInfo p) {
		Path file = this.directory.resolve(p.id + ".pdat");
		
		try {
			save(p, file);
		} catch (IOException e) {
			logger.warn("Failed to save file " + file.getFileName(), e);
		}
	}
	
	public void save(PlayerInfo p, Path file) throws IOException {
		try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file))) {
			save(p, out);
		}
	}
	
	public void save(PlayerInfo p, DataOutputStream out) throws IOException {
		out.writeInt(CURRENT_VERSION);

		GZIPOutputStream zip = new GZIPOutputStream(out);
		out = new DataOutputStream(zip);
		
		out.writeLong(p.id.getMostSignificantBits());
		out.writeLong(p.id.getLeastSignificantBits());
		out.writeUTF(p.name);
		out.writeUTF(p.guild == null ? "" : p.guild);
		out.writeLong(p.date.toEpochMilli());

		out.writeInt(p.stats.size());
		for (Entry<String, Map<String, Double>> e : p.stats.entrySet()) {
			out.writeUTF(e.getKey());
			Map<String, Double> map = e.getValue();
			out.writeInt(map.size());
			for (Entry<String, Double> stat : map.entrySet()) {
				out.writeUTF(stat.getKey());
				out.writeDouble(stat.getValue());
			}
		}

		zip.finish();
	}
}
