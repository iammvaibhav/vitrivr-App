package org.vitrivr.vitrivrapp.core.exception

/**
 * Base Class for handling errors/failures/exceptions.
 * Every feature specific failure should extend [FeatureFailure] class.
 */
sealed class Failure {
    //define failure classes

    /** * Extend this class for feature specific failures.*/
    abstract class FeatureFailure: Failure()
}
