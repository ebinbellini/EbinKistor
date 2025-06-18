package ebinbellini.ebinkistor.check;

import net.minecraft.util.math.BlockPos;

public interface ShopableSign {
    void setShopChestPosition(BlockPos pos, String id, String name, boolean serverShop);
    BlockPos getShopChestPosition();
    boolean isShop();
    String getShopOwner();
    String getShopID();
    boolean isServerShop();
}
