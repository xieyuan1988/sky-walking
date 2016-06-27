package com.ai.cloud.skywalking.analysis.chainbuild.entity;

import com.ai.cloud.skywalking.analysis.chainbuild.po.ChainNode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;

public class ChainNodeSpecificHourSummary {
    /**
     * key : 小时
     */
    private Map<String, ChainNodeSpecificTimeWindowSummaryValue> summaryValueMap;

    public ChainNodeSpecificHourSummary(String originData) {
        JsonObject jsonObject = (JsonObject) new JsonParser().parse(originData);
        summaryValueMap = new Gson().fromJson(jsonObject.get("summaryValueMap").toString(),
                new TypeToken<Map<String, ChainNodeSpecificTimeWindowSummaryValue>>() {
                }.getType());
    }

    public ChainNodeSpecificHourSummary() {
        summaryValueMap = new HashMap<String, ChainNodeSpecificTimeWindowSummaryValue>();
    }

    public void summary(String minute, ChainNode node) {
        ChainNodeSpecificTimeWindowSummaryValue summarValue = summaryValueMap.get(minute);
        if (summarValue == null) {
            summarValue = new ChainNodeSpecificTimeWindowSummaryValue();
            summaryValueMap.put(minute, summarValue);
        }

        summarValue.summary(node);
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
