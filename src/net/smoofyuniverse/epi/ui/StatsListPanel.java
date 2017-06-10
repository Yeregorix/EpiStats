package net.smoofyuniverse.epi.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;

import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import net.smoofyuniverse.common.app.Application;
import net.smoofyuniverse.common.fxui.control.LabelCell;
import net.smoofyuniverse.common.fxui.dialog.Popup;
import net.smoofyuniverse.common.fxui.field.IntegerField;
import net.smoofyuniverse.common.logger.core.Logger;
import net.smoofyuniverse.common.util.GridUtil;
import net.smoofyuniverse.common.util.StringUtil;
import net.smoofyuniverse.epi.EpiStats;
import net.smoofyuniverse.epi.stats.Ranking;
import net.smoofyuniverse.epi.stats.RankingList;

public final class StatsListPanel extends GridPane {
	private static final Logger logger = Application.getLogger("UserInterface");
	
	private Label rankingsL = new Label("Catégories:"), indexL = new Label("Index:"), searchL = new Label("Rechercher:"), dateL = new Label("Date:"), date = new Label();
	private ListView<Ranking> rankings = new ListView<>();
	private IntegerField index = new IntegerField(0);
	private TextField search = new TextField();
	private Button open = new Button("Ouvrir"), save = new Button("Sauvegarder");
	
	private RankingList list;
	
	private EpiStats epi;
	private UserInterface ui;
	
	public StatsListPanel(EpiStats epi, UserInterface ui) {
		this.epi = epi;
		this.ui = ui;
		
		GridPane.setValignment(this.rankingsL, VPos.TOP);
		this.rankings.setPrefSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
		
		this.open.setPrefWidth(Integer.MAX_VALUE);
		this.save.setPrefWidth(Integer.MAX_VALUE);
		
		this.rankings.setCellFactory(l -> new LabelCell<>(r -> r.name));
		this.rankings.getSelectionModel().selectedItemProperty().addListener((v, oldV, newV) -> {
			if (newV != null)
				this.ui.getStatsListView().open(newV);
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
				this.ui.getStatsListView().getSelectionModel().select(i -1);
			}
		});
		
		this.search.textProperty().addListener((v, oldV, newV) -> {
			Ranking r = this.ui.getStatsListView().currentRanking().orElse(null);
			if (r == null || this.list == null)
				return;
			String name = newV.toLowerCase();
			
			int i = 0;
			Iterator<Integer> it = r.iterator();
			while (it.hasNext()) {
				int p = it.next();
				String n = this.list.getPlayerName(p);
				if (n.toLowerCase().startsWith(name)) {
					this.index.valueProperty().set(i +1);
					break;
				}
				i++;
			}
		});
		
		FileChooser rListChooser = new FileChooser();
		rListChooser.getExtensionFilters().addAll(new ExtensionFilter("Liste de classements", "*.rlist"), new ExtensionFilter("Représentation JSON", "*.json"));
		
		this.open.setOnAction((ev) -> {
			File f = rListChooser.showOpenDialog(this.epi.getStage());
			if (f == null)
				return;
			Path file = f.toPath();
			
			this.epi.getExecutor().submit(() -> {
				try {
					open(RankingList.read(file));
				} catch (IOException e) {
					Popup.error().title("Erreur de lecture").header("Une erreur est survenue lors de la lecture de la liste de classements").message(e).show();
					logger.error("Failed to read ranking list to file " + file.getFileName(), e);
				}
			});
		});
		
		FileChooser rListChooser2 = new FileChooser();
		rListChooser2.getExtensionFilters().addAll(new ExtensionFilter("Liste de classements", "*.rlist"), new ExtensionFilter("Représentation JSON", "*.json"), new ExtensionFilter("Représentation tabulaire", "*.csv"));
		
		this.save.setOnAction((ev) -> {
			if (this.list == null)
				return;
			
			File f = rListChooser2.showSaveDialog(this.epi.getStage());
			if (f == null)
				return;
			Path file = f.toPath();
			
			this.epi.getExecutor().submit(() -> {
				try {
					this.list.save(file);
				} catch (IOException e) {
					Popup.error().title("Erreur de sauvegarde").header("Une erreur est survenue lors de la sauvegarde de la liste de classements").message(e).show();
					logger.error("Failed to save ranking list to file " + file.getFileName(), e);
				}
			});
		});
		
		setVgap(4);
		setHgap(4);
		
		add(this.dateL, 0, 0);
		add(this.date, 1, 0);
		
		add(this.rankingsL, 0, 1);
		add(this.rankings, 1, 1, 3, 1);
		
		add(this.indexL, 0, 2);
		add(this.index, 1, 2);
		
		add(this.searchL, 2, 2);
		add(this.search, 3, 2);
		
		add(this.open, 0, 3, 2, 1);
		add(this.save, 2, 3, 2, 1);
		
		getColumnConstraints().addAll(GridUtil.createColumn(15), GridUtil.createColumn(35), GridUtil.createColumn(15), GridUtil.createColumn(35));
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
			
			if (list == null) {
				this.rankings.getItems().clear();
				this.date.setText(null);
			} else {
				this.rankings.getItems().setAll(list.getRankings());
				if (list.getRankings().size() != 0)
					this.rankings.getSelectionModel().select(0);
				this.date.setText(StringUtil.DATETIME_FORMAT.format(list.getDate()));
			}
		} else
			Platform.runLater(() -> open(list));
	}
}
