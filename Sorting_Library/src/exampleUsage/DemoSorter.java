package exampleUsage;

import java.util.*;
import java.util.concurrent.*;
import logic.Sorter;

// Beispiel-Objekt , wird nach der Count Variabel sortiert.
class Item {
    final String name;
    final int count;
    Item(String name, int count) { this.name = name; this.count = count; }
    @Override public String toString() { return name + "(" + count + ")"; }
}

public class DemoSorter {

    public static void main(String[] args) {
        demoIntegers();
        demoStrings();
        demoCustomObjects();
        demoLargeIntegers(); //  letzter Test mit Zeitmessung
    }

    static void demoIntegers() {
        List<Integer> nums1 = new ArrayList<>(List.of(5, 1, 9, 2, 9, 3));
        List<Integer> nums2 = new ArrayList<>(nums1);

        Sorter.sortSeq(nums1);
        Sorter.sortPar(nums2);

        System.out.println("Integer seq : " + nums1);
        System.out.println("Integer par : " + nums2);
        System.out.println();
    }

    static void demoStrings() {
        List<String> words1 = new ArrayList<>(List.of("z", "aa", "b", "aa", "m"));
        List<String> words2 = new ArrayList<>(words1);

        Sorter.sortSeq(words1);
        Sorter.sortPar(words2);

        System.out.println("String  seq : " + words1);
        System.out.println("String  par : " + words2);
        System.out.println();
    }

    static void demoCustomObjects() {
        List<Item> items1 = new ArrayList<>(List.of(
            new Item("A", 2),
            new Item("B", 1),
            new Item("C", 2),
            new Item("D", 1),
            new Item("E", 3)
        ));
        List<Item> items2 = new ArrayList<>(items1);

        Comparator<Item> byCount = Comparator.comparingInt(it -> it.count);

        Sorter.sortSeq(items1, byCount);
        Sorter.sortPar(items2, byCount);

        System.out.println("Objekte seq : " + items1);
        System.out.println("Objekte par : " + items2);
        System.out.println();
    }

    // ========= Time meassurement damit para_sort gewürdigt wird :) =========
    static void demoLargeIntegers() {
        final int N = 2_000_000_0; //Make this smaller if your pcu is a bit slow (seq takes 15s for me)
        System.out.println("=== Großer Test mit N = " + N + " ===");

        List<Integer> aSeq = randomIntList(N, 42);
        List<Integer> aPar = new ArrayList<>(aSeq); // identische Daten

        // Sequentiell
        long t0 = System.nanoTime();
        Sorter.sortSeq(aSeq);
        long t1 = System.nanoTime();

        // Parallel
        long t2 = System.nanoTime();
        Sorter.sortPar(aPar);
        long t3 = System.nanoTime();

        long seqMs = (t1 - t0) / 1_000_000;
        long parMs = (t3 - t2) / 1_000_000;

        System.out.println("Zeit seq : " + seqMs + " ms");
        System.out.println("Zeit par : " + parMs + " ms");
        System.out.printf("Speedup   : %.2fx%n", parMs == 0 ? 0.0 : (seqMs * 1.0 / parMs));
        System.out.println("Sort_Par was executed on " + Runtime.getRuntime().availableProcessors() + " logical cores.");

        // Korrektheit grob prüfen (erste/letzte 10 Elemente)
        System.out.println("Seq first/last: " + headTail(aSeq, 10));
        System.out.println("Par first/last: " + headTail(aPar, 10));
        System.out.println("equally:  " + aSeq.equals(aPar));
        System.out.println();
    }

  // For random Testing
    static List<Integer> randomIntList(int n, long seed) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        ArrayList<Integer> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(rnd.nextInt()); 
        }
        return list;
    }

    static String headTail(List<Integer> list, int k) {
        int n = list.size();
        List<Integer> head = list.subList(0, Math.min(k, n));
        List<Integer> tail = list.subList(Math.max(0, n - k), n);
        return head + " ... " + tail;
    }
}
