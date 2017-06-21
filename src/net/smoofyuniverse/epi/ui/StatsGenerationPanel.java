/*******************************************************************************
 * Copyright (C) 2017 Hugo Dupanloup (Yeregorix)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package net.smoofyuniverse.epi.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import net.smoofyuniverse.common.app.Application;
import net.smoofyuniverse.common.fxui.dialog.Popup;
import net.smoofyuniverse.common.fxui.task.ObservableTask;
import net.smoofyuniverse.common.logger.core.Logger;
import net.smoofyuniverse.common.util.GridUtil;
import net.smoofyuniverse.epi.EpiStats;
import net.smoofyuniverse.epi.api.GuildInfo;
import net.smoofyuniverse.epi.api.PlayerInfo;
import net.smoofyuniverse.epi.stats.ObjectList;
import net.smoofyuniverse.epi.stats.RankingList;
import net.smoofyuniverse.epi.stats.operation.OperationException;
import net.smoofyuniverse.epi.stats.operation.RankingOperation;

public final class StatsGenerationPanel extends GridPane {
	private static final Logger logger = Application.getLogger("UserInterface");
	
	private Label playersL = new Label("Joueurs:"), guildsL = new Label("Guildes:");
	private Label players = new Label("0"), guilds = new Label("0");
	private Button addP = new Button("Ajouter"), addG = new Button("Ajouter");
	private Button removeP = new Button("Retirer"), removeG = new Button("Retirer");
	private Button clearL = new Button("Vider"), importL = new Button("Importer"), exportL = new Button("Exporter"), refreshL = new Button("Actualiser");
	private TextArea editor = new TextArea();
	private Button generate = new Button("Générer");
	
	private RankingOperation operation;
	private PlayerInfo[] infosCache;
	private String[] namesCache;
	
	private EpiStats epi;
	private ObjectList list;
	private UserInterface ui;
	
	public StatsGenerationPanel(EpiStats epi, UserInterface ui) {
		this.epi = epi;
		this.list = epi.getObjectList();
		this.ui = ui;
		
		this.addP.setPrefWidth(Integer.MAX_VALUE);
		this.addG.setPrefWidth(Integer.MAX_VALUE);
		this.removeP.setPrefWidth(Integer.MAX_VALUE);
		this.removeG.setPrefWidth(Integer.MAX_VALUE);
		
		this.clearL.setPrefWidth(Integer.MAX_VALUE);
		this.importL.setPrefWidth(Integer.MAX_VALUE);
		this.exportL.setPrefWidth(Integer.MAX_VALUE);
		this.refreshL.setPrefWidth(Integer.MAX_VALUE);
		
		this.editor.setPrefSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
		this.generate.setPrefWidth(Integer.MAX_VALUE);
		
		this.addP.setOnAction((e) -> {
			String arg = Popup.textInput().title("Ajouter").message("Ajouter un joueur et sa guilde:").showAndWait().orElse("");
			if (arg.isEmpty())
				return;
			this.epi.getExecutor().submit(() -> {
				PlayerInfo p = PlayerInfo.get(arg, false).orElse(null);
				if (p == null)
					Popup.info().title("Données introuvables").message("Aucune donnée n'a pu être récupérée pour le pseudo : " + arg).showAndWait();
				else if (this.list.addPlayer(p))
					saveObjectList();
				else
					Popup.info().title("Données déjà existantes").message("Le joueur spécifié est déjà contenu dans la liste.").showAndWait();
			});
		});
		
		this.addG.setOnAction((e) -> {
			String arg = Popup.textInput().title("Ajouter").message("Ajouter une guilde et ses membres:").showAndWait().orElse("");
			if (arg.isEmpty())
				return;
			this.epi.getExecutor().submit(() -> {
				GuildInfo g = GuildInfo.get(arg).orElse(null);
				if (g == null)
					Popup.info().title("Données introuvables").message("Aucune donnée n'a pu être récupérée pour la guilde : " + arg).showAndWait();
				else if (this.list.addGuild(g))
					saveObjectList();
				else
					Popup.info().title("Données déjà existantes").message("La guilde spécifiée est déjà contenu dans la liste.").showAndWait();
			});
		});
		
		this.removeP.setOnAction((e) -> {
			if (Popup.confirmation().title("Retirer").message("Un joueur retiré sera re-ajouté si vous actualiser alors que la liste contient des membres de sa guilde.\nEtes-vous sûr de vouloir continuer ?").submitAndWait()) {
				String arg = Popup.textInput().title("Retirer").message("Retirer un joueur:").showAndWait().orElse("");
				if (arg.isEmpty())
					return;
				this.epi.getExecutor().submit(() -> {
					PlayerInfo p = PlayerInfo.get(arg, false).orElse(null);
					if (p == null || !this.list.removePlayer(p))
						Popup.info().title("Données introuvables").message("Le joueur spécifié n'est pas contenu dans la liste.").showAndWait();
					else
						saveObjectList();
				});
			}
		});
		
		this.removeG.setOnAction((e) -> {
			if (Popup.confirmation().title("Retirer").message("Retirer une guilde retire par la même occasion tout ses membres.\nEtes-vous sûr de vouloir continuer ?").submitAndWait()) {
				String arg = Popup.textInput().title("Retirer").message("Retirer une guilde et ses membres:").showAndWait().orElse("");
				if (arg.isEmpty())
					return;
				this.epi.getExecutor().submit(() -> {
					GuildInfo g = GuildInfo.get(arg).orElse(null);
					if (g == null || !this.list.removeGuild(g))
						Popup.info().title("Données introuvables").message("La guilde spécifiée n'est pas contenu dans la liste.").showAndWait();
					else
						saveObjectList();
				});
			}
		});
		
		FileChooser objListChooser = new FileChooser();
		objListChooser.getExtensionFilters().addAll(new ExtensionFilter("Liste d'objets", "*.olist"), new ExtensionFilter("Représentation JSON", "*.json"));
		
		this.clearL.setOnAction((e) -> {
			if (Popup.confirmation().title("Vider").message("Vider retire tout les joueurs et les guildes de la liste.\nEtes-vous sûr de vouloir continuer ?").submitAndWait()) {
				this.list.clear();
				saveObjectList();
			}
		});
		
		this.importL.setOnAction((ev) -> {
			File f = objListChooser.showOpenDialog(this.epi.getStage());
			if (f == null)
				return;
			Path file = f.toPath();
			try {
				this.list.merge(file);
			} catch (IOException e) {
				Popup.error().title("Erreur de lecture").header("Une erreur est survenue lors de la lecture de la liste d'objets").message(e).show();
				logger.error("Failed to read object list from file " + file.getFileName(), e);
			}
			saveObjectList();
		});
		
		this.exportL.setOnAction((e) -> {
			File f = objListChooser.showSaveDialog(this.epi.getStage());
			if (f == null)
				return;
			saveObjectList(f.toPath());
		});
		
		this.refreshL.setOnAction((ev) -> {
			if (Popup.confirmation().title("Attention").message("Actualiser peut être très long pour des quantités importantes de données !\nEtes-vous sûr de vouloir continuer ?").submitAndWait()) {
				if (Popup.consumer(this.list::refresh).title("Actualisation des données ..").submitAndWait())
					saveObjectList();
				else {
					try {
						this.list.read();
					} catch (IOException e) {
						logger.error("Failed to read object list from file objects.olist", e);
					}
					updateObjectList();
				}
			}
		});
		
		this.editor.textProperty().addListener((v, oldV, newV) -> {
			this.operation = RankingOperation.parseAll(newV).orElse(null);
			this.generate.setDisable(this.operation == null);
		});
		
		this.generate.setDisable(true);
		this.generate.setOnAction((a) -> {
			if (this.list.players.isEmpty())
				return;
			
			if (Popup.confirmation().title("Attention").message("Générer les classements peut être très long pour des quantités importantes de données !\nEtes-vous sûr de vouloir continuer ?").submitAndWait()) {
				Consumer<ObservableTask> consumer = (task) -> {
					if (this.infosCache == null) {
						int progress, total;
						
						task.setTitle("Collecte des données des joueurs ..");
						task.setProgress(0);
						progress = 0;
						total = this.list.players.size();
						
						List<PlayerInfo> players = new ArrayList<>(total);
						for (UUID id : this.list.players) {
							if (task.isCancelled())
								return;
							task.setMessage("Joueur: " + id);
							PlayerInfo.get(id, true).ifPresent(players::add);
							task.setProgress(++progress / (double) total);
						}
						
						task.setTitle("Génération de la liste des joueurs ..");
						task.setProgress(0);
						progress = 0;
						total = this.list.players.size();
						
						this.infosCache = new PlayerInfo[players.size()];
						this.namesCache = new String[players.size()];
						for (PlayerInfo p : players) {
							if (task.isCancelled())
								return;
							task.setMessage("Joueur: " + p.name);
							this.infosCache[progress] = p;
							this.namesCache[progress] = p.name;
							task.setProgress(++progress / (double) total);
						}
					}
					
					RankingList l = new RankingList(this.namesCache);
					l.infosCache = this.infosCache;
					
					try {
						this.operation.accept(l, task);
					} catch (OperationException e) {
						Popup.error().title("Erreur de génération").header("Une erreur est survenue ligne " + e.line +".").expandable(new Label(e.getMessage())).show();
						return;
					}
					
					Popup.info().title("Génération terminée").message("Un classement contenant " + l.getRankings().size() + " " + (l.getRankings().size() > 1 ? "catégories" : "catégorie")
							+ " a été généré avec " + l.infosCache.length + " " + (l.infosCache.length > 1 ? "joueurs" : "joueur") + ".").show();
					this.ui.getStatsListPanel().open(l);
				};
				
				Popup.consumer(consumer).title("Génération des classements ..").submitAndWait();
			}
		});
		
		setVgap(4);
		setHgap(4);
		
		add(this.playersL, 0, 0);
		add(this.guildsL, 0, 1);
		add(this.players, 1, 0);
		add(this.guilds, 1, 1);
		add(this.addP, 2, 0);
		add(this.addG, 2, 1);
		add(this.removeP, 3, 0);
		add(this.removeG, 3, 1);
		
		add(new HBox(4, this.clearL, this.importL, this.exportL, this.refreshL), 0, 2, 4, 1);
		add(this.editor, 0, 3, 4, 1);
		add(this.generate, 0, 4, 4, 1);
		
		getColumnConstraints().addAll(GridUtil.createColumn(15), GridUtil.createColumn(35), GridUtil.createColumn(25), GridUtil.createColumn(25));
		
		updateObjectList();
	}
	
	private void updateObjectList() {
		if (Platform.isFxApplicationThread()) {
			this.players.setText(Integer.toString(this.list.players.size()));
			this.guilds.setText(Integer.toString(this.list.guilds.size()));
			
			this.infosCache = null;
			this.namesCache = null;
		} else
			Platform.runLater(this::updateObjectList);
	}
	
	private boolean saveObjectList() {
		updateObjectList();
		return saveObjectList(this.list.defaultFile);
	}
	
	private boolean saveObjectList(Path file) {
		try {
			this.list.save(file);
			return true;
		} catch (IOException e) {
			Popup.error().title("Erreur de sauvegarde").header("Une erreur est survenue lors de la sauvegarde de la liste d'objets").message(e).show();
			logger.error("Failed to save object list to file " + file.getFileName(), e);
			return false;
		}
	}
}
