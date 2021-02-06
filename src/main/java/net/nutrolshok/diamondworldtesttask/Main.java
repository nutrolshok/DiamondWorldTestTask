package net.nutrolshok.diamondworldtesttask;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.server.v1_13_R2.*;
import net.nutrolshok.diamondworldtesttask.entities.IOcelot;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftItem;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public final class Main extends JavaPlugin {

    static Main instance;
    MySQLWorker mySQLWorker;

    @Override
    public void onEnable() {
        instance = this;

        loadConfig();

        this.mySQLWorker = new MySQLWorker(Objects.requireNonNull(getConfig().getConfigurationSection("mysql")));

        EntityTypes.a("ocelot", EntityTypes.a.a(IOcelot.class, IOcelot::new));

        Bukkit.getPluginManager().registerEvents(new IListener(), this);

        registerProtocolLibEvent();
    }

    @Override
    public void onDisable() {
        this.mySQLWorker.finish();
    }

    public static Main getInstance() {
        return instance;
    }

    private void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if(!configFile.exists()) saveDefaultConfig();
        reloadConfig();
    }

    private void registerProtocolLibEvent() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                Main.getInstance(),
                ListenerPriority.NORMAL,
                PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(@NotNull final PacketEvent event) {
                if(event.getPlayer() == null || event.getPacket() == null || !event.getPlayer().isOnline()) return;

                try {
                    final Object object = event.getPacket().getEntityModifier(event).read(0);
                    if(!(object instanceof CraftItem)) return;

                    final CraftItem item = (CraftItem) object;

                    final net.minecraft.server.v1_13_R2.ItemStack nmsItem = CraftItemStack.asNMSCopy(item.getItemStack());
                    final NBTTagCompound tag = nmsItem.getTag() != null ? nmsItem.getTag() : new NBTTagCompound();

                    if(!tag.hasKey("iocelot")) return;

                    final List<WrappedWatchableObject> list = event.getPacket().getWatchableCollectionModifier().read(0);
                    list.get(2).setValue(Optional.of(new ChatComponentText(event.getPlayer().getName())));
                    list.get(3).setValue(true);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
        });
    }

}
