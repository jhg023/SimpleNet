/**
 * The package {@code ni2.ssl} contains the classes that implement the functionality of class SSLAsynchronousSocketChannel.
 * This is an asynchronous socket channel that adds a layer for SSL/TLS processing to the jdk class
 * java.nio.channels.AsynchronousSocketChannel. The API of the package's public classes is decribed below in Javadoc.
 * The package contains futhermore the following package-local helper classes:
 * <table class="striped" style="text-align:left; margin-left:2em">
 *   <caption style="display:none">Lists channels and their descriptions</caption>
 *   <thead>
 *     <tr>
 *       <th scope="col">Name</th>
  *       <th scope="col">Type</th>
 *       <th scope="col">Description</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <th scope="row">{@code Action}</th>
 *       <td>{@code enum}</td>
 *       <td>Enumerates the actions that can be processed during a SSL/TLS communication</td>
 *     </tr><tr>
 *       <th scope="row">{@code BufferState}</th>
 *       <td>{@code class}</td>
 *       <td>Stores information on the state of a {@code java.nio.Buffer}
 *     </tr><tr>
  *       <th scope="row">{@code Context}</th>
 *       <td>{@code class}</td>
 *       <td>Stores context information that is used as attachement for the internal read and write handlers
 *     </tr><tr>
 *       <th scope="row">{@code Handler}</th>
 *       <td>{@code enum}</td>
 *       <td>Enumerates possible types of completion handlers</td>
 *     </tr><tr>
 *       <th scope="row">{@code Response}</th>
 *       <td>{@code enum}</td>
 *       <td>Enumerates the possible responses from {@code SSLEngineAutomat}</td>
 *     </tr><tr>
 *       <th scope="row">{@code SSLAsynchronousChannelGroup}</th>
 *       <td>{@code public class}</td>
 *       <td>See below</td>
 *     </tr><tr>
 *       <th scope="row">{@code SSLAsynchronousSocketChannel}</th>
 *       <td>{@code public class}</td>
 *       <td>See below</td>
 *     </tr><tr>
 *       <th scope="row">{@code SSLAsynchronousSocketChannelLayer}</th>
 *       <td>{@code public class}</td>
 *       <td>Layer for SSL/TLS processing</td>
 *     </tr><tr>
 *       <th scope="row">{@code SSLAsynchronousSocketChannelImpl}</th>
 *       <td>{@code public class}</td>
 *       <td>Implements public abstract methods of class SSLAsynchronousSocketChannel</td>
 *     </tr><tr>
 *       <th scope="row">{@code SSLEngineAutomat}</th>
 *       <td>{@code class}</td>
 *       <td>Evaluates stati of {@code SSLEngineResult.Status} and {@code SSLEngineResult.HandshakeStatus} and decides which action is be
 *           performed according to the status</td>
 *     </tr>
 *   </tbody>
 * </table>
 *
 */
package nio2.ssl;