package io.openepcis.epc.eventhash;

import static org.junit.Assert.assertEquals;

import io.smallrye.mutiny.Multi;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.ParallelComputer;
import org.junit.runner.JUnitCore;

public class ParallelExecutionTests {

  // Execute Tests Parallel
  @Test
  public void ParallelTest() {
    Class[] cls = {ParallelExecutionXmlTests.class, ParallelExecutionJsonTests.class};

    // Parallel among classes
    JUnitCore.runClasses(ParallelComputer.classes(), cls);

    // Parallel among methods in a class
    JUnitCore.runClasses(ParallelComputer.methods(), cls);

    // Parallel all methods in all classes
    JUnitCore.runClasses(new ParallelComputer(true, true), cls);
  }

  // Test to display the Pre-Hash String for XML events
  public static class ParallelExecutionXmlTests {
    @Test
    public void displayXmlPreHashTest() {
      // For event in XML format display the pre-hash string
      final InputStream xmlStream = getClass().getResourceAsStream("/PreHashEvent.xml");

      final String[] hashParameters = {"prehash", "sha3-512"};

      final Multi<Map<String, String>> xmlHashIds =
          EventHashGenerator.fromXml(xmlStream, hashParameters);

      // Confirm there are 3 hashids and prehash in the results.
      assertEquals(3, xmlHashIds.subscribe().asStream().toList().size());

      /*System.out.println("XML Document");
      xmlHashIds.subscribe().asStream().forEach(entrySet ->{
          entrySet.forEach((key,value)->{
              System.out.println(key + ":" + value);
          });
          System.out.println();
      });*/
    }
  }

  // Test to display the Pre-Hash String for JSON events
  public static class ParallelExecutionJsonTests {
    @Test
    public void displayJsonPreHashTest() throws IOException {
      // For event JSON in format display the pre-hash string
      final InputStream jsonStream = getClass().getResourceAsStream("/PreHashEvent.json");

      final String[] hashParameters = {"prehash", "sha3-384"};

      EventHashGenerator.prehashJoin("\\n");
      final Multi<Map<String, String>> jsonHashIds =
          EventHashGenerator.fromJson(jsonStream, hashParameters);

      // Confirm there are 2 hashids and pre-hash in the results.
      assertEquals(2, jsonHashIds.subscribe().asStream().toList().size());

      /*System.out.println("JSON Document");
      jsonHashIds.subscribe().asStream().forEach(entrySet ->{
          entrySet.entrySet().forEach(entry->{
              System.out.println(entry.getKey() + ":" + entry.getValue());
          });
          System.out.println();
      });*/
    }
  }
}
