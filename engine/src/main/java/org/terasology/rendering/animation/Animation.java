/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.animation;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

/*
 * Single animation that traverses frames.
 */
public final class Animation {
    private final List<AnimationListener> listeners = new ArrayList<AnimationListener>();

    private RepeatMode repeatMode;

    private float elapsedTime;
    private int currentFrame;

    private TimeModifier timeModifier;

    private enum AnimState {
        STOPPED, PAUSED, RUNNING
    }

    private enum RepeatMode {
        RUN_ONCE,
        REPEAT_INFINITE
    }

    private AnimState currentState = AnimState.STOPPED;

    private final float duration;

    private Animator animator;

    /**
     * @param animator the animator that is updated over time
     * @param duration the duration in seconds (must be positive)
     * @param repeatMode the repeat mode
     * @param timeModifier the time modifier to apply
     */
    private Animation(Animator animator, float duration, RepeatMode repeatMode, TimeModifier timeModifier) {
        Preconditions.checkArgument(animator != null);
        Preconditions.checkArgument(repeatMode != null);
        Preconditions.checkArgument(timeModifier != null);
        Preconditions.checkArgument(duration > 0);

        this.animator = animator;
        this.duration = duration;
        this.repeatMode = repeatMode;
        this.timeModifier = timeModifier;
    }

    /**
     * Constructs a new animation that runs once with linear speed.
     * @param animator the animator that is updated over time
     * @param duration the duration in seconds
     * @param timeModifier the time modifier to apply
     * @return the animation
     */
    public static Animation once(Animator animator, float duration, TimeModifier timeModifier) {
        Animation anim = new Animation(animator, duration, RepeatMode.RUN_ONCE, timeModifier);
        return anim;
    }

     /**
      * Creates an animation that loops infinitely
      * @param animator the animator that is updated over time
      * @param duration the duration in seconds (must be positive)
      * @param timeModifier the time modifier to apply
      * @return the animation
      */
     public static Animation infinite(Animator animator, float duration, TimeModifier timeModifier) {
         Animation anim = new Animation(animator, duration, RepeatMode.REPEAT_INFINITE, timeModifier);
         return anim;
     }

    /**
     * Updates the animation if {@link #start} has been called and is not finished.
     *
     * @param delta elapsed time since last update, in seconds.
     */
    public void update(float delta) {
        if (currentState != AnimState.RUNNING) {
            return;
        }

        elapsedTime += delta;
        while (elapsedTime > duration) {
            elapsedTime -= duration;
            currentFrame++;

            if (repeatMode == RepeatMode.RUN_ONCE) {
                stop();
                return;
            }
        }

        updateAnimator();
    }

    private void updateAnimator() {
        float ipol = timeModifier.apply(elapsedTime / duration);
        animator.apply(ipol);
    }

    /**
     * Notifies that this animation has been set up and is ready for use.
     * @return this
     */
    public Animation start() {
        if (currentState == AnimState.STOPPED) {
            currentState = AnimState.RUNNING;
            elapsedTime = 0;
            for (AnimationListener li : this.listeners) {
                li.onStart();
            }
            updateAnimator();
        }
        return this;
    }

    /**
     * Notifies that this animation is finished or should end.
     * @return this
     */
    public Animation stop() {
        if (currentState == AnimState.RUNNING) {
            currentState = AnimState.STOPPED;
            updateAnimator();
            for (AnimationListener li : this.listeners) {
                li.onEnd();
            }
        }
        return this;
    }

    /**
     * Stops an animation without signaling that it is finished and
     * maintains its current state.
     * @return this
     */
    public Animation pause() {
        if (currentState == AnimState.RUNNING) {
            currentState = AnimState.PAUSED;
        }
        return this;
    }

    /**
     * Resumes a paused animation.
     * @return this
     */
    public Animation resume() {
        if (currentState == AnimState.PAUSED) {
            currentState = AnimState.RUNNING;
        }
        return this;
    }

    /**
     * @return the current frame, always non-negative
     */
    public int getCurrentFrame() {
        return currentFrame;
    }

    /**
     * Adds a listener for animation events.
     *
     * @param li the listener for animation events
     */
    public void addListener(AnimationListener li) {
        this.listeners.add(li);
    }


    /**
     * Unsubscribes a listener from animation events.
     *
     * @param li the listener to stop receiving animation events for
     */
    public void removeListener(AnimationListener li) {
        this.listeners.remove(li);
    }

    public boolean isRunning() {
        return currentState == AnimState.RUNNING;
    }
}
