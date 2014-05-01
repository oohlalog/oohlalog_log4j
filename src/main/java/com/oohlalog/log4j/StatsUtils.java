package com.oohlalog.log4j;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
public class StatsUtils {
	private static Object sysmonInstance = getJavaSysMonInstance("JavaSysMon");
	private static Class sysmonClass = getJavaSysMonClass("JavaSysMon");
	private static Class memoryStatsClass = getJavaSysMonClass("MemoryStats");
	private static Class cpuTimesClass = getJavaSysMonClass("CpuTimes");

	public static Map<String,Double> getStats(OohLaLogAppender logger) {
		Map<String, Double> map = new HashMap<String, Double>();
		if (logger.getMemoryStats()) getMemoryStats(logger, map);
		if (logger.getFileSystemStats()) getFileSystemStats(logger, map);
		if (logger.getCpuStats()) getCpuStats(logger, map);
		return map;
	}

	public static Map<String,Double> getMemoryStats(OohLaLogAppender logger, Map<String,Double> map) {
		if (map == null) map = new HashMap<String, Double>();
		Runtime runtime = Runtime.getRuntime();
		map.put("memory.maxMemory", new Double(runtime.maxMemory()));
		map.put("memory.freeMemory", new Double(runtime.freeMemory()));
		map.put("memory.totalMemory", new Double(runtime.totalMemory()));
		map.put("memory.usedMemory", new Double(runtime.totalMemory() - runtime.freeMemory()));

		try {
			Object physicalMemoryResults = getJavaSysMonResult("physical");
			if (physicalMemoryResults != null) {
				map.put("memory.physical.freeBytes", new Double(getJavaSysMonResultProperty(physicalMemoryResults, memoryStatsClass, "getFreeBytes").toString() ));
				map.put("memory.physical.totalBytes", new Double(getJavaSysMonResultProperty(physicalMemoryResults, memoryStatsClass, "getTotalBytes").toString() ));
			}
			Object swapMemoryResults = getJavaSysMonResult("swap");
			if (swapMemoryResults != null) {
				map.put("memory.swap.freeBytes", new Double(getJavaSysMonResultProperty(swapMemoryResults, memoryStatsClass, "getFreeBytes").toString() ));
				map.put("memory.swap.totalBytes", new Double(getJavaSysMonResultProperty(swapMemoryResults, memoryStatsClass, "getTotalBytes").toString() ));
			}
		} catch (RuntimeException e) {
			if (logger.getDebug()) System.err.println(e.getMessage());
		}
		return map;
	}

	public static Map<String,Double> getCpuStats(OohLaLogAppender logger, Map<String,Double> map ) {
		if (map == null) map = new HashMap<String, Double>();

		try {
			Object results = getJavaSysMonResult("cpuTimes");
			if (results != null) {
				map.put("cpu.idleMillis", new Double(getJavaSysMonResultProperty(results, cpuTimesClass, "getIdleMillis").toString() ));
				map.put("cpu.totalMillis", new Double(getJavaSysMonResultProperty(results, cpuTimesClass, "getTotalMillis").toString() ));
				map.put("cpu.systemMillis", new Double(getJavaSysMonResultProperty(results, cpuTimesClass, "getSystemMillis").toString() ));
				map.put("cpu.userMillis", new Double(getJavaSysMonResultProperty(results, cpuTimesClass, "getUserMillis").toString() ));
				if (logger.previousCpuUsage != null) {
					try {
						Method method = cpuTimesClass.getMethod("getCpuUsage", cpuTimesClass);
						map.put("cpu.cpuUsage", new Double(method.invoke(results, logger.previousCpuUsage).toString()));

					} catch (Throwable t) {
						if (logger.getDebug()) System.err.println(t.getMessage());
					}
				}
				logger.previousCpuUsage = results;
			}
		} catch (RuntimeException e) {
			if (logger.getDebug()) System.err.println(e.getMessage());
		}
		return map;
	}


	public static Map<String,Double> getFileSystemStats(OohLaLogAppender logger, Map<String,Double> map) {
		if (map == null) map = new HashMap<String, Double>();
		File[] paths;
		try {      
			// returns pathnames for files and directory
			paths = File.listRoots();
			for(File path:paths) {
				map.put("fileSystem."+path.toString() + ".totalSpace", new Double(path.getTotalSpace()));
				map.put("fileSystem."+path.toString() + ".usableSpace", new Double(path.getUsableSpace()));
				map.put("fileSystem."+path.toString() + ".freeSpace", new Double(path.getFreeSpace()));
			}
		} catch(Throwable e){
			if (logger.getDebug()) System.err.println(e.getMessage());
		}		
		return map;
	}

	private static Class getJavaSysMonClass(String className) {
		try {
			return Class.forName("com.jezhumble.javasysmon."+className);
		} catch (Throwable e) {
			//System.err.println(e.getMessage());
			return null;
		}
	}

	private static Object getJavaSysMonInstance(String className) {
		try {
			return Class.forName("com.jezhumble.javasysmon."+className).newInstance();
		} catch (Throwable e) {
			//System.err.println(e.getMessage());
			return null;
		}
	}


	private static Object getJavaSysMonResult(String methodName) {
		Object rtn = null;
		try {
			if (sysmonClass !=  null) {
				Method method = sysmonClass.getMethod(methodName);
				rtn = method.invoke(sysmonInstance);
			}
		} catch (Throwable e) {
			//System.err.println(e.getMessage());

		}
		return rtn;
	}

	private static Object getJavaSysMonResultProperty(Object result, Class clazz, String propertyName) {
		Object rtn = null;
		try {
			if (clazz !=  null) {
				Method method = clazz.getMethod(propertyName);
				rtn = method.invoke(result);
			}
		} catch (Throwable e) {
			//System.err.println(e.getMessage());
		}
		return rtn;

	}

}