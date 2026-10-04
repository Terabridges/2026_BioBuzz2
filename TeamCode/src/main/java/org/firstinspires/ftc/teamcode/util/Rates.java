package org.firstinspires.ftc.teamcode.util;

/**
 * Link for desmos visualization of formulas: https://www.desmos.com/calculator/ig1zzfog0r
 *
 * This code is taken from an open source flight control library called 'BetaFlight',
 * for further information, see this article:
 *  https://oscarliang.com/rates/
 */
public class Rates {

    private double centerRate;
    private double maxRate;
    private double expo;
    private double deadzone;

    /**
     *
     * @param centerRate slope of the curve at center position
     * @param maxRate max rate the computer reads 1
     * @param expo blends the values of two different exponents. When expo = 0, the curve x^5 is used. when expo = 1, the curve x^5 is used
     * @param deadzone zone that kills movement past a certain point of throttle on the Joystick.
     */
    public Rates(double centerRate, double maxRate, double expo, double deadzone) {
        if (centerRate > maxRate) {
            throw new IllegalArgumentException("Center rate must be less than max rate.");
        }

        this.centerRate = centerRate;
        this.maxRate = maxRate;
        this.expo = expo;
        this.deadzone = deadzone;
    }

    /**
     * apply Deadzone prevents robot twitches while the joystick is moving without intent.
     * @param stickPosition
     * @return
     */
    private double applyDeadzone(double stickPosition) {
        double magnitude = Math.abs(stickPosition);
        if (magnitude < deadzone) {
            return 0.0;
        }

        return Math.signum(stickPosition) * (magnitude - deadzone) / (1 - deadzone);
    }

    /**
     *
     * @param stickPosition A parameter read from a controller that should be in the domain {-1..1}
     * @return A power value representative of what we want to hand to a Drive train / motors
     */
    public double apply(double stickPosition) {
        if (Math.abs(stickPosition) > 1) {
            throw new IllegalArgumentException("Stick Position cannot be more than 1 or less than -1");
        }

        stickPosition = applyDeadzone(stickPosition);
        double actualRatesExpoFactor =
                Math.abs(stickPosition) * (Math.pow(stickPosition, 5) * expo + stickPosition * (1-expo));

        return (centerRate * stickPosition) +
                ((maxRate - centerRate) * actualRatesExpoFactor);
    }
}
