/*
 * Copyright (c) 2023 Firefly Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fireflyframework.transactional.shared.observability;

import org.fireflyframework.observability.health.FireflyHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.Map;

/**
 * Spring Boot Actuator health indicator for transactional systems.
 * <p>
 * Extends {@link FireflyHealthIndicator} for consistent health reporting.
 * Provides health status based on transaction execution metrics,
 * success rates, and system performance indicators across all patterns.
 */
public class TransactionalHealthIndicator extends FireflyHealthIndicator {

    private final TransactionalMetricsCollector metricsCollector;
    private final TransactionalHealthProperties properties;

    public TransactionalHealthIndicator(TransactionalMetricsCollector metricsCollector,
                                        TransactionalHealthProperties properties) {
        super("transactional");
        this.metricsCollector = metricsCollector;
        this.properties = properties;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try {
            OverallMetrics metrics = metricsCollector.getOverallMetrics();

            // Check overall success rate
            double successRate = metrics.getOverallSuccessRate();
            double stepSuccessRate = metrics.getOverallStepSuccessRate();
            double compensationSuccessRate = metrics.getOverallCompensationSuccessRate();

            // Determine health status
            Status status = determineStatus(successRate, stepSuccessRate, compensationSuccessRate);
            builder.status(status);

            // Add detailed metrics
            builder.withDetail("overall.success.rate", String.format("%.2f%%", successRate * 100))
                   .withDetail("step.success.rate", String.format("%.2f%%", stepSuccessRate * 100))
                   .withDetail("compensation.success.rate", String.format("%.2f%%", compensationSuccessRate * 100))
                   .withDetail("total.transactions", metrics.getTotalTransactionsStarted())
                   .withDetail("total.steps", metrics.getTotalStepsStarted())
                   .withDetail("total.compensations", metrics.getTotalCompensationsStarted())
                   .withDetail("average.execution.time", String.format("%.2fms", metrics.getOverallAverageExecutionTime()))
                   .withDetail("active.transaction.types", metrics.getActiveTransactionTypes());

            // Add per-type metrics
            metrics.getMetricsByType().forEach((type, typeMetrics) -> {
                builder.withDetail(type.toLowerCase() + ".metrics", Map.of(
                    "success.rate", String.format("%.2f%%", typeMetrics.getSuccessRate() * 100),
                    "transactions", typeMetrics.getTransactionsStarted(),
                    "steps", typeMetrics.getStepsStarted(),
                    "compensations", typeMetrics.getCompensationsStarted()
                ));
            });

        } catch (Exception e) {
            builder.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("error.type", e.getClass().getSimpleName());
        }
    }

    private Status determineStatus(double successRate, double stepSuccessRate, double compensationSuccessRate) {
        if (successRate < properties.getCriticalSuccessRateThreshold() ||
            stepSuccessRate < properties.getCriticalSuccessRateThreshold() ||
            compensationSuccessRate < properties.getCriticalSuccessRateThreshold()) {
            return Status.DOWN;
        }

        if (successRate < properties.getWarningSuccessRateThreshold() ||
            stepSuccessRate < properties.getWarningSuccessRateThreshold() ||
            compensationSuccessRate < properties.getWarningSuccessRateThreshold()) {
            return new Status("DEGRADED");
        }

        return Status.UP;
    }

    /**
     * Configuration properties for health checks.
     */
    public static class TransactionalHealthProperties {
        private double warningSuccessRateThreshold = 0.95; // 95%
        private double criticalSuccessRateThreshold = 0.85; // 85%

        public double getWarningSuccessRateThreshold() {
            return warningSuccessRateThreshold;
        }

        public void setWarningSuccessRateThreshold(double warningSuccessRateThreshold) {
            this.warningSuccessRateThreshold = warningSuccessRateThreshold;
        }

        public double getCriticalSuccessRateThreshold() {
            return criticalSuccessRateThreshold;
        }

        public void setCriticalSuccessRateThreshold(double criticalSuccessRateThreshold) {
            this.criticalSuccessRateThreshold = criticalSuccessRateThreshold;
        }
    }
}
