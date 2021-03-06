package uk.gov.pay.products.exception.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.products.exception.PaymentCreatorNotFoundException;
import uk.gov.pay.products.util.Errors;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static java.lang.String.format;

public class PaymentCreatorNotFoundExceptionMapper implements ExceptionMapper<PaymentCreatorNotFoundException> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Response toResponse(PaymentCreatorNotFoundException exception) {
        logger.error("PaymentCreatorNotFoundException thrown.", exception);
        return Response
                .status(Response.Status.NOT_FOUND)
                .entity(Errors.from(format("Product with product id %s not found.", exception.getProductExternalId())))
                .build();
    }
}
