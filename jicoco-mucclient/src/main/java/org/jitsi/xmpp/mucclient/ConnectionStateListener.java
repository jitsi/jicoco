/*
 * Copyright @ 2022 - present, 8x8 Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.jitsi.xmpp.mucclient;

import org.jetbrains.annotations.*;

public interface ConnectionStateListener
{
    void connected(@NotNull MucClient mucClient);

    void closed(@NotNull MucClient mucClient);

    void closedOnError(@NotNull MucClient mucClient);

    void reconnecting(@NotNull MucClient mucClient);

    void reconnectionFailed(@NotNull MucClient mucClient);

    void pingFailed(@NotNull MucClient mucClient);
}
