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
import java.util.Collections;
import java.util.NavigableSet;
import java.util.TreeSet;

public class Ranking {
	public final RankingList parent;
	public final String name;
	public boolean descendingMode;

	private NavigableSet<Integer> players, unmodifiablePlayers;
	private double[] values;

	public Ranking(RankingList parent, String name) {
		this.players = new TreeSet<>(this::compare);
		this.unmodifiablePlayers = Collections.unmodifiableNavigableSet(this.players);
		this.values = new double[parent.getCollection().getPlayerCount()];
		Arrays.fill(this.values, Double.NaN);

		this.parent = parent;
		this.name = name;
	}

	private Ranking(NavigableSet<Integer> players, double[] values, RankingList parent, String name, boolean descendingMode) {
		this.players = players;
		this.unmodifiablePlayers = Collections.unmodifiableNavigableSet(this.players);
		this.values = values;

		this.parent = parent;
		this.name = name;
		this.descendingMode = descendingMode;
	}

	public Ranking copy(String newName) {
		return new Ranking((TreeSet) ((TreeSet) this.players).clone(), Arrays.copyOf(this.values, this.values.length), this.parent, newName, this.descendingMode);
	}
	
	public void put(int p, double v) {
		this.players.remove(p);
		this.values[p] = v;
		if (!Double.isNaN(v))
			this.players.add(p);
	}

	public void remove(int p) {
		this.players.remove(p);
		this.values[p] = Double.NaN;
	}

	public boolean contains(int p) {
		return !Double.isNaN(this.values[p]);
	}

	public int size() {
		return this.players.size();
	}

	public NavigableSet<Integer> collection() {
		return this.descendingMode ? this.unmodifiablePlayers.descendingSet() : this.unmodifiablePlayers;
	}

	public double getValue(int p) {
		return this.values[p];
	}

	public int getRank(int p) {
		return contains(p) ? collection().headSet(p).size() : -1;
	}

	public int compare(int p1, int p2) {
		int r = Double.compare(this.values[p1], this.values[p2]);
		return r == 0 ? Integer.compare(p1, p2) : -r;
	}
}
