# openepcis-event-hash-generator-benchmarks

JMH (Java Microbenchmark Harness) microbenchmarks for the event-hash hot paths.

This module is **not** part of the default build. It only joins the reactor when the `benchmarks` Maven profile is active, so normal `mvn clean package` is completely unaffected.

## What it measures

| Benchmark        | What it covers                                                                                                                                                                                                                   |
|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `generateHashId` | The isolated digest layer: `MessageDigest` per-thread cache + `HexFormat` + the algorithm registry + the CBV-version suffix lookup. **This is the clean hot-path number** — the regression canary for the caching optimisations. |
| `fromXml`        | The full XML pipeline: SAX parse → `ContextNode` tree → canonicalize → hash, including the reactive worker-pool offload. End-to-end latency, not pure hashing.                                                                   |
| `fromJson`       | The full JSON pipeline: Jackson stream → `ContextNode` tree → canonicalize → hash, including the reactive worker-pool offload.                                                                                                   |

## Build

```bash
# From the repo root. -Pbenchmarks pulls this module into the build; -Pci-build skips the formatter.
mvn -Pci-build -Pbenchmarks -pl benchmarks clean package -DskipTests
```

Produces a runnable uber-jar at `benchmarks/target/benchmarks.jar`.

## Run

```bash
# All benchmarks, full accuracy (3 warmup + 5 measurement iterations, 2 forks). ~3 minutes.
java -jar benchmarks/target/benchmarks.jar

# One benchmark by name (regex match against the method)
java -jar benchmarks/target/benchmarks.jar generateHashId

# Quick smoke run (1 warmup, 2 measurement, 1 fork) — seconds, lower accuracy
java -jar benchmarks/target/benchmarks.jar -wi 1 -i 2 -f 1
```

## Reading the output

Final table columns:

```
Benchmark      Mode  Cnt    Score    Error  Units
generateHashId avgt   10    2.096 ±  0.039  us/op
```

- **Mode** `avgt` — average time per operation (we use `@BenchmarkMode(Mode.AverageTime)`).
- **Cnt** — number of measurement data points pooled = `forks × measurement-iterations` (e.g. 2 × 5 = 10).
- **Score** — the headline number: average time per call.
- **Error** — half-width of the 99.9% confidence interval. Smaller = more stable result.
- **Units** `us/op` — microseconds per operation.

Lower Score = faster. The `generateHashId` Score is the one to watch over time: if it jumps
sharply, a hot-path caching optimisation (Pattern, MessageDigest, HexFormat) was likely reverted.

`fromXml` / `fromJson` Scores include SAX/Jackson parsing **and** a reactive worker-pool
round-trip, so their absolute numbers reflect end-to-end latency rather than raw hashing speed.

## Known caveat — 30s shutdown wait on `fromXml` / `fromJson`

These two benchmarks exercise the reactive pipeline, which offloads work to background
worker-pool threads (`runSubscriptionOn(...)`). Some of those threads are non-daemon and
created inside library code we can't shut down from the benchmark, so JMH waits ~30s per fork
before force-killing the forked JVM. **The benchmark numbers are still valid** — the wait
happens after measurement completes.

Mitigations:

- Run with `-f 1` to incur the wait once per benchmark instead of twice.
- Run only the clean benchmark: `java -jar benchmarks/target/benchmarks.jar generateHashId` (no offload, no wait).
