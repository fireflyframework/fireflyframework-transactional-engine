# Firefly Framework - Transactional Engine

[![CI](https://github.com/fireflyframework/fireflyframework-transactional-engine/actions/workflows/ci.yml/badge.svg)](https://github.com/fireflyframework/fireflyframework-transactional-engine/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

> SAGA and TCC distributed transaction engine with annotation-driven orchestration, compensation, and persistence for reactive microservices.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [License](#license)

## Overview

Firefly Framework Transactional Engine provides a production-grade distributed transaction management system implementing both the SAGA and TCC (Try-Confirm/Cancel) patterns. It enables developers to define complex multi-step business transactions using annotations, with automatic compensation handling when steps fail.

The SAGA engine supports annotation-driven step definitions (`@Saga`, `@SagaStep`, `@CompensationSagaStep`), data flow between steps via `@FromStep`, external HTTP call steps, composition-based saga building, and persistence via in-memory or Redis providers. The TCC engine offers similar capabilities with `@Tcc`, `@TccParticipant`, `@TryMethod`, `@ConfirmMethod`, and `@CancelMethod` annotations.

Both engines include observability through Micrometer metrics and OpenTelemetry tracing, health indicators, recovery services for interrupted transactions, and configurable backpressure strategies for high-throughput scenarios.

## Features

- SAGA pattern with `@Saga`, `@SagaStep`, `@CompensationSagaStep` annotations
- TCC pattern with `@Tcc`, `@TccParticipant`, `@TryMethod`, `@ConfirmMethod`, `@CancelMethod`
- Data flow between steps via `@FromStep`, `@Input`, `@Variable` annotations
- External HTTP call steps with `@ExternalSagaStep`
- Saga/TCC composition builder for programmatic orchestration
- Saga topology visualization and reporting
- Persistence providers: in-memory, Redis
- Transaction recovery service for interrupted sagas
- Step event publishing for cross-service coordination
- Configurable compensation error handlers (fail-fast, retry-with-backoff, log-and-continue)
- Backpressure strategies: adaptive, batched, circuit-breaker
- Micrometer metrics and health indicators
- Compilation-time saga validation
- `@EnableTransactionalEngine` annotation for easy activation

## Requirements

- Java 21+
- Spring Boot 3.x
- Maven 3.9+
- Redis (optional, for distributed saga persistence)

## Installation

```xml
<dependency>
    <groupId>org.fireflyframework</groupId>
    <artifactId>fireflyframework-transactional-engine</artifactId>
    <version>26.02.03</version>
</dependency>
```

## Quick Start

```java
import org.fireflyframework.transactional.saga.annotations.*;

@Saga(name = "create-order")
@Component
public class CreateOrderSaga {

    @SagaStep(order = 1, name = "reserve-inventory")
    public Mono<ReservationResult> reserveInventory(@Input OrderRequest request) {
        return inventoryService.reserve(request);
    }

    @CompensationSagaStep(compensates = "reserve-inventory")
    public Mono<Void> cancelReservation(@FromStep("reserve-inventory") ReservationResult result) {
        return inventoryService.cancel(result.getReservationId());
    }

    @SagaStep(order = 2, name = "process-payment")
    public Mono<PaymentResult> processPayment(
            @Input OrderRequest request,
            @FromStep("reserve-inventory") ReservationResult reservation) {
        return paymentService.charge(request, reservation);
    }
}
```

## Configuration

```yaml
firefly:
  transactional-engine:
    saga:
      persistence:
        type: redis  # in-memory, redis
      redis:
        host: localhost
        port: 6379
      backpressure:
        strategy: adaptive
    tcc:
      timeout: 30s
```

## Documentation

Additional documentation is available in the [docs/](docs/) directory:

- [Architecture](docs/ARCHITECTURE.md)
- [Configuration](docs/CONFIGURATION.md)
- [Api Reference](docs/API_REFERENCE.md)
- [Saga Guide](docs/SAGA_GUIDE.md)
- [Tcc Guide](docs/TCC_GUIDE.md)

## Contributing

Contributions are welcome. Please read the [CONTRIBUTING.md](CONTRIBUTING.md) guide for details on our code of conduct, development process, and how to submit pull requests.

## License

Copyright 2024-2026 Firefly Software Solutions Inc.

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
