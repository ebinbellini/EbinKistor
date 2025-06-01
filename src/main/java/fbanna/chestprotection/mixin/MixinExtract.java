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

import fbanna.chestprotection.ChestProtection;

import java.util.Objects;

@Mixin(HopperBlockEntity.class)
public class MixinExtract {

    @Inject(method = "canExtract", at = @At("HEAD"), cancellable = true)
    private static void inject(Inventory hopperInventory, Inventory fromInventory, ItemStack stack, int slot, Direction facing, CallbackInfoReturnable<Boolean> cir) {
        HopperBlockEntity hopper = (HopperBlockEntity) (Object) hopperInventory;
        net.minecraft.world.World world = hopper.getWorld();

        // Check if the feeding chest is locked
        net.minecraft.util.math.BlockPos feedingPos = hopper.getPos().offset(Direction.UP);
        if (world.getBlockEntity(feedingPos) instanceof LockableChest) {
            LockableChest chestEntity = (LockableChest) world.getBlockEntity(feedingPos);
            if (chestEntity.isLocked()) {
                // Block extraction if the chest is locked
                cir.setReturnValue(false);
                return;
            }
        }

        // Check if the receiving chest is locked
        net.minecraft.util.math.BlockPos receivingPos = hopper.getPos().offset(facing);
        if (world.getBlockEntity(receivingPos) instanceof LockableChest) {
            LockableChest chestEntity = (LockableChest) world.getBlockEntity(receivingPos);
            if (chestEntity.isLocked()) {
                // Block extraction if the chest is locked
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
