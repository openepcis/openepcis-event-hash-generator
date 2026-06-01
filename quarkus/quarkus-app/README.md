[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# openepcis-event-hash-generator-app

### Introduction:

This RESTful Quarkus application generates the Hash Ids for EPCIS documents in JSON/XML format using the project the openepcis-event-hash-generator (
ref: https://github.com/openepcis/openepcis-event-hash-generator). By requiring only a few simple inputs, this utility simplifies it for users to produce Hash Ids for EPCIS
documents.

Application Programming Interface, or API for short, is a software bridge that enables the communication between two applications. It enables businesses to provide other users and
developers access to the data and functionality of their apps. Representational State Transfer, or REST, is a set of architectural constraints. A REST API, commonly referred to as
a RESTful API, is a web API that complies with the restrictions of the REST architectural style and enables communication with RESTful web services.

The RESTful API has been made available as a Swagger UI document for the OpenEPCIS Event Hash Generator application. The term "Swagger" refers to a set of guidelines, requirements,
and resources that aid with API documentation. Developers may then produce documentation that is helpful to users in this way. One of the platform's appealing features is Swagger
UI. Documentation must be easy to browse and precisely arranged for quick access in order to be useful. It saves a lot of time to provide API documentation using the Swagger UI.
Developers may group methods and even add examples using Swagger UI.

### Parameters:

In addition to the EPCIS document, the POST request accepts the following query parameters:

`hashAlgorithm`: It is the kind of algorithm that must be produced by the application. sha-1, sha-224, sha-256, sha-384, sha-512, sha3-224, sha3-256, sha3-384, sha3-512, md2 and
md5 are all acceptable values. default: sha-256.

`preHash`: It instructs if the pre-hash string should be printed or not. Prehash string is a string that is created utilizing the EPCIS event information and used to create the
Hash-Id. It takes either True or False value, default: False.

`beautifyPreHash`: It instructs if the Pre-Hash string needs to be formatted for easy reading purpose. It accepts either True or False value. If True then a new line is added for
each value-pair for easy reading purpose, Default: false. It is used only if the previous parameter preHash has been set to True.

`cbvVersion`: The Core Business Vocabulary version used during canonicalization and reflected in the hash-id suffix (e.g. `?ver=CBV2.1`). Acceptable
values: `2.0.0`, `2.1.0`. Default: the value of the `openepcis.event-hash.cbv-version` startup property (which itself defaults to `2.0.0`). Per-request overrides via this
parameter always win over the startup default.

### Configuration:

The service accepts the following startup properties (via `application.yml`, environment variables, or `-D` JVM flags):

| Property                               | Env variable                           | Default | Purpose                                                                                                                                                                                                                                                                    |
|----------------------------------------|----------------------------------------|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `openepcis.event-hash.cbv-version`     | `OPENEPCIS_EVENT_HASH_CBV_VERSION`     | `2.0.0` | Default CBV version when the request does not provide a `cbvVersion` query parameter.                                                                                                                                                                                      |
| `openepcis.eventhash.maxPendingEvents` | `OPENEPCIS_EVENTHASH_MAXPENDINGEVENTS` | `4096`  | Maximum number of pending events buffered between the SAX/JSON producer and a slow downstream consumer. Exceeding it fails the stream with `BackPressureFailure` rather than running out of memory. Tune up for network-bound consumers, down for memory-tight containers. |

Example — run the JVM jar with CBV 2.1 as the default and a 16k-event buffer:

```bash
java \
  -Dopenepcis.event-hash.cbv-version=2.1.0 \
  -Dopenepcis.eventhash.maxPendingEvents=16384 \
  -jar openepcis-event-hash-generator-service-runner.jar
```

### Observability:

The Quarkus service exposes standard management endpoints under `/q/`:

| Endpoint              | Purpose                                                                                                            |
|-----------------------|--------------------------------------------------------------------------------------------------------------------|
| `GET /q/health`       | Aggregated liveness + readiness — returns `UP` only if both pass                                                   |
| `GET /q/health/ready` | Readiness — wire to a Kubernetes `readinessProbe` or Docker `HEALTHCHECK`                                          |
| `GET /q/health/live`  | Liveness — wire to a Kubernetes `livenessProbe`                                                                    |
| `GET /q/metrics`      | Prometheus-format metrics — HTTP request rate/latency, JVM heap/GC/thread metrics. Scrape with Prometheus/Grafana. |
| `GET /q/swagger-ui/`  | Interactive API documentation.                                                                                     |

The readiness check runs a **smoke test**: it computes the SHA-256 of the empty string under CBV 2.0 and compares against a known-good expected URI. If the result diverges (e.g.
malformed native image, broken JCA provider) the response reports `DOWN` with both `expected` and `actual` values so operators can diagnose without reading source.

Example healthy response:

```bash
curl -s http://localhost:9000/q/health/ready | jq
```

```json
{
    "status": "UP",
    "checks": [
	{
	    "name": "OpenEPCIS Event Hash Generator health check",
	    "status": "UP",
	    "data": {
		"smokeTest": "PASS"
	    }
	}
    ]
}
```

Example Kubernetes probe wiring:

```yaml
readinessProbe:
  httpGet: { path: /q/health/ready, port: 9000 }
  initialDelaySeconds: 5
  periodSeconds: 10
livenessProbe:
  httpGet: { path: /q/health/live, port: 9000 }
  initialDelaySeconds: 15
  periodSeconds: 30
```

### Usage:

Swagger UI documentation for OpenEPCIS Event Hash Generator application can be accessed using following link:

```
https://tools.openepcis.io/q/swagger-ui/#/Hash%20Id%20Generator%20Resource/post_api_hashIdGenerator
```

1. Start using Hash Id Generator API by clicking on the "Try it out" button in the top-right corner.
2. Set the appropriate parameters, such as the Hash Algorithm, the PreHash, and Beautify PreHash, according to the requirements.
3. Specify the EPCIS document format as either XML or JSON/JSON-LD above the text area and then paste the EPCIS document in below text area.
4. When you click the "Execute" button, the program should return a List of Hash Ids if everything is in order. Any problems or missing parameters in the EPCIS document will result
   in the display of the relevant error messages as an exception.
5. If PreHash string is requested, the response will be shown as a list of objects, each of which will include the PreHash string and associated Hash-Id.

Please refer to the below GIF that describes the steps for Hash id generation using RestAPI:

![](readme-rest-swaggerui.gif)

### References:

1. For more information on the Event Hash Generator, please refer to following GitHub repository: https://github.com/openepcis/openepcis-event-hash-generator.


2. For using the Command Line or Terminal utility to generate Hash Ids, please refer to following GitHub repository: https://github.com/openepcis/openepcis-event-hash-generator-cli


3. For more information on Event Hash Generation, ordering of elements, or canonicalization, please refer to the detailed Documentation by Ralph
   Tröger: https://github.com/RalphTro/epcis-event-hash-generator
