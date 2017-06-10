package net.smoofyuniverse.epi.stats;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import org.mariuszgromada.math.mxparser.Function;
import org.mariuszgromada.math.mxparser.FunctionExtension;

public class Ranking {
	private double[] values;
	private TreeSet<Integer> players = new TreeSet<>((p1, p2) -> this.values[p1] > this.values[p2] ? -1 : 1);
	
	public final GetExtension getExtension = new GetExtension();
	public final RankExtension rankExtension = new RankExtension();
	public final Function getFunction, rankFunction;
	public final RankingList parent;
	public final String name;
	
	public boolean descendingMode;
	
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
