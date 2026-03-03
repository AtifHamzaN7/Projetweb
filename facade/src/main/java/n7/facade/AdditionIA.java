package n7.facade;

public class AdditionIA {

    private final SoustractionIA soustractionIA;

    public AdditionIA() {
        this.soustractionIA = new SoustractionIA();
    }

    public int add(int a, int b) {
        return a + b;
    }

    // Nouvelle méthode dépendante de SoustractionIA
    public int addThenSubtract(int a, int b, int c) {
        int sum = add(a, b);
        return soustractionIA.subtract(sum, c);
    }

    public int addThree(int a, int b, int c) {
        return a + b + c;
    }

    public int sum(int[] values) {
        if (values == null) {
            throw new IllegalArgumentException("values must not be null");
        }
        int result = 0;
        for (int v : values) {
            result += v;
        }
        return result;
    }

    public int safeAdd(int a, int b) {
        return Math.addExact(a, b);
    }

    public int rangeSum(int start, int end) {
        if (start > end) {
            throw new IllegalArgumentException("start must be <= end");
        }
        int result = 0;
        for (int i = start; i <= end; i++) {
            result += i;
        }
        return result;
    }

    public int addAll(int... values) {
        if (values == null) {
            throw new IllegalArgumentException("values must not be null");
        }
        int result = 0;
        for (int v : values) {
            result += v;
        }
        return result;
    }

    public boolean isSumEven(int a, int b) {
        long sum = (long) a + (long) b;
        return (sum & 1L) == 0L;
    }

    public int clampAdd(int a, int b, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min must be <= max");
        }
        long sum = (long) a + (long) b;
        if (sum < min) {
            return min;
        }
        if (sum > max) {
            return max;
        }
        return (int) sum;
    }
} //trigger CI for addition testsss