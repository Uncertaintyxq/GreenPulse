package com.greenpulse.service;

import com.greenpulse.dao.SmartBinDAO;
import com.greenpulse.event.BinEventBus;
import com.greenpulse.model.ReadingSource;
import com.greenpulse.model.SmartBin;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TelemetrySimulatorEngine {
    private static final Logger LOG = Logger.getLogger(TelemetrySimulatorEngine.class.getName());
    private static ScheduledExecutorService scheduler;
    private static boolean isRunning = false;
    private static final Random random = new Random();

    public static synchronized void startSimulation(Runnable onTelemetryUpdated) {
        if (isRunning) return;

        scheduler = Executors.newSingleThreadScheduledExecutor();
        isRunning = true;

        scheduler.scheduleAtFixedRate(() -> {
            try {
                SmartBinDAO dao = new SmartBinDAO();
                Optional<SmartBin> picked = dao.findRandomActive();
                if (picked.isEmpty()) return;
                SmartBin targetBin = picked.get();

                double fillIncrement = 3.0 + (12.0 - 3.0) * random.nextDouble();
                targetBin.setFillLevel(Math.min(100.0, targetBin.getFillPercentage() + fillIncrement));
                targetBin.simulateSensorDrift(random); // each bin type drifts its own sensors (polymorphism)

                if (dao.recordReading(targetBin, ReadingSource.SIMULATOR)) {
                    BinEventBus.getInstance().publish(targetBin, ReadingSource.SIMULATOR);
                    if (onTelemetryUpdated != null) onTelemetryUpdated.run();
                }
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Telemetry simulator tick failed", ex);
            }
        }, 2, 5, TimeUnit.SECONDS);

        LOG.info("Live simulation engine started.");
    }

    public static synchronized void stopSimulation() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            isRunning = false;
            LOG.info("Simulation engine stopped.");
        }
    }

    public static boolean isRunning() {
        return isRunning;
    }
}
