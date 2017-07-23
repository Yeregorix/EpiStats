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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class GuildInfo {
	public static final URL URL_BASE;
	private static final Logger logger = Application.getLogger("GuildInfo");
	private static final JsonFactory factory = new JsonFactory();
	public UUID[] members;
	public String name;

	public static Optional<GuildInfo> get(String guildName) {
		try {
			GuildInfo g = new GuildInfo();
			g.read(guildName);
			return Optional.of(g);
		} catch (IOException e) {
			logger.error("Failed to get json content for guild '" + guildName + "'", e);
			return Optional.empty();
		}
	}

	public void read(String guildName) throws IOException {
		read(DownloadUtil.appendUrlSuffix(URL_BASE, DownloadUtil.encode(guildName) + ".json"), Application.get().getConnectionConfig());
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
			throw new JsonParseException(json, "Expected to start a new object");

		while (json.nextToken() != JsonToken.END_OBJECT) {
			String field = json.getCurrentName();

			if (field.equals("name")) {
				if (json.nextToken() != JsonToken.VALUE_STRING)
					throw new JsonParseException(json, "Field 'name' was expected to be a string");

				this.name = json.getValueAsString();
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

				this.members = newMembers.toArray(new UUID[newMembers.size()]);
				continue;
			}

			json.nextToken();
			json.skipChildren();
		}
	}

	static {
		try {
			URL_BASE = new URL("https://stats.epicube.fr/guild/");
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
