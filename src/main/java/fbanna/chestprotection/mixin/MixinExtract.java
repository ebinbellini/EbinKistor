package fbanna.chestprotection.mixin;

import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.util.math.Direction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fbanna.chestprotection.check.LockableChest;

import java.util.Objects;

@Mixin(HopperBlockEntity.class)
public class MixinExtract {

    @Inject(method = "canExtract", at = @At("HEAD"), cancellable = true)
    private static void inject(Inventory hopperInventory, Inventory fromInventory, ItemStack stack, int slot, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        // Get the block that is being extracted from
        HopperBlockEntity hopper = (HopperBlockEntity) (Object) hopperInventory;
        //net.minecraft.util.math.BlockPos blockPos = hopper.getPos().offset(facing);
        net.minecraft.util.math.BlockPos blockPos = hopper.getPos().offset(Direction.UP);
        net.minecraft.world.World world = hopper.getWorld();

        if (world.getBlockEntity(blockPos) instanceof LockableChest) {
            LockableChest chestEntity = (LockableChest) world.getBlockEntity(blockPos);
            if (chestEntity.isLocked()) {
                cir.setReturnValue(false);
            }
        }
    }
}
