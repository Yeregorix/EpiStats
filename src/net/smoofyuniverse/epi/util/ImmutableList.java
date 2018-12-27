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

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;

public class ImmutableList<T> extends AbstractList<T> {
	private static final ImmutableList EMPTY = new ImmutableList(new Object[0]);

	protected ImmutableList<T> inverse;
	protected T[] array;

	private ImmutableList(T[] array) {
		this.array = array;
		this.inverse = new Inverted<>(this);
	}

	private ImmutableList() {}

	@Override
	public T get(int index) {
		return this.array[index];
	}

	@Override
	public int size() {
		return this.array.length;
	}

	public ImmutableList<T> originalOrder() {
		return this;
	}

	public ImmutableList<T> inverseOrder() {
		return this.inverse;
	}

	public static <T> ImmutableList<T> empty() {
		return EMPTY;
	}

	public static <T> ImmutableList<T> of(T[] array) {
		if (array.length == 0)
			return EMPTY;
		return new ImmutableList<>(array);
	}

	public static <T> ImmutableList<T> copyOf(T[] array) {
		if (array.length == 0)
			return EMPTY;
		return new ImmutableList<>(Arrays.copyOf(array, array.length));
	}

	public static <T> ImmutableList<T> copyOf(Collection<T> col) {
		if (col.isEmpty())
			return EMPTY;
		if (col instanceof ImmutableList)
			return (ImmutableList<T>) col;
		return new ImmutableList(col.toArray(new Object[0]));
	}

	private static class Inverted<T> extends ImmutableList<T> {
		public Inverted(ImmutableList<T> list) {
			this.array = list.array;
			this.inverse = list;
		}

		@Override
		public T get(int index) {
			return this.array[this.array.length - 1 - index];
		}

		@Override
		public ImmutableList<T> originalOrder() {
			return this.inverse;
		}
	}
}
