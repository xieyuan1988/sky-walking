package com.ai.cloud.skywalking.web.controller;

import com.ai.cloud.skywalking.web.common.BaseController;
import com.ai.cloud.skywalking.web.dto.CallChainTree;
import com.ai.cloud.skywalking.web.dto.TypicalCallTree;
import com.ai.cloud.skywalking.web.service.inter.IAnalysisResultService;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Created by xin on 16-4-5.
 */
@RequestMapping("/analy")
@Controller
public class AnalysisResultController extends BaseController {

    private Logger logger = LogManager.getLogger(AnalysisResultController.class);

    @Autowired
    private IAnalysisResultService analysisResultService;

    @RequestMapping("/mainPage")
    public String analysisResult(String treeId, HttpServletRequest request) {
        request.setAttribute("treeId", treeId);
        return "anls-result/analysisResult";
    }

    /**
     * Analysis Type:   MONTH,     DAY,       HOUR
     * analysis Date: 2015-07  2015-07-17  2015-07-17:18
     */
    @RequestMapping(value = "/load/{treeId}/{analyType}/{analyDate}", produces = "application/json; charset=UTF-8")
    @ResponseBody
    public String loadAnalysisResult(HttpServletRequest request,
                                     @PathVariable("treeId") String treeId,
                                     @PathVariable("analyType") String analyType,
                                     @PathVariable("analyDate") String analyDate) {
        JSONObject result = new JSONObject();
        try {
            // LoginUserInfo userInfo = fetchLoginUserInfoFromSession(request);
            CallChainTree callChainTree = analysisResultService.
                    fetchAnalysisResult(treeId, analyType, analyDate);
            result.put("code", "200");
            if (callChainTree != null) {
                result.put("result", new Gson().toJson(callChainTree));
            } else {
                result.put("result", "{}");
            }
        } catch (Exception e) {
            logger.error("Failed to load treeId[{}], anlysisType:[{}], anlyDate:[{}]", treeId, analyType, analyDate);
            e.printStackTrace();
            result.put("code", "500");
            result.put("message", "Fatal error");
        }
        return result.toJSONString();
    }

    @RequestMapping(value = "/load/{treeId}/{analyDate}", produces = "application/json; charset=UTF-8")
    @ResponseBody
    public String loadTypicalCallTree(HttpServletRequest request,
                                      @PathVariable("treeId") String treeId,
                                      @PathVariable("analyDate") String analyDate) {
        JSONObject result = new JSONObject();

        try {
            List<TypicalCallTree> typicalCallTrees = analysisResultService.fetchTypicalCallTrees(treeId, analyDate);
            result.put("code", "200");
            if (typicalCallTrees != null) {
                result.put("result", new Gson().toJson(typicalCallTrees));
            } else {
                result.put("result", "{}");
            }
        } catch (Exception e) {
            logger.error("Failed to load treeId[{}], anlyDate:[{}]", treeId, analyDate);
            e.printStackTrace();
            result.put("code", "500");
            result.put("message", "Fatal error");
        }
        return result.toJSONString();
    }
}
