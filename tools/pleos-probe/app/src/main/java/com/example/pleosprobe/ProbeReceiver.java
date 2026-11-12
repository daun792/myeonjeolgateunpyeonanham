package com.example.pleosprobe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import ai.pleos.playground.vehicle.Vehicle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import kotlin.Unit;

/** Diagnostic only: read a brake-pedal state, never infer parking or control vehicle. */
public final class ProbeReceiver extends BroadcastReceiver {
    private static final String TAG = "PleosReadOnlyProbe";
    @Override public void onReceive(Context context, Intent intent) {
        PendingResult pending = goAsync();
        new Thread(() -> {
            Vehicle vehicle = null;
            try {
                vehicle = new Vehicle(context.getApplicationContext());
                vehicle.initialize();
                Log.i(TAG, "initialize returned");
                CountDownLatch latch = new CountDownLatch(1);
                AtomicBoolean readable = new AtomicBoolean(false);
                vehicle.getBrake().checkBrakePedalStatusCapabilities(config -> {
                    Log.i(TAG, "capability success: " + config.getAccessType());
                    String access = config.getAccessType().name();
                    readable.set("READ".equals(access) || "READ_WRITE".equals(access));
                    latch.countDown();
                    return Unit.INSTANCE;
                }, error -> {
                    Log.e(TAG, "capability error: " + error.getClass().getName() + ": " + error.getMessage());
                    latch.countDown();
                    return Unit.INSTANCE;
                });
                if (!latch.await(4, TimeUnit.SECONDS)) Log.e(TAG, "capability callback timeout");
                if (readable.get()) {
                    CountDownLatch valueLatch = new CountDownLatch(1);
                    vehicle.getBrake().getCurrentBrakePedalStatus(value -> {
                        Log.i(TAG, "brake pedal read success: " + value);
                        valueLatch.countDown();
                        return Unit.INSTANCE;
                    }, error -> {
                        Log.e(TAG, "brake pedal read error: " + error.getClass().getName() + ": " + error.getMessage());
                        valueLatch.countDown();
                        return Unit.INSTANCE;
                    });
                    if (!valueLatch.await(4, TimeUnit.SECONDS)) Log.e(TAG, "brake pedal read callback timeout");
                }
            } catch (Throwable error) {
                Log.e(TAG, "probe failed", error);
            } finally {
                if (vehicle != null) {
                    try { vehicle.release(); } catch (Throwable error) { Log.e(TAG, "release failed", error); }
                }
                pending.finish();
            }
        }, "pleos-read-only-probe").start();
    }
}
