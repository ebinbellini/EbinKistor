package ebinbellini.ebinkistor.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import ebinbellini.ebinkistor.check.LockableChest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.ChestBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WitherEntity.class)
public abstract class MixinWither {

    @ModifyExpressionValue(method = "mobTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/boss/WitherEntity;canDestroy(Lnet/minecraft/block/BlockState;)Z"))

    private boolean canDestroy(boolean original, @Local BlockPos blockPos) {
        World world = ((Entity) (Object) this).getWorld();

        if (world.getBlockState(blockPos).getBlock() instanceof ChestBlock) {
            LockableChest chestEntity = (LockableChest) world.getBlockEntity(blockPos);
            return original && (chestEntity == null || !chestEntity.isLocked());
        } else {
            return original;
        }
    }
}
