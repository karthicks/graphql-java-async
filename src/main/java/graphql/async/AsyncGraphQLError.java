package graphql.async;


import java.util.ArrayList;
import java.util.List;

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

public class AsyncGraphQLError implements GraphQLError {

    private final List<SourceLocation> sourceLocations = new ArrayList<SourceLocation>();

    public AsyncGraphQLError(SourceLocation sourceLocation) {
        if (sourceLocation != null)
            this.sourceLocations.add(sourceLocation);
    }

    public AsyncGraphQLError(List<SourceLocation> sourceLocations) {
        if (sourceLocations != null) {
            this.sourceLocations.addAll(sourceLocations);
        }
    }


    @Override
    public String getMessage() {
        return "Invalid Syntax";
    }

    @Override
    public List<SourceLocation> getLocations() {
        return sourceLocations;
    }

    @Override
    public ErrorType getErrorType() {
        return ErrorType.InvalidSyntax;
    }

    @Override
    public String toString() {
        return "InvalidSyntaxError{" +
                "sourceLocations=" + sourceLocations +
                '}';
    }

}
