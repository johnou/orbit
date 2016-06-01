/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

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

package cloud.orbit.actors.serialization;

import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import cloud.orbit.actors.ActorObserver;
import cloud.orbit.actors.cluster.NodeAddress;
import cloud.orbit.actors.runtime.AbstractActor;
import cloud.orbit.actors.runtime.BasicRuntime;
import cloud.orbit.actors.runtime.RemoteReference;

import java.io.IOException;
import java.io.Serializable;

/**
 * @author Johno Crawford (johno@sulake.com)
 */
public class RemoteReferenceSerializer extends FSTBasicObjectSerializer
{

    @Override
    public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPositioin) throws Exception
    {
        ReferenceReplacement replacement = (ReferenceReplacement) in.readObject();
        if (replacement.address != null)
        {
            return BasicRuntime.getRuntime().getRemoteObserverReference(replacement.address, (Class) replacement.interfaceClass, replacement.id);
        }
        return BasicRuntime.getRuntime().getReference((Class) replacement.interfaceClass, replacement.id);
    }

    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException
    {
        final RemoteReference reference;
        if ((toWrite instanceof RemoteReference))
        {
            reference = (RemoteReference) toWrite;
        }
        else
        {
            if (toWrite instanceof AbstractActor)
            {
                reference = RemoteReference.from(((AbstractActor) toWrite));
            }
            else if (toWrite instanceof ActorObserver)
            {
                ActorObserver objectReference = BasicRuntime.getRuntime().registerObserver(null, (ActorObserver) toWrite);
                reference = (RemoteReference) objectReference;
            }
            else
            {
                throw new IllegalStateException("Unsupported class " + toWrite.getClass());
            }
        }

        ReferenceReplacement replacement = new ReferenceReplacement();
        replacement.address = RemoteReference.getAddress(reference);
        replacement.interfaceClass = RemoteReference.getInterfaceClass(reference);
        replacement.id = RemoteReference.getId(reference);
        out.writeObject(replacement);
    }

    private static class ReferenceReplacement implements Serializable
    {
        private static final long serialVersionUID = 1L;

        Class<?> interfaceClass;
        Object id;
        NodeAddress address;
    }
}