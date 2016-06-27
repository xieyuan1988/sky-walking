package com.ai.cloud.skywalking.analysis.chainbuild;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ai.cloud.skywalking.analysis.chainbuild.exception.Tid2CidECovertException;
import com.ai.cloud.skywalking.analysis.chainbuild.filter.SpanNodeProcessChain;
import com.ai.cloud.skywalking.analysis.chainbuild.filter.SpanNodeProcessFilter;
import com.ai.cloud.skywalking.analysis.chainbuild.po.ChainInfo;
import com.ai.cloud.skywalking.analysis.chainbuild.po.ChainNode;
import com.ai.cloud.skywalking.analysis.chainbuild.po.SummaryType;
import com.ai.cloud.skywalking.analysis.chainbuild.util.HBaseUtil;
import com.ai.cloud.skywalking.analysis.chainbuild.util.SubLevelSpanCostCounter;
import com.ai.cloud.skywalking.analysis.chainbuild.util.TokenGenerator;
import com.ai.cloud.skywalking.analysis.chainbuild.util.VersionIdentifier;
import com.ai.cloud.skywalking.analysis.config.ConfigInitializer;
import com.ai.cloud.skywalking.protocol.Span;
import com.ai.cloud.skywalking.util.SpanLevelIdComparators;
import com.google.gson.Gson;

public class ChainBuildMapper extends TableMapper<Text, Text> {

    private Logger logger = LogManager.getLogger(ChainBuildMapper.class);
    private SimpleDateFormat hourSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH");
    private SimpleDateFormat daySimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat monthSimpleDateFormat = new SimpleDateFormat("yyyy-MM");
    private SimpleDateFormat yearSimpleDateFormat = new SimpleDateFormat("yyyy");

    @Override
    protected void setup(Context context) throws IOException,
            InterruptedException {
        ConfigInitializer.initialize();
    }

