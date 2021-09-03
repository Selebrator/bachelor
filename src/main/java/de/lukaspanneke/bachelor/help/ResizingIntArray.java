package de.lukaspanneke.bachelor.help;

import java.util.*;

public class ResizingIntArray implements RandomAccess {

	private int size;
	private int[] array;

	public ResizingIntArray() {
		this(16);
	}

	public ResizingIntArray(int initialCapacity) {
		this.size = 0;
		this.array = new int[Math.max(4, initialCapacity)];
	}

	public int get(int index) {
		Objects.checkIndex(index, this.size);
		return this.array[index];
	}

	public int getAndIncrementLast() {
		return this.array[this.size - 1]++;
	}

	public int size() {
		return this.size;
	}

	public boolean isEmpty() {
		return this.size == 0;
	}

	public void addLast(int value) {
		this.ensureCapacity(this.size + 1);
		this.array[this.size++] = value;
	}

	public void addLast2(int v1, int v2) {
		this.ensureCapacity(this.size + 2);
		this.array[this.size++] = v1;
		this.array[this.size++] = v2;
	}

	public int removeLast() {
		return this.array[--this.size];
	}

	public void removeLast2() {
		this.size -= 2;
	}

	private void ensureCapacity(int minSize) {
		if (minSize <= this.array.length) {
			return;
		}
		int[] newArray = new int[this.array.length * 2];
		System.arraycopy(this.array, 0, newArray, 0, this.size);
		this.array = newArray;
	}
}
