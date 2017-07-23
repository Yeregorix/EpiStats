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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import net.smoofyuniverse.common.app.Application;
import net.smoofyuniverse.common.download.ConnectionConfiguration;
import net.smoofyuniverse.common.logger.core.Logger;
import net.smoofyuniverse.common.util.DownloadUtil;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlayerInfo {
	public static final URL URL_BASE;
	private static final Logger logger = Application.getLogger("GuildInfo");
	private static final JsonFactory factory = new JsonFactory();
	public Map<String, Map<String, Double>> startStats, endStats;
	public String name, guild;
	public UUID id;
	public Instant startDate, endDate;

	// Not a deep copy
	public PlayerInfo copy() {
		PlayerInfo p = new PlayerInfo();

		p.startDate = this.startDate;
		p.endDate = this.endDate;
		p.id = this.id;
		p.name = this.name;
		p.guild = this.guild;
		p.startStats = this.startStats;
		p.endStats = this.endStats;

		return p;
	}

	public static Optional<PlayerInfo> get(String playerName, boolean stats) {
		try {
			PlayerInfo p = new PlayerInfo();
			p.read(playerName, stats);
			p.endDate = Instant.now();
			return Optional.of(p);
		} catch (IOException e) {
			logger.error("Failed to get json content for player '" + playerName + "'", e);
			return Optional.empty();
		}
	}

	public void read(String playerName, boolean stats) throws IOException {
		read(DownloadUtil.appendUrlSuffix(URL_BASE, playerName + (stats ? ".json?with=stats" : ".json")), Application.get().getConnectionConfig());
	}

	private void read(URL url, ConnectionConfiguration config) throws IOException {
		HttpURLConnection co = config.openHttpConnection(url);
		co.connect();

		int code = co.getResponseCode();
		if (code / 100 == 2) {
			try (JsonParser json = factory.createParser(co.getInputStream())) {
				read(json);
			}
		} else
			throw new IOException("Invalid response code: " + code);
	}

	public void read(JsonParser json) throws IOException {
		if (json.nextToken() != JsonToken.START_OBJECT)
			throw new IOException("Expected to start an new object");

		while (json.nextToken() != JsonToken.END_OBJECT) {
			String field = json.getCurrentName();

			if (field.equals("player_uuid")) {
				if (json.nextToken() != JsonToken.VALUE_STRING)
					throw new JsonParseException(json, "Field 'player_uuid' was expected to be a string");

				this.id = idFromString(json.getValueAsString());
				continue;
			}

			if (field.equals("player_name")) {
				if (json.nextToken() != JsonToken.VALUE_STRING)
					throw new JsonParseException(json, "Field 'player_name' was expected to be a string");

				this.name = json.getValueAsString();
				continue;
			}

			if (field.equals("guild")) {
				if (json.nextToken() == JsonToken.VALUE_NULL) {
					this.guild = null;
					continue;
				}

				if (json.currentToken() != JsonToken.START_OBJECT)
					throw new JsonParseException(json, "Field 'guild' was expected to be an object");

				while (json.nextToken() != JsonToken.END_OBJECT) {
					String field2 = json.getCurrentName();

					if (field2.equals("name")) {
						if (json.nextToken() != JsonToken.VALUE_STRING)
							throw new JsonParseException(json, "Field 'name' of the guild was expected to be a string");

						this.guild = json.getValueAsString();
						continue;
					}

					json.nextToken();
					json.skipChildren();
				}
				continue;
			}

			if (field.equals("stats")) {
				if (json.nextToken() != JsonToken.START_OBJECT)
					throw new JsonParseException(json, "Field 'stats' was expected to be an object");

				this.endStats = new HashMap<>();

				while (json.nextToken() != JsonToken.END_OBJECT) {
					Map<String, Double> map = new HashMap<>();
					this.endStats.put(json.getCurrentName(), map);

					if (json.nextToken() != JsonToken.START_OBJECT)
						throw new JsonParseException(json, "Subfield in 'stats' was expected to be an object");

					while (json.nextToken() != JsonToken.END_OBJECT) {
						String field2 = json.getCurrentName();
						json.nextToken();
						map.put(field2, json.getDoubleValue());
					}
				}
				continue;
			}

			json.nextToken();
			json.skipChildren();
		}
	}

	public static UUID idFromString(String v) {
		return UUID.fromString(v.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
	}

	public static Optional<PlayerInfo> get(UUID playerId, boolean stats) {
		try {
			PlayerInfo p = new PlayerInfo();
			p.read(playerId, stats);
			p.endDate = Instant.now();
			return Optional.of(p);
		} catch (IOException e) {
			logger.error("Failed to get json content for player '" + playerId + "'", e);
			return Optional.empty();
		}
	}

	public void read(UUID playerId, boolean stats) throws IOException {
		read(DownloadUtil.appendUrlSuffix(URL_BASE, idToString(playerId) + (stats ? ".json?with=stats" : ".json")), Application.get().getConnectionConfig());
	}

	public static String idToString(UUID id) {
		return id.toString().replace("-", "");
	}

	static {
		try {
			URL_BASE = new URL("https://stats.epicube.fr/player/");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
