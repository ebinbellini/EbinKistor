package ebinbellini.ebinkistor.mixin;

import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ebinbellini.ebinkistor.check.LockableChest;

@Mixin(ChestBlockEntity.class)
public class MixinLockableChestBlockEntity implements LockableChest {
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
    }

    @Unique
    public boolean isLocked() {
        return lockOwnerID != null && !lockOwnerID.equals("");
    }

    @Unique
    public void setLockingPlayer(String id, String name) {
        this.lockOwnerID = id;
        this.lockOwnerName = name;
    }

    @Unique
    public String getLockOwner() {
        return lockOwnerName;
    }

    @Unique
    public String getLockID() {
        return lockOwnerID;
    }
}
