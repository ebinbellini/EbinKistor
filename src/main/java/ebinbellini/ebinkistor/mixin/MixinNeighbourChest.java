package ebinbellini.ebinkistor.mixin;

import ebinbellini.ebinkistor.check.LockableChest;
import ebinbellini.ebinkistor.EbinKistor;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public class MixinNeighbourChest {

    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
    private void onGetPlacementState(ItemPlacementContext ctx, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state == null) {
            return;
        }

        // Get the chest type from the state
        ChestType type = state.get(ChestBlock.CHEST_TYPE);
        if (type == ChestType.SINGLE) {
            return;
        }

        // Calculate adjacent position based on chest orientation and type
        Direction facing = state.get(ChestBlock.FACING);
        BlockPos adjacentPos;

        if (type == ChestType.LEFT) {
            adjacentPos = ctx.getBlockPos().offset(facing.rotateYClockwise());
        } else { // RIGHT
            adjacentPos = ctx.getBlockPos().offset(facing.rotateYCounterclockwise());
        }

        // Check if adjacent block is not a chest
        if (!(ctx.getWorld().getBlockState(adjacentPos).getBlock() instanceof ChestBlock)) {
            return; // No change needed
        }

        // Check if adjacent chest is locked
        LockableChest adjacentChest = (LockableChest) ctx.getWorld().getBlockEntity(adjacentPos);
        if (adjacentChest != null && adjacentChest.isLocked()) {
            // Create a new state with SINGLE chest type
            BlockState newState = state.with(ChestBlock.CHEST_TYPE, ChestType.SINGLE);
            cir.setReturnValue(newState);
        }
    }
}
