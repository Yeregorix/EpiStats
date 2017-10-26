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
import net.smoofyuniverse.epi.stats.Ranking;
import net.smoofyuniverse.epi.stats.RankingList;

import java.util.List;
import java.util.function.Predicate;

public class InverseOperation implements RankingOperation {
	public final Predicate<String> category;

	public InverseOperation(Predicate<String> category) {
		this.category = category;
	}

	@Override
	public void accept(RankingList list, ObservableTask task) {
		task.setTitle("Inversion des catégories ..");
		task.setProgress(0);

		List<Ranking> l = list.list(this.category);
		int total = l.size(), i = 0;

		for (Ranking r : l) {
			task.setMessage("Inversion de " + r.name + " ..");
			r.descendingMode = !r.descendingMode;
			task.setProgress(++i / total);
		}
	}
}
