# Monitor Spring Kafka Streams Applications with kPow

Adapted [Spring Cloud Stream Wordcount](https://github.com/spring-cloud/spring-cloud-stream-samples/tree/main/kafka-streams-samples/kafka-streams-word-count) Kafka Streams application with the [kPow Streams Agent](https://github.com/operatr-io/kpow-streams-agent).

## Integration Steps

Include the kPow Streams Agent library in your application:

```xml
<dependency>
  <groupId>io.operatr</groupId>
  <artifactId>kpow-streams-agent</artifactId>
  <version>0.2.7</version>
</dependency>
```

Start the kPow Streams Agent (view full source)

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

        // Create connection properties for the StreamsRegistry producer to send metrics to internal kPow topics
        // You should be able to use streamsBuilderFactoryBean.getStreamsConfiguration() but in this particular case
        // Those properties contain 'bootstrap.servers = [[localhost:9092]]' which errors on startup
        
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "127.0.0.1:9092");

        // Create a kPow StreamsRegistry
        
        StreamsRegistry registry = new StreamsRegistry(properties);

        // Register your KafkaStreams and Topology instances with the StreamsRegistry
        
        registry.register(streams, topology);
    }
```

Follow the original project steps (below), then start kPow (see: kPow Local for evaluation) and navigate to localhost:3000

![kpow-kafka-streams-wordcount-viz](https://user-images.githubusercontent.com/2832467/131286862-36589a97-667a-4d56-bcb2-bc9fcfd3abe7.png)

*-- Original Project Readme Follows --*

## What is this app?

This is an example of a Spring Cloud Stream processor using Kafka Streams support.

The example is based on the word count application from the https://github.com/confluentinc/examples/blob/3.2.x/kafka-streams/src/main/java/io/confluent/examples/streams/WordCountLambdaExample.java[reference documentation].
It uses a single input and a single output.
In essence, the application receives text messages from an input topic and computes word occurrence counts in a configurable time window and report that in an output topic.
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