package net.blay09.mods.eirairc.client.screenshot;

import net.blay09.mods.eirairc.api.upload.UploadHoster;
import net.blay09.mods.eirairc.api.upload.UploadedFile;
import net.blay09.mods.eirairc.config.ClientGlobalConfig;
import net.blay09.mods.eirairc.config.ScreenshotAction;

public class AsyncUploadScreenshot implements Runnable {

    private final UploadHoster hoster;
    private final Screenshot screenshot;
    private final ScreenshotAction followUpAction;
    private UploadedFile uploadedFile;
    private boolean complete;

    public AsyncUploadScreenshot(UploadHoster hoster, Screenshot screenshot, ScreenshotAction followUpAction) {
        this.hoster = hoster;
        this.screenshot = screenshot;
        this.followUpAction = followUpAction;
        Thread thread = new Thread(this, "ScreenshotUpload");
        thread.start();
    }

    @Override
    public void run() {
        uploadedFile = hoster.uploadFile(screenshot.getFile(), ClientGlobalConfig.uploadBufferSize.get());
        if (uploadedFile != null) {
            screenshot.setUploadedFile(uploadedFile);
        }
        complete = true;
    }

    public boolean isComplete() {
        return complete;
    }

    public ScreenshotAction getFollowUpAction() {
        return followUpAction;
    }

    public Screenshot getScreenshot() {
        return screenshot;
    }

    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }
}
