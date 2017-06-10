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
			PlayerInfo p = list.infosCache[i];
			task.setMessage("Joueur: " + p.name);
			this.arg.setArgumentValue(i);
			r.put(i, this.expression.calculate());
			task.setProgress(i / (double) total);
		}
	}
}
