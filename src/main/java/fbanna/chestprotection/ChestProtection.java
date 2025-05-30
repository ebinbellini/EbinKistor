package fbanna.chestprotection;

import eu.pb4.sgui.api.gui.SimpleGui;

import fbanna.chestprotection.check.LockableChest;
import fbanna.chestprotection.check.CheckChest;
import fbanna.chestprotection.trade.TradeScreen;
import fbanna.chestprotection.trade.setup.SetupScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.block.SignBlock;
import net.minecraft.block.enums.ChestType;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChestProtection implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("ChestProtection");

    // TODO remove
    public static List<CheckChest> SHOPS = new ArrayList<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Nu skyddas dina kistor!");

        // Check for block use
        UseBlockCallback.EVENT.register(
                (player, world, hand, hitResult) -> {
                    // Check if the block is a minecraft:chest block 
                    if (world.getBlockState(hitResult.getBlockPos()).getBlock() == Blocks.CHEST) {
                        return OnChestClicked(player, world, hand, hitResult);
                    } else if (world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof SignBlock) {
                        LOGGER.info("Sign block clicked at {}", hitResult.getBlockPos());
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
                            LOGGER.info("Chest at {} is locked by {}", pos, lockableChest.getLockOwner());
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
        // Check if the player is holding a stick
        if (hand != null && player.getStackInHand(hand).isOf(Items.STICK)) {
            // Check if player is clicking a chest block
            if (hitResult.getBlockPos() == null || world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof ChestBlock) {
                ChestBlockEntity chestEntity = (ChestBlockEntity) world.getBlockEntity(hitResult.getBlockPos());
                ChestBlock chestBlock = (ChestBlock) world.getBlockState(hitResult.getBlockPos()).getBlock();
                player.sendMessage(
                        Text.translatable("Du håller i en pinne för att interagera med kistor... Bra jobbat!").formatted(Formatting.GREEN), true);

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

        if (!player.isSpectator()) {
            LockableChest chestEntity = (LockableChest) world.getBlockEntity(hitResult.getBlockPos());
            String playerID = player.getUuid().toString();
            String playerName = player.getName().getString();

            if (chestEntity != null && chestEntity.isLocked() && !chestEntity.getLockID().equals(playerID)) {
                player.sendMessage(
                        Text.translatable("Kistan är låst av %s!".formatted(playerName)).formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
            /*else if (book.chestStatus == CheckChest.status.SELL) {
                            SHOPS.add(book);
                            SimpleGui gui = new TradeScreen((ServerPlayerEntity) player, book);
                            gui.open();

                            return ActionResult.FAIL;
                        } else if (book.chestStatus == CheckChest.status.ERROR) {
                            if (Objects.equals(book.author, player.getName().getString())) {
                                SimpleGui gui = new SetupScreen((ServerPlayerEntity) player, book);
                                gui.open();
                            } else {
                                player.sendMessage(Text.translatable("Shop is in an error state. Contact %s!".formatted(book.author))
                                        .formatted(Formatting.RED), true);
                            }
                            return ActionResult.FAIL;
                        }*/
        }
        return ActionResult.PASS;
    }
}
