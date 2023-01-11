[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# openepcis-event-hash-generator-cli

Command line tool to generate Hash-Ids based on XML/JSON EPCIS document provided.

### Introduction:

This command line tool generates the Hash Ids for EPCIS documents in JSON/XML format using the project the `openepcis-event-hash-generator` (ref: https://github.com/openepcis/openepcis-event-hash-generator).

By requiring only a few simple command-line inputs, this utility simplifies it for users to produce Hash Ids for EPCIS documents. It accepts numerous arguments and performs various operations.


### Parameters:

The program accepts a number of parameters that are supplied using the simple alphabet. The different acceptable parameters and their values are listed below:

`-h`: denotes "help," when used will provide all acceptable arguments along with their descriptions.

`-a`: denotes "algorithm", It indicates the kind of algorithm that must be produced by the tool. Sha-1, Sha-224, Sha-256, Sha-384, Sha-512, Sha3-224, Sha3-256, Sha3-384, Sha3-512, MD2 and MD5 are all acceptable values. default: SHA-256.

`-e`: denotes the "enforced format", It describes the type of EPCIS document that has been provided. It is essential to indicate the document type since XML and JSON documents are handled in different ways. JSON and XML are accepted values. Default: JSON and can be identified based on the file format automatically.

`-p`: denotes the "pre-hash", It is a string created utilizing the EPCIS event information and used to create the Hash Id. It instructs if the pre-hash string should be printed 
or not. Upon request, the pre-hash string for each event will be displayed.

`-b`: denotes the "batch", It gives instructions on whether to write the pre-hash string and Hash ids to the file. If provided then all event pre-hashes and their 
corresponding Hash-ids will be written into two separate files.

`-j`: denotes the "join". It's easy to beautify the pre-hash string by indicating whether to attach a certain string to the pre-hash. It could be simpler to discern between distinct properties in a pre-hash text to troubleshoot problems If you add a new line (\n) or a tab character (\t). By default, no additional characters are added to the pre-hash string.

### Runtime Requirements:

The java sources are using some of the latest java language improvements that are only support by recent Java Runtime Environments. The java jar-file which is provided which each release was compiled using Java 17, which means to run and execute it you also need to have Java 17 runtime environment installed on your local machine.  

### Releases:
There is no need to compile this project by yourself, we are providing release build assets, which can be found at the following link: 
[https://github.com/openepcis/openepcis-event-hash-generator/releases](https://github.com/openepcis/openepcis-event-hash-generator/releases)

#### Assets:
| name | description |
|----------|-----------|
|openepcis-event-hash-generator-cli-${VERSION}-jar-with-dependencies.jar|Java runnable JAR File. Use this file if you already have Java 17 Runtime Environment installed on your system|
|openepcis-event-hash-generator-cli-${VERSION}-linux|Linux binary executable [amd64]|
|openepcis-event-hash-generator-cli-${VERSION}-windows| Windows binary executable [amd64]|
|openepcis-event-hash-generator-cli-${VERSION}-mac|MacOS Intel binary executable|

We do not provide binaries for any arm-based platforms yet, but that might change in the near future. 
If you need to run it on any other OS, please download and install a Java 17 Runtime Environment that is suitable for your environment and execute the runnable jar using the appropriate JVM for your platform.  

### Example commands:

If you have Java 17 Runtime EnvironmentDownload the corresponding binaries from the releases link above. Navigate to the folder containing the binary using the terminal or command prompt. The general command that may be used to create hash IDs for the EPCIS document located within the remote URL is as follows:
```
./openepcis-event-hash-generator-cli-${VERSION}-[linux|windows|mac] -a hash-algorithm-type url-for-epcis-document
```

Note: 
* The following commands are based on the MacOS native library, however other OS commands work similarly. 
* If security concerns prevent you from opening the binary file, you might need to provide some access privileges in order to run the program.

1. To access every help command available in the tool, use the command below:
```
./openepcis-event-hash-generator-cli-0.9.1-SNAPSHOT-mac -h
```

2. The following command will only construct hash-ids for the EPCIS document in the remote URL:
```
./openepcis-event-hash-generator-cli-0.9.1-SNAPSHOT-mac  -e XML -a sha1-224 https://raw.githubusercontent.
com/gs1/EPCIS/master/XML/AssociationEvent/AssociationEventExamples.xml
```

3. The following command will generate remote URL hash-ids for EPCIS documents and display the pre-hash string:
```
./openepcis-event-hash-generator-cli-0.9.1-SNAPSHOT-mac -e XML -a sha3-512 -p https://raw.githubusercontent.
com/gs1/EPCIS/master/XML/AssociationEvent/AssociationEventExamples.xml
```

4. The following command will generate hash IDs for EPCIS documents at a remote URL and write the hash IDs and pre-hash strings to a file:
```
./openepcis-event-hash-generator-cli-0.9.1-SNAPSHOT-mac -e XML -a sha3-512 -b https://raw.githubusercontent.
com/gs1/EPCIS/master/XML/AssociationEvent/AssociationEventExamples.xml
```

5. The following command will create hash ids for remote EPCIS documents and will also append a new line character to a pre-hash text:
 ```
./openepcis-event-hash-generator-cli-0.9.1-SNAPSHOT-mac -e XML -a md2 -p -j \\n https://raw.githubusercontent.
com/gs1/EPCIS/master/XML/AssociationEvent/AssociationEventExamples.xml
```

6. Create hash IDs for EPCIS documents in the local directory using the following command, and add a new line character to the pre-hash string:
```
./openepcis-event-hash-generator-cli-0.9.1-SNAPSHOT-mac -e JSON -a sha3-384 -p -j \\n /Users/aravinda.
baligab/GitHub/openepcis-event-hash-generator-cli/jsonFile.json
```

7. Create hash Ids by providing EPCIS document as text:
```
./openepcis-event-hash-generator-cli-0.9.1-SNAPSHOT-mac -e XML -
```
press enter and then paste the EPCIS document.

### Building From Source:

You need to have at least Apache Maven Version 3.8 installed on your system to run the build.
The following commands must be executed from within the project root folder.

1. build and install maven artefacts
```
mvn clean install
```

2. To build a native executable, [GraalVM](https://www.graalvm.org/) is required 

build native executable
```
export JAVA_HOME=/path-to-graalvm-java17
export GRAALVM_HOME=/path-to-graalvm-java17
mvn clean package -Pnative
```

### References:
1. For more information on the Event Hash Generator, please refer to following GitHub repository:
https://github.com/openepcis/openepcis-event-hash-generator.


2. For more information on Event Hash Generation, ordering of elements, or canonicalization, please refer to the detailed Documentation by Ralph Tröger: https://github.com/RalphTro/epcis-event-hash-generator