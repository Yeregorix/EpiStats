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

import net.smoofyuniverse.common.task.Task;
import net.smoofyuniverse.epi.stats.ranking.RankingList;
import net.smoofyuniverse.epi.util.ImmutableDoubleList;

import java.util.Map;
import java.util.Map.Entry;

public class ImportOperation implements RankingOperation {
	public final String category;

	public ImportOperation(String category) {
		this.category = category;
	}

	@Override
	public void accept(RankingList list, Task task) {
		boolean all = this.category.equals("*");

		task.setTitle(all ? "Importation de toutes les catégories .." : "Importation de la catégorie '" + this.category + "' ..");
		task.setProgress(0);

		Map<String, Map<String, ImmutableDoubleList>> stats = list.collection.stats;
		if (all) {
			for (Entry<String, Map<String, ImmutableDoubleList>> section : stats.entrySet()) {
				if (task.isCancelled())
					return;

				String category = section.getKey();
				int total = section.getValue().size(), i = 0;
				for (Entry<String, ImmutableDoubleList> e : section.getValue().entrySet()) {
					String key = e.getKey();
					String name = category + "_" + (key.startsWith("stat_") ? key.substring(5) : key);
					task.setMessage("Catégorie: " + name);
					list.getOrCreate(name).set(e.getValue());
					task.setProgress(++i / (double) total);
				}
			}
		} else {
			Map<String, ImmutableDoubleList> section = stats.get(this.category);
			if (section != null) {
				int total = section.size(), i = 0;
				for (Entry<String, ImmutableDoubleList> e : section.entrySet()) {
					String key = e.getKey();
					String name = this.category + "_" + (key.startsWith("stat_") ? key.substring(5) : key);
					task.setMessage("Catégorie: " + name);
					list.getOrCreate(name).set(e.getValue());
					task.setProgress(++i / (double) total);
				}
			}
		}
	}
}
