package graphql.execution

import graphql.ExecutionResult
import graphql.NewsSchema
import graphql.async.GraphQL
import spock.lang.Specification

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

class AsyncExecutionStrategyTest extends Specification {

    def 'Example usage of AsyncExecutionStrategy.'() {
        given:
        def query = """
        query receive {
            news {
              article {
                text
              }
            }
        }
        """
        def mutation = """
        mutation publish {
            news {
              article (text: "Hello World") {
                text
              }
            }
        }
        """
        def expected = [
                news: [
                        article: [
                                text: 'Hello World'
                        ]
                ]
        ]

        when:
        BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>() {

            @Override
            public boolean offer(Runnable e) {
                /* queue that always rejects tasks */
                return false;
            }
        };

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                2, /* core pool size 2 thread */
                2, /* max pool size 2 thread */
                30, TimeUnit.SECONDS,
                /*
                 * Do not use the queue to prevent threads waiting on enqueued tasks.
                 */
                queue,
                /*
                 *  If all the threads are working, then the caller thread
                 *  should execute the code in its own thread. (serially)
                 */
                new ThreadPoolExecutor.CallerRunsPolicy());

        def graphQL = GraphQL.newAsyncGraphQL(NewsSchema.newsSchema)
                .queryExecutionStrategy(AsyncExecutionStrategy.serial(threadPoolExecutor))
                .build()

        def received = new AtomicReference<Map>()
        def latch = new CountDownLatch(1)
        graphQL.execute(query, new Consumer<ExecutionResult>() {

            @Override
            void accept(ExecutionResult executionResult) {
                received.set(executionResult.data)
                latch.countDown()
            }
        })

        def published = graphQL.execute(mutation)
        latch.await()
        def result = received.get()

        then:
        result == expected
    }
}
