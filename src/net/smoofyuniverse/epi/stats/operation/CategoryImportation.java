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

import java.util.Map;
import java.util.Map.Entry;

import net.smoofyuniverse.common.fxui.task.ObservableTask;
import net.smoofyuniverse.epi.api.PlayerInfo;
import net.smoofyuniverse.epi.stats.RankingList;

public class CategoryImportation implements RankingOperation {
	public final String category;
	
	public CategoryImportation(String category) {
		this.category = category;
	}

	@Override
	public void accept(RankingList list, ObservableTask task) {
		task.setTitle("Importation de la catégorie '" + this.category + "' ..");
		task.setProgress(0);
		
		int total = list.infosCache.length;
		for (int i = 0; i < total; i++) {
			if (task.isCancelled())
				return;
			PlayerInfo p = list.infosCache[i];
			task.setMessage("Joueur: " + p.name);
			
			Map<String, Double> stats = p.stats.get(this.category);
			if (stats != null) {
				for (Entry<String, Double> e : stats.entrySet()) {
					String key = e.getKey();
					if (key.startsWith("stat_"))
						key = key.substring(5);
					list.getOrCreate(this.category + "_" + key).put(i, e.getValue());
				}
			}
			
			task.setProgress(i / (double) total);
		}
	}
}
