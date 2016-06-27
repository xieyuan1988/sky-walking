package com.ai.cloud.skywalking.plugin.dubbo;

import com.ai.cloud.skywalking.buriedpoint.RPCBuriedPointReceiver;
import com.ai.cloud.skywalking.buriedpoint.RPCBuriedPointSender;
import com.ai.cloud.skywalking.conf.AuthDesc;
import com.ai.cloud.skywalking.model.ContextData;
import com.ai.cloud.skywalking.model.Identification;
import com.ai.cloud.skywalking.plugin.dubbox.bugfix.below283.BugFixAcitve;
import com.ai.cloud.skywalking.plugin.dubbox.bugfix.below283.SWBaseBean;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;

@Activate
public class SWDubboEnhanceFilter implements Filter {

    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (!AuthDesc.isAuth()) {
            return invoker.invoke(invocation);
        }

        RpcContext context = RpcContext.getContext();
        boolean isConsumer = context.isConsumerSide();
        Result result = null;
        if (isConsumer) {
            RPCBuriedPointSender sender = new RPCBuriedPointSender();

            ContextData contextData = sender.beforeSend(createIdentification(invoker, invocation));
            String contextDataStr = contextData.toString();

            //追加参数
            if (!BugFixAcitve.isActive) {
                // context.setAttachment("contextData", contextDataStr);
                // context的setAttachment方法在重试机制的时候并不会覆盖原有的Attachment
                // 参见Dubbo源代码：“com.alibaba.dubbo.rpc.RpcInvocation”
                //  public void setAttachmentIfAbsent(String key, String value) {
                //      if (attachments == null) {
                //          attachments = new HashMap<String, String>();
                //      }
                //      if (! attachments.containsKey(key)) {
                //          attachments.put(key, value);
                //      }
                //  }
                // 在Rest模式中attachment会被抹除，不会传入到服务端
                // Rest模式会将attachment存放到header里面，具体见com.alibaba.dubbo.rpc.protocol.rest.RpcContextFilter
                //invocation.getAttachments().put("contextData", contextDataStr);
                context.getAttachments().put("contextData", contextDataStr);
            } else {
                fix283SendNoAttachmentIssue(invocation, contextDataStr);
            }

            try {
                //执行结果
                result = invoker.invoke(invocation);
                //结果是否包含异常
                if (result.getException() != null) {
                    sender.handleException(result.getException());
                }
            } catch (RpcException e) {
                // 自身异常
                sender.handleException(e);
                throw e;
            } finally {
                sender.afterSend();
            }
        } else {
            // 读取参数
            RPCBuriedPointReceiver rpcBuriedPointReceiver = new RPCBuriedPointReceiver();
            String contextDataStr;

            if (!BugFixAcitve.isActive) {
                contextDataStr = context.getAttachment("contextData");
            } else {
                contextDataStr = fix283RecvNoAttachmentIssue(invocation);
            }

            ContextData contextData = null;
            if (contextDataStr != null && contextDataStr.length() > 0) {
                contextData = new ContextData(contextDataStr);
            }

            rpcBuriedPointReceiver.beforeReceived(contextData, createIdentification(invoker, invocation));

            try {
                //执行结果
                result = invoker.invoke(invocation);
                //结果是否包含异常
                if (result.getException() != null) {
                    rpcBuriedPointReceiver.handleException(result.getException());
                }
            } catch (RpcException e) {
                // 自身异常
                rpcBuriedPointReceiver.handleException(e);
                throw e;
            } finally {
                rpcBuriedPointReceiver.afterReceived();
            }
        }

        return result;
    }

    private static Identification createIdentification(Invoker<?> invoker, Invocation invocation) {
        StringBuilder viewPoint = new StringBuilder();
        viewPoint.append(invoker.getUrl().getProtocol() + "://");
        viewPoint.append(invoker.getUrl().getHost());
        viewPoint.append(":" + invoker.getUrl().getPort());
        viewPoint.append(invoker.getUrl().getAbsolutePath());
        viewPoint.append("." + invocation.getMethodName() + "(");
        for (Class<?> classes : invocation.getParameterTypes()) {
            viewPoint.append(classes.getSimpleName() + ",");
        }

        if (invocation.getParameterTypes().length > 0) {
            viewPoint.delete(viewPoint.length() - 1, viewPoint.length());
        }

        viewPoint.append(")");
        return Identification.newBuilder().viewPoint(viewPoint.toString()).spanType(DubboBuriedPointType.instance()).build();
    }


    private static void fix283SendNoAttachmentIssue(Invocation invocation, String contextDataStr) {

        for (Object parameter : invocation.getArguments()) {
            if (parameter instanceof SWBaseBean) {
                ((SWBaseBean) parameter).setContextData(contextDataStr);
                return;
            }
        }
    }

    private static String fix283RecvNoAttachmentIssue(Invocation invocation) {
        for (Object parameter : invocation.getArguments()) {
            if (parameter instanceof SWBaseBean) {
                return ((SWBaseBean) parameter).getContextData();
            }
        }

        return null;
    }
}