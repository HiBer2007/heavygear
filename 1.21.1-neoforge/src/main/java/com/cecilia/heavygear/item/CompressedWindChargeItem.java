package com.cecilia.heavygear.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.level.Level;

/** A wind charge with a compressed core: thrown the same way, hits harder, has a cooldown. */
public class CompressedWindChargeItem extends WindChargeItem {
    private static final float SPEED = 1.5F;

    private final int cooldownTicks;
    private final WindChargeFactory factory;

    public CompressedWindChargeItem(Properties properties, int cooldownTicks, WindChargeFactory factory) {
        super(properties);
        this.cooldownTicks = cooldownTicks;
        this.factory = factory;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            WindCharge charge = this.factory.create(serverLevel);
            charge.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
            charge.setOwner(player);
            charge.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, SPEED, 1.0F);
            serverLevel.addFreshEntity(charge);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WIND_CHARGE_THROW,
                    SoundSource.PLAYERS, 0.5F, 0.4F / (serverLevel.getRandom().nextFloat() * 0.4F + 0.8F));
            stack.consume(1, player);
            if (this.cooldownTicks > 0) {
                player.getCooldowns().addCooldown(this, this.cooldownTicks);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Dispensers use this path, so they also fire the compressed variants. */
    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        WindCharge charge = this.factory.create((ServerLevel) level);
        charge.setPos(position.x(), position.y(), position.z());
        return charge;
    }

    /** Creates the projectile for this item. */
    @FunctionalInterface
    public interface WindChargeFactory {
        WindCharge create(ServerLevel level);
    }
}
