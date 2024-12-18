import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class ParallelArrayProcessing {
    private static final int ARRAY_SIZE = 10_000_000;
    private static final int THRESHOLD = 100_000;
    private static final int[] array = new int[ARRAY_SIZE];

    // RecursiveTask for parallel processing
    static class SquareSumTask extends RecursiveTask<Long> {
        private final int start;
        private final int end;
        private static final int SEQUENTIAL_THRESHOLD = 1000;

        public SquareSumTask(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            // Base case: if the segment is small enough, process sequentially
            if (end - start <= SEQUENTIAL_THRESHOLD) {
                long sum = 0;
                for (int i = start; i < end; i++) {
                    if (array[i] > THRESHOLD) {
                        sum += (long) Math.pow(array[i], 2);
                    }
                }
                return sum;
            }

            // Divide the task
            int mid = start + (end - start) / 2;
            SquareSumTask leftTask = new SquareSumTask(start, mid);
            SquareSumTask rightTask = new SquareSumTask(mid, end);

            // Fork both tasks
            leftTask.fork();
            rightTask.fork();

            // Join and combine results
            return leftTask.join() + rightTask.join();
        }
    }

    // Sequential processing method for comparison
    private static long sequentialProcess() {
        long sum = 0;
        for (int value : array) {
            if (value > THRESHOLD) {
                sum += (long) Math.pow(value, 2);
            }
        }
        return sum;
    }

    // Parallel processing method with configurable thread pool
    private static long parallelProcess(int numThreads) {
        ForkJoinPool pool = new ForkJoinPool(numThreads);
        try {
            return pool.invoke(new SquareSumTask(0, array.length));
        } finally {
            pool.shutdown();
        }
    }

    // Initialize array with some test data
    private static void initializeArray() {
        for (int i = 0; i < array.length; i++) {
            array[i] = i % 200_000; // This ensures some values are above THRESHOLD
        }
    }

    public static void main(String[] args) {
        // Initialize the array
        initializeArray();

        // Warm-up run
        sequentialProcess();

        // Benchmark sequential processing
        long startTime = System.nanoTime();
        long sequentialResult = sequentialProcess();
        long sequentialDuration = System.nanoTime() - startTime;
        System.out.printf("Sequential Processing:\n");
        System.out.printf("Result: %d\n", sequentialResult);
        System.out.printf("Time: %.3f ms\n\n", sequentialDuration / 1_000_000.0);

        // Test different numbers of threads
        int[] threadCounts = {1, 2, 4, 8, 16, 32, 64};
        for (int threads : threadCounts) {
            startTime = System.nanoTime();
            long parallelResult = parallelProcess(threads);
            long parallelDuration = System.nanoTime() - startTime;

            System.out.printf("Parallel Processing (%d threads):\n", threads);
            System.out.printf("Result: %d\n", parallelResult);
            System.out.printf("Time: %.3f ms\n", parallelDuration / 1_000_000.0);
            System.out.printf("Speedup: %.2fx\n\n", 
                (double) sequentialDuration / parallelDuration);
        }
    }
}