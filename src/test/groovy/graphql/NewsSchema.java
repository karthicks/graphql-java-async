package graphql;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import graphql.language.Field;
import graphql.language.StringValue;
import graphql.schema.GraphQLNonNull;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

public class NewsSchema {

    public static class Article {

        private String text;

        public Article(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    public static class Subscriber implements Supplier<Article> {

        private CountDownLatch latch;
        private Article article;

        public Subscriber() {
            latch = new CountDownLatch(1);
        }

        public void set(Article article) {
            this.article = article;
            latch.countDown();
        }

        @Override
        public Article get() {
            try {
                latch.await();
                return article;
            } catch (InterruptedException e) {
                throw new RuntimeException("Got interrupted while waiting for article");
            }
        }
    }

    public static class News {

        private Article article;
        private List<Subscriber> subscribers = new ArrayList<>();

        public News() {
        }

        public News(Article article) {
            this.article = article;
        }

        public Article getArticle() {
            return article;
        }

        public void publish(final Article article) {
            for (Subscriber subscriber : subscribers) {
                subscriber.set(article);
            }
            subscribers.clear();
        }

        public CompletableFuture<News> receive() {
            Subscriber subscriber = new Subscriber();
            subscribers.add(subscriber);
            return CompletableFuture
                .supplyAsync(subscriber)
                .thenApply((article) -> new News(article));
        }
    }

    public static News news = new News();

    public static GraphQLObjectType articleType = newObject()
        .name("article")
        .description("A news article.")
        .field(newFieldDefinition()
                   .name("text")
                   .description("The text of the article.")
                   .type(new GraphQLNonNull(GraphQLString)))
        .build();

    public static GraphQLObjectType newsType = newObject()
        .name("news")
        .description("A collection of articles.")
        .field(newFieldDefinition()
                   .name("article")
                   .description("A news article.")
                   .argument(newArgument()
                                 .name("text")
                                 .description("If omitted, returns the next sent article. If provided, sends a article with that text.")
                                 .type(GraphQLString))
                   .type(articleType))
        .build();


    public static GraphQLObjectType queryType = newObject()
        .name("QueryType")
        .field(newFieldDefinition()
                   .name("news")
                   .type(newsType)
                   .dataFetcher((environment) -> news.receive()))
        .build();

    public static GraphQLObjectType mutationType = newObject()
        .name("MutationType")
        .field(newFieldDefinition()
                   .name("news")
                   .type(newsType)
                   .dataFetcher((environment) -> {
                       Field field = environment.getFields().get(0);
                       Field selection = (Field) field.getSelectionSet().getSelections().get(0);
                       String text = ((StringValue) selection.getArguments().get(0).getValue()).getValue();
                       Article article = new Article(text);
                       news.publish(article);
                       return article;
                   }))
        .build();


    public static GraphQLSchema newsSchema = GraphQLSchema.newSchema()
        .query(queryType)
        .mutation(mutationType)
        .build();
}
