package fbanna.chestprotection.mixin;

import java.util.List;

import fbanna.chestprotection.ChestProtection;
import fbanna.chestprotection.check.LockableChest;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinExplosion {
    @Inject(method = "canExplosionDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void prevent_explosion(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof ChestBlock) {
            LockableChest chestEntity = (LockableChest) world.getBlockEntity(pos);

            if (chestEntity != null && chestEntity.isLocked()) {
                cir.setReturnValue(false);
            }
        }
    }
}