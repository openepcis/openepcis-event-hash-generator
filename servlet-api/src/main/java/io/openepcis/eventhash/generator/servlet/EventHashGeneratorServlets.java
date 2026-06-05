/*
 * Copyright 2022-2026 benelog GmbH & Co. KG
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
package io.openepcis.eventhash.generator.servlet;

import io.openepcis.constants.CBVVersion;
import io.openepcis.eventhash.DocumentWrapperSupport;
import io.openepcis.eventhash.EventHashGenerator;
import io.openepcis.model.rest.servlet.ServletSupport;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventHashGeneratorServlets {
    private static final String SHA_256 = "sha-256";

    // Functional interface so each endpoint can plug in its own JSON-input strategy (identity or document-wrap).
    @FunctionalInterface
    private interface JsonInputTransformer {
        InputStream transform(InputStream in) throws IOException;
    }

    // Bundles the per-request EventHashGenerator with the algorithm list to pass to fromXml/fromJson.
    private record HashRequest(EventHashGenerator generator, String[] hashParameters) {
    }

    // Parse common query parameters (cbvVersion, prehash, beautifyPreHash, hashAlgorithm) into a HashRequest.
    private static HashRequest parseRequest(final HttpServletRequest req) {
        // Read CBV version (default 2.0.0) and build the generator
        final String cbvVersion = Optional.ofNullable(req.getParameter("cbvVersion")).orElse(CBVVersion.VERSION_2_0_0.getVersion());
        final EventHashGenerator generator = new EventHashGenerator(CBVVersion.of(cbvVersion));

        // Collect the algorithm names that will be returned for each event
        final List<String> hashParameters = new ArrayList<>();

        // If prehash output is requested,
        if (Boolean.parseBoolean(Optional.ofNullable(req.getParameter("prehash")).orElse("false"))) {
            hashParameters.add("prehash");
            final boolean beautify = Boolean.parseBoolean(Optional.ofNullable(req.getParameter("beautifyPreHash")).orElse("false"));
            generator.prehashJoin(beautify ? "\\n" : "");
        }

        // Append the actual hash algorithm (defaults to sha-256 when not provided)
        final String hashAlgorithm = req.getParameter("hashAlgorithm");
        hashParameters.add(hashAlgorithm != null && !hashAlgorithm.isEmpty() ? hashAlgorithm : SHA_256);

        return new HashRequest(generator, hashParameters.toArray(String[]::new));
    }

    // Shared pipeline: negotiate media types, build request state, dispatch to XML or JSON path, write JSON response.
    private static void dispatch(final HttpServletRequest req,
                                 final HttpServletResponse resp,
                                 final ServletSupport servletSupport,
                                 final List<String> allowedContentTypes,
                                 final JsonInputTransformer jsonInputTransformer) throws IOException {
        try {
            // Negotiate the media type with callers accept request header
            final Optional<String> accept = servletSupport.accept(List.of(MediaType.APPLICATION_JSON, MediaType.WILDCARD), req, resp);
            if (accept.isEmpty()) return;

            // Validate the request's Content-Type against the endpoint's allow-list
            final Optional<String> contentType = servletSupport.contentType(allowedContentTypes, accept.get(), req, resp);
            if (contentType.isEmpty()) return;

            // Parse params once and reuse for both XML and JSON branches
            final HashRequest hashRequest = parseRequest(req);

            resp.setContentType(MediaType.APPLICATION_JSON);

            // Branch on incoming format; JSON path uses the endpoint-specific input transformer
            servletSupport.writeJson(resp,
                    contentType.get().contains("application/xml")
                            ? hashRequest.generator.fromXml(req.getInputStream(), hashRequest.hashParameters)
                            : hashRequest.generator.fromJson(jsonInputTransformer.transform(req.getInputStream()), hashRequest.hashParameters));

        } catch (Exception e) {
            // Wrap non-WebApplicationException causes so the JAX-RS error path serializes them correctly
            final WebApplicationException webEx = WebApplicationException.class.isAssignableFrom(e.getClass()) ? (WebApplicationException) e : new WebApplicationException(e);
            servletSupport.writeException(webEx, MediaType.APPLICATION_JSON, resp);
        }
    }


    @WebServlet(name = "EventHashGeneratorServlets.EPCISDocument", urlPatterns = "/api/generate/event-hash/document")
    public static final class EPCISDocument extends HttpServlet {
        @Inject
        ServletSupport servletSupport;

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            // Document endpoint: caller sends a complete EPCIS document; pass JSON stream straight through.
            dispatch(req, resp, servletSupport, List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML), in -> in);
        }
    }

    @WebServlet(name = "EventHashGeneratorServlets.EPCISEvents", urlPatterns = "/api/generate/event-hash/events")
    public static final class EPCISEvents extends HttpServlet {

        @Inject
        ServletSupport servletSupport;
        @Inject
        DocumentWrapperSupport documentWrapperSupport;

        @Override
        protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
            // Events endpoint: caller sends bare events; wrap them in a synthetic document for the JSON parser.
            dispatch(req, resp, servletSupport, List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML), documentWrapperSupport::generateJsonDocumentWrapper);
        }
    }
}
