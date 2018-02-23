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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import net.smoofyuniverse.common.app.App;
import net.smoofyuniverse.common.fxui.dialog.Popup;
import net.smoofyuniverse.common.util.GridUtil;
import net.smoofyuniverse.epi.EpiStats;
import net.smoofyuniverse.epi.api.GuildInfo;
import net.smoofyuniverse.epi.api.PlayerInfo;
import net.smoofyuniverse.epi.stats.ObjectList;
import net.smoofyuniverse.logger.core.Logger;

import java.io.File;
import java.nio.file.Path;

public class ObjectListPanel extends GridPane {
	private static final Logger logger = App.getLogger("ObjectListPanel");

	private Label playersL = new Label("Joueurs:"), guildsL = new Label("Guildes:");
	private Label players = new Label("0"), guilds = new Label("0");
	private Button addP = new Button("Ajouter"), addG = new Button("Ajouter");
	private Button removeP = new Button("Retirer"), removeG = new Button("Retirer");
	private Button clearL = new Button("Vider"), importL = new Button("Importer"), exportL = new Button("Exporter"), refreshL = new Button("Actualiser");

	private EpiStats epi;
	private ObjectList list;

	private FileChooser chooser = new FileChooser();

	public ObjectListPanel(UserInterface ui, ObjectList list) {
		this.epi = ui.getEpiStats();
		this.list = list;

		this.addP.setPrefWidth(Integer.MAX_VALUE);
		this.addG.setPrefWidth(Integer.MAX_VALUE);
		this.removeP.setPrefWidth(Integer.MAX_VALUE);
		this.removeG.setPrefWidth(Integer.MAX_VALUE);

		this.clearL.setPrefWidth(Integer.MAX_VALUE);
		this.importL.setPrefWidth(Integer.MAX_VALUE);
		this.exportL.setPrefWidth(Integer.MAX_VALUE);
		this.refreshL.setPrefWidth(Integer.MAX_VALUE);

		this.chooser.getExtensionFilters().addAll(new ExtensionFilter("Liste d'objets", "*.olist"), new ExtensionFilter("Représentation JSON", "*.json"));

		this.addP.setOnAction(a -> {
			String arg = Popup.textInput().title("Ajouter").message("Ajouter un joueur:").showAndWait().orElse("");
			if (arg.isEmpty())
				return;
			this.epi.getExecutor().submit(() -> {
				PlayerInfo p = PlayerInfo.get(arg, false).orElse(null);
				if (p == null)
					Popup.info().title("Données introuvables").message("Aucune donnée n'a pu être récupérée pour le pseudo : " + arg).showAndWait();
				else if (this.list.addPlayer(p)) {
					logger.debug("Added player: " + p.id);
					saveObjectList();
				} else
					Popup.info().title("Données déjà existantes").message("Le joueur spécifié est déjà contenu dans la liste.").showAndWait();
			});
		});

		this.addG.setOnAction(a -> {
			String arg = Popup.textInput().title("Ajouter").message("Ajouter une guilde et ses membres:").showAndWait().orElse("");
			if (arg.isEmpty())
				return;
			this.epi.getExecutor().submit(() -> {
				GuildInfo g = GuildInfo.get(arg).orElse(null);
				if (g == null)
					Popup.info().title("Données introuvables").message("Aucune donnée n'a pu être récupérée pour la guilde : " + arg).showAndWait();
				else if (this.list.addGuild(g)) {
					logger.debug("Added guild: " + g.name);
					saveObjectList();
				} else
					Popup.info().title("Données déjà existantes").message("La guilde spécifiée est déjà contenu dans la liste.").showAndWait();
			});
		});

		this.removeP.setOnAction(a -> {
			if (Popup.confirmation().title("Retirer").message("Un joueur retiré sera re-ajouté si vous actualiser alors que la liste contient des membres de sa guilde.\nEtes-vous sûr de vouloir continuer ?").submitAndWait()) {
				String arg = Popup.textInput().title("Retirer").message("Retirer un joueur:").showAndWait().orElse("");
				if (arg.isEmpty())
					return;
				this.epi.getExecutor().submit(() -> {
					PlayerInfo p = PlayerInfo.get(arg, false).orElse(null);
					if (p == null || !this.list.removePlayer(p))
						Popup.info().title("Données introuvables").message("Le joueur spécifié n'est pas contenu dans la liste.").showAndWait();
					else {
						logger.debug("Removed player: " + p.id);
						saveObjectList();
					}
				});
			}
		});

		this.removeG.setOnAction(a -> {
			if (Popup.confirmation().title("Retirer").message("Retirer une guilde retire par la même occasion tout ses membres.\nEtes-vous sûr de vouloir continuer ?").submitAndWait()) {
				String arg = Popup.textInput().title("Retirer").message("Retirer une guilde et ses membres:").showAndWait().orElse("");
				if (arg.isEmpty())
					return;
				this.epi.getExecutor().submit(() -> {
					GuildInfo g = GuildInfo.get(arg).orElse(null);
					if (g == null || !this.list.removeGuild(g))
						Popup.info().title("Données introuvables").message("La guilde spécifiée n'est pas contenu dans la liste.").showAndWait();
					else {
						logger.debug("Removed guild: " + g.name);
						saveObjectList();
					}
				});
			}
		});

		this.clearL.setOnAction(a -> {
			if (Popup.confirmation().title("Vider").message("Vider retire tout les joueurs et les guildes de la liste.\nEtes-vous sûr de vouloir continuer ?").submitAndWait()) {
				this.list.clear();
				logger.debug("Cleared object list.");
				saveObjectList();
			}
		});

		this.importL.setOnAction(a -> {
			File f = this.chooser.showOpenDialog(this.epi.getStage());
			if (f == null)
				return;
			mergeObjectList(f.toPath());
			saveObjectList();
		});

		this.exportL.setOnAction(a -> {
			File f = this.chooser.showSaveDialog(this.epi.getStage());
			if (f == null)
				return;
			saveObjectList(f.toPath());
		});

		this.refreshL.setOnAction(a -> {
			if (Popup.confirmation().title("Attention").message("Actualiser peut être très long pour des quantités importantes de données !\nEtes-vous sûr de vouloir continuer ?").submitAndWait()) {
				logger.info("Starting refresh task ..");
				if (Popup.consumer(this.list::refresh).title("Actualisation des données ..").submitAndWait()) {
					logger.info("Refresh task ended.");
					saveObjectList();
				} else {
					logger.info("Cancelled.");
					readObjectList();
					updateObjectList();
				}
			}
		});

		setVgap(4);
		setHgap(4);

		add(UserInterface.title("1 - Objets à traiter:"), 0, 0, 4, 1);

		add(this.playersL, 0, 1);
		add(this.players, 1, 1, 2, 1);
		add(this.addP, 3, 1);
		add(this.removeP, 4, 1);

		add(this.guildsL, 0, 2);
		add(this.guilds, 1, 2, 2, 1);
		add(this.addG, 3, 2);
		add(this.removeG, 4, 2);

		add(this.clearL, 0, 3, 2, 1);
		add(this.importL, 2, 3);
		add(this.exportL, 3, 3);
		add(this.refreshL, 4, 3);

		getColumnConstraints().addAll(GridUtil.createColumn(15), GridUtil.createColumn(10), GridUtil.createColumn(25), GridUtil.createColumn(25), GridUtil.createColumn(25));

		readObjectList();
		updateObjectList();
	}

