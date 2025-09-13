package logic;

import java.util.*;
import java.util.concurrent.*;

public final class Sorter {

    private Sorter() {}

    public static <T extends Comparable<? super T>> void sortSeq(List<T> list) {
        sortSeq(list, naturalOrder());
    }

    public static <T> void sortSeq(List<T> list, Comparator<? super T> cmp) {
        if (list.size() < 2) return;
        ArrayList<T> a = new ArrayList<>(list);
        ArrayList<T> aux = new ArrayList<>(a);
        mergeSortSeq(a, aux, 0, a.size() - 1, cmp);
        for (int i = 0; i < list.size(); i++) list.set(i, a.get(i));
    }

    public static <T extends Comparable<? super T>> void sortPar(List<T> list) {
        sortPar(list, naturalOrder());
    }

    public static <T> void sortPar(List<T> list, Comparator<? super T> cmp) {
        sortPar(list, cmp, ForkJoinPool.commonPool());
    }

    public static <T> void sortPar(List<T> list, Comparator<? super T> cmp, ForkJoinPool pool) {
        if (list.size() < 2) return;
        ArrayList<T> a = new ArrayList<>(list);
        ArrayList<T> aux = new ArrayList<>(a);
        pool.invoke(new MergeTask<>(a, aux, 0, a.size() - 1, cmp, 50_000, 24));
        for (int i = 0; i < list.size(); i++) list.set(i, a.get(i));
    }

    private static <T> void mergeSortSeq(List<T> a, List<T> aux, int lo, int hi, Comparator<? super T> c) {
        if (hi - lo + 1 <= 24) {
            insertion(a, lo, hi, c);
            return;
        }
        int mid = (lo + hi) >>> 1;
        mergeSortSeq(a, aux, lo, mid, c);
        mergeSortSeq(a, aux, mid + 1, hi, c);
        if (c.compare(a.get(mid), a.get(mid + 1)) <= 0) return;
        merge(a, aux, lo, mid, hi, c);
    }

    private static final class MergeTask<T> extends RecursiveAction {
        private final List<T> a, aux;
        private final int lo, hi;
        private final Comparator<? super T> c;
        private final int parallelCutoff, insertionCutoff;

        MergeTask(List<T> a, List<T> aux, int lo, int hi,
                  Comparator<? super T> c, int parallelCutoff, int insertionCutoff) {
            this.a = a; this.aux = aux; this.lo = lo; this.hi = hi;
            this.c = c; this.parallelCutoff = parallelCutoff; this.insertionCutoff = insertionCutoff;
        }

        @Override protected void compute() {
            int n = hi - lo + 1;
            if (n <= insertionCutoff) {
                insertion(a, lo, hi, c);
                return;
            }
            int mid = (lo + hi) >>> 1;
            if (n >= parallelCutoff) {
                invokeAll(
                    new MergeTask<>(a, aux, lo, mid, c, parallelCutoff, insertionCutoff),
                    new MergeTask<>(a, aux, mid + 1, hi, c, parallelCutoff, insertionCutoff)
                );
            } else {
                mergeSortSeq(a, aux, lo, mid, c);
                mergeSortSeq(a, aux, mid + 1, hi, c);
            }
            if (c.compare(a.get(mid), a.get(mid + 1)) <= 0) return;
            merge(a, aux, lo, mid, hi, c);
        }
    }

    private static <T> void merge(List<T> a, List<T> aux, int lo, int mid, int hi, Comparator<? super T> c) {
        for (int k = lo; k <= hi; k++) aux.set(k, a.get(k));
        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) a.set(k, aux.get(j++));
            else if (j > hi) a.set(k, aux.get(i++));
            else if (c.compare(aux.get(j), aux.get(i)) < 0) a.set(k, aux.get(j++));
            else a.set(k, aux.get(i++));
        }
    }

    private static <T> void insertion(List<T> a, int lo, int hi, Comparator<? super T> c) {
        for (int i = lo + 1; i <= hi; i++) {
            T key = a.get(i);
            int j = i - 1;
            while (j >= lo && c.compare(a.get(j), key) > 0) {
                a.set(j + 1, a.get(j));
                j--;
            }
            a.set(j + 1, key);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Comparator<? super T> naturalOrder() {
        return (o1, o2) -> ((Comparable<? super T>) o1).compareTo(o2);
    }
}
