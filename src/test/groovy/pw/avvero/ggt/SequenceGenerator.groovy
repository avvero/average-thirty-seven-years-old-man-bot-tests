package pw.avvero.ggt

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function

/**
 * A utility class for generating unique sequence numbers for a given set of keys.
 * Each key will have its own sequence which increments independently of other keys.
 * <p>
 * For example, for a key "key1" the sequence starts at 1001, while for a key "key2"
 * the sequence starts at 10001. Distribution rule is provided by SEQUENCES_DISTRIBUTOR
 * </p>
 * Thread safety is ensured through the use of {@code AtomicInteger} and {@code ConcurrentHashMap}.
 */
class SequenceGenerator {

    private static AtomicInteger SEQUENCES_DISTRIBUTOR = new AtomicInteger(2)
    private static Map<String, AtomicInteger> SEQUENCES = new ConcurrentHashMap<>()

    /**
     * Retrieves the next sequence number for a list of keys.
     *
     * @param keys The list of keys for which the next sequence numbers are required.
     * @return A map where each key is mapped to its next sequence number.
     */
    static Map<String, Integer> getNext(List<String> keys) {
        def result = [:] as Map<String, Integer>
        keys.forEach {key ->
            result[key] = getNext(key)
        }
        return result
    }

    /**
     * Retrieves the next sequence number for a single key.
     * <p>
     * If the key doesn't exist in the sequences map, it initializes the sequence
     * starting at a power of 10 based on the current value of {@code SEQUENCES_DISTRIBUTOR}.
     * </p>
     *
     * @param key The key for which the next sequence number is required.
     * @return The next sequence number for the provided key.
     */
    static Integer getNext(String key) {
        return SEQUENCES.computeIfAbsent(key, new Function<String, AtomicInteger>() {
            @Override
            AtomicInteger apply(String s) {
                return new AtomicInteger((int) Math.pow(10, SEQUENCES_DISTRIBUTOR.incrementAndGet()))
            }
        }).incrementAndGet()
    }
}
