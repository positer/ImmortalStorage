package com.immortalstorage.immortalstorage.source.definition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Strict parser: unknown fields and malformed bounds disable only the offending definition. */
public final class SourceDefinitionParser {
    private static final Set<String> FIELDS = Set.of(
            "id", "type", "item", "fluid", "yuan_cost_per_batch", "outputs_per_batch",
            "min_stage", "default_rate", "max_rate", "display_name", "core_color",
            "model_hint", "aliases");

    public static SourceDefinition parse(Identifier source, JsonObject json) {
        for (String field : json.keySet()) {
            if (!FIELDS.contains(field)) throw error(source, "unknown field '" + field + "'");
        }
        Identifier id = json.has("id")
                ? id(source, GsonHelper.getAsString(json, "id"), "id") : source;
        String rawType = GsonHelper.getAsString(json, "type");
        SourceDefinition.OutputType type;
        try {
            type = SourceDefinition.OutputType.valueOf(rawType.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw error(source, "type must be 'item' or 'fluid'");
        }
        boolean hasItem = json.has("item");
        boolean hasFluid = json.has("fluid");
        if (hasItem == hasFluid) throw error(source, "exactly one of item/fluid is required");
        if (type == SourceDefinition.OutputType.ITEM && !hasItem
                || type == SourceDefinition.OutputType.FLUID && !hasFluid) {
            throw error(source, "type does not match the declared output field");
        }
        Identifier output = id(source,
                GsonHelper.getAsString(json, type == SourceDefinition.OutputType.ITEM ? "item" : "fluid"),
                type.name().toLowerCase(java.util.Locale.ROOT));
        long cost = nonNegativeLong(source, json, "yuan_cost_per_batch", 0L);
        long batch = positiveLong(source, json, "outputs_per_batch", 1L);
        int minStage = GsonHelper.getAsInt(json, "min_stage", 0);
        long defaultRate = nonNegativeLong(source, json, "default_rate", type == SourceDefinition.OutputType.FLUID ? 1000L : 64L);
        long maxRate = nonNegativeLong(source, json, "max_rate", Long.MAX_VALUE);
        String displayName = GsonHelper.getAsString(json, "display_name", "");
        int color = parseColor(source, json.get("core_color"));
        String modelHint = GsonHelper.getAsString(json, "model_hint", "");
        List<Identifier> aliases = parseAliases(source, json.get("aliases"));
        try {
            return new SourceDefinition(id, type, output, cost, batch, minStage,
                    defaultRate, maxRate, displayName, color, modelHint, aliases, null);
        } catch (IllegalArgumentException error) {
            throw error(source, error.getMessage());
        }
    }

    private static long nonNegativeLong(Identifier source, JsonObject json, String field, long fallback) {
        long value = json.has(field) ? integralLong(source, json.get(field), field) : fallback;
        if (value < 0L) throw error(source, field + " must be non-negative");
        return value;
    }

    private static long positiveLong(Identifier source, JsonObject json, String field, long fallback) {
        long value = json.has(field) ? integralLong(source, json.get(field), field) : fallback;
        if (value <= 0L) throw error(source, field + " must be positive");
        return value;
    }

    private static long integralLong(Identifier source, JsonElement element, String field) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw error(source, field + " must be an integer");
        }
        try {
            java.math.BigDecimal decimal = element.getAsBigDecimal();
            return decimal.longValueExact();
        } catch (ArithmeticException | NumberFormatException error) {
            throw error(source, field + " is outside the signed long integer range");
        }
    }

    private static int parseColor(Identifier source, JsonElement element) {
        if (element == null || element.isJsonNull()) return 0xFFFFFF;
        if (!element.isJsonPrimitive()) throw error(source, "core_color must be #RRGGBB or an integer");
        if (element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            if (!value.matches("#[0-9a-fA-F]{6}")) throw error(source, "core_color must be #RRGGBB");
            return Integer.parseInt(value.substring(1), 16);
        }
        long value = integralLong(source, element, "core_color");
        if (value < 0L || value > 0xFFFFFFL) throw error(source, "core_color must be RGB");
        return (int) value;
    }

    private static List<Identifier> parseAliases(Identifier source, JsonElement element) {
        if (element == null || element.isJsonNull()) return List.of();
        if (!(element instanceof JsonArray array)) throw error(source, "aliases must be an array");
        ArrayList<Identifier> result = new ArrayList<>();
        for (JsonElement alias : array) {
            if (!alias.isJsonPrimitive() || !alias.getAsJsonPrimitive().isString()) {
                throw error(source, "every alias must be a resource location string");
            }
            Identifier parsed = id(source, alias.getAsString(), "aliases");
            if (!result.add(parsed)) throw error(source, "duplicate alias " + parsed);
        }
        return List.copyOf(result);
    }

    private static Identifier id(Identifier source, String raw, String field) {
        Identifier parsed = Identifier.tryParse(raw);
        if (parsed == null) throw error(source, field + " is not a valid resource location: " + raw);
        return parsed;
    }

    private static IllegalArgumentException error(Identifier source, String message) {
        return new IllegalArgumentException(source + ": " + message);
    }

    private SourceDefinitionParser() {}
}
