[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Java CI](https://github.com/openepcis/openepcis-event-hash-generator/actions/workflows/maven-cli.yml/badge.svg)

# openepcis-event-hash-generator
Java library to generate event hash for EPCIS document/event in XML/JSON-LD format.

# Table of Contents
1. [Introduction](#introduction)
2. [Problem & Proposed Solution](#problem--proposed-solution)
3. [Reactive Streams](#reactive-streams)
4. [Hash Algorithm](#hash-algorithm)
5. [CBV Versions](#cbv-versions)
6. [Usage](#usage)
    - [HashId Generation for XML document](#hashid-generation-for-xml-document)
    - [HashId Generation for JSON/JSON-LD Document](#hashid-generation-for-jsonjson-ld-document)
    - [Subscription logic](#subscription-logic)
7. [Releases](#releases)
    - [Release Builds](#release-builds)
    - [Usage](#usage-1)
8. [Related Projects for direct usage](#related-projects-for-direct-usage)
    - [core library](#core-library)
    - [command-line utility](#command-line-utility)
    - [JARX-RS RESTful service bindings](#jarx-rs-restful-service-bindings)
    - [Quarkus REST Application with OpenAPI support](#quarkus-rest-application-with-openapi-support)
9. [References](#references)

### Introduction:

For various organizations to communicate with each other and control the flow of goods, there should be a common medium. EPCIS is a GS1 standard to improve cooperation between trading partners through a detailed exchange of data on physical or digital products. EPCIS is an ISO (International Organization for Standardization), IEC (International Electrotechnical Commission), and GS1 Standard that helps to generate and exchange the visibility data within an organization or across multiple organizations based on the business context. The organizations exchange information in the form of events. These events include information on what happened to object during that specific supply chain step.

### Problem & Proposed Solution:

There are instances where the same events are transmitted to the repository more than once. This can be caused by malfunctioning hardware, poor implementations, or human mistake. Generally, organizations do not wish to store the same event within their repository, and they need a mechanism through which they can associate each event uniquely within the repository. Hence, the `OpenEPCIS Event Hash Generator` tool has been developed. This tool will generate unique Hash-IDs for each event using the information present within them.

The tool adheres to a predefined order for properties, and all data included in event is canonical. Therefore, it is always assured that the event will have the same Hash-Id and will be considered duplicate events even if they have the attributes in a different sequence or if they contain the information in URN/WebURI format. Organizations can benefit greatly from using this tool to maintain unique data in their repository and prevent confusion from several copies of the same event. Detailed information on the ordering of the elements and conversion of the value can be found here: https://github.com/RalphTro/epcis-event-hash-generator.

### Reactive Streams:

The tool makes use of the Reactive stream methodology. It is standard for asynchronous stream processing with a non-blocking back pressure approach which makes it simpler to
navigate through hundreds of millions of events without degrading performance or memory. The Reactive stream technique, which is at the core of this application, allows us to
handle hundreds of millions of events easily and effectively. It is crucial to process events along the entire supply chain as soon as possible while using the least amount of memory feasible. Using this approach, events in the EPCIS document are traversed one by one, and generated HashIds are returned to the calling function.

### Hash Algorithm:

Cryptographic hash functions are created via hashing algorithms. It is a mathematical formula that converts data of any size into a fixed-size hash. The goal of a hash function
algorithm is to create a one-way function that is impossible to invert. Digital evolution has led to the development of a large number of hashing algorithms, some of the most
well-known of which are as follows: Secure Hash Algorithm (SHA), Rivest-Shamir-Adleman (RSA), Message Digest 5 (MD5), etc. Currently, the tool supports the generation following Hash Ids: sha-224, 
sha-256, sha-384, sha-512, sha3-224, sha3-256, and sha3-512.

### CBV Versions:

The current implementation facilitates the generation of Pre-Hash string and Hash-Id based on either `CBV 2.0` or `CBV 2.1` versions. CBV 2.1 introduces minor enhancements, including the immediate 
appending of user extensions to the pre-hash string after the respective fields and the introduction of the sensorElementList keyword, etc. The resulting Hash ID indicates the corresponding CBV version, such as `ver=CBV2.0` or `ver=CBV2.1`. Users have the flexibility to specify the desired CBV version for Hash-Id generation. It is important to note that the generated Hash-Id may differ between CBV 2.0 and CBV 2.1 versions due to changes in ordering.

### Usage:
The Java methods for generating the HashIds are encapsulated within the class EventHashGenerator. The technique for XML documents is described below:

If the users have EPCIS documents in XML format, then they can be provided as InputStream, which serves as the first parameter to the `fromXml` method, and the second parameter
specifies the type of hash algorithm needed (by default sha-256 algorithm is used):

#### HashId Generation for XML document
```
//Input EPCIS document/eventList as stream
final InputStream xmlStream = getClass().getResourceAsStream("/XmlEpcisDocument.xml");

//Default constructor defaults to CBV 2.0: VERSION_2_0_0
final EventHashGenerator eventHashGenerator = new EventHashGenerator();

//Parameterized constructor for CBV 2.1: VERSION_2_1_0 or CBV 2.0: VERSION_2_0_0
final EventHashGenerator eventHashGenerator2_1 = new EventHashGenerator(CBVVersion.VERSION_2_1_0);

//If only Hash-Ids are required
final List<String> xmlHashIds = eventHashGenerator.fromXml(xmlStream, "sha-256").subscribe().asStream().toList();
```

If the users have the EPCIS documents in JSON/JSON-LD format, then they can be provided as InputStream, which serves as the first argument to the `fromJson` method, and the second
parameter specifies the type of hash algorithm needed (by default sha-256 algorithm is used):

#### HashId Generation for JSON/JSON-LD Document
```
//Input EPCIS document/eventList as stream
final InputStream jsonStream  = getClass().getResourceAsStream("/JsonEpcisDocument.json");

//Default constructor defaults to CBV 2.0: VERSION_2_0_0
final EventHashGenerator eventHashGenerator = new EventHashGenerator();

//Parameterized constructor for CBV 2.1: VERSION_2_1_0 or CBV 2.0: VERSION_2_0_0
final EventHashGenerator eventHashGenerator2_1 = new EventHashGenerator(CBVVersion.VERSION_2_1_0);

//If only Hash-Ids are required
final List<String> jsonHashIds = eventHashGenerator2_1.fromJson(jsonStream, "sha-256").subscribe().asStream().toList();
```

#### Subscription logic:

The generation of Hash Ids using the Reactive Streams approach is illustrated by the example below:

If users have a large EPCIS document in XML or JSON/JSON-LD consisting of millions of events, they may supply it to the corresponding `fromXml` or `fromJson` of the EventHashGenerator class, as previously mentioned. Additionally, users can subscribe to the method so that they can print or utilize generated HashIds for additional processing as soon as they are generated and returned using the Reactive Stream approach. A simple illustration of how the HashIds may be printed using the subscription logic is shown in the following lines of code. In this simple example, when HashId is generated it is printed out to the console. By using this approach it is not required for the process to wait until all events are completed. Hence making this approach much faster and efficient.

#### HashId Generation using Subscription logic
```
//Input EPCIS document/eventList as stream
final InputStream xmlStream = getClass().getResourceAsStream("/XmlEpcisDocument.xml");
final InputStream jsonStream = getClass().getResourceAsStream("/JsonEpcisDocument.json");

//Default constructor defaults to CBV 2.0: VERSION_2_0_0
final EventHashGenerator eventHashGenerator = new EventHashGenerator();

//Parameterized constructor for CBV 2.1: VERSION_2_1_0 or CBV 2.0: VERSION_2_0_0
final EventHashGenerator eventHashGenerator2_1 = new EventHashGenerator(CBVVersion.VERSION_2_1_0);

//To beautify pre-hash string
eventHashGenerator.prehashJoin("\\n");

//Generate SHA-256 Hash-Ids and also pre-hash string
final Multi<Map<String, String>> xmlEventHash = eventHashGenerator2_1.fromXml(xmlStream, "prehash", "sha-256");

//To display Pre-hash and Hash-Id
xmlEventHash.subscribe().with(xmlHash -> System.out.println(xmlHash.get("sha-256") + "\n" + xmlHash.get("prehash") + "\n\n"), failure -> System.out.println("XML HashId Generation Failed with " + failure));

//Generate SHA-256 Hash-Ids and also pre-hash string
final Multi<Map<String, String>> jsonEventHash = eventHashGenerator.fromJson(jsonStream, "prehash", "sha-256");

//To display Pre-hash and Hash-Id
jsonEventHash.subscribe().with(jsonHash -> System.out.println(jsonHash.get("sha-256") + "\n" + jsonHash.get("prehash") + "\n\n"), failure -> System.out.println("JSON HashId Generation Failed with " + failure));
```

### Releases

#### Release Builds

The easiest and fastest way to try it out, is by downloading the jar or the native command line clients directly from the latest release:

[v0.9.4](https://github.com/openepcis/openepcis-event-hash-generator/releases/tag/v0.9.4)

#### Usage

For a detailed description, check out the [command line client documentation](cli/README.md).

### Related Projects for direct usage

#### core library :
[openepcis-event-hash-generator](core)

#### command-line utility :
[openepcis-event-hash-generator-cli](cli)

#### JARX-RS RESTful service bindings
[openepcis-event-hash-generator-rest-api](rest-api)

#### Quarkus REST Application with OpenAPI support
[openepcis-event-hash-generator-quarkus-app](quarkus-app)

### References:
For more information on Event Hash Generation, ordering of elements, or canonicalization, please refer to the detailed Documentation by Ralph Tröger: https://github.com/RalphTro/epcis-event-hash-generator.
