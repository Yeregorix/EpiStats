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
package net.smoofyuniverse.epi.stats.operation;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import net.smoofyuniverse.common.fxui.task.ObservableTask;
import net.smoofyuniverse.epi.api.PlayerInfo;
import net.smoofyuniverse.epi.stats.Ranking;
import net.smoofyuniverse.epi.stats.RankingList;

public class CategoryGeneration implements RankingOperation {
	public final String category;
	public final Expression expression;
	public final Argument arg = new Argument("p", -1);
	
	public CategoryGeneration(String category, Expression expression) {
		this.category = category;
		this.expression = expression;
		this.expression.addArguments(arg);
	}

	@Override
	public void accept(RankingList list, ObservableTask task) throws OperationException {
		task.setTitle("Génération de la catégorie '" + this.category + "' ..");
		task.setProgress(0);
		
		this.expression.removeAllFunctions();
		this.expression.addFunctions(list.getFunctions());
		
		if (!this.expression.checkSyntax())
			throw new OperationException(this.expression.getErrorMessage());
		
		Ranking r = list.getOrCreate(this.category);
		
		int total = list.getPlayerCount();
		for (int i = 0; i < total; i++) {
			if (task.isCancelled())
				return;
			PlayerInfo p = list.getPlayer(i);
			task.setMessage("Joueur: " + p.name);
			this.arg.setArgumentValue(i);
			r.put(i, this.expression.calculate());
			task.setProgress(i / (double) total);
		}
	}
}
