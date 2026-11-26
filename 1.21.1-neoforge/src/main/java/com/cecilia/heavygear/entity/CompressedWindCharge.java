package com.cecilia.heavygear.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A wind charge with a compressed core: darkened model and a stronger blast.
 *
 * <p>1.21.1 has no impact-scale hook on {@code WindCharge}, so the scale lives on this entity: the
 * vanilla explosion is performed first (identical sound, particles, damage and knockback) and the
 * velocity delta it wrote into every nearby creature is multiplied afterwards, which is exactly the
 * "five times the impact of one wind charge" the 1.21.11 build produced with a mixin.</p>
 */
public class CompressedWindCharge extends WindCharge {
    /** Radius used to snapshot the entities the blast can affect. */
    private static final double SNAPSHOT_RADIUS = 8.0D;

    private final double impactScale;

    public CompressedWindCharge(EntityType<? extends WindCharge> type, Level level) {
        this(type, level, 2.0D);
    }

    protected CompressedWindCharge(EntityType<? extends WindCharge> type, Level level, double impactScale) {
        super(type, level);
        this.impactScale = impactScale;
    }

    public double heavygear$impactScale() {
        return this.impactScale;
    }

    @Override
    protected void explode(Vec3 pos) {
        Level level = this.level();
        if (level.isClientSide() || this.impactScale <= 1.0D) {
            super.explode(pos);
            return;
        }
        AABB box = new AABB(pos, pos).inflate(SNAPSHOT_RADIUS);
        List<Entity> entities = level.getEntities(this, box);
        List<Entity> kept = new ArrayList<>(entities.size());
        List<Vec3> before = new ArrayList<>(entities.size());
        for (Entity entity : entities) {
            kept.add(entity);
            before.add(entity.getDeltaMovement());
        }

        // the vanilla gust: same damage calculator, radius, particles and sound as a wind charge
        level.explode(this, null, AbstractWindCharge.EXPLOSION_DAMAGE_CALCULATOR,
                pos.x(), pos.y(), pos.z(), 1.2F, false,
                Level.ExplosionInteraction.TRIGGER,
                ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE,
                SoundEvents.WIND_CHARGE_BURST);

        for (int i = 0; i < kept.size(); i++) {
            Entity entity = kept.get(i);
            if (!entity.isAlive()) {
                continue;
            }
            Vec3 old = before.get(i);
            Vec3 delta = entity.getDeltaMovement().subtract(old);
            if (delta.lengthSqr() < 1.0E-8D) {
                continue;
            }
            entity.setDeltaMovement(old.add(delta.scale(this.impactScale)));
            entity.hurtMarked = true;
            if (entity instanceof ServerPlayer player) {
                com.cecilia.heavygear.item.CompressedWindChargeImpulse.markLaunch(player.getUUID(), player.getY());
                if (this.impactScale > 3.0D) {
                    com.cecilia.heavygear.item.CompressedWindChargeImpulse.markDoubleLaunch(
                            player.getUUID(), player.getY());
                }
            }
        }
    }
}
