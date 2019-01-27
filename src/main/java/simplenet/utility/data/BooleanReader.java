package simplenet.utility.data                                                                                        ;
import java.nio.ByteBuffer                                                                                            ;
import java.util.concurrent.CompletableFuture                                                                         ;
import java.util.function.Consumer                                                                                    ;
import simplenet.utility.exposed.BooleanConsumer                                                                      ;
/**                                                                                                                   
 * An interface that defines the methods required to read {@code boolean}s over a network with SimpleNet.             
 * <br><br>                                                                                                           
 * The {@code boolean}s are sent over the network as {@code byte}s with a value of {@code 1} for {@code true} and a   
 * value of {@code 0} for {@code false}.                                                                              
 *                                                                                                                    
 * @author Jacob G.                                                                                                   
 * @version January 21, 2019                                                                                          
 */                                                                                                                   
public interface BooleanReader extends DataReader                                                                     {
    /**                                                                                                               
     * Reads a {@code boolean} from the network, but blocks the executing thread unlike                               
     * {@link #readBoolean(BooleanConsumer)}.                                                                         
     *                                                                                                                
     * @return A {@code boolean}.                                                                                     
     */                                                                                                               
    default boolean readBoolean()                                                                                     {
        var future = new CompletableFuture<Boolean>()                                                                 ;
        readBoolean(future::complete)                                                                                 ;
        return read(future)                                                                                           ;}
    /**                                                                                                               
     * Requests a single {@code boolean}, and accepts a {@link BooleanConsumer} with the {@code boolean} when it is   
     * received.                                                                                                      
     *                                                                                                                
     * @param consumer Holds the operations that should be performed once the {@code boolean} is received.            
     */                                                                                                               
    default void readBoolean(BooleanConsumer consumer)                                                                {
        read(Byte.BYTES, buffer -> consumer.accept(buffer.get() == 1))                                                ;}
    /**                                                                                                               
     * Calls {@link #readBoolean(BooleanConsumer)}; however, once finished, {@link #readBoolean(BooleanConsumer)} is  
     * called once again with the same consumer; this method loops indefinitely, whereas                              
     * {@link #readBoolean(BooleanConsumer)} completes after a single iteration.                                      
     *                                                                                                                
     * @param consumer Holds the operations that should be performed once the {@code boolean} is received.            
     */                                                                                                               
    default void readBooleanAlways(BooleanConsumer consumer)                                                          {
        readAlways(Byte.BYTES, buffer -> consumer.accept(buffer.get() == 1))                                          ;}
    /**                                                                                                               
     * Requests a {@code boolean[]} of length {@code n} and accepts a {@link Consumer} when all of the                
     * {@code boolean}s are received.                                                                                 
     *                                                                                                                
     * @param n        The amount of {@code boolean}s requested.                                                      
     * @param consumer Holds the operations that should be performed once the {@code n} {@code boolean}s are received.
     */                                                                                                               
    default void readBooleans(int n, Consumer<boolean[]> consumer)                                                    {
        read(n, buffer -> processBooleans(buffer, n, consumer))                                                       ;}
    /**                                                                                                               
     * Calls {@link #readBooleans(int, Consumer)}; however, once finished, {@link #readBooleans(int, Consumer)} is    
     * called once again with the same parameter; this loops indefinitely, whereas                                    
     * {@link #readBooleans(int, Consumer)} completes after a single iteration.                                       
     *                                                                                                                
     * @param n        The amount of {@code boolean}s requested.                                                      
     * @param consumer Holds the operations that should be performed once the {@code n} {@code boolean}s are received.
     */                                                                                                               
    default void readBooleansAlways(int n, Consumer<boolean[]> consumer)                                              {
        readAlways(n, buffer -> processBooleans(buffer, n, consumer))                                                 ;}
    /**                                                                                                               
     * A helper method to eliminate duplicate code.                                                                   
     *                                                                                                                
     * @param buffer   The {@link ByteBuffer} that contains the {@code byte}s needed to map to {@code boolean}s.      
     * @param n        The amount of {@code boolean}s requested.                                                      
     * @param consumer Holds the operations that should be performed once the {@code n} {@code boolean}s are received.
     */                                                                                                               
    private void processBooleans(ByteBuffer buffer, int n, Consumer<boolean[]> consumer)                              {
        var b = new boolean[n]                                                                                        ;
        for (int i = 0; i < n; i++)                                                                                   {
            b[i] = buffer.get() == 1                                                                                  ;}
        consumer.accept(b)                                                                                            ;}}
