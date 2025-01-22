# Monitor Spring Cloud Applications with Kpow

![streams-topology-usage](https://github.com/user-attachments/assets/c26d9feb-3dcb-45be-b457-5b1d30fa9f9d)


Integrated [Spring Cloud Stream Wordcount](https://github.com/spring-cloud/spring-cloud-stream-samples/tree/main/kafka-streams-samples/kafka-streams-word-count) Kafka Streams example application with the [Kpow Streams Agent](https://github.com/factorhouse/kpow-streams-agent).

Run this project with the original instructions below, we have integrated the Kpow Agent. You will see log-lines like:

```
Kpow: sent [112] streams metrics for application.id hello-word-count-sample
```

Once started, run Kpow with the target cluster and navigate to 'Streams' to view the live topology and metrics.

### Quickstart

* Follow the original project setup steps (instructions below)
* Put data on the wordcount topic (instructions below)
* Start Kpow (see: [Kpow Local](https://github.com/factorhouse/kpow-local) for local evaluation + trial licenses)
  * If using the single-node Kafka Cluster from this project, set `REPLICATION_FACTOR=1` when running Kpow
* Navigate to localhost:3000 > Streams
* View WordCount Topology + Metrics
* Navigate to Consumers to reset WordCount offsets 

## How We Integrated WordCount Streams with the Kpow Agent

### Get the Kpow Streams Dependency

Include the Kpow Streams Agent library in your application:

```xml
<dependency>
  <groupId>io.factorhouse</groupId>
  <artifactId>kpow-streams-agent</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Integrate the Agent

Start the Kpow Streams Agent (view full source)

```java
public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(KafkaStreamsWordCountApplication.class, args);

        // The StreamsBuilderFactoryBean name is '&stream-builder-' + your function name from config, .e.g
        //
        //   spring.cloud.stream:
        //      function:
        //        definition: process <-- '&stream-builder-' + this name here
        //
        // We use the SBFB to obtain the streams and topology of your built Spring Kafka Streams application
        
        StreamsBuilderFactoryBean streamsBuilderFactoryBean = context.getBean("&stream-builder-process", StreamsBuilderFactoryBean.class);
        KafkaStreams streams = streamsBuilderFactoryBean.getKafkaStreams();
        Topology topology = streamsBuilderFactoryBean.getTopology();

        // Create connection properties for the StreamsRegistry producer to send metrics to internal Kpow topics
        // You should be able to use streamsBuilderFactoryBean.getStreamsConfiguration() but in this particular case
        // Those properties contain 'bootstrap.servers = [[localhost:9092]]' which errors on startup
        
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "127.0.0.1:9092");

        // Create a Kpow StreamsRegistry
        
        StreamsRegistry registry = new StreamsRegistry(properties);

        // Specify the key strategy when writing metrics to the internal Kafka topic
        // props are java.util.Properties describing the Kafka Connection
        ClusterIdKeyStrategy keyStrat = new ClusterIdKeyStrategy(factory.getStreamsConfiguration());
      
        // Register your KafkaStreams and Topology instances with the StreamsRegistry
        registry.register(streams, topology, keyStrat);
    }
```
----

### Original Project Readme Follows

## What is this app?

This is an example of a Spring Cloud Stream processor using Kafka Streams support.

The example is based on the word count application from the [reference documentation](https://github.com/apache/kafka/blob/2.8/streams/examples/src/main/java/org/apache/kafka/streams/examples/wordcount/WordCountDemo.java).

It uses a single input and a single output. In essence, the application receives text messages from an input topic and computes word occurrence counts in a configurable time window and report that in an output topic.
The sample uses a default timewindow of 30 seconds.

### Running the app:

Go to the root of the repository.

`docker-compose up -d`

`./mvnw clean package`

`java -jar target/kafka-streams-word-count-0.0.1-SNAPSHOT.jar`

Assuming you are running the dockerized Kafka cluster as above.

Issue the following commands:

`docker exec -it kafka-wordcount /opt/kafka/bin/kafka-console-producer.sh --broker-list 127.0.0.1:9092 --topic words`

Or if you prefer `kafkacat`:

`kafkacat -b localhost:9092 -t words -P`

On another terminal:

`docker exec -it kafka-wordcount /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 --topic counts`

Or if you prefer `kafkacat`:

`kafkacat -b localhost:9092 -t counts`

Enter some text in the console producer and watch the output in the console consumer.

Once you are done, stop the Kafka cluster: `docker-compose down`
