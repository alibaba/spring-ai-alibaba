package com.alibaba.cloud.ai.functioncalling.amp;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.function.Function;

/**
 * @author YunLong
 */
public class WeatherSearchService implements Function<WeatherSearchService.Request, WeatherSearchService.Response> {

	private final WeatherTools weatherTools;

	public WeatherSearchService(AmapProperties amapProperties) {
		this.weatherTools = new WeatherTools(amapProperties);
	}

	@Override
	public Response apply(Request request) {

		String responseBody = weatherTools.getAddressCityCode(request.address);

		String adcode = "";

		try {
			JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
			JsonArray geocodesArray = jsonObject.getAsJsonArray("geocodes");
			if (geocodesArray != null && !geocodesArray.isEmpty()) {
				JsonObject firstGeocode = geocodesArray.get(0).getAsJsonObject();
				adcode = firstGeocode.get("adcode").getAsString();
			}
		}
		catch (Exception e) {
			return new Response("Error occurred while processing the request.");
		}

		String weather = weatherTools.getWeather(adcode);

		return new Response(weather);
	}

	@JsonClassDescription("Get the weather conditions for a specified address.")
	public record Request(
			@JsonProperty(required = true, value = "address") @JsonPropertyDescription("The address") String address) {
	}

	public record Response(String message) {
	}

}
