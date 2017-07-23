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

import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import net.smoofyuniverse.common.app.Application;
import net.smoofyuniverse.common.fxui.control.AbstractTreeCell;
import net.smoofyuniverse.common.fxui.dialog.Popup;
import net.smoofyuniverse.common.fxui.field.IntegerField;
import net.smoofyuniverse.common.logger.core.Logger;
import net.smoofyuniverse.common.util.GridUtil;
import net.smoofyuniverse.common.util.StringUtil;
import net.smoofyuniverse.epi.EpiStats;
import net.smoofyuniverse.epi.stats.DataCollection;
import net.smoofyuniverse.epi.stats.Ranking;
import net.smoofyuniverse.epi.stats.RankingList;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

public final class RankingListPanel extends GridPane {
	private static final Logger logger = Application.getLogger("RankingListPanel");

	private Label rankingsL = new Label("Catégories:"), indexL = new Label("Index:"), searchL = new Label("Rechercher:");
	private Label datesL = new Label("Dates:"), startL = new Label("Début:"), startDates = new Label(), endL = new Label("Fin:"), endDates = new Label();
	private TreeView<Object> rankings = new TreeView<>();
	private IntegerField index = new IntegerField(0);
	private TextField search = new TextField();
	private Button open = new Button("Ouvrir"), save = new Button("Sauvegarder");
	
	private RankingList list;

	private UserInterface ui;
	private EpiStats epi;

	private FileChooser openChooser = new FileChooser(), saveChooser = new FileChooser();

	public RankingListPanel(UserInterface ui) {
		this.ui = ui;
		this.epi = ui.getEpiStats();
		
		GridPane.setValignment(this.rankingsL, VPos.TOP);
		
		this.rankings.setShowRoot(false);
		
		this.open.setPrefWidth(Integer.MAX_VALUE);
		this.save.setPrefWidth(Integer.MAX_VALUE);

		this.openChooser.getExtensionFilters().addAll(new ExtensionFilter("Liste de classements", "*.rlist"), new ExtensionFilter("Représentation JSON", "*.json"));
		this.saveChooser.getExtensionFilters().addAll(new ExtensionFilter("Liste de classements", "*.rlist"), new ExtensionFilter("Représentation JSON", "*.json"), new ExtensionFilter("Représentation tabulaire", "*.csv"));

		this.rankings.setCellFactory(l -> new RankingTreeCell());
		this.rankings.getSelectionModel().selectedItemProperty().addListener((v, oldV, newV) -> {
			if (newV != null) {
				Object obj = newV.getValue();
				if (obj instanceof Ranking)
					this.ui.getRankingView().open((Ranking) obj);
			}
		});
		
		this.index.valueProperty().addListener((v, oldV, newV) -> {
			int i = newV.intValue();
			if (this.list == null) {
				if (i != 0) {
					i = 0;
					this.index.valueProperty().set(i);
				}
			} else {
				if (i < 0) {
					i = 0;
					this.index.valueProperty().set(i);
				} else if (i >= this.list.getPlayerCount()) {
					i = this.list.getPlayerCount();
					this.index.valueProperty().set(i);
				}

				this.ui.getRankingView().getSelectionModel().select(i - 1);
				this.ui.getRankingView().scrollToVisible(i - 1);
			}
		});
		
		this.search.setPromptText("Joueur");
		this.search.textProperty().addListener((v, oldV, newV) -> {
			Ranking r = this.ui.getRankingView().currentRanking().orElse(null);
			if (r == null || this.list == null)
				return;
			String name = newV.toLowerCase();
			
			int i = 0;
			Iterator<Integer> it = r.iterator();
			while (it.hasNext()) {
				int p = it.next();
				String n = this.list.getPlayer(p).name;
				if (n.toLowerCase().startsWith(name)) {
					this.index.valueProperty().set(i +1);
					break;
				}
				i++;
			}
		});

		this.open.setOnAction(a -> {
			File f = this.openChooser.showOpenDialog(this.epi.getStage());
			if (f == null)
				return;
			Path file = f.toPath();
			
			this.epi.getExecutor().submit(() -> {
				try {
					logger.debug("Reading ranking list from file ..");
					open(RankingList.read(file));
				} catch (Exception e) {
					Popup.error().title("Erreur de lecture").header("Une erreur est survenue lors de la lecture de la liste de classements").message(e).show();
					logger.error("Failed to read ranking list from file " + file.getFileName(), e);
				}
			});
		});

		this.save.setOnAction(a -> {
			if (this.list == null)
				return;

			File f = this.saveChooser.showSaveDialog(this.epi.getStage());
			if (f == null)
				return;
			Path file = f.toPath();
			
			this.epi.getExecutor().submit(() -> {
				try {
					logger.debug("Saving ranking list to file ..");
					this.list.save(file);
				} catch (Exception e) {
					Popup.error().title("Erreur de sauvegarde").header("Une erreur est survenue lors de la sauvegarde de la liste de classements").message(e).show();
					logger.error("Failed to save ranking list to file " + file.getFileName(), e);
				}
			});
		});
		
		setVgap(4);
		setHgap(4);

		add(UserInterface.title("4 - Résultats:"), 0, 0, 4, 1);

		add(this.datesL, 0, 1);

		add(this.startL, 1, 1);
		add(this.startDates, 2, 1, 3, 1);

		add(this.endL, 1, 2);
		add(this.endDates, 2, 2, 3, 1);

		add(this.rankingsL, 0, 3);
		add(this.rankings, 1, 3, 4, 1);

		add(this.indexL, 0, 4);
		add(this.index, 1, 4, 2, 1);

		add(this.searchL, 3, 4);
		add(this.search, 4, 4);

		add(this.open, 0, 5, 3, 1);
		add(this.save, 3, 5, 2, 1);

		getColumnConstraints().addAll(GridUtil.createColumn(15), GridUtil.createColumn(10), GridUtil.createColumn(25), GridUtil.createColumn(15), GridUtil.createColumn(35));
	}
	
