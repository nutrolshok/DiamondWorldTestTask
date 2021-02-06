package net.nutrolshok.diamondworldtesttask;

import net.minecraft.server.v1_13_R2.WorldServer;
import net.nutrolshok.diamondworldtesttask.entities.IOcelot;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class IListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onZombieDeath(@NotNull final EntityDeathEvent event) {
        final Entity entity = event.getEntity();

        if(!(entity instanceof Zombie)) return;

        final Location location = entity.getLocation();
        final WorldServer world = ((CraftWorld) Objects.requireNonNull(location.getWorld())).getHandle();

        final IOcelot ocelot = new IOcelot(world);
        ocelot.setPosition(location.getX(), location.getY(), location.getZ());

        world.addEntity(ocelot, CreatureSpawnEvent.SpawnReason.CUSTOM);
    }

}
