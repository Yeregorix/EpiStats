package net.smoofyuniverse.epi.ui;

import java.util.Optional;

import javafx.scene.control.ListView;
import net.smoofyuniverse.common.fxui.control.LabelCell;
import net.smoofyuniverse.epi.stats.Ranking;

public final class StatsListView extends ListView<Integer> {
	private Ranking ranking;
	
	private UserInterface ui;
	
	public StatsListView(UserInterface ui) {
		this.ui = ui;
		
		setCellFactory(l -> new LabelCell<>(p -> "%index% - " + this.ranking.parent.getPlayerName(p) + ": " + this.ranking.getValue(p)));
		getSelectionModel().selectedIndexProperty().addListener((v, oldV, newV) -> {
			this.ui.getStatsListPanel().setSelectedIndex(newV.intValue());
		});
	}
	
	public Optional<Ranking> currentRanking() {
		return Optional.ofNullable(this.ranking);
	}
	
	public void open(Ranking r) {
		this.ranking = r;
		if (r == null)
			getItems().clear();
		else
			getItems().setAll(r.collection());
	}
}
