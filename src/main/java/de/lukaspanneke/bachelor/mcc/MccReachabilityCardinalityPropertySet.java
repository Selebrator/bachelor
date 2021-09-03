package de.lukaspanneke.bachelor.mcc;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public final class MccReachabilityCardinalityPropertySet<N, M, P, T> extends AbstractList<MccReachabilityCardinalityProperty<N, M, P, T>> {

	private final LoadingCache<Integer, MccReachabilityCardinalityProperty<N, M, P, T>> elements;
	private final Map<Integer, Exception> brokenIndices = new HashMap<>();
	private final int size;

	public MccReachabilityCardinalityPropertySet(Function<Integer, MccReachabilityCardinalityProperty<N, M, P, T>> elements, int size) {
		this.elements = CacheBuilder.newBuilder().build(CacheLoader.from(elements::apply));
		this.size = size;
	}

	@Override
	public MccReachabilityCardinalityProperty<N, M, P, T> get(int index) {
		Exception ex = this.brokenIndices.get(index);
		if (ex != null) {
			throw new NoSuchElementException(ex);
		}
		try {
			return this.elements.get(index);
		} catch (ExecutionException | UncheckedExecutionException e) {
			brokenIndices.put(index, e);
			throw new NoSuchElementException(e);
		}
	}

	@Override
	public int size() {
		return this.size;
	}
}
