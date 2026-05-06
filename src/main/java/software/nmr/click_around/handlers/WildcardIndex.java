package software.nmr.click_around.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handle multi-level indexing and wildcard matching.
 */
public class WildcardIndex<V> extends HashMap<String, V> {

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static class MultiLevelIndexer<In, Out> {
        private Supplier finalOutSupplier;
        private final BinaryOperator merger;
        private final ArrayList<Function<In, String>> indexers = new ArrayList<>();

        public MultiLevelIndexer(BinaryOperator<In> merger) {
            this.merger = merger;
        }

        public MultiLevelIndexer<In, WildcardIndex<Out>> addLevel(Function<In, String> indexer) {
            assert finalOutSupplier == null : "Cannot add more levels when finalOutSupplier is already set";
            indexers.add(indexer);
            return (MultiLevelIndexer<In, WildcardIndex<Out>>) this;
        }

        /** Sets the final type via supplier. */
        public <FinalOut extends Out> MultiLevelIndexer<In, FinalOut> withOutput(Supplier<FinalOut> finalOutSupplier) {
            this.finalOutSupplier = finalOutSupplier;
            return (MultiLevelIndexer<In, FinalOut>) this;
        }

        public Out index(Stream<In> source) {
            Collector<In, ?, WildcardIndex> collector = Collectors.toMap(indexers.getLast(), x -> x, merger,
                    WildcardIndex::new);
            for (int i = indexers.size() - 2; i >= 0; i--) {
                collector = Collectors.groupingBy(indexers.get(i), i == 0 ? finalOutSupplier : WildcardIndex::new,
                        collector);
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
