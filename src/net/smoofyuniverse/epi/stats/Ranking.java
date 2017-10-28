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

import net.smoofyuniverse.epi.util.ImmutableList;

import java.util.Arrays;
import java.util.HashSet;

public class Ranking {
	public final RankingList parent;
	public final String name;
	public boolean descending;

	private HashSet<Integer> players;
	private ImmutableList<Integer> sortedPlayers;
	private double[] values;

	public Ranking(RankingList parent, String name) {
		this(new HashSet<>(), new double[parent.getCollection().getPlayerCount()], parent, name, false);
		Arrays.fill(this.values, Double.NaN);
	}

	private Ranking(HashSet<Integer> players, double[] values, RankingList parent, String name, boolean descending) {
		this.players = players;
		this.values = values;

		this.parent = parent;
		this.name = name;
		this.descending = descending;
	}

	public Ranking copy(String newName) {
		return new Ranking((HashSet) this.players.clone(), this.values.clone(), this.parent, newName, this.descending);
	}
	
	public void put(int p, double v) {
		if (Double.isNaN(v)) {
			remove(p);
		} else {
			if (!contains(p))
				this.players.add(p);
			this.values[p] = v;
			this.sortedPlayers = null;
		}
	}

	public void remove(int p) {
		if (contains(p)) {
			this.players.remove(p);
			this.values[p] = Double.NaN;
			this.sortedPlayers = null;
		}
	}

	public boolean contains(int p) {
		return !Double.isNaN(this.values[p]);
	}

	public int size() {
		return this.players.size();
	}

	public int getRank(int p) {
		return contains(p) ? collection().indexOf(p) : -1;
	}

	public double getValue(int p) {
		return this.values[p];
	}

	public ImmutableList<Integer> collection() {
		if (this.sortedPlayers == null)
			this.sortedPlayers = ImmutableList.sortedCopyOf(this.players, this::compare);
		return this.descending ? this.sortedPlayers.inverseOrder() : this.sortedPlayers;
	}

	public int compare(int p1, int p2) {
		int r = Double.compare(this.values[p1], this.values[p2]);
		return r == 0 ? Integer.compare(p1, p2) : -r;
	}
}
