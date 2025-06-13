package ebinbellini.ebinkistor.check;

import net.minecraft.util.math.BlockPos;

public interface EbinPlayer {
    void setLastSignPos(BlockPos pos);
    BlockPos getLastSignPos();
    void setLastShopPos(BlockPos pos);
    BlockPos getLastShopPos();
}