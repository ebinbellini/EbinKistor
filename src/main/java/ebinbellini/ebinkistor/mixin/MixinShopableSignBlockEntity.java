package ebinbellini.ebinkistor.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;

import net.minecraft.block.entity.SignBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ebinbellini.ebinkistor.check.ShopableSign;


@Mixin(SignBlockEntity.class)
public class MixinShopableSignBlockEntity implements ShopableSign {
    @Unique
    private BlockPos shopPosition;

    @Unique
    private String lockOwnerID;

    @Unique
    private String lockOwnerName;

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void readLockData(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup, CallbackInfo ci) {
        if (nbt.contains("lockOwnerID")) {
            this.lockOwnerID = nbt.getString("lockOwnerID").isPresent() ? nbt.getString("lockOwnerID").get() : null;
            this.lockOwnerName = nbt.getString("lockOwnerName").isPresent() ? nbt.getString("lockOwnerName").get() : null;
        } else {
            this.lockOwnerID = null;
            this.lockOwnerName = null;
        }

        if (nbt.contains("shopPosition")) {
            NbtCompound shopPosNbt = nbt.getCompound("shopPosition").get();
            this.shopPosition = new BlockPos(
                shopPosNbt.getInt("x").get(),
                shopPosNbt.getInt("y").get(),
                shopPosNbt.getInt("z").get()
            );
        } else {
            this.shopPosition = null;
        }
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void writeLockData(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup, CallbackInfo ci) {
        if (lockOwnerID != null) {
            nbt.putString("lockOwnerID", lockOwnerID);
            nbt.putString("lockOwnerName", lockOwnerName);
        } else {
            nbt.remove("lockOwnerID");
            nbt.remove("lockOwnerName");
        }

        if (shopPosition != null) {
            NbtCompound shopPosNbt = new NbtCompound();
            shopPosNbt.putInt("x", shopPosition.getX());
            shopPosNbt.putInt("y", shopPosition.getY());
            shopPosNbt.putInt("z", shopPosition.getZ());
            nbt.put("shopPosition", shopPosNbt);
        } else {
            nbt.remove("shopPosition");
        }
    }

    @Unique
    public boolean isShop() {
        return this.shopPosition != null;
    }

    @Unique
    public void setShopChestPosition(BlockPos pos, String id, String name) {
        this.shopPosition = pos;
        this.lockOwnerID = id;
        this.lockOwnerName = name;
    }

    @Unique
    public BlockPos getShopChestPosition() {
        return this.shopPosition;
    }

    @Unique
    public String getShopOwner() {
        return lockOwnerName;
    }

    @Unique
    public String getShopID() {
        return lockOwnerID;
    }
}
