package com.ai.cloud.skywalking.alarm.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ai.cloud.skywalking.alarm.conf.Config;
import com.ai.cloud.skywalking.alarm.dao.SystemConfigDao;
import com.ai.cloud.skywalking.alarm.model.AlarmType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class AlarmTypeUtil {

	private static Logger logger = LogManager.getLogger(AlarmTypeUtil.class);
	private static List<AlarmType> alarmTypeList;

	static {
		try {
			String typeInfo = SystemConfigDao.getSystemConfig(Config.Alarm.ALARM_TYPE_CONFIG_ID);
			alarmTypeList = new Gson().fromJson(typeInfo, new TypeToken<ArrayList<AlarmType>>() {
			}.getType());
		} catch (Exception e) {
			logger.error("Failed to load alarm type info.", e);
			System.exit(-1);
		}
	}

	public static List<AlarmType> getAlarmTypeList() {
		
		if (alarmTypeList == null || alarmTypeList.isEmpty()) {
			alarmTypeList = new ArrayList<AlarmType>();
		} 
    	
    	return alarmTypeList;
	}
}
