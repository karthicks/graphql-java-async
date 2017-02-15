package graphql.async;


import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import graphql.ExecutionResult;
import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.ExecutionStrategy;
import graphql.execution.SimpleExecutionStrategy;
import graphql.schema.GraphQLSchema;

import static graphql.Assert.assertNotNull;
import static graphql.async.ExecutionFuture.completable;
import static graphql.async.ExecutionFuture.complete;

/**
 * An asynchronous flavor of {@link graphql.GraphQL}, which allows you to register a
 * {@link Consumer} to process {@link ExecutionResult}s, as opposed to waiting for them.
 */
public class GraphQL extends graphql.GraphQL {

  /**
   * A GraphQL object ready to execute queries
   *
   * @param graphQLSchema the schema to use
   * @deprecated use the {@link #newAsyncGraphQL(GraphQLSchema)} builder instead.  This will be
   * removed in a future version.
   */
  public GraphQL(GraphQLSchema graphQLSchema) {
    //noinspection deprecation
    this(graphQLSchema, AsyncExecutionStrategy.parallel());
  }


  /**
   * A GraphQL object ready to execute queries
   *
   * @param graphQLSchema the schema to use
   * @param queryStrategy the query execution strategy to use
   * @deprecated use the {@link #newAsyncGraphQL(GraphQLSchema)} builder instead.  This will be
   * removed in a future version.
   */
  public GraphQL(GraphQLSchema graphQLSchema, ExecutionStrategy queryStrategy) {
    //noinspection deprecation
    super(graphQLSchema, queryStrategy, AsyncExecutionStrategy.parallel());
  }

  /**
   * A GraphQL object ready to execute queries
   *
   * @param graphQLSchema    the schema to use
   * @param queryStrategy    the query execution strategy to use
   * @param mutationStrategy the mutation execution strategy to use
   * @deprecated use the {@link #newAsyncGraphQL(GraphQLSchema)} builder instead.  This will be
   * removed in a future version.
   */
  public GraphQL(GraphQLSchema graphQLSchema, ExecutionStrategy queryStrategy,
                 ExecutionStrategy mutationStrategy) {
    super(graphQLSchema, queryStrategy, mutationStrategy);
    assert queryStrategy instanceof AsyncExecutionStrategy;
    assert mutationStrategy instanceof AsyncExecutionStrategy;
  }

  /**
   * Helps you build a GraphQL object ready to execute queries
   *
   * @param graphQLSchema the schema to use
   * @return a builder of GraphQL objects
   */
  public static Builder newAsyncGraphQL(GraphQLSchema graphQLSchema) {
    return new Builder(graphQLSchema);
  }

  public static class Builder {

    private GraphQLSchema graphQLSchema;
    private ExecutionStrategy queryExecutionStrategy = AsyncExecutionStrategy.parallel();
    private ExecutionStrategy mutationExecutionStrategy = AsyncExecutionStrategy.serial();

    public Builder(GraphQLSchema graphQLSchema) {
      this.graphQLSchema = graphQLSchema;
    }

    public Builder schema(GraphQLSchema graphQLSchema) {
      assertNotNull(graphQLSchema, "GraphQLSchema must be non null");
      this.graphQLSchema = graphQLSchema;
      return this;
    }

    public Builder queryExecutionStrategy(ExecutionStrategy executionStrategy) {
      assertNotNull(executionStrategy, "Query ExecutionStrategy must be non null");
      this.queryExecutionStrategy = executionStrategy;
      return this;
    }

    public Builder mutationExecutionStrategy(ExecutionStrategy executionStrategy) {
      assertNotNull(executionStrategy, "Mutation ExecutionStrategy must be non null");
      this.mutationExecutionStrategy = executionStrategy;
      return this;
    }

    public GraphQL build() {
      return new GraphQL(graphQLSchema, queryExecutionStrategy, mutationExecutionStrategy);
    }
  }

  /**
   * Ensure that the {@link ExecutionResult} is in a completed state, before returning it.
   *
   * @param requestString the request string
   * @param operationName the operation name
   * @param context the context object
   * @param arguments the map of arguments
   * @return a completed execution result
   */
  @Override
  public ExecutionResult execute(String requestString, String operationName, Object context,
                                 Map<String, Object> arguments) {
    return complete(super.execute(requestString, operationName, context, arguments));
  }

  public void execute(String requestString, Consumer<ExecutionResult> consumer) {
    execute(requestString, (Object) null, consumer);
  }

  public void execute(String requestString, Object context, Consumer<ExecutionResult> consumer) {
    execute(requestString, context, Collections.<String, Object>emptyMap(), consumer);
  }

  public void execute(String requestString, String operationName, Object context,
                      Consumer<ExecutionResult> consumer) {
    execute(requestString, operationName, context, Collections.<String, Object>emptyMap(),
            consumer);
  }

  public void execute(String requestString, Object context, Map<String, Object> arguments,
                      Consumer<ExecutionResult> consumer) {
    execute(requestString, null, context, arguments, consumer);
  }

  /**
   * Register the given {@link Consumer} with the {@link java.util.concurrent.CompletableFuture}
   * corresponding to the {@link ExecutionResult} and be done with it.
   *
   * @param requestString the request string
   * @param operationName the operation name
   * @param context the context object
   * @param arguments the map of arguments
   * @param consumer the consumer of the execution result
   */
  public void execute(String requestString, String operationName, Object context,
                      Map<String, Object> arguments, Consumer<ExecutionResult> consumer) {
    completable(super.execute(requestString, operationName, context, arguments))
      .thenAccept(consumer);
  }
}
