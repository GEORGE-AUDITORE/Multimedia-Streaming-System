package client;

import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SpeedTester {

    public static double measureDownloadSpeedMbps() {
        SpeedTestSocket speedTestSocket = new SpeedTestSocket();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Double> speedMbps = new AtomicReference<>(5.0);

        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {
            @Override
            public void onCompletion(SpeedTestReport report) {
                double bitsPerSecond = report.getTransferRateBit().doubleValue();
                speedMbps.set(bitsPerSecond / 1_000_000.0);
                latch.countDown();
            }

            @Override
            public void onProgress(float percent, SpeedTestReport report) {
                System.out.printf("Testing speed... %.0f%%%n", percent);
            }

            @Override
            public void onError(SpeedTestError speedTestError, String errorMessage) {
                System.out.println("Speed test failed, using fallback speed. Details: " + errorMessage);
                latch.countDown();
            }
        });

        try {
            speedTestSocket.startDownload("http://ipv4.ikoula.testdebit.info/10M.iso");

            boolean finished = latch.await(15, TimeUnit.SECONDS);
            if (!finished) {
                System.out.println("Speed test timed out.");
            }

        } catch (Exception e) {
            System.out.println("Speed test failed: " + e.getMessage());
        }

        return Math.max(0.5, speedMbps.get());
    }
}