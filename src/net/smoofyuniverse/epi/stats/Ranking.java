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

import java.util.*;

public class Ranking implements Comparator<Integer> {
	public final RankingList parent;
	public final String name;
	public boolean descendingMode;
	private double[] values;
	private TreeSet<Integer> players;
	
	public Ranking(RankingList parent, String name, int size) {
		this(new double[size], parent, name);
		this.players = new TreeSet<>(this);
	}

	private Ranking(double[] values, RankingList parent, String name) {
		this.values = values;
		this.parent = parent;
		this.name = name;
	}

	public Ranking copy(String newName) {
		Ranking r = new Ranking(Arrays.copyOf(this.values, this.values.length), this.parent, newName);
		r.players = (TreeSet<Integer>) this.players.clone();
		return r;
	}

	public Ranking rename(String newName) {
		Ranking r = new Ranking(this.values, this.parent, newName);
		r.players = this.players;
		return r;
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
		if (Double.isFinite(this.values[p])) {
			int r = this.players.headSet(p).size();
			return r == 0 ? 0 : r - 1;
		}
		return -1;
	}

	@Override
	public int compare(Integer p1, Integer p2) {
		return this.values[p1] > this.values[p2] ? -1 : 1;
	}
}
