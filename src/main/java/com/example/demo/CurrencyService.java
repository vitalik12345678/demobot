package com.example.demo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CurrencyService {

    public static Map<String, Float> getCurrencyRates() throws IOException {
        try (InputStream inputStream = new URL("https://api.mexc.com/api/v3/ticker/price").openStream();
             Scanner scanner = new Scanner(inputStream)) {
            String result = scanner.useDelimiter("\\A").next();

            JSONArray jsonArray = new JSONArray(result);

            Map<String, Float> currencyMap = new HashMap<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String symbol = jsonObject.getString("symbol");
                String priceStr = jsonObject.getString("price");

                try {
                    Float price = Float.parseFloat(priceStr);
                    currencyMap.put(symbol, price);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Failed to parse price: " + priceStr);
                }
            }

            return currencyMap;
        }
    }
}
