package sg.com.temasys.skylink.sdk.messagecache.demo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Util {
    private static ExecutorService THREAD_POOL;

    /**
     * Gets the thread pool that contains a single worker thread.
     * @return
     */
    public static ExecutorService getThreadPool() {
        if (THREAD_POOL == null || THREAD_POOL.isShutdown()) THREAD_POOL = Executors.newFixedThreadPool(1);
        return THREAD_POOL;
    }

    /**
     * Stops the single-threaded thread pool.
     */
    public static void shutdownThreadPool() {
        if (!THREAD_POOL.isShutdown()) {
            THREAD_POOL.shutdown();
            THREAD_POOL = null;
        }
    }
}
