package openepcis.epc.eventhash.generator.exception;

import com.fasterxml.jackson.core.JsonParseException;
import io.openepcis.model.rest.ProblemResponseBody;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.RestResponse;

@Provider
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    @Override
    public Response toResponse(JsonParseException exception) {
        return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                .entity(ProblemResponseBody.fromException(exception, RestResponse.Status.UNSUPPORTED_MEDIA_TYPE))
                .build();
    }
}
