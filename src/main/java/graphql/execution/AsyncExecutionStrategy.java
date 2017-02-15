package graphql.execution;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.language.Field;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;

import static graphql.async.ExecutionFuture.completable;
import static graphql.async.ExecutionFuture.array;

/**
 * <p>AsyncExecutionStrategy implements the {@link ExecutionStrategy} in a non-blocking manner.</p>
 *
 * It modifies the result of the execution such that it's a {@link CompletableFuture} of the actual
 * value of {@link ExecutionResult#getData()}. This facilitates the handling of the execution result
 * without blocking, through a {@link java.util.function.Consumer}.
 *
 * Further, it permits it's {@link graphql.schema.DataFetcher} to wrap their result in a {@link
 * CompletableFuture}, thereby promoting non-blocking data fetching.
 *
 * The {@link ExecutorService} used here must be configured along the lines of the one passed to the
 * {@link ExecutorServiceExecutionStrategy}, and if anything, it should have a higher setting for
 * {@code maximumPoolSize}, given the asynchronous nature of the execution.
 *
 * See {@code graphql.execution.AsyncExecutionStrategyTest} for example usage.
 */
public class AsyncExecutionStrategy extends ExecutionStrategy {

  protected ExecutorService executorService;
  protected boolean serial;

  public static AsyncExecutionStrategy serial() {
    return new AsyncExecutionStrategy(true);
  }

  public static AsyncExecutionStrategy serial(ExecutorService executorService) {
    return new AsyncExecutionStrategy(true, executorService);
  }

  public static AsyncExecutionStrategy parallel() {
    return new AsyncExecutionStrategy(false);
  }

  public static AsyncExecutionStrategy parallel(ExecutorService executorService) {
    return new AsyncExecutionStrategy(false, executorService);
  }

  private AsyncExecutionStrategy(boolean serial) {
    this(serial, ForkJoinPool.commonPool());
  }

  protected AsyncExecutionStrategy(boolean serial, ExecutorService executorService) {
    this.serial = serial;
    this.executorService = executorService;
  }

  /**
   * Resolve the given fields in parallel and return an execution result without blocking.
   *
   * @return an execution result whose data is wrapped in a completable future.
   */
  @Override
  public ExecutionResult execute(final ExecutionContext executionContext,
                                 final GraphQLObjectType parentType,
                                 final Object source, final Map<String, List<Field>> fields) {
    if (executorService == null) {
      return new SimpleExecutionStrategy().execute(executionContext, parentType, source, fields);
    }

    Map<String, CompletableFuture<ExecutionResult>> fieldFutures = new LinkedHashMap<>();
    Set<String> fieldNames = fields.keySet();

    // Create tasks to resolve each of the fields
    (serial ? fieldNames.stream() : fieldNames.parallelStream()).forEach((fieldName) -> {
      fieldFutures.put(fieldName, CompletableFuture.supplyAsync(
        () -> resolveField(executionContext, parentType, source, fields.get(fieldName)),
        executorService));
    });

    // Prepare a completable for the map of field results
    CompletableFuture<Map<String, Object>> resultsFuture = CompletableFuture
      // First, wait for all the tasks above to complete
      .allOf(array(fieldFutures.values()))
      .exceptionally((throwable) -> {
        executionContext.addError(new ExceptionWhileDataFetching(throwable));
        return null;
      })
      // Then, wait for each of the field results to complete
      .thenComposeAsync((resolveDone) -> {
        Map<String, ExecutionResult> fieldResults = new LinkedHashMap<>();
        return CompletableFuture
          .allOf(array(
            fieldNames.stream()
              .map((fieldName) -> {
                final ExecutionResult fieldResult = fieldFutures.get(fieldName).join();
                fieldResults.put(fieldName, fieldResult);
                return completable(fieldResult, executorService);
              })
              .collect(Collectors.<CompletableFuture<ExecutionResult>>toList())
          ))
          .exceptionally((throwable) -> {
            executionContext.addError(new ExceptionWhileDataFetching(throwable));
            return null;
          })
          .thenApplyAsync((resultsDone) -> fieldResults, executorService);
      }, executorService)
      // Last, collect the results of the field into a map
      .exceptionally((throwable) -> {
        executionContext.addError(new ExceptionWhileDataFetching(throwable));
        return new LinkedHashMap<>();
      })
      .thenApplyAsync((fieldResults) -> {
        Map<String, Object> results = new LinkedHashMap<>();
        for (String fieldName : fieldResults.keySet()) {
          ExecutionResult fieldResult = fieldResults.get(fieldName);
          Object fieldData = fieldResult != null ? fieldResult.getData() : null;
          results.put(fieldName, fieldData);
        }
        return results;
      }, executorService);

    return new ExecutionResultImpl(resultsFuture, executionContext.getErrors());
  }

  /**
   * If the result that is returned by the {@link graphql.schema.DataFetcher} is a {@link
   * CompletableFuture}, then wait for it to complete, prior to calling the super method.
   *
   * @return the completed execution result
   */
  @Override
  protected ExecutionResult completeValue(final ExecutionContext executionContext,
                                          final GraphQLType fieldType,
                                          final List<Field> fields, Object result) {
    if (!(result instanceof CompletableFuture)) {
      return super.completeValue(executionContext, fieldType, fields, result);
    }
    ExecutionStrategy executionStrategy = this;
    return ((CompletableFuture<ExecutionResult>) result)
      .thenComposeAsync(
        (Function<Object, CompletableFuture<ExecutionResult>>)
          (completedResult) -> completable(
            executionStrategy.completeValue(executionContext, fieldType, fields, completedResult),
            executorService), executorService)
      .exceptionally((throwable) -> {
        executionContext.addError(new ExceptionWhileDataFetching(throwable));
        return null;
      })
      .join();
  }

  /**
   * If the result is a list, then it's elements can now potentially be a {@link CompletableFuture}.
   * To reduce the number of tasks in progress, it behooves us to wait for all those completable
   * elements to wrap up.
   *
   * @return the completed execution result
   */
  @Override
  protected ExecutionResult completeValueForList(ExecutionContext executionContext,
                                                 GraphQLList fieldType,
                                                 List<Field> fields, Iterable<Object> result) {
    ExecutionResult executionResult =
      super.completeValueForList(executionContext, fieldType, fields, result);
    List<Object> completableResults = (List<Object>) executionResult.getData();
    List<Object> completedResults = new ArrayList<>();
    completableResults.forEach((completedResult) -> {
      completedResults.add((completedResult instanceof CompletableFuture) ?
                           ((CompletableFuture) completedResult).join() : completedResult);
    });
    ((ExecutionResultImpl) executionResult).setData(completedResults);
    return executionResult;
  }
}
