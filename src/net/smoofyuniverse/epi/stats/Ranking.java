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

public class Ranking {
	public final RankingList parent;
	public final String name;
	public boolean descending = false;

	private ImmutableList<Integer> sortedPlayers;
	private double[] values;
	private int size = 0;

	public Ranking(RankingList parent, String name) {
		this.values = new double[parent.getCollection().getPlayerCount()];
		this.parent = parent;
		this.name = name;
		Arrays.fill(this.values, Double.NaN);
	}

	private Ranking(Ranking r, String newName) {
		this.values = Arrays.copyOf(r.values, r.values.length);
		this.parent = r.parent;
		this.name = newName;
		this.descending = r.descending;
		this.size = r.size;
	}

	public Ranking copy(String newName) {
		return new Ranking(this, newName);
	}

	public double put(int p, double v) {
		double oldV = this.values[p];
		if (v == v) {
			this.values[p] = v;
			if (v != oldV)
				this.sortedPlayers = null;
			if (oldV != oldV)
				this.size++;
		} else if (oldV == oldV) {
			this.values[p] = v;
			this.sortedPlayers = null;
			this.size--;
		}
		return oldV;
	}

	public double remove(int p) {
		double oldV = this.values[p];
		if (oldV == oldV) {
			this.values[p] = Double.NaN;
			this.sortedPlayers = null;
			this.size--;
		}
		return oldV;
	}

	public boolean contains(int p) {
		double v = this.values[p];
		return v == v;
	}

	public int size() {
		return this.size;
	}

	public int getRank(int p) {
		return contains(p) ? collection().indexOf(p) : -1;
	}

	public double getValue(int p) {
		return this.values[p];
	}

	public ImmutableList<Integer> collection() {
		if (this.sortedPlayers == null)
			this.sortedPlayers = ImmutableList.of(toSortedArray());
		return this.descending ? this.sortedPlayers.inverseOrder() : this.sortedPlayers;
	}

	public Integer[] toSortedArray() {
		Integer[] array = toArray();
		Arrays.sort(array, this::compare);
		return array;
	}

	public Integer[] toArray() {
		Integer[] array = new Integer[this.size];
		int index = 0;
		for (int p = 0; p < this.values.length; p++) {
			double v = this.values[p];
			if (v == v)
				array[index++] = p;
		}
		return array;
	}

	public int compare(int p1, int p2) {
		int r = Double.compare(this.values[p1], this.values[p2]);
		return r == 0 ? Integer.compare(p1, p2) : -r;
	}
}
