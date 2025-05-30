package fbanna.chestprotection.mixin;

import fbanna.chestprotection.check.LockableChest;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Objects;

@Mixin(ChestBlock.class)
public class MixinNeighbourChest {
    @ModifyVariable(method = "getPlacementState", at = @At(value = "STORE", ordinal = 3))
    private ChestType doubleChest(ChestType type, ItemPlacementContext ctx) {
        // Determine the adjacent block position based on the player's facing and intended chest type
        // TODO: fix this
        BlockPos blockPos;
        if (type == ChestType.LEFT) {
            blockPos = ctx.getBlockPos().offset(ctx.getHorizontalPlayerFacing().getOpposite().rotateYClockwise(), 1);
        } else if (type == ChestType.RIGHT) {
            blockPos = ctx.getBlockPos().offset(ctx.getHorizontalPlayerFacing().getOpposite().rotateYCounterclockwise(), 1);
        } else {
            blockPos = ctx.getBlockPos();
        }
        if (!(ctx.getWorld().getBlockState(blockPos).getBlock() instanceof ChestBlock)) {
            return ChestType.SINGLE;
        }
        LockableChest chestEntity = (LockableChest) ctx.getWorld().getBlockEntity(blockPos);

        if (chestEntity != null && chestEntity.isLocked()) {
            //String playerID = ctx.getPlayer().getUuid().toString();
            String playerID = "TESTNING!?";
            if (Objects.equals(playerID, !chestEntity.getLockID().equals(playerID))) {
                return type;
            } else {
                return ChestType.SINGLE;
            }
        } else {
            return type;
        }
    }
}
