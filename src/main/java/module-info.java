module com.github.simplenet {
    requires org.slf4j;
    requires com.github.pbbl;

    exports com.github.simplenet;
    exports com.github.simplenet.packet;
    exports com.github.simplenet.utility.exposed.consumer;
    exports com.github.simplenet.utility.exposed.cryptography;
    exports com.github.simplenet.utility.exposed.data;
    exports com.github.simplenet.utility.exposed.predicate;
}
