package uk.gov.pay.products.validations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.math.NumberUtils.isDigits;

public class RequestValidations {

    static final Long MAX_PRICE = 10000000L;

    Optional<List<String>> checkIsNumeric(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, isNotNumeric(), fieldNames, "Field [%s] must be a number");
    }
    
    Optional<List<String>> checkIsString(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, isNotString(), fieldNames, "Field [%s] must be a string");
    }
    
    Optional<List<String>> checkIsUrl(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, isNotUrl(), fieldNames, "Field [%s] must be a https url");
    }

    Optional<List<String>> checkIfExistsOrEmpty(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, notExistAndNotEmpty(), fieldNames, "Field [%s] is required");
    }

    Optional<List<String>> checkIsBelowMaxAmount(JsonNode payload, String... fieldNames) {
        return applyCheck(payload, isBelowMax(), fieldNames, "Field [%s] must be a number below " + MAX_PRICE);
    }

    Optional<List<String>> checkIsValidEnumValue(JsonNode payload, EnumSet<?> enumSet, String field) {
        String value = payload.get(field).asText();
        if (enumSet.stream().noneMatch(constant -> constant.toString().equals(value))) {
            return Optional.of(singletonList(format("Field [%s] must be one of %s", field, enumSet)));
        }
        return Optional.empty();
    }

    private Optional<List<String>> applyCheck(JsonNode payload, Function<JsonNode, Boolean> check, String[] fieldNames, String errorMessage) {
        List<String> errors = newArrayList();
        for (String fieldName : fieldNames) {
            if (check.apply(payload.get(fieldName))) {
                errors.add(format(errorMessage, fieldName));
            }
        }
        return errors.size() > 0 ? Optional.of(errors) : Optional.empty();
    }

    private Function<JsonNode, Boolean> notExistAndNotEmpty() {
        return (jsonElement) -> {
            if (jsonElement instanceof ArrayNode) {
                return notExistOrEmptyArray().apply(jsonElement);
            } else {
                return notExistText().apply(jsonElement);
            }
        };
    }

    private Function<JsonNode, Boolean> notExistOrEmptyArray() {
        return jsonElement -> (
                jsonElement == null ||
                        ((jsonElement instanceof ArrayNode) && (jsonElement.size() == 0))
        );
    }

    private static Function<JsonNode, Boolean> notExistText() {
        return jsonElement -> (
                jsonElement == null ||
                        isBlank(jsonElement.asText())
        );
    }

    private static Function<JsonNode, Boolean> isNotNumeric() {
        return jsonNode -> !isDigits(jsonNode.asText());
    }

    private static Function<JsonNode, Boolean> isNotString() {
        return jsonNode -> !jsonNode.isTextual();
    }
    
    private static Function<JsonNode, Boolean> isBelowMax() {
        return jsonNode -> isDigits(jsonNode.asText()) && jsonNode.asLong() > MAX_PRICE;
    }

    private static Function<JsonNode, Boolean> isNotUrl() {
        return jsonNode -> {
            if (jsonNode == null || isBlank(jsonNode.asText()) || !jsonNode.asText().startsWith("https")) {
                return true;
            }

            try {
                new URL(jsonNode.asText());
            } catch (MalformedURLException e) {
                return true;
            }
            return false;
        };
    }
}
