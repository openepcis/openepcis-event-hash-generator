[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# openepcis-event-hash-generator-quarkus-app

### Introduction:
This RESTful Quarkus application generates the Hash Ids for EPCIS documents in JSON/XML format using the project the openepcis-event-hash-generator (ref: https://github.com/openepcis/openepcis-event-hash-generator). By requiring only a few simple inputs, this utility simplifies it for users to produce Hash Ids for EPCIS documents.

Application Programming Interface, or API for short, is a software bridge that enables the communication between two applications. It enables businesses to provide other users and developers access to the data and functionality of their apps. Representational State Transfer, or REST, is a set of architectural constraints. A REST API, commonly referred to as a RESTful API, is a web API that complies with the restrictions of the REST architectural style and enables communication with RESTful web services.

The RESTful API has been made available as a Swagger UI document for the OpenEPCIS Event Hash Generator application. The term "Swagger" refers to a set of guidelines, requirements, and resources that aid with API documentation. Developers may then produce documentation that is helpful to users in this way. One of the platform's appealing features is Swagger UI. Documentation must be easy to browse and precisely arranged for quick access in order to be useful. It saves a lot of time to provide API documentation using the Swagger UI. Developers may group methods and even add examples using Swagger UI.

### Parameters:

In addition to the EPCIS document, the POST request needs the following three additional parameters:

`hashAlgorithm`: It is the kind of algorithm that must be produced by the application. sha-1, sha-224, sha-256, sha-384, sha-512, sha-224, sha3-256, sha3-384, sha3-512, md2 and md5 are all 
acceptable values. default: sha-256.

`preHash`: It instructs if the pre-hash string should be printed or not. Prehash string is a string that is created utilizing the EPCIS event information and used to create the Hash-Id. It takes either True or False value, default: False.

`beautifyPreHash`: It instructs if the Pre-Hash string needs to be formatted for easy reading purpose. It accepts either True or False value. If True then a new line is added for each value-pair for easy reading purpose, Default: false. It is used only if the previous parameter preHash has been set to True.

### Usage:
Swagger UI documentation for OpenEPCIS Event Hash Generator application can be accessed using following link:
```
https://tools.openepcis.io/q/swagger-ui/#/Hash%20Id%20Generator%20Resource/post_api_hashIdGenerator
```

1. Start using Hash Id Generator API by clicking on the "Try it out" button in the top-right corner.
2. Set the appropriate parameters, such as the Hash Algorithm, the PreHash, and Beautify PreHash, according to the requirements.
3. Specify the EPCIS document format as either XML or JSON/JSON-LD above the text area and then paste the EPCIS document in below text area.
4. When you click the "Execute" button, the program should return a List of Hash Ids if everything is in order. Any problems or missing parameters in the EPCIS document will result in the display of the relevant error messages as an exception.
5. If PreHash string is requested, the response will be shown as a list of objects, each of which will include the PreHash string and associated Hash-Id.

Please refer to the below GIF that describes the steps for Hash id generation using RestAPI:

![](readme-rest-swaggerui.gif)

### References:
1. For more information on the Event Hash Generator, please refer to following GitHub repository: https://github.com/openepcis/openepcis-event-hash-generator.


2. For using the Command Line or Terminal utility to generate Hash Ids, please refer to following GitHub repository: https://github.com/openepcis/openepcis-event-hash-generator-cli


3. For more information on Event Hash Generation, ordering of elements, or canonicalization, please refer to the detailed Documentation by Ralph Tröger: https://github.com/RalphTro/epcis-event-hash-generator
