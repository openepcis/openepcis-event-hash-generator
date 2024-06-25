/*
 * Copyright 2022-2024 benelog GmbH & Co. KG
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
package io.openepcis.eventhash.main;

import io.openepcis.eventhash.EventHashGenerator;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

  private static String PREHASH_JOIN = null;

  private static final ExecutorService EXECUTOR_SERVICE = Executors.newWorkStealingPool();

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
        PREHASH,
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
        """
                    String used to join the pre hash string.
                    Defaults to empty string as specified.
                    Values like "\\n" might be useful for debugging.""");

    // ***Parsing Stage***
    // Create a parser
    parser = new GnuParser();
  }

  public static void main(String[] args) throws ParseException, IOException, InterruptedException {
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
      PREHASH_JOIN = preHashJoin;
    }

    // check if read from stdin is requested
    if (cmd.getArgs().length == 1 && "-".equals(cmd.getArgs()[0])) {
      if (batchMode) {
        System.out.println("batch mode not supported when reading from stdin");
        System.exit(1);
      }
      final String type = cmd.hasOption("e") ? cmd.getOptionValue("e").toLowerCase() : TYPE_JSON;
      runHashGenerator(type, System.in, hashAlgorithms, createConsumer(Optional.empty()));
    } else {
      for (final String path : cmd.getArgs()) {
        process(path, cmd, batchMode, hashAlgorithms);
      }
    }

    // shutdown executor and wait for a ridiculously long time,
    // because we don't know how many files a user may want to be processing
    EXECUTOR_SERVICE.shutdown();
    EXECUTOR_SERVICE.awaitTermination(4096, TimeUnit.DAYS);

    // After completing the execution terminate the program for next execution.
    System.exit(0);
  }

  private static void process(String path, CommandLine cmd, boolean batchMode, String[] hashAlgorithms) throws IOException {
    final String type = !cmd.hasOption("e") && path.toLowerCase().endsWith(".xml")?TYPE_XML:cmd.hasOption("e") ? cmd.getOptionValue("e").toLowerCase() : TYPE_JSON;

    // Check if the path contains the http/https if so then make remote request call
    if (path.matches("^(https?)://.*$")) {
      EXECUTOR_SERVICE.execute(() -> {
        HttpEntity httpEntity = null;
        final Optional<Map<String, PrintStream>> printStreamMap =
                createPrintWriterMap(path, batchMode, hashAlgorithms);
        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
          httpEntity = httpClient.execute(new HttpGet(path)).getEntity();
          runHashGenerator(
                  type, httpEntity.getContent(), hashAlgorithms, createConsumer(printStreamMap));
        } catch (IOException e) {
          throw new RuntimeException(e);
        } finally {
          if (httpEntity != null) {
            EntityUtils.consumeQuietly(httpEntity);
          }
          if (printStreamMap.isPresent()) {
            for (PrintStream printStream : printStreamMap.get().values()) {
              printStream.flush();
              printStream.close();
            }
          }
        }
      });
    } else {
      for (final File f : locateFiles(path)) {
          if (f.isFile()) {
            EXECUTOR_SERVICE.execute(() -> {
              final Optional<Map<String, PrintStream>> printStreamMap =
                      createPrintWriterMap(f.getPath(), batchMode, hashAlgorithms);
              try {
                runHashGenerator(
                        type, new FileInputStream(f), hashAlgorithms, createConsumer(printStreamMap));
              } catch (IOException e) {
                throw new RuntimeException(e);
              } finally {
                if (printStreamMap.isPresent()) {
                  for (PrintStream printStream : printStreamMap.get().values()) {
                    printStream.flush();
                    printStream.close();
                  }
                }
              }
            });
          }
      }
    }
  }

  private static Optional<Map<String, PrintStream>> createPrintWriterMap(
      final String path, boolean batchMode, String[] hashAlgorithms) {
    try {
      if (batchMode) {
        final File hashesFile =
            path.matches("^(https?)://.*$")
                ? new File(path.substring(path.lastIndexOf("/") + 1).concat(HASHES_SUFFIX))
                : new File(path.replaceAll("\\.[^.]*$", HASHES_SUFFIX));
        final Map<String, PrintStream> printStreamMap = new HashMap<>();
        printStreamMap.put(HASHES_SUFFIX, new PrintStream(hashesFile));
        if (Arrays.asList(hashAlgorithms).contains(PREHASH)) {
          final File prehashesFile =
                  path.matches("^(https?)://.*$")
                          ? new File(path.substring(path.lastIndexOf("/") + 1).concat(PREHASHES_SUFFIX))
                          : new File(path.replaceAll("\\.[^.]*$", PREHASHES_SUFFIX));
          printStreamMap.put(PREHASHES_SUFFIX, new PrintStream(prehashesFile));
        }
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
  private static void runHashGenerator(
      final String type,
      final InputStream inputStream,
      final String[] hashAlgorithms,
      Consumer<? super Map<String, String>> consumer) {
    try {
      if (TYPE_XML.equals(type.toLowerCase())) {
        xmlDocumentHashIdGenerator(inputStream, hashAlgorithms, consumer);
      } else {
        jsonDocumentHashIdGenerator(inputStream, hashAlgorithms, consumer);
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
      createEventHashGenerator()
              .fromXml(xmlStream, hashAlgorithms)
              .subscribe()
              .with(consumer, HashGenerator::fail);
  }

  // Private method to generate Hash Ids for the EPCIS events in JSON/JSON-LD format.
  private static void jsonDocumentHashIdGenerator(
      final InputStream jsonStream,
      final String[] hashAlgorithms,
      Consumer<? super Map<String, String>> consumer)
      throws IOException {
        createEventHashGenerator()
                .fromJson(jsonStream, hashAlgorithms)
                .subscribe()
                .with(consumer, HashGenerator::fail);
  }

  private static EventHashGenerator createEventHashGenerator() {
    final EventHashGenerator eventHashGenerator = new EventHashGenerator();
    if (PREHASH_JOIN != null) {
      eventHashGenerator.prehashJoin(PREHASH_JOIN);
    }
    return eventHashGenerator;
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
