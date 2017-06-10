package net.smoofyuniverse.epi.stats.operation;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;
import net.smoofyuniverse.common.fxui.task.ObservableTask;
import net.smoofyuniverse.epi.stats.Ranking;
import net.smoofyuniverse.epi.stats.RankingList;

public class CategoryDeletion implements RankingOperation {
	public final Predicate<String> category;
	
	public CategoryDeletion(Predicate<String> category) {
		this.category = category;
	}

	@Override
	public void accept(RankingList list, ObservableTask task) {
		task.setTitle("Suppression des catégories ..");
		task.setProgress(0);
		
		Collection<Ranking> l = list.getRankings();
		int total = l.size(), i = 0;
		
		Iterator<Ranking> it = l.iterator();
		while (it.hasNext()) {
			Ranking r = it.next();
			if (this.category.test(r.name)) {
				task.setMessage("Suppression de " + r.name + " ..");
				it.remove();
			}
			task.setProgress(++i / total);
		}
	}
}
