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
