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
package io.openepcis.epc.eventhash.azure;

import io.smallrye.mutiny.Multi;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "EPCSISDocumentHashIdServlet", urlPatterns = "/api/generate/event-hash/document")
public class EPCSISDocumentHashIdServlet extends AbstractHashIdServlet {

  @Override
  protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException {

    final Boolean prehash = Boolean.valueOf(req.getParameter("prehash"));
    final Boolean beautifyPreHash = Boolean.valueOf(req.getParameter("beautifyPreHash"));
    final String ignoreFields = req.getParameter("ignoreFields");
    if (!StringUtils.isBlank(ignoreFields)) {
      eventHashGenerator.excludeFieldsInPreHash(ignoreFields);
    }

    final List<String> hashAlgorithms =
        req.getParameter("hashAlgorithm") != null
            ? List.of(req.getParameterValues("hashAlgorithm"))
            : List.of("sha-256");
    final String contentType = req.getContentType();

    final List<String> hashParameters = getHashParameters(prehash, beautifyPreHash, hashAlgorithms);
    Multi<Map<String, String>> result =
        req.getContentType().equals("application/xml")
            ? eventHashGenerator.fromXml(
                req.getInputStream(), hashParameters.toArray(String[]::new))
            : eventHashGenerator.fromJson(
                req.getInputStream(), hashParameters.toArray(String[]::new));
    writeResult(resp, result);
  }
}
