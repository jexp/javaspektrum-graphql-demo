package movies;

import graphql.GraphQL;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

/**
 * @author mh
 * @since 31.05.17
 */
public class MovieExample {

    public static Map<String, Object> map(Object... args) {
        Map<String, Object> result = new HashMap<>(args.length / 2);
        for (int i = 0; i < args.length; i += 2) {
            result.put(args[i].toString(), args[i + 1]);
        }
        return result;
    }

    public static <T> Set<T> asSet(T... args) {
        return new LinkedHashSet<>(asList(args));
    }

    public static class Person {
        public final String name;
        public final int born;
        public final List<String> movies;

        // constructor & getter omitted
        public Person(String name, int born, List<String> movies) {
            this.name = name;
            this.born = born;
            this.movies = movies;
        }

        public String getName() {
            return name;
        }

        public int getBorn() {
            return born;
        }

        public List<String> getMovies() {
            return movies;
        }
    }

    Map<String, Map<String, Object>> movies = new HashMap<>();
    Map<String, Person> people = new HashMap<>();

    {
        movies.put("The Matrix", map("title", "The Matrix", "released", 1999,
                "actors", asList("Keanu Reeves"), "directors", asList("Lana Wachowski")));

        people.put("Keanu Reeves", new Person("Keanu Reeves", 1964,
                asList("The Matrix")));
        people.put("Carrie Ann Moss", new Person("Carrie Ann Moss", 1967,
                asList("Memento", "Chocolat", "The Matrix")));
    }

    public GraphQL movieSchema() {

        GraphQLObjectType person =
                GraphQLObjectType.newObject().name("Person")
                        .field(newFieldDefinition().name("name").type(GraphQLString))
                        .field(newFieldDefinition().name("born").type(GraphQLInt))
                        .field(newFieldDefinition().name("movies").type(new GraphQLList(new GraphQLTypeReference("Movie")))
                                .dataFetcher((env) ->
                                        env.<Person>getSource().getMovies()
                                                .stream().map(env.<MovieExample>getContext().movies::get).collect(toList())
                                ))
                        .build();

        GraphQLObjectType movie =
                GraphQLObjectType.newObject().name("Movie")
                        .field(newFieldDefinition().name("title").type(GraphQLString))
                        .field(newFieldDefinition().name("released").type(GraphQLInt))
                        .field(newFieldDefinition().name("actors").type(new GraphQLList(new GraphQLTypeReference("Person")))
                                .dataFetcher((env) ->
                                        env.<Map<String, List<String>>>getSource().get("actors")
                                                .stream().map(env.<MovieExample>getContext().people::get).collect(toList())
                                ))

                        .field(newFieldDefinition().name("directors").type(new GraphQLList(new GraphQLTypeReference("Person")))
                                .dataFetcher((env) ->
                                        env.<Map<String, List<String>>>getSource().get("directors")
                                                .stream().map(env.<MovieExample>getContext().people::get).collect(toList())
                                ))
                        .build();

        GraphQLSchema schema = GraphQLSchema.newSchema()

                .query(GraphQLObjectType.newObject().name("QueryType")
                        .field(newFieldDefinition().name("movieByTitle").type(new GraphQLTypeReference("Movie"))
                                .argument(newArgument().name("title").type(GraphQLString))
                                .dataFetcher((env) -> env.<MovieExample>getContext().movies
                                        .get(env.<String>getArgument("title")))
                        ))

                .mutation(GraphQLObjectType.newObject().name("MutationType")
                        .field(newFieldDefinition().name("createPerson").type(new GraphQLTypeReference("Person"))
                                .argument(newArgument().name("name").type(GraphQLString))
                                .argument(newArgument().name("born").type(GraphQLInt))
                                .argument(newArgument().name("movies").type(new GraphQLList(GraphQLString)))
                                .dataFetcher((env) -> env.<MovieExample>getContext().movies
                                        .put(env.<String>getArgument("name"), env.getArguments()))
                        ))

                .build(asSet(person, movie));

        return GraphQL.newGraphQL(schema).build();
    }

    public GraphQL parsedSchema() throws IOException {

        SchemaParser schemaParser = new SchemaParser();
        InputStreamReader stream = new InputStreamReader(getClass().getResource("/movies-schema.graphql").openStream());
        TypeDefinitionRegistry compiledSchema = schemaParser.parse(stream);

        RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("MutationType", typeWiring -> typeWiring
                        .dataFetcher("createPerson",
                                (env) -> env.<MovieExample>getContext().movies
                                        .put(env.<String>getArgument("name"), env.getArguments()))
                ).type("QueryType", typeWiring -> typeWiring
                        .dataFetcher("movieByTitle",
                                (env) -> env.<MovieExample>getContext().movies
                                        .get(env.<String>getArgument("title")))
                ).type("Person", typeWiring -> typeWiring
                        .dataFetcher("movies", (env) ->
                                env.<Person>getSource().getMovies().stream()
                                        .map(env.<MovieExample>getContext().movies::get).collect(toList())
                        )
                ).type("Movie", typeWiring -> typeWiring
                        .dataFetcher("actors", (env) ->
                                env.<Map<String, List<String>>>getSource().get("actors")
                                        .stream().map(env.<MovieExample>getContext().people::get).collect(toList())
                        )
                        .dataFetcher("directors", (env) ->
                                env.<Map<String, List<String>>>getSource().get("directors")
                                        .stream().map(env.<MovieExample>getContext().people::get).collect(toList())
                        ))
                .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema schema = schemaGenerator.makeExecutableSchema(compiledSchema, runtimeWiring);

        return GraphQL.newGraphQL(schema).build();
    }
}
