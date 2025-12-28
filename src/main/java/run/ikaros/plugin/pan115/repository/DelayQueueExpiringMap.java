package run.ikaros.plugin.pan115.repository;

import java.util.concurrent.*;

public class DelayQueueExpiringMap<K, V> {
    private static class DelayedItem<K> implements Delayed {
        private final K key;
        private final long expireTime;

        DelayedItem(K key, long delay, TimeUnit unit) {
            this.key = key;
            this.expireTime = System.currentTimeMillis() + unit.toMillis(delay);
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long diff = expireTime - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.expireTime, ((DelayedItem<?>) o).expireTime);
        }
    }

    private final ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();
    private final DelayQueue<DelayedItem<K>> delayQueue = new DelayQueue<>();
    private final ExecutorService cleanupService = Executors.newSingleThreadExecutor();

    public DelayQueueExpiringMap() {
        // 启动清理线程
        cleanupService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    DelayedItem<K> item = delayQueue.take();
                    map.remove(item.key);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    public void put(K key, V value) {
        put(key, value, 30, TimeUnit.MINUTES);
    }

    public void put(K key, V value, long duration, TimeUnit unit) {
        map.put(key, value);
        delayQueue.put(new DelayedItem<>(key, duration, unit));
    }

    public V get(K key) {
        return map.get(key);
    }

    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public V remove(K key) {
        // 注意：DelayQueue中的项会自然过期，这里只是从map中移除
        return map.remove(key);
    }

    public void shutdown() {
        cleanupService.shutdown();
    }
}
