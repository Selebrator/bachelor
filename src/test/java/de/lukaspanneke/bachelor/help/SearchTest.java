package de.lukaspanneke.bachelor.help;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static de.lukaspanneke.bachelor.help.Search.binarySearch;
import static de.lukaspanneke.bachelor.help.Search.linearSearch;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.junit.jupiter.api.Assertions.*;

class SearchTest {

	private final Random random = ThreadLocalRandom.current();

	@Test
	void linear_equals_binary() {
		for (int i = 0; i < 100000000; i++) {
			int length = random.nextInt(10);
			int[] array = randomSortedNoRepeatArray(length, 0, 10);
			int value = random.nextInt(10);
			int x, y;
			if (length == 0) {
				x = 0;
				y = 0;
			} else {
				x = random.nextInt(length);
				y = random.nextInt(length);
			}
			int lo = min(x, y);
			int hi = max(x, y);
			assert_linear_equals_binary(array, value, lo, hi);
		}
	}

	private void assert_linear_equals_binary(int[] array, int value, int lo, int hi) {
		int bin = binarySearch(array, value, lo, hi);
		int lin = linearSearch(array, value, lo, hi);
		assertEquals(bin, lin, () ->
				"value = " + value + ", lo = " + lo + ", hi = " + hi + ", array = " + Arrays.toString(array) + "\n"
						+ "  bin = " + (bin >= 0 ? bin : "~" + ~bin) + "\n"
						+ "  lin = " + (lin >= 0 ? lin : "~" + ~lin)
		);
	}

	private int[] randomSortedNoRepeatArray(int length, int minInclusive, int maxExclusive) {
		return this.random.ints(minInclusive, maxExclusive).distinct().limit(length).sorted().toArray();
	}

}