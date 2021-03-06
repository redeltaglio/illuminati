package me.phoboslabs.illuminati.processor.infra.backup.shutdown.handler.impl;

import me.phoboslabs.illuminati.processor.executor.impl.IlluminatiTemplateExecutorImpl;
import me.phoboslabs.illuminati.processor.infra.backup.shutdown.handler.ContainerShutdownHandler;

/**
 *  - @marcus.moon provided me with an Graceful idea.
 *
 * Created by leekyoungil (leekyoungil@gmail.com) on 04/05/2018.
 */
public class IlluminatiShutdownHandler implements ContainerShutdownHandler {

    // ################################################################################################################
    // ### init illuminati template executor                                                                        ###
    // ################################################################################################################
    private IlluminatiTemplateExecutorImpl illuminatiTemplateExecutor;

    public IlluminatiShutdownHandler (IlluminatiTemplateExecutorImpl illuminatiExecutor) {
        this.illuminatiTemplateExecutor = illuminatiExecutor;
    }

    @Override public boolean isRunning() {
        int templateQueueSize = this.illuminatiTemplateExecutor.getQueueSize();
        int backupQueueSize = this.illuminatiTemplateExecutor.getBackupQueueSize();

        return templateQueueSize > 0 && backupQueueSize > 0;
    }

    @Override public void stop() {
        this.illuminatiTemplateExecutor.connectionClose();
    }

    @Override public void stopSignal() {
        this.illuminatiTemplateExecutor.executeStopThread();
    }
}
