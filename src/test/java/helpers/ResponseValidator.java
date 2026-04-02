package helpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Compares an actual API response against an expected JSON "expectation" file.
 *
 * <h3>Design philosophy</h3>
 * <ul>
 *   <li>Partial matching — the actual response may contain fields not mentioned
 *       in the expectation; only fields declared in the expectation are checked.</li>
 *   <li>Recursive — nested objects and arrays are walked automatically.</li>
 *   <li>All mismatches are collected before failing, so one test run surfaces
 *       every problem instead of stopping at the first.</li>
 * </ul>
 *
 * <h3>Supported matchers</h3>
 * Use these as string values in the expectation JSON:
 * <pre>
 *   ${ignore}           — field must exist, value is not checked
 *   ${notNull}          — field must exist and not be JSON null
 *   ${type:string}      — field must be a string  (also: number, boolean, array, object)
 *   ${regex:pattern}    — field's text value must match the regex
 *   ${contains:text}    — field's text value must contain the substring
 *   ${greaterThan:n}    — field must be a number greater than n
 * </pre>
 *
 * Any value that is NOT a matcher is compared literally (numbers, booleans,
 * strings, nulls).
 */
public final class ResponseValidator {

    private static final ObjectMapper mapper = new ObjectMapper();

    private ResponseValidator() {
    }

    /**
     * Loads the expectation file from the classpath and validates the actual
     * response against it.
     *
     * @param actualJson      raw JSON body from the API response
     * @param expectationName path under {@code expectations/}, without {@code .json}
     * @return list of mismatch descriptions — empty means the response matched
     */
    public static List<String> validate(String actualJson, String expectationName) {
        String expectedRaw = DataHelper.loadRawJson("expectations/" + expectationName + ".json");
        List<String> errors = new ArrayList<>();
        try {
            JsonNode actual = mapper.readTree(actualJson);
            JsonNode expected = mapper.readTree(expectedRaw);
            compare(actual, expected, "$", errors);
        } catch (IOException e) {
            errors.add("Failed to parse JSON: " + e.getMessage());
        }
        return errors;
    }

    // ── recursive comparison ────────────────────────────────────────────

    private static void compare(JsonNode actual, JsonNode expected, String path, List<String> errors) {
        // Check for matchers first (they are always string nodes)
        if (expected.isTextual() && isMatcher(expected.asText())) {
            applyMatcher(actual, expected.asText(), path, errors);
            return;
        }

        if (expected.isObject()) {
            compareObject(actual, expected, path, errors);
        } else if (expected.isArray()) {
            compareArray(actual, expected, path, errors);
        } else {
            // Literal comparison (string, number, boolean, null)
            if (!expected.equals(actual)) {
                errors.add(path + ": expected " + expected + " but got " + actual);
            }
        }
    }

    private static void compareObject(JsonNode actual, JsonNode expected, String path, List<String> errors) {
        if (actual == null || !actual.isObject()) {
            errors.add(path + ": expected an object but got " + describe(actual));
            return;
        }
        Iterator<String> fields = expected.fieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            String childPath = path + "." + field;
            if (!actual.has(field)) {
                errors.add(childPath + ": field is missing");
            } else {
                compare(actual.get(field), expected.get(field), childPath, errors);
            }
        }
    }

    private static void compareArray(JsonNode actual, JsonNode expected, String path, List<String> errors) {
        if (actual == null || !actual.isArray()) {
            errors.add(path + ": expected an array but got " + describe(actual));
            return;
        }
        for (int i = 0; i < expected.size(); i++) {
            String childPath = path + "[" + i + "]";
            if (i >= actual.size()) {
                errors.add(childPath + ": missing (actual array has only " + actual.size() + " elements)");
            } else {
                compare(actual.get(i), expected.get(i), childPath, errors);
            }
        }
    }

    // ── matcher handling ────────────────────────────────────────────────

    private static boolean isMatcher(String value) {
        return value.startsWith("${") && value.endsWith("}");
    }

    private static void applyMatcher(JsonNode actual, String matcher, String path, List<String> errors) {
        String directive = matcher.substring(2, matcher.length() - 1);

        if (directive.equals("ignore")) {
            return;
        }

        if (directive.equals("notNull")) {
            if (actual == null || actual.isNull()) {
                errors.add(path + ": expected a non-null value but got null");
            }
            return;
        }

        if (directive.startsWith("type:")) {
            checkType(actual, directive.substring(5), path, errors);
            return;
        }

        if (directive.startsWith("regex:")) {
            String pattern = directive.substring(6);
            if (actual == null || actual.isNull()) {
                errors.add(path + ": expected a value matching /" + pattern + "/ but got null");
            } else if (!Pattern.matches(pattern, actual.asText())) {
                errors.add(path + ": value '" + actual.asText() + "' does not match /" + pattern + "/");
            }
            return;
        }

        if (directive.startsWith("contains:")) {
            String substring = directive.substring(9);
            if (actual == null || actual.isNull()) {
                errors.add(path + ": expected a value containing '" + substring + "' but got null");
            } else if (!actual.asText().contains(substring)) {
                errors.add(path + ": value '" + actual.asText() + "' does not contain '" + substring + "'");
            }
            return;
        }

        if (directive.startsWith("greaterThan:")) {
            double threshold = Double.parseDouble(directive.substring(12));
            if (actual == null || !actual.isNumber()) {
                errors.add(path + ": expected a number > " + threshold + " but got " + describe(actual));
            } else if (actual.asDouble() <= threshold) {
                errors.add(path + ": expected > " + threshold + " but got " + actual.asDouble());
            }
            return;
        }

        errors.add(path + ": unknown matcher " + matcher);
    }

    private static void checkType(JsonNode actual, String expectedType, String path, List<String> errors) {
        boolean matches = switch (expectedType) {
            case "string" -> actual != null && actual.isTextual();
            case "number" -> actual != null && actual.isNumber();
            case "boolean" -> actual != null && actual.isBoolean();
            case "array" -> actual != null && actual.isArray();
            case "object" -> actual != null && actual.isObject();
            default -> false;
        };
        if (!matches) {
            errors.add(path + ": expected type '" + expectedType + "' but got " + describe(actual));
        }
    }

    private static String describe(JsonNode node) {
        if (node == null) return "missing";
        if (node.isNull()) return "null";
        return node.getNodeType().toString().toLowerCase();
    }
}
