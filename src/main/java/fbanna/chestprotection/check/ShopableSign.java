package fbanna.chestprotection.check;

import net.minecraft.util.math.BlockPos;

public interface ShopableSign {
    void setShopChestPosition(BlockPos pos, String id, String name);
    BlockPos getShopChestPosition();
    boolean isShop();
    String getShopOwner();
    String getShopID();
}