	public void setSelectedIndex(int v) {
		this.index.valueProperty().set(v +1);
	}
	
	public Optional<RankingList> currentList() {
		return Optional.ofNullable(this.list);
	}
	
	public void open(RankingList list) {
		if (Platform.isFxApplicationThread()) {
			this.list = list;
			this.rankings.getSelectionModel().clearSelection();
			
			if (list == null) {
				this.rankings.setRoot(null);
				this.startDates.setText(null);
				this.endDates.setText(null);
			} else {
				logger.info("Loading ranking list in UI ..");

				Map<String, List<Ranking>> groups = new TreeMap<>();
				for (Ranking r : list.getRankings()) {
					int i = r.name.indexOf('_');
					String parent = i == -1 ? r.name : r.name.substring(0, i);
					
					List<Ranking> l = groups.get(parent);
					if (l == null) {
						l = new ArrayList<>();
						groups.put(parent, l);
					}
					l.add(r);
				}
				
				TreeItem<Object> root = new TreeItem<>();
				root.setExpanded(true);
				for (Entry<String, List<Ranking>> e : groups.entrySet()) {
					List<Ranking> l = e.getValue();
					TreeItem<Object> item;
					if (l.size() == 1)
						item = new TreeItem<>(l.get(0));
					else {
						item = new TreeItem<>(e.getKey());
						for (Ranking r : l)
							item.getChildren().add(new TreeItem<>(r));
					}
					root.getChildren().add(item);
				}
				
				this.rankings.setRoot(root);

				DataCollection col = list.getCollection();
				if (col.containsIntervals())
					this.startDates.setText("Du " + StringUtil.DATETIME_FORMAT.format(col.getMinStartDate()) + " au " + StringUtil.DATETIME_FORMAT.format(col.getMaxStartDate()));
				else
					this.startDates.setText("Depuis toujours");
				this.endDates.setText("Du " + StringUtil.DATETIME_FORMAT.format(col.getMinEndDate()) + " au " + StringUtil.DATETIME_FORMAT.format(col.getMaxEndDate()));
			}
		} else
			Platform.runLater(() -> open(list));
	}
	
	private class RankingTreeCell extends AbstractTreeCell<Object> {
		private Label label = new Label();

		@Override
		protected Node getContent() {
			Object item = getTreeItem().getValue();
			this.label.setText(item instanceof Ranking ? ((Ranking) item).name : item.toString());
			return this.label;
		}
	}
}
