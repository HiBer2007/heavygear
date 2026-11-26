package com.cecilia.heavygear.combat;

/**
 * Marks the single {@code Player#attack} call that the vanilla riptide dash performs when it touches
 * a creature ({@code checkAutoSpinAttack -> doAutoAttackOnTouch -> attack}).
 *
 * <p>Only that contact hit should lose the mace smash: the Spear of Destiny is a spear, so any
 * deliberate attack (charged attack, stab) must keep its smash bonus and feedback. Deliberate stabs
 * never go through {@code attack}, and the flag is cleared as soon as {@code doAutoAttackOnTouch}
 * returns, so nothing else is affected.</p>
 */
public final class RiptideContact {
    private static boolean active;

    private RiptideContact() {
    }

    public static void begin() {
        active = true;
    }

    public static void end() {
        active = false;
    }

    public static boolean isActive() {
        return active;
    }
}