	public ObjectList getObjectList() {
		return this.list;
	}

	private void updateObjectList() {
		if (Platform.isFxApplicationThread()) {
			this.players.setText(Integer.toString(this.list.players.size()));
			this.guilds.setText(Integer.toString(this.list.guilds.size()));
		} else
			Platform.runLater(this::updateObjectList);
	}

	private boolean readObjectList() {
		return readObjectList(this.list.defaultFile);
	}

	private boolean readObjectList(Path file) {
		this.list.clear();
		return mergeObjectList(file);
	}

	private boolean mergeObjectList() {
		return mergeObjectList(this.list.defaultFile);
	}

	private boolean mergeObjectList(Path file) {
		try {
			logger.debug("Reading object list from file ..");
			this.list.merge(file);
			return true;
		} catch (Exception e) {
			Popup.error().title("Erreur de lecture").header("Une erreur est survenue lors de la lecture de la liste d'objets").message(e).show();
			logger.error("Failed to read object list from file " + file.getFileName(), e);
			return false;
		}
	}

	private boolean saveObjectList() {
		return saveObjectList(this.list.defaultFile);
	}

	private boolean saveObjectList(Path file) {
		if (this.list.defaultFile == file)
			updateObjectList();

		try {
			logger.debug("Saving object list to file ..");
			this.list.save(file);
			return true;
		} catch (Exception e) {
			Popup.error().title("Erreur de sauvegarde").header("Une erreur est survenue lors de la sauvegarde de la liste d'objets").message(e).show();
			logger.error("Failed to save object list to file " + file.getFileName(), e);
			return false;
		}
	}
}
