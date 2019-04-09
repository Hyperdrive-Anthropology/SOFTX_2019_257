/*
 * Copyright 2006-2019 DLR, Germany
 * 
 * SPDX-License-Identifier: EPL-1.0
 * 
 * http://www.rcenvironment.de/
 */

package de.rcenvironment.toolkit.modules.concurrency.api;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * Convenience interface to execute groups of {@link Callable}s in parallel.
 * 
 * @param <T> the callback type of the {@link Callable}s
 * 
 * @author Robert Mischke
 */
public interface CallablesGroup<T> {

    /**
     * Enqueues a {@link Callable} task for parallel execution.
     * 
     * @param task the new task
     */
    void add(Callable<T> task);

    /**
     * Enqueues a {@link Callable} task with a unique task id for parallel execution. The task id is used the same way as when calling
     * {@link AsyncTaskService#submit(Callable, String)}.
     * 
     * @param task the new task
     * @param taskId the task id
     */
    void add(Callable<T> task, String taskId);

    /**
     * Executes all enqueued tasks, and blocks until they have completed (or failed). The return value is a list of the results generated by
     * the {@link Callable}s, in the order they were added via {@link #add(Callable)}. On failure of a {@link Callable}, a result of "null"
     * is added in the respective place of the result list. Optionally, an {@link AsyncExceptionListener} can be set that is called with the
     * generated exception.
     * 
     * @param exceptionHandler an optional callback for occurring {@link Exception}s; set to null to disable
     * @return the ordered list of task results
     */
    List<T> executeParallel(AsyncExceptionListener exceptionHandler);

}
