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

import org.fireflyframework.observability.metrics.FireflyMetricsSupport;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Duration;

/**
 * Micrometer-based implementation of GenericTransactionalObserver.
 * <p>
 * This implementation publishes counters, timers, and distribution summaries
 * for transactional operations across different patterns (SAGA, TCC, etc.).
 * <p>
 * All metrics use the Firefly naming convention ({@code firefly.transactional.*}).
 */
public class MicrometerTransactionalObserver extends FireflyMetricsSupport implements GenericTransactionalObserver {

    private final TransactionalMetricsCollector metricsCollector;

    public MicrometerTransactionalObserver(MeterRegistry registry, TransactionalMetricsCollector metricsCollector) {
        super(registry, "transactional");
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void onTransactionStarted(String transactionType, String transactionName, String correlationId) {
        counter("transaction.started",
                "transaction.type", transactionType.toLowerCase(),
                "transaction.name", transactionName)
                .increment();
        metricsCollector.recordTransactionStarted(transactionType, transactionName, correlationId);
    }

    @Override
    public void onTransactionCompleted(String transactionType, String transactionName, String correlationId, boolean success, long durationMs) {
        String outcome = success ? "success" : "failure";

        counter("transaction.completed",
                "transaction.type", transactionType.toLowerCase(),
                "transaction.name", transactionName,
                "outcome", outcome)
                .increment();

        if (durationMs > 0) {
            timer("transaction.duration",
                    "transaction.type", transactionType.toLowerCase(),
                    "transaction.name", transactionName,
                    "outcome", outcome)
                    .record(Duration.ofMillis(durationMs));
        }

        metricsCollector.recordTransactionCompleted(transactionType, transactionName, correlationId, success, durationMs);
    }

    @Override
    public void onStepStarted(String transactionType, String transactionName, String correlationId, String stepId) {
        counter("step.started",
                "transaction.type", transactionType.toLowerCase(),
                "transaction.name", transactionName,
                "step.id", stepId)
                .increment();
        metricsCollector.recordStepStarted(transactionType, transactionName, stepId);
    }

    @Override
    public void onStepSuccess(String transactionType, String transactionName, String correlationId, String stepId, int attempts, long durationMs) {
        String txType = transactionType.toLowerCase();

        counter("step.completed",
                "transaction.type", txType,
                "transaction.name", transactionName,
                "step.id", stepId,
                "outcome", "success")
                .increment();

        if (durationMs > 0) {
            timer("step.duration",
                    "transaction.type", txType,
                    "transaction.name", transactionName,
                    "step.id", stepId,
                    "outcome", "success")
                    .record(Duration.ofMillis(durationMs));
        }

        if (attempts > 1) {
            counter("step.retries",
                    "transaction.type", txType,
                    "transaction.name", transactionName,
                    "step.id", stepId,
                    "outcome", "success")
                    .increment(attempts - 1);
        }

        distributionSummary("step.attempts",
                "transaction.type", txType,
                "transaction.name", transactionName,
                "step.id", stepId,
                "outcome", "success")
                .record(attempts);

        metricsCollector.recordStepCompleted(transactionType, transactionName, stepId, attempts);
    }

    @Override
    public void onStepFailure(String transactionType, String transactionName, String correlationId, String stepId, int attempts, long durationMs, Throwable error) {
        String txType = transactionType.toLowerCase();
        String errorType = error.getClass().getSimpleName();

        counter("step.completed",
                "transaction.type", txType,
                "transaction.name", transactionName,
                "step.id", stepId,
                "outcome", "failure",
                "error.type", errorType)
                .increment();

        if (durationMs > 0) {
            timer("step.duration",
                    "transaction.type", txType,
                    "transaction.name", transactionName,
                    "step.id", stepId,
                    "outcome", "failure",
                    "error.type", errorType)
                    .record(Duration.ofMillis(durationMs));
        }

        if (attempts > 1) {
            counter("step.retries",
                    "transaction.type", txType,
                    "transaction.name", transactionName,
                    "step.id", stepId,
                    "outcome", "failure",
                    "error.type", errorType)
                    .increment(attempts - 1);
        }

        distributionSummary("step.attempts",
                "transaction.type", txType,
                "transaction.name", transactionName,
                "step.id", stepId,
                "outcome", "failure",
                "error.type", errorType)
                .record(attempts);

        metricsCollector.recordStepFailed(transactionType, transactionName, stepId, attempts);
    }

    @Override
    public void onCompensationStarted(String transactionType, String transactionName, String correlationId, String stepId) {
        counter("compensation.started",
                "transaction.type", transactionType.toLowerCase(),
                "transaction.name", transactionName,
                "step.id", stepId)
                .increment();
        metricsCollector.recordCompensationStarted(transactionType, transactionName, stepId);
    }

    @Override
    public void onCompensationSuccess(String transactionType, String transactionName, String correlationId, String stepId, int attempts, long durationMs) {
        String txType = transactionType.toLowerCase();

        counter("compensation.completed",
                "transaction.type", txType,
                "transaction.name", transactionName,
                "step.id", stepId,
                "outcome", "success")
                .increment();

        if (durationMs > 0) {
            timer("compensation.duration",
                    "transaction.type", txType,
                    "transaction.name", transactionName,
                    "step.id", stepId,
                    "outcome", "success")
                    .record(Duration.ofMillis(durationMs));
        }

        if (attempts > 1) {
            counter("compensation.retries",
                    "transaction.type", txType,
                    "transaction.name", transactionName,
                    "step.id", stepId,
                    "outcome", "success")
                    .increment(attempts - 1);
        }

        metricsCollector.recordCompensationCompleted(transactionType, transactionName, stepId, true);
    }

    @Override
    public void onCompensationFailure(String transactionType, String transactionName, String correlationId, String stepId, int attempts, long durationMs, Throwable error) {
        String txType = transactionType.toLowerCase();
        String errorType = error.getClass().getSimpleName();

        counter("compensation.completed",
                "transaction.type", txType,
                "transaction.name", transactionName,
                "step.id", stepId,
                "outcome", "failure",
                "error.type", errorType)
                .increment();

        if (durationMs > 0) {
            timer("compensation.duration",
                    "transaction.type", txType,
                    "transaction.name", transactionName,
                    "step.id", stepId,
                    "outcome", "failure",
                    "error.type", errorType)
                    .record(Duration.ofMillis(durationMs));
        }

        if (attempts > 1) {
            counter("compensation.retries",
                    "transaction.type", txType,
                    "transaction.name", transactionName,
                    "step.id", stepId,
                    "outcome", "failure",
                    "error.type", errorType)
                    .increment(attempts - 1);
        }

        metricsCollector.recordCompensationCompleted(transactionType, transactionName, stepId, false);
    }
}
