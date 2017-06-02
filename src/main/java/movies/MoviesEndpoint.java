package movies;

import com.google.gson.Gson;
import graphql.ExecutionResult;
import graphql.GraphQL;

import java.util.Map;

import static movies.MovieExample.map;
import static spark.Spark.port;
import static spark.Spark.post;

// curl -d'{"query":"{ movieByTitle(title:\"The Matrix\") {title}}","variables":{}}' http://localhost:8080/graphql

@SuppressWarnings("unchecked")
public class MoviesEndpoint {
    public static void main(String[] args) {
        port(8080);

        MovieExample context = new MovieExample();
        GraphQL graphQL = context.movieSchema();
        Gson gson = new Gson();

        post("/graphql", (req, res) -> {
            try {
                System.err.println(req.body());
                Map body = gson.fromJson(req.body(), Map.class);

                String query = (String) body.get("query");
                Object variables = body.getOrDefault("variables", "{}");
                Map<String, Object> arguments = variables instanceof String ? gson.fromJson(variables.toString(), Map.class) : (Map)variables;

                ExecutionResult result = graphQL.execute(query, context, arguments);

                Map<String, Object> resultData = result.getErrors().isEmpty() ?
                        map("data", result.getData()) :
                        map("data", result.getData(), "errors", result.getErrors());

                String response = gson.toJson(resultData);
                System.err.println(response);
                return response;
            } catch(Exception e) {
                e.printStackTrace();
                res.status(500);
                return null;
            }
        });
    }
}
