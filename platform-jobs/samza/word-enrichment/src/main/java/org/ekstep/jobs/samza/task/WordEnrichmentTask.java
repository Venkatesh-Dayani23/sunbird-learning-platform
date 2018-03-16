package org.ekstep.jobs.samza.task;

import java.util.HashMap;
import java.util.Map;

import org.apache.samza.config.Config;
import org.apache.samza.system.IncomingMessageEnvelope;
import org.apache.samza.system.OutgoingMessageEnvelope;
import org.apache.samza.system.SystemStream;
import org.apache.samza.task.InitableTask;
import org.apache.samza.task.MessageCollector;
import org.apache.samza.task.StreamTask;
import org.apache.samza.task.TaskContext;
import org.apache.samza.task.TaskCoordinator;
import org.apache.samza.task.WindowableTask;
import org.ekstep.jobs.samza.service.ISamzaService;
import org.ekstep.jobs.samza.service.WordEnrichmentService;
import org.ekstep.jobs.samza.service.task.JobMetrics;
import org.ekstep.jobs.samza.util.JobLogger;

public class WordEnrichmentTask implements StreamTask, InitableTask, WindowableTask {
	
	static JobLogger LOGGER = new JobLogger(WordEnrichmentTask.class);

	private JobMetrics metrics;
	
	ISamzaService service = new WordEnrichmentService();

	@Override
	public void init(Config config, TaskContext context) throws Exception {

		try {
			metrics = new JobMetrics(context, config.get("output.metrics.job.name"), config.get("output.metrics.topic.name"));
			service.initialize(config);
			LOGGER.info("Task initialized");
		} catch(Exception ex) {
			LOGGER.error("Task initialization failed", ex);
			throw ex;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void process(IncomingMessageEnvelope envelope, MessageCollector collector, TaskCoordinator coordinator) throws Exception {
		Map<String, Object> outgoingMap = getMessage(envelope);
		try {
			service.processMessage(outgoingMap, metrics, collector);
		} catch (Exception e) {
			metrics.incErrorCounter();
			LOGGER.error("Message processing failed", outgoingMap, e);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getMessage(IncomingMessageEnvelope envelope) {
		try {
			return (Map<String, Object>) envelope.getMessage();
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Invalid message:" + envelope.getMessage(), e);
			return new HashMap<String, Object>();
		}
	}
	
	@Override
	public void window(MessageCollector collector, TaskCoordinator coordinator) throws Exception {
		Map<String, Object> event = metrics.collect();
		collector.send(new OutgoingMessageEnvelope(new SystemStream("kafka", metrics.getTopic()), event));
		metrics.clear();
	}
}
