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

package net.smoofyuniverse.epi.stats.operation;

import net.smoofyuniverse.common.fxui.task.ObservableTask;
import net.smoofyuniverse.epi.stats.ranking.Ranking;
import net.smoofyuniverse.epi.stats.ranking.RankingList;
import org.mariuszgromada.math.mxparser.Expression;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class FilterOperation implements RankingOperation {
	public final Predicate<String> category;
	public final Expression expression;

	public FilterOperation(Predicate<String> category, Expression expression) {
		this.category = category;
		if (!expression.checkLexSyntax())
			throw new IllegalArgumentException("Invalid syntax");
		this.expression = expression;
	}

	@Override
	public void accept(RankingList list, ObservableTask task) throws OperationException {
		task.setTitle("Filtrage des catégories ..");
		task.setProgress(0);

		AtomicInteger p = new AtomicInteger();

		this.expression.removeAllArguments();
		this.expression.addArguments(list.getArguments(this.expression.getExpressionString(), p));

		if (!this.expression.checkSyntax())
			throw new OperationException(this.expression.getErrorMessage());

		List<Ranking> l = list.list(this.category);
		if (l.isEmpty())
			return;

		int total = list.collection.size;
		for (int i = 0; i < total; i++) {
			if (task.isCancelled())
				return;

			task.setMessage("Joueur: " + list.collection.names.get(i));
			p.set(i);

			if (this.expression.calculate() == 0d) {
				for (Ranking r : l)
					r.remove(i);
			}
			task.setProgress(i / (double) total);
		}
	}
}
