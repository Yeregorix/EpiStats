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

package net.smoofyuniverse.epi.util;

import java.util.Arrays;

public class DoubleList {
	protected double[] array;
	protected int size;

	public DoubleList(int capacity) {
		this.array = new double[capacity];
	}

	protected DoubleList() {}

	public double get(int index) {
		return this.array[index];
	}

	public void add(double value) {
		this.array[this.size] = value;
		this.size++;
	}

	public void set(int index, double value) {
		this.array[index] = value;
		if (index >= this.size)
			this.size = index + 1;
	}

	public double remove(int index) {
		double oldV = this.array[index];
		if (index >= this.size)
			return 0;
		int mov = this.size - index - 1;
		if (mov > 0)
			System.arraycopy(this.array, index + 1, this.array, index, mov);
		this.array[--this.size] = 0;
		return oldV;
	}

	public void clear() {
		Arrays.fill(this.array, 0);
		this.size = 0;
	}

	public DoubleIterator iterator() {
		return new Iterator();
	}

	public int size() {
		return this.size;
	}

	public ImmutableDoubleList toImmutable() {
		return ImmutableDoubleList.of(toArray());
	}

	public double[] toArray() {
		return toArray(new double[this.size]);
	}

	public double[] toArray(double[] array) {
		System.arraycopy(this.array, 0, array, 0, Math.min(this.size, array.length));
		return array;
	}

	private class Iterator implements DoubleIterator {
		private int index = 0;

		@Override
		public boolean hasNext() {
			return this.index < DoubleList.this.size;
		}

		@Override
		public double next() {
			return DoubleList.this.array[this.index++];
		}

		@Override
		public void remove() {
			DoubleList.this.remove(this.index);
		}
	}
}
