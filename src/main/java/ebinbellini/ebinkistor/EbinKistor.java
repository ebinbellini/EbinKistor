package ebinbellini.ebinkistor;

import ebinbellini.ebinkistor.check.LockableChest;
import ebinbellini.ebinkistor.trade.Bank;
import ebinbellini.ebinkistor.commands.Commands;
import ebinbellini.ebinkistor.check.EbinPlayer;
import ebinbellini.ebinkistor.check.ShopableSign;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.block.enums.ChestType;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EbinKistor implements ModInitializer {

    public static final String MOD_ID = "ebinkistor";
    public static final Logger LOGGER = LoggerFactory.getLogger("EbinKistor");

    @Override
    public void onInitialize() {
        LOGGER.info("Nu skyddas dina kistor!");

        // Register commands
        CommandRegistrationCallback.EVENT.register(Commands::registerCommands);

        // Check for block use
        UseBlockCallback.EVENT.register(
                (player, world, hand, hitResult) -> {
                    // Check if the block is a chest
                    if (world.getBlockState(hitResult.getBlockPos()).getBlock() == Blocks.CHEST) {
                        return OnChestClicked(player, world, hand, hitResult);
                    } else if (world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof SignBlock) {
                        return OnSignBlockClicked(player, world, hand, hitResult);
                    }

                    return ActionResult.PASS;
                }
        );

        // Check for block breaking
        PlayerBlockBreakEvents.BEFORE.register(
                (world, player, pos, state, entity) -> {
                    if (!player.isSpectator()) {
                        if (!state.getBlock().equals(Blocks.CHEST)) {
                            return true; // Not a chest block, allow breaking
                        }
                        LockableChest lockableChest = (LockableChest) world.getBlockEntity(pos);
                        if (lockableChest == null) {
                            return true; // No lockable chest found, allow breaking
                        }
                        // Check if the chest is locked
                        if (lockableChest.isLocked()) {
                            // If locked, check if the player is the one who locked it
                            if (!Objects.equals(lockableChest.getLockID(), player.getUuid().toString())) {
                                player.sendMessage(
                                        Text.translatable("Kistan är låst av %s. Du kan inte slå sönder den!".formatted(lockableChest.getLockOwner())).formatted(Formatting.RED), true);
                                return false; // Prevent breaking
                            }
                        }
                    }
                    return true;
                }
        );
    }

    public static ActionResult OnChestClicked(PlayerEntity player, net.minecraft.world.World world, net.minecraft.util.Hand hand, net.minecraft.util.hit.BlockHitResult hitResult) {
        if (hand != null) {
            // Check if the player is holding a stick
            if (player.getStackInHand(hand).isOf(Items.STICK)) {
                // Check if player is clicking a chest block
                if (hitResult.getBlockPos() == null || world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof ChestBlock) {
                    ChestBlockEntity chestEntity = (ChestBlockEntity) world.getBlockEntity(hitResult.getBlockPos());
                    ChestBlock chestBlock = (ChestBlock) world.getBlockState(hitResult.getBlockPos()).getBlock();

                    BlockPos pos = hitResult.getBlockPos();

                    // Store the player name who clicked the chest
                    String playerName = player.getName().getString();
                    String playerID = player.getUuid().toString();

                    LockableChest lockableChest = (LockableChest) chestEntity;

                    // Check if the chest is already locked
                    if (lockableChest.isLocked()) {
                        if (lockableChest.getLockID().equals(playerID)) {
                            // If locked by the same player, unlock it
                            lockableChest.setLockingPlayer(null, null);
                            world.markDirty(pos);
                            player.sendMessage(
                                    Text.translatable("Kistan är nu upplåst!").formatted(Formatting.YELLOW), true);

                            // Check for double chest and unlock the other half
                            LOGGER.info("Checking for double chest for unlocking at {}", pos);
                            ChestType chestType = chestEntity.getCachedState().get(ChestBlock.CHEST_TYPE);
                            BlockPos otherHalfPos = null;
                            switch (chestType) {
                                case LEFT:
                                    otherHalfPos = pos.offset(chestBlock.getFacing(chestEntity.getCachedState()));
                                    break;
                                case RIGHT:
                                    otherHalfPos = pos.offset(chestBlock.getFacing(chestEntity.getCachedState()));
                                    break;
                                default:
                                    break;
                            }
                            if (otherHalfPos != null) {
                                BlockEntity otherEntity = chestEntity.getWorld().getBlockEntity(otherHalfPos);
                                if (otherEntity instanceof LockableChest) {
                                    ((LockableChest) otherEntity).setLockingPlayer(null, null);
                                    world.markDirty(otherHalfPos);
                                }
                            }
                        } else {
                            // If locked, update the locking player information
                            player.sendMessage(
                                    Text.translatable("Kistan är redan låst av %s.".formatted(lockableChest.getLockOwner()))
                                            .formatted(Formatting.BLUE), true);
                        }
                    } else {
                        // If not locked, set the locking player information
                        // Lock both sides if it's a double chest
                        lockableChest.setLockingPlayer(playerID, playerName);

                        // Check for double chest and lock the other half
                        ChestType chestType = chestEntity.getCachedState().get(ChestBlock.CHEST_TYPE);
                        BlockPos otherHalfPos = null;
                        switch (chestType) {
                            case LEFT:
                                otherHalfPos = pos.offset(chestBlock.getFacing(chestEntity.getCachedState()));
                                break;
                            case RIGHT:
                                otherHalfPos = pos.offset(chestBlock.getFacing(chestEntity.getCachedState()));
                                break;
                            default:
                                break;
                        }
                        if (otherHalfPos != null) {
                            BlockEntity otherEntity = chestEntity.getWorld().getBlockEntity(otherHalfPos);
                            if (otherEntity instanceof LockableChest) {
                                ((LockableChest) otherEntity).setLockingPlayer(playerID, playerName);
                                world.markDirty(otherHalfPos);
                            }
                        }

                        world.markDirty(pos);
                        player.sendMessage(
                                Text.translatable("Kistan är nu låst av dig!").formatted(Formatting.GREEN), true);
                    }
                }

                return ActionResult.FAIL;
            }

            if (player.getStackInHand(hand).isOf(Items.REDSTONE)) {
                LockableChest lockableChest = (LockableChest) world.getBlockEntity(hitResult.getBlockPos());
                if (!lockableChest.isLocked()) {
                    player.sendMessage(
                            Text.translatable("Kistan måste vara låst för att skapa en butik!").formatted(Formatting.RED), false);
                    return ActionResult.FAIL;
                }

                if (!lockableChest.getLockID().equals(player.getUuid().toString())) {
                    player.sendMessage(
                            Text.translatable("Kistan är låst av %s. Du kan inte skapa butik av den!".formatted(lockableChest.getLockOwner())).formatted(Formatting.RED), false);
                    return ActionResult.FAIL;
                }

                EbinPlayer ebinPlayer = (EbinPlayer) player;
                BlockPos lastSignPos = ebinPlayer.getLastSignPos();
                if (lastSignPos == null || !(world.getBlockEntity(lastSignPos) instanceof ShopableSign)) {
                    player.sendMessage(
                            Text.translatable("Du måste först klicka på en skylt med rödsten!").formatted(Formatting.RED), false);
                    return ActionResult.FAIL;
                }

                ShopableSign signEntity = (ShopableSign) world.getBlockEntity(lastSignPos);

                if (signEntity != null && signEntity.getShopChestPosition() != null) {
                    player.sendMessage(
                            Text.translatable("Denna skylt är redan kopplad till en butik!").formatted(Formatting.RED), false);
                    return ActionResult.FAIL;
                }

                String playerID = player.getUuid().toString();
                String playerName = player.getName().getString();
                signEntity.setShopChestPosition(hitResult.getBlockPos(), playerID, playerName);
                player.sendMessage(Text.translatable("Din butik är nu skapad!").formatted(Formatting.GREEN), false);
                ebinPlayer.setLastSignPos(null);

                return ActionResult.FAIL;
            }
        }

        if (!player.isSpectator()) {
            LockableChest chestEntity = (LockableChest) world.getBlockEntity(hitResult.getBlockPos());
            String playerID = player.getUuid().toString();
            String playerName = player.getName().getString();

            if (chestEntity != null && chestEntity.isLocked() && !chestEntity.getLockID().equals(playerID)) {
                player.sendMessage(
                        Text.translatable("Kistan är låst av %s!".formatted(playerName)).formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
        }

        return ActionResult.PASS;
    }

    public static ActionResult OnSignBlockClicked(PlayerEntity player, net.minecraft.world.World world, net.minecraft.util.Hand hand, net.minecraft.util.hit.BlockHitResult hitResult) {
        BlockPos pos = hitResult.getBlockPos();
        SignBlockEntity signEntity = (SignBlockEntity) world.getBlockEntity(pos);
        ShopableSign shopableSign = (ShopableSign) signEntity;

        // Check if the player is holding redstone dust
        if (hand != null && player.getStackInHand(hand).isOf(Items.REDSTONE)) {
            if (shopableSign.getShopChestPosition() != null) {
                player.sendMessage(
                        Text.translatable("Denna skylt är redan kopplad till en butik!").formatted(Formatting.RED), false);
                return ActionResult.FAIL;
            }

            EbinPlayer ebinPlayer = (EbinPlayer) player;
            ebinPlayer.setLastSignPos(pos);
            player.sendMessage(
                    Text.translatable("Klicka nu på en kista för att slutföra din butik!").formatted(Formatting.BLUE), false);

            return ActionResult.FAIL;
        }

        // Check if the sign is a shop
        if (shopableSign.isShop()) {
            BlockEntity shopEntity = world.getBlockEntity(shopableSign.getShopChestPosition());
            if (shopEntity == null || !(shopEntity instanceof LockableChest)) {
                player.sendMessage(
                        Text.translatable("Denna skylt är inte kopplad till en butik!").formatted(Formatting.RED), false);
                return ActionResult.FAIL;
            }
            LockableChest shopChest = (LockableChest) shopEntity;
            if (!shopChest.isLocked()) {
                player.sendMessage(
                        Text.translatable("Butikens kista är inte låst!").formatted(Formatting.RED), false);
                return ActionResult.FAIL;
            }
            if (!shopChest.getLockID().equals(shopableSign.getShopID())) {
                player.sendMessage(
                        Text.translatable("Kistan och skylten ägs av olika spelare!").formatted(Formatting.RED), false);
                return ActionResult.FAIL;
            }

            SignText texts = signEntity.getFrontText();

            String firstLine = texts.getMessage(0, false).getString().trim().toUpperCase();
            String lastLine = texts.getMessage(3, false).getString().trim();
            int price = 0;
            try {
                price = Integer.parseInt(lastLine);
            } catch (NumberFormatException e) {
                player.sendMessage(Text.literal("Butiken har ogiltigt pris (" + lastLine + ") på sista raden").formatted(Formatting.RED), false);
                return ActionResult.FAIL;
            }

            // Find the first item in the shop chest
            ChestBlockEntity chestEntity = (ChestBlockEntity) shopChest;
            ItemStack firstItem = ItemStack.EMPTY;
            boolean hasMultipleItems = false;
            int firstItemIndex = -1;
            for (int i = 0; i < chestEntity.size(); i++) {
                ItemStack stack = chestEntity.getStack(i);
                if (!stack.isEmpty() && stack.getCount() > 0) {
                    if (firstItem.isEmpty()) {
                        // Found the first item
                        firstItem = stack;
                        firstItemIndex = i;
                    }

                    boolean sameEnchantments = EnchantmentHelper.getEnchantments(firstItem).equals(EnchantmentHelper.getEnchantments(stack));
                    if (!firstItem.isOf(stack.getItem()) || !sameEnchantments) {
                        // There are multiple different items or items with different enchantments in the chest
                        hasMultipleItems = true;
                        break;
                    }
                }
            }

            Bank bank = Bank.getServerState(world.getServer());
            if (bank == null) {
                player.sendMessage(Text.literal("Banken är inte tillgänglig just nu. Kom tillbaka om 3-5 arbetsdagar").formatted(Formatting.RED), false);
                return ActionResult.FAIL;
            }

            if (firstLine.equals("KÖP")) {
                // Buying from the shop
                if (firstItem.isEmpty()) {
                    player.sendMessage(Text.literal("Butiken har inget att sälja!").formatted(Formatting.RED), false);
                    return ActionResult.FAIL;
                }

                // Print what the shop is selling
                MutableText itemName = firstItem.getItemName().copy();

                // Add enchantment information if present
                if (firstItem.hasEnchantments() || firstItem.isOf(Items.ENCHANTED_BOOK)) {
                    ItemEnchantmentsComponent itemEnchantments = EnchantmentHelper.getEnchantments(firstItem);
                    MutableText enchantmentsText = Text.literal("");
                    itemEnchantments.getEnchantments().forEach(enchantmentEntry -> {
                        int level = itemEnchantments.getLevel(enchantmentEntry);
                        Text enchantmentName = Enchantment.getName(enchantmentEntry, level);
                        if (!enchantmentsText.getString().isEmpty()) {
                            enchantmentsText.append(", ");
                        }
                        enchantmentsText.append(enchantmentName);
                    });
                    if (!enchantmentsText.getString().isEmpty()) {
                        itemName = itemName.append(" (Förtrollningar: ").append(enchantmentsText).append(")");
                    }
                } else {
                    LOGGER.info("Item has no enchantments.");
                }

                if (hasMultipleItems) {
                    player.sendMessage(Text.literal("Butiken säljer flera olika saker. Det tillåts inte!").formatted(Formatting.RED), false);
                    return ActionResult.FAIL;
                }

                EbinPlayer ebinPlayer = (EbinPlayer) player;
                if (ebinPlayer.getLastShopPos() == null || !ebinPlayer.getLastShopPos().equals(pos)) {
                    player.sendMessage(Text.literal("Butiken säljer").formatted(Formatting.YELLOW)
                            .append(Text.literal(" ").formatted(Formatting.YELLOW))
                            .append(itemName.formatted(Formatting.AQUA))
                            .append(Text.literal(" för " + price + " blocksdaler").formatted(Formatting.YELLOW)), false);
                    player.sendMessage(Text.literal("Klicka igen på skylten för att köpa").formatted(Formatting.YELLOW), false);
                    player.sendMessage(Text.literal("Du kan smyga för att köpa 64 st.").formatted(Formatting.YELLOW), false);
                    ebinPlayer.setLastShopPos(pos);
                } else {
                    // Player clicked the sign again to buy
                    int playerMoney = bank.getBalance(player.getUuid().toString());
                    int itemsToBuy = player.isSneaking() ? 64 : 1;
                    int itemsBought = 0;

                    if (playerMoney < price) {
                        player.sendMessage(Text.literal("Du har inte tillräckligt med pengar för att köpa!").formatted(Formatting.RED), false);
                        return ActionResult.FAIL;
                    }

                    while (itemsToBuy > itemsBought) {
                        // Deduct money from the player's account
                        bank.transfer(player.getUuid().toString(), shopableSign.getShopID(), price);
                        playerMoney -= price;

                        firstItem = ItemStack.EMPTY;
                        firstItemIndex = -1;
                        for (int i = 0; i < chestEntity.size(); i++) {
                            ItemStack stack = chestEntity.getStack(i);
                            if (!stack.isEmpty() && stack.getCount() > 0) {
                                firstItem = stack;
                                firstItemIndex = i;
                            }
                        }

                        if (firstItem.isEmpty()) {
                            player.sendMessage(Text.literal("Butiken har inget mer att sälja!").formatted(Formatting.RED), false);
                            break;
                        }

                        // Give the item to the player
                        ItemStack itemToGive = firstItem.copy();
                        itemToGive.setCount(1); // Give only one item at a time

                        if (!player.getInventory().insertStack(itemToGive)) {
                            player.dropItem(itemToGive, false);
                        }

                        // Remove the item from the shop chest
                        if (firstItem.getCount() > 1) {
                            firstItem.decrement(1);
                        } else {
                            chestEntity.setStack(firstItemIndex, ItemStack.EMPTY);
                        }

                        itemsBought++;

                        playerMoney = bank.getBalance(player.getUuid().toString());
                        if (playerMoney < price) {
                            player.sendMessage(Text.literal("Du hade bara råd att köpa " + price + " st.").formatted(Formatting.YELLOW), false);
                            break;
                        }
                    }

                    playerMoney = bank.getBalance(player.getUuid().toString());
                    player.sendMessage(Text.literal("Du köpte " + itemsBought + " st. ")
                            .append(itemName)
                            .append(" för " + (itemsBought * price) + " blocksdaler")
                            .formatted(Formatting.GREEN), false);
                    player.sendMessage(Text.literal("Du har nu " + playerMoney + " blocksdaler kvar").formatted(Formatting.GREEN), false);

                    world.markDirty(shopableSign.getShopChestPosition());
                }
            } else if (firstLine.equals("SÄLJ")) {
                // Buying from the shop
                if (firstItem.isEmpty()) {
                    player.sendMessage(Text.literal("Butiken vill inte köpa något").formatted(Formatting.RED), false);
                    return ActionResult.FAIL;
                }

                EbinPlayer ebinPlayer = (EbinPlayer) player;
                if (ebinPlayer.getLastShopPos() == null || !ebinPlayer.getLastShopPos().equals(pos)) {
                    player.sendMessage(Text.literal("Butiken köper " + firstItem.getItemName().getString() + " för " + price + " blocksdaler").formatted(Formatting.YELLOW), false);
                    player.sendMessage(Text.literal("Klicka igen på skylten för att sälja det du håller i").formatted(Formatting.YELLOW), false);
                    player.sendMessage(Text.literal("Du kan smyga för att sälja allt i din hand").formatted(Formatting.YELLOW), false);
                    ebinPlayer.setLastShopPos(pos);
                    return ActionResult.FAIL;
                }

                if (player.getStackInHand(hand).isEmpty()) {
                    player.sendMessage(Text.literal("Du måste hålla ett föremål i handen för att sälja!").formatted(Formatting.RED), false);
                    return ActionResult.FAIL;
                }

                ItemStack itemToSell = player.getStackInHand(hand);
                if (itemToSell.getCount() <= 0) {
                    player.sendMessage(Text.literal("Du har inget att sälja!").formatted(Formatting.RED), false);
                    return ActionResult.FAIL;
                }

                int itemsToSellCount = player.isSneaking() ? itemToSell.getCount() : 1;
                String itemName = firstItem.getItemName().getString();

                // Check if the item is the same as the one in the shop
                if (!itemToSell.isOf(firstItem.getItem())) {
                    player.sendMessage(Text.literal("Du kan bara sälja ").append(itemName).append(" till butiken!").formatted(Formatting.RED), false);
                    return ActionResult.FAIL;
                }

                // Check if the item has the same enchantments as the one in the shop
                boolean sameEnchantments = EnchantmentHelper.getEnchantments(firstItem).equals(EnchantmentHelper.getEnchantments(itemToSell));
                if (!sameEnchantments) {
                    player.sendMessage(Text.literal("Du kan bara sälja ").append(itemName).append(" med samma förtrollningar!").formatted(Formatting.RED), false);
                    return ActionResult.FAIL;
                }

                // Check if the player has enough items to sell
                if (itemToSell.getCount() < 1) {
                    player.sendMessage(Text.literal("Du har inte tillräckligt med " + itemName + " att sälja!").formatted(Formatting.RED), false);
                    return ActionResult.FAIL;
                }


                int itemsSold = 0;
                while (itemsToSellCount > itemsSold) {
                    // Check if the player has enough money
                    int shopMoney = bank.getBalance(shopableSign.getShopID());
                    if (shopMoney < price) {
                        player.sendMessage(Text.literal("Butiken har inte tillräckligt med pengar för att sälja!").formatted(Formatting.RED), false);
                        break;
                    }

                    // Check if the player is holding the item to sell
                    itemToSell = player.getStackInHand(hand);
                    if (itemToSell.isEmpty() || itemToSell.getCount() < 1) {
                        break;
                    }

                    // Add the item to the shop chest
                    boolean itemTransferred = false;
                    for (int i = 0; i < chestEntity.size(); i++) {
                        ItemStack stack = chestEntity.getStack(i);
                        if (stack.isEmpty()) {
                            chestEntity.setStack(i, itemToSell.split(1));
                            itemTransferred = true;
                            break;
                        } else if (stack.getCount() < stack.getMaxCount()) {
                            // Add to existing stack
                            chestEntity.setStack(i, chestEntity.getStack(i).copyWithCount(stack.getCount() + 1));
                            itemTransferred = true;
                            break;
                        }
                    }

                    if (!itemTransferred) {
                        player.sendMessage(Text.literal("Butiken har inte plats för fler " + itemName).formatted(Formatting.RED), false);
                        break;
                    }

                    // Remove one item from the player's hand
                    itemToSell.decrement(1);

                    // Transfer money from the shop's account to the player's account
                    bank.transfer(shopableSign.getShopID(), player.getUuid().toString(), price);
                    itemsSold++;
                }

                world.markDirty(shopableSign.getShopChestPosition());

                int playerMoney = bank.getBalance(player.getUuid().toString());
                if (itemsSold > 0) {
                    player.sendMessage(Text.literal("Du sålde " + itemsSold + " st. " + itemName + " för " + (itemsSold * price) + " blocksdaler").formatted(Formatting.GREEN), false);
                    player.sendMessage(Text.literal("Du har nu " + playerMoney + " blocksdaler kvar").formatted(Formatting.GREEN), false);
                }
            }

            return ActionResult.FAIL;
        }

        return ActionResult.PASS;
    }
}
