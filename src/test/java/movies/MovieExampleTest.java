package movies;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import graphql.ExecutionResult;
import graphql.GraphQL;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author mh
 * @since 02.06.17
 */
public class MovieExampleTest {

    private static final String QUERY = "{ movieByTitle(title:\"The Matrix\") { title, released, actors { name, born, movies { title } } } }";
    private MovieExample context;

    @Before
    public void setUp() throws Exception {
        context = new MovieExample();
    }

    @Test
    public void testSchema() throws Exception {
        GraphQL graphQL = context.movieSchema();
        ExecutionResult result = graphQL.execute(QUERY, context);
        System.out.println("result = " + result.getData());
    }

    @Test
    public void testParseSchema() throws Exception {
        GraphQL graphQL = context.parsedSchema();
        ExecutionResult result = graphQL.execute(QUERY, context);
        System.out.println("result = " + result.getData());
    }

    @Test
    public void testQueryServer() throws Exception {
        ApolloClient apolloClient = ApolloClient.builder()
                .okHttpClient(new OkHttpClient())
                .serverUrl("http://localhost:8080/graphql").build();

        ApolloCall<MovieByTitle.Data> call = apolloClient.newCall(MovieByTitle.builder()
                .title("The Matrix")
                .build());
        Response<MovieByTitle.Data> response = call.execute();

        MovieByTitle.Data.MovieByTitle1 movieByTitle = response.data().movieByTitle();
        System.err.println(movieByTitle.title());
        System.err.println(movieByTitle.actors());
    }
    @Test
    public void testQueryServerAsync() throws Exception {
        ApolloClient apolloClient = ApolloClient.builder()
                .okHttpClient(new OkHttpClient())
                .serverUrl("http://localhost:8080/graphql").build();

        ApolloCall<MovieByTitle.Data> call = apolloClient.newCall(MovieByTitle.builder()
                .title("The Matrix")
                .build());

        CountDownLatch latch = new CountDownLatch(1);
        call.enqueue(new ApolloCall.Callback<MovieByTitle.Data>() {
            @Override
            public void onResponse(@Nonnull Response<MovieByTitle.Data> response) {
                MovieByTitle.Data.MovieByTitle1 movieByTitle = response.data().movieByTitle();
                System.err.println(movieByTitle.title());
                System.err.println(movieByTitle.actors());
                latch.countDown();
            }

            @Override
            public void onFailure(@Nonnull ApolloException e) {
                e.printStackTrace();
            }
        });
        latch.await();
    }
}
