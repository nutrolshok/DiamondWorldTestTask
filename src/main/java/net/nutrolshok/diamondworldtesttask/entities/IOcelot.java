package net.nutrolshok.diamondworldtesttask.entities;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.server.v1_13_R2.*;
import net.nutrolshok.diamondworldtesttask.Main;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IOcelot extends EntityOcelot {

    String name;

    public IOcelot(@NotNull final World world) {
        super(world);

        this.name = new Random().ints(48, 122)
                .filter(i -> (i < 57 || i > 65) && (i < 90 || i > 97))
                .mapToObj(i -> (char) i)
                .limit(5)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                .toString();
    }

    // Pathfinder'ы
    @Override
    protected void n() {
        super.n();

        this.targetSelector.a(1, new PathfinderGoalRandomTargetNonTamed(this, EntityHuman.class, false, null));
    }

    // Метод, возвращающий имя моба
    @Override
    public IChatBaseComponent getDisplayName() {
        return new ChatMessage(this.name);
    }

    // Метод, из-за которого моб убегает от игрока
    @Override
    protected void dz() {}

    // Метод, который вызывается при смерти моба
    @Override
    public void die(@NotNull final DamageSource damageSource) {
        super.die(DamageSource.GENERIC);

        final Entity damager = damageSource.getEntity();
        if(damager == null || !(damager.getBukkitEntity() instanceof Player)) return;

        final ItemStack itemStack = new ItemStack(Material.LEATHER);

        final net.minecraft.server.v1_13_R2.ItemStack nmsItem = CraftItemStack.asNMSCopy(itemStack);
        final NBTTagCompound tag = nmsItem.getTag() != null ? nmsItem.getTag() : new NBTTagCompound();

        tag.setBoolean("iocelot", true);

        nmsItem.setTag(tag);

        final CraftWorld craftWorld = this.world.getWorld();
        final Location location = new Location(craftWorld, this.locX, this.locY, this.locZ);
        craftWorld.dropItemNaturally(location, CraftItemStack.asBukkitCopy(nmsItem));

        Main.getInstance().getMySQLWorker().insert(damager.getName(),
                this.getDisplayName().getString(),
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    }

}
