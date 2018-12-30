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
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import net.smoofyuniverse.common.util.GridUtil;
import net.smoofyuniverse.common.util.TextUtil;
import net.smoofyuniverse.epi.EpiStats;

public class ConnectionConfigPanel extends GridPane {

	private final UserInterface ui;
	private final EpiStats epi;
	private Hyperlink userAgentL = TextUtil.openLink("User-Agent:", "http://useragent.fr/");
	private TextField userAgent = new TextField();

	public ConnectionConfigPanel(UserInterface ui) {
		this.ui = ui;
		this.epi = ui.getEpiStats();

		this.userAgent.setPromptText(this.epi.getConnectionConfig().userAgent);
		this.userAgent.textProperty().addListener((v, oldV, newV) -> this.epi.setPreferredConnectionConfig(
				newV == null || newV.isEmpty() ? this.epi.getConnectionConfig() : this.epi.getConnectionConfig().toBuilder().userAgent(newV).build()));

		setVgap(4);
		setHgap(4);

		add(UserInterface.title("0 - Options de connection:"), 0, 0, 2, 1);

		add(this.userAgentL, 0, 1);
		add(this.userAgent, 1, 1);

		getColumnConstraints().addAll(GridUtil.createColumn(15), GridUtil.createColumn(85));
	}

	public StringProperty getUserAgent() {
		return this.userAgent.textProperty();
	}
}
