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

package net.smoofyuniverse.epi.ui;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import net.smoofyuniverse.common.app.App;
import net.smoofyuniverse.common.util.GridUtil;
import net.smoofyuniverse.epi.EpiStats;
import net.smoofyuniverse.epi.api.PlayerCache;
import net.smoofyuniverse.epi.stats.ObjectList;
import net.smoofyuniverse.logger.core.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UserInterface extends GridPane {
	private static final Logger logger = App.getLogger("UserInterface");

	private ConnectionConfigPanel connectionConfigPanel;
	private ObjectListPanel objectListPanel;
	private DataCollectionPanel dataCollectionPanel;
	private GenerationPanel generationPanel;
	private RankingListPanel rankingListPanel;
	private RankingView rankingView;

	private EpiStats epi;
	private Path saveFile;

	public UserInterface(EpiStats epi, Path saveFile, ObjectList list, PlayerCache cache) {
		this.epi = epi;
		this.saveFile = saveFile;

		this.connectionConfigPanel = new ConnectionConfigPanel(this);
		this.objectListPanel = new ObjectListPanel(this, list);
		this.dataCollectionPanel = new DataCollectionPanel(this, cache);
		this.generationPanel = new GenerationPanel(this);
		this.rankingListPanel = new RankingListPanel(this);
		this.rankingView = new RankingView(this);

		loadUI();

		this.connectionConfigPanel.getUserAgent().addListener(this::saveUI);
		this.dataCollectionPanel.getCacheAge().addListener(this::saveUI);
		this.generationPanel.getEditor().addListener(this::saveUI);

		setVgap(10);
		setHgap(10);
		setPadding(new Insets(10));

		add(this.connectionConfigPanel, 0, 0);
		add(this.objectListPanel, 0, 1);
		add(this.dataCollectionPanel, 0, 2);
		add(this.generationPanel, 0, 3);
		add(this.rankingListPanel, 0, 4);

		add(this.rankingView, 1, 0, 1, 5);
		
		getColumnConstraints().addAll(GridUtil.createColumn(60), GridUtil.createColumn(40));
		getRowConstraints().addAll(GridUtil.createRow(), GridUtil.createRow(), GridUtil.createRow(), GridUtil.createRow(Priority.ALWAYS), GridUtil.createRow(30));
	}

	private void loadUI() {
		if (!Files.exists(this.saveFile))
			return;
		try (DataInputStream in = new DataInputStream(Files.newInputStream(this.saveFile))) {
			this.generationPanel.getEditor().set(in.readUTF());
			this.dataCollectionPanel.getCacheAge().set(in.readUTF());
			try {
				this.connectionConfigPanel.getUserAgent().set(in.readUTF());
			} catch (EOFException ignored) {
			}
		} catch (IOException e) {
			logger.warn("Failed to load ui from file", e);
		}
	}

	private <T> void saveUI(ObservableValue<? extends T> observable, T oldValue, T newValue) {
		saveUI();
	}

	private void saveUI() {
		try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(this.saveFile))) {
			out.writeUTF(this.generationPanel.getEditor().get());
			out.writeUTF(this.dataCollectionPanel.getCacheAge().get());
			out.writeUTF(this.connectionConfigPanel.getUserAgent().get());
		} catch (IOException e) {
			logger.warn("Failed to save ui to file", e);
		}
	}

	public EpiStats getEpiStats() {
		return this.epi;
	}

	public ObjectListPanel getObjectListPanel() {
		return this.objectListPanel;
	}

	public DataCollectionPanel getDataCollectionPanel() {
		return this.dataCollectionPanel;
	}

	public GenerationPanel getGenerationPanel() {
		return this.generationPanel;
	}

	public RankingListPanel getRankingListPanel() {
		return this.rankingListPanel;
	}

	public RankingView getRankingView() {
		return this.rankingView;
	}

	protected static Label title(String content) {
		Label l = new Label(content);
		l.setFont(Font.font(l.getFont().getName(), FontWeight.BOLD, 16));
		return l;
	}
}
