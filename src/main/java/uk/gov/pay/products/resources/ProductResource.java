package uk.gov.pay.products.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import io.dropwizard.jersey.PATCH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.products.model.Product;
import uk.gov.pay.products.model.ProductUsageStat;
import uk.gov.pay.products.service.ProductApiTokenManager;
import uk.gov.pay.products.service.ProductFactory;
import uk.gov.pay.products.validations.ProductRequestValidator;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;

import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.pay.logging.LoggingKeys.GATEWAY_ACCOUNT_ID;
import static uk.gov.pay.products.model.Product.FIELD_GATEWAY_ACCOUNT_ID;
import static uk.gov.pay.products.model.Product.FIELD_NAME;
import static uk.gov.pay.products.model.Product.FIELD_TYPE;

@Path("/v1/api")
public class ProductResource {
    private static final Logger logger = LoggerFactory.getLogger(ProductResource.class);

    private final ProductRequestValidator requestValidator;
    private final ProductFactory productFactory;
    private final ProductApiTokenManager productApiTokenManager;

    @Inject
    public ProductResource(ProductRequestValidator requestValidator,
                           ProductFactory productFactory,
                           ProductApiTokenManager productApiTokenManager) {
        this.requestValidator = requestValidator;
        this.productFactory = productFactory;
        this.productApiTokenManager = productApiTokenManager;
    }

    @POST
    @Path("/products")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response createProduct(JsonNode payload) {
        logger.info(
                "Create Service POST request",
                kv(GATEWAY_ACCOUNT_ID, payload.get(FIELD_GATEWAY_ACCOUNT_ID)),
                kv(FIELD_TYPE, payload.get(FIELD_TYPE)),
                kv(FIELD_NAME, payload.get(FIELD_NAME))
        );
        return requestValidator.validateCreateRequest(payload)
                .map(errors -> Response.status(Status.BAD_REQUEST).entity(errors).build())
                .orElseGet(() -> {
                    try {
                        Product product = productFactory.productCreator().doCreate(Product.from(payload));
                        return Response.status(Status.CREATED).entity(product).build();
                    } catch (javax.persistence.RollbackException ex) {
                        logger.info("Conflict error while persisting product, product path already exists. " + ex.getLocalizedMessage());
                        return Response.status(Status.CONFLICT).build();
                    }
                });
    }

    @GET
    @Path("/products/{productExternalId}")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response findProductByExternalId(@PathParam("productExternalId") String productExternalId) {
        logger.info("Find a product with externalId - [ {} ]", productExternalId);
        return productFactory.productFinder().findByExternalId(productExternalId)
                .map(product ->
                        Response.status(OK).entity(product).build())
                .orElseGet(() ->
                        Response.status(NOT_FOUND).build());
    }

    @GET
    @Path("/gateway-account/{gatewayAccountId}/products/{productExternalId}")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response findProductByGatewayAccountIdAndExternalId(@PathParam("gatewayAccountId") Integer gatewayAccountId, @PathParam("productExternalId") String productExternalId) {
        logger.info("Find a product with externalId - [ {} ]", productExternalId);
        return productFactory.productFinder().findByGatewayAccountIdAndExternalId(gatewayAccountId, productExternalId)
                .map(product ->
                        Response.status(OK).entity(product).build())
                .orElseGet(() ->
                        Response.status(NOT_FOUND).build());
    }

