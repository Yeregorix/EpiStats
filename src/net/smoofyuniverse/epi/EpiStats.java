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

package net.smoofyuniverse.epi;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import net.smoofyuniverse.common.app.Application;
import net.smoofyuniverse.common.app.Arguments;
import net.smoofyuniverse.common.util.ResourceUtil;
import net.smoofyuniverse.epi.api.PlayerCache;
import net.smoofyuniverse.epi.stats.ObjectList;
import net.smoofyuniverse.epi.ui.UserInterface;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public class EpiStats extends Application {
	private ObjectList objectList;
	private PlayerCache cache;
	
	public EpiStats(Arguments args) {
		super(args, "EpiStats", "1.3.2");
		initServices(Executors.newSingleThreadExecutor());

		Path dir = getWorkingDirectory();
		this.objectList = new ObjectList(dir.resolve("objects.olist"));
		try {
			this.objectList.read();
		} catch (IOException e) {
			getLogger().warn("Failed to read object list from file objects.olist", e);
		}

		this.cache = new PlayerCache(dir.resolve("cache/"));

		Platform.runLater(() -> {
			initStage(1000, 600, true, ResourceUtil.loadImage("favicon.png"));
			setScene(new UserInterface(this, dir.resolve("ui.dat"))).show();
			Scene sc = getStage().getScene();
			sc.setOnKeyPressed((e) -> {
				if (e.getCode() == KeyCode.ENTER) {
					Node n = sc.getFocusOwner();
					if (n instanceof Button)
						((Button) n).fire();
				}
			});
		});

		checkForUpdate();
	}

	public static void main(String[] args) {
		new EpiStats(Arguments.parse(args));
	}
	
	public ObjectList getObjectList() {
		return this.objectList;
	}
	
	public PlayerCache getCache() {
		return this.cache;
	}
}
