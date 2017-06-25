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

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import net.smoofyuniverse.common.fxui.control.AbstractListCell;
import net.smoofyuniverse.common.util.GridUtil;
import net.smoofyuniverse.common.util.StringUtil;
import net.smoofyuniverse.epi.stats.Ranking;

import java.util.Optional;

public final class StatsListView extends ListView<Integer> {
	private Ranking ranking;
	
	private UserInterface ui;
	
	public StatsListView(UserInterface ui) {
		this.ui = ui;
		
		setCellFactory(l -> new StatsListCell());
		getSelectionModel().selectedIndexProperty().addListener((v, oldV, newV) -> this.ui.getStatsListPanel().setSelectedIndex(newV.intValue()));
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
	
	private class StatsListCell extends AbstractListCell<Integer> {
		private Label index = new Label(), name = new Label(), value = new Label();
		private GridPane content = new GridPane();
		private Tooltip tooltip = new Tooltip();
		
		public StatsListCell() {
			setTooltip(this.tooltip);
			
			this.content.add(this.index, 0, 0);
			this.content.add(this.name, 1, 0);
			this.content.add(this.value, 2, 0);
			
			this.content.getColumnConstraints().addAll(GridUtil.createColumn(20), GridUtil.createColumn(40), GridUtil.createColumn(40));
		}
		
		@Override
		protected Node getContent() {
			int p = getItem();
			
			this.index.setText("#" + (getIndex() +1));
			this.name.setText(StatsListView.this.ranking.parent.getPlayer(p).name);
			this.value.setText(Double.toString(StatsListView.this.ranking.getValue(p)));
			this.tooltip.setText(StringUtil.DATETIME_FORMAT.format(StatsListView.this.ranking.parent.getPlayer(p).date));
			
			return this.content;
		}
	}
}