    @Override
    protected void map(ImmutableBytesWritable key, Result value, Context context)
            throws IOException, InterruptedException {
        if (!VersionIdentifier.enableAnaylsis(Bytes.toString(key.get()))) {
            return;
        }

        List<Span> spanList = new ArrayList<Span>();
        ChainInfo chainInfo = null;
        try {
            for (Cell cell : value.rawCells()) {
                Span span = new Span(Bytes.toString(cell.getValueArray(),
                        cell.getValueOffset(), cell.getValueLength()));
                spanList.add(span);
            }
            if (spanList.size() == 0) {
                throw new Tid2CidECovertException("tid["
                        + Bytes.toString(key.get()) + "] has no span data.");
            }


            if (spanList.size() > 2000) {
                throw new Tid2CidECovertException("tid["
                        + Bytes.toString(key.get()) + "] node size has over 2000.");
            }

            chainInfo = spanToChainInfo(Bytes.toString(key.get()), spanList);
            logger.debug("convert tid[" + Bytes.toString(key.get())
                    + "] to chain with cid[" + chainInfo.getCID() + "].");
            HBaseUtil.saveTraceIdAndTreeIdMapping(Bytes.toString(key.get()), chainInfo.getCID());

            if (chainInfo.getCallEntrance() != null && chainInfo.getCallEntrance().length() > 0) {
                for (ChainNode chainNode : chainInfo.getNodes()) {
                	/**
                	 * TODO: 进一步提高运行速度所需的性能提升
                	 * 此处修改原因，
                	 * 1.更细粒度划分reduce任务，提高性能。
                	 * 2.减少数据传输量，以及处理复杂度。
                	 * 3.请避免使用gson序列化，提高程序处理性能
                	 * 
                	 * hour/day/month/year，
                	 * key 修改为：类型+时间字符+callEntrance+levelId+viewpoint, 
                	 * value 为ChainNodeSpecificTimeWindowSummaryValue中所需的明确的值组成的简单串
                	 *     value包含：
                	 *         1.是否正确调用，由NodeStatus获取，值为N/A/I
                	 *         2.调用所需时间，由cost获取
                	 * 
                	 */
                	
                    context.write(new Text(SummaryType.HOUR.getValue() + "-" + hourSimpleDateFormat.format(
                            new Date(chainNode.getStartDate())
                    ) + ":" + chainInfo.getCallEntrance()), new Text(new Gson().toJson(chainNode)));

                    context.write(new Text(SummaryType.DAY.getValue() + "-" + daySimpleDateFormat.format(
                            new Date(chainNode.getStartDate())
                    ) + ":" + chainInfo.getCallEntrance()), new Text(new Gson().toJson(chainNode)));

                    context.write(new Text(SummaryType.MONTH.getValue() + "-" + monthSimpleDateFormat.format(
                            new Date(chainNode.getStartDate())
                    ) + ":" + chainInfo.getCallEntrance()), new Text(new Gson().toJson(chainNode)));

                    context.write(new Text(SummaryType.YEAR.getValue() + "-" + yearSimpleDateFormat.format(
                            new Date(chainNode.getStartDate())
                    ) + ":" + chainInfo.getCallEntrance()), new Text(new Gson().toJson(chainNode)));
                }
                
                /**
                 * TODO：通过对本地的调用链进行缓存，每个map任务中的调用链，在一个JVM内只会被传递一次，大幅度降低reduce任务的数据量。
                 * 
                 * 1.使用静态变量，缓存MAP中的调用链。每个典型调用链ID只传递一次
                 * 2.注意缓存需要限制容量，初期规划，缓存1W个典型调用链KEY（可通过配置扩展）。仅缓存典型调用链ID，非链路明细。
                 * 3.对于节点数量大于2K条的调用量，暂不进行关系处理
                 * 
                 * 注意：CallChainRelationshipAction暂时不做修改，此处的修改会大规模降低reduce的处理数据量，提高总体运行速度
                 * 
                 */
                // Reduce key : R-CallEntrance
                context.write(new Text(SummaryType.RELATIONSHIP + "-" + TokenGenerator.generateTreeToken(chainInfo.getCallEntrance())
                                + ":" + chainInfo.getCallEntrance()),
                        new Text(new Gson().toJson(chainInfo)));
            }
        } catch (Exception e) {
            logger.error("Failed to mapper call chain[" + key.toString() + "]",
                    e);
        }
    }

    public static ChainInfo spanToChainInfo(String tid, List<Span> spanList) {
        SubLevelSpanCostCounter costMap = new SubLevelSpanCostCounter();
        ChainInfo chainInfo = new ChainInfo(tid);
        Collections.sort(spanList, new SpanLevelIdComparators.SpanASCComparator());

        Map<String, SpanEntry> spanEntryMap = mergeSpanDataSet(spanList);
        for (Map.Entry<String, SpanEntry> entry : spanEntryMap.entrySet()) {
            ChainNode chainNode = new ChainNode();
            SpanNodeProcessFilter filter = SpanNodeProcessChain
                    .getProcessChainByCallType(entry.getValue().getSpanType());
            filter.doFilter(entry.getValue(), chainNode, costMap);
            chainInfo.addNodes(chainNode);
        }
        chainInfo.generateChainToken();
        return chainInfo;
    }

    private static Map<String, SpanEntry> mergeSpanDataSet(List<Span> spanList) {
        Map<String, SpanEntry> spanEntryMap = new LinkedHashMap<String, SpanEntry>();
        for (int i = spanList.size() - 1; i >= 0; i--) {
            Span span = spanList.get(i);
            SpanEntry spanEntry = spanEntryMap.get(span.getParentLevel() + "."
                    + span.getLevelId());
            if (spanEntry == null) {
                spanEntry = new SpanEntry();
                spanEntryMap.put(
                        span.getParentLevel() + "." + span.getLevelId(),
                        spanEntry);
            }
            spanEntry.setSpan(span);
        }
        return spanEntryMap;
    }
}
