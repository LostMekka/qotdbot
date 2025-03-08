package xyz.elspeth.schedule;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler {
	
	private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);
	
	public static void schedule(Runnable runnable) {
		
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
		ZonedDateTime nextRun = now.withHour(0)
								   .withMinute(0)
								   .withSecond(0);
		if (now.compareTo(nextRun) > 0) {
			nextRun = nextRun.plusDays(1);
		}
		
		Duration duration     = Duration.between(now, nextRun);
		long     initialDelay = duration.getSeconds();
		
		//noinspection resource
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(
			runnable,
			initialDelay,
			TimeUnit.DAYS.toSeconds(1),
			TimeUnit.SECONDS
		);
		logger.info("Started logger. Next run: {}", duration);
	}
	
}
