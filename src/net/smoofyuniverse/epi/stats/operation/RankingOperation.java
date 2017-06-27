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
import net.smoofyuniverse.common.util.StringUtil;
import net.smoofyuniverse.epi.stats.RankingList;
import org.mariuszgromada.math.mxparser.Expression;

import java.util.Optional;

public interface RankingOperation {
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
		} catch (Exception ignored) {
		}
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

	public void accept(RankingList list, ObservableTask task) throws OperationException;
}
