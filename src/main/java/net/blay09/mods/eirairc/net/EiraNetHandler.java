// Copyright (c) 2015, Christopher "BlayTheNinth" Baker

package net.blay09.mods.eirairc.net;

import com.google.common.collect.Maps;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;

import java.util.Map;

public class EiraNetHandler {

    private final Map<String, EiraPlayerInfo> playerInfoMap = Maps.newHashMap();

    public EiraPlayerInfo getPlayerInfo(String username) {
        synchronized (playerInfoMap) {
            EiraPlayerInfo playerInfo = playerInfoMap.get(username);
            if (playerInfo == null) {
                playerInfo = new EiraPlayerInfo(username);
                playerInfoMap.put(username, playerInfo);
            }
            return playerInfo;
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerLoggedOutEvent event) {
        synchronized (playerInfoMap) {
            playerInfoMap.remove(event.player.getName());
        }
    }

}
