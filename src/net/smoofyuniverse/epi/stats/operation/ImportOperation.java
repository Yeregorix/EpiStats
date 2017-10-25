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
import net.smoofyuniverse.epi.api.PlayerInfo;
import net.smoofyuniverse.epi.stats.RankingList;

import java.util.Map;
import java.util.Map.Entry;

public class ImportOperation implements RankingOperation {
	public final String category;

	public ImportOperation(String category) {
		this.category = category;
	}

	@Override
	public void accept(RankingList list, ObservableTask task) {
		boolean all = this.category.equals("*");

		task.setTitle(all ? "Importation de toutes les catégories .." : "Importation de la catégorie '" + this.category + "' ..");
		task.setProgress(0);

		boolean useIntervals = list.getCollection().containsIntervals();
		int total = list.getPlayerCount();
		for (int i = 0; i < total; i++) {
			if (task.isCancelled())
				return;
			PlayerInfo p = list.getPlayer(i);
			task.setMessage("Joueur: " + p.name);

			if (all) {
				for (Entry<String, Map<String, Double>> stats : p.endStats.entrySet()) {
					String category = stats.getKey();

					Map<String, Double> map2 = null;
					if (useIntervals) {
						map2 = p.startStats.get(category);
						if (map2 == null)
							continue;
					}

					for (Entry<String, Double> e : stats.getValue().entrySet()) {
						String key = e.getKey();
						double value = e.getValue();

						if (useIntervals) {
							Double v2 = map2.get(key);
							if (v2 == null)
								continue;
							value -= v2;
						}

						list.getOrCreate(category + "_" + (key.startsWith("stat_") ? key.substring(5) : key)).put(i, value);
					}
				}
			} else {
				Map<String, Double> stats = p.endStats.get(this.category);
				if (stats != null) {
					Map<String, Double> map2 = useIntervals ? p.startStats.get(this.category) : null;
					if (!useIntervals || map2 != null) {
						for (Entry<String, Double> e : stats.entrySet()) {
							String key = e.getKey();
							double value = e.getValue();

							if (useIntervals) {
								Double v2 = map2.get(key);
								if (v2 == null)
									continue;
								value -= v2;
							}

							list.getOrCreate(category + "_" + (key.startsWith("stat_") ? key.substring(5) : key)).put(i, value);
						}
					}
				}
			}

			task.setProgress(i / (double) total);
		}
	}
}
