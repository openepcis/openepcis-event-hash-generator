package openepcis.epc.eventhash.generator.exception;

import io.openepcis.model.rest.ProblemResponseBody;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.RestResponse;
import org.xml.sax.SAXParseException;

@Provider
public class SaxParseExceptionMapper implements ExceptionMapper<SAXParseException> {

    @Override
    public Response toResponse(SAXParseException exception) {
        return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
            .entity(ProblemResponseBody.fromException(exception, RestResponse.Status.UNSUPPORTED_MEDIA_TYPE))
                .build();
    }
}
