package ai.yottaflex.attribution.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler=new ApiExceptionHandler();

    @Test
    void invalidBusinessRequestReturnsBadRequestEnvelope() {
        var response=handler.handleIllegalArgument(
            new IllegalArgumentException("Opportunity count cannot exceed contact count."));
        assertEquals(HttpStatus.BAD_REQUEST,response.getStatusCode());
        assertEquals("INVALID_REQUEST",response.getBody().get("code"));
        assertEquals("Opportunity count cannot exceed contact count.",response.getBody().get("message"));
    }

    @Test
    void malformedJsonReturnsStableErrorCode() {
        var exception=new org.springframework.http.converter.HttpMessageNotReadableException("invalid json");
        var response=handler.handleMalformedJson(exception);
        assertEquals(HttpStatus.BAD_REQUEST,response.getStatusCode());
        assertEquals("MALFORMED_JSON",response.getBody().get("code"));
    }
}
