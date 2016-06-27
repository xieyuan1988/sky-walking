package com.ai.cloud.skywalking.util;

import java.util.Arrays;

public class ProtocolPackager {
    public static byte[] pack(byte[] data) {
        // 对协议格式进行修改
        // | check sum(4 byte) |  data
        byte[] dataPackage = new byte[data.length + 4];

        packDataText(data, dataPackage);
        packCheckSum(data, dataPackage);

        return dataPackage;
    }

    private static void packCheckSum(byte[] data, byte[] dataPackage) {
        byte[] checkSumArray = generateChecksum(data, 0);
        System.arraycopy(checkSumArray, 0, dataPackage, 0, 4);
    }

    private static void packDataText(byte[] data, byte[] dataPackage) {
        System.arraycopy(data, 0, dataPackage, 4, data.length);
    }


    public static byte[] unpack(byte[] dataPackage) {
        if (validateCheckSum(dataPackage)) {
            return unpackDataText(dataPackage);
        } else {
            return null;
        }

    }

    private static byte[] unpackDataText(byte[] dataPackage) {
        byte[] data = new byte[dataPackage.length - 4];
        System.arraycopy(dataPackage, 4, data, 0, data.length);
        return data;
    }

    private static boolean validateCheckSum(byte[] dataPackage){
        byte[] checkSum = generateChecksum(dataPackage, 4);
        byte[] originCheckSum = new byte[4];
        System.arraycopy(dataPackage, 0, originCheckSum, 0, 4);
        return Arrays.equals(checkSum, originCheckSum);
    }

    /**
     * 生成校验和参数
     *
     * @param data
     * @return
     */
    private static byte[] generateChecksum(byte[] data, int offset) {
        int result = data[0];
        for (int i = offset; i < data.length; i++) {
            result ^= data[i];
        }

        return intToBytes(result);
    }

    private static byte[] intToBytes(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) ((value >> 24) & 0xFF);
        src[1] = (byte) ((value >> 16) & 0xFF);
        src[2] = (byte) ((value >> 8) & 0xFF);
        src[3] = (byte) (value & 0xFF);
        return src;
    }

}
