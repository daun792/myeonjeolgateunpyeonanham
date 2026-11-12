package com.example.vehicleprivacy;

import android.car.Car;
import android.car.drivingstate.CarUxRestrictions;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.Objects;

/** Read-only UX restriction observer. Unrestricted UX never proves that the car is parked. */
public final class CarUxParkingVeto {
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final UxRestrictionVeto veto;
    private Car car;
    private CarUxRestrictionsManager manager;
    private long sourceGeneration;
    private long connectionGeneration;
    private boolean active;

    public CarUxParkingVeto(Context context, DisclosureGate gate) {
        this.context = Objects.requireNonNull(context).getApplicationContext();
        this.veto = new UxRestrictionVeto(gate, SystemClock::elapsedRealtime);
    }

    /** Start for a newly issued DisclosureGate session, on the main thread. */
    public void start(long gateSession) {
        requireMainThread();
        stop();
        sourceGeneration = veto.start(gateSession);
        active = true;
        final long expected = sourceGeneration;
        try {
            if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) return;
            car = Car.createCar(context, main, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT,
                    (source, ready) -> main.post(() -> onLifecycle(expected, source, ready)));
            if (car == null) veto.onRestrictions(expected, null);
        } catch (RuntimeException | LinkageError error) {
            veto.onRestrictions(expected, null);
            disconnectCar();
        }
    }

    /** Stop observation and invalidate this provider's evidence. */
    public void stop() {
        requireMainThread();
        active = false;
        connectionGeneration++;
        releaseManager();
        disconnectCar();
        veto.stop();
    }

    private void onLifecycle(long expected, Car source, boolean ready) {
        if (!active || expected != sourceGeneration) return;
        final long expectedConnection = ++connectionGeneration;
        if (!ready) {
            releaseManager();
            veto.onRestrictions(expected, null);
            return;
        }
        releaseManager();
        try {
            Object service = source.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE);
            if (!(service instanceof CarUxRestrictionsManager)) {
                veto.onRestrictions(expected, null);
                return;
            }
            manager = (CarUxRestrictionsManager) service;
            manager.registerListener(restrictions -> main.post(
                    () -> accept(expected, expectedConnection, restrictions)));
            accept(expected, expectedConnection, manager.getCurrentCarUxRestrictions());
        } catch (RuntimeException | LinkageError error) {
            releaseManager();
            veto.onRestrictions(expected, null);
        }
    }

    private void accept(long expected, long expectedConnection, CarUxRestrictions restrictions) {
        if (!active || expected != sourceGeneration || expectedConnection != connectionGeneration) return;
        try {
            veto.onRestrictions(expected, restrictions == null
                    ? null : restrictions.isRequiresDistractionOptimization());
        } catch (RuntimeException | LinkageError error) {
            veto.onRestrictions(expected, null);
        }
    }

    private void releaseManager() {
        CarUxRestrictionsManager previous = manager;
        manager = null;
        if (previous != null) {
            try { previous.unregisterListener(); } catch (RuntimeException | LinkageError ignored) { }
        }
    }

    private void disconnectCar() {
        Car previous = car;
        car = null;
        if (previous != null) {
            try { previous.disconnect(); } catch (RuntimeException | LinkageError ignored) { }
        }
    }

    private static void requireMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Vehicle UX observer must use the main thread");
        }
    }
}
