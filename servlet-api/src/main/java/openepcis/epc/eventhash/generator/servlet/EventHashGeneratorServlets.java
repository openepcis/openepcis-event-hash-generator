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
package openepcis.epc.eventhash.generator.servlet;

import io.openepcis.constants.CBVVersion;
import io.openepcis.epc.eventhash.DocumentWrapperSupport;
import io.openepcis.epc.eventhash.EventHashGenerator;
import io.openepcis.model.rest.servlet.ServletSupport;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventHashGeneratorServlets {
    private static final String SHA_256 = "sha-256";

    @WebServlet(name = "EventHashGeneratorServlets.EPCISDocument", urlPatterns = "/api/generate/event-hash/document")
    public static final class EPCISDocument extends HttpServlet {

        @Inject
        ServletSupport servletSupport;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try {
                // List to store the parameters based on the user provided inputs.
                final List<String> hashParameters = new ArrayList<>();

                // Add provided CBV version else default to CBV 2.0.0
                final String cbvVersion = Optional.ofNullable(req.getParameter("cbvVersion")).orElse(CBVVersion.VERSION_2_0_0.getVersion());
                final CBVVersion targetCbvVersion = CBVVersion.of(cbvVersion);

                final EventHashGenerator eventHashGenerator = new EventHashGenerator(targetCbvVersion);

                // If Pre-Hash string is requested then add the prehash string to the List
                if (Boolean.parseBoolean(Optional.ofNullable(req.getParameter("prehash")).orElse("false"))) {
                    hashParameters.add("prehash");

                    // If user has requested for beautification for prehash string then add beautification.
                    if (Boolean.parseBoolean(Optional.ofNullable(req.getParameter("beautifyPreHash")).orElse("false"))) {
                        eventHashGenerator.prehashJoin("\\n");
                    } else {
                        eventHashGenerator.prehashJoin("");
                    }
                }

                // If user has provided fields to ignore during hash generation then add them
                final String ignoreFields = req.getParameter("ignoreFields");
                if (!StringUtils.isBlank(ignoreFields)) {
                    eventHashGenerator.excludeFieldsInPreHash(ignoreFields);
                }

                // Add the Hash Algorithm type to the List.
                final String hashAlgorithm = req.getParameter("hashAlgorithm");
                hashParameters.add(hashAlgorithm != null && !hashAlgorithm.isEmpty() ? hashAlgorithm : SHA_256);


                Optional<String> accept = servletSupport.accept(List.of(MediaType.APPLICATION_JSON, MediaType.WILDCARD), req, resp);
                if (accept.isEmpty()) {
                    return;
                }
                Optional<String> contentType = servletSupport.contentType(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML), accept.get(), req, resp);
                if (contentType.isEmpty()) {
                    return;
                }
                resp.setContentType(MediaType.APPLICATION_JSON);
                servletSupport.writeJson(resp, contentType.get().contains("application/xml")
                        ? eventHashGenerator.fromXml(req.getInputStream(), hashParameters.toArray(String[]::new))
                        : eventHashGenerator.fromJson(
                        req.getInputStream(), hashParameters.toArray(String[]::new)));
            } catch (Exception e) {
                final WebApplicationException webApplicationException =
                        WebApplicationException.class.isAssignableFrom(e.getClass())?(WebApplicationException)e:new WebApplicationException(e);
                servletSupport.writeException(webApplicationException, MediaType.APPLICATION_JSON, resp);
            }
        }
    }

    @WebServlet(name = "EventHashGeneratorServlets.EPCISEvents", urlPatterns = "/api/generate/event-hash/events")
    public static final class EPCISEvents extends HttpServlet {
        @Inject
        ServletSupport servletSupport;

        @Inject
        DocumentWrapperSupport documentWrapperSupport;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            try {
                // List to store the parameters based on the user provided inputs.
                final List<String> hashParameters = new ArrayList<>();

                // Add provided CBV version else default to CBV 2.0.0
                final String cbvVersion = Optional.ofNullable(req.getParameter("cbvVersion")).orElse(CBVVersion.VERSION_2_0_0.getVersion());
                final CBVVersion targetCbvVersion = CBVVersion.of(cbvVersion);

                final EventHashGenerator eventHashGenerator = new EventHashGenerator(targetCbvVersion);

                // If Pre-Hash string is requested then add the prehash string to the List
                if (Boolean.parseBoolean(Optional.ofNullable(req.getParameter("prehash")).orElse("false"))) {
                    hashParameters.add("prehash");

                    // If user has requested for beautification for prehash string then add beautification.
                    if (Boolean.parseBoolean(Optional.ofNullable(req.getParameter("beautifyPreHash")).orElse("false"))) {
                        eventHashGenerator.prehashJoin("\\n");
                    } else {
                        eventHashGenerator.prehashJoin("");
                    }
                }

                // If user has provided fields to ignore during hash generation then add them
                final String ignoreFields = req.getParameter("ignoreFields");
                if (!StringUtils.isBlank(ignoreFields)) {
                    eventHashGenerator.excludeFieldsInPreHash(ignoreFields);
                }

                // Add the Hash Algorithm type to the List.
                final String hashAlgorithm = req.getParameter("hashAlgorithm");
                hashParameters.add(hashAlgorithm != null && !hashAlgorithm.isEmpty() ? hashAlgorithm : SHA_256);


                Optional<String> accept = servletSupport.accept(List.of(MediaType.APPLICATION_JSON, MediaType.WILDCARD), req, resp);
                if (accept.isEmpty()) {
                    return;
                }
                Optional<String> contentType = servletSupport.contentType(List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML), accept.get(), req, resp);
                if (contentType.isEmpty()) {
                    return;
                }
                resp.setContentType(MediaType.APPLICATION_JSON);
                servletSupport.writeJson(resp, contentType.get().contains("application/xml")
                        ? eventHashGenerator.fromXml(req.getInputStream(), hashParameters.toArray(String[]::new))
                        : eventHashGenerator.fromJson(
                        documentWrapperSupport.generateJsonDocumentWrapper(req.getInputStream()), hashParameters.toArray(String[]::new)));
            } catch (Exception e) {
                final WebApplicationException webApplicationException =
                        WebApplicationException.class.isAssignableFrom(e.getClass())?(WebApplicationException)e:new WebApplicationException(e);
                servletSupport.writeException(webApplicationException, MediaType.APPLICATION_JSON, resp);
            }
        }
    }
}
