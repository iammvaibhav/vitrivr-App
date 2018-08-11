package org.vitrivr.vitrivrapp.data.gson

/**
 * This annotation will exclude Gson from serializing if object is built setting AnnotationExclusionStrategy
 * in exclusion strategies
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Exclude