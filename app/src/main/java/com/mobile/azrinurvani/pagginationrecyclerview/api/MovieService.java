package com.mobile.azrinurvani.pagginationrecyclerview.api;


import com.mobile.azrinurvani.pagginationrecyclerview.models.TopRatedMovies;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

//todo 9
public interface MovieService {

    @GET("movie/top_rated")
    Call<TopRatedMovies> getTopRatedMovies(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("page") int pageIndex
    );

}
