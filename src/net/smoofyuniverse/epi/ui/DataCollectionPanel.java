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
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import net.smoofyuniverse.common.app.Application;
import net.smoofyuniverse.common.fxui.dialog.Popup;
import net.smoofyuniverse.common.fxui.task.ObservableTask;
import net.smoofyuniverse.common.logger.core.Logger;
import net.smoofyuniverse.common.util.GridUtil;
import net.smoofyuniverse.common.util.StringUtil;
import net.smoofyuniverse.epi.EpiStats;
import net.smoofyuniverse.epi.api.PlayerCache;
import net.smoofyuniverse.epi.api.PlayerInfo;
import net.smoofyuniverse.epi.stats.DataCollection;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

public class DataCollectionPanel extends GridPane {
	private static final Logger logger = Application.getLogger("DataCollectionPanel");

	private Label datesL = new Label("Dates:"), startL = new Label("Début:"), startDates = new Label("Depuis toujours"), startPlayers = new Label();
	private Label endL = new Label("Fin:"), endDates = new Label("Indéfinie"), endPlayers = new Label(), cacheL = new Label("Cache:");
	private Button loadStart = new Button("Charger"), clearStart = new Button("Effacer");
	private Button loadEnd = new Button("Charger"), genEnd = new Button("Générer"), saveEnd = new Button("Sauvegarder");
	private TextField cacheAge = new TextField();

	private DataCollection startCol, endCol;
	private Duration maxAge = Duration.ofDays(1);

	private UserInterface ui;
	private EpiStats epi;
	private PlayerCache cache;

	private FileChooser chooser = new FileChooser();

	public DataCollectionPanel(UserInterface ui, PlayerCache cache) {
		this.ui = ui;
		this.epi = ui.getEpiStats();
		this.cache = cache;

		this.loadStart.setPrefWidth(Integer.MAX_VALUE);
		this.clearStart.setPrefWidth(Integer.MAX_VALUE);

		this.loadEnd.setPrefWidth(Integer.MAX_VALUE);
		this.genEnd.setPrefWidth(Integer.MAX_VALUE);
		this.saveEnd.setPrefWidth(Integer.MAX_VALUE);

		this.cacheAge.setPromptText("Âge maximum (défaut: 1d)");
		this.cacheAge.textProperty().addListener((v, oldV, newV) -> parseCacheAge());

		this.chooser.getExtensionFilters().add(new ExtensionFilter("Collection de données", "*.dcol"));

		this.loadStart.setOnAction(a -> {
			File f = this.chooser.showOpenDialog(this.epi.getStage());
			if (f == null)
				return;
			Path file = f.toPath();

			this.epi.getExecutor().submit(() -> {
				try {
					logger.debug("Reading data collection from file ..");
					setStartCollection(DataCollection.read(file));
				} catch (Exception e) {
					Popup.error().title("Erreur de lecture").header("Une erreur est survenue lors de la lecture de la collection de données").message(e).show();
					logger.error("Failed to read data collection from file " + file.getFileName(), e);
				}
			});
		});

		this.clearStart.setOnAction(a -> setStartCollection(null));

		this.loadEnd.setOnAction(a -> {
			File f = this.chooser.showOpenDialog(this.epi.getStage());
			if (f == null)
				return;
			Path file = f.toPath();

			this.epi.getExecutor().submit(() -> {
				try {
					logger.debug("Reading data collection from file ..");
					setEndCollection(DataCollection.read(file));
				} catch (Exception e) {
					Popup.error().title("Erreur de lecture").header("Une erreur est survenue lors de la lecture de la collection de données").message(e).show();
					logger.error("Failed to read data collection from file " + file.getFileName(), e);
				}
			});
		});

		this.genEnd.setOnAction(a -> {
			if (this.ui.getObjectListPanel().getObjectList().players.isEmpty())
				return;

			generateEnd(true);
		});

		this.saveEnd.setOnAction(a -> {
			if (this.endCol == null)
				return;

			File f = this.chooser.showSaveDialog(this.epi.getStage());
			if (f == null)
				return;
			Path file = f.toPath();

			this.epi.getExecutor().submit(() -> {
				try {
					logger.debug("Saving data collection to file ..");
					this.endCol.save(file);
				} catch (Exception e) {
					Popup.error().title("Erreur de sauvegarde").header("Une erreur est survenue lors de la sauvegarde de la collection de données").message(e).show();
					logger.error("Failed to save data collection to file " + file.getFileName(), e);
				}
			});
		});

		setVgap(4);
		setHgap(4);

		add(UserInterface.title("2 - Données à traiter:"), 0, 0, 5, 1);

		add(this.datesL, 0, 1);

		add(this.startL, 1, 1);
		add(this.startDates, 2, 1, 2, 1);
		add(this.startPlayers, 4, 1);

		add(this.loadStart, 2, 2);
		add(this.clearStart, 3, 2);

		add(this.endL, 1, 3);
		add(this.endDates, 2, 3, 2, 1);
		add(this.endPlayers, 4, 3);

		add(this.loadEnd, 2, 4);
		add(this.genEnd, 3, 4);
		add(this.saveEnd, 4, 4);

		add(this.cacheL, 0, 5);
		add(this.cacheAge, 1, 5, 3, 1);

		getColumnConstraints().addAll(GridUtil.createColumn(15), GridUtil.createColumn(10), GridUtil.createColumn(25), GridUtil.createColumn(25), GridUtil.createColumn(25));
	}

