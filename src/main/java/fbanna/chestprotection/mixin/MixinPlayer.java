package fbanna.chestprotection.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import fbanna.chestprotection.check.EbinPlayer;

@Mixin(PlayerEntity.class)
public class MixinPlayer implements EbinPlayer {
    @Unique
    private BlockPos lastSignPos = null;
    
    @Unique
    @Override
    public void setLastSignPos(BlockPos pos) {
        this.lastSignPos = pos;
    }
    
    @Unique
    @Override
    public BlockPos getLastSignPos() {
        return this.lastSignPos;
    }

    @Unique
    private BlockPos lastShopPos = null;
    
    @Unique
    public void setLastShopPos(BlockPos pos) {
        this.lastShopPos = pos;
    }
    
    @Unique
    public BlockPos getLastShopPos() {
        return this.lastShopPos;
    }
}