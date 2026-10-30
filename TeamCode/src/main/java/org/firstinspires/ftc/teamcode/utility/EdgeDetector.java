package org.firstinspires.ftc.teamcode.utility;

import java.util.Optional;
import java.util.function.Consumer;

import javax.annotation.Nullable;

public class EdgeDetector implements Consumer<Boolean> {

    private final Optional<Runnable> risingEdge;
    private final Optional<Runnable> fallingEdge;

    public boolean wasPressed = false;

    //Constructor
    public EdgeDetector(
            @Nullable Runnable risingEdge,
            @Nullable Runnable fallingEdge)
    {
        this.risingEdge = Optional.ofNullable(risingEdge);
        this.fallingEdge = Optional.ofNullable(fallingEdge);
    }
    public EdgeDetector(Runnable f, boolean isFalling) {
        if (isFalling) {
            this.risingEdge = Optional.empty();
            this.fallingEdge = Optional.of(f);
        } else {
            this.risingEdge = Optional.of(f);
            this.fallingEdge = Optional.empty();
        }
    }

    public EdgeDetector(Runnable f) {
        this(f, false);
    }

    public void update(Boolean condition) {
        accept(condition);
    }

    @Override
    public void accept(Boolean isPressed) {
        if (isPressed && !wasPressed) { //Rising Edge
            risingEdge.ifPresent(Runnable::run);
        } else if (!isPressed && wasPressed) { //Falling edge
            fallingEdge.ifPresent(Runnable::run);
        }
        wasPressed = isPressed;
    }

    public void reset() {
        wasPressed = false;
    }
}
