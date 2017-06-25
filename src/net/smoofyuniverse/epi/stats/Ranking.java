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

package net.smoofyuniverse.epi.stats;

import org.mariuszgromada.math.mxparser.Function;
import org.mariuszgromada.math.mxparser.FunctionExtension;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

public class Ranking {
	public final GetExtension getExtension = new GetExtension();
	public final RankExtension rankExtension = new RankExtension();
	public final Function getFunction, rankFunction;
	public final RankingList parent;
	public final String name;
	public boolean descendingMode;
	private double[] values;
	private TreeSet<Integer> players = new TreeSet<>((p1, p2) -> this.values[p1] > this.values[p2] ? -1 : 1);
	
	public Ranking(RankingList parent, String name, int size) {
		this.values = new double[size];
		this.getFunction = new Function("get_" + name, this.getExtension);
		this.rankFunction = new Function("rank_" + name, this.rankExtension);
		this.parent = parent;
		this.name = name;
	}
	
	public void put(int p, double v) {
		this.values[p] = v;
		if (Double.isFinite(v))
			this.players.add(p);
	}
	
	public Iterator<Integer> iterator() {
		return this.descendingMode ? this.players.descendingIterator() : this.players.iterator();
	}
	
	public Collection<Integer> collection() {
		return this.descendingMode ? this.players.descendingSet() : this.players;
	}
	
	public double getValue(int p) {
		return this.values[p];
	}
	
	public int getRank(int p) {
		if (Double.isFinite(this.values[p]))
			return this.players.headSet(p).size();
		return -1;
	}
	
	public class GetExtension implements FunctionExtension {
		public int player;

		@Override
		public double calculate(double... params) {
			return getValue(this.player);
		}

		@Override
		public FunctionExtension clone() {
			return this;
		}

		@Override
		public int getParametersNumber() {
			return 1;
		}

		@Override
		public void setParameterValue(int index, double param) {
			if (index == 0)
				this.player = (int) param;
		}
	}
	
	public class RankExtension implements FunctionExtension {
		public int player;

		@Override
		public double calculate(double... params) {
			int r = getRank(this.player);
			if (r == -1)
				return Double.NaN;
			return r +1;
		}

		@Override
		public FunctionExtension clone() {
			return this;
		}

		@Override
		public int getParametersNumber() {
			return 1;
		}

		@Override
		public void setParameterValue(int index, double param) {
			if (index == 0)
				this.player = (int) param;
		}
	}
}
