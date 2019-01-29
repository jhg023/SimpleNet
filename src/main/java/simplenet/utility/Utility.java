package simplenet.utility;

/**
 * A class that holds miscellaneous utility methods.
 *
 * @author Jacob G.
 * @version January 27, 2019
 */
public final class Utility {
    
    /**
     * A {@code private} constructor that throws an {@link UnsupportedOperationException} when invoked.
     */
    private Utility() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated!");
    }
    
    /**
     * A method that rounds the specified value up to the next multiple of the specified multiple.
     *
     * @param num      The number to round.
     * @param multiple The multiple to round the number to.
     * @return An {@code int}, greater than or equal to {@code num}, and a multiple of {@code multiple}.
     */
    public static int roundUpToNextMultiple(int num, int multiple) {
        if (multiple == 0) {
            return num;
        }
        
        int remainder = num % multiple;
        return remainder == 0 ? num + multiple : num + multiple - remainder;
    }
    
}
