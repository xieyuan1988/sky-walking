package com.ai.cloud.skywalking.analysis.chainbuild.po;

import com.ai.cloud.skywalking.analysis.chainbuild.entity.CallChainDetailForMysql;
import com.ai.cloud.skywalking.analysis.chainbuild.util.HBaseUtil;
import com.ai.cloud.skywalking.analysis.config.HBaseTableMetaData;
import com.google.gson.Gson;
import org.apache.hadoop.hbase.client.Put;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class SpecificTimeCallChainTreeContainer {

    private String treeToken;

    private Map<String, List<String>> hasBeenMergedChainIds;

    private Map<String, CallChainDetailForMysql> newChain4DB;

    // 本次Reduce合并过的调用链
    private Map<String, ChainInfo> newChains;

    public SpecificTimeCallChainTreeContainer(String treeToken) {
        this.treeToken = treeToken;
        hasBeenMergedChainIds = new HashMap<String, List<String>>();
        newChains = new HashMap<String, ChainInfo>();
        newChain4DB = new HashMap<String, CallChainDetailForMysql>();
    }

    public void addChainIfNew(ChainInfo chainInfo) throws IOException {
        String key = generateKey(chainInfo.getStartDate());
        List<String> cIds = hasBeenMergedChainIds.get(key);
        if (cIds == null) {
            cIds = HBaseUtil.loadHasBeenMergeChainIds(key);
            hasBeenMergedChainIds.put(key, cIds);
        }

        if (!cIds.contains(chainInfo.getCID())) {
            cIds.add(chainInfo.getCID());
            newChains.put(chainInfo.getCID(), chainInfo);

            if (chainInfo.getChainStatus() == ChainInfo.ChainStatus.NORMAL) {
                newChain4DB.put(chainInfo.getCID(), new CallChainDetailForMysql(chainInfo, treeToken));
            }
        }
    }


    private String generateKey(long date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(date));
        return treeToken + "@" + calendar.get(Calendar.YEAR) + "-" + calendar.get(Calendar.MONTH);
    }

    public void saveToHBase() throws IOException, InterruptedException, SQLException {
        batchSaveNewChainsInfo();
        batchSaveMergedChainId();
        batchSaveToMysql();
    }

    private void batchSaveToMysql() throws SQLException {
        for (Map.Entry<String, CallChainDetailForMysql> entry : newChain4DB.entrySet()) {
            entry.getValue().saveToMysql();
        }
    }

    /**
     * 保存被合并的cid信息列表
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void batchSaveMergedChainId() throws IOException, InterruptedException {
        List<Put> chainIdPuts = new ArrayList<Put>();
        for (Map.Entry<String, List<String>> entry : hasBeenMergedChainIds.entrySet()) {
            Put chainIdPut = new Put(entry.getKey().getBytes());
            chainIdPut.addColumn(HBaseTableMetaData.TABLE_CALL_CHAIN_TREE_ID_AND_CID_MAPPING.COLUMN_FAMILY_NAME.getBytes()
                    , HBaseTableMetaData.TABLE_CALL_CHAIN_TREE_ID_AND_CID_MAPPING.COLUMN_NAME.getBytes(), new Gson().toJson(entry.getValue()).getBytes());
            chainIdPuts.add(chainIdPut);
        }

        HBaseUtil.batchSaveHasBeenMergedCID(chainIdPuts);
    }

    /**
     * 保存已经合并的调用链信息，包含调用链明细
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void batchSaveNewChainsInfo() throws IOException, InterruptedException {
        List<Put> chainInfoPuts = new ArrayList<Put>();
        for (Map.Entry<String, ChainInfo> entry : newChains.entrySet()) {
            Put put = new Put(entry.getKey().getBytes());
            entry.getValue().saveToHBase(put);
            chainInfoPuts.add(put);
        }
        HBaseUtil.batchSaveChainInfo(chainInfoPuts);
    }

}
