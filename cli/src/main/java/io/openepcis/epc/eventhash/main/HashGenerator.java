/*
 * Copyright 2022 benelog GmbH & Co. KG
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package io.openepcis.epc.eventhash.main;

import io.openepcis.epc.eventhash.EventHashGenerator;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.commons.cli.*;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class HashGenerator {

  private static final Options options;
  private static final CommandLineParser parser;

  private static final String TYPE_XML = "xml";
  private static final String TYPE_JSON = "json";
  private static final String PREHASH = "prehash";
  private static final String PREHASHES_SUFFIX = ".prehashes";
  private static final String HASHES_SUFFIX = ".hashes";

  static {

    // ***Definition Stage***
    // create Options object
    options = new Options();

    // Parameter "-h" to obtain Help options in the utility.
    options.addOption("h", "help", false, "All the available options.");

    // Parameter "-a" to obtain Algorithm (sha-256, sha-384) based on which respective Hash Ids need
    // to be generated.
    options.addOption(
        "a",
        "algorithm",
        true,
        "Type of Hash Algorithm required: sha-1, sha-224, sha-256, sha-384, sha-512, sha3-224, sha3-256, sha3-384, sha3-512, md2, md5. - default: sha-256");

    // Parameter "-e" to obtain direct Text which contains the EPCIS events in either XML/JSON
    // format.
    options.addOption(
        "e",
        "enforce-format",
        true,
        "Enforce parsing the given files all as JSON or XML if given."
            + "\n Defaults to guessing the format from the file ending.");

    // Parameter "-p" to obtain direct Text which contains the EPCIS events in either XML/JSON
    // format.
    options.addOption(
        "p",
        "prehash",
        false,
        "If given, also output the prehash string to stdout. Output to a .prehashes file, if combined with -b.");

    // Parameter "-b" batch mode
    options.addOption(
        "b",
        "batch",
        false,
        "If given, write the new line separated list of hashes for each input file into \n a sibling output file "
            + "with the same name + '.hashes' instead of stdout.");

    // Parameter "-j" join string for pre-hashes
    options.addOption(
        "j",
        "join",
        true,
        "String used to join the pre hash string.\n"
            + "Defaults to empty string as specified.\nValues like \"\\n\" might be useful for debugging.");

    // ***Parsing Stage***
    // Create a parser
    parser = new GnuParser();
  }

  public static void main(String[] args) throws ParseException, IOException {
    // parse the options passed as command line arguments
    final CommandLine cmd = parser.parse(options, args);

    // If user has provided -h or empty arguments then display all the available option with their
    // description and terminate the application.
    if ((cmd.getOptions().length == 0 && cmd.getArgList().isEmpty()) || cmd.hasOption("h")) {
      // Format all options to display as help section
      final HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(
          "OpenEPCIS Event Hash Generator Utility: [options] file.. url.., - to read from stdin ",
          options);
      System.exit(1);
    } else if (cmd.getArgList().isEmpty()) {
      // Check if either of the p or i option is present if not then terminate the application.
      System.out.println(
          "OpenEPCIS Event Hash Generator Utility Execution Failed: Please provide the EPCIS events file/content!");
      System.exit(1);
    }

    String type = cmd.hasOption("e") ? cmd.getOptionValue("e").toLowerCase() : TYPE_JSON;
    String[] hashAlgorithms =
        cmd.hasOption("a") ? cmd.getOptionValues("a") : new String[] {"sha-256"};
    final boolean batchMode = cmd.hasOption("b");

    if (cmd.hasOption("p")) {
      final List<String> hashStrings = new ArrayList<>();
      hashStrings.add(PREHASH);
      hashStrings.addAll(Arrays.asList(hashAlgorithms));
      hashAlgorithms = hashStrings.toArray(new String[0]);
    }

    if (cmd.hasOption("j")) {
      final String preHashJoin = cmd.getOptionValue("j");
      EventHashGenerator.prehashJoin(preHashJoin);
    }

    // check if read from stdin is requested
    if (cmd.getArgs().length == 1 && "-".equals(cmd.getArgs()[0])) {
      if (batchMode) {
        System.out.println("batch mode not supported when reading from stdin");
        System.exit(1);
      }
      typeDifferentiator(type, System.in, hashAlgorithms, createConsumer(Optional.empty()));

    } else {
      // ***Interrogation Stage***
      for (final String path : cmd.getArgs()) {
        if (!cmd.hasOption("e")) {
          if (path.toLowerCase().endsWith(".xml")) {
            type = TYPE_XML;
          }
        }
        // Check if the path contains the http/https if so then make remote request call
        if (path.matches("^(https?)://.*$")) {
          HttpEntity httpEntity = null;
          try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
            httpEntity = httpClient.execute(new HttpGet(path)).getEntity();
            final Optional<Map<String, PrintStream>> printStreamMap =
                createPrintWriterMap(path, batchMode);
            typeDifferentiator(
                type, httpEntity.getContent(), hashAlgorithms, createConsumer(printStreamMap));
            if (printStreamMap.isPresent()) {
              for (PrintStream printStream : printStreamMap.get().values()) {
                printStream.flush();
                printStream.close();
              }
            }
          } finally {
            if (httpEntity != null) {
              EntityUtils.consumeQuietly(httpEntity);
            }
          }
        } else {
          for (final File f : locateFiles(path)) {
            final Optional<Map<String, PrintStream>> printStreamMap =
                createPrintWriterMap(path, batchMode);
            typeDifferentiator(
                type, new FileInputStream(f), hashAlgorithms, createConsumer(printStreamMap));
            if (printStreamMap.isPresent()) {
              for (PrintStream printStream : printStreamMap.get().values()) {
                printStream.flush();
                printStream.close();
              }
            }
          }
        }
      }
    }

    // After completing the execution terminate the program for next execution.
    System.exit(0);
  }

  private static Optional<Map<String, PrintStream>> createPrintWriterMap(
      final String path, boolean batchMode) {
    try {
      if (batchMode) {
        final File hashesFile =
            path.matches("^(https?)://.*$")
                ? new File(path.substring(path.lastIndexOf("/") + 1).concat(HASHES_SUFFIX))
                : new File(path.concat(HASHES_SUFFIX));
        final File prehashesFile =
            path.matches("^(https?)://.*$")
                ? new File(path.substring(path.lastIndexOf("/") + 1).concat(PREHASHES_SUFFIX))
                : new File(path.concat(PREHASHES_SUFFIX));
        final Map<String, PrintStream> printStreamMap = new HashMap<>();
        printStreamMap.put(HASHES_SUFFIX, new PrintStream(hashesFile));
        printStreamMap.put(PREHASHES_SUFFIX, new PrintStream(prehashesFile));
        return Optional.of(printStreamMap);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return Optional.empty();
  }

  private static Consumer<? super Map<String, String>> createConsumer(
      final Optional<Map<String, PrintStream>> output) throws RuntimeException {

    return new Consumer<Map<String, String>>() {
      @Override
      public void accept(Map<String, String> map) {
        try {
          if (map.containsKey(PREHASH)) {
            if (output.isEmpty()) {
              System.out.println(map.get(PREHASH));
            } else {
              output.get().get(PREHASHES_SUFFIX).println(map.get(PREHASH));
            }
          }
          for (final Map.Entry<String, String> entry : map.entrySet()) {
            if (!PREHASH.equals(entry.getKey().toLowerCase())) {
              if (output.isEmpty()) {
                System.out.println(entry.getValue());
              } else {
                output.get().get(HASHES_SUFFIX).println(entry.getValue());
              }
            }
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  // Private method to validate the JSON if JSON then call the respective JSON method else call XML
  // method.
  private static void typeDifferentiator(
      final String type,
      final InputStream inputStream,
      final String[] hashAlgorithms,
      Consumer<? super Map<String, String>> consumer) {
    try {
      switch (type.toLowerCase()) {
        case TYPE_XML -> {
          xmlDocumentHashIdGenerator(inputStream, hashAlgorithms, consumer);
        }
        default -> {
          jsonDocumentHashIdGenerator(inputStream, hashAlgorithms, consumer);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  // Private method to generate Hash IDs for the EPCIS in XML format.
  private static void xmlDocumentHashIdGenerator(
      final InputStream xmlStream,
      final String[] hashAlgorithms,
      Consumer<? super Map<String, String>> consumer) {
    EventHashGenerator.fromXml(xmlStream, hashAlgorithms)
        .subscribe()
        .with(consumer, HashGenerator::fail);
  }

  // Private method to generate Hash Ids for the EPCIS events in JSON/JSON-LD format.
  private static void jsonDocumentHashIdGenerator(
      final InputStream jsonStream,
      final String[] hashAlgorithms,
      Consumer<? super Map<String, String>> consumer)
      throws IOException {
    EventHashGenerator.fromJson(jsonStream, hashAlgorithms)
        .subscribe()
        .with(consumer, HashGenerator::fail);
  }

  private static void fail(Throwable failure) {
    System.err.println("Failed with error : " + failure.getMessage());
    failure.printStackTrace(System.err);
    System.exit(1);
  }

  private static List<File> locateFiles(final String path) {
    final File f = new File(path);
    if (!f.exists()) {
      final String dir =
          path.indexOf('\\') == -1
              ? path.substring(0, path.lastIndexOf("/"))
              : path.substring(0, path.lastIndexOf("\\"));
      final File d = new File(dir);
      if (d.isDirectory()) {
        final String filter =
            path.indexOf('\\') == -1
                ? path.substring(path.lastIndexOf("/") + 1)
                : path.substring(path.lastIndexOf("\\") + 1);
        return Stream.of(d.list(new WildcardFileFilter(filter))).map(File::new).toList();
      }
    }
    return List.of(f);
  }
}
