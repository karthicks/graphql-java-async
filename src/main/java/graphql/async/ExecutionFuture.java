package graphql.async;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;

public class ExecutionFuture {

  public static ExecutionResult complete(ExecutionResult executionResult) {
    return completable(executionResult).join();
  }

  public static CompletableFuture<ExecutionResult> completable(ExecutionResult executionResult) {
    return completable(executionResult, ForkJoinPool.commonPool());
  }

  @SuppressWarnings("unchecked")
  public static CompletableFuture<ExecutionResult> completable(ExecutionResult executionResult,
                                                               ExecutorService executorService) {
    Object executionData = executionResult != null ? executionResult.getData() : null;
    if (executionData instanceof CompletableFuture) {
      return ((CompletableFuture) executionData)
        .exceptionally((throwable) -> {
          ((ExecutionResultImpl) executionResult).addErrors(Arrays.asList(
            new ExceptionWhileDataFetching((Throwable) throwable)
          ));
          return executionResult;
        })
        .thenApplyAsync(
//          (Function<Object, ExecutionResult>)
            (completedData) -> {
              ((ExecutionResultImpl) executionResult).setData(completedData);
              return executionResult;
            }, executorService);
    } else if (executionData instanceof Map) {
      final Map<String, Object> results = (Map<String, Object>) executionData;
      List<CompletableFuture<Object>> completables = new ArrayList<>();
      for (final String fieldName : results.keySet()) {
        Object fieldValue = results.get(fieldName);
        if (fieldValue instanceof CompletableFuture) {
          completables.add(
            ((CompletableFuture) fieldValue)
              .thenAccept((fieldValueDone) -> results.put(fieldName, fieldValueDone)));
        }
      }
      if (!completables.isEmpty()) {
        return CompletableFuture.allOf(array(completables))
          .exceptionally((throwable) -> {
            ((ExecutionResultImpl) executionResult).addErrors(Arrays.asList(
              new ExceptionWhileDataFetching(throwable)
            ));
            return null;
          })
          .thenApply(
//            (Function<Void, ExecutionResult>)
              (completablesDone) -> executionResult);
      }
    }
    return CompletableFuture.completedFuture(executionResult);
  }

  public static CompletableFuture[] array(
    Collection<? extends CompletableFuture> completableFutures) {
    return completableFutures.toArray(new CompletableFuture[]{});
  }

}
