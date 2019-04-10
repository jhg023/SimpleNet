/*
 * MIT License
 *
 * Copyright (c) 2019 Jacob Glickman
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
        return multiple == 0 ? num : num + multiple - (num % multiple);
    }

}
