package su.nightexpress.excellentcrates.crate.reward.impl;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentcrates.CratesPlugin;
import su.nightexpress.excellentcrates.Placeholders;
import su.nightexpress.excellentcrates.api.crate.RewardType;
import su.nightexpress.excellentcrates.api.item.ItemProvider;
import su.nightexpress.excellentcrates.crate.impl.Crate;
import su.nightexpress.excellentcrates.crate.impl.Rarity;
import su.nightexpress.excellentcrates.item.ItemTypes;
import su.nightexpress.excellentcrates.crate.reward.AbstractReward;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.ItemNbt;
import su.nightexpress.nightcore.util.ItemUtil;
import su.nightexpress.nightcore.util.Players;
import su.nightexpress.nightcore.util.placeholder.Replacer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class ItemReward extends AbstractReward {

    private List<ItemProvider> items;

    public ItemReward(@NotNull CratesPlugin plugin, @NotNull Crate crate, @NotNull String id, @NotNull Rarity rarity) {
        super(plugin, crate, id, rarity);
        this.items = new ArrayList<>();
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config, @NotNull String path) {
        if (config.contains(path + ".Items")) {
            int count = 0;
            for (String encoded : config.getStringList(path + ".Items")) {
                ItemStack itemStack = ItemNbt.decompress(encoded);
                if (itemStack == null) return;

                ItemProvider provider = ItemTypes.vanilla(itemStack);
                config.set(path + ".ItemsData." + count++, provider);
            }
            config.remove(path + ".Items");
        }

        config.getSection(path + ".ItemsData").forEach(sId -> {
            ItemProvider provider = ItemTypes.read(config, path + ".ItemsData." + sId);
            this.items.add(provider);
        });
    }

    @Override
    protected void writeAdditional(@NotNull FileConfig config, @NotNull String path) {
        config.remove(path + ".ItemsData");

        int count = 0;
        for (ItemProvider provider : this.items) {
            config.set(path + ".ItemsData." + count++, provider);
        }
    }

    @Override
    @NotNull
    public UnaryOperator<String> replaceAllPlaceholders() {
        return Placeholders.ITEM_REWARD_EDITOR.replacer(this);
    }

    @Override
    @NotNull
    public RewardType getType() {
        return RewardType.ITEM;
    }

    @Override
    public boolean hasContent() {
        return !this.items.isEmpty();
    }

    public boolean hasInvalidItems() {
        return this.items.stream().anyMatch(provider -> !provider.canProduceItem());
    }

    @Override
    public void giveContent(@NotNull Player player) {
        Replacer replacer = this.createContentReplacer(player);

        this.getItems().forEach(provider -> {
            ItemStack itemStack = provider.getItemStack();
            if (ItemTypes.isDummy(itemStack)) return;

            if (this.placeholderApply) {
                replacer.apply(itemStack);
            }

            Players.addItem(player, itemStack);
        });
    }

    @NotNull
    public String getName() {
        return ItemUtil.getSerializedName(this.getPreviewItem());
    }

    @Override
    @NotNull
    public List<String> getDescription() {
        return ItemUtil.getSerializedLore(this.getPreviewItem());
    }

    @NotNull
    public ItemProvider getPreview() {
        return this.items.isEmpty() ? ItemTypes.DUMMY : this.items.getFirst();
    }

    @NotNull
    public List<ItemProvider> getItems() {
        return this.items;
    }

    public void setItems(@NotNull List<ItemProvider> items) {
        this.items = new ArrayList<>(items.stream().limit(27).toList());
        this.items.removeIf(provider -> !provider.isValid());
    }
}
