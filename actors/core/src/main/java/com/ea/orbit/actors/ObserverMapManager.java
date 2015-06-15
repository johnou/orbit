/*
 Copyright (C) 2015 Electronic Arts Inc.  All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
 2.  Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
     its contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ea.orbit.actors;

import com.ea.orbit.concurrent.Task;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * {@link ObserverMapManager} is an optional thread safe "map" that can be used by actors which need to
 * perform various operations on an {@link Pingable} linked to another object eg. playerId.
 *
 * <p>Its principal utility method is {@code cleanup()} which asynchronously removes dead observers.
 *
 * @author Johno Crawford (johno@sulake.com)
 */
public class ObserverMapManager<K, V extends Pingable> implements Serializable
{

    private static final long serialVersionUID = 1L;

    private final ConcurrentHashMap<K, V> observers = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public Task<?> cleanup()
    {
        Stream stream = this.observers.entrySet().stream().map(entry -> {
            Pingable observer = entry.getValue();
            return (observer.ping()).whenComplete((Object result, Throwable throwable) -> {
                if (throwable != null)
                {
                    onObserverError(entry.getKey(), entry.getValue());
                    remove(entry.getKey());
                }
            });
        });
        return Task.allOf(stream);
    }

    /**
     * This method is invoked when an observer is removed as a result of {@code ObserverMapManager.cleanup()}.
     *
     * @param key the observer's key.
     * @param observer the observer reference.
     */
    public void onObserverError(K key, V observer) {

    }

    public V remove(K key)
    {
        return this.observers.remove(key);
    }

    public V get(K key)
    {
        return this.observers.get(key);
    }

    public V put(K key, V observer)
    {
        if (key == null)
        {
            throw new NullPointerException("Key must not be null");
        }
        if (observer == null)
        {
            throw new NullPointerException("Observer must not be null");
        }
        return this.observers.put(key, observer);
    }

    public void forEach(BiConsumer<K, V> callable)
    {
        this.observers.forEach(callable);
    }

    public int size()
    {
        return this.observers.size();
    }

    public Stream<V> stream()
    {
        return this.observers.values().stream();
    }
}
