package com.cnh.ies.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable method-level logging via {@link LoggingAspect}.
 * 
 * When placed on a method, the aspect will log:
 * - Method entry with parameters
 * - Method exit with return value and duration
 * - Any exceptions thrown
 * 
 * Usage:
 * <pre>
 * &#64;Loggable
 * public Result processPayment(PaymentRequest request) {
 *     // method implementation
 * }
 * </pre>
 * 
 * Note: Service layer methods are automatically logged without this annotation.
 * Use this annotation for methods outside the service layer that need logging.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {
}
