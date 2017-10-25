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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

public class Ranking {
	public final RankingList parent;
	public final String name;
	public boolean descendingMode;
	private double[] values;
	private TreeSet<Integer> players;

	public Ranking(RankingList parent, String name) {
		this(new double[parent.getCollection().getPlayerCount()], parent, name);
		this.players = new TreeSet<>(this::compare);
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
	
	public void put(int p, double v) {
		if (Double.isNaN(v)) {
			this.values[p] = 0;
			this.players.remove(p);
		} else {
			this.values[p] = v;
			this.players.add(p);
		}
	}

	public int size() {
		return this.players.size();
	}
	
	public Iterator<Integer> iterator() {
		return this.descendingMode ? this.players.descendingIterator() : this.players.iterator();
	}
	
	public Collection<Integer> collection() {
		return this.descendingMode ? this.players.descendingSet() : this.players;
	}

	public double getValue(int p) {
		return this.players.contains(p) ? this.values[p] : Double.NaN;
	}
	
	public int getRank(int p) {
		if (this.players.contains(p)) {
			int r = this.players.headSet(p).size();
			return r == 0 ? 0 : r - 1;
		}
		return -1;
	}

	public int compare(int p1, int p2) {
		return -Double.compare(this.values[p1], this.values[p2]);
	}
}
