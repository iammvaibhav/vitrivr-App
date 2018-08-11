package org.vitrivr.vitrivrapp.data.gson

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes

/**
 * AnnotationExclusionStrategy used while building a gson object for setting exclusion strategies
 */
class AnnotationExclusionStrategy : ExclusionStrategy {

    override fun shouldSkipField(f: FieldAttributes): Boolean {
        return f.getAnnotation(Exclude::class.java) != null
    }

    override fun shouldSkipClass(clazz: Class<*>): Boolean {
        return false
    }
}
