// Copyright (c) 2015 Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.api.event;

import net.blay09.mods.eirairc.api.upload.UploadedFile;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.io.File;

/**
 * This event is published on the MinecraftForge.EVENTBUS bus whenever EiraIRC has uploaded a screenshot.
 * It is also published for failed uploads, in which case uploadedFile will be null.
 */
public class ScreenshotUploadEvent extends Event {

    /**
     * the screenshot file that was meant to be uploaded
     */
    public final File sourceFile;

    /**
     * the resulting URLs of the screenshot upload or null if the upload failed
     */
    public final UploadedFile uploadedFile;

    /**
     * INTERNAL EVENT. YOU SHOULD NOT POST THIS YOURSELF.
     *
     * @param sourceFile   the screenshot file that was meant to be uploaded
     * @param uploadedFile the resulting URLs of the screenshot upload or null if the upload failed
     */
    public ScreenshotUploadEvent(File sourceFile, UploadedFile uploadedFile) {
        this.sourceFile = sourceFile;
        this.uploadedFile = uploadedFile;
    }

}
