package com.github.rohansuri.art;

import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections4.BulkTest;
import org.apache.commons.collections4.set.AbstractSortedSetTest;

public abstract class AbstractNavigableSetTest<K> extends org.apache.commons.collections4.set.AbstractNavigableSetTest<K> {
	public AbstractNavigableSetTest(String name) {
		super(name);
	}

	public BulkTest bulkTestSortedSetSubSet() {
		int length = this.getFullElements().length;
		int lobound = length / 3;
		int hibound = lobound * 2;
		return new TestSortedSetSubSet(lobound, hibound);
	}

	public BulkTest bulkTestSortedSetHeadSet() {
		int length = this.getFullElements().length;
		int lobound = length / 3;
		int hibound = lobound * 2;
		return new TestSortedSetSubSet(hibound, true);
	}

	public BulkTest bulkTestSortedSetTailSet() {
		int length = this.getFullElements().length;
		int lobound = length / 3;
		return new TestSortedSetSubSet(lobound, false);
	}

	@Override
	public BulkTest bulkTestNavigableSetSubSet() {
		int length = this.getFullElements().length;
		int lobound = length / 3;
		int hibound = lobound * 2;
		return new TestNavigableSetSubSet(lobound, hibound, false);
	}

	@Override
	public BulkTest bulkTestNavigableSetHeadSet() {
		int length = this.getFullElements().length;
		int lobound = length / 3;
		int hibound = lobound * 2;
		return new TestNavigableSetSubSet(hibound, true, true);
	}

	@Override
	public BulkTest bulkTestNavigableSetTailSet() {
		int length = this.getFullElements().length;
		int lobound = length / 3;
		return new TestNavigableSetSubSet(lobound, false, false);
	}

	// request upstream to forward call to makeConfirmedCollection
	public class TestNavigableSetSubSet extends org.apache.commons.collections4.set.AbstractNavigableSetTest.TestNavigableSetSubSet {

		public TestNavigableSetSubSet(int bound, boolean head, boolean inclusive) {
			super(bound, head, inclusive);
		}

		public TestNavigableSetSubSet(int lobound, int hibound, boolean inclusive) {
			super(lobound, hibound, inclusive);
		}

		// need this for navigableKeySet, descendingKeySet
		@Override
		public NavigableSet makeConfirmedCollection() {
			return AbstractNavigableSetTest.this.makeConfirmedCollection();
		}
	}

	public class TestSortedSetSubSet extends org.apache.commons.collections4.set.AbstractSortedSetTest.TestSortedSetSubSet {

		public TestSortedSetSubSet(int bound, boolean head) {
			super(bound, head);
		}

		public TestSortedSetSubSet(int lobound, int hibound) {
			super(lobound, hibound);
		}

		@Override
		public SortedSet makeConfirmedCollection() {
			return AbstractNavigableSetTest.this.makeConfirmedCollection();
		}

	}
}