    @Deprecated
    @PATCH
    @Path("/products/{productExternalId}/disable")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response disableProductByExternalId(@PathParam("productExternalId") String productExternalId) {
        logger.info("Disabling a product with externalId - [ {} ]", productExternalId);
        return productFactory.productFinder().disableByExternalId(productExternalId)
                .map(product -> Response.status(NO_CONTENT).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    @DELETE
    @Path("/products/{productExternalId}")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response deleteProductByExternalId(@PathParam("productExternalId") String productExternalId) {
        logger.info("Deleting a product with externalId - [ {} ]", productExternalId);
        Boolean success = productFactory.productFinder().deleteByExternalId(productExternalId);
        return success ? Response.status(NO_CONTENT).build() : Response.status(NOT_FOUND).build();
    }

    @Deprecated
    @PATCH
    @Path("/gateway-account/{gatewayAccountId}/products/{productExternalId}/disable")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response disableProductByGatewayAccountIdAndExternalId(@PathParam("gatewayAccountId") Integer gatewayAccountId, @PathParam("productExternalId") String productExternalId) {
        logger.info("Disabling a product with externalId - [ {} ]", productExternalId);
        return productFactory.productFinder().disableByGatewayAccountIdAndExternalId(gatewayAccountId, productExternalId)
                .map(product -> Response.status(NO_CONTENT).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    @PATCH
    @Path("/gateway-account/{gatewayAccountId}/products/{productExternalId}")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response updateProduct(@PathParam("gatewayAccountId") Integer gatewayAccountId, @PathParam("productExternalId") String productExternalId, JsonNode payload) {
        logger.info(
                "Updating a product with externalId - [ {} ]",
                productExternalId,
                kv("product_external_id", productExternalId)
        );

        return requestValidator.validateUpdateRequest(payload)
                .map(errors -> Response.status(Status.BAD_REQUEST).entity(errors).build())
                .orElseGet(() ->
                    productFactory.productCreator().doUpdateByGatewayAccountId(gatewayAccountId, productExternalId, Product.from(payload))
                            .map(product -> Response.status(OK).entity(product).build())
                            .orElseGet(() -> Response.status(NOT_FOUND).build()));
    }

    @DELETE
    @Path("/gateway-account/{gatewayAccountId}/products/{productExternalId}")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response deleteProductByGatewayAccountIdAndExternalId(@PathParam("gatewayAccountId") Integer gatewayAccountId, @PathParam("productExternalId") String productExternalId) {
        logger.info("Deleting a product with externalId - [ {} ]", productExternalId);
        Boolean success = productFactory.productFinder().deleteByGatewayAccountIdAndExternalId(gatewayAccountId, productExternalId);
        return success ? Response.status(NO_CONTENT).build() : Response.status(NOT_FOUND).build();
    }

    @GET
    @Path("/gateway-account/{gatewayAccountId}/products")
    @Produces(APPLICATION_JSON)
    public Response findProductsByGatewayAccountId(@PathParam("gatewayAccountId") Integer gatewayAccountId) {
        logger.info("Searching for products with gatewayAccountId - [ {} ]", gatewayAccountId);
        List<Product> products = productFactory.productFinder().findByGatewayAccountId(gatewayAccountId);
        return Response.status(OK).entity(products).build();
    }

    @GET
    @Path("/products")
    @Produces(APPLICATION_JSON)
    public Response findProductByProductPath(
            @QueryParam("serviceNamePath") String serviceNamePath,
            @QueryParam("productNamePath") String productNamePath) {
        logger.info(format("Searching for product with product path - [ serviceNamePath=%s productNamePath=%s ]", serviceNamePath, productNamePath));
        return productFactory.productFinder().findByProductPath(serviceNamePath, productNamePath)
                .map(product -> Response.status(OK).entity(product).build())
                .orElseGet(() -> Response.status(NOT_FOUND).build());
    }

    @GET
    @Path("/stats/products")
    @Produces(APPLICATION_JSON)
    public Response findProductsAndStats(@QueryParam("gatewayAccountId") Integer gatewayAccountId) {
        logger.info(
                format("Listing usage stats on all non-prototype payment links for gateway account [%s=%s]", GATEWAY_ACCOUNT_ID, gatewayAccountId),
                kv(GATEWAY_ACCOUNT_ID, gatewayAccountId)
        );
        List<ProductUsageStat> usageStats = productFactory.productFinder().findProductsAndUsage(gatewayAccountId);
        return Response.status(OK).entity(usageStats).build();
    }

    @POST
    @Path("/products/{productExternalId}/regenerate-api-token")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response regenerateProductApiToken(@PathParam("productExternalId") String productExternalId) {
        return productFactory.productFinder().findByExternalId(productExternalId).map((product) -> {
            productApiTokenManager.replaceApiTokenForAProduct(product, productApiTokenManager.getNewApiTokenFromPublicAuth(product));
            return Response.ok().build();
        }).orElseGet(() -> Response.status(NOT_FOUND).build());
    }
}
