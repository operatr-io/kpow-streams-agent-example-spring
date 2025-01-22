/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kafka.streams.word.count;

import io.factorhouse.kpow.StreamsRegistry;
import io.factorhouse.kpow.key.ClusterIdKeyStrategy;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.function.Function;

@SpringBootApplication
public class KafkaStreamsWordCountApplication {

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(KafkaStreamsWordCountApplication.class, args);
        
        // The StreamsBuilderFactoryBean name is '&stream-builder-' + your function name from config, .e.g
        //
        // spring.cloud.stream:
        //   function:
        //     definition: process <-- '&stream-builder-' + this name here
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

        // Specify the key strategy when writing metrics to the internal Kafka topic
        // props are java.util.Properties describing the Kafka Connection
        ClusterIdKeyStrategy keyStrat = new ClusterIdKeyStrategy(properties);

        // Register your KafkaStreams and Topology instances with the StreamsRegistry
        registry.register(streams, topology, keyStrat);
    }

    public static class WordCountProcessorApplication {

        public static final String INPUT_TOPIC = "input";
        public static final String OUTPUT_TOPIC = "output";
        public static final int WINDOW_SIZE_MS = 30_000;

        @Bean
        public Function<KStream<Bytes, String>, KStream<Bytes, WordCount>> process() {

            return input -> input
                    .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
                    .map((key, value) -> new KeyValue<>(value, value))
                    .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
                    .windowedBy(TimeWindows.of(Duration.ofMillis(WINDOW_SIZE_MS)))
                    .count(Materialized.as("WordCounts-1"))
                    .toStream()
                    .map((key, value) -> new KeyValue<>(null, new WordCount(key.key(), value, new Date(key.window().start()), new Date(key.window().end()))));
        }
    }

    static class WordCount {

        private String word;

        private long count;

        private Date start;

        private Date end;

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("WordCount{");
            sb.append("word='").append(word).append('\'');
            sb.append(", count=").append(count);
            sb.append(", start=").append(start);
            sb.append(", end=").append(end);
            sb.append('}');
            return sb.toString();
        }

        WordCount() {

        }

        WordCount(String word, long count, Date start, Date end) {
            this.word = word;
            this.count = count;
            this.start = start;
            this.end = end;
        }

        public String getWord() {
            return word;
        }

        public void setWord(String word) {
            this.word = word;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        public Date getStart() {
            return start;
        }

        public void setStart(Date start) {
            this.start = start;
        }

        public Date getEnd() {
            return end;
        }

        public void setEnd(Date end) {
            this.end = end;
        }
    }
}
