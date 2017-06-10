package net.smoofyuniverse.epi.stats.operation;

import java.util.Optional;

import org.mariuszgromada.math.mxparser.Expression;
import net.smoofyuniverse.common.fxui.task.ObservableTask;
import net.smoofyuniverse.common.util.StringUtil;
import net.smoofyuniverse.epi.stats.RankingList;

public interface RankingOperation {
	public void accept(RankingList list, ObservableTask task) throws OperationException;
	
	public static RankingOperation merge(RankingOperation... childs) {
		return (list, task) -> {
			int line = 0;
			for (RankingOperation op : childs) {
				line++;
				try {
					op.accept(list, task);
				} catch (OperationException e) {
					e.line = line;
					throw e;
				}
			}
		};
	}
	
	public static Optional<RankingOperation> parse(String operation) {
		String[] args = operation.split("\\s+");
		if (args.length == 0)
			return Optional.empty();
		
		try {
			switch (args[0]) {
			case "import":
				if (args.length == 2)
					return Optional.of(new CategoryImportation(args[1]));
				break;
			case "delete":
				if (args.length == 2)
					return Optional.of(new CategoryDeletion(StringUtil.simplePredicate(args[1])));
				break;
			case "inverse":
				if (args.length == 2)
					return Optional.of(new OrderInversion(StringUtil.simplePredicate(args[1])));
				break;
			case "debug":
				if (args.length == 2)
					return Optional.of(new CategoryDebug(StringUtil.simplePredicate(args[1])));
				break;
			case "generate":
				if (args.length > 2)
					return Optional.of(new CategoryGeneration(args[1], new Expression(operation.substring(args[1].length() +10))));
				break;
			}
		} catch (Exception e) {}
		return Optional.empty();
	}
	
	public static Optional<RankingOperation> parseAll(String operations) {
		String[] lines = operations.split("\n");
		RankingOperation[] childs = new RankingOperation[lines.length];
		for (int i = 0; i < lines.length; i++) {
			RankingOperation op = parse(lines[i]).orElse(null);
			if (op == null)
				return Optional.empty();
			childs[i] = op;
		}
		return Optional.of(merge(childs));
	}
}
