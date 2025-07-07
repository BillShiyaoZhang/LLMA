package LLMABelief;

import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.data.model.WeaviateObject;
import io.weaviate.client.v1.filters.Operator;
import io.weaviate.client.v1.filters.WhereFilter;
import io.weaviate.client.v1.graphql.model.GraphQLResponse;
import io.weaviate.client.v1.graphql.query.argument.NearVectorArgument;
import io.weaviate.client.v1.graphql.query.builder.GetBuilder;
import io.weaviate.client.v1.graphql.query.fields.Field;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/***
 * @See https://weaviate.io/developers/weaviate/api/graphql for more information.
 */
public class Weaviate {

    public static final WeaviateClient client = new WeaviateClient(new Config("http", "localhost:8080"));
    private String collectionName;
    public Weaviate(){}
    public Weaviate(String collectionName){
        this.collectionName = collectionName;
    }

    public static void print(WeaviateObject obj) {
//        System.out.println(toString(obj));
        System.out.println(toJsonString(obj));
    }

    public static String toString(WeaviateObject obj) {
        final String[] str = {"id: " + obj.getId() + "\n" +
                "vector: " + Arrays.toString(obj.getVector()) + "\n" +
                "properties:\n"};
        obj.getProperties().forEach((k, v) -> {
            str[0] = str[0] + "  " + k + ": " + v.toString() + "\n";
        });
        return str[0];
    }

    public static String toJsonString(WeaviateObject obj) {
        return new GsonBuilder().create().toJson(obj);
//        return new GsonBuilder().setPrettyPrinting().create().toJson(obj);
    }

    public Float[] getEmbedding(String uri){
        Field uriField = Field.builder().name("uri").build();
        Field _additional = Field.builder()
                .name("_additional")
                .fields(new Field[]{
                        Field.builder().name("vector").build(),
                }).build();
        WhereFilter where = WhereFilter.builder()
                .path(new String[]{ "uri" })
                .operator(Operator.Equal)
                .valueString(uri)
                .build();

        Result<GraphQLResponse> result = client.graphQL().get()
                .withClassName(collectionName)
                .withFields(uriField, _additional)
                .withWhere(where)
                .run();
        if (result.hasErrors()) {
            System.out.println(result.getError());
            return null;
        }
        var data = (ArrayList<Double>) ((LinkedTreeMap) ((ArrayList<LinkedTreeMap>) ((LinkedTreeMap) ((LinkedTreeMap) result.getResult().getData()).get("Get")).get(collectionName)).get(0).get("_additional")).get("vector");
        return data.stream()
                .map(Double::floatValue)
                .toArray(Float[]::new);

    }

    public void add(String collectionName, String uri, Float[] vector) {
        client.data().creator()
                .withClassName(collectionName)
                .withVector(vector)
                .withProperties(new HashMap<String, Object>() {{
                    put("uri", uri);
//                    put("isNegotiated", var.get("isNegotiated")); // will be automatically added as a number property
                }})
                .run();
    }

    public void search(Float[] vector, String className, float threshold){
        NearVectorArgument nearVector = NearVectorArgument.builder()
                .vector(vector)
                .build();

        String query = GetBuilder.builder()
                .className(className)
                .withNearVectorFilter(nearVector)
                .build()
                .buildQuery();

        query = query.replace("{}", ""); // add distance to the query

        Result<GraphQLResponse> result = client.graphQL().raw().withQuery(query).run();
        System.out.println();
    }
}