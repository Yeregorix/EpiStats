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

public class ImmutableDoubleList extends DoubleList {

	private ImmutableDoubleList(double[] array) {
		this.array = array;
		this.size = this.array.length;
	}

	@Override
	public void add(double value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(int index, double value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ImmutableDoubleList toImmutable() {
		return this;
	}

	public static ImmutableDoubleList of(double[] array) {
		return new ImmutableDoubleList(array);
	}

	public static ImmutableDoubleList copyOf(double[] array) {
		return new ImmutableDoubleList(Arrays.copyOf(array, array.length));
	}
}
