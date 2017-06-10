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
