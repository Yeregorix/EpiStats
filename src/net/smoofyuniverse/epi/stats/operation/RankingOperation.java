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
import net.smoofyuniverse.epi.stats.ranking.RankingList;
import org.mariuszgromada.math.mxparser.Expression;

import java.util.regex.Pattern;

public interface RankingOperation {
	public final static Pattern CATEGORY_NAME = Pattern.compile("([a-zA-Z_])+([a-zA-Z0-9_])*");
	public static final RankingOperation EMPTY = (l, t) -> {};

	public static String validateName(String category) {
		if (category.startsWith("rank_") || category.startsWith("total_"))
			throw new IllegalArgumentException("Keyword in name");
		if (!CATEGORY_NAME.matcher(category).matches())
			throw new IllegalArgumentException("Invalid name");
		return category;
	}

	public static RankingOperation parse(String[] lines) {
		RankingOperation[] children = new RankingOperation[lines.length];
		for (int i = 0; i < lines.length; i++)
			children[i] = parse(lines[i]);
		return merge(children);
	}

	public static RankingOperation parse(String line) {
		String[] args = line.split("\\s+");
		if (args.length == 0 || args[0].isEmpty())
			return EMPTY;

		switch (args[0]) {
			case "import":
				if (args.length == 2)
					return new ImportOperation(args[1]);
				break;
			case "delete":
				if (args.length == 2)
					return new DeleteOperation(StringUtil.simplePredicate(args[1]));
				break;
			case "move":
				if (args.length == 3)
					return new MoveOperation(args[1], args[2]);
				break;
			case "copy":
				if (args.length == 3)
					return new CopyOperation(args[1], args[2]);
				break;
			case "inverse":
				if (args.length == 2)
					return new InverseOperation(StringUtil.simplePredicate(args[1]));
				break;
			case "debug":
				if (args.length == 2)
					return new DebugOperation(StringUtil.simplePredicate(args[1]));
				break;
			case "filter":
				if (args.length > 2)
					return new FilterOperation(StringUtil.simplePredicate(args[1]), new Expression(line.substring(args[1].length() + 8)));
				break;
			case "generate":
				if (args.length > 2)
					return new GenerateOperation(args[1], new Expression(line.substring(args[1].length() + 10)));
				break;
			default:
				throw new IllegalArgumentException("Invalid operation: " + args[0]);
		}

		throw new IllegalArgumentException("Invalid arguments length: " + args.length);
	}

	public static RankingOperation merge(RankingOperation... children) {
		return (list, task) -> {
			int line = 0;
			for (RankingOperation op : children) {
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

	public void accept(RankingList list, ObservableTask task) throws OperationException;
}
