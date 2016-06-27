package com.ai.cloud.skywalking.analysis.chainbuild.po;

import com.ai.cloud.skywalking.analysis.chainbuild.action.IStatisticsAction;
import com.ai.cloud.skywalking.analysis.chainbuild.action.impl.NumberOfCalledStatisticsAction;
import com.ai.cloud.skywalking.analysis.chainbuild.action.impl.CallChainRelationshipAction;

import java.io.IOException;

public enum SummaryType {
    HOUR('H'), DAY('D'), MONTH('M'), YEAR('Y'), RELATIONSHIP('R');

    private char value;

    SummaryType(char value) {
        this.value = value;
    }

    public char getValue() {
        return value;
    }

    public static IStatisticsAction chooseSummaryAction(String summaryTypeAndDateStr, String entryKey) throws IOException {
        char valueChar = summaryTypeAndDateStr.charAt(0);
        // HOUR : 2016-05-02/12
        // DAY : 2016-05-02
        // MONTH : 2016-05
        // YEAR : 2016
        // RELATIONSHIP : treeId
        String summaryDateStr = summaryTypeAndDateStr.substring(summaryTypeAndDateStr.indexOf("-") + 1);
        SummaryType type = null;
        switch (valueChar) {
            case 'H':
                type = HOUR;
                break;
            case 'D':
                type = DAY;
                break;
            case 'M':
                type = MONTH;
                break;
            case 'Y':
                type = YEAR;
                break;
            case 'R':
                return new CallChainRelationshipAction(entryKey);
            default:
                throw new RuntimeException("Can not find the summary type[" + valueChar + "]");
        }
        NumberOfCalledStatisticsAction summaryAction = new NumberOfCalledStatisticsAction(entryKey, summaryDateStr);
        summaryAction.setSummaryType(type);
        return summaryAction;
    }
}
