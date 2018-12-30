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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import net.smoofyuniverse.common.app.App;
import net.smoofyuniverse.common.download.ConnectionConfiguration;
import net.smoofyuniverse.common.util.DownloadUtil;
import net.smoofyuniverse.epi.EpiStats;
import net.smoofyuniverse.logger.core.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.*;

public class PlayerInfo {
	public static final UUID EMPTY_UUID = new UUID(0, 0);

	public static final URL URL_BASE;
	private static final Logger logger = App.getLogger("PlayerInfo");

	public final Map<String, Map<String, Double>> stats;
	public final UUID id;
	public final String name, guild;
	public final Instant date;

	public PlayerInfo(String name, UUID id, Instant date) {
		this(null, id, name, null, date);
	}

	public PlayerInfo(Map<String, Map<String, Double>> stats, UUID id, String name, String guild, Instant date) {
		this.stats = stats;
		this.id = id;
		this.name = name;
		this.guild = guild;
		this.date = date;
	}

	public static Optional<PlayerInfo> get(String playerName, ConnectionConfiguration config, boolean stats) {
		try {
			return Optional.of(read(playerName, config, stats));
		} catch (IOException e) {
			String msg = e.getMessage();
			if (msg != null && msg.startsWith("Invalid response code"))
				logger.error("Failed to get json content for player '" + playerName + "' (" + msg + ")");
			else
				logger.error("Failed to get json content for player '" + playerName + "'", e);
			return Optional.empty();
		}
	}

	public static PlayerInfo read(String playerName, ConnectionConfiguration config, boolean stats) throws IOException {
		return read(DownloadUtil.appendUrlSuffix(URL_BASE, playerName + (stats ? ".json?with=stats" : ".json")), config, Instant.now(), stats);
	}

	public static PlayerInfo read(URL url, ConnectionConfiguration config, Instant date, boolean stats) throws IOException {
		HttpURLConnection co = config.openHttpConnection(url);
		co.connect();

		int code = co.getResponseCode();
		if (code / 100 == 2) {
			try (JsonParser json = EpiStats.JSON_FACTORY.createParser(co.getInputStream())) {
				return read(json, date, stats);
			}
		} else
			throw new IOException("Invalid response code: " + code);
	}

	public static PlayerInfo read(JsonParser json, Instant date, boolean stats) throws IOException {
		if (json.nextToken() != JsonToken.START_OBJECT)
			throw new IOException("Expected to start an new object");

		Map<String, Map<String, Double>> statsMap = null;
		String name = null, guild = null;
		UUID id = null;

		while (json.nextToken() != JsonToken.END_OBJECT) {
			String field = json.getCurrentName();

			if (field.equals("player_uuid")) {
				if (json.nextToken() != JsonToken.VALUE_STRING)
					throw new JsonParseException(json, "Field 'player_uuid' was expected to be a string");

				id = idFromString(json.getValueAsString());
				continue;
			}

			if (field.equals("player_name")) {
				if (json.nextToken() != JsonToken.VALUE_STRING)
					throw new JsonParseException(json, "Field 'player_name' was expected to be a string");

				name = json.getValueAsString();
				continue;
			}

			if (field.equals("guild")) {
				if (json.nextToken() == JsonToken.VALUE_NULL) {
					guild = null;
					continue;
				}

				if (json.currentToken() != JsonToken.START_OBJECT)
					throw new JsonParseException(json, "Field 'guild' was expected to be an object");

				while (json.nextToken() != JsonToken.END_OBJECT) {
					String field2 = json.getCurrentName();

					if (field2.equals("name")) {
						if (json.nextToken() != JsonToken.VALUE_STRING)
							throw new JsonParseException(json, "Field 'name' of the guild was expected to be a string");

						guild = json.getValueAsString();
						continue;
					}

					json.nextToken();
					json.skipChildren();
				}
				continue;
			}

			if (stats && field.equals("stats")) {
				if (json.nextToken() != JsonToken.START_OBJECT)
					throw new JsonParseException(json, "Field 'stats' was expected to be an object");

				statsMap = new HashMap<>();

				while (json.nextToken() != JsonToken.END_OBJECT) {
					Map<String, Double> map = new HashMap<>();
					statsMap.put(json.getCurrentName(), Collections.unmodifiableMap(map));

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

		if (stats && statsMap == null)
			throw new IllegalArgumentException("Field 'stats' is missing");
		if (name == null)
			throw new IllegalArgumentException("Field 'name' is missing");
		if (id == null)
			throw new IllegalArgumentException("Field 'player_uuid' is missing");

		return new PlayerInfo(stats ? Collections.unmodifiableMap(statsMap) : null, id, name, guild, date);
	}

	public static UUID idFromString(String v) {
		return UUID.fromString(v.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
	}

	public static Optional<PlayerInfo> get(UUID playerId, ConnectionConfiguration config, boolean stats) {
		try {
			return Optional.of(read(playerId, config, stats));
		} catch (IOException e) {
			String msg = e.getMessage();
			if (msg != null && msg.startsWith("Invalid response code"))
				logger.error("Failed to get json content for player '" + playerId + "' (" + msg + ")");
			else
				logger.error("Failed to get json content for player '" + playerId + "'", e);
			return Optional.empty();
		}
	}

	public static PlayerInfo read(UUID playerId, ConnectionConfiguration config, boolean stats) throws IOException {
		return read(DownloadUtil.appendUrlSuffix(URL_BASE, idToString(playerId) + (stats ? ".json?with=stats" : ".json")), config, Instant.now(), stats);
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
