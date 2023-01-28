package io.openecpis.epc.eventhash.azure;

import io.openepcis.epc.eventhash.EventHashGenerator;
import io.smallrye.mutiny.Multi;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

public abstract class AbstractHashIdServlet extends HttpServlet {

  @Inject EventHashGenerator eventHashGenerator;

  protected final List<String> getHashParameters(
      Boolean prehash, Boolean beautifyPreHash, List<String> hashAlgorithms) {
    // List to store the parameters based on the user provided inputs.
    final List<String> hashParameters = new ArrayList<>();
    // If Pre-Hash string is requested then add the prehash string to the List
    if (Boolean.TRUE.equals(prehash)) {
      hashParameters.add("prehash");

      // If user has requested for beautification for prehash string then add beautification.
      if (Boolean.TRUE.equals(beautifyPreHash)) {
        eventHashGenerator.prehashJoin("\\n");
      } else {
        eventHashGenerator.prehashJoin("");
      }
    }

    // Add the Hash Algorithm type to the List.
    hashParameters.addAll(
        hashAlgorithms != null && !hashAlgorithms.isEmpty() ? hashAlgorithms : List.of("sha-256"));
    return hashParameters;
  }

  protected final void writeResult(HttpServletResponse resp, Multi<Map<String, String>> result)
      throws IOException {
    resp.setStatus(200);
    resp.addHeader("Content-Type", MediaType.APPLICATION_JSON);
    PrintWriter writer = new PrintWriter(resp.getWriter());
    writer.println("[");
    final AtomicBoolean start = new AtomicBoolean(true);
    result
        .subscribe()
        .with(
            r -> {
              r.entrySet()
                  .forEach(
                      entry -> {
                        if (!start.getAndSet(false)) {
                          writer.println(",");
                        }
                        writer.print(
                            " { \"" + entry.getKey() + "\": \"" + entry.getValue() + "\" }");
                      });
            });
    writer.println();
    writer.println("]");
  }
}
