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
import java.util.*;

public final class GuildInfo {
	public static final URL URL_BASE;
	private static final Logger logger = Application.getLogger("GuildInfo");
	private static final JsonFactory factory = new JsonFactory();

	public final List<UUID> members;
	public final String name;

	public GuildInfo(List<UUID> members, String name) {
		this.members = members;
		this.name = name;
	}

	public static Optional<GuildInfo> get(String guildName) {
		try {
			return Optional.of(read(guildName));
		} catch (IOException e) {
			String msg = e.getMessage();
			if (msg != null && msg.startsWith("Invalid response code"))
				logger.error("Failed to get json content for guild '" + guildName + "' (" + msg + ")");
			else
				logger.error("Failed to get json content for guild '" + guildName + "'", e);
			return Optional.empty();
		}
	}

	public static GuildInfo read(String guildName) throws IOException {
		return read(DownloadUtil.appendUrlSuffix(URL_BASE, DownloadUtil.encode(guildName) + ".json"), Application.get().getConnectionConfig());
	}

	public static GuildInfo read(URL url, ConnectionConfiguration config) throws IOException {
		HttpURLConnection co = config.openHttpConnection(url);
		co.connect();

		int code = co.getResponseCode();
		if (code / 100 == 2) {
			try (JsonParser json = factory.createParser(co.getInputStream())) {
				return read(json);
			}
		} else
			throw new IOException("Invalid response code: " + code);
	}

	public static GuildInfo read(JsonParser json) throws IOException {
		if (json.nextToken() != JsonToken.START_OBJECT)
			throw new JsonParseException(json, "Expected to start a new object");

		List<UUID> members = null;
		String name = null;

		while (json.nextToken() != JsonToken.END_OBJECT) {
			String field = json.getCurrentName();

			if (field.equals("name")) {
				if (json.nextToken() != JsonToken.VALUE_STRING)
					throw new JsonParseException(json, "Field 'name' was expected to be a string");

				name = json.getValueAsString();
				continue;
			}

			if (field.equals("members")) {
				if (json.nextToken() != JsonToken.START_ARRAY)
					throw new JsonParseException(json, "Field 'members' was expected to be an array");

				List<UUID> newMembers = new ArrayList<>();

				while (json.nextToken() != JsonToken.END_ARRAY) {
					if (json.currentToken() != JsonToken.START_OBJECT)
						throw new JsonParseException(json, "Expected to start a new member object");

					while (json.nextToken() != JsonToken.END_OBJECT) {
						String field2 = json.getCurrentName();

						if (field2.equals("uuid")) {
							if (json.nextToken() != JsonToken.VALUE_STRING)
								throw new JsonParseException(json, "Field 'uuid' of a member was expected to be a string");

							newMembers.add(PlayerInfo.idFromString(json.getValueAsString()));
							continue;
						}

						json.nextToken();
						json.skipChildren();
					}
				}

				members = Collections.unmodifiableList(newMembers);
				continue;
			}

			json.nextToken();
			json.skipChildren();
		}

		if (members == null)
			throw new IllegalArgumentException("Field 'members' is missing");
		if (name == null)
			throw new IllegalArgumentException("Field 'name' is missing");

		return new GuildInfo(members, name);
	}

	static {
		try {
			URL_BASE = new URL("https://stats.epicube.fr/guild/");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
