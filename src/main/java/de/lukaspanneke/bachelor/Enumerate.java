package de.lukaspanneke.bachelor;

import java.util.*;

public class Enumerate {

	private static class ListEnumerator<T> implements Iterable<IndexedItem<T>> {
		private Iterable<T> target;
		private int start;

		public ListEnumerator(Iterable<T> target, int start) {
			this.target = target;
			this.start = start;
		}

		@Override
		public Iterator<IndexedItem<T>> iterator() {
			final Iterator<T> targetIterator = target.iterator();
			return new Iterator<>() {
				int index = start;

				@Override
				public boolean hasNext() {
					return targetIterator.hasNext();
				}

				@Override
				public IndexedItem<T> next() {
					IndexedItem<T> nextIndexedItem = new IndexedItem<T>(targetIterator.next(), index);
					index++;
					return nextIndexedItem;
				}
			};
		}
	}

	public static <T> Iterable<IndexedItem<T>> enumerate(Iterable<T> iterable, int start) {
		return new ListEnumerator<T>(iterable, start);
	}

	public static <T> Iterable<IndexedItem<T>> enumerate(Iterable<T> iterable) {
		return enumerate(iterable, 0);
	}
}
