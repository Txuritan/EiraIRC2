// Copyright (c) 2015, Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.client;

import com.google.common.collect.Maps;
import net.blay09.mods.eirairc.api.upload.UploadHoster;
import net.blay09.mods.eirairc.config.ClientGlobalConfig;

import java.util.Collection;
import java.util.Map;

public class UploadManager {

    private static final Map<String, UploadHoster> uploadHosters = Maps.newHashMap();
    private static String[] availableHosters;

    public static UploadHoster getUploadHoster(String name) {
        return uploadHosters.get(name);
    }

    public static void registerUploadHoster(UploadHoster hoster) {
        uploadHosters.put(hoster.getName(), hoster);
        availableHosters = null;
        ClientGlobalConfig.updateUploadHosters(getAvailableHosters());
    }

    public static Collection<UploadHoster> getUploadHosters() {
        return uploadHosters.values();
    }

    public static String[] getAvailableHosters() {
        if (availableHosters == null) {
            availableHosters = uploadHosters.keySet().toArray(new String[uploadHosters.size()]);
        }
        return availableHosters;
    }

    public static boolean isValidHoster(String name) {
        return uploadHosters.containsKey(name);
    }
}
