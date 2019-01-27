package simplenet.utility                                             ;
import java.util.Objects                                              ;
/**                                                                   
 * A class that acts as an {@code int}-{@link T} tuple.               
 *                                                                    
 * @author Jacob G.                                                   
 * @version January 12, 2019                                          
 */                                                                   
public final class IntPair<T>                                         {
    /**                                                               
     * The key of this {@link IntPair}.                               
     */                                                               
    public int key                                                    ;
    /**                                                               
     * The value of this {@link IntPair}.                             
     */                                                               
    public T value                                                    ;
    /**                                                               
     * Creates a new {@link IntPair} with the specified key and value.
     *                                                                
     * @param key   the key.                                          
     * @param value the value.                                        
     */                                                               
    public IntPair(int key, T value)                                  {
        this.key = key                                                ;
        this.value = value                                            ;}
    @Override                                                         
    public boolean equals(Object o)                                   {
        if (!(o instanceof IntPair<?>))                               {
            return false                                              ;}
        var pair = (IntPair<?>) o                                     ;
        return key == pair.key && Objects.equals(value, pair.value)   ;}
    @Override                                                         
    public int hashCode()                                             {
        return Objects.hash(key, value)                               ;}
    @Override                                                         
    public String toString()                                          {
        return "IntPair[key: " + key + ", value: " + value + "]"      ;}}
