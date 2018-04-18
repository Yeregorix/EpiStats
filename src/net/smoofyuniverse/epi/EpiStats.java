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

import com.fasterxml.jackson.core.JsonFactory;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import net.smoofyuniverse.common.app.App;
import net.smoofyuniverse.common.app.Application;
import net.smoofyuniverse.common.app.Arguments;
import net.smoofyuniverse.epi.api.PlayerCache;
import net.smoofyuniverse.epi.stats.ObjectList;
import net.smoofyuniverse.epi.ui.UserInterface;

import java.nio.file.Path;
import java.util.concurrent.Executors;

public class EpiStats extends Application {
	public static final JsonFactory JSON_FACTORY = new JsonFactory();

	public EpiStats(Arguments args) {
		super(args, "EpiStats", "1.6.1");
	}

	@Override
	public void init() {
		initServices(Executors.newSingleThreadExecutor());

		if (this.UIEnabled) {
			App.runLater(() -> {
				initStage(1000, 800, true, "favicon.png");

				Path dir = getWorkingDirectory();
				setScene(new UserInterface(this, dir.resolve("ui.dat"), new ObjectList(dir.resolve("objects.olist")), new PlayerCache(dir.resolve("cache/")))).show();

				Scene sc = getStage().get().getScene();
				sc.setOnKeyPressed((e) -> {
					if (e.getCode() == KeyCode.ENTER) {
						Node n = sc.getFocusOwner();
						if (n instanceof Button)
							((Button) n).fire();
					}
				});
			});

			checkForUpdate();
		} else {
			skipStage();
			checkForUpdate();

			getLogger().info("This functionality is not ready yet.");

			shutdown();
		}
	}

	public static void main(String[] args) {
		new EpiStats(Arguments.parse(args)).launch();
	}
}
