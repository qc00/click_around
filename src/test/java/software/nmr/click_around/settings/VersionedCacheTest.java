package software.nmr.click_around.settings;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class VersionedCacheTest {
    AtomicInteger calls = new AtomicInteger();
    VersionedCache<String> cache = new VersionedCache<>(() -> "v" + calls.incrementAndGet());

    @Test
    void returnsCachedValueForSameVersion() {
        var first = cache.getForVersion(1);
        var second = cache.getForVersion(1);
        assertSame(first, second);
        assertEquals(1, calls.get());
    }

    @Test
    void recomputesWhenVersionAdvances() {
        assertEquals("v1", cache.getForVersion(1));
        assertEquals("v2", cache.getForVersion(2));
        assertEquals(2, calls.get());
    }

    @Test
    void staleVersionDoesNotOverwriteNewerCache() {
        cache.getForVersion(5);
        assertEquals("v1", cache.getForVersion(5));

        assertEquals("v1", cache.getForVersion(5));
    }
}
