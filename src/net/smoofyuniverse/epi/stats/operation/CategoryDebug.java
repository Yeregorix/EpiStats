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

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

import net.smoofyuniverse.common.app.Application;
import net.smoofyuniverse.common.fxui.task.ObservableTask;
import net.smoofyuniverse.common.logger.core.Logger;
import net.smoofyuniverse.epi.stats.Ranking;
import net.smoofyuniverse.epi.stats.RankingList;

public class CategoryDebug implements RankingOperation {
	private static final Logger logger = Application.getLogger("CategoryDebug");
	
	public final Predicate<String> category;
	
	public CategoryDebug(Predicate<String> category) {
		this.category = category;
	}

	@Override
	public void accept(RankingList list, ObservableTask task) {
		task.setTitle("Debug des catégories ..");
		task.setProgress(0);
		
		Collection<Ranking> l = list.getRankings();
		int total = l.size(), i = 0;
		
		Iterator<Ranking> it = l.iterator();
		while (it.hasNext()) {
			Ranking r = it.next();
			if (this.category.test(r.name)) {
				task.setMessage("Debug de " + r.name + " ..");
				debug(r);
			}
			task.setProgress(++i / total);
		}
	}
	
	public static void debug(Ranking r) {
		logger.debug("Catégorie: " + r.name);
		int i = 1;
		Iterator<Integer> it = r.iterator();
		while (it.hasNext()) {
			int p = it.next();
			logger.debug(i + " - " + r.parent.getPlayer(p).name + ": " + r.getValue(p));
			i++;
		}
	}
}