	private void parseCacheAge() {
		String s = this.cacheAge.getText();
		this.maxAge = s.isEmpty() ? Duration.ofDays(1) : StringUtil.parseDuration(s);
		this.genEnd.setDisable(this.maxAge.equals(Duration.ZERO));
	}

	private void generateEnd(boolean notifyTaskEnd) {
		if (Popup.confirmation().title("Attention").message("Générer une collection de données peut être très long pour un nombre important de joueurs !\nEtes-vous sûr de vouloir continuer ?").submitAndWait()) {
			Instant minDate = Instant.now().minus(this.maxAge);

			Consumer<ObservableTask> consumer = (task) -> {
				Set<UUID> ids = this.ui.getObjectListPanel().getObjectList().players;
				int progress = 0, total = ids.size();

				logger.info("Collecting data for " + total + " players ..");
				task.setTitle("Collecte des données des joueurs ..");
				task.setProgress(0);

				long time = System.currentTimeMillis();

				List<PlayerInfo> players = new ArrayList<>(total);
				for (UUID id : ids) {
					if (task.isCancelled())
						return;
					task.setMessage("Joueur: " + id);

					PlayerInfo p = this.cache.read(id).orElse(null);
					if (p == null || p.endDate.isBefore(minDate)) {
						p = PlayerInfo.get(id, true).orElse(null);
						if (p != null) {
							this.cache.save(p);
							players.add(p);
						}
					} else
						players.add(p);

					task.setProgress(++progress / (double) total);
				}

				logger.info("Collected data of " + players.size() + " players in " + (System.currentTimeMillis() - time) / 1000F + "s.");

				DataCollection col = new DataCollection(players.toArray(new PlayerInfo[players.size()]));
				setEndCollection(col);

				if (notifyTaskEnd)
					Popup.info().title("Génération terminée").message("Une collection contenant " + col.getPlayerCount() + " " + (col.getPlayerCount() > 1 ? "joueurs" : "joueur") + " a été générée.").show();
			};

			Popup.consumer(consumer).title("Génération de la collection de données ..").submitAndWait();
		}
	}

