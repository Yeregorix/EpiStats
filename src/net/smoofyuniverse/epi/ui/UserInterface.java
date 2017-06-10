package net.smoofyuniverse.epi.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import net.smoofyuniverse.common.util.GridUtil;
import net.smoofyuniverse.epi.EpiStats;

public final class UserInterface extends GridPane {
	private StatsGenerationPanel statsGen;
	private StatsListPanel statsList;
	private StatsListView statsView;
	
	public UserInterface(EpiStats epi) {
		this.statsGen = new StatsGenerationPanel(epi, this);
		this.statsList = new StatsListPanel(epi, this);
		this.statsView = new StatsListView(this);
		
		setVgap(15);
		setHgap(10);
		setPadding(new Insets(10));
		
		add(this.statsGen, 0, 0);
		add(this.statsList, 0, 1);
		add(this.statsView, 1, 0, 1, 2);
		
		getColumnConstraints().addAll(GridUtil.createColumn(60), GridUtil.createColumn(40));
		getRowConstraints().addAll(GridUtil.createRow(60), GridUtil.createRow(40));
	}
	
	public StatsGenerationPanel getStatsGenerationPanel() {
		return this.statsGen;
	}
	
	public StatsListPanel getStatsListPanel() {
		return this.statsList;
	}
	
	public StatsListView getStatsListView() {
		return this.statsView;
	}
}
