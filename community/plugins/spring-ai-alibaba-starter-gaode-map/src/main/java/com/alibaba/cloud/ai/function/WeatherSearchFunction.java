package com.alibaba.cloud.ai.function;

import com.alibaba.cloud.ai.properties.GaoDeProperties;
import com.alibaba.cloud.ai.service.WebService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.function.Function;

/**
 * @author YunLong
 */
public class WeatherSearchFunction implements Function<WeatherSearchFunction.Request, WeatherSearchFunction.Response> {

    private final WebService webService;

    public WeatherSearchFunction(GaoDeProperties gaoDeProperties) {
        this.webService = new WebService(gaoDeProperties);
    }

    @Override
    public Response apply(Request request) {

        String responseBody = webService.getAddressCityCode(request.address);

        String adcode = "";

        try {
            JSONObject jsonObject = JSON.parseObject(responseBody);
            JSONArray geocodesArray = jsonObject.getJSONArray("geocodes");
            if (geocodesArray != null && !geocodesArray.isEmpty()) {
                JSONObject firstGeocode = geocodesArray.getJSONObject(0);
                adcode = firstGeocode.getString("adcode");
            }
        } catch (Exception e){
            return new Response("Error occurred while processing the request.");
        }

        String weather = webService.getWeather(adcode);

        return new Response(weather);
    }

    @JsonClassDescription("Get the weather conditions for a specified address.")
    public record Request(
            @JsonProperty(required = true, value = "address")
            @JsonPropertyDescription("The address") String address) {
    }

    public record Response(String message) {
    }
}
