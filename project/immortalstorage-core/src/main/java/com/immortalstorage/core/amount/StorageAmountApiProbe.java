package com.immortalstorage.core.amount;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Reflective contract probe for optional storage APIs.
 *
 * <p>The core does not link any optional mod.  Integrations use this probe at
 * their guarded bootstrap boundary to decide whether a target really accepts
 * long-valued amounts.  An integer fallback is therefore a proven capability
 * decision, never a version-name guess.</p>
 */
public final class StorageAmountApiProbe {
    private StorageAmountApiProbe() {
    }

    public static Result probe(
            ClassLoader loader,
            String className,
            String methodName,
            int parameterCount,
            int longParameterIndex) {
        Objects.requireNonNull(loader, "loader");
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(methodName, "methodName");
        try {
            Class<?> type = Class.forName(className, false, loader);
            for (Method method : type.getMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (!methodName.equals(method.getName())
                        || parameters.length != parameterCount
                        || method.getReturnType() != long.class) {
                    continue;
                }
                if (longParameterIndex >= 0
                        && (longParameterIndex >= parameters.length
                        || parameters[longParameterIndex] != long.class)) {
                    continue;
                }
                return new Result(className, methodName, true, true,
                        method.toGenericString());
            }
            return new Result(className, methodName, true, false,
                    "no matching long-valued method");
        } catch (ClassNotFoundException exception) {
            return new Result(className, methodName, false, false, "class not found");
        } catch (LinkageError error) {
            return new Result(className, methodName, false, false,
                    "linkage error: " + error.getClass().getSimpleName());
        } catch (SecurityException exception) {
            return new Result(className, methodName, false, false,
                    "security error: " + exception.getClass().getSimpleName());
        }
    }

    public record Result(
            String className,
            String methodName,
            boolean classPresent,
            boolean longSignature,
            String detail) {
        public boolean supported() {
            return classPresent && longSignature;
        }
    }
}
