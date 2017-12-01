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

import com.sun.javafx.scene.control.skin.VirtualFlow;
import javafx.scene.Node;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import net.smoofyuniverse.common.fxui.control.AbstractListCell;
import net.smoofyuniverse.common.util.GridUtil;
import net.smoofyuniverse.common.util.StringUtil;
import net.smoofyuniverse.epi.stats.collection.DataCollection;
import net.smoofyuniverse.epi.stats.ranking.Ranking;

import java.text.DecimalFormat;
import java.util.Optional;

public final class RankingView extends ListView<Integer> {
	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0#####");

	private Ranking ranking;
	private UserInterface ui;

	public RankingView(UserInterface ui) {
		this.ui = ui;
		
		setCellFactory(l -> new StatsListCell());
		getSelectionModel().selectedIndexProperty().addListener((v, oldV, newV) -> this.ui.getRankingListPanel().setSelectedIndex(newV.intValue()));
	}
	
	public Optional<Ranking> currentRanking() {
		return Optional.ofNullable(this.ranking);
	}

	public void scrollToVisible(int index) {
		VirtualFlow<?> flow = (VirtualFlow<?>) lookup(".virtual-flow");
		IndexedCell firstCell = flow.getFirstVisibleCell();
		if (firstCell == null)
			return;
		int first = firstCell.getIndex();
		int last = flow.getLastVisibleCell().getIndex();
		if (index <= first) {
			while (index <= first && flow.adjustPixels((index - first) - 1) < 0) {
				first = flow.getFirstVisibleCell().getIndex();
			}
		} else {
			while (index >= last && flow.adjustPixels((index - last) + 1) > 0) {
				last = flow.getLastVisibleCell().getIndex();
			}
		}
	}
	
	public void open(Ranking r) {
		this.ranking = r;
		if (r == null)
			getItems().clear();
		else
			getItems().setAll(r.list());
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
			DataCollection col = RankingView.this.ranking.parent.collection;
			int p = getItem();
			
			this.index.setText("#" + (getIndex() +1));
			this.name.setText(col.names.get(p));
			this.value.setText(DECIMAL_FORMAT.format(RankingView.this.ranking.getValue(p)));
			this.tooltip.setText(col.containsIntervals ? (StringUtil.DATETIME_FORMAT.format(col.startDates.get(p)) + " - " + StringUtil.DATETIME_FORMAT.format(col.endDates.get(p))) : StringUtil.DATETIME_FORMAT.format(col.endDates.get(p)));
			
			return this.content;
		}
	}
}