	private void setStartCollection(DataCollection col) {
		if (Platform.isFxApplicationThread()) {
			if (col == null) {
				this.startCol = null;
				this.startDates.setText("Depuis toujours");
				this.startPlayers.setText(null);
			} else {
				if (col.containsIntervals())
					throw new IllegalArgumentException("Intervals");

				this.startCol = col;
				this.startDates.setText("Du " + StringUtil.DATETIME_FORMAT.format(col.getMinEndDate()) + " au " + StringUtil.DATETIME_FORMAT.format(col.getMaxEndDate()));
				this.startPlayers.setText("(" + col.getPlayerCount() + " " + (col.getPlayerCount() > 1 ? "joueurs" : "joueur") + ")");
			}
		} else
			Platform.runLater(() -> setStartCollection(col));
	}

	private void setEndCollection(DataCollection col) {
		if (Platform.isFxApplicationThread()) {
			if (col == null) {
				this.endCol = null;
				this.endDates.setText("Indéfinie");
				this.endPlayers.setText(null);
			} else {
				if (col.containsIntervals())
					throw new IllegalArgumentException("Intervals");

				this.endCol = col;
				this.endDates.setText("Du " + StringUtil.DATETIME_FORMAT.format(col.getMinEndDate()) + " au " + StringUtil.DATETIME_FORMAT.format(col.getMaxEndDate()));
				this.endPlayers.setText("(" + col.getPlayerCount() + " " + (col.getPlayerCount() > 1 ? "joueurs" : "joueur") + ")");
			}
		} else
			Platform.runLater(() -> setEndCollection(col));
	}

	public Optional<DataCollection> getTargetCollection() {
		Set<UUID> ids = this.ui.getObjectListPanel().getObjectList().players;
		if (ids.isEmpty())
			return Optional.empty();

		if (this.endCol == null) {
			if (!this.maxAge.equals(Duration.ZERO))
				generateEnd(false);
			if (this.endCol == null)
				return Optional.empty();
		}

		boolean useIntervals = this.startCol != null;

		if (useIntervals && this.startCol.getMaxEndDate().isAfter(this.endCol.getMinEndDate())) {
			Popup.error().title("Collisions de données").header("Impossible de générer la liste de joueurs à traiter.").message("Les extremums des dates des deux collections de données ne sont pas compatibles.").show();
			return Optional.empty();
		}

		List<PlayerInfo> players = new ArrayList<>();
		int missing1 = 0, missing2 = 0;

		for (UUID id : ids) {
			PlayerInfo info = this.endCol.getPlayer(id).orElse(null);
			if (info == null) {
				missing1++;
				continue;
			}

			if (useIntervals) {
				PlayerInfo info2 = this.startCol.getPlayer(id).orElse(null);
				if (info2 == null) {
					missing2++;
					continue;
				}

				info = info.copy();
				info.startDate = info2.endDate;
				info.startStats = info2.endStats;
			}

			players.add(info);
		}

		int missing = missing1 + missing2;
		if (missing != 0) {
			String msg;
			if (missing == 1) {
				msg = missing + " joueur présent dans la liste d'objets à traiter n'a pas été trouvé dans les données à traiter."
						+ "\nSi vous souhaiter le traiter, vous devez " + (missing2 == 0 ? "générer ou fournir" : "fournir") + " des données contenant ce joueur."
						+ "\nSouhaitez-vous continuer sans ce joueur ?";
			} else {
				msg = missing + " joueurs présents dans la liste d'objets à traiter n'ont pas été trouvés dans les données à traiter."
						+ "\nSi vous souhaiter les traiter, vous devez " + (missing2 == 0 ? "générer ou fournir" : "fournir") + " des données contenant ces joueurs."
						+ "\nSouhaitez-vous continuer sans ces joueurs ?";
			}
			if (!Popup.confirmation().title("Joueurs manquants").message(msg).submitAndWait())
				return Optional.empty();
		}

		return Optional.of(new DataCollection(players.toArray(new PlayerInfo[players.size()])));
	}

	public StringProperty getCacheAge() {
		return this.cacheAge.textProperty();
	}
}
