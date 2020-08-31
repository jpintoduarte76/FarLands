package net.farlands.sanctuary.data.struct;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.farlands.sanctuary.util.Logging;
import net.minecraft.server.v1_16_R2.MojangsonParser;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class JsonItemStack {
    private String itemName;
    private int count;
    private String nbt;
    private transient ItemStack stack;

    public JsonItemStack() {
        this.itemName = null;
        this.count = 0;
        this.nbt = null;
        this.stack = null;
    }

    public ItemStack getStack() {
        if (stack == null)
            genStack();

        return stack;
    }

    private void genStack() {
        net.minecraft.server.v1_16_R2.ItemStack tmp = CraftItemStack.asNMSCopy(new ItemStack(Material.valueOf(itemName.toUpperCase()), count));
        if (nbt != null && !nbt.isEmpty()) {
            try {
                tmp.setTag(MojangsonParser.parse(nbt));
            } catch (CommandSyntaxException ex) {
                Logging.error("Invalid item JSON detected.");
                return;
            }
        }
        stack = CraftItemStack.asBukkitCopy(tmp);
    }
}
