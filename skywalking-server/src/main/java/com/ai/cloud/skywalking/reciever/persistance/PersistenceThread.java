package com.ai.cloud.skywalking.reciever.persistance;

import com.ai.cloud.skywalking.reciever.conf.Config;
import com.ai.cloud.skywalking.reciever.selfexamination.ServerHealthCollector;
import com.ai.cloud.skywalking.reciever.selfexamination.ServerHeathReading;
import com.ai.cloud.skywalking.reciever.storage.StorageChainController;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.NameFileComparator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

import static com.ai.cloud.skywalking.reciever.conf.Config.Persistence.*;

public class PersistenceThread extends Thread {

	private Logger logger = LogManager.getLogger(PersistenceThread.class);

	PersistenceThread(int threadIdx) {
		super("PersistenceThread_" + threadIdx);
	}

	@Override
	public void run() {
		File file1 = null;
		BufferedReader bufferedReader = null;
		int offset;
		while (true) {
			try {
				file1 = getDataFiles();
				if (file1 == null) {
					try {
						Thread.sleep(SWITCH_FILE_WAIT_TIME);
					} catch (InterruptedException e) {
						logger.error("Failure sleep", e);
					}
					continue;
				}

				bufferedReader = new BufferedReader(new FileReader(file1));
				offset = moveOffSet(file1, bufferedReader);
				if (logger.isDebugEnabled()) {
					logger.debug("Get file[{}] offset [{}]", file1.getName(),
							offset);
				}
				StringBuilder stringBuilder = new StringBuilder(
						MAX_STORAGE_SIZE_PER_TIME);
				String tmpData;
				while (true) {
					tmpData = bufferedReader.readLine();
					// 文件结束
					if (tmpData == null) {
						if (stringBuilder != null && stringBuilder.length() > 0) {
							MemoryRegister.instance().updateOffSet(
									file1.getName(), offset);
							StorageChainController.doStorage(stringBuilder
									.toString());
							stringBuilder.delete(0, stringBuilder.length());
						}

						try {
							Thread.sleep(READ_ENDING_FILE_MAX_WAITE_TIME);
						} catch (InterruptedException e) {
							logger.error("Sleep failed", e);
						}

						continue;
					}

					// 文件读入/n字符串
					if (tmpData.length() <= 0) {
						// 加上回车的字符串长度
						offset += 1;
						continue;
					}

					ServerHealthCollector.getCurrentHeathReading(null)
							.updateData(
									ServerHeathReading.INFO,
									"read " + tmpData.length()
											+ " chars from local file:"
											+ file1.getName());

					if ("EOF".equals(tmpData)) {
						if (stringBuilder != null && stringBuilder.length() > 0) {
							StorageChainController.doStorage(stringBuilder
									.toString());
						}

						bufferedReader.close();
						logger.info(
								"Data in file[{}] has been successfully processed",
								file1.getName());
						boolean deleteSuccess = false;
						while (!deleteSuccess) {
							deleteSuccess = FileUtils.deleteQuietly(new File(
									file1.getParent(), file1.getName()));
						}
						logger.info("Delete file[{}] {}", file1.getName(),
								(deleteSuccess ? "success" : "failed"));

						MemoryRegister.instance().removeEntry(file1.getName());
						break;
					}

					if (stringBuilder.length() + tmpData.length() >= MAX_STORAGE_SIZE_PER_TIME) {
						StorageChainController.doStorage(stringBuilder
								.toString());
						stringBuilder.delete(0, stringBuilder.length());
						MemoryRegister.instance().updateOffSet(file1.getName(),
								offset);
					}

					stringBuilder.append(tmpData);
					// 加上回车的字符串长度
					offset += tmpData.length() + 1;
				}
			} catch (FileNotFoundException e) {
				logger.error("The data file could not be found.", e);
				ServerHealthCollector.getCurrentHeathReading(null).updateData(
						ServerHeathReading.ERROR, e.getMessage());
			} catch (IOException e) {
				logger.error("The data file I/O exception.", e);
				ServerHealthCollector.getCurrentHeathReading(null).updateData(
						ServerHeathReading.ERROR, e.getMessage());
			} catch (Throwable t) {
				logger.error(t.getMessage(), t);
				ServerHealthCollector.getCurrentHeathReading(null).updateData(
						ServerHeathReading.ERROR, t.getMessage());
			} finally {
				try{
					if (file1 != null) {
						MemoryRegister.instance().unRegister(file1.getName());
					}
				}catch (Throwable t) {
					logger.error("unRegister file[{}] failure", file1.getName(), t);
				}
				try {
					if (bufferedReader != null)
						bufferedReader.close();
				} catch (IOException e) {
					logger.error("can't close data file", e);
				}
			}

			try {
				Thread.sleep(SWITCH_FILE_WAIT_TIME);
			} catch (InterruptedException e) {
				logger.error("Failure sleep.", e);
			}
		}
	}

	private int moveOffSet(File file1, BufferedReader bufferedReader)
			throws IOException {
		int offset = MemoryRegister.instance().getOffSet(file1.getName());
		if (-1 == offset || offset == 0) {
			offset = 0;
		} else {
			char[] cha = new char[STEP_SIZE_FOR_LOCATING_FILE_OFFSET];
			int length = 0;
			while (length + STEP_SIZE_FOR_LOCATING_FILE_OFFSET < offset) {
				length += STEP_SIZE_FOR_LOCATING_FILE_OFFSET;
				bufferedReader.read(cha);
			}
			bufferedReader.read(cha, 0, Math.abs(offset - length));
			cha = null;
		}
		return offset;
	}

	private File getDataFiles() {
		File file1 = null;
		File parentDir = new File(
				Config.Buffer.DATA_BUFFER_FILE_PARENT_DIRECTORY);
		NameFileComparator sizeComparator = new NameFileComparator();
		File[] dataFileList = sizeComparator.sort(parentDir.listFiles());
		for (File file : dataFileList) {
			if (file.getName().startsWith(".")) {
				continue;
			}
			if (MemoryRegister.instance().doRegister(file.getName()) == null) {
				if (logger.isDebugEnabled())
					logger.debug(
							"The file [{}] is being used by another thread ",
							file);
				continue;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Begin to deal data file [{}]", file.getName());
			}
			file1 = file;
			break;
		}

		return file1;
	}
}
