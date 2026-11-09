package com.dts.progress.simulationperformance;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class SimulationStreamConfig {
    @Bean(name="simulationPerformanceListenerFactory")
    public ConcurrentKafkaListenerContainerFactory<String,String> factory(KafkaProperties properties,ObjectProvider<SslBundles> bundles) {
        var config=new java.util.HashMap<String,Object>(properties.buildConsumerProperties(bundles.getIfAvailable()));
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,false);config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
        var factory=new ConcurrentKafkaListenerContainerFactory<String,String>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(config,new StringDeserializer(),new StringDeserializer()));
        factory.setConcurrency(1);factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000,FixedBackOff.UNLIMITED_ATTEMPTS)));
        return factory;
    }
}
