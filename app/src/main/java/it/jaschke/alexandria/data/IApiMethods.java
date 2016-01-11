package it.jaschke.alexandria.data;

/**
 * Created by Admin on 7/01/2016.
 */
import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;


public interface IApiMethods{

    @GET("books/v1/volumes")
    Call<BookObject> getBooks(
             @Query("q") String isbn,
             @Query("key") String key

    );


}
