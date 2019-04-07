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

import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import net.smoofyuniverse.common.app.App;
import net.smoofyuniverse.common.fx.dialog.Popup;
import net.smoofyuniverse.common.util.GridUtil;
import net.smoofyuniverse.epi.stats.collection.DataCollection;
import net.smoofyuniverse.epi.stats.operation.OperationException;
import net.smoofyuniverse.epi.stats.operation.RankingOperation;
import net.smoofyuniverse.epi.stats.ranking.RankingList;
import net.smoofyuniverse.logger.core.Logger;

public final class GenerationPanel extends GridPane {
	private static final Logger logger = App.getLogger("GenerationPanel");

	private TextArea editor = new TextArea();
	private Button generate = new Button("Générer");

	private RankingOperation operation;

	private UserInterface ui;

	public GenerationPanel(UserInterface ui) {
		this.ui = ui;

		this.editor.setPrefSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
		this.generate.setPrefWidth(Integer.MAX_VALUE);

		this.editor.textProperty().addListener((v, oldV, newV) -> parseEditor());

		this.generate.setDisable(true);

		this.generate.setOnAction(a -> {
			DataCollection col = this.ui.getDataCollectionPanel().getTargetCollection().orElse(null);
			if (col == null)
				return;

			Popup.consumer((task) -> {
				logger.info("Generating ranking list ..");
				long time = System.currentTimeMillis();

				RankingList l = new RankingList(col);

				try {
					this.operation.accept(l, task);
				} catch (OperationException e) {
					logger.warn("Generation was interrupted by an error at line " + e.line + ".");
					Popup.error().title("Erreur de génération").header("Une erreur est survenue ligne " + e.line + ".").expandable(new Label(e.getMessage())).show();
					return;
				}

				logger.info("Generated ranking list in " + (System.currentTimeMillis() - time) / 1000F + "s.");

				Popup.info().title("Génération terminée").message("Un classement contenant " + l.getRankings().size() + " " + (l.getRankings().size() > 1 ? "catégories" : "catégorie")
						+ " a été généré avec " + l.collection.size + " " + (l.collection.size > 1 ? "joueurs" : "joueur") + ".").show();
				this.ui.getRankingListPanel().open(l);
			}).title("Génération des classements ..").submitAndWait();
		});

		setVgap(4);
		setHgap(4);

		add(UserInterface.title("3 - Instructions de traitement:"), 0, 0, 4, 1);

		add(this.editor, 0, 1, 4, 1);
		add(this.generate, 1, 2, 2, 1);

		getColumnConstraints().addAll(GridUtil.createColumn(25), GridUtil.createColumn(25), GridUtil.createColumn(25), GridUtil.createColumn(25));
	}

	private void parseEditor() {
		try {
			String s = this.editor.getText();
			this.operation = s.isEmpty() ? null : RankingOperation.parse(s.split("\n"));
		} catch (Exception e) {
			this.operation = null;
		}
		this.generate.setDisable(this.operation == null);
	}

	public StringProperty getEditor() {
		return this.editor.textProperty();
	}
}
