package com.dts.progress.simulationperformance;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Listener is separate from its container factory to avoid bootstrap circular references. */
@Component
public class SimulationPerformanceConsumer {
    private final SimulationEventStore store;
    public SimulationPerformanceConsumer(SimulationEventStore store){this.store=store;}
    @KafkaListener(topics="${simulation.performance.topic:simulation-performance-v1}",groupId="dts-progress-simulation-performance-v1",
            containerFactory="simulationPerformanceListenerFactory",autoStartup="${simulation.performance.consumer-enabled:true}")
    public void receive(String body){store.accept(body);}
}
