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
			logger.debug(i + " - " + r.parent.getPlayerName(p) + ": " + r.getValue(p));
			i++;
		}
	}
}
