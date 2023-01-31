[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# openepcis-event-hash-generator-rest-api

### Introduction:
JAX-RS REST API resources for generating Hash Ids for EPCIS documents in JSON/XML format using the project the openepcis-event-hash-generator (ref: https://github.com/openepcis/openepcis-event-hash-generator). By requiring only a few simple inputs, this utility simplifies it for users to produce Hash Ids for EPCIS documents.

Application Programming Interface, or API for short, is a software bridge that enables the communication between two applications. It enables businesses to provide other users and developers access to the data and functionality of their apps. Representational State Transfer, or REST, is a set of architectural constraints. A REST API, commonly referred to as a RESTful API, is a web API that complies with the restrictions of the REST architectural style and enables communication with RESTful web services.

### Parameters:

In addition to the EPCIS document, the POST request needs the following three additional parameters:

`hashAlgorithm`: It is the kind of algorithm that must be produced by the application. sha-1, sha-224, sha-256, sha-384, sha-512, sha-224, sha3-256, sha3-384, sha3-512, md2 and md5 are all 
acceptable values. default: sha-256.

`preHash`: It instructs if the pre-hash string should be printed or not. Prehash string is a string that is created utilizing the EPCIS event information and used to create the Hash-Id. It takes either True or False value, default: False.

`beautifyPreHash`: It instructs if the Pre-Hash string needs to be formatted for easy reading purpose. It accepts either True or False value. If True then a new line is added for each value-pair for easy reading purpose, Default: false. It is used only if the previous parameter preHash has been set to True.
