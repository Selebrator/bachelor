package de.lukaspanneke.bachelor.pn.sparse;

import de.lukaspanneke.bachelor.pn.sparse.mutable.SparseIntVector;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SparseIntVectorTest {

	@Test
	@RepeatedTest(10000)
	public void vectorPlusVector() {

		Map<Integer, Integer> aM = randomVector();
		SparseIntVector aV = new SparseIntVector(aM);
		Map<Integer, Integer> bM = randomVector();
		SparseIntVector bV = new SparseIntVector(bM);

		assertEqualsMapVector(aM, aV);
		assertEqualsMapVector(bM, bV);

		Map<Integer, Integer> sumM = Stream.concat(aM.entrySet().stream(), bM.entrySet().stream())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum));
		//aV.plusAssign(bV);
		assertEqualsMapVector(sumM, aV);
	}

	private static Map<Integer, Integer> randomVector() {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		return IntStream.generate(() -> random.nextInt(-100 ,100))
				.mapToObj(i -> Map.entry(i, random.nextInt(1, 5)))
				.limit(10)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum));
	}

	private static void assertEqualsMapVector(Map<Integer, Integer> map, SparseIntVector vector) {
		assertEquals(map.size(), vector.size());
		for (int i = 0; i < vector.size(); i++) {
			assertEquals(map.get(vector.keyAt(i)), vector.valueAt(i));
		}
	}

	@Test
	public void vectorDivideVector() {
		assertEquals(OptionalInt.of(3), new SparseIntVector(new int[] {0,3,0,9}).scalar(new SparseIntVector(new int[] {0,1,0,3})));
		assertEquals(OptionalInt.of(2), new SparseIntVector(new int[] {2}).scalar(new SparseIntVector(new int[] {1})));
		assertEquals(OptionalInt.of(1), new SparseIntVector(new int[] {4}).scalar(new SparseIntVector(new int[] {4})));
		assertEquals(OptionalInt.empty(), new SparseIntVector(new int[] {1}).scalar(new SparseIntVector(new int[] {2,3})));
		assertEquals(OptionalInt.empty(), new SparseIntVector(new int[] {1,3}).scalar(new SparseIntVector(new int[] {1,4})));
	}

}