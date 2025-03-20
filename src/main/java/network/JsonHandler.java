package network;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Provides utility methods for converting between JSON strings and Java objects.
 */
public class JsonHandler {
    private static final Gson gson = new Gson();

    /**
     * Converts Json string -> to specific class
     *
     * @param clazz the class in which the json string should be converted
     * @return an instance of the class clazz
     * @param <T> the type of the object that`s going to be returned
     */
    public static <T> T fromJson(String jsonString, Class<T> clazz) throws JsonSyntaxException {
        return gson.fromJson(jsonString, clazz);
    }

    /**
     * Converts an object -> to Json string
     */
    public static String toJson(Object object) {
        return gson.toJson(object);
    }
}
