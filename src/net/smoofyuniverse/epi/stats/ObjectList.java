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
import net.smoofyuniverse.common.fxui.task.ObservableTask;
import net.smoofyuniverse.epi.api.GuildInfo;
import net.smoofyuniverse.epi.api.PlayerInfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ObjectList {
	public static final int CURRENT_VERSION = 1, MINIMUM_VERSION = 1;
	
	private static final JsonFactory factory = new JsonFactory();
	
	public final Set<String> guilds = new HashSet<>();
	public final Set<UUID> players = new HashSet<>();
	public final Path defaultFile;
	
	public ObjectList(Path file) {
		this.defaultFile = file;
	}
	
	public void clear() {
		this.guilds.clear();
		this.players.clear();
	}
	
	public void read() throws IOException {
		read(this.defaultFile);
	}
	
	public void read(Path file) throws IOException {
		clear();
		merge(file);
	}
	
	public void merge(Path file) throws IOException {
		if (!Files.exists(file))
			return;
		String fn = file.getFileName().toString();
		
		if (fn.endsWith(".json")) {
			try (JsonParser json = factory.createParser(Files.newInputStream(file))) {
				mergeJSON(json);
			}
		} else {
			try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
				merge(in);
			}
		}
	}
	
	public void merge(DataInputStream in) throws IOException {
		int version = in.readInt();
		if (version > CURRENT_VERSION || version < MINIMUM_VERSION)
			throw new IOException("Invalid format version: " + version);
		
		int count = in.readInt();
		for (int i = 0; i < count; i++)
			this.guilds.add(in.readUTF());
		
		count = in.readInt();
		for (int i = 0; i < count; i++)
			this.players.add(new UUID(in.readLong(), in.readLong()));
	}
	
	public void mergeJSON(JsonParser json) throws IOException {
		if (json.nextToken() != JsonToken.START_OBJECT)
			throw new JsonParseException(json, "Expected to start a new object");
		
		boolean versionCheck = true;
		while (json.nextToken() != JsonToken.END_OBJECT) {
			String field = json.getCurrentName();
			
			if (versionCheck) {
				if (!field.equals("format_version") || json.nextToken() != JsonToken.VALUE_NUMBER_INT)
					throw new IOException("Format version not provided");
				
				int version = json.getIntValue();
				if (version > CURRENT_VERSION || version < MINIMUM_VERSION)
					throw new IOException("Invalid format version: " + version);
				
				versionCheck = false;
				continue;
			}
			
			if (field.equals("guilds")) {
				if (json.nextToken() != JsonToken.START_ARRAY)
					throw new JsonParseException(json, "Field 'guilds' was expected to be an array");
				
				while (json.nextToken() != JsonToken.END_ARRAY) {
					String g = json.getValueAsString();
					if (g != null)
						this.guilds.add(g);
				}
				continue;
			}
			
			if (field.equals("players")) {
				if (json.nextToken() != JsonToken.START_ARRAY)
					throw new JsonParseException(json, "Field 'players' was expected to be an array");
				
				while (json.nextToken() != JsonToken.END_ARRAY) {
					String p = json.getValueAsString();
					if (p != null) {
						try {
							this.players.add(UUID.fromString(p));
						} catch (Exception ignored) {}
					}
				}
				continue;
			}
			
			json.nextToken();
			json.skipChildren();
		}
	}
	
	public void save() throws IOException {
		save(this.defaultFile);
	}
	
	public void save(Path file) throws IOException {
		String fn = file.getFileName().toString();
		
		if (fn.endsWith(".json")) {
			try (JsonGenerator json = factory.createGenerator(Files.newOutputStream(file))) {
				json.useDefaultPrettyPrinter();
				saveJSON(json);
			}
		} else {
			try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file))) {
				save(out);
			}
		}
	}
	
	public void save(DataOutputStream out) throws IOException {
		out.writeInt(CURRENT_VERSION);
		
		out.writeInt(this.guilds.size());
		for (String g : this.guilds)
			out.writeUTF(g);
		
		out.writeInt(this.players.size());
		for (UUID p : this.players) {
			out.writeLong(p.getMostSignificantBits());
			out.writeLong(p.getLeastSignificantBits());
		}
	}
	
	public void saveJSON(JsonGenerator json) throws IOException {
		json.writeStartObject();
		
		json.writeFieldName("format_version");
		json.writeNumber(CURRENT_VERSION);
		
		json.writeFieldName("guilds");
		json.writeStartArray();
		for (String g : this.guilds)
			json.writeString(g);
		json.writeEndArray();
		
		json.writeFieldName("players");
		json.writeStartArray();
		for (UUID p : this.players)
			json.writeString(p.toString());
		json.writeEndArray();
		
		json.writeEndObject();
	}
	
	public void refresh(ObservableTask task) {
		int progress, total;
		
		task.setTitle("Collecte des données des guildes ..");
		task.setProgress(0);
		progress = 0;
		total = this.guilds.size();
		
		Set<GuildInfo> newGuilds = new HashSet<>();
		
		for (String name : this.guilds) {
			if (task.isCancelled())
				return;
			task.setMessage("Guilde: " + name);
			GuildInfo g = GuildInfo.get(name).orElse(null);
			if (g != null) {
				newGuilds.add(g);
				for (UUID id : g.members)
					this.players.remove(id);
			}
			task.setProgress(++progress / (double) total);
		}
		this.guilds.clear();
		
		task.setTitle("Collecte des données des joueurs ..");
		task.setProgress(0);
		progress = 0;
		total = this.players.size();
		
		Set<PlayerInfo> newPlayers = new HashSet<>();
		
		for (UUID id : this.players) {
			if (task.isCancelled())
				return;
			task.setMessage("Joueur: " + id);
			PlayerInfo p = PlayerInfo.get(id, false).orElse(null);
			if (p != null) {
				if (p.guild == null)
					newPlayers.add(p);
				else {
					GuildInfo g = GuildInfo.get(p.guild).orElse(null);
					if (g == null)
						newPlayers.add(p);
					else
						newGuilds.add(g);
				}
			}
			task.setProgress(++progress / (double) total);
		}
		this.players.clear();
		
		task.setTitle("Mise à jour de la liste des guildes ..");
		task.setProgress(0);
		progress = 0;
		total = newGuilds.size();
		
		for (GuildInfo g : newGuilds) {
			if (task.isCancelled())
				return;
			task.setMessage("Guilde: " + g.name);
			addGuild(g);
			task.setProgress(++progress / (double) total);
		}
		
		task.setTitle("Mise à jour de la liste des joueurs ..");
		task.setProgress(0);
		progress = 0;
		total = newPlayers.size();
		
		for (PlayerInfo p : newPlayers) {
			if (task.isCancelled())
				return;
			task.setMessage("Joueur: " + p.name);
			addPlayer(p);
			task.setProgress(++progress / (double) total);
		}
	}
	
	public boolean addPlayer(PlayerInfo p) {
		return this.players.add(p.id);
	}
	
	public boolean addGuild(GuildInfo g) {
		if (this.guilds.add(g.name.toLowerCase())) {
			for (UUID id : g.members)
				this.players.add(id);
			return true;
		}
		return false;
	}
	
	public boolean removePlayer(PlayerInfo p) {
		return this.players.remove(p.id);
	}
	
	public boolean removeGuild(GuildInfo g) {
		if (this.guilds.remove(g.name.toLowerCase())) {
			for (UUID id : g.members)
				this.players.remove(id);
			return true;
		}
		return false;
	}
}
