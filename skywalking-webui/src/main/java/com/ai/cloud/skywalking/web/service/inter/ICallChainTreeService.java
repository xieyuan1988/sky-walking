package com.ai.cloud.skywalking.web.service.inter;

import com.ai.cloud.skywalking.web.entity.BreviaryChainTree;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by xin on 16-4-6.
 */
public interface ICallChainTreeService {
    List<BreviaryChainTree> queryCallChainTreeByKey(String uid, String viewpoint, int pageSize) throws SQLException, IOException;
}
