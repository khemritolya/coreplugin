package org.coreplugin;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedSoundEffect;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.lang.reflect.Field;

public class GhastSoundListener implements Listener {

    private static final String HANDLER = "ghast_moan_filter";
    private static final Field SOUND_NAME;

    static {
        try {
            SOUND_NAME = PacketPlayOutNamedSoundEffect.class.getDeclaredField("a");
            SOUND_NAME.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Cannot find sound name field in PacketPlayOutNamedSoundEffect", e);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        inject(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remove(event.getPlayer());
    }

    private static void inject(Player player) {
        ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel
            .pipeline().addBefore("packet_handler", HANDLER, new ChannelDuplexHandler() {
                @Override
                public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                    if (msg instanceof PacketPlayOutNamedSoundEffect) {
                        try {
                            if ("mob.ghast.moan".equals(SOUND_NAME.get(msg))) return;
                        } catch (IllegalAccessException ignored) {}
                    }
                    super.write(ctx, msg, promise);
                }
            });
    }

    private static void remove(Player player) {
        io.netty.channel.ChannelPipeline pipeline = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel.pipeline();
        if (pipeline.get(HANDLER) != null) pipeline.remove(HANDLER);
    }
}