[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# openepcis-event-hash-generator
Java library to generate event hash for EPCIS document/event in XML/JSON-LD format.

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
well-known of which are as follows: Secure Hash Algorithm (SHA), Rivest-Shamir-Adleman (RSA), Message Digest 5 (MD5), etc. Currently, the tool supports the generation following Hash Ids: sha-1, sha-224, sha-256, sha-384, sha-512, sha3-224, sha3-256, sha3-384, sha3-512, md2, md5.

### Usage:
The Java methods for generating the HashIds are encapsulated within the class EventHashGenerator. The technique for XML documents is described below:

If the users have EPCIS documents in XML format, then they can be provided as InputStream, which serves as the first parameter to the `fromXml` method, and the second parameter
specifies the type of hash algorithm needed (by default sha-256 algorithm is used):

#### HashId Generation for XML document
```
final InputStream xmlStream = getClass().getResourceAsStream("/XmlEpcisDocument.xml");
final Multi<String> eventHashIds = EventHashGenerator.fromXml(xmlStream, "sha-512");
//final List<String> eventHashIds = EventHashGenerator.fromXml(xmlStream, null).subscribe().asStream().toList();
```

If the users have the EPCIS documents in JSON/JSON-LD format, then they can be provided as InputStream, which serves as the first argument to the `fromJson` method, and the second
parameter specifies the type of hash algorithm needed (by default sha-256 algorithm is used):

#### HashId Generation for JSON/JSON-LD Document
```
final InputStream jsonStream = getClass().getResourceAsStream("/JsonEpcisDocument.json");
final Multi<String> eventHashIds = EventHashGenerator.fromJson(jsonStream, "sha3-256");
//final List<String> eventHashIds = EventHashGenerator.fromJson(jsonStream, "sha-512").subscribe().asStream().toList();
```

#### Subscription logic:

The generation of Hash Ids using the Reactive Streams approach is illustrated by the example below:

If users have a large EPCIS document in XML or JSON/JSON-LD consisting of millions of events, they may supply it to the corresponding `fromXml` or `fromJson` of the EventHashGenerator class, as previously mentioned. Additionally, users can subscribe to the method so that they can print or utilize generated HashIds for additional processing as soon as they are generated and returned using the Reactive Stream approach. A simple illustration of how the HashIds may be printed using the subscription logic is shown in the following lines of code. In this simple example, when HashId is generated it is printed out to the console. By using this approach it is not required for the process to wait until all events are completed. Hence making this approach much faster and efficient.

#### HashId Generation using Subscription logic
```
final InputStream xmlStream = getClass().getResourceAsStream("/XmlEpcisDocument.xml");
final InputStream jsonStream = getClass().getResourceAsStream("/JsonEpcisDocument.json");
 
EventHashGenerator.fromXml(xmlStream, "sha-256").subscribe().with(
                xmlHashId -> System.out.println(xmlHashId),
                failure -> System.out.println("XML HashId Generation Failed with : " + failure));
 
EventHashGenerator.fromJson(jsonStream, "sha-256").subscribe().with(
                jsonHashId -> System.out.println(jsonHashId),
                failure -> System.out.println("JSON HashId Generation Failed with " + failure));
```


### Related Projects for direct usage

#### command-line utility :
Project: openepcis-event-hash-generator-cli

Reference: https://github.com/openepcis/openepcis-event-hash-generator-cli

#### RESTful service
Project: openepcis-event-hash-generator-rest-api

Reference: https://github.com/openepcis/openepcis-event-hash-generator-rest-api


### References:
For more information on Event Hash Generation, ordering of elements, or canonicalization, please refer to the detailed Documentation by Ralph Tröger: https://github.com/RalphTro/epcis-event-hash-generator.
