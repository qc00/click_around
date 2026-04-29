package software.nmr.click_around.settings;

import java.util.function.Supplier;

public class VersionedCache<T> {
    private T cached;
    private int cachedVersion = -1;
    private final Supplier<T> supplier;

    public VersionedCache(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T getForVersion(int sourceVersion) {
        if (sourceVersion != cachedVersion) {
            synchronized (this) {
                if (sourceVersion == cachedVersion) return cached;
            }
            var replacement = supplier.get();
            synchronized (this) {
                if (sourceVersion > cachedVersion) {
                    cached = replacement;
                    cachedVersion = sourceVersion;
                }
            }
        }
        return cached;
    }
}
