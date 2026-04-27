package software.nmr.click_around.settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handle multi-level indexing and wildcard matching.
 */
public class WildcardIndex<V> extends HashMap<String, V> {

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static class MultiLevelIndexer<In, Out> {
        private final ArrayList<Function<In, String>> indexers  = new ArrayList<>();
        private final Function<In, Out> finalValue;

        public MultiLevelIndexer(Function<In, Out> finalValue){
            this.finalValue = finalValue;
        }

        public MultiLevelIndexer<In, WildcardIndex<Out>> addLevel(Function<In, String> indexer) {
            indexers.add(indexer);
            return (MultiLevelIndexer<In, WildcardIndex<Out>>) this;
        }

        public Out index(Stream<In> source) {
            Collector<In, ?, WildcardIndex> collector = Collectors.toMap(indexers.getLast(), finalValue, (a, b) -> a, WildcardIndex::new);
            for (int i = indexers.size() - 2; i >= 0; i--) {
                collector = Collectors.groupingBy(indexers.get(i), WildcardIndex::new, collector);
            }
            return (Out) source.collect(collector);
        }
    }

    public void lookup(String wildcard, Consumer<V> consumer) {
        var v = get(wildcard);
        if (v != null) {
            consumer.accept(v);
        }

        if (!wildcard.isEmpty()) {
            v = get("*");
            if (v != null) {
                consumer.accept(v);
            }
        }
    }
}
